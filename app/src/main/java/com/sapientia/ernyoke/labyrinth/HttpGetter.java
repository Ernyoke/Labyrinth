package com.sapientia.ernyoke.labyrinth;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Ernyoke on 11/10/2014.
 */
public class HttpGetter extends AsyncTask<String, Void, String> {

    private ProgressBar rolling;

    public HttpGetter(ProgressBar rolling) {
        this.rolling = rolling;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        rolling.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        rolling.setVisibility(View.INVISIBLE);
    }

    @Override
    protected String doInBackground(String... urls) {

        String urlString = urls[0];
        try {
//            Log.d(TAG, urlString);
            HttpURLConnection con = null;
            URL url = new URL(urlString);
            con = (HttpURLConnection) url.openConnection();
            Log.d(Constants.TAG, "open connection");
            con.setReadTimeout(10000 /* milliseconds */);
            con.setConnectTimeout(15000 /* milliseconds */);
            con.setRequestMethod("GET");
            con.setDoInput(true);
// Start the query
            con.connect();
            Log.d(Constants.TAG, "Connected");
// Check if task has been interrupted
            if (Thread.interrupted())
                throw new InterruptedException();
// Read results from the query
            Scanner scanner = new Scanner(con.getInputStream());
            String result = scanner.nextLine();
            Log.d(Constants.TAG, "Result: " + result);
            scanner.close();
            con.disconnect();
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
