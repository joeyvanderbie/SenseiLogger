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
import org.hva.sensei.sensors.LocationUtils;
import org.hva.sensei.sensors.bluetooth.BluetoothHeartRateActivity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Interpolator;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainMovementActivity extends BluetoothHeartRateActivity implements
LocationListener,
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener {
	public static DatagramSocket mSocket = null;
	public static DatagramPacket mPacket = null;
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
	private int delayInMicroseconds = 50000; // for 20Hz sampling rate   SensorManager.SENSOR_DELAY_FASTEST;//
	private boolean streamData = false;
	Sensor mSensor;

	private boolean mScanning;
	private Handler mHandler;

	private static final int REQUEST_ENABLE_BT = 1;
	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;

	// BroadcastReceiver for handling ACTION_SCREEN_OFF.
//	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			// Check action just to be on the safe side.
//			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
//				// Unregisters the listener and registers it again.
//				sensorManager.unregisterListener(accelerometerListener);
//				sensorManager.registerListener(accelerometerListener,
//						accelerometer, delayInMicroseconds);
//			}
//		}
//	};

	private TextView heart_rate;
//	private Button connect;
	
	private HeartRateDataSource hds;
	private LinearLayout map_layout;
	private GoogleMap mMap;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		map_layout = (LinearLayout) findViewById(R.id.map_layout);
		setUpMapIfNeeded();

		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		accelerometerListener = new AccelerometerListener(this);
		sensorManager.registerListener(accelerometerListener, accelerometer,
				delayInMicroseconds);
		
		   hds = new HeartRateDataSource(this);
	

		// Register our receiver for the ACTION_SCREEN_OFF action. This will
		// make our receiver
		// code be called whenever the phone enters standby mode.
//		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
//		registerReceiver(mReceiver, filter);

		PowerManager mgr = (PowerManager) MainMovementActivity.this
				.getSystemService(Context.POWER_SERVICE);
		wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"SenseiWakeLock");

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

				startUpdates();
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
				// stop_UDP_Stream();
//				try {
//					exportAcceltoCSV(accelerometerListener.run_id);
//					updateFileList();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				recording = false;
				
				stopUpdates();

				String backupLocation = Environment
						.getExternalStorageDirectory().getAbsolutePath()
						+ "/Sensei/backup"
						+ System.currentTimeMillis()
						+ ".zip";

				ArrayList<String> uploadData = new ArrayList<String>();
				uploadData.add(backupLocation);
				makeZip mz = new makeZip(backupLocation);
				mz.addZipFile(getDatabasePath(DatabaseHelper.DATABASE_NAME)
						.getAbsolutePath());
				mz.closeZip();
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
						if(streamData){
							start_UDP_Stream();
							
						}else{
							stop_UDP_Stream();
						}
					}
				});

		updateFileList();

		//bluetooth hr
		 heart_rate = (TextView) findViewById(R.id.heart_rate_info);
//		 connect = (Button) findViewById(R.id.connect_device);
//		 connect.setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View arg0) {
//				scanLeDevice(true);
//			}
//		});
		 
		 
		// Create a new global location parameters object
	        mLocationRequest = LocationRequest.create();

	        /*
	         * Set the update interval
	         */
	        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

	        // Use high accuracy
	        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

	        // Set the interval ceiling to one minute
	        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

	        // Note that location updates are off until the user turns them on
	        mUpdatesRequested = false;

	        // Open Shared Preferences
	       // mPrefs = getSharedPreferences(LocationUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);

	        // Get an editor
	       // mEditor = mPrefs.edit();

	        /*
	         * Create a new location client, using the enclosing class to
	         * handle callbacks.
	         */
	        mLocationClient = new LocationClient(this, this, this);
	}

	protected void onResume() {
		super.onResume();
		// sensorManager.registerListener(accelerometerListener, accelerometer,
		// delayInMicroseconds);
		setUpMapIfNeeded();
	}

	protected void onPause() {
		super.onPause();
		// sensorManager.unregisterListener(accelerometerListener);
	}

	@Override
	public void onDestroy() {
		// Unregister our receiver.
		//unregisterReceiver(mReceiver);
		
		stopUpdates();

		// Unregister from SensorManager.
		sensorManager.unregisterListener(accelerometerListener);
		stop_UDP_Stream();
		if (wakeLock.isHeld()){
			wakeLock.release();
		}
		super.onDestroy();

	}

	private void updateFileList() {
		File dir = new File(accelPath);
		List<String> list = getSortedFilenames(dir);
		LinearLayout files = (LinearLayout) findViewById(R.id.fileList);
		files.removeAllViews();
		for (final String file : list) {
			Button b = new Button(this);
			b.setText(this.getString(R.string.share_prefix_button) + " "+file);
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
	}

	public void displayRates() {
		// button1.setEnabled(true);

		sampling_rate_textview.setText(""+Math.round(accelerometerListener.getSamplingRate()));
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
					contentTitle, getString(R.string.msg_please_wait), true);// please wait
			final Handler handler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					updateFileList();
				}
			};

			new Thread() {
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
				client_adress = InetAddress.getByName(mIP_Adress.getText().toString());
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
	protected void processData(String data){
		if(mConnected){
			Log.d(TAG, "Hear rate data: "+data);
			if(heart_rate != null){
				heart_rate.setText(data);
				
				
				if(recording){
					hds.open();
					hds.addHeartRateSilent(new HeartRateData(Long.parseLong(data), System.currentTimeMillis(), accelerometerListener.run_id));
					hds.close();
					//new UDPThread().execute(data + ", " + System.currentTimeMillis());
				}
			}
		}
	}
	
	@Override
	protected void onBluetoothDisconnected(){
		Log.d(TAG, "Hear rate sensor disconnected");
		if(heart_rate != null){
			heart_rate.setText("-");
		}
		
		//connect.setVisibility(View.VISIBLE);
	}
	
	@Override
	protected void onBluetoothConnected(){
		Log.d(TAG, "Hear rate sensor connected");
		if(heart_rate != null){
			heart_rate.setText("...");
//			connect.setVisibility(View.GONE);
		}
	}
	
	/*
	 * Location specific changes from here
	 */
    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // Log the result
                        Log.d(LocationUtils.APPTAG, getString(R.string.resolved));

                        // Display the result
//                        mConnectionState.setText(R.string.connected);
//                        mConnectionStatus.setText(R.string.resolved);
                    break;

                    // If any other result was returned by Google Play services
                    default:
                        // Log the result
                        Log.d(LocationUtils.APPTAG, getString(R.string.no_resolution));

                        // Display the result
//                        mConnectionState.setText(R.string.disconnected);
//                        mConnectionStatus.setText(R.string.no_resolution);

                    break;
                }

            // If any other request code was received
            default:
               // Report that this Activity received an unknown requestCode
               Log.d(LocationUtils.APPTAG,
                       getString(R.string.unknown_activity_request_code, requestCode));

               break;
        }
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(LocationUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;
        // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), LocationUtils.APPTAG);
            }
            return false;
        }
    }
    

    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
            errorCode,
            this,
            LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(getSupportFragmentManager(), LocationUtils.APPTAG);
        }
    }

    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
    

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
       // mConnectionStatus.setText(R.string.connected);

        if (mUpdatesRequested) {
            startPeriodicUpdates();
        }
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
       // mConnectionStatus.setText(R.string.disconnected);
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {

            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    Marker marker;
    
    /**
     * Report location updates to the UI.
     *
     * @param location The updated location.
     */
    @Override
    public void onLocationChanged(Location location) {

        // Report to the UI that the location was updated
       // mConnectionStatus.setText(R.string.location_updated);

        // In the UI, set the latitude and longitude to the value received
       // mLatLng.setText(LocationUtils.getLatLng(this, location));
    	Log.d(TAG, "Location: "+location.getLatitude() + " "+location.getLongitude());
    	
    	LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        mMap.animateCamera(cameraUpdate);
        if(marker == null){
	        marker = mMap.addMarker(new MarkerOptions()
	        .position(latLng)
	        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }else{
        	animateMarker(marker, latLng, false);
        }
        
        //update location in DB

    }
    
    public void animateMarker(final Marker marker, final LatLng toPosition,
            final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final LinearInterpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    /**
     * In response to a request to start updates, send a request
     * to Location Services
     */
    private void startPeriodicUpdates() {

        mLocationClient.requestLocationUpdates(mLocationRequest, this);
        //mConnectionState.setText(R.string.location_requested);
    }

    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);
       // mConnectionState.setText(R.string.location_updates_stopped);
    }
    
    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {

        super.onStart();

        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        mLocationClient.connect();

    }
    
    /*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    public void onStop() {

        // If the client is connected
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }

        // After disconnect() is called, the client is considered "dead".
        mLocationClient.disconnect();

        super.onStop();
    }
    
    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;


    /*
     * Note if updates have been turned on. Starts out as "false"; is set to "true" in the
     * method handleRequestSuccess of LocationUpdateReceiver.
     *
     */
    boolean mUpdatesRequested = true;

    /**
     * Invoked by the "Start Updates" button
     * Sends a request to start location updates
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void startUpdates() {
        mUpdatesRequested = true;
        map_layout.setVisibility(View.VISIBLE);
        //mMap.setMyLocationEnabled(true);
        
        if (servicesConnected()) {
            startPeriodicUpdates();
        }
    }

    /**
     * Invoked by the "Stop Updates" button
     * Sends a request to remove location updates
     * request them.
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void stopUpdates() {
        mUpdatesRequested = false;

        map_layout.setVisibility(View.GONE);
       // mMap.setMyLocationEnabled(false);
        
        if (servicesConnected()) {
            stopPeriodicUpdates();
        }
    }
    
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                                .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                // The Map is verified. It is now safe to manipulate the map.

            }
        }
    }


}
