package com.byteshaft.healthvideo.fragments;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.MainActivity;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.interfaces.OnLocationAcquired;
import com.byteshaft.healthvideo.serializers.DataFile;
import com.byteshaft.healthvideo.utils.GetLocation;
import com.byteshaft.healthvideo.utils.Helpers;
import com.byteshaft.requests.HttpRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by s9iper1 on 6/14/17.
 */

public class RemoteFilesFragment extends Fragment implements HttpRequest.OnReadyStateChangeListener,
        HttpRequest.OnErrorListener, AdapterView.OnItemClickListener, OnLocationAcquired{

    private View mBaseView;
    private ListView mListView;
    private ArrayList<DataFile> remoteFileArrayList;
    private static RemoteFilesAdapter remoteFilesAdapter;
    private HashMap<Integer, String[]> toBeDownload;
    private static final String SPACE = " ";
    private static final String DOTS = "...";
    private ArrayList<String> downloadAbleUrl;
    private File directory;
    public static ArrayList<String> alreadyExistFiles;
    private ArrayList<Integer> downloadingNow;
    private boolean foreground = false;
    private int id = 1001;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder  mBuilder;
    private MenuItem saveMenuItem;
    private final int PERMISSION_REQUEST = 10;
    private int counter = 0;
    private SwipeRefreshLayout swipeRefreshLayout;
    private static RemoteFilesFragment instance;

    public static RemoteFilesFragment getInstance() {
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        instance = this;
        mBaseView = inflater.inflate(R.layout.remote_files_fragment, container, false);
        remoteFileArrayList = new ArrayList<>();
        toBeDownload = new HashMap<>();
        mNotificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        directory = getActivity().getDir(AppGlobals.INTERNAL, MODE_PRIVATE);
        downloadAbleUrl = new ArrayList<>();
        mListView = (ListView) mBaseView.findViewById(R.id.remote_files_list);
        swipeRefreshLayout = (SwipeRefreshLayout) mBaseView.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                remoteFileArrayList = new ArrayList<>();
                remoteFilesAdapter = new RemoteFilesAdapter(getActivity().getApplicationContext(), remoteFileArrayList);
                mListView.setAdapter(remoteFilesAdapter);
                getRemoteFiles();
            }
        });
        mListView.setOnItemClickListener(this);
        getRemoteFiles();
        File files = getActivity().getDir(AppGlobals.INTERNAL, MODE_PRIVATE);
        File[] filesArray = files.listFiles();
        alreadyExistFiles = new ArrayList<>();
        remoteFilesAdapter = new RemoteFilesAdapter(getActivity().getApplicationContext(), remoteFileArrayList);
        mListView.setAdapter(remoteFilesAdapter);
        for (File file: filesArray) {
            Log.i("TAG", "file " + file);
            String[] onlyFileName = file.getName().split("\\|");
            alreadyExistFiles.add(onlyFileName[1]+onlyFileName[2]);
//            Log.i("TAG", "Remote file " + onlyFileName[0]+onlyFileName[1]);
        }
        return mBaseView;
    }

    public static void update() {
        if (remoteFilesAdapter != null)
        remoteFilesAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        foreground = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        foreground = false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.download, menu);
        saveMenuItem = menu.findItem(R.id.download);
        saveMenuItem.setVisible(false);
        if (toBeDownload.size() > 0) {
            saveMenuItem.setVisible(true);
        }
    }

    private  boolean checkAndRequestPermissions() {
        int permissionSendMessage = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_PHONE_STATE);
        int locationPermission = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (permissionSendMessage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            requestPermissions(listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:
                downloadingNow = new ArrayList<>();
                for (Map.Entry<Integer,String[]> entry : toBeDownload.entrySet()) {
                    Integer key = entry.getKey();
                    downloadingNow.add(key);
                }
                if (ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
                    checkAndRequestPermissions();
                } else {
                    if (toBeDownload.size() > 0) {
                        counter = 0;
                        String[] strings = toBeDownload.get(downloadingNow.get(counter));
                        Log.i("TAG", "Remote file " + strings[0] + " " + strings[1] + " " + strings[2] + " " + strings[3]);
                        mBuilder = new NotificationCompat.Builder(getActivity().getApplicationContext());
                        mBuilder.setContentInfo("Download...")
                                .setContentText("Downloading: " + capitalizeLetter(strings[2]))
                                .setAutoCancel(false)
                                .setSmallIcon(R.drawable.downlaod);
                        mBuilder.setProgress(100, 0, false);
                        mNotificationManager.notify(id, mBuilder.build());
                        new DownloadTask().execute(strings);
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private String capitalizeLetter(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void getRemoteFiles() {
        HttpRequest request = new HttpRequest(getActivity());
        request.setOnReadyStateChangeListener(this);
        request.setOnErrorListener(this);
        request.open("GET", String.format("%sgetcatalogue ", AppGlobals.BASE_URL));
        request.setRequestHeader("authorization",
                AppGlobals.getStringFromSharedPreferences(AppGlobals.KEY_TOKEN));
        request.send();
    }

    @Override
    public void onReadyStateChange(HttpRequest request, int readyState) {
        switch (readyState) {
            case HttpRequest.STATE_DONE:
                switch (request.getStatus()) {
                    case HttpURLConnection.HTTP_OK:
                        swipeRefreshLayout.setRefreshing(false);
                        System.out.println(request.getResponseText());
                        try {
                            JSONObject jsonObject = new JSONObject(request.getResponseText());
                            if (jsonObject.getBoolean("success")) {
                                JSONArray jsonArray = jsonObject.getJSONArray("result");
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject file = jsonArray.getJSONObject(i);
                                    DataFile dataFile = new DataFile();
                                    dataFile.setId(file.getInt("id"));
                                    dataFile.setTitle(file.getString("title"));
                                    dataFile.setExtension(file.getString("extension"));
                                    dataFile.setDuration(file.getString("duration"));
                                    dataFile.setSize(file.getString("size"));
                                    dataFile.setUrl(file.getString("url"));
                                    dataFile.setUuid(file.getString("uuid"));
                                    remoteFileArrayList.add(dataFile);
                                    remoteFilesAdapter.notifyDataSetChanged();
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                }
        }
    }

    @Override
    public void onError(HttpRequest request, int readyState, short error, Exception exception) {
        Log.i("TAG", "response " +request.getResponseText());
        swipeRefreshLayout.setRefreshing(false);
        switch (readyState) {
            case HttpRequest.ERROR_CONNECTION_TIMED_OUT:
                Helpers.showSnackBar(getView(), getResources().getString(R.string.connection_time_out));
                break;
            case HttpRequest.ERROR_NETWORK_UNREACHABLE:
                Helpers.showSnackBar(getView(), getResources().getString(R.string.network_unreachable));
                break;
            case HttpRequest.ERROR_SSL_CERTIFICATE_INVALID:
                Helpers.showSnackBar(getView(), getResources().getString(R.string.hand_shake_error));
        }

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        DataFile dataFile = remoteFileArrayList.get(i);
        String fullFileName = dataFile.getId()+dataFile.getTitle()+"."+dataFile.getExtension();
        if (alreadyExistFiles.contains(fullFileName)) {
            Helpers.showSnackBar(getView(), getResources().getString(R.string.file_already_exist));
        }
        if (!toBeDownload.containsKey(dataFile.getId())) {
            String strings[] = {dataFile.getUrl(), dataFile.getExtension(), dataFile.getTitle(),
                    String.valueOf(dataFile.getId()), String.valueOf(dataFile.getUuid())};
            toBeDownload.put(dataFile.getId(), strings);
        } else {
            toBeDownload.remove(dataFile.getId());
        }
        Log.i("TAG", "download size " + toBeDownload.size());
        Log.i("TAG", "download size " + toBeDownload);
        if (toBeDownload.size() > 0) {
            saveMenuItem.setVisible(true);
            MainActivity.getInstance().backItem.setVisible(false);
        } else {
            saveMenuItem.setVisible(false);
            MainActivity.getInstance().backItem.setVisible(true);
        }
        remoteFilesAdapter.notifyDataSetChanged();
        Log.i("TAG", "Download " + toBeDownload);
    }

    @Override
    public void onLocationForNurse(Location location, File file) {

    }

    @Override
    public void onLocationForAidWorker(Location location, String fileId) {
        sendProgressUpdateForSuccessDownload(fileId, location.getLatitude() + "," + location.getLongitude(), true);

    }

    private class RemoteFilesAdapter extends ArrayAdapter<DataFile> {

        private ViewHolder viewHolder;
        private ArrayList<DataFile> arrayList;

        public RemoteFilesAdapter(Context context, ArrayList<DataFile> arrayList) {
            super(context, R.layout.remote_delegate_files);
            this.arrayList = arrayList;
        }

        @Override
        public View getView(int position, android.view.View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.remote_delegate_files,
                        parent, false);
                viewHolder = new ViewHolder();
                viewHolder.fileName = (TextView) convertView.findViewById(R.id.name);
                viewHolder.relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relative_layout);
                convertView.setTag(viewHolder);
                viewHolder.fileName.setTypeface(AppGlobals.normalTypeFace);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            DataFile dataFile = arrayList.get(position);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(dataFile.getId());
            stringBuilder.append(SPACE);
            if (dataFile.getTitle().length() > 30) {
                String bigString = dataFile.getTitle().substring(0, Math.min(
                        dataFile.getTitle().length(), 30));
                stringBuilder.append(bigString);
                stringBuilder.append(DOTS);
                stringBuilder.append(SPACE);
            } else {
                stringBuilder.append(dataFile.getTitle());
                stringBuilder.append(SPACE);
            }
            stringBuilder.append(dataFile.getSize());
            viewHolder.fileName.setText(stringBuilder.toString());
            String fullFileName = dataFile.getId()+dataFile.getTitle()+"."+dataFile.getExtension();
            Log.i("TAG", "Remote file full name " + fullFileName);
            if (alreadyExistFiles.contains(fullFileName)) {
                viewHolder.fileName.setTextColor(getResources().getColor(R.color.already_existing_files));
                viewHolder.fileName.setTypeface(null, Typeface.BOLD_ITALIC);
                viewHolder.relativeLayout.setBackgroundColor(getResources().getColor(R.color.already_exist_file_background));

            } else {
                viewHolder.fileName.setTypeface(Typeface.SANS_SERIF);
                viewHolder.fileName.setTypeface(null, Typeface.BOLD);
                if (toBeDownload.containsKey(dataFile.getId())) {
                    viewHolder.fileName.setTextColor(getResources()
                            .getColor(R.color.blue_color));
                    viewHolder.relativeLayout.setBackgroundColor(getResources().getColor(R.color.highlighted));
                } else {
                    viewHolder.fileName.setTextColor(getResources()
                            .getColor(android.R.color.black));
                    viewHolder.relativeLayout.setBackgroundColor(getResources().getColor(android.R.color.white));

                }
            }
            return convertView;
        }

        @Override
        public int getCount() {
            return arrayList.size();
        }
    }

    private class ViewHolder {
        TextView fileName;
        RelativeLayout relativeLayout;
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }
                int fileLength = connection.getContentLength();
                input = connection.getInputStream();
                output = new FileOutputStream(directory+"/"+sUrl[4] +"|"+downloadingNow.get(counter)+"|"+sUrl[2]+"."+sUrl[1]);
                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    if (fileLength > 0)
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                Toast.makeText(AppGlobals.getContext(), AppGlobals.getContext()
                                .getResources().getString(R.string.check_internet),
                        Toast.LENGTH_SHORT).show();
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {

                }

                if (connection != null)
                    connection.disconnect();
            }
            return sUrl[2]+"."+sUrl[1] + "-" + sUrl[3];
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mBuilder.setProgress(100, values[0], false);
            mNotificationManager.notify(id, mBuilder.build());
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s.length() > 0) {
                Log.i("TAG", "DONE " + s);
                String[] file = s.split(".");
                String[] spilitted = file[1].split("-");
                if (foreground) {
                    LocalFilesFragment.getInstance().readFiles();
                    alreadyExistFiles.add(spilitted[1] + spilitted[0]);
                    remoteFilesAdapter.notifyDataSetChanged();
                }
                GetLocation getLocation = new GetLocation(RemoteFilesFragment.this);
                getLocation.acquireLocation(spilitted[1], false, null);
            } else {
                Toast.makeText(AppGlobals.getContext(), AppGlobals.getContext()
                                .getResources().getString(R.string.check_internet),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendProgressUpdateForSuccessDownload(String fileId, String locationCoordinates
            , boolean isCurrentLocation) {
        TelephonyManager mngr = (TelephonyManager) AppGlobals.getContext().
                getSystemService(Context.TELEPHONY_SERVICE);
        HttpRequest request = new HttpRequest(AppGlobals.getContext());
        request.setOnReadyStateChangeListener(new HttpRequest.OnReadyStateChangeListener() {
            @Override
            public void onReadyStateChange(HttpRequest request, int readyState) {
                switch (readyState) {
                    case HttpRequest.STATE_DONE:
                        switch (request.getStatus()) {
                            case HttpURLConnection.HTTP_OK:
                                Log.i("TAG", request.getResponseText());
                                counter++;
                                if (counter < downloadingNow.size()) {
                                    new android.os.Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            String[] strings = toBeDownload.get(downloadingNow.get(counter));
                                            new DownloadTask().execute(strings);
                                            mBuilder.setContentText("Downloading: "+capitalizeLetter(strings[2]));
                                        }
                                    }, 2000);
                                } else {
                                    toBeDownload = new HashMap<>();
                                    mNotificationManager.cancel(id);
                                }
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
        request.open("POST", String.format("%suser_download_file", AppGlobals.BASE_URL));
        request.setRequestHeader("authorization",
                AppGlobals.getStringFromSharedPreferences(AppGlobals.KEY_TOKEN));
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("deviceid", mngr.getDeviceId());
            jsonObject.put("fileid", fileId);
            jsonObject.put("location", locationCoordinates);
            jsonObject.put("timestamp", new Date().getTime());
            jsonObject.put("is_current_location", isCurrentLocation);
            Log.i("TAG", jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        request.send(jsonObject.toString());
    }
}
