/**
 * CLASS: CommandGroupTask
 *   This class is an asynchronous task used for retrieving the command groups for the scripts page
 */

package com.cjcornell.samplebluetooth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cjcornell.samplebluetooth.data.DataStore;

import android.os.AsyncTask;
import android.util.Log;

public class CommandGroupTask extends AsyncTask<Void, Void, JSONArray> {
    private final static String TAG = "CommandGroupTask";
    private final static String COMMAND_URL = WebLogin.SERVER_ROOT + "/commandgroups";
    private CyranoActivity activity;
    
    /**
     * Constructor - simply used to set the activity attribute
     * @param activity: The CyranoActivity activity
     */
    public CommandGroupTask(CyranoActivity activity) {
        this.activity = activity;
    }
    
    /**
     * This is the task to execute in the background. In this case, it will grab the specified command group.
     */
    @Override
    protected JSONArray doInBackground(Void... arg0) {
        HttpClient httpClient = new DefaultHttpClient();
        String requestUrl = COMMAND_URL + "/" + DataStore.getInstance().getBaseParameterString();
        Log.d(TAG, "Sending GET request at URL " + requestUrl);

        try {
            HttpGet httpGet = new HttpGet(requestUrl);
            JSONObject response = new JSONObject(httpClient.execute(httpGet, new BasicResponseHandler()));
                
            Log.d(TAG, "Successfully obtained instruction set for group");
            if (response.has("body")) {
                return response.getJSONArray("body");
            } else {
                return null;
            }
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        } catch (ClientProtocolException e) {
            Log.e(TAG, e.toString());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * This will parse the results of the JSONObject retrieved from the doInBackground Method.
     */
    @Override
    protected void onPostExecute(JSONArray groups) {
        // If the JSONArray is null, there are no groups to parse
        if (groups == null) return;
        
        // Go through each item in the JSONObject and add them to an ItemGroup ArrayList
        List<ItemGroup> parsedGroups = new ArrayList<ItemGroup>();
        for (int i = 0; i < groups.length(); i++) {
            try {
                parsedGroups.add(new ItemGroup(groups.getJSONObject(i)));
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        }
        // Set the troubleshooting items and remove the splash screen
        activity.setTroubleshootingItemGroups(parsedGroups);
        activity.removeSplashScreen();
    }
}
