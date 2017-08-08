package com.byteshaft.healthvideo.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.database.DatabaseHelpers;
import com.byteshaft.healthvideo.serializers.NurseDetails;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by s9iper1 on 8/8/17.
 */

public class InternetBroadCastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isNetworkAvailable(context)) {
            new CheckInternet().execute();
        }

    }

    private boolean isNetworkAvailable(final Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public boolean isInternetWorking() {
        boolean success = false;
        try {
            URL url = new URL("https://google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.connect();
            success = connection.getResponseCode() == 200;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return success;
    }

    private class CheckInternet extends AsyncTask<Void, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            return isInternetWorking();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            DatabaseHelpers databaseHelpers = new DatabaseHelpers(AppGlobals.getContext());
            ArrayList<NurseDetails> nurseDetailses = databaseHelpers.getAllNursesDetails();
            for (NurseDetails nurseDetails : nurseDetailses)
            Log.e(AppGlobals.getLogTag(getClass()), "location " + nurseDetails.getLocation());

        }
    }
}
