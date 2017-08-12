package com.byteshaft.healthvideo.wifi;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.serializers.DataFile;

import static com.byteshaft.healthvideo.MainActivity.wifiP2pDevice;
import static com.byteshaft.healthvideo.wifi.DeviceDetailFragment.IP_SERVER;
import static com.byteshaft.healthvideo.wifi.DeviceDetailFragment.PORT;


public class WifiActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener, DeviceListFragment.DeviceActionListener {

    public static final String TAG = "wifidirectdemo";
    private static WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private final IntentFilter intentFilter = new IntentFilter();
    private static WifiP2pManager.Channel channel;
    public static BroadcastReceiver receiver = null;
    private DeviceDetailFragment fragment;
    public static MenuItem stateMenu;
    private static WifiActivity instance;

    public static WifiActivity getInstance() {
        return instance;
    }

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.main);
        instance = this;
        // add necessary intent values to be matched.

        if (getIntent().getExtras() != null) {
            if (getIntent().getExtras().getBoolean("send_file")) {
                sendRequestedFiles();
            }
        }

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
    }


    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        stopDiscovery(1000*20);
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frag_list);
        fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragment != null) {
            fragment.resetViews();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        stateMenu = menu.findItem(R.id.atn_direct_enable);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.atn_direct_enable:
                if (manager != null && channel != null) {

                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.

                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;
            case R.id.atn_direct_discover:
                if (!isWifiP2pEnabled) {
                    Toast.makeText(WifiActivity.this, "p2p",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                final DeviceListFragment fragment = (DeviceListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.frag_list);
                initiateDiscovery(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void initiateDiscovery(boolean manual) {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(AppGlobals.getContext(), "Discovery Initiated",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(AppGlobals.getContext(), "Discovery Failed : " + reasonCode,
                        Toast.LENGTH_SHORT).show();
                AppGlobals.getNotificationManager().cancel(AppGlobals.NOTIFICATION_ID);
            }
        });
        stopDiscovery(1000*60);
        if (manual)
        if (AppGlobals.USER_TYPE == 1) {
            DeviceListFragment.getInstance().start();
            DeviceListFragment.radarScanView.startAnimation();
        } else {
            DeviceListFragment.rippleView.startRippleAnimation();
        }
    }


    public static void stopDiscovery(long time) {
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!AppGlobals.CURRENT_STATE.equals("Connected")) {
                    if (manager != null && channel != null) {
                        getInstance().disconnect();
                        manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(AppGlobals.getContext(), "Discovery Stop",
                                        Toast.LENGTH_SHORT).show();
                                if (AppGlobals.USER_TYPE == 1) {
                                    if (DeviceListFragment.randomTextView != null) {
                                        DeviceListFragment.randomTextView.removeAll();
                                        DeviceListFragment.radarScanView.stopRadar();
                                    }
                                } else {
                                    DeviceListFragment.rippleView.stopRippleAnimation();
                                }
                                AppGlobals.getNotificationManager().cancel(AppGlobals.NOTIFICATION_ID);
                            }

                            @Override
                            public void onFailure(int i) {

                            }
                        });
                    }
                } else {
                    stopDiscovery(1000*40);
                }
            }
        }, time);
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);
    }

    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.i("TAG", "Connected");
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Log.i("TAG", "failure" + Utils.getP2pStatus(reason));
                Toast.makeText(WifiActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect() {
        if (DeviceDetailFragment.foreground) {
            fragment = (DeviceDetailFragment) getFragmentManager()
                    .findFragmentById(R.id.frag_detail);
            fragment.resetViews();
        }
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
                NotificationManager notificationManager = (NotificationManager)
                        getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(100011);
                if (fragment.getView() != null) {
                    fragment.getView().setVisibility(View.GONE);
                }
                AppGlobals.CURRENT_STATE = "Disconnected";
            }

        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void cancelDisconnect() {
        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(WifiActivity.this, R.string.connection_abort,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(WifiActivity.this,
                                "Connect abort request failed. Reason Code: " + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    public void sendRequestedFiles() {
        DataFile dataFile = AppGlobals.requestedFileArrayList.get(AppGlobals.senderCounter);
        Log.i(getClass().getSimpleName(), dataFile.getUrl());
        sendFile(dataFile.getUrl(), dataFile.getUuid()+"|"+ dataFile.getId() +"|" +
                dataFile.getTitle() +"."+ dataFile.getExtension());

    }

    public void sendFile(String filePath, String utf) {
        String localIP = Utils.getIPAddress(true);
        // Trick to find the ip in the file /proc/net/arp
        String client_mac_fixed = new String(wifiP2pDevice.deviceAddress).replace("99", "19");
        String clientIP = Utils.getIPFromMac(client_mac_fixed);
        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = Uri.parse(filePath);
//		TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
//		statusText.setText("Sending: " + uri);
        Log.d(WifiActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(AppGlobals.getContext(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_UTF, utf);
        Log.i("TAG","local ip"+  String.valueOf(localIP == null));
        Log.i("TAG","Server ip" +String.valueOf(IP_SERVER == null));
        if(localIP.equals(IP_SERVER)){
            serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, clientIP);
        }else{
            serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, IP_SERVER);
        }
        serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
        startService(serviceIntent);
    }
}
