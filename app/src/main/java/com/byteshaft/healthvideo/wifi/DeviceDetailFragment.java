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

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.byteshaft.healthvideo.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */

public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

	public static final String IP_SERVER = "192.168.49.1";
	public static int PORT = 8988;
	private static boolean server_running = false;

	protected static final int CHOOSE_FILE_RESULT_CODE = 20;
	private View mContentView = null;
	private WifiP2pDevice device;
	private WifiP2pInfo info;
	private ProgressDialog progressDialog = null;
	private static DeviceDetailFragment instance;

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
		mContentView = inflater.inflate(R.layout.device_detail, null);
		mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				WifiP2pConfig config = new WifiP2pConfig();
				if (device != null)
				config.deviceAddress = device.deviceAddress;
				Log.i("TAG", "device " + device.deviceAddress);
				Log.i("TAG", "device status" + device.status);
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

//		mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
//				new View.OnClickListener() {
//
//					@Override
//					public void onClick(View v) {
//						// Allow user to pick an image from Gallery or other
//						// registered apps
//						Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//						intent.setType("image/*");
//						startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
//					}
//				});

		return mContentView;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		String localIP = Utils.getIPAddress(true);
		// Trick to find the ip in the file /proc/net/arp
		String client_mac_fixed = new String(device.deviceAddress).replace("99", "19");
		String clientIP = Utils.getIPFromMac(client_mac_fixed);
		// User has picked an image. Transfer it to group owner i.e peer using
		// FileTransferService.
		Uri uri = data.getData();
//		TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
//		statusText.setText("Sending: " + uri);
		Log.d(WifiActivity.TAG, "Intent----------- " + uri);
		Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
		serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
		serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
		Log.i("TAG","local ip"+  String.valueOf(localIP == null));
		Log.i("TAG","Server ip" +String.valueOf(IP_SERVER == null));
		if(localIP.equals(IP_SERVER)){
			serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, clientIP);
		}else{
			serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, IP_SERVER);
		}
		serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
		getActivity().startService(serviceIntent);
	}

	@Override
	public void onConnectionInfoAvailable(final WifiP2pInfo info) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		this.info = info;
		this.getView().setVisibility(View.VISIBLE);

//		// The owner IP is now known.
//		TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
//		view.setText(getResources().getString(R.string.group_owner_text)
//				+ ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
//						: getResources().getString(R.string.no)));
//
//		// InetAddress from WifiP2pInfo struct.
//		view = (TextView) mContentView.findViewById(R.id.device_info);
//		view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

//		mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);

		if (!server_running){
			new ServerAsyncTask(getActivity()).execute();
			server_running = true;
		}
		// hide the connect button
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
	}

	/**
	 * Updates the UI with device data
	 * 
	 * @param device the device to be displayed
	 */
	public void showDetails(WifiP2pDevice device) {
		this.device = device;
		this.getView().setVisibility(View.VISIBLE);
//		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
//		view.setText(device.deviceAddress);
//		view = (TextView) mContentView.findViewById(R.id.device_info);
//		view.setText(device.toString());
	}

	/**
	 * Clears the UI fields after a disconnect or direct mode disable operation.
	 */
	public void resetViews() {
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
//		TextView view = (TextView) mContentView.findViewById(R.id.device_address);
//		view.setText(R.string.empty);
//		view = (TextView) mContentView.findViewById(R.id.device_info);
//		view.setText(R.string.empty);
//		view = (TextView) mContentView.findViewById(R.id.group_owner);
//		view.setText(R.string.empty);
//		view = (TextView) mContentView.findViewById(R.id.status_text);
//		view.setText(R.string.empty);
		this.getView().setVisibility(View.GONE);
	}

	/**
	 * A simple server socket that accepts connection and writes some data on
	 * the stream.
	 */

	public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {

		private final Context context;

		/**
		 * @param context
		 */

		public ServerAsyncTask(Context context) {
			this.context = context;
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				ServerSocket serverSocket = new ServerSocket(PORT);
				Log.d(WifiActivity.TAG, "Server: Socket opened");
				Socket client = serverSocket.accept();
				Log.d(WifiActivity.TAG, "Server: connection done");
				final File f = new File(Environment.getExternalStorageDirectory() + "/"
						+ context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
						+ ".jpg");

				File dirs = new File(f.getParent());
				if (!dirs.exists())
					dirs.mkdirs();
				f.createNewFile();

				Log.d(WifiActivity.TAG, "server: copying files " + f.toString());
				InputStream inputstream = client.getInputStream();
				copyFile(inputstream, new FileOutputStream(f));
				serverSocket.close();
				server_running = false;
				return f.getAbsolutePath();
			} catch (IOException e) {
				Log.e(WifiActivity.TAG, e.getMessage());
				return null;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				Log.i("TAG", "completed");
//				Intent intent = new Intent();
//				intent.setAction(android.content.Intent.ACTION_VIEW);
//				intent.setDataAndType(Uri.parse("file://" + result), "image/*");
//				context.startActivity(intent);
			}

		}

		/*
		 * (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {

		}

	}

	public static boolean copyFile(InputStream inputStream, OutputStream out) {
		byte buf[] = new byte[1024];
		int len;
		try {
			while ((len = inputStream.read(buf)) != -1) {
				out.write(buf, 0, len);

			}
			out.close();
			inputStream.close();
		} catch (IOException e) {
			Log.d(WifiActivity.TAG, e.toString());
			return false;
		}
		return true;
	}

}
