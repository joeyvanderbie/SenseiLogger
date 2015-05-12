package org.hva.sensei.logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.hva.sensei.data.AccelData;
import org.hva.sensei.data.HeartRateData;
import org.hva.sensei.db.AccelDataSource;
import org.hva.sensei.db.DatabaseHelper;
import org.hva.sensei.db.HeartRateDataSource;
import org.hva.sensei.sensors.AccelerometerListener;
import org.hva.sensei.sensors.bluetooth.BluetoothHeartRateActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainMovementActivity extends BluetoothHeartRateActivity {
	public static DatagramSocket mSocket = null;
	public static DatagramPacket mPacket = null;
	private RadioGroup radioHeartRateGroup;
	private EditText samplingRateTextField;
	private LinearLayout settingsLayout;
	TextView mIP_Adress;
	TextView mPort;
	TextView sampling_rate_textview;
	Button button1;
	Button button2;
	Button button3;
	SensorManager sensorManager;
	Sensor accelerometer;
	Sensor uiAccelerometer;
	String accelPath = Environment.getExternalStorageDirectory()
			+ "/Sensei/Accelerometer";
	WakeLock wakeLock;
	boolean recording = false;

	AccelerometerListener accelerometerListener;
	private int delayInMicroseconds = 10000;//50000; // for 20Hz sampling rate SensorManager.SENSOR_DELAY_FASTEST;//
	// 
	private boolean streamData = false;
	Sensor mSensor;

	private boolean mScanning;
	private Handler mHandler;

	private static final int REQUEST_ENABLE_BT = 1;
	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;

	private ProgressDialog mProgressDialog;
	private LinearLayout files;
	
	// BroadcastReceiver for handling ACTION_SCREEN_OFF.
	// private BroadcastReceiver mReceiver = new BroadcastReceiver() {
	// @Override
	// public void onReceive(Context context, Intent intent) {
	// // Check action just to be on the safe side.
	// if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
	// // Unregisters the listener and registers it again.
	// sensorManager.unregisterListener(accelerometerListener);
	// sensorManager.registerListener(accelerometerListener,
	// accelerometer, delayInMicroseconds);
	// }
	// }
	// };

	private TextView heart_rate, heart_raterr;
	// private Button connect;

	private HeartRateDataSource hds;

	private void addNewAcceleromterListener(int newDelayInMicroseconds){
		sensorManager.unregisterListener(accelerometerListener);

		delayInMicroseconds = newDelayInMicroseconds;
		
		sensorManager.registerListener(accelerometerListener, accelerometer,
				delayInMicroseconds);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setIndeterminate(false);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		accelerometerListener = new AccelerometerListener(this);
		sensorManager.registerListener(accelerometerListener, accelerometer,
				delayInMicroseconds);

		
	settingsLayout = (LinearLayout) findViewById(R.id.settingsLayout);
		
		Button settingsButton = (Button) findViewById(R.id.toggleSettings);
		settingsButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(settingsLayout.getVisibility() == View.VISIBLE){
					settingsLayout.setVisibility(View.GONE);
				}else{
					settingsLayout.setVisibility(View.VISIBLE);
				}
			}
		});
		
		samplingRateTextField = (EditText) findViewById(R.id.sampling_rate);
		samplingRateTextField.setText(""+delayInMicroseconds);
		
		Button updateSamplingRateButton = (Button) findViewById(R.id.update_sampling_rate_button);
		updateSamplingRateButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int newSamplingRate = Integer.parseInt(samplingRateTextField.getText().toString());
				if(newSamplingRate > 0){
					addNewAcceleromterListener(newSamplingRate);
				}
				
			}
		});
		
		hds = new HeartRateDataSource(this);

		// Register our receiver for the ACTION_SCREEN_OFF action. This will
		// make our receiver
		// code be called whenever the phone enters standby mode.
		// IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		// registerReceiver(mReceiver, filter);

		PowerManager mgr = (PowerManager) MainMovementActivity.this
				.getSystemService(Context.POWER_SERVICE);
		wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"SenseiWakeLock");

		radioHeartRateGroup = (RadioGroup) findViewById(R.id.radioHeartRateSensor);
		radioHeartRateGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(checkedId == R.id.radioHeartRateNone){
					//disable bluetooth connection ect
					MainMovementActivity.this.disconnectBluetooth();
					MainMovementActivity.this.updateHearRate("-");
					MainMovementActivity.this.updateHearRateRR("-");
				}else if(checkedId == R.id.radioHeartRateMioLink){
					MainMovementActivity.this.updateHearRate("0");
					MainMovementActivity.this.updateHearRateRR("0");
					MainMovementActivity.this.setDeviceAdress("C3:4D:F2:BD:3B:63");
					MainMovementActivity.this.reconnectBluetooth();
				}else if(checkedId == R.id.radioHeartRateWahooTICKR){
					MainMovementActivity.this.updateHearRate("0");
					MainMovementActivity.this.updateHearRateRR("0");
					MainMovementActivity.this.setDeviceAdress("E5:86:D2:0E:8E:E6");
					MainMovementActivity.this.reconnectBluetooth();
				}
				
			}
		});
		
		sampling_rate_textview = (TextView) findViewById(R.id.sampling_rate_info);
		button1 = (Button) findViewById(R.id.button1);
		button1.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				button1.setEnabled(false);
				button1.setVisibility(View.GONE);

				button2.setVisibility(View.VISIBLE);
				button2.setEnabled(true);
				sampling_rate_textview.setText("...");

				// start_UDP_Stream();
				accelerometerListener.startRecording();

				wakeLock.acquire();
				recording = true;

			}
		});
		button2 = (Button) findViewById(R.id.button2);
		button2.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				button1.setEnabled(true);
				button1.setVisibility(View.VISIBLE);
				button2.setEnabled(false);
				button2.setVisibility(View.GONE);

				sampling_rate_textview.setText("-");

				accelerometerListener.stopRecording();
				wakeLock.release();
//				 stop_UDP_Stream();
//				try {
//					exportAcceltoCSV(accelerometerListener.run_id);
//					updateFileList();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				recording = false;

				 backupDB();
			}
		});

		mIP_Adress = (TextView) findViewById(R.id.target_ip);
		mPort = (TextView) findViewById(R.id.target_port);

		ToggleButton streamDataButton = (ToggleButton) findViewById(R.id.stream_data_toggle);
		streamDataButton.setChecked(streamData);
		streamDataButton
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						streamData = isChecked;
						if (streamData) {
							start_UDP_Stream();

						} else {
							stop_UDP_Stream();
						}
					}
				});

		updateFileList();

		// bluetooth hr
		heart_rate = (TextView) findViewById(R.id.heart_rate_info);
		heart_raterr = (TextView) findViewById(R.id.heart_rate_rr_info);
		// connect = (Button) findViewById(R.id.connect_device);
		// connect.setOnClickListener(new View.OnClickListener() {
		//
		// @Override
		// public void onClick(View arg0) {
		// scanLeDevice(true);
		// }
		// });

		Button clear_data = (Button) findViewById(R.id.clear_data);
		clear_data.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(MainMovementActivity.this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle(R.string.button_clear_data)
						.setMessage(R.string.confirm_clear_data)
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										new ProgressTaskBackupAndDelete()
												.execute(new String[0]);
										updateFileList();
									}

								}).setNegativeButton(R.string.no, null).show();

			}
		});
		Button refresh = (Button) findViewById(R.id.refresh_list);
		refresh.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				updateFileList();
			}
		});

	}

	private void backupDB() {
		String backupLocation = Environment.getExternalStorageDirectory()
				.getAbsolutePath()
				+ "/Sensei/backup"
				+ System.currentTimeMillis() + ".zip";

		ArrayList<String> uploadData = new ArrayList<String>();
		uploadData.add(backupLocation);
		makeZip mz = new makeZip(backupLocation);
		mz.addZipFile(getDatabasePath(DatabaseHelper.DATABASE_NAME)
				.getAbsolutePath());
		mz.closeZip();
	}
	

	@Override
	protected void onResume() {
		super.onResume();
		// sensorManager.registerListener(accelerometerListener, accelerometer,
		// delayInMicroseconds);
	}

	@Override
	protected void onPause() {
		super.onPause();
		// sensorManager.unregisterListener(accelerometerListener);
	}

	@Override
	public void onDestroy() {
		// Unregister our receiver.
		// unregisterReceiver(mReceiver);

		// Unregister from SensorManager.
		sensorManager.unregisterListener(accelerometerListener);
		stop_UDP_Stream();
		if (wakeLock.isHeld()) {
			wakeLock.release();
		}
		super.onDestroy();

	}

	private void updateFileList() {
		mProgressDialog.show();
		File dir = new File(accelPath);
		List<String> list = getSortedFilenames(dir);
		LinearLayout files = (LinearLayout) findViewById(R.id.fileList);
		files.removeAllViews();
		for (final String file : list) {
			Button b = new Button(this);
			b.setText(this.getString(R.string.share_prefix_button) + " " + file);
			b.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent sharingIntent = new Intent(
							android.content.Intent.ACTION_SEND);
					sharingIntent.setType("text/csv");
					sharingIntent.putExtra(
							android.content.Intent.EXTRA_SUBJECT,
							getResources().getString(R.string.share_title));
					sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT,
							getResources().getString(R.string.share_body));
					sharingIntent.putExtra(Intent.EXTRA_STREAM,
							Uri.parse("file:///" + accelPath + "/" + file));
					startActivity(Intent.createChooser(
							sharingIntent,
							getResources().getString(
									R.string.share_dialog_title)));
				}
			});
			files.addView(b);
		}
		mProgressDialog.dismiss();
	}

	public void displayRates() {
		// button1.setEnabled(true);

		sampling_rate_textview.setText(""
				+ Math.round(accelerometerListener.getSamplingRate()));
	}

	public void exportAcceltoCSV(final int runId) throws IOException {
		{
			File folder = new File(accelPath);

			folder.mkdirs();

			final String filename = folder.toString() + "/" + "Accel" + runId
					+ ".csv";

			// show waiting screen
			CharSequence contentTitle = getString(R.string.app_name);
			final ProgressDialog progDailog = ProgressDialog.show(this,
					contentTitle, getString(R.string.msg_please_wait), true);// please
																				// wait
			final Handler handler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					updateFileList();
				}
			};

			new Thread() {
				@Override
				public void run() {
					try {

						// FileWriter fw = new FileWriter(filename);
						FileOutputStream fOut = new FileOutputStream(filename);

						OutputStreamWriter osw = new OutputStreamWriter(fOut);

						AccelDataSource ads = new AccelDataSource(
								MainMovementActivity.this);
						ads.open();
						ArrayList<AccelData> allAccel = ads.getAllAccel(runId,
								ads.getAllAccelCount(runId), 0);
						ads.close();

						osw.append("X");
						osw.append(',');

						osw.append("Y");
						osw.append(',');

						osw.append("Z");
						osw.append(',');

						osw.append("TimeStamp");
						osw.append(',');

						osw.append("RunId");
						osw.append(',');

						osw.append("Id");

						osw.append('\n');

						for (AccelData oneAccel : allAccel) {
							osw.append("" + oneAccel.getX());
							osw.append(',');

							osw.append("" + oneAccel.getY());
							osw.append(',');

							osw.append("" + oneAccel.getZ());
							osw.append(',');

							osw.append("" + oneAccel.getTimestamp());
							osw.append(',');

							osw.append("" + oneAccel.getRun_id());
							osw.append(',');

							osw.append("" + oneAccel.getId());

							osw.append('\n');

						}

						osw.flush();
						osw.close();

					} catch (Exception e) {
						e.printStackTrace();
					}
					handler.sendEmptyMessage(0);
					progDailog.dismiss();
				}
			}.start();

		}

	}

	private List<String> getSortedFilenames(File dir) {
		return getSortedFilenames(dir, null);
	}

	private List<String> getSortedFilenames(File dir, String sub) {
		List<String> list = new ArrayList<String>();
		readDirectory(dir, list, "", ".csv");
		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String object1, String object2) {
				if (object1.compareTo(object2) < 0) {
					return -1;
				} else if (object1.equals(object2)) {
					return 0;
				}
				return 1;
			}

		});
		return list;
	}

	private void readDirectory(File dir, final List<String> list,
			String parent, String extension) {
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.getName().toLowerCase().endsWith(extension)) { //$NON-NLS-1$
						list.add(parent + f.getName());
					} else if (f.isDirectory()) {
						readDirectory(f, list, parent + f.getName() + "/",
								extension);
					}
				}
			}
		}
	}

	public class makeZip {
		static final int BUFFER = 2048;

		ZipOutputStream out;
		byte data[];

		public makeZip(String name) {
			FileOutputStream dest = null;
			try {
				dest = new FileOutputStream(name);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			out = new ZipOutputStream(new BufferedOutputStream(dest));
			data = new byte[BUFFER];
		}

		public void addZipFile(String name) {
			Log.v("addFile", "Adding: ");
			FileInputStream fi = null;
			try {
				fi = new FileInputStream(name);
				Log.v("addFile", "Adding: ");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.v("atch", "Adding: ");
			}
			BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
			ZipEntry entry = new ZipEntry(name);
			try {
				out.putNextEntry(entry);
				Log.v("put", "Adding: ");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int count;
			try {
				while ((count = origin.read(data, 0, BUFFER)) != -1) {
					out.write(data, 0, count);
					// Log.v("Write", "Adding: "+origin.read(data, 0, BUFFER));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.v("catch", "Adding: ");
			}
			try {
				origin.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void closeZip() {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private boolean start_UDP_Stream() {
		if (streamData) {
			boolean isOnWifi = isOnWifi();
			if (isOnWifi == false) {
				showDialog(R.string.error_warningwifi);
				return false;
			}

			InetAddress client_adress = null;
			try {
				client_adress = InetAddress.getByName(mIP_Adress.getText()
						.toString());
			} catch (UnknownHostException e) {
				showDialog(R.string.error_invalidaddr);
				return false;
			}
			try {
				mSocket = new DatagramSocket();
				mSocket.setReuseAddress(true);
			} catch (SocketException e) {
				mSocket = null;
				showDialog(R.string.error_neterror);
				return false;
			}

			byte[] buf = new byte[256];
			int port;
			try {
				port = Integer.parseInt(mPort.getText().toString());
				mPacket = new DatagramPacket(buf, buf.length, client_adress,
						port);
			} catch (Exception e) {
				mSocket.close();
				mSocket = null;
				showDialog(R.string.error_neterror);
				return false;
			}

			return true;
		} else {
			return false;
		}

	}

	private void stop_UDP_Stream() {
		if (mSocket != null)
			mSocket.close();
		mSocket = null;
		mPacket = null;

	}

	private boolean isOnWifi() {
		ConnectivityManager conman = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		return conman.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.isConnectedOrConnecting();
	}

	@Override
	protected void processData(String data) {
		if (mConnected) {
			if(data.substring(0, 3).equals("HR:")) {
				updateHearRate(data.substring(3));
			}else if(data.substring(0, 3).equals("RR:")) {
				updateHearRateRR(data.substring(3));
			}
		}
	}
	
	protected void updateHearRate(String data) {
			Log.d(TAG, "Hear rate data: " + data);
			if (heart_rate != null) {
				heart_rate.setText(data);

				if (recording) {
					hds.open();
					hds.addHeartRateSilent(new HeartRateData(Long
							.parseLong(data), System.currentTimeMillis(),
							accelerometerListener.run_id));
					hds.close();
					// new UDPThread().execute(data + ", " +
					// System.currentTimeMillis());
				}
			}
	}
	
	protected void updateHearRateRR(String data) {
		Log.d(TAG, "Hear rate RR data: " + data);
		if (heart_raterr != null) {
			heart_raterr.setText(data);

			if (recording) {
				hds.open();
				hds.addHeartRateRRSilent(new HeartRateData(Long
						.parseLong(data), System.currentTimeMillis(),
						accelerometerListener.run_id));
				hds.close();
				// new UDPThread().execute(data + ", " +
				// System.currentTimeMillis());
			}
		}
}

	@Override
	protected void onBluetoothDisconnected() {
		Log.d(TAG, "Hear rate sensor disconnected");
		if (heart_rate != null) {
			heart_rate.setText("-");
		}

		// connect.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onBluetoothConnected() {
		Log.d(TAG, "Hear rate sensor connected");
		if (heart_rate != null) {
			heart_rate.setText("...");
			// connect.setVisibility(View.GONE);
		}
	}
	
	public class ProgressTask extends AsyncTask<String, Void, String> {
		@Override
		protected void onPreExecute() {
			mProgressDialog.show();
		}

		@Override
		protected String doInBackground(String... arg0) {

			// my stuff is here
			backupDB();
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			mProgressDialog.dismiss();
		}
	}

	public class ProgressTaskBackupAndDelete extends
			AsyncTask<String, Void, String> {
		@Override
		protected void onPreExecute() {
			mProgressDialog.show();
		}

		@Override
		protected String doInBackground(String... arg0) {
			// Remove the datapoints to save space
			DatabaseHelper dbHelper = new DatabaseHelper(
					MainMovementActivity.this);
			dbHelper.doSaveDelete(dbHelper.getWritableDatabase());

			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			mProgressDialog.dismiss();
		}
	}
}
