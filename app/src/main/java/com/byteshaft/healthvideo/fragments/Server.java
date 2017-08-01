package com.byteshaft.healthvideo.fragments;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.byteshaft.healthvideo.MainActivity;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.serializers.DataFile;
import com.byteshaft.healthvideo.utils.Helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by s9iper1 on 6/17/17.
 */

public class Server extends Fragment implements AdapterView.OnItemClickListener {

    private View mBaseView;
    private ListView mListView;
    public static ArrayList<DataFile> remoteFileArrayList;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mBaseView = inflater.inflate(R.layout.server, container, false);
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
            }
        });
        mListView.setOnItemClickListener(this);

        File files = getActivity().getDir(AppGlobals.INTERNAL, MODE_PRIVATE);
        File[] filesArray = files.listFiles();
        Log.i("TAG", "Remote file " + filesArray.length);
        alreadyExistFiles = new ArrayList<>();
        remoteFilesAdapter = new RemoteFilesAdapter(getActivity().getApplicationContext(), remoteFileArrayList);
        mListView.setAdapter(remoteFilesAdapter);
        for (File file: filesArray) {
            String[] onlyFileName = file.getName().split("\\|");
            alreadyExistFiles.add(onlyFileName[0]+onlyFileName[1]);
            Log.i("TAG", "Remote file " + onlyFileName[0]+onlyFileName[1]);
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
                    // send requested files
                }
                return true;
            default:
                return false;
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
            String strings[] = {dataFile.getUrl(), dataFile.getExtension(), dataFile.getTitle(), String.valueOf(dataFile.getId())};
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
}
