/**
 * CLASS: AppSettings
 *   This class provides methods to save/load local settings
 */

package com.cjcornell.samplebluetooth.data;

import java.io.*;
import java.text.DecimalFormat;

import com.cjcornell.samplebluetooth.CyranoActivity;
import com.cjcornell.samplebluetooth.DefaultSettingsTask;

import android.content.Context;
import android.util.Log;

public class AppSettings {
    private static final String TAG = "AppSettings";
    
    private static final String SETTINGS_FILE = "app_settings.dat";
    
    // true for on; false for off
    public static Boolean terseMode, tsAudio, graphicalMode, friendFinder, friendAudio, autoDisplayFriends;
    
    // <= 0: small; 1: medium; > 1: large
    public static Integer textSize;
    
    // In seconds
    public static Double gpsTimeDelay;
    
    // Max # of friends to announce
    public static Integer maxFriends;
    
    // # seconds to pause between announcing friends
    public static Integer pauseLength = 2;
    
    // Format to display coordinate precision
    public static DecimalFormat formatter = new DecimalFormat("#0.0");
    
    /**
     * Initialize the settings based on the settings file. If there are issues with the file, it will
     * set all variables to a local default value, then attempt to load the global defaults from the database.
     */
    public static void initSettings(CyranoActivity activity) {
        // A flag to see if anything went wrong
        boolean errorFlag = false;
        
        try {
            FileInputStream fis = activity.openFileInput(SETTINGS_FILE);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader file = new BufferedReader(isr);
            
            // Load settings from the file
            terseMode = Boolean.parseBoolean(file.readLine());
            tsAudio = Boolean.parseBoolean(file.readLine());
            graphicalMode = Boolean.parseBoolean(file.readLine());
            friendFinder = Boolean.parseBoolean(file.readLine());
            friendAudio = Boolean.parseBoolean(file.readLine());
            
            textSize = Integer.parseInt(file.readLine());
            gpsTimeDelay = Double.parseDouble(file.readLine());
            
            maxFriends = Integer.parseInt(file.readLine());
            autoDisplayFriends = Boolean.parseBoolean(file.readLine()); 
            
            // Close the file
            file.close();
        } catch(IOException e) {
            e.printStackTrace(); 
            errorFlag = true;
        } catch(NumberFormatException e) {
            e.printStackTrace();
            errorFlag = true;
        } catch(NullPointerException e) {
            e.printStackTrace();
            errorFlag = true;
        }
        
        /**
         * If there was an error, two things must happen
         *   1. Set all variables to local defaults - this ensures that the settings at least have SOME default value, should the
         *      DefaultSettingsTask not complete before a setting is needed.
         *   2. Start the DefaultSettingsTask to grab all the global defaults from the database
         */
        if (errorFlag) {
            Log.v(TAG, "Error on getting local settings - grabbing defaults");
            setDefaults();
            new DefaultSettingsTask().execute();
        }
    }
    
    /**
     * Save the settings to a hidden data file, so we can store local settings
     */
    public static void saveSettings(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(SETTINGS_FILE, Context.MODE_PRIVATE);
            OutputStreamWriter osr = new OutputStreamWriter(fos);
            BufferedWriter file = new BufferedWriter(osr);
            file.write(terseMode + "\n");
            file.write(tsAudio + "\n");
            file.write(graphicalMode + "\n");
            file.write(friendFinder + "\n");
            file.write(friendAudio + "\n");
            file.write(textSize + "\n");
            file.write(gpsTimeDelay + "\n");
            file.write(maxFriends + "\n");
            file.write(autoDisplayFriends + "\n");
            file.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Set the default values to local defaults
     */
    public static void setDefaults() {
        terseMode = false;
        tsAudio = true;
        graphicalMode = true;
        friendFinder = true;
        friendAudio = true;
        textSize = 1;
        gpsTimeDelay = 2.0;
        maxFriends = 3;
        autoDisplayFriends = true;
    }
    
    /**
     * Set the default values based on given parameters - this is meant to be used with the
     * results of a database call.
     */
    public static void setDefaults(String terseModeStr, String tsAudioStr, String graphicalModeStr, String friendFinderStr,
            String friendAudioStr, String textSizeStr, String gpsTimeDelayStr, String maxFriendsStr, String autoDisplayFriendsStr) {
        terseMode = !terseModeStr.equals("0");
        tsAudio = !tsAudioStr.equals("0");
        graphicalMode = !graphicalModeStr.equals("0");
        friendFinder = !friendFinderStr.equals("0");
        friendAudio = !friendAudioStr.equals("0");
        textSize = Integer.parseInt(textSizeStr);
        
        if (Double.parseDouble(gpsTimeDelayStr) < 0.5) {
            gpsTimeDelay = 0.5;
        } else {
            gpsTimeDelay = Double.parseDouble(gpsTimeDelayStr);
        }
        
        maxFriends = Integer.parseInt(maxFriendsStr);
        autoDisplayFriends = !autoDisplayFriendsStr.equals("0");
    }
    
    /**
     * Get the text size value for titles, based on the current settings
     */
    public static int getTitleSize() {
        // Small
        if (textSize < 1) {
            return 14;
        // Medium
        } else if (textSize == 1) {
            return 24;
        // Large
        } else {
            return 36;
        }
    }
    
    /**
     * Get the text size value for descriptive text, based on the current settings
     */
    public static int getTextSize() {
        // Small
        if (textSize < 1) {
            return 10;
        // Medium
        } else if (textSize == 1) {
            return 14;
        // Large
        } else {
            return 24;
        }
    }
}
