package com.byteshaft.healthvideo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;

/**
 * Created by s9iper1 on 6/10/17.
 */

public class AppGlobals extends Application {

    private static Context sContext;
    public static final String KEY_LOGIN = "login";
    public static final String BASE_URL = "https://services.iuiunet.com/api/";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_FIRST_NAME = "first_name";
    public static final String KEY_LAST_NAME = "last_name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_TOKEN = "token";
    public static int USER_TYPE = 0;
    public static String sLanguage = "";
    public static final String INTERNAL = "internal";
    public static Typeface boldTypeFace;
    public static Typeface normalTypeFace;
    public static Typeface moreBold;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        boldTypeFace = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/bold.ttf");
        normalTypeFace = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/simple.ttf");
        moreBold = Typeface.createFromAsset(getApplicationContext().getAssets(), "fonts/more_bold.ttf");

    }

    public static Context getContext() {
        return sContext;
    }

    public static SharedPreferences getPreferenceManager() {
        return getContext().getSharedPreferences("shared_prefs", MODE_PRIVATE);
    }

    public static void loginState(boolean type) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putBoolean(KEY_LOGIN, type).apply();
    }

    public static boolean isLogin() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getBoolean(KEY_LOGIN, false);
    }

    public static void saveDataToSharedPreferences(String key, String value) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putString(key, value).apply();
    }

    public static String getStringFromSharedPreferences(String key) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getString(key, "");
    }

    public static void clearSettings() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().clear().commit();
    }
}
