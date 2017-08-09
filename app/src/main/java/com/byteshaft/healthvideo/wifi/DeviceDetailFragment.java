package com.byteshaft.healthvideo.wifi;

import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.AppCompatButton;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.database.DatabaseHelpers;
import com.byteshaft.healthvideo.fragments.Server;
import com.byteshaft.healthvideo.interfaces.OnLocationAcquired;
import com.byteshaft.healthvideo.serializers.DataFile;
import com.byteshaft.healthvideo.serializers.NurseDetails;
import com.byteshaft.healthvideo.utils.GetLocation;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import static android.content.Context.MODE_PRIVATE;
import static com.byteshaft.healthvideo.MainActivity.serverThread;
import static com.byteshaft.healthvideo.MainActivity.wifiP2pDevice;
import static com.byteshaft.healthvideo.fragments.LocalFilesFragment.convertToStringRepresentation;

public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener, OnLocationAcquired {

    public static final String IP_SERVER = "192.168.49.1";
    public static int PORT = 8988;
    public static boolean serverRunning = false;

    private View mContentView = null;
    private WifiP2pInfo info;
    private ProgressDialog progressDialog = null;
    private static DeviceDetailFragment instance;
    private AppCompatButton sendArrayButton;
    private ArrayList<DataFile> dataFileArrayList;
    public static boolean foreground = false;
    private File currentFile;

    public static DeviceDetailFragment getInstance() {
        return instance;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        instance = this;
        foreground = true;
        mContentView = inflater.inflate(R.layout.device_detail, null);
        sendArrayButton = (AppCompatButton) mContentView.findViewById(R.id.btn_send_files);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                if (wifiP2pDevice != null)
                    config.deviceAddress = wifiP2pDevice.deviceAddress;
                Log.i("TAG", "device " + wifiP2pDevice.deviceAddress);
                Log.i("TAG", "device status" + wifiP2pDevice.status);
                config.wps.setup = WpsInfo.PBC;
                ((DeviceListFragment.DeviceActionListener) getActivity()).connect(config);
            }
        });
        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceListFragment.DeviceActionListener) getActivity()).disconnect();
                    }
                });
        return mContentView;
    }

    public void showSendButtonToAidWorker() {
        if (AppGlobals.isLogin() && AppGlobals.USER_TYPE == 1) {
            sendArrayButton.setVisibility(View.VISIBLE);
            sendArrayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendLocalFilesToNurse();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        foreground = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        foreground = false;
    }

    private void sendLocalFilesToNurse() {
        dataFileArrayList = new ArrayList<>();
        File files = getActivity().getDir(AppGlobals.INTERNAL, MODE_PRIVATE);
        File[] filesArray = files.listFiles();
        for (File file : filesArray) {
            Log.i("TAG", file.getAbsolutePath());
            String[] onlyFileName = file.getName().split("\\|");
            DataFile dataFile = new DataFile();
            dataFile.setUrl(file.getAbsolutePath());
            dataFile.setUuid(onlyFileName[0]);
            dataFile.setId(Integer.parseInt(onlyFileName[1]));
            String[] fileAndExt = onlyFileName[2].split("\\.");
            dataFile.setTitle(fileAndExt[0]);
            dataFile.setExtension(fileAndExt[1]);
            dataFile.setSize(convertToStringRepresentation(file.getAbsoluteFile().length()));
            dataFileArrayList.add(dataFile);
        }

        String localIP = Utils.getIPAddress(true);
        if (wifiP2pDevice == null) {
            Toast.makeText(getActivity(), R.string.try_turning_wifi_off_on, Toast.LENGTH_SHORT).show();
        }
        if (wifiP2pDevice == null) {
            ((DeviceListFragment.DeviceActionListener) getActivity()).disconnect();
        } else {
            String clientMacFixed = new String(wifiP2pDevice.deviceAddress).replace("99", "19");
            String clientIP = Utils.getIPFromMac(clientMacFixed);
            Log.d(WifiActivity.TAG, "sending ----------- files " + dataFileArrayList.size());
            Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_ARRAY);
            serviceIntent.putExtra(FileTransferService.EXTRAS_DATA_FILES, dataFileArrayList);
            if (localIP.equals(IP_SERVER)) {
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, clientIP);
            } else {
                serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, IP_SERVER);
            }
            serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
            getActivity().startService(serviceIntent);
        }

    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        Log.i(getClass().getSimpleName(), "owner " + info.groupOwnerAddress);
        Log.i(getClass().getSimpleName(), "owner " + info.isGroupOwner);
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);
        if (!serverRunning) {
            startServer();
        }
        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
        sendArrayButton.setEnabled(true);
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        wifiP2pDevice = device;
        this.getView().setVisibility(View.VISIBLE);
    }

    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        this.getView().setVisibility(View.GONE);
    }

    public void startServer() {
        if (serverRunning)
            return;
        // Handle not to start multiple parallel threads
        // on exception on thread make it true again
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                serverRunning = true;
                while (serverRunning) {
                    try {
                        ServerSocket serverSocket = new ServerSocket(PORT);
                        Log.d(WifiActivity.TAG, "Server: Socket opened");
                        Socket client = serverSocket.accept();
                        Logger.getLogger("Do in Background").info(client.getInetAddress().getHostAddress());
                        Logger.getLogger("Do in Background 2").info(client.getInetAddress().toString());
                        AppGlobals.clientIp = client.getInetAddress().getHostAddress();
                        Log.i("TAG", "before stream");
                        getDataFromStream(client);
                        Log.i("TAG", "after stream");
                    } catch (IOException e) {
                        serverRunning = true;
                        if (serverThread != null) {
                            serverThread.interrupt();
                            serverThread = null;
                        }
                    }
                }
            }
        });
        serverThread.start();
    }

    private void getDataFromStream(Socket socket) throws IOException {
        Log.i("GetDataFromStream", "--------------- Start");
        InputStream inputStream = socket.getInputStream();
        Log.i("GetDataFromStream", "--------------- after inputstream");
//        byte[] input = Utils.getInputStreamByteArray(inputStream);
        Log.i("GetDataFromStream", "--------------- after input");
        try {
            DataInputStream dIn = null;

            try {
                Log.i("GetDataFromStream", "--------------- before obkect inputstream");
                dIn = new DataInputStream(socket.getInputStream());
                Log.i("GetDataFromStream", "--------------- after object inputstream");
                Log.i("GetDataFromStream", "after object");
                boolean done = false;
                while (!done) {
                    Log.i("GetDataFromStream", "before read byte");
                    byte messageType = dIn.readByte();
                    Log.i("GetDataFromStream", "type" + messageType);
                    switch (messageType) {
                        case AppGlobals.DATA_TYPE_ARRAY:
                            ObjectInputStream objectInputStream = new ObjectInputStream(dIn);
                            Log.i("TAG", "Received Array");
                            ArrayList<DataFile> fileArrayList = (ArrayList<DataFile>) objectInputStream.readObject();
                            if (fileArrayList.size() > 0) {
                                Server.remoteFileArrayList = fileArrayList;
                                Log.i("TAG", "received " + fileArrayList.size());
                                if (foreground) {
                                    getInstance().getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(AppGlobals.getContext(), R.string.received, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                            done = true;
                            break;
                        case AppGlobals.DATA_TYPE_NURSE_DATA:
                            ObjectInputStream object = new ObjectInputStream(dIn);
                            Log.e("TAG", "Received Array");
                            NurseDetails nurseDetails = (NurseDetails) object.readObject();
                            DatabaseHelpers databaseHelpers = new DatabaseHelpers(AppGlobals.getContext());
                            databaseHelpers.createNewEntry(nurseDetails);
                            Log.e("TAG", "Received " + nurseDetails.getLocation());
                            done = true;
                            break;
                        case AppGlobals.DATA_TYPE_REQUESTED_ARRAY:
                            Log.i("TAG", "Received Requested Array");
                            ObjectInputStream objectInput = new ObjectInputStream(dIn);
                            ArrayList<DataFile> fileArray = (ArrayList<DataFile>) objectInput.readObject();
                            if (fileArray.size() > 0) {
                                requestAidWorkerToSendFiles(fileArray);
                            }
                            done = true;
                            break;
                        case AppGlobals.DATA_TYPE_FILES:
                            Log.i("TAG", "receiving files");
                            String name = dIn.readUTF();
                            long size = dIn.readLong();
                            Log.e(getClass().getSimpleName(), "------------- Exception Start");
                            File directory = AppGlobals.getContext().getDir(AppGlobals.INTERNAL, MODE_PRIVATE);
                            final File f = new File(directory + "/"
                                    + name);
                            GetLocation getLocation = new GetLocation(DeviceDetailFragment.this);
                            getLocation.acquireLocation(null, true, f);
                            File dirs = new File(f.getParent());
                            if (!dirs.exists()) {
                                dirs.mkdir();
                                f.mkdir();
                            }
                            Log.i("File", dirs.getAbsolutePath());
                            Log.i("File", f.getAbsolutePath());
                            AppGlobals.showFileProgress("Receiving", f.getName().split("\\|")[2], R.drawable.downlaod,(int) size);

                            Log.d(getClass().getSimpleName(), "server: copying files " + f.toString());
                            copyFile(dIn, new FileOutputStream(f), size);
                            Log.i("GetDataFromStream", "----------- END");
                            Log.d(getClass().getSimpleName(), "Client: Data written " + f.length());
                            done = true;
                            break;
                        default:
                            done = true;
                    }
                }
            } catch (ClassNotFoundException e) {
                Log.e(getClass().getSimpleName(), "------------- Exception ClassNotFoundException");
            } finally {
                if (dIn != null) {
                    dIn.close();
                }
            }
        } catch (StreamCorruptedException e) {

        }
    }

    public void requestAidWorkerToSendFiles(ArrayList<DataFile> fileArray) {
        final StringBuilder stringBuilder = new StringBuilder();
        AppGlobals.requestedFileArrayList = fileArray;
        for (DataFile dataFile : fileArray) {
            stringBuilder.append(dataFile.getTitle());
            stringBuilder.append(".");
            stringBuilder.append(dataFile.getExtension());
            stringBuilder.append("\n");
        }
        Log.i("TAG", "received " + fileArray.size());
        getInstance().getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(AppGlobals.getContext(), R.string.request_received, Toast.LENGTH_SHORT).show();
                if (foreground) {
                    AlertDialog alertDialog;
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(new android.view.ContextThemeWrapper(getInstance().getActivity(), R.style.myDialog));
                    alertDialogBuilder.setTitle(R.string.file_request);
                    alertDialogBuilder.setIcon(R.mipmap.folder);
                    alertDialogBuilder.setMessage(stringBuilder.toString()).setCancelable(false)
                            .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    WifiActivity.getInstance().sendRequestedFiles();
                                    dialog.dismiss();


                                }
                            });
                    alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });

                    alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                } else {
                    fileRequestNotification(stringBuilder.toString());
                }
            }
        });
    }


    private static int mSent = 0;
    private static long mSize;
    private Handler mMainThreadHandler = null;
    long startTime;
    long elapsedTime = 0L;

    public boolean copyFile(DataInputStream inputStream, OutputStream out, long size) throws IOException {
        Log.i("TAG", "start");
        byte buf[] = new byte[1024];
        int bytesRead;
        mSent = 0;
        mSize = size;
        Log.i("TAG", "total size " + mSize);
        try {
            Log.i("TAG", "before while");
            while (size > 0 && (bytesRead = inputStream.read(
                    buf, 0, (int) Math.min(buf.length, size))) != -1) {
                out.write(buf, 0, bytesRead);
                size -= bytesRead;
                mSent += bytesRead;
//                Log.i("TAG", "progress " + (int) (bytesRead / mSize * 100));
                if (elapsedTime > 500) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            AppGlobals.updateFileProgress(mSent,(int) mSize);
                            startTime = System.currentTimeMillis();
                            elapsedTime = 0;
                            Log.i("TAG", "updated" + (mSent));
                            Log.i("TAG", "updated" + (mSize));
                            Log.i("TAG", "updated" + (mSent / mSize));
                        }
                    });
                } else {
                    elapsedTime = new Date().getTime() - startTime;
                }

            }
            AppGlobals.removeNotification();
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d("Copy File", e.toString());
            return false;
        }
        return true;
    }

    public static void fileRequestNotification(String filesName) {
        Intent resultIntent =
                new Intent(AppGlobals.getContext(), WifiActivity.class);
        resultIntent.putExtra("send_file", true);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        AppGlobals.getContext(),
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        int drawable = R.mipmap.folder;

        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_send,
                        "Send", resultPendingIntent)
                        .build();

        Notification newMessageNotification =
                new NotificationCompat.Builder(AppGlobals.getContext())
                        .setColor(ContextCompat.getColor(AppGlobals.getContext(), android.R.color.white))
                        .setLargeIcon(BitmapFactory.decodeResource(AppGlobals.getContext().getResources(), drawable))
                        .setSmallIcon(drawable)
                        .setContentTitle(AppGlobals.getContext()
                                .getResources().getString(R.string.file_request))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(filesName))
                        .addAction(replyAction).build();
        NotificationManager notificationManager =
                (NotificationManager)
                        AppGlobals.getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(AppGlobals.FILE_NOTIFICATION_ID,
                newMessageNotification);
    }

    @Override
    public void onLocationForNurse(Location location, File file) {
        NurseDetails  nurseDetails = new NurseDetails();
        TelephonyManager telephonyManager = (TelephonyManager) AppGlobals.getContext()
                .getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        nurseDetails.setDeviceId(telephonyManager.getDeviceId());
        nurseDetails.setFileId(Integer.parseInt(file.getName().split("\\|")[1]));
        nurseDetails.setLocation(location.getLatitude()+","+location.getLongitude());
        nurseDetails.setCurrentLocation(true);
        nurseDetails.setTimeStamp(String.valueOf(new Date().getTime()));
        sendNurseDetailsToWorker(nurseDetails);
    }

    @Override
    public void onLocationForAidWorker(Location location, String fileId) {

    }

    private void sendNurseDetailsToWorker(NurseDetails nurseDetails) {
        // Trick to find the ip in the file /proc/net/arp
        // Trick to find the ip in the file /proc/net/arp
        Log.d(WifiActivity.TAG, "sending ----------- Nurse details");
        Intent serviceIntent = new Intent(AppGlobals.getContext(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_NURSE_DETAILS);
        serviceIntent.putExtra(FileTransferService.EXTRAS_DATA_FILES, nurseDetails);
        serviceIntent.putExtra(FileTransferService.EXTRAS_DATA_TYPE, AppGlobals.DATA_TYPE_NURSE_DATA);
        serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, AppGlobals.clientIp);
        serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
        AppGlobals.getContext().startService(serviceIntent);

    }
}
