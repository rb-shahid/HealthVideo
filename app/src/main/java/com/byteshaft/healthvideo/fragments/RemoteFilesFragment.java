package com.byteshaft.healthvideo.fragments;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
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

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.serializers.DataFile;
import com.byteshaft.healthvideo.utils.Helpers;
import com.byteshaft.requests.HttpRequest;

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
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by s9iper1 on 6/14/17.
 */

public class RemoteFilesFragment extends Fragment implements HttpRequest.OnReadyStateChangeListener,
        HttpRequest.OnErrorListener, AdapterView.OnItemClickListener {

    private View mBaseView;
    private ListView mListView;
    private ArrayList<DataFile> remoteFileArrayList;
    private RemoteFilesAdapter remoteFilesAdapter;
    private HashMap<Integer, String[]> toBeDownload;
    private static final String SPACE = " ";
    private static final String DOTS = "...";
    private ArrayList<String> downloadAbleUrl;
    private File directory;
    private ArrayList<String> alreadyExistFiles;
    private int counter = 0;
    private ArrayList<Integer> downloadingNow;
    private boolean foreground = false;
    private int id = 1001;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder  mBuilder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        remoteFileArrayList = new ArrayList<>();
        toBeDownload = new HashMap<>();
        mNotificationManager =
                (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        alreadyExistFiles = new ArrayList<>();
        directory = getActivity().getDir(AppGlobals.INTERNAL, MODE_PRIVATE);
        downloadAbleUrl = new ArrayList<>();
        mBaseView = inflater.inflate(R.layout.remote_files_fragment, container, false);
        mListView = (ListView) mBaseView.findViewById(R.id.remote_files_list);
        mListView.setOnItemClickListener(this);
        remoteFilesAdapter = new RemoteFilesAdapter(getActivity().getApplicationContext(), remoteFileArrayList);
        mListView.setAdapter(remoteFilesAdapter);
        getRemoteFiles();
        File files = getActivity().getDir(AppGlobals.INTERNAL, MODE_PRIVATE);
        File[] filesArray = files.listFiles();
        Log.i("TAG", "size " + filesArray.length);
        for (File file: filesArray) {
            Log.i("TAG", "name " + file.getName());
            String[] onlyFileName = file.getName().split("\\|");
            Log.i("TAG", "after split " + onlyFileName[1]);
            alreadyExistFiles.add(onlyFileName[1]);
            Log.i("TAG", "file " + alreadyExistFiles);
        }
        return mBaseView;
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
                if (toBeDownload.size() > 0) {
                    counter = 0;
                    String[] strings = toBeDownload.get(downloadingNow.get(counter));
                    mBuilder = new NotificationCompat.Builder(getActivity().getApplicationContext());
                    mBuilder.setContentInfo("Download...")
                            .setContentText("Download in progress")
                            .setAutoCancel(false)
                            .setSmallIcon(R.mipmap.ic_launcher_round);
                    mBuilder.setProgress(100, 0, false);
                    mNotificationManager.notify(id, mBuilder.build());
                    new DownloadTask().execute(strings);
                }
                return true;
            default:
                return false;
        }
    }

    private void getRemoteFiles() {
//        Helpers.showSnackBar(getView(), getResources().getString(R.string.loading_videos));
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
        switch (readyState) {
            case HttpRequest.ERROR_CONNECTION_TIMED_OUT:
                Helpers.showSnackBar(getView(), getResources().getString(R.string.connection_time_out));
                break;
            case HttpRequest.ERROR_NETWORK_UNREACHABLE:
                Helpers.showSnackBar(getView(), exception.getLocalizedMessage());
                break;
            case HttpRequest.ERROR_SSL_CERTIFICATE_INVALID:
                Helpers.showSnackBar(getView(), getResources().getString(R.string.hand_shake_error));
        }

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        DataFile dataFile = remoteFileArrayList.get(i);
        String fullFileName = dataFile.getTitle()+"."+dataFile.getExtension();
        if (alreadyExistFiles.contains(fullFileName)) {
            Helpers.showSnackBar(getView(), getResources().getString(R.string.file_already_exist));
        }
        if (!toBeDownload.containsKey(dataFile.getId())) {
            String strings[] = {dataFile.getUrl(), dataFile.getExtension(), dataFile.getTitle()};
            toBeDownload.put(dataFile.getId(), strings);
        } else {
            toBeDownload.remove(dataFile.getId());
        }
        remoteFilesAdapter.notifyDataSetChanged();
        Log.i("TAG", "Download " + toBeDownload);
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
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            DataFile dataFile = arrayList.get(position);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(dataFile.getId());
            stringBuilder.append(SPACE);
            if (dataFile.getTitle().length() > 15) {
                String bigString = dataFile.getTitle().substring(0, Math.min(
                        dataFile.getTitle().length(), 13));
                stringBuilder.append(DOTS);
                stringBuilder.append(bigString);
                stringBuilder.append(SPACE);
            } else {
                stringBuilder.append(dataFile.getTitle());
                stringBuilder.append(SPACE);
            }
            stringBuilder.append(dataFile.getSize());
            viewHolder.fileName.setText(stringBuilder.toString());
            String fullFileName = dataFile.getTitle()+"."+dataFile.getExtension();
            Log.i("TAG", "file " + fullFileName);
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

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(directory+"/"+downloadingNow.get(counter)+"|"+sUrl[2]+"."+sUrl[1]);
                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
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
            return sUrl[2]+"."+sUrl[1];
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
            Log.i("TAG", "DONE " + s);
            if (foreground) {
                LocalFilesFragment.getInstance().readFiles();
                alreadyExistFiles.add(s);
                remoteFilesAdapter.notifyDataSetChanged();
            }
            counter++;
            if (counter < downloadingNow.size()) {
                String[] strings = toBeDownload.get(downloadingNow.get(counter));
                new DownloadTask().execute(strings);
            } else {
                mNotificationManager.cancel(id);
            }
        }
    }
}
