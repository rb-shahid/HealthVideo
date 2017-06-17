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

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.radar.RandomTextView;
import com.byteshaft.healthvideo.radar.RippleView;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.shadowColor;

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class DeviceListFragment extends Fragment implements PeerListListener, View.OnClickListener {

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private ProgressDialog progressDialog = null;
    private View mContentView = null;
    private WifiP2pDevice device;
    private TextView state;
    public RippleView rippleView;
    private RandomTextView randomTextView;
    private TextView myDevice;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        WifiActivity.getInstance().startDiscovery();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((AppCompatActivity) getActivity()).getSupportActionBar()
                .setHomeButtonEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setHasOptionsMenu(true);
        if (AppGlobals.USER_TYPE == 1) {
            mContentView = inflater.inflate(R.layout.wifi_connection_aid_worker_ui, null);
        } else {
            mContentView = inflater.inflate(R.layout.wifi_connection_nurse_ui, null);
        }
        state = (TextView) mContentView.findViewById(R.id.state);
        myDevice = (TextView) mContentView.findViewById(R.id.my_name);
        if (AppGlobals.USER_TYPE == 1) {
            randomTextView = (RandomTextView) mContentView.findViewById(
                    R.id.random_textview);
            randomTextView.setOnRippleViewClickListener(
                    new RandomTextView.OnRippleViewClickListener() {
                        @Override
                        public void onRippleViewClicked(WifiP2pDevice salutDevice) {
                            ((DeviceActionListener) getActivity()).showDetails(device);
                        }
                    });
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

    /**
     * @return this device
     */
    public WifiP2pDevice getDevice() {
        return device;
    }

    private  String getDeviceStatus(int deviceStatus) {
        Log.d(getClass().getName(), "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                if (WifiActivity.stateMenu != null) {
                    WifiActivity.stateMenu.setIcon(R.mipmap.wifi_off);
                }
                state.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                return "Available";
            case WifiP2pDevice.INVITED:
                if (WifiActivity.stateMenu != null) {
                    WifiActivity.stateMenu.setIcon(R.mipmap.wifi_off);
                }
                state.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                if (WifiActivity.stateMenu != null) {
                    WifiActivity.stateMenu.setIcon(R.mipmap.wifi_on);
                }
                state.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                return "Connected";
            case WifiP2pDevice.FAILED:
                if (WifiActivity.stateMenu != null) {
                    WifiActivity.stateMenu.setIcon(R.mipmap.wifi_off);
                }
                state.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
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

    /**
     * Initiate a connection with the peer.
     */


    @Override
    public void onClick(View view) {

    }

    /**
     * Update UI for this device.
     * 
     * @param device WifiP2pDevice object
     */
    public void updateThisDevice(WifiP2pDevice device) {
        this.device = device;
        String[] name = device.deviceName.split("_");
        myDevice.setText("My Device: "+name[0]);
        state.setText(getDeviceStatus(device.status));
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
                Log.d(getClass().getName(), "No devices found");
                return;
            }
        }

    }

    public void clearPeers() {
        peers.clear();
    }

    public void onInitiateDiscovery() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "finding peers", true,
                true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                    }
                });
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
