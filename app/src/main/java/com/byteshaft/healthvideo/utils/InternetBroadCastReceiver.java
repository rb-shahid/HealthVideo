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
import com.byteshaft.requests.HttpRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by s9iper1 on 8/8/17.
 */

public class InternetBroadCastReceiver extends BroadcastReceiver {

    private static Context mContext;

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.e("TAG", "Internet changed");
        this.mContext = context;
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("TAG", "Internet State" + isNetworkAvailable(context));
                if (isNetworkAvailable(context)) {
                    new CheckInternet().execute();
                }
            }
        }, 5000);

    }

    public static boolean isNetworkAvailable(final Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean isInternetWorking() {
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

    public static class CheckInternet extends AsyncTask<Void, Boolean, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            return isInternetWorking();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            DatabaseHelpers databaseHelpers = new DatabaseHelpers(AppGlobals.getContext());
            ArrayList<NurseDetails> nurseDetails = databaseHelpers.getAllNursesDetails();
            JSONArray jsonArray = new JSONArray();
            ArrayList<Integer> toBeDelete = new ArrayList<>();
            for (NurseDetails nurseDetail : nurseDetails) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("deviceid", nurseDetail.getDeviceId());
                    jsonObject.put("fileid", nurseDetail.getFileId());
                    jsonObject.put("location", nurseDetail.getLocation());
                    jsonObject.put("timestamp", nurseDetail.getTimeStamp());
                    jsonObject.put("is_current_location", nurseDetail.isCurrentLocation());
                    if (nurseDetail.getId() != -1) {
                        toBeDelete.add(nurseDetail.getId());
                    }
                    jsonArray.put(jsonObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            sendUsageUpdate(jsonArray, toBeDelete);

        }

    }

    private static void sendUsageUpdate(JSONArray jsonArray, final ArrayList<Integer> toBeDelete) {
        HttpRequest request = new HttpRequest(AppGlobals.getContext());
        request.setOnReadyStateChangeListener(new HttpRequest.OnReadyStateChangeListener() {
            @Override
            public void onReadyStateChange(HttpRequest request, int readyState) {
                switch (readyState) {
                    case HttpRequest.STATE_DONE:
                        switch (request.getStatus()) {
                            case HttpURLConnection.HTTP_OK:
                                Log.i("TAG", request.getResponseText());
                                DatabaseHelpers databaseHelpers = new DatabaseHelpers(AppGlobals.getContext());
                                for (Integer integer: toBeDelete) {
                                    databaseHelpers.deleteNurse(String.valueOf(integer));
                                }

                                Log.i("TAG",  "database "+databaseHelpers.getAllNursesDetails().size());

                                break;
                            case HttpURLConnection.HTTP_BAD_REQUEST:
                                Log.i("TAG", request.getResponseText());
                                break;

                        }
                }

            }
        });
        request.setOnErrorListener(new HttpRequest.OnErrorListener() {
            @Override
            public void onError(HttpRequest request, int readyState, short error, Exception exception) {

            }
        });
        request.open("POST", String.format("%susagedata", AppGlobals.BASE_URL));
        request.setRequestHeader("authorization",
                AppGlobals.getStringFromSharedPreferences(AppGlobals.KEY_TOKEN));
        request.send(jsonArray.toString());
    }
}
