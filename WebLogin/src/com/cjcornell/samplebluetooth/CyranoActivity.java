/**
 * CLASS: CyranoActivity
 *   This activity is where the troubleshooting scripts and all the friend displays are shown.
 */

package com.cjcornell.samplebluetooth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.cjcornell.samplebluetooth.data.AppSettings;
import com.cjcornell.samplebluetooth.data.DataStore;
import com.facebook.Session;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class CyranoActivity extends Activity implements AudioMethods.AudioCompletionNotifiable {
    private final static String TAG = "Cyrano";
    private final static int MAX_BRANCHES = 4;
    public final static String DISPLAY_FRIENDS = "com.cjcornell.samplebluetooth.DISPLAY_FRIENDS";
    
    // Milliseconds before automatically closing splash screen
    private final static int SPLASH_TIMEOUT = 10000;
    
    /**
     * Layout attributes - it makes sense to put these here as they are accessed by many methods
     */
    private RelativeLayout friendContent;
    private ImageView friendPicture;
    private TextView friendName;
    private TextView friendCoordinates;
    
    private RelativeLayout friendsContent;
    private ListView friendsList;
    private RelativeLayout commandGroupContent;
    private ListView commandGroups;
    private CommandGroupAdapter commandGroupAdapter;
    
    private LinearLayout mainContent, playbackControls, branchControls;
    
    @SuppressWarnings("unused")
    private ImageView mainPicture, splashPicture;
    private TextView mainTitle, mainMessage;
    private Button pauseButton, stopButton, previousButton, nextButton;
    private Button[] branchButtons;
    private ScheduledThreadPoolExecutor runner;
    private ScheduledFuture<?> autoAdvance = null;
    private Handler uiHandler;
    
    // Used with friend notifications
    private BroadcastReceiver gotFriends = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Got message to display friends.");
            List<Friend> friends = Arrays.asList((Friend[])intent.getSerializableExtra("friends"));
            displayFriends(friends);
        }
    };
    
    
    // A list of troubleshooting items
    private List<Item> tsItems = new ArrayList<Item>();
    private Item currentItem;
    
    // Application state variables
    private boolean currentlyTroubleshooting;
    private Runnable backButtonAction;
    
    // Dialog that displays the splash screen
    private Dialog mSplashDialog;
    
    
    /**
     * Automatically advances to the next item. You can cancel the advance if (e.g.) the user
     * presses the next or previous button.
     * 
     * If there is nothing to advance to (we are the last item), we clear the UI and finish the
     * troubleshooting script.
     */
    private class Advancer implements Runnable {
        private Item whichItem;
        public Advancer(Item i) {
            whichItem = i;
        }
        public void run() {
            Log.v(TAG, "Automatically advancing to next item...");
            autoAdvance = null;
            uiHandler.post(new Runnable() {
                public void run() {
                    if (whichItem.isLast()) {
                        finishTroubleshooting();
                    } else if (whichItem.canAdvance()) {
                        advanceItem(1);
                    }
                }
            });
        }
    }
    
    
    /**
     * Executed when the activity is first created
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cyrano);
        
        // We must set these AFTER the content view is set
        setLayoutAttributes();
        showSplashScreen();
        
        // Load up the command groups
        new CommandGroupTask(this).execute();
        runner = new ScheduledThreadPoolExecutor(0);
        runner.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        runner.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        
        // Give the settings activity access to this instance
        SettingsActivity.activity = CyranoActivity.this;
        
        // Load default settings
        AppSettings.initSettings(this);
        
        // Initialize for BT (if a headset is connected)
        BluetoothManager.enableBluetooth(this);
        
        // Start up the FriendFinderService
        startService(new Intent(this, FriendFinderService.class));
    }
    
    /**
     * Called when the options menu is created
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.cyrano, menu);
        return true;
    }
    
    /**
     * Called when an option is selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get the option selected, and do the appropriate action
        int itemId = item.getItemId();
        Intent stopFFS;
        switch (itemId) {
            case R.id.action_nearby:
                switchToNearbyFriends();
                return true;
            case R.id.action_scripts:
                if (currentlyTroubleshooting) {
                    finishTroubleshooting();
                } else {
                    resetUI();
                }
                return true;
            case R.id.action_settings:
                if (currentlyTroubleshooting) {
                    currentItem.pause();
                }
                Intent intent = new Intent(CyranoActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_account:
                showAccountInformation();
                return true;
            case R.id.action_about:
                showAbout();
                return true;
            case R.id.action_logout:
                stopFFS = new Intent(FriendFinderService.SHUTDOWN_FFS);
                LocalBroadcastManager.getInstance(this).sendBroadcast(stopFFS);
                Session session = Session.getActiveSession();
                session.closeAndClearTokenInformation();
                Log.v(TAG, "Logout detected");
                
                // Make an intent to close all activities (and including) under CyranoActivitiy
                Intent closeIntent = new Intent(getApplicationContext(), CyranoActivity.class);
                closeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                closeIntent.putExtra("EXIT", true);
                
                // Make a new intent for the login page
                Intent loginIntent = new Intent(this, WebLogin.class);
                
                // Start both activities
                startActivity(closeIntent);
                startActivity(loginIntent);
                finish();
                return true;
            case R.id.action_exit:
                stopFFS = new Intent(FriendFinderService.SHUTDOWN_FFS);
                LocalBroadcastManager.getInstance(this).sendBroadcast(stopFFS);
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                startActivity(homeIntent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /**
     * Called when the pause play control is pressed
     */
    @Override
    protected void onPause() {
        cancelAutoAdvance();
        uiHandler = null;
        if (currentItem != null) {
            currentItem.pause();
        }
        AudioMethods.stopTextToSpeech();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gotFriends);
        
        super.onPause();
    }
    
    

    /**
     * Called when the application is closed
     */
    @Override
    protected void onDestroy() {
        stopService(new Intent(this, FriendFinderService.class));
        super.onDestroy();
    }
    
    /**
     * XXX: We stop the GPS friend detect stuff when Cyrano is closed. Perhaps later we'll want
     * to keep running, but do something besides switch to Cyrano and read it out? 
     */
    @Override
    protected void onResume() {
        super.onResume();
        
        uiHandler = new Handler();
        
        IntentFilter iff = new IntentFilter(DISPLAY_FRIENDS);
        LocalBroadcastManager.getInstance(this).registerReceiver(gotFriends, iff);
        //if (currentItem != null) {
        //    currentItem.pause();
        // }
        if (currentItem != null) {
            currentItem.resume();
         }
    }
    
    /**
     * Called when the back button play control is pressed
     */
    @Override
    public void onBackPressed() {
        if (backButtonAction != null) {
            // copy the action in case r.run() sets a new action.
            Runnable r = backButtonAction;
            backButtonAction = null;
            r.run();
        } else {
            super.onBackPressed();
        }
    }
    
    /**
     * Shows the splash screen over the CyranoActivity
     */
    protected void showSplashScreen() {
        mSplashDialog = new Dialog(this, R.style.SplashScreen);
        mSplashDialog.setContentView(R.layout.splash_screen);
        //setSplashImage();
        mSplashDialog.setCancelable(false);
        mSplashDialog.show();
        
        // Set Runnable to remove splash screen just in case
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            removeSplashScreen();
          }
        }, SPLASH_TIMEOUT);
    }
    
    /**
     * Removes the Dialog that displays the splash screen
     */
    protected void removeSplashScreen() {
        if (mSplashDialog != null) {
            mSplashDialog.dismiss();
            mSplashDialog = null;
        }
    }

    /**
     * Show the nearby friends display
     */
    protected void switchToNearbyFriends() {
        // If we are in troubleshooting mode, do not auto advance and pause the current item before switching displays
        if (currentlyTroubleshooting) {
            cancelAutoAdvance();
            if (currentItem != null) {
                currentItem.pause();
            }
        }
        // Get the list of friends to display and display them
        List<Friend> fl = FriendFinderService.friendList;
        if (fl != null && fl.size() > 0) {
            displayMultipleFriends(fl);
            setupFriendsDisplay();
            setUpFriendDisplayBackButton();
        
        // If no friends are nearby, display a toast stating this
        } else {
            Toast.makeText(this, R.string.noFriendsMessage, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show the about display with Cyrano's version info
     */
    public void showAbout() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.app_name);
            PackageInfo info;
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionStr = getString(R.string.versionText, info.versionName);
            builder.setMessage(versionStr);
            builder.create();
            builder.show();
        } catch (NameNotFoundException e) {
            Log.w(TAG, e);
        }
    }

    /**
     * Show the screen with the user's account information - this is the same as the single friend display
     */
    private void showAccountInformation() {
        cancelAutoAdvance();
        setupFriendDisplay(true);
        setUpFriendDisplayBackButton();
        Friend me = DataStore.getInstance().getMe();
        displaySingleFriend(me, true);
    }


    /** Set all the layout attributes */
    public void setLayoutAttributes() {
        friendContent = (RelativeLayout)findViewById(R.id.friendContent);
        friendPicture = (ImageView)findViewById(R.id.friendPicture);
        friendName = (TextView)findViewById(R.id.friendName);
        friendCoordinates = (TextView)findViewById(R.id.friendCoordinates);
        
        friendsContent = (RelativeLayout)findViewById(R.id.friendsContent);
        friendsList = (ListView)findViewById(R.id.friendsList);
        
        commandGroupContent = (RelativeLayout)findViewById(R.id.commandGroupContent);
        commandGroups = (ListView)findViewById(R.id.commandGroups);
        
        mainContent = (LinearLayout)findViewById(R.id.mainContent);
        mainTitle = (TextView)findViewById(R.id.mainTitle);
        mainPicture = (ImageView)findViewById(R.id.mainPicture);
        splashPicture = (ImageView)findViewById(R.id.splashPicture);
        mainMessage = (TextView)findViewById(R.id.mainMessage);
        
        playbackControls = (LinearLayout)findViewById(R.id.playbackControls);
        pauseButton = (Button)findViewById(R.id.pauseButton);
        stopButton = (Button)findViewById(R.id.stopButton);
        previousButton = (Button)findViewById(R.id.previousButton);
        nextButton = (Button)findViewById(R.id.nextButton);
        
        branchControls = (LinearLayout)findViewById(R.id.branchControls);
        branchButtons = new Button[MAX_BRANCHES];
        branchButtons[0] = (Button)findViewById(R.id.branch1);
        branchButtons[1] = (Button)findViewById(R.id.branch2);
        branchButtons[2] = (Button)findViewById(R.id.branch3);
        branchButtons[3] = (Button)findViewById(R.id.branch4);
    }
    
    /**
     * Set up the back button functionality for the friend display
     */
    private void setUpFriendDisplayBackButton() {
        if (currentlyTroubleshooting && currentItem != null) {
            backButtonAction = new Runnable() {
                @Override
                public void run() {
                    AudioMethods.stopTextToSpeech();
                    displayItem(currentItem);
                }
            };
        } else {
            backButtonAction = new Runnable() {
                @Override
                public void run() {
                    AudioMethods.stopTextToSpeech();
                    resetUI();
                }
            };
        }
    }
    
    /**
     * Sets up the UI objects for multiple friends. Call {@link #setupFriendsDisplay()} to actually
     * show them. To set up everything for you, taking into account settings, you should be calling
     * {@link #displayFriends(List)}.
     * 
     * @param friends The list of friends to display. Must not be null.
     */
    private void displayMultipleFriends(final List<Friend> friends) {
        // Set the adapter, using the layout corresponding to the current font size setting
        ArrayAdapter<Friend> adapter = null;
        if (AppSettings.textSize < 1) {
            adapter = new ArrayAdapter<Friend>(this, R.layout.layout_list1, friends);
        } else if (AppSettings.textSize == 1) {
            adapter = new ArrayAdapter<Friend>(this, R.layout.layout_list2, friends);
        } else {
            adapter = new ArrayAdapter<Friend>(this, R.layout.layout_list3, friends);
        }
        friendsList.setAdapter(adapter);
        
        // Set up the action for clicking on a friend in the list
        friendsList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setupFriendDisplay(false);
                displaySingleFriend(friends.get(position), false);
                backButtonAction = new Runnable() {
                    @Override
                    public void run() {
                        setupFriendsDisplay();
                        displayMultipleFriends(friends);
                    }
                };
            }
        });
    }
    
    /**
     * Displays the UI objects to view multiple friends.
     */
    private void setupFriendsDisplay() {
        clearUI();
        setTitle(getString(R.string.app_name) + ": " + getString(R.string.multiFriendsHeading));
        friendsContent.setVisibility(View.VISIBLE);
    }
    
    /**
     * Sets up the UI objects for multiple friends. Call {@link #setupFriendDisplay(boolean)} to actually
     * show them. To set up everything for you, taking into account settings, you should be calling
     * {@link #displayFriends(List)}.
     * 
     * @param friend The friend whose details to display. Must not be null.
     * @param isDrilldown If we're coming from the multiple friend list
     * @param isMe If we're displaying the Cyrano user
     */
    private void displaySingleFriend(Friend friend, boolean isMe) {
        friendName.setText(friend.getName() + (friend.getEmail().equals("") ? "" : " (" + friend.getEmail() + ")"));
        if (isMe) {
            friendCoordinates.setText("Facebook ID: " + friend.getId() + "\nMac Address: " + DataStore.getInstance().getMacAddress());
        } else {
            // About to get very hacky...
            StringBuilder sb = new StringBuilder();
            sb.append(AppSettings.formatter.format(friend.getLatitude())).append(", ")
                .append(AppSettings.formatter.format(friend.getLongitude())).append(" (")
                .append(friend.getDistanceString()).append(")\n")
                .append("Details1: ").append(friend.getDetails1()).append("\nDetails2: ")
                .append(friend.getDetails2()).append("\nDetails3: ").append(friend.getDetails3());
            friendCoordinates.setText(sb.toString());
        }
        
        Bitmap picture = null;
        try {
            picture = new FacebookProfileDownloader().execute(friend.getId()).get();
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        } catch (ExecutionException e) {
            Log.e(TAG, e.toString());
        }
        
        // Only display the picture if we are in graphical mode
        if (AppSettings.graphicalMode) {
            friendPicture.setVisibility(View.VISIBLE);
            friendPicture.setImageBitmap(picture);
        } else {
            friendPicture.setVisibility(View.GONE);
        }
        
        // Only say anything if we drill down and friend audio is enabled,
        // and we are not displaying the current Cyrano user.
        // Our service takes care of reading out friends usually.
        if (AppSettings.friendAudio && !isMe) {
            AudioMethods.textToSpeech(this, friend.getName());
        }
    }

    /**
     * Displays the UI objects to show a single friend.
     *  
     * @param isMe If we're displaying the Cyrano user
     */
    private void setupFriendDisplay(boolean isMe) {
        clearUI();
        if (isMe) {
            setTitle(getString(R.string.app_name) + ": " + getString(R.string.myAccountHeading));
        } else {
            setTitle(getString(R.string.app_name) + ": " + getString(R.string.singleFriendHeading));
        }
        friendContent.setVisibility(View.VISIBLE);
    }
    
    /**
     * Display found friends, whether zero, one, or many. You should call this function to
     * display some friends.
     * 
     * @param friends The friends to display. May be null or empty.
     */
    public void displayFriends(final List<Friend> friends) {
        if (friends == null || friends.isEmpty())
        {
            Log.v(TAG, "No friends found nearby");
            return;
        }
        cancelAutoAdvance();
        
        if (AppSettings.autoDisplayFriends) {
            setupFriendsDisplay();
            setUpFriendDisplayBackButton();
        }
        displayMultipleFriends(friends);
    }
    
    
    /** Update the UI for the main display */
    public void displayItem(Item item) {
        clearUI();
        
        Log.v(TAG, "URL: " + item.getURL());
        
        // cancel the auto advance as we display something else
        cancelAutoAdvance();
        if (currentItem != null) {
            currentItem.stop();
        }
        
        currentItem = item;
        setTitle(getString(R.string.app_name) + ": " + item.getGroupName());
        
        // Show the step number if in verbose mode
        mainTitle.setText(item.getName());
        
        // Show the description only if we are NOT in terse mode
        if (!AppSettings.terseMode) { 
            mainMessage.setText(item.getDescription());
            mainMessage.setVisibility(View.VISIBLE);
        }
        
        // We want the main content and playback controls to always be visible
        mainContent.setVisibility(View.VISIBLE);
        playbackControls.setVisibility(View.VISIBLE);
        playControlButtons(item);
        
        // Display the image associated with the item, if there is one and if graphical mode is on
        if (AppSettings.graphicalMode && item.getURL() != null && !"".equals(item.getURL())) {
            mainPicture.setVisibility(View.VISIBLE);
            new DownloadImageTask(mainPicture).execute(item.getURL());
        } else {
            mainPicture.setImageBitmap(null);
        }
        
        boolean shouldShowButtons = false;
        for (int i = 0; i < item.getBranches().size(); ++i) {
            Item.Branch br = item.getBranches().get(i);
            if (br != null) {
                shouldShowButtons = true;
                branchButtons[i].setText(br.label);
                branchButtons[i].setVisibility(View.VISIBLE);
            } else {
                branchButtons[i].setVisibility(View.INVISIBLE);
            }
        }
        if (shouldShowButtons) {
            branchControls.setVisibility(View.VISIBLE);
        }
        
        // play the item automatically
        item.play(this);
        scheduleAutoAdvance(item);
    }
    
    
    /**
     * playControlButtons
     *   This method creates the play control buttons. Based on the play control flags in the database,
     *   it will hide/display various play controls.
     * 
     *   The items know about the item 0 defaults and take them into account.
     * 
     *   @param item: The item currently displayed
     */
    private void playControlButtons(Item item)
    {
        if(item.isStoppable())
            stopButton.setVisibility(View.VISIBLE);
        else
            stopButton.setVisibility(View.INVISIBLE);

        if(item.isPausable())
            pauseButton.setVisibility(View.VISIBLE);
        else
            pauseButton.setVisibility(View.INVISIBLE);
        
        nextButton.setVisibility(View.VISIBLE);
        nextButton.setEnabled(item.canAdvance());
        
        previousButton.setVisibility(View.VISIBLE);
        previousButton.setEnabled(item.canGoBack());
    }
    
    /** 
     * Display a single coaching item 
     * @param itemIndex The instruction number of the item to show (NOT an index into our list of items).
     */
    public void displayItemAt(int itemIndex) {
        if (itemIndex > 0 && itemIndex <= tsItems.size()) {
            displayItem(tsItems.get(itemIndex - 1));
        }
    }
    
    /** Called when the pause button is clicked */
    public void pauseButtonClicked(View view) {
        Log.v(TAG, "Pause/play button clicked.");
        if (pauseButton.getText().equals(getString(R.string.pauseButtonText))) {
            currentItem.pause();
            cancelAutoAdvance();
            pauseButton.setText(getString(R.string.playButtonText));
        } else {
            currentItem.play(this);
            if (autoAdvance == null) {
                scheduleAutoAdvance(currentItem);
            }
            pauseButton.setText(getString(R.string.pauseButtonText));
        }
    }
    
    /** Called when the stop button is clicked */
    public void stopButtonClicked(View view) {
        Log.v(TAG, "Stop button clicked.");
        // Stop aborts the whole troubleshooting script
        finishTroubleshooting();
    }
    
    /** Called when the previous button is clicked */
    public void previousButtonClicked(View view) {
        Log.v(TAG, "Previous button clicked.");
        //Above template will be over-written by non-nulls
        advanceItem(-1);
    }
    
    /** Called when the next button is clicked */
    public void nextButtonClicked(View view) {
        Log.v(TAG, "Next button clicked.");
        //Above template will be over-written by non-nulls
        advanceItem(1);
    }
    
    /** Get the branch button number */
    private int branchButtonNo(View view) {
        for (int i = 0; i < MAX_BRANCHES; ++i) {
            if (branchButtons[i] == view) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * branchClicked
     *   Executed when a branch is clicked. It will start a new CoachingTask with the
     *   appropriate scripts to branch to.
     */
    public void branchClicked(View view) {
        int branchNo = branchButtonNo(view);
        
        // If the branch number is invalid, do nothing
        if (branchNo < 0 || branchNo >= currentItem.getBranches().size())
            return;
        
        // Branch to the appropriate troubleshooting group
        Item.Branch br = currentItem.getBranches().get(branchNo);
        if (br.groupId >= 0) {
            new CoachingTask().execute(br.groupId, this, br.instructionId);
        } else if (br.instructionId > 0) {
            new BranchTask().execute(br.instructionId, this);
        }
    }
    
    /**
     * Start troubleshooting.
     */
    public void startTroubleshooting(int groupId) {
        currentlyTroubleshooting = true;
        new CoachingTask().execute(groupId, this);
        backButtonAction = new Runnable() {
            @Override
            public void run() {
                finishTroubleshooting();
            }
        };
    }
    
    /**
     * Finish troubleshooting. This allows the friend detection to take over the screen again.
     */
    public void finishTroubleshooting() {
        cancelAutoAdvance();
        if (currentItem != null) {
            currentItem.stop();
        }
        resetUI();
        currentlyTroubleshooting = false;
    }

    /**
     * Advance item or finish script. This is called when an audio item with delay == 0
     * finishes.
     */
    public void audioCompleted() {
        if (currentItem.isLast()) {
            finishTroubleshooting();
        } else {
            advanceItem(1);
        }
    }
    /**
     * Goes to the offset'th item after {@link currentItem} in {@link tsItems}.
     * @param offset The item offset number (negative numbers go to previous items)
     */
    public void advanceItem(int offset) {
        displayItemAt(currentItem.getItemNumber() + offset);
    }
    
    /**
     * Schedules an item to automatically advance. Does nothing if the item should not advance
     * automatically.
     * @param item The item which should advance
     */
    public void scheduleAutoAdvance(Item item) {
        cancelAutoAdvance();
        if (currentItem.getDelay() > 0) {
            Log.v(TAG, "Advancing to next item in " + currentItem.getDelay() + " seconds");
            autoAdvance = runner.schedule(new Advancer(currentItem), (long)(currentItem.getDelay()*1000), TimeUnit.MILLISECONDS);
        }
    }
    /**
     * Cancels the current auto-advance.
     */
    public void cancelAutoAdvance() {
        if (autoAdvance != null) {
            autoAdvance.cancel(true);
            autoAdvance = null;
        }
    }
    
    /** Reset the UI to its default display */
    public void resetUI() {
        clearUI();
        backButtonAction = null;
        if (commandGroupAdapter != null) {
            commandGroupContent.setVisibility(View.VISIBLE);
            setTitle(getString(R.string.app_name) + ": " + getString(R.string.scriptsHeading));
        } else {
            new CommandGroupTask(this).execute();
        }
    }
    
    /** Clear the UI */
    public void clearUI() {
        // Set all content to be gone
        friendContent.setVisibility(View.GONE);
        friendsContent.setVisibility(View.GONE);
        commandGroupContent.setVisibility(View.GONE);
        mainContent.setVisibility(View.GONE);
        mainMessage.setVisibility(View.GONE);
        mainPicture.setVisibility(View.GONE);
        playbackControls.setVisibility(View.GONE);
        branchControls.setVisibility(View.GONE);
        setTitle(getString(R.string.app_name));
    }
    
    /** Set the font sizes based on the settings */
    public void setFontSizes() {
        mainMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, AppSettings.getTextSize());
        mainTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, AppSettings.getTitleSize());
    }
    
    // TODO: Probably get rid of this in favor of the single "currentItem"
    public void setTroubleshootingItems(List<Item> items) {
        this.tsItems = items;
    }
    
    /**
     * Set troubleshooting item groups - we really only need to call this once.
     */
    public void setTroubleshootingItemGroups(final List<ItemGroup> groups) {
        commandGroupAdapter = new CommandGroupAdapter(this, groups);
        
        commandGroups.setAdapter(commandGroupAdapter);
        commandGroups.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startTroubleshooting(groups.get(position).getGroupId());
            }
        });
        
        resetUI();
    }
}
