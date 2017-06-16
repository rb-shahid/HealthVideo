package com.byteshaft.healthvideo.radar;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.serializers.Message;
import com.byteshaft.healthvideo.utils.WifiReceiver;
import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Callbacks.SalutDataCallback;
import com.peak.salut.Callbacks.SalutDeviceCallback;
import com.peak.salut.Salut;
import com.peak.salut.SalutDataReceiver;
import com.peak.salut.SalutDevice;
import com.peak.salut.SalutServiceData;

import java.util.Vector;

import static android.R.attr.shadowColor;

/**
 * Created by s9iper1 on 6/12/17.
 */

public class WifiDirectActivity extends AppCompatActivity implements SalutDataCallback,
        View.OnClickListener {

    private Salut network;
    private Button aidWorkerSendButton;
    private SalutDevice receiverDevice;
    public static boolean foreground = false;
    private static WifiDirectActivity instance;
    private TextView state;
    public RippleView rippleView;
    private WifiReceiver wifiReceiver;

    public static WifiDirectActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        if (AppGlobals.USER_TYPE == 1) {
            setContentView(R.layout.wifi_connection_aid_worker_ui);
            aidWorkerSendButton = (Button) findViewById(R.id.button);
            aidWorkerSendButton.setOnClickListener(this);
        } else {
            setContentView(R.layout.wifi_connection_nurse_ui);
        }
        state = (TextView) findViewById(R.id.state);
        startService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }


    public void startService() {
        SalutDataReceiver dataReceiver = new SalutDataReceiver(this, this);
        SalutServiceData serviceData = new SalutServiceData("P2P", 2112, Build.MODEL);
        network = new Salut(dataReceiver, serviceData, new SalutCallback() {
            @Override
            public void call() {
                Log.e("This", "Sorry, but this device does not support WiFi Direct.");
            }
        });
        if (AppGlobals.USER_TYPE == 1) {
            aidWorkerSendButton.setEnabled(false);
            final RandomTextView randomTextView = (RandomTextView) findViewById(
                    R.id.random_textview);
            randomTextView.setOnRippleViewClickListener(
                    new RandomTextView.OnRippleViewClickListener() {
                        @Override
                        public void onRippleViewClicked(final SalutDevice salutDevice) {
                            receiverDevice = salutDevice;
                            network.registerWithHost(salutDevice, new SalutCallback() {
                                @Override
                                public void call() {
                                    Log.d("TAG", "We're now registered.");
                                    state.setText("Connected :" + salutDevice.deviceName);
                                    state.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                                    aidWorkerSendButton.setEnabled(true);
                                    Snackbar.make(findViewById(android.R.id.content), "Connected!",
                                            Snackbar.LENGTH_SHORT).show();

                                }
                            }, new SalutCallback() {
                                @Override
                                public void call() {
                                    Vector<String> strings = randomTextView.getKeyWords();
                                    for (String string : strings) {
                                        randomTextView.removeKeyWord(string);
                                    }
                                    Log.d("TAG", "We failed to register.");
                                    state.setText("Not Connected");
                                    state.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                    Snackbar.make(findViewById(android.R.id.content), "Connection failed!", Snackbar.LENGTH_SHORT).show();
                                }
                            });
//                            startActivity(new Intent(getApplicationContext(), .class));

                        }
                    });

            network.discoverNetworkServices(new SalutDeviceCallback() {

                @Override
                public void call(SalutDevice device) {
                    randomTextView.addKeyWord(device.deviceName.split("_")[0], device);
                    randomTextView.show();
                    Log.d("TAG", "A device has found with the name " + device.deviceName);

                }
            }, true);
        } else {
            rippleView = (RippleView) findViewById(R.id.ripple);
            rippleView.setMode(RippleView.MODE_OUT);
            rippleView.setTextColor(0xff0000ff);
            rippleView.setShadowLayer(1, 1, 1, shadowColor);
            rippleView.setGravity(Gravity.CENTER);
            rippleView.startRippleAnimation();
            network.startNetworkService(new SalutDeviceCallback() {
                @Override
                public void call(SalutDevice device) {
                    Log.d("TAG", device.readableName + " has connected!");
                    state.setText("Connected :" + device.deviceName);
                    state.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                }
            }, new SalutCallback() {
                @Override
                public void call() {
                    Snackbar.make(findViewById(android.R.id.content), "Service Created Successfully",
                            Snackbar.LENGTH_SHORT).show();

                }
            }, new SalutCallback() {
                @Override
                public void call() {
                    Snackbar.make(findViewById(android.R.id.content), "Try turning wifi off and on",
                            Snackbar.LENGTH_SHORT).show();
                    rippleView.stopRippleAnimation();
                    network.stopNetworkService(true);
                    Log.i("TAG", "Turning off wifi");
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("TAG", "Turning on wifi");
                            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                            wifiManager.setWifiEnabled(true);
                            Log.i("TAG", "wifi state " + wifiManager.isWifiEnabled());
                        }
                    }, 5000);

                }
            });
        }
    }


    @Override
    public void onDataReceived(Object o) {
        Log.d("TAG", "Received network data.");
        Log.i("TAG", o.toString());

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiReceiver);
        foreground = false;
        if (network != null) {
            if (network.isRunningAsHost) {
                network.stopNetworkService(true);
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("TAG", "wifi");
                        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        wifiManager.setWifiEnabled(true);
                        Log.i("TAG", "wifi state " + wifiManager.isWifiEnabled());
                    }
                }, 5000);
            } else {
                try {
                    network.unregisterClient(true);
                } catch (Exception e) {
                    Log.e("WifiDirectActivity", e.getLocalizedMessage());
                }
            }
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button:
                Message message = new Message();
                message.description = "This is a test";
                network.sendToHost(message, new SalutCallback() {



                    @Override
                    public void call() {
                        Log.e("TAG", "Oh no! The data failed to send.");
                    }
                });
                break;
        }
    }
}
