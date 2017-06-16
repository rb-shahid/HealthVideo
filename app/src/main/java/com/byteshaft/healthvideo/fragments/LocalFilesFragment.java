package com.byteshaft.healthvideo.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mBaseView = inflater.inflate(R.layout.local_files_fragment, container, false);
        File files = getActivity().getDir(AppGlobals.INTERNAL, MODE_PRIVATE);
        mListView = (ListView) mBaseView.findViewById(R.id.local_files_list);
        mListView.setOnItemClickListener(this);
        toBeDelete = new HashMap<>();
        dataFileArrayList = new ArrayList<>();
        localFileFilesAdapter = new LocalFileFilesAdapter(getActivity().getApplicationContext(), dataFileArrayList);
        mListView.setAdapter(localFileFilesAdapter);
        File[] filesArray = files.listFiles();
        for (File file: filesArray) {
            String[] onlyFileName = file.getName().split("\\|");
            DataFile dataFile = new DataFile();
            dataFile.setId(Integer.parseInt(onlyFileName[0]));
            dataFile.setTitle(onlyFileName[1]);
            dataFile.setSize(convertToStringRepresentation(file.getAbsoluteFile().length()));
            dataFileArrayList.add(dataFile);
            localFileFilesAdapter.notifyDataSetChanged();
        }
        return mBaseView;
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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:

                return true;
            default: return false;
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        DataFile dataFile = dataFileArrayList.get(i);
        String fullFileName = dataFile.getTitle()+"."+dataFile.getExtension();
        if (!toBeDelete.containsKey(dataFile.getId())) {
            String strings[] = {dataFile.getTitle()};
            toBeDelete.put(dataFile.getId(), strings);
        } else {
            toBeDelete.remove(dataFile.getId());
        }
        localFileFilesAdapter.notifyDataSetChanged();
        Log.i("TAG", "Download " + toBeDelete);
    }

    private class LocalFileFilesAdapter extends ArrayAdapter<DataFile> {

        private ViewHolder viewHolder;
        private ArrayList<DataFile> arrayList;

        public LocalFileFilesAdapter(Context context, ArrayList<DataFile> arrayList) {
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
