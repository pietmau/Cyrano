/**
 * CLASS: DefaultSettingsTask
 *   This class will load the default settings in the database.
 *   It is to be called at the start of CyranoActivity.
 */
package com.cjcornell.samplebluetooth;

import java.io.IOException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cjcornell.samplebluetooth.data.AppSettings;
import com.cjcornell.samplebluetooth.data.DataStore;

import android.os.AsyncTask;
import android.util.Log;

public class DefaultSettingsTask extends AsyncTask<Void, Void, JSONArray> {
    private final static String TAG = "DefaultSettingsTask";
    private final static String SETTINGS_URL = WebLogin.SERVER_ROOT + "/defaultsettings";
  
    /**
     * This is executed when the asynchronous task is started. It will grab a JSONObject of default settings.
     */
    @Override
    protected JSONArray doInBackground(Void... arg0) {
        HttpClient httpClient = new DefaultHttpClient();
        String requestUrl = SETTINGS_URL + "/" + DataStore.getInstance().getBaseParameterString();
        Log.d(TAG, "Sending GET request at URL " + requestUrl);

        try {
            HttpGet httpGet = new HttpGet(requestUrl);
            JSONObject response = new JSONObject(httpClient.execute(httpGet, new BasicResponseHandler()));
                
            Log.d(TAG, "Successfully obtained settings");
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
     * This is executed after doInBackground method completes. It will set the default settings based
     * on the contents of the JSONObject returned by the doInBackground method.
     */
    @Override
    protected void onPostExecute(JSONArray arr) {
        // If there was a problem, the JSONArray is null, and we should simply return
        if (arr == null) {
            Log.v(TAG, "JSONArray is null.");
            return;
        }
        try {
            // Set all the settings as appropriate
            JSONObject settings = arr.getJSONObject(0);
            AppSettings.setDefaults(
                settings.getString("terseMode"),
                settings.getString("tsAudio"),
                settings.getString("graphicalMode"),
                settings.getString("friendFinder"),
                settings.getString("friendAudio"),
                settings.getString("textSize"),
                settings.getString("gpsTimeDelay"),
                settings.getString("maxFriends"),
                settings.getString("autoDisplayFriends"));
            Log.v(TAG, "Default settings loaded successfully.");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
