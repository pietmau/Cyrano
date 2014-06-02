/**
 * CLASS: SettingsActivity
 *   This is the settings page for Cyrano.
 */


package com.cjcornell.samplebluetooth;

import java.text.DecimalFormat;

import com.cjcornell.samplebluetooth.data.AppSettings;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SettingsActivity extends Activity {
    private final String TAG = "Settings Activity";
    
    public static CyranoActivity activity;
    private static DecimalFormat df = new DecimalFormat("0");
    
    // GUI elements
    private static ToggleButton terseModeButton, tsAudioButton, graphicalModeButton, friendFinderButton, friendAudioButton, autoDisplayFriendsButton;
    public static Spinner fontSizeSpinner;
    private static TextView gpsDelayText;
    private static TextView maxFriendsText;
    
    /**
     * Called on initial creation of the page
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Display version info
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView)findViewById(R.id.versionInfo)).setText(
                    getString(R.string.versionText, info.versionName));
        } catch (NameNotFoundException e) {
            Log.e(TAG, e.toString());
            ((TextView)findViewById(R.id.versionInfo)).setVisibility(View.GONE);
        }
        
        // Load the GUI components
        terseModeButton = (ToggleButton)findViewById(R.id.terseModeButton);
        tsAudioButton = (ToggleButton)findViewById(R.id.tsAudioButton);
        graphicalModeButton = (ToggleButton)findViewById(R.id.graphicalModeButton);
        friendFinderButton = (ToggleButton)findViewById(R.id.friendFinderButton);
        friendAudioButton = (ToggleButton)findViewById(R.id.friendAudioButton);
        autoDisplayFriendsButton = (ToggleButton)findViewById(R.id.autoDisplayFriendsButton); 
        maxFriendsText = (EditText)findViewById(R.id.maxFriendsText);
        
        gpsDelayText = (EditText)findViewById(R.id.gpsDelayText);
        gpsDelayText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); // Limit to one decimal point
        gpsDelayText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // Remove the listener to avoid infinite recursion - we may be changing text here!
                gpsDelayText.removeTextChangedListener(this);
                
                // Get the text from gpsDelayText
                String text = gpsDelayText.getText().toString();
                
                // Split the string at any decimal places
                String[] textArray = text.split("\\.");
                
                // Simply cut the second element, if any, to one place
                if (textArray.length == 2 && textArray[1].length() > 1) {
                    textArray[1] = textArray[1].substring(0, 1);
                    Log.v(TAG, "Ignored an input, the tenths place should still be: " + textArray[1]);
                    
                    // Set the new text - the append is to maintain the cursor position
                    gpsDelayText.setText("");
                    gpsDelayText.append(textArray[0] + "." + textArray[1]);
                }
                
                // If this wasn't the case, check if there are 5 characters, and the last is a decimal
                else if (text.length() == 5 && text.charAt(4) == '.') {
                    // Do not allow the decimal to be there
                    gpsDelayText.setText("");
                    gpsDelayText.append(text.substring(0, 4));
                }
                
                // Put the listener back, as we are finished changing text.
                gpsDelayText.addTextChangedListener(this);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int count) {}
        });
        
        fontSizeSpinner = (Spinner)findViewById(R.id.fontSizeSpinner);
        ArrayAdapter<CharSequence> fontSizeAdapter = ArrayAdapter.createFromResource(
        		this, R.array.fontSizes, R.layout.spinner_layout);
        fontSizeAdapter.setDropDownViewResource(R.layout.spinner_layout);
        fontSizeSpinner.setAdapter(fontSizeAdapter);
        
        // Load all the settings
        terseModeButton.setChecked(AppSettings.terseMode);
        tsAudioButton.setChecked(AppSettings.tsAudio);
        graphicalModeButton.setChecked(AppSettings.graphicalMode);
        friendFinderButton.setChecked(AppSettings.friendFinder);
        friendAudioButton.setChecked(AppSettings.friendAudio);
        autoDisplayFriendsButton.setChecked(AppSettings.autoDisplayFriends);
        gpsDelayText.setText(AppSettings.gpsTimeDelay + "");
        maxFriendsText.setText(df.format(AppSettings.maxFriends));
        
        // Set the font size based on the text size setting
        if (AppSettings.textSize < 1) {
            fontSizeSpinner.setSelection(0);
        } else if (AppSettings.textSize == 1) {
            fontSizeSpinner.setSelection(1);
        } else {
            fontSizeSpinner.setSelection(2);
        }
    }

    /**
     * Called when the options menu is created
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }
    
    /**
     * Called when an item in the options menu is clicked. This will finish the SettingsActivity and
     * perform the action corresponding to CyranoActivity's onOptionsItemSelected(Item) method.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return activity.onOptionsItemSelected(item);
    }
    
    /**
     * Called when the settings menu is clicked
     * @param view
     */
    public void saveSettingsButtonClicked(View view) {
        Log.v(TAG, "Save settings button clicked");
        
        // Save all the settings to the AppSettings variables
        AppSettings.terseMode = terseModeButton.isChecked();
        AppSettings.tsAudio = tsAudioButton.isChecked();
        AppSettings.graphicalMode = graphicalModeButton.isChecked();
        AppSettings.friendFinder = friendFinderButton.isChecked();
        AppSettings.friendAudio = friendAudioButton.isChecked();
        AppSettings.textSize = fontSizeSpinner.getSelectedItemPosition();
        AppSettings.maxFriends = Integer.parseInt(maxFriendsText.getText().toString());
        AppSettings.autoDisplayFriends = autoDisplayFriendsButton.isChecked();
        
        // We don't want any GPS upload times to be below 30 seconds - to prevent too many calculations
        try {
            double gpsTime = Double.parseDouble(gpsDelayText.getText().toString().trim());
            Log.v(TAG, "Time delay: " + gpsTime);
            if (gpsTime < 0.5) {
                Log.v(TAG, "Time delay reset to 0.5 mins from " + gpsTime + " mins");
                gpsTime = 0.5;
            }
            AppSettings.gpsTimeDelay = gpsTime;
            
            // Restart the GPS scheduler, so it can run with the new time delay
            Intent i = new Intent(FriendFinderService.RESTART_GPS);
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        } catch(NumberFormatException ex) {
            Log.v(TAG, "Invalid format for GPS text");
        }
        
        // Set the font sizes appropriately
        activity.setFontSizes();
        
        // Save the settings to the local data file
        AppSettings.saveSettings(this);
        
        // Exit the settings activity
        finish();
    }
}
