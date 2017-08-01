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
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.byteshaft.healthvideo.AppGlobals;
import com.byteshaft.healthvideo.R;
import com.byteshaft.healthvideo.fragments.Server;
import com.byteshaft.healthvideo.serializers.DataFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Logger;

import static android.content.Context.MODE_PRIVATE;
import static com.byteshaft.healthvideo.MainActivity.wifiP2pDevice;
import static com.byteshaft.healthvideo.fragments.LocalFilesFragment.convertToStringRepresentation;

public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

	public static final String IP_SERVER = "192.168.49.1";
	public static int PORT = 8988;
	private static boolean server_running = false;

	private View mContentView = null;
	private WifiP2pInfo info;
	private ProgressDialog progressDialog = null;
	private static DeviceDetailFragment instance;
	private AppCompatButton sendFilesButton;
	private ArrayList<DataFile> dataFileArrayList;
	public static boolean foreground = false;

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
		sendFilesButton = (AppCompatButton) mContentView.findViewById(R.id.btn_send_files);
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
			sendFilesButton.setVisibility(View.VISIBLE);
			sendFilesButton.setOnClickListener(new View.OnClickListener() {
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
		for (File file: filesArray) {
			Log.i("TAG", file.getAbsolutePath());
			String[] onlyFileName = file.getName().split("\\|");
			DataFile dataFile = new DataFile();
			dataFile.setUrl(file.getAbsolutePath());
			dataFile.setId(Integer.parseInt(onlyFileName[0]));
			String[] fileAndExt = onlyFileName[1].split("\\.");
			Log.i("TAG", "name " + onlyFileName[1]);
			Log.i("TAG", "only name " + fileAndExt[0]);
			dataFile.setTitle(fileAndExt[0]);
			dataFile.setExtension(fileAndExt[1]);
			dataFile.setSize(convertToStringRepresentation(file.getAbsoluteFile().length()));
			dataFileArrayList.add(dataFile);
		}

		String localIP = Utils.getIPAddress(true);
		// Trick to find the ip in the file /proc/net/arp
        if (wifiP2pDevice == null) {
            Toast.makeText(getActivity(), R.string.try_turning_wifi_off_on, Toast.LENGTH_SHORT).show();
        }
		String clientMacFixed = new String(wifiP2pDevice.deviceAddress).replace("99", "19");
		String clientIP = Utils.getIPFromMac(clientMacFixed);
		Log.d(WifiActivity.TAG, "sending ----------- files " + dataFileArrayList.size());
		Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
		serviceIntent.setAction(FileTransferService.ACTION_SEND_ARRAY);
		serviceIntent.putExtra(FileTransferService.EXTRAS_DATA_FILES, dataFileArrayList);
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
		Log.i(getClass().getSimpleName(), "owner " + info.groupOwnerAddress);
        Log.i(getClass().getSimpleName(), "owner " + info.isGroupOwner);
		this.info = info;
		this.getView().setVisibility(View.VISIBLE);
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
		wifiP2pDevice = device;
		this.getView().setVisibility(View.VISIBLE);
	}

	public void resetViews() {
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
		this.getView().setVisibility(View.GONE);
	}

	public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {

		private final Context context;

		public ServerAsyncTask(Context context) {
			this.context = context;
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				ServerSocket serverSocket = new ServerSocket(PORT);
				Log.d(WifiActivity.TAG, "Server: Socket opened");
				Socket client = serverSocket.accept();
				Logger.getLogger("Do in Background").info(client.getInetAddress().getHostAddress());
				Logger.getLogger("Do in Background 2").info(client.getInetAddress().toString());
				AppGlobals.clientIp = client.getInetAddress().getHostAddress();
				getArrayFromStream(client);
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
				return "";
			} catch (IOException e) {
				Log.e(WifiActivity.TAG, e.getMessage());
				return null;
			}
		}

		private void getArrayFromStream(Socket socket) throws IOException {
			ObjectInputStream dIn = new ObjectInputStream(socket.getInputStream());
			boolean done = false;
			while(!done) {
				byte messageType = dIn.readByte();

				switch(messageType) {
					case AppGlobals.DATA_TYPE_ARRAY:
						try {
							ArrayList<DataFile> fileArrayList = (ArrayList<DataFile>) dIn.readObject();
							if (fileArrayList.size() > 0) {
								Server.remoteFileArrayList = fileArrayList;
								Log.i("TAG", "received " + fileArrayList.size());
                                getInstance().getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, R.string.received, Toast.LENGTH_SHORT).show();
                                    }
                                });
							}
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						done = true;
						break;
					case AppGlobals.DATA_TYPE_REQUESTED_ARRAY:
						try {
							ArrayList<DataFile> fileArrayList = (ArrayList<DataFile>) dIn.readObject();
							final StringBuilder stringBuilder = new StringBuilder();
							if (fileArrayList.size() > 0) {
								AppGlobals.requestedFileArrayList = fileArrayList;
								for (DataFile dataFile : fileArrayList) {
									stringBuilder.append(dataFile.getTitle());
                                    stringBuilder.append(".");
                                    stringBuilder.append(dataFile.getExtension());
									stringBuilder.append("\n");
								}
								Log.i("TAG", "received " + fileArrayList.size());
								getInstance().getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(context, R.string.request_received, Toast.LENGTH_SHORT).show();
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
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						done = true;
						break;
					case 3: // Type C
						System.out.println("Message C [1]: " + dIn.readUTF());
						System.out.println("Message C [2]: " + dIn.readUTF());
						done = true;
						break;
					default:
						done = true;
				}
			}

			dIn.close();
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				Log.i("TAG", "completed");
			}

		}

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
}
