package com.apollo.apollo;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;

// Test comment in NavThread

import javax.net.ssl.HttpsURLConnection;

public class NavThread extends AsyncTask<Void, Void, String> {
    // WeakReference prevents memory leaks, allows object to get garbage collected
    private WeakReference<ProgressBar> weakProgressBar;
    private WeakReference<TextView> weakStatus;
    private WeakReference<EditText> weakText;
    private String origin;

    public NavThread(ProgressBar progressBar, TextView status, EditText text, String origin) {
        this.weakProgressBar = new WeakReference<>(progressBar);
        this.weakStatus = new WeakReference<>(status);
        this.weakText = new WeakReference<>(text);
        this.origin = origin;
    }

    protected void onPreExecute() {
        weakProgressBar.get().setVisibility(View.VISIBLE);
        weakStatus.get().setText("Retrieving Navigation Info");
    }

    protected String doInBackground(Void... urls) {
        String dest = weakText.get().getText().toString();
        String key = "AIzaSyBCw85fEIJI0b13hbATj32v0Z00Z1sw4_U";
        String base = "https://maps.googleapis.com/maps/api/directions/json?";

        try {
            URL url = new URL(base + "origin=" + origin + "&destination=" +
                        dest + "&key=" + key);

            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            try {
                BufferedReader bufferedReader =
                        new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }

                bufferedReader.close();
                return stringBuilder.toString();
            }
            finally {
                urlConnection.disconnect();
            }

        }
        catch (Exception e) {
            Log.e("ERROR", e.getMessage(), e);
            return null;
        }
    }

    protected void onPostExecute(String response) {
        if (response == null) {
            response = "There was an error";
        }

        weakProgressBar.get().setVisibility(View.GONE);
        weakStatus.get().setText("");
        Log.d("NAV", response);
    }
}
