package com.byteshaft.healthvideo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.byteshaft.healthvideo.R;

/**
 * Created by s9iper1 on 6/14/17.
 */

public class RemoteFilesFragment extends Fragment {

    private View mBaseView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBaseView = inflater.inflate(R.layout.remote_files_fragment, container, false);
        return mBaseView;
    }
}
