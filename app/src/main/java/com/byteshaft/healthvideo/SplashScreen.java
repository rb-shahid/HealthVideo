package com.byteshaft.healthvideo;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.byteshaft.healthvideo.accountfragments.AccountManagerActivity;
import com.byteshaft.healthvideo.utils.Helpers;

import java.util.ArrayList;

/**
 * Created by s9iper1 on 6/10/17.
 */

public class SplashScreen extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private ImageButton aidWorker;
    private ImageButton nurse;
    private static SplashScreen sInstance;
    private AppCompatSpinner appCompatSpinner;
    private ImageButton questionMark;

    public static SplashScreen getInstance() {
        return sInstance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        sInstance = this;
        aidWorker = (ImageButton) findViewById(R.id.aid_worker);
        nurse = (ImageButton) findViewById(R.id.nurse);
        questionMark = (ImageButton) findViewById(R.id.question_mark);
        questionMark.setOnClickListener(this);
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("English");
        arrayList.add("Arabic");
        arrayList.add("Swahili");
        appCompatSpinner = (AppCompatSpinner) findViewById(R.id.spinner);
        MySpinnerAdapter mySpinnerAdapter = new MySpinnerAdapter(getApplicationContext(),
                android.R.layout.simple_list_item_1, arrayList);
        appCompatSpinner.setAdapter(mySpinnerAdapter);
        appCompatSpinner.setOnItemSelectedListener(this);
        aidWorker.setOnClickListener(this);
        nurse.setOnClickListener(this);
        buttonEffect(questionMark);
        buttonEffect(nurse);
        buttonEffect(aidWorker);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.aid_worker:
                AppGlobals.USER_TYPE = 1;
                if (!AppGlobals.isLogin()) {
                    startActivity(new Intent(getApplicationContext(), AccountManagerActivity.class));
                } else {
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                }
                break;
            case R.id.nurse:
                AppGlobals.USER_TYPE = 0;
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                break;
            case R.id.question_mark:
                Helpers.showAlertDialogForQuestionMark(this);
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Log.i("TAG", "item " + adapterView.getItemAtPosition(i));
        String language = String.valueOf(adapterView.getItemAtPosition(i));
        AppGlobals.sLanguage = language.substring(0,1);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public static void buttonEffect(View button) {
        button.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        v.getBackground().setColorFilter(0xe0f7f7f7, PorterDuff.Mode.SRC_ATOP);
                        v.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        v.getBackground().clearColorFilter();
                        v.invalidate();
                        break;
                    }
                }
                return false;
            }
        });
    }

    private static class MySpinnerAdapter extends ArrayAdapter<String> {

        private ArrayList<String> items;

        private MySpinnerAdapter(Context context, int resource, ArrayList<String> items) {
            super(context, resource, items);
            this.items = items;
        }

        // Affects default (closed) state of the spinner
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setText(getItem(position));
            view.setTypeface(AppGlobals.normalTypeFace);
            return view;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        // Affects opened state of the spinner
        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
            view.setTypeface(AppGlobals.normalTypeFace);
            return view;
        }
    }
}
