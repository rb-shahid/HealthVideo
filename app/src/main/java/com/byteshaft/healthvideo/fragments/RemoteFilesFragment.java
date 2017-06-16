package com.byteshaft.healthvideo.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.serializers.DataFile;
import com.byteshaft.healthvideo.utils.Helpers;
import com.byteshaft.requests.HttpRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by s9iper1 on 6/14/17.
 */

public class RemoteFilesFragment extends Fragment implements HttpRequest.OnReadyStateChangeListener,
        HttpRequest.OnErrorListener, AdapterView.OnItemClickListener {

    private View mBaseView;
    private ListView mListView;
    private ArrayList<DataFile> remoteFileArrayList;
    private RemoteFilesAdapter remoteFilesAdapter;
    private HashMap<Integer, String> toBeDownload;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        remoteFileArrayList = new ArrayList<>();
        toBeDownload = new HashMap<>();
        mBaseView = inflater.inflate(R.layout.remote_files_fragment, container, false);
        mListView = (ListView) mBaseView.findViewById(R.id.remote_files_list);
        mListView.setOnItemClickListener(this);
        remoteFilesAdapter = new RemoteFilesAdapter(getActivity().getApplicationContext(), remoteFileArrayList);
        mListView.setAdapter(remoteFilesAdapter);
        getRemoteFiles();
        return mBaseView;
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

                return true;
            default: return false;
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
        if (!toBeDownload.containsKey(dataFile.getId())) {
            toBeDownload.put(dataFile.getId(), dataFile.getUrl());
        } else {
            toBeDownload.remove(dataFile.getId());
        }
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
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            DataFile dataFile = arrayList.get(position);
            viewHolder.fileName.setText(dataFile.getTitle());
            return convertView;
        }

        @Override
        public int getCount() {
            return arrayList.size();
        }
    }

    private class ViewHolder {
        TextView fileName;
    }
}
