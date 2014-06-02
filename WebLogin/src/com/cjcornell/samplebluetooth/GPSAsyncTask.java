/**
 * CLASS: GPSAsyncTask
 *   This asynchronous task is used to update GPS coordinates
 */

package com.cjcornell.samplebluetooth;

import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.json.JSONArray;

import com.cjcornell.samplebluetooth.data.AppSettings;
import com.cjcornell.samplebluetooth.data.DataStore;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;


public class GPSAsyncTask extends AsyncTask<Location, Void, ArrayList<Friend>>
{
    public String locationURL = WebLogin.SERVER_ROOT + "/location";
    
    private final String TAG = "GPSListener";
    private final long MAX_DISTANCE = 1000;
    
    private FriendFinderService ffs;
    
    /** Constructor - set the friend finder service attribute */
    public GPSAsyncTask(FriendFinderService service) {
        ffs = service;
    }
    
    /**
     * This method is the one to be executed in the background - it will get the user's GPS coordinates and
     * update the database with them.
     */
    @Override
    protected ArrayList<Friend> doInBackground(Location... params) 
    {
        Location loc = params[0];
        Log.d(TAG, "Latitude: " + loc.getLatitude());
        Log.d(TAG, "Longitude: " + loc.getLongitude());
        
        String requestURL = locationURL + "/" + DataStore.getInstance().getBaseParameterString() + "/" +
                loc.getLatitude() + "/" + loc.getLongitude();
        HttpClient client = new DefaultHttpClient();
        Log.d(TAG, "Sending the Put Request");
        try 
        {
            HttpPut httpPut = new HttpPut(requestURL);
            HttpResponse response = client.execute(httpPut);
            if(response.getStatusLine().getStatusCode() == 200)
            {
                Log.d(TAG,"Successfully uploaded coordinates." );
            }  
            else 
            {
                Log.e(TAG, "Could not upload coordinates.");
            }
            
            /* Read the response fully before sending another request */
            response.getEntity().consumeContent();
            
            requestURL = requestURL + "/" + MAX_DISTANCE;
            
            // Only ask the server for friends if the friend finder is on
            if (AppSettings.friendFinder) {
                Log.d(TAG, "Asking server for nearby friends.");
                HttpGet httpGet = new HttpGet(requestURL);
                
                // Code copied from FindFriendsTask begins here
                String response2 = client.execute(httpGet, new BasicResponseHandler());
                JSONObject parsedResponse = new JSONObject(response2);
                if (parsedResponse.has("body")) 
                {
                    ArrayList<Friend> friends = new ArrayList<Friend>(0);
                    JSONArray userIds = parsedResponse.getJSONArray("body");
                    for (int index = 0; index < userIds.length(); index++) 
                    {
                        JSONObject friendData = userIds.getJSONObject(index);
                        Friend f = new Friend(friendData.getString("id"), friendData.getString("first_name"), 
                                friendData.getString("last_name"), friendData.getString("email"), 
                                friendData.getDouble("distance"), friendData.getDouble("latitude"), 
                                friendData.getDouble("longitude"), friendData.getString("details1"),
                                friendData.getString("details2"), friendData.getString("details3"));
                        Log.d(TAG, "User " + f + " is nearby.");
                        friends.add(f);
                    }
                    return friends;
                }
                else 
                {
                    Log.e(TAG, "Error checking users near me.");
                }
            } else {
                Log.v(TAG, "Friend finder off - not asking server for nearby friends");
            }
        }
        
        catch (Exception e)
        {
            Log.e(TAG, "Error contacting server.");
        }
        
        return null;
    }
    
    /**
     * This method is executed after the doInBackground method finishes. It will display any
     * found friends, if the settings permit it.
     */ 
    @Override
    protected void onPostExecute(ArrayList<Friend> friends) {
        // Do not display anything if the friend finder setting is off
        if (AppSettings.friendFinder) {
            if (friends != null && friends.size() > 0) {
                ffs.gotFriends(friends);
            }
            Log.v(TAG, "Displayed found friends");
        } else {
            Log.v(TAG, "Friend finder off - not displaying any friends");
        }
    }
}