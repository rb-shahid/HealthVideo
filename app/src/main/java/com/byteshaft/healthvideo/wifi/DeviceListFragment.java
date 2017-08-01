/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.byteshaft.healthvideo.wifi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.MainActivity;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.radar.RadarScanView;
import com.byteshaft.healthvideo.radar.RandomTextView;
import com.byteshaft.healthvideo.radar.RippleView;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.shadowColor;

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class DeviceListFragment extends Fragment implements PeerListListener {

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private ProgressDialog progressDialog = null;
    private static View mContentView = null;
    private WifiP2pDevice device;
    private TextView myDevice;
    private TextView state;
    public static RippleView rippleView;
    public static RandomTextView randomTextView;
    public static RadarScanView radarScanView;
    private static DeviceListFragment instance;

    public static DeviceListFragment getInstance() {
        return instance;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        WifiActivity.getInstance().initiateDiscovery(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (AppGlobals.USER_TYPE == 1) {
            mContentView = inflater.inflate(R.layout.wifi_connection_aid_worker_ui, null);
        } else {
            mContentView = inflater.inflate(R.layout.wifi_connection_nurse_ui, null);
        }
        instance = this;
        state = (TextView) mContentView.findViewById(R.id.state);
        state.setTypeface(AppGlobals.boldTypeFace);
        myDevice = (TextView) mContentView.findViewById(R.id.my_name);
        myDevice.setTypeface(AppGlobals.normalTypeFace);
        if (AppGlobals.USER_TYPE == 1) {
            start();
        } else {
            rippleView = (RippleView) mContentView.findViewById(R.id.ripple);
            rippleView.setMode(RippleView.MODE_OUT);
            rippleView.setTextColor(0xff0000ff);
            rippleView.setShadowLayer(1, 1, 1, shadowColor);
            rippleView.setGravity(Gravity.CENTER);
            rippleView.startRippleAnimation();
        }
        return mContentView;
    }

    public void start() {
        randomTextView = null;
        randomTextView = (RandomTextView) mContentView.findViewById(
                R.id.random_textview);
        if (radarScanView != null) {
            radarScanView.stopRadar();
        }
        radarScanView = (RadarScanView) mContentView.findViewById(R.id.scan_view);

        randomTextView.setOnRippleViewClickListener(
                new RandomTextView.OnRippleViewClickListener() {
                    @Override
                    public void onRippleViewClicked(WifiP2pDevice device) {
                        ((DeviceActionListener) getActivity()).showDetails(device);
                    }
                });
    }


    public WifiP2pDevice getDevice() {
        return device;
    }

    private String getDeviceStatus(int deviceStatus) {
        Log.d(getClass().getName(), "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                if (WifiActivity.stateMenu != null) {
                    WifiActivity.stateMenu.setIcon(R.mipmap.wifi_off);
                }
                return "Available";
            case WifiP2pDevice.INVITED:
                if (WifiActivity.stateMenu != null) {
                    WifiActivity.stateMenu.setIcon(R.mipmap.wifi_off);
                }
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                if (WifiActivity.stateMenu != null) {
                    WifiActivity.stateMenu.setIcon(R.mipmap.wifi_on);
                }
                return "Connected";
            case WifiP2pDevice.FAILED:
                if (WifiActivity.stateMenu != null) {
                    WifiActivity.stateMenu.setIcon(R.mipmap.wifi_off);
                }
                state.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                if (WifiActivity.stateMenu != null) {
                    WifiActivity.stateMenu.setIcon(R.mipmap.wifi_off);
                }
                state.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                return "Unavailable";
            default:
                WifiActivity.stateMenu.setIcon(R.mipmap.wifi_off);
                state.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                return "Unknown";
        }
    }

    public void stateNotification(String state) {
        Intent resultIntent =
                new Intent(getActivity(), MainActivity.class);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        AppGlobals.getContext(),
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        int drawable = R.mipmap.wifi_off;
        if (state.equals("Connected")) {
            drawable = R.mipmap.wifi_on;
        }

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Disconnect", resultPendingIntent)
                        .build();

        Notification newMessageNotification =
                new NotificationCompat.Builder(AppGlobals.getContext())
                        .setColor(ContextCompat.getColor(AppGlobals.getContext(), android.R.color.white))
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), drawable))
                        .setSmallIcon(drawable)
                        .setContentTitle("Connection State")
                        .setContentText(state)
                        .addAction(replyAction).build();

        NotificationManager notificationManager =
                (NotificationManager)
                        AppGlobals.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(AppGlobals.NOTIFICATION_ID,
                newMessageNotification);
    }

    public void updateThisDevice(WifiP2pDevice device) {
        this.device = device;
        String[] name = device.deviceName.split("_");
        myDevice.setText("My Device: "+name[0]);
        String currentState = getDeviceStatus(device.status);
        AppGlobals.CURRENT_STATE = currentState;
        state.setText(currentState);
        stateNotification(currentState);
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (AppGlobals.CURRENT_STATE.equals("Connected")) {
                    DeviceDetailFragment.getInstance().showSendButtonToAidWorker();
                    WifiActivity.stateMenu.setIcon(R.mipmap.wifi_on);
                } else {
                    WifiActivity.stateMenu.setIcon(R.mipmap.wifi_off);
                }
            }
        }, 1500);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        Log.i("TAG", "device : called");
        if (AppGlobals.USER_TYPE == 1) {
            for (WifiP2pDevice wifiP2pDevice : peerList.getDeviceList()) {
                Log.i("TAG", "device :" + wifiP2pDevice.deviceName);
                randomTextView.addKeyWord(wifiP2pDevice.deviceName, wifiP2pDevice);
                randomTextView.show();
            }
            if (peers.size() == 0) {
                if (randomTextView != null) {
                    randomTextView.removeAll();
                    radarScanView.stopRadar();
            }
                return;
            }
        }
    }

    public void clearPeers() {
        peers.clear();
    }

    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    public interface DeviceActionListener {

        void showDetails(WifiP2pDevice device);

        void cancelDisconnect();

        void connect(WifiP2pConfig config);

        void disconnect();
    }

}
