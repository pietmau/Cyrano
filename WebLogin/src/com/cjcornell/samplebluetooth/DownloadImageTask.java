/** CLASS: DownloadImageTask
 *   This class is used to download images used in troubleshooting.
 */

package com.cjcornell.samplebluetooth;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    // The image variable to download to
    private ImageView image;
    
    // The constructor simply sets the  image
    public DownloadImageTask(ImageView image) {
        this.image = image;
    }
    
    // Download the image
    protected Bitmap doInBackground(String... urls) {
        String url = urls[0];
        Bitmap bmImage = null;
        try {
            InputStream in = new URL(url).openStream();
            bmImage = BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bmImage;
    }
    
    // Finally, set the bitmap
    protected void onPostExecute(Bitmap result) {
        image.setImageBitmap(result);
    }
}
