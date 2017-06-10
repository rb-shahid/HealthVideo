package com.byteshaft.healthvideo.utils;

import android.support.design.widget.Snackbar;
import android.view.View;

import com.byteshaft.healthvideo.AppGlobals;

/**
 * Created by s9iper1 on 6/10/17.
 */

public class Helpers {

    public static void showSnackBar(View view, int id) {
        Snackbar.make(view, AppGlobals.getContext().getResources()
                .getString(id), Snackbar.LENGTH_SHORT)
                .setActionTextColor(AppGlobals.getContext().getResources().getColor(android.R.color.holo_red_light))
                .show();
    }

    public static void showSnackBar(View view, String text) {
        Snackbar.make(view, text, Snackbar.LENGTH_LONG)
                .setActionTextColor(AppGlobals.getContext().getResources().getColor(android.R.color.holo_red_light))
                .show();
    }
}
