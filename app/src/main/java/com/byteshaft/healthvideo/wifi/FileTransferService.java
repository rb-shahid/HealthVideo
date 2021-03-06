// Copyright 2011 Google Inc. All Rights Reserved.

package com.byteshaft.healthvideo.wifi;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.serializers.DataFile;
import com.byteshaft.healthvideo.serializers.NurseDetails;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Logger;

import static com.byteshaft.healthvideo.MainActivity.serverThread;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

	private static final int SOCKET_TIMEOUT = 5000;
	public static final String ACTION_SEND_FILE = "com.byteshaft.health_video.SEND_FILE";
	public static final String ACTION_SEND_ARRAY = "com.byteshaft.health_video.SEND_ARRAY";
    public static final String ACTION_SEND_NURSE_DETAILS = "com.byteshaft.health_video.SEND_NURSE_DETAILS";
	public static final String EXTRAS_FILE_PATH = "file_url";
	public static final String EXTRAS_DATA_FILES = "data_files";
	public static final String EXTRAS_ADDRESS = "go_host";
	public static final String EXTRAS_UTF = "utf_data";
	public static final String EXTRAS_PORT = "go_port";
    public static final String EXTRAS_DATA_TYPE = "data_type";
	private Handler mMainThreadHandler = null;



	public FileTransferService(String name) {
		super(name);
	}

	public FileTransferService() {
		super("FileTransferService");
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		mMainThreadHandler = new Handler(getMainLooper());
		if (intent.getAction().equals(ACTION_SEND_FILE)) {
			Uri fileUri = Uri.parse(intent.getExtras().getString(EXTRAS_FILE_PATH));
			String host = intent.getExtras().getString(EXTRAS_ADDRESS);
			String utfData = intent.getExtras().getString(EXTRAS_UTF);
			Socket socket = new Socket();
			int port = intent.getExtras().getInt(EXTRAS_PORT);
			try {
				Log.d(WifiActivity.TAG, "Opening client socket - ");
				socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

				Log.d(getClass().getSimpleName(), "Client socket - " + socket.isConnected());
				OutputStream stream = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(stream);
                dos.writeByte(AppGlobals.DATA_TYPE_FILES);
                dos.writeUTF(utfData);

                File myFile = new File(fileUri.getPath());
                FileInputStream fileInputStream = new FileInputStream(myFile);
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
                Log.d(getClass().getSimpleName(), "Client: Data written " + myFile.getAbsoluteFile().length());
                dos.writeLong(myFile.length());
                byte[] buffer = new byte[8096];
                int bytesRead;
                int uploaded = 0;
                AppGlobals.showFileProgress("Sending", myFile.getName().split("\\|")[2],
						android.R.drawable.ic_menu_upload_you_tube, 100);
                while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    uploaded += bytesRead;
                    int progress = (int)
                            ((float) uploaded / myFile.length() * 100);
                    AppGlobals.updateFileProgress(progress, 100);
//                    Log.i("TAG", "progress" + progress);
                    dos.flush();
                }
                AppGlobals.removeNotification();
                dos.close();
                mMainThreadHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(getClass().getSimpleName(),"counter " + AppGlobals.senderCounter);
                        AppGlobals.senderCounter++;
                        if (AppGlobals.senderCounter < AppGlobals.requestedFileArrayList.size()) {
                            Log.e(getClass().getSimpleName(),"counter " + AppGlobals.senderCounter);
                            WifiActivity.getInstance().sendRequestedFiles();
                            Log.e(getClass().getSimpleName(),"sending next file ");
                        } else {
                            AppGlobals.senderCounter = 0;
                        }
                    }
                }, 4000);
//				DeviceDetailFragment.copyFile(is, stream);
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(), e.getMessage());
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        DeviceDetailFragment.resend.setEnabled(true);
                        DeviceDetailFragment.resend.setVisibility(View.VISIBLE);
                    }
                });
//                AppGlobals.removeNotification();
//                DeviceDetailFragment.getInstance().requestAidWorkerToSendFiles(AppGlobals.requestedFileArrayList);
			} finally {
				if (socket != null) {
					if (socket.isConnected()) {
						try {
							socket.close();
						} catch (IOException e) {
							// Give up
							e.printStackTrace();
						}
					}
				}
			}

		} else if (intent.getAction().equals(ACTION_SEND_ARRAY)) {
			ArrayList<DataFile> fileUri = (ArrayList<DataFile>) intent.getExtras().getSerializable(EXTRAS_DATA_FILES);
			String host = intent.getExtras().getString(EXTRAS_ADDRESS);
            Log.d(getClass().getSimpleName(), "host   " + host);
            int dataType = intent.getIntExtra(EXTRAS_DATA_TYPE, 0);
			Socket socket = new Socket();
			int port = intent.getExtras().getInt(EXTRAS_PORT);
			try {
				Log.d(getClass().getSimpleName(), "Opening client socket - ");
				socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

				Log.d(getClass().getSimpleName(), "Client socket - " + socket.isConnected());
				DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
				stream.writeByte(dataType);
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(stream);
				objectOutputStream.writeObject(fileUri);
				stream.flush();
				objectOutputStream.flush();
				Log.d(getClass().getSimpleName(), "Client: Data written");
				mMainThreadHandler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(AppGlobals.getContext(), "sent", Toast.LENGTH_SHORT).show();
					}
				});
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(), e.getMessage());
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AppGlobals.getContext(), "Try turning wifi off and on again", Toast.LENGTH_SHORT).show();
                    }
                });
			} finally {
				if (socket != null) {
					if (socket.isConnected()) {
						try {
							socket.close();
						} catch (IOException e) {
							// Give up
							e.printStackTrace();
						}
					}
				}
			}

		} else if (intent.getAction().equals(ACTION_SEND_NURSE_DETAILS)) {
            NurseDetails fileUri = (NurseDetails) intent.getExtras().getSerializable(EXTRAS_DATA_FILES);
            String host = intent.getExtras().getString(EXTRAS_ADDRESS);
            Log.d(getClass().getSimpleName(), "host   " + host);
            int dataType = intent.getIntExtra(EXTRAS_DATA_TYPE, 0);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_PORT);
            try {
                Log.d(getClass().getSimpleName(), "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                Log.d(getClass().getSimpleName(), "Client socket - " + socket.isConnected());
                DataOutputStream stream = new DataOutputStream(socket.getOutputStream());
                stream.writeByte(dataType);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(stream);
                objectOutputStream.writeObject(fileUri);
                stream.flush();
                objectOutputStream.flush();
                Log.e(getClass().getSimpleName(), "Client: Data written");
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AppGlobals.getContext(), "sent", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), e.getMessage());
                mMainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AppGlobals.getContext(), "Try turning wifi off and on again", Toast.LENGTH_SHORT).show();
                    }
                });
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
	}
}
