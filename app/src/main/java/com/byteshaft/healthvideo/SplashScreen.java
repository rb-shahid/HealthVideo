package com.byteshaft.healthvideo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.byteshaft.healthvideo.accountfragments.AccountManagerActivity;

/**
 * Created by s9iper1 on 6/10/17.
 */

public class SplashScreen extends AppCompatActivity implements View.OnClickListener {

    private ImageButton aidWorker;
    private ImageButton nurse;
    private static SplashScreen sInstance;

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
        aidWorker.setOnClickListener(this);
        nurse.setOnClickListener(this);
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
        }
    }
}
