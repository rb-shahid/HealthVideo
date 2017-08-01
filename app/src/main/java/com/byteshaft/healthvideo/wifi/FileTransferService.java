// Copyright 2011 Google Inc. All Rights Reserved.

package com.byteshaft.healthvideo.wifi;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.serializers.DataFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

	private static final int SOCKET_TIMEOUT = 5000;
	public static final String ACTION_SEND_FILE = "com.byteshaft.health_video.SEND_FILE";
	public static final String ACTION_SEND_ARRAY = "com.byteshaft.health_video.SEND_ARRAY";
	public static final String EXTRAS_FILE_PATH = "file_url";
	public static final String EXTRAS_DATA_FILES = "data_files";
	public static final String EXTRAS_ADDRESS = "go_host";
	public static final String EXTRAS_PORT = "go_port";

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

		Context context = getApplicationContext();
		if (intent.getAction().equals(ACTION_SEND_FILE)) {
			String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
			String host = intent.getExtras().getString(EXTRAS_ADDRESS);
			Socket socket = new Socket();
			int port = intent.getExtras().getInt(EXTRAS_PORT);

			try {
				Log.d(WifiActivity.TAG, "Opening client socket - ");
				socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

				Log.d(WifiActivity.TAG, "Client socket - " + socket.isConnected());
				OutputStream stream = socket.getOutputStream();
				ContentResolver cr = context.getContentResolver();
				InputStream is = null;
				try {
					is = cr.openInputStream(Uri.parse(fileUri));
				} catch (FileNotFoundException e) {
					Log.d(WifiActivity.TAG, e.toString());
				}
				DeviceDetailFragment.copyFile(is, stream);
				Log.d(WifiActivity.TAG, "Client: Data written");
			} catch (IOException e) {
				Log.e(WifiActivity.TAG, e.getMessage());
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
			Socket socket = new Socket();
			int port = intent.getExtras().getInt(EXTRAS_PORT);

			try {
				Log.d(WifiActivity.TAG, "Opening client socket - ");
				socket.bind(null);
				socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

				Log.d(WifiActivity.TAG, "Client socket - " + socket.isConnected());
				ObjectOutputStream stream = new ObjectOutputStream(socket.getOutputStream());
				stream.writeByte(AppGlobals.DATA_TYPE_ARRAY);
				stream.writeObject(fileUri);
				Log.d(WifiActivity.TAG, "Client: Data written");
			} catch (IOException e) {
				Log.e(WifiActivity.TAG, e.getMessage());
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
