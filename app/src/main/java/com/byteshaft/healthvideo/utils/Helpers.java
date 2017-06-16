package com.byteshaft.healthvideo.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.View;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.R;

/**
 * Created by s9iper1 on 6/10/17.
 */

public class Helpers {

    private static ProgressDialog progressDialog;
    private static AlertDialog alertDialog;

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

    public static void showProgressDialog(Activity activity, String message) {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    public static void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public static void showAlertDialogForQuestionMark(Activity activity) {
        AlertDialog alertDialog;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder.setTitle(activity.getResources().getString(R.string.support));
        alertDialogBuilder.setMessage(Html.fromHtml("If you have a problem using the app please send SMS to <font color='#4242EC'>+923448797786</font> or email to <font color='#4242EC'>Test@test.com</font>")).setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public static void alertDialog(int icon, Activity activity, String title, String message) {
        AlertDialog alertDialog;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setIcon(icon);
        alertDialogBuilder.setMessage(Html.fromHtml(message)).setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();


                    }
                });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
