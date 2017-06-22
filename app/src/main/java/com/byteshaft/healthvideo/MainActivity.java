package com.byteshaft.healthvideo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.byteshaft.healthvideo.accountfragments.AccountManagerActivity;
import com.byteshaft.healthvideo.accountfragments.ChangePassword;
import com.byteshaft.healthvideo.fragments.Local;
import com.byteshaft.healthvideo.fragments.LocalFilesFragment;
import com.byteshaft.healthvideo.fragments.RemoteFilesFragment;
import com.byteshaft.healthvideo.fragments.Server;
import com.byteshaft.healthvideo.utils.CustomTypefaceSpan;
import com.byteshaft.healthvideo.wifi.WifiActivity;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private ViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private static MainActivity instance;
    private int[] icons = {
            R.drawable.user,
            R.drawable.connection
    };
    public MenuItem backItem;

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        if (AccountManagerActivity.getInstance() != null) {
            AccountManagerActivity.getInstance().finish();
        }
        Log.i("TAG", AppGlobals.getStringFromSharedPreferences(AppGlobals.KEY_TOKEN));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            tabLayout.getTabAt(i).setIcon(icons[i]);
        }

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (AppGlobals.USER_TYPE == 1) {
            navigationView.inflateMenu(R.menu.menu_aid_worker);
            MenuItem menuItem = navigationView.getMenu().findItem(R.id.nav_username);
            menuItem.setTitle(AppGlobals.getStringFromSharedPreferences(AppGlobals.KEY_USER_NAME));
            MenuItem connectivity = navigationView.getMenu().findItem(R.id.connectivity_item);
            Typeface font = Typeface.create("sans-serif-thin", Typeface.NORMAL);
            SpannableString mNewTitle = new SpannableString(connectivity.getTitle());
            mNewTitle.setSpan(font, 0 , mNewTitle.length(),  Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            connectivity.setTitle(mNewTitle);
            /// Doctor's Navigation items
        } else {
            navigationView.inflateMenu(R.menu.menu_nurse);
            MenuItem menuItem = navigationView.getMenu().findItem(R.id.nav_device_name);
            menuItem.setTitle(Build.MODEL);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        backItem = menu.findItem(R.id.action_back_press);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem mi = menu.getItem(i);
            switch (mi.getItemId()) {
                case R.id.connectivity_item:
                    applyFontToMenuItem(mi, Typeface.DEFAULT_BOLD);
                    break;
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void applyFontToMenuItem(MenuItem mi, Typeface font) {
        SpannableString mNewTitle = new SpannableString(mi.getTitle());
        mNewTitle.setSpan(new CustomTypefaceSpan("", font), 0, mNewTitle.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        mi.setTitle(mNewTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_back_press:
                onBackPressed();
                return true;
            default: return false;
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_wifi_connection) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager.isWifiEnabled()) {
                startActivity(new Intent(getApplicationContext(), WifiActivity.class));
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Wifi Disabled!", Snackbar.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_logout) {
            AlertDialog alertDialog;
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("Logout");
            alertDialogBuilder.setMessage("Are you sure you want to logout ?").setCancelable(false)
                    .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    AppGlobals.clearSettings();
                    startActivity(new Intent(getApplicationContext(), AccountManagerActivity.class));
                }
            });
            alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        } else if (id == R.id.nav_change_password) {
            startActivity(new Intent(getApplicationContext(), ChangePassword.class));
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void loadFragment(Fragment fragment) {
        String backStateName = fragment.getClass().getName();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
        fragmentTransaction.replace(R.id.container, fragment, backStateName);
        fragmentTransaction.commit();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if (AppGlobals.isLogin() && AppGlobals.USER_TYPE == 1) {
                switch (position) {
                    case 0:
                        return new LocalFilesFragment();
                    case 1:
                        return new RemoteFilesFragment();
                    default:
                        return new LocalFilesFragment();
                }
            } else {
                switch (position) {
                    case 0:
                        return new Local();
                    case 1:
                        return new Server();
                    default:
                        return new Local();
                }
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "";
                case 1:
                    return "";
            }
            return null;
        }
    }
}
