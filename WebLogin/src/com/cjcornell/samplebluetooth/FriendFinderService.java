/**
 * CLASS: FriendFinderService
 *   This is the service used to find nearby friends.
 */

package com.cjcornell.samplebluetooth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.cjcornell.samplebluetooth.data.AppSettings;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class FriendFinderService extends Service implements LocationListener {
    public static final String TAG = "FriendFinderService";
    public static final String RESTART_GPS = "com.cjcornell.samplebluetooth.RESTART_GPS";
    public static final String SHUTDOWN_FFS = "com.cjcornell.samplebluetooth.SHUTDOWN_FFS";
    public static List<Friend> friendList = new ArrayList<Friend>();
    public static final int NOTIFICATION_ID = 123123;
    
    private ScheduledThreadPoolExecutor gpsRunner;
    private ScheduledFuture<?> gpsFuture;
    private Location location = null;
    private long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; //10 meter default
    private long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;  //1 minute?
    
    // This receiver detects messages to restart the GPS service and to shut down the FriendFinderService
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RESTART_GPS.equals(intent.getAction())) {
                Log.v(TAG, "Got message to restart GPS");
                scheduleGPS();
            } else if (SHUTDOWN_FFS.equals(intent.getAction())) {
                Log.v(TAG, "Shutting down FriendFinderService");
                LocalBroadcastManager.getInstance(FriendFinderService.this).unregisterReceiver(receiver);
                cancelGPS();
                gpsRunner.shutdownNow();
                
                stopSelf();
            }
        }
    };
    
    /** Called on creation - used to set up everything */
    @Override
    public void onCreate() {
        super.onCreate();
        
        // For some reason, we call this on force close? Here's a hacky gross band-aid fix
        if (AppSettings.gpsTimeDelay == null) {
            stopSelf();
            return;
        }
        
        // Set up the thrad pools and start the GPS service */
        gpsRunner = new ScheduledThreadPoolExecutor(1);
        gpsRunner.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        gpsRunner.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduleGPS(true);
        
        IntentFilter iff = new IntentFilter(RESTART_GPS);
        iff.addAction(SHUTDOWN_FFS);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, iff);
        
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(getString(R.string.notificationTitle))
                .setContentText(getString(R.string.notificationText));
        
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, builder.build());
        
        Log.v(TAG, "Created FriendFinderService");
    }
    
    /** Destroys the FriendFinderService */
    @Override
    public void onDestroy() {
        Log.v(TAG, "Destroying FriendFinderService");
        //This line removes the icon from the notification bar
        ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
        super.onDestroy();
    }
    
    /** Used to schedule the GPS service, but not start it right away. */
    public void scheduleGPS() {
        scheduleGPS(false);
    }
    
    /** 
     * scheduleGPS
     *   Used to schedule the GPS service
     *   @param startRightAway: Whether to start the service right away
     */
    public void scheduleGPS(boolean startRightAway) {
        // Get the GPS provider
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        String bestProvider = lm.getBestProvider(new Criteria(), true);
        location = lm.getLastKnownLocation(bestProvider);
		
        if (lm.getProvider(LocationManager.GPS_PROVIDER) != null) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        }
        if (lm.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        }
        
        if (gpsFuture != null) {
            gpsFuture.cancel(true);
        }
        
        // Schedule the GPS update. The delay is derived from the current application setting "gpsTimeDelay"
        long delay = (long)(AppSettings.gpsTimeDelay * 1000 * 60); // This is in minutes * 1000 due to the "* 60"
        gpsFuture = gpsRunner.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Starting GPSAsyncTask");
                if (location != null) {
                    new GPSAsyncTask(FriendFinderService.this).execute(location);
                } else {
                    Log.w(TAG, "No location found");
                }
            }
        }, startRightAway && location != null ? 0 : delay, delay, TimeUnit.MILLISECONDS);
    }
    
    /** Cancel the GPS updater */
    public void cancelGPS() {
        if (gpsFuture != null) {
            gpsFuture.cancel(true);
            gpsFuture = null;
        }
        
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        lm.removeUpdates(this);
    }
    
    /** 
     * gotFriends
     *   This method should be called when we have retrieved friends. It will announce friends nearby 
     *   and display a list of them in the CyranoActivity.
     */
    public void gotFriends(List<Friend> friends) {
        friendList = friends;
        
        // Announce friends, if the setting is enabled
        if (AppSettings.friendAudio) {
            announceMultipleFriends(friends);
        }
        
        // Display the friends in the CyranoActivity
        Intent i = new Intent(CyranoActivity.DISPLAY_FRIENDS);
        i.putExtra("friends", friends.toArray(new Friend[friends.size()]));
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }
    
    /**
     * announceMultipleFriends
     *   Announce multiple friends using textToSpeech
     *   
     * @param friends: The friends to announce
     */
    private void announceMultipleFriends(final List<Friend> friends) {
        int numFriends = Math.min(AppSettings.maxFriends, friends.size());
        List<String> phrases = new ArrayList<String>();
        List<Integer> pauses = new ArrayList<Integer>();
        if (friends.size() == 1) {
            phrases.add(getString(R.string.singleFriendsMessage));
        } else {
            phrases.add(getString(R.string.multipleFriendsMessage, friends.size()));
        }
        pauses.add(AppSettings.pauseLength);
        for (int i = 0; i < numFriends; i++) {
            phrases.add(getString(R.string.singleFriendMessage, friends.get(i).getName()));
            pauses.add(1);
        }
        
        AudioMethods.playInstructions(this, phrases, pauses);
    }
    
    /**
     * onLocationChanged
     *   This method is called when your GPS location is changed
     */
    @Override
    public void onLocationChanged(Location location) {
        if (this.location == null) {
            // If it's the first location we've gotten, run off the AsyncTask right away.
            new GPSAsyncTask(FriendFinderService.this).execute(location);
        }
        this.location = location;
    }
    @Override
    public void onProviderDisabled(String arg0) {}
    @Override
    public void onProviderEnabled(String arg0) {}
    @Override
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
    
    @Override
    public IBinder onBind(Intent intent) {
        // We don't support binding.
        return null;
    }
}
