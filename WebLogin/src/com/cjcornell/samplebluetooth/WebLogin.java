/**
 * CLASS: SettingsActivity
 *   This is the login page for Cyrano
 */

package com.cjcornell.samplebluetooth;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import com.cjcornell.samplebluetooth.data.DataStore;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.Request;
import com.facebook.Session;
import com.facebook.Response;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.OnErrorListener;

public class WebLogin extends Activity
{
    private static final String TAG = "WebLogin";
    private static final int FACEBOOK_REQUEST_CODE = 64206;
    public static final String SERVER_ROOT = "http://cyrano.cjcornell.com/REST/index.php";
    public static final String LOGIN_URL = SERVER_ROOT + "/auth";

    ProgressDialog dialog;
    
    private Request.GraphUserCallback requestCallback = new Request.GraphUserCallback() {
        
        @Override
        public void onCompleted(GraphUser user, Response response) {
            if (user != null) { 
                Friend me = new Friend(user.getId(), user.getFirstName(), user.getLastName(),
                        user.asMap().get("email").toString());
                DataStore.getInstance().setMe(me);
                WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = manager.getConnectionInfo();
                String address = info.getMacAddress();
                DataStore.getInstance().setMacAddress(address);
                login();
            }
        }
    };
    
    private void sendFacebookMeRequest(Session session) {
        if (session != null && session.isOpened()) {
            final String accessToken = session.getAccessToken();
            DataStore.getInstance().setAccessToken(accessToken);
            Request.newMeRequest(session, requestCallback).executeAsync();
        }
    }

    public void onCreate(Bundle savedState)
    {
        super.onCreate(savedState);
        
        //Sets view to weblogin
        setContentView(R.layout.weblogin);
        
        if (dialog == null) {
            dialog = new ProgressDialog(this);
            dialog.setMessage(getString(R.string.loggingIn));
        }
        
        try {
            generateHash();
        } catch (NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e.getMessage());
        }
        
        LoginButton fbAuthButton = (LoginButton) findViewById(R.id.fbAuthButton);
        fbAuthButton.setOnErrorListener(new OnErrorListener() {
            
            @Override
            public void onError(FacebookException error) {
                Log.i(TAG, "Error " + error.getMessage());
            }
        });
        
        // Specify the FB permissions our app will have
        fbAuthButton.setReadPermissions(Arrays.asList("public_profile", "email","user_friends"));// "basic_info"
        
        // Specify logic to execute after logging into Facebook
        fbAuthButton.setSessionStatusCallback(new Session.StatusCallback() {
            
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                sendFacebookMeRequest(session);
            }
       });
        
       // Log the user into Cyrano if they have an active session
       Session session = Session.getActiveSession();
       sendFacebookMeRequest(session);
    }
  
    /**
     * Receives the registration results on return from the RegisterActivity.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FACEBOOK_REQUEST_CODE && resultCode == RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();

        if(dialog != null) {
            dialog.dismiss();
        }
        dialog = null;
    }
    
    @Override
    public void onResume() {
        super.onResume();

        if (dialog == null) {
            dialog = new ProgressDialog(this);
            dialog.setMessage(getString(R.string.loggingIn));
        }
    }


    /**
     * Logs in to the server. This class is needed to avoid too much network activity (pausing)
     * on the UI thread.
     */
    private class LoginTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            if (dialog != null) {
                dialog.show();
            }
        }

        @Override
        protected String doInBackground(String... loginInfo) {
            String accessToken = loginInfo[0];
            String userId = loginInfo[1];
            String requestUrl = LOGIN_URL + "/" + accessToken + "/" + userId;

            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet request = new HttpGet(requestUrl);
                String response = httpClient.execute(request, new BasicResponseHandler());
                JSONObject parsedResponse = new JSONObject(response);

                return parsedResponse.getJSONObject("success").getString("code");
            }
            catch (HttpResponseException e) {
                Log.d(TAG, "Could not log in with accessToken=\""+accessToken+"\" userId=\""+userId+"\" ("
                        + e.getStatusCode() + ")");
            }
            catch (JSONException e) {
                Log.w(TAG, e);
            }
            catch (IOException e) {
                Log.w(TAG, e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            if (result == null) {
                Toast toast = Toast.makeText(getApplicationContext(), "Incorrect login information.", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 0);
                toast.show();
            } else {
                successfulLogin();
            }
        }
    }


    /**
     * Log in to the app. Asynchronously validates the username and password and, if successful, transfers control
     * to the main activity.
     * 
     * @param username potential username
     * @param password potential password
     */
    private void login() {
        Log.v(TAG, "Logging in using Facebook Access Token...");
        new LoginTask().execute(DataStore.getInstance().getAccessToken(), DataStore.getInstance().getMe().getId());
    }

    /**
     * Transfers to the main activity.
     */
    private void successfulLogin() {
        Intent i = new Intent(this, CyranoActivity.class);
        startActivity(i);
        finish(); // XXX: Should we finish here? Or not?
    }
    
    /**
     * Present for debugging purposes only, to get the appropriate hash
     * for Facebook API settings.
     */
    private void generateHash() throws NameNotFoundException, NoSuchAlgorithmException {
        PackageInfo info = getPackageManager().getPackageInfo(getPackageName(),  PackageManager.GET_SIGNATURES);

        for (Signature signature : info.signatures)
            {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
    }
}
