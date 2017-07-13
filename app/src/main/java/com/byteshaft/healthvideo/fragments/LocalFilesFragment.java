package com.byteshaft.healthvideo.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.MainActivity;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.serializers.DataFile;
import com.byteshaft.healthvideo.uihelpers.VideoPlayerActivity;
import com.byteshaft.requests.HttpRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static com.byteshaft.healthvideo.fragments.RemoteFilesFragment.alreadyExistFiles;

/**
 * Created by s9iper1 on 6/14/17.
 */

public class LocalFilesFragment extends Fragment implements AdapterView.OnItemClickListener {

    private View mBaseView;
    private static final String SPACE = " ";
    private static final String DOTS = "...";
    private ArrayList<DataFile> dataFileArrayList;
    private ListView mListView;
    private LocalFileFilesAdapter localFileFilesAdapter;
    private HashMap<Integer, String[]> toBeDelete;
    private static final long K = 1024;
    private static final long M = K * K;
    private static final long G = M * K;
    private static final long T = G * K;
    private static LocalFilesFragment localFilesFragment;
    private MenuItem deleteMenu;

    public static LocalFilesFragment getInstance() {
        return localFilesFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        localFilesFragment = this;
        mBaseView = inflater.inflate(R.layout.local_files_fragment, container, false);
        mListView = (ListView) mBaseView.findViewById(R.id.local_files_list);
        mListView.setOnItemClickListener(this);
        readFiles();
        return mBaseView;
    }

    public void readFiles() {
        toBeDelete = new HashMap<>();
        dataFileArrayList = new ArrayList<>();
        localFileFilesAdapter = new LocalFileFilesAdapter(getActivity().getApplicationContext(), dataFileArrayList);
        mListView.setAdapter(localFileFilesAdapter);
        File files = getActivity().getDir(AppGlobals.INTERNAL, MODE_PRIVATE);
        File[] filesArray = files.listFiles();
        for (File file: filesArray) {
            Log.i("TAG", file.getAbsolutePath());
            String[] onlyFileName = file.getName().split("\\|");
            DataFile dataFile = new DataFile();
            dataFile.setUrl(file.getAbsolutePath());
            dataFile.setId(Integer.parseInt(onlyFileName[0]));
            String[] fileAndExt = onlyFileName[1].split("\\.");
            Log.i("TAG", "name " + onlyFileName[1]);
            Log.i("TAG", "only name " + fileAndExt[0]);
            dataFile.setTitle(fileAndExt[0]);
            dataFile.setExtension(fileAndExt[1]);
            dataFile.setSize(convertToStringRepresentation(file.getAbsoluteFile().length()));
            dataFileArrayList.add(dataFile);
            localFileFilesAdapter.notifyDataSetChanged();
        }
    }

    public static String convertToStringRepresentation(final long value){
        final long[] dividers = new long[] { T, G, M, K, 1 };
        final String[] units = new String[] { "TB", "GB", "MB", "KB", "B" };
        if(value < 1)
            throw new IllegalArgumentException("Invalid file size: " + value);
        String result = null;
        for(int i = 0; i < dividers.length; i++){
            final long divider = dividers[i];
            if(value >= divider){
                result = format(value, divider, units[i]);
                break;
            }
        }
        return result;
    }

    private static String format(final long value,
                                 final long divider,
                                 final String unit){
        final double result =
                divider > 1 ? (double) value / (double) divider : (double) value;
        return String.format("%.1f %s", Double.valueOf(result), unit);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.delete, menu);
        deleteMenu = menu.findItem(R.id.delete);
        deleteMenu.setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                ArrayList<Integer> delete = new ArrayList<>();
                for (Map.Entry<Integer,String[]> entry : toBeDelete.entrySet()) {
                    Integer key = entry.getKey();
                    String[] value = entry.getValue();
                    File files = getActivity().getDir(AppGlobals.INTERNAL, MODE_PRIVATE);
                    File file = new File(files.getAbsoluteFile() + "/" + key+"|"+value[0] +"."+ value[1]);
                    Log.i("TAG", "to be delete"+ file.getAbsolutePath());
                    if (file.delete())
                        Log.i("TAG", String.valueOf(Integer.parseInt(value[2])));
//                    dataFileArrayList.remove((Integer.parseInt(value[2])));
                    delete.add(Integer.parseInt(value[2]));
//                    localFileFilesAdapter.notifyDataSetChanged();
                    Log.i("TAG", "Name "+key+" " + value[0]+" "+value[1]);
                    alreadyExistFiles.remove(value[3]+value[0]+"."+value[1]);
                }
                readFiles();
                toBeDelete = new HashMap<>();
                return true;
            default: return false;
        }
    }


    private void sendProgressUpdateForSuccessDownload(String fileId) {
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
        request.open("POST", String.format("%suser_delete_file", AppGlobals.BASE_URL));
        request.setRequestHeader("authorization",
                AppGlobals.getStringFromSharedPreferences(AppGlobals.KEY_TOKEN));
        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.put("fileid", fileId);
            Log.i("TAG", jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        request.send(jsonObject.toString());
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        DataFile dataFile = dataFileArrayList.get(i);
        String fullFileName = dataFile.getTitle()+"."+dataFile.getExtension();
        if (!toBeDelete.containsKey(dataFile.getId())) {
            String strings[] = {dataFile.getTitle(), dataFile.getExtension(), String.valueOf(i), String.valueOf(dataFile.getId())};
            toBeDelete.put(dataFile.getId(), strings);
        } else {
            toBeDelete.remove(dataFile.getId());
        }
        if (toBeDelete.size() > 0) {
            deleteMenu.setVisible(true);
            MainActivity.getInstance().backItem.setVisible(false);
        } else {
            deleteMenu.setVisible(false);
            MainActivity.getInstance().backItem.setVisible(true);
        }
        localFileFilesAdapter.notifyDataSetChanged();
        Log.i("TAG", "Deleted " + toBeDelete);
    }

    private class LocalFileFilesAdapter extends ArrayAdapter<DataFile> {

        private ViewHolder viewHolder;
        private ArrayList<DataFile> arrayList;

        public LocalFileFilesAdapter(Context context, ArrayList<DataFile> arrayList) {
            super(context, R.layout.remote_delegate_files);
            this.arrayList = arrayList;
        }

        @Override
        public View getView(final int position, android.view.View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.delegate_local_file,
                        parent, false);
                viewHolder = new ViewHolder();
                viewHolder.fileName = (TextView) convertView.findViewById(R.id.name);
                viewHolder.relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relative_layout);
                viewHolder.openButton = (Button) convertView.findViewById(R.id.open);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.fileName.setTypeface(AppGlobals.normalTypeFace);
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
            String fullFileName = dataFile.getTitle()+"."+dataFile.getExtension();
            Log.i("TAG", "file " + fullFileName);
                viewHolder.fileName.setTypeface(Typeface.SANS_SERIF);
                viewHolder.fileName.setTypeface(null, Typeface.BOLD);
                if (toBeDelete.containsKey(dataFile.getId())) {
                    viewHolder.fileName.setTextColor(getResources()
                            .getColor(R.color.blue_color));
                    viewHolder.relativeLayout.setBackgroundColor(getResources().getColor(R.color.highlighted));
                } else {
                    viewHolder.fileName.setTextColor(getResources()
                            .getColor(android.R.color.black));
                    viewHolder.relativeLayout.setBackgroundColor(getResources().getColor(android.R.color.white));
            }
            viewHolder.openButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DataFile dataFile = arrayList.get(position);
                    if (dataFile.getExtension().equals("3gp") || dataFile.getExtension().equals("mp4") ||
                            dataFile.getExtension().equals("mkv") || dataFile.getExtension().equals("mov")) {
                        Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
                        intent.putExtra("path", dataFile.getUrl());
                        startActivity(intent);
                    }
                }
            });

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
        Button openButton;
    }
}
