package com.byteshaft.healthvideo.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
 * Created by s9iper1 on 6/17/17.
 */

public class Local extends Fragment implements AdapterView.OnItemClickListener {

    private View mBaseView;
    private static final String SPACE = " ";
    private static final String DOTS = "...";
    public static ArrayList<DataFile> dataFileArrayList;
    private ListView mListView;
    public LocalFileFilesAdapter localFileFilesAdapter;
    private HashMap<Integer, DataFile> toBeDelete;
    private static final long K = 1024;
    private static final long M = K * K;
    private static final long G = M * K;
    private static final long T = G * K;
    public static Local localFilesFragment;
    private MenuItem deleteMenu;
    public static boolean foreground = false;

    public static Local getInstance() {
        return localFilesFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        foreground = true;
        AppGlobals.sActivity = getActivity();
        localFilesFragment = this;
        mBaseView = inflater.inflate(R.layout.local, container, false);
        mListView = (ListView) mBaseView.findViewById(R.id.local_files_list);
        mListView.setOnItemClickListener(this);
        readFiles();
        return mBaseView;
    }

    public void updatedata() {
        if (foreground && localFileFilesAdapter != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    localFileFilesAdapter.notifyDataSetChanged();
                }
            });
        }
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

    public void readFiles() {
        toBeDelete = new HashMap<>();
        dataFileArrayList = new ArrayList<>();
        localFileFilesAdapter = new LocalFileFilesAdapter(getActivity().getApplicationContext(), dataFileArrayList);
        mListView.setAdapter(localFileFilesAdapter);
        File files = getActivity().getDir(AppGlobals.INTERNAL_NURSE, MODE_PRIVATE);
        File[] filesArray = files.listFiles();
        Log.i("TAG", "size" +files.length());
        for (File file: filesArray) {
            Log.i("TAG", file.getAbsolutePath());
            String[] onlyFileName = file.getName().split("\\|");
            Log.i("TAG", onlyFileName[0]);
            Log.i("TAG", onlyFileName[1]);
            Log.i("TAG", onlyFileName[2]);
            DataFile dataFile = new DataFile();
            dataFile.setUrl(file.getAbsolutePath());
            dataFile.setId(Integer.parseInt(onlyFileName[1]));
            dataFile.setUuid(onlyFileName[0]);
            String[] fileAndExt = onlyFileName[2].split("\\.");
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
                AlertDialog alertDialog;
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(new android.view.ContextThemeWrapper(getInstance().getActivity(), R.style.myDialog));
                alertDialogBuilder.setTitle(R.string.delete_confirmation);
                alertDialogBuilder.setIcon(android.R.drawable.ic_menu_delete);
                alertDialogBuilder.setMessage(getResources().getString(
                        R.string.want_to_delete)).setCancelable(false)
                        .setPositiveButton(getResources().getString(R.string.action_delete), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                                ArrayList<Integer> delete = new ArrayList<>();
                                int count = 0;
                                for (Map.Entry<Integer, DataFile> entry : toBeDelete.entrySet()) {
                                    Integer key = entry.getKey();
                                    DataFile file = entry.getValue();
                                    File toBeDelete = new File(file.getUrl());
                                    String strings[] = {file.getTitle(), file.getExtension(), String.valueOf(count),
                                            String.valueOf(file.getId())};
                                    Server.alreadyExistFiles.remove(file.getId()+file.getTitle()+"."+ file.getExtension());
                                    Server.getInstance().updateList();
                                    if (toBeDelete.delete())
                                        Log.i("TAG", String.valueOf(Integer.parseInt(strings[2])));
                                    delete.add(Integer.parseInt(strings[2]));
                                    Log.i("TAG", "Name "+key+" " + strings[0]+" "+strings[1]);
                                    dataFileArrayList.remove(count);
                                }
                                readFiles();
                                toBeDelete = new HashMap<>();
                            }
                        });
                alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return true;
            default: return false;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        DataFile dataFile = dataFileArrayList.get(i);
        String fullFileName = dataFile.getTitle()+"."+dataFile.getExtension();
        if (!toBeDelete.containsKey(dataFile.getId())) {
            toBeDelete.put(dataFile.getId(), dataFile);
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
                convertView = getActivity().getLayoutInflater().inflate(R.layout.delegate_play_local_files,
                        parent, false);
                viewHolder = new ViewHolder();
                viewHolder.fileName = (TextView) convertView.findViewById(R.id.name);
                viewHolder.relativeLayout = (RelativeLayout) convertView.findViewById(R.id.relative_layout);
                viewHolder.open = (ImageButton) convertView.findViewById(R.id.open);
                viewHolder.fileName.setTypeface(AppGlobals.normalTypeFace);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.fileName.setTypeface(AppGlobals.normalTypeFace);
            DataFile dataFile = arrayList.get(position);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(dataFile.getId());
            stringBuilder.append(SPACE);
            if (dataFile.getTitle().length() > 20) {
                String bigString = dataFile.getTitle().substring(0, Math.min(
                        dataFile.getTitle().length(), 20));
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
            viewHolder.open.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.e("TAG", "click");
                    DataFile dataFile = arrayList.get(position);
                    if (dataFile.getExtension().equals("3gp") || dataFile.getExtension().equals("mp4") ||
                            dataFile.getExtension().equals("mkv") || dataFile.getExtension().equals("mov")) {
                        Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
                        String path = String.format("file://%s", dataFile.getUrl());
                        Log.e("TAG", "File path " + path);
                        intent.putExtra("path", path);
                        startActivity(intent);
                    } else if (dataFile.getExtension().equals("mp3")) {
//                        Uri uri = Uri.parse(dataFile.getUrl());
//                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                        startActivity(intent);
//                        Log.e("TAG", "Extension " + dataFile.getExtension());
                        Toast.makeText(getActivity(), "file must be a video", Toast.LENGTH_SHORT).show();

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
        private ImageButton open;
        RelativeLayout relativeLayout;
    }
}
