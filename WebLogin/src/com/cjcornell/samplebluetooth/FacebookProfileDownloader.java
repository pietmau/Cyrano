/**
 * CLASS: FacebookProfileDownloader
 *   This class is an asynchronous task used to download Facebook profile pictures
 */

package com.cjcornell.samplebluetooth;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
//Asynctas to just get profile pic will be extended soon. 


public class FacebookProfileDownloader extends AsyncTask<String, Void, Bitmap>
{

    private static final String TAG = "Facebook downloader:";

    /**
     * The task to run in the background - returns a Bitmap of the facebook picture
     */
    @Override
    protected Bitmap doInBackground(String... params) 
    {
        Bitmap picture = null;
        String id = params[0];
        URL imgURL= null;
        try
        {
            // The Facebook picture URL is based on the passed Facebook id
            imgURL = new URL("https://graph.facebook.com/"+id+"/picture?type=large");
            picture = BitmapFactory.decodeStream(imgURL.openConnection().getInputStream());
        }
        catch (MalformedURLException e) {
            Log.e(TAG, "Error with URL or Facebook userID.");
        }
        catch (IOException e) {
            Log.e(TAG, "Error getting Profile Picture.");
        }
        return picture;
    }
    
}
