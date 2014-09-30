package org.hva.sensei.sensors.bluetooth;

import java.util.List;

import org.hva.sensei.logger.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class BluetoothHeartRateActivity extends Activity{

	protected boolean mConnected;
	public String mDeviceAddress = "C3:4D:F2:BD:3B:63"; //mio
			// TRKR "E5:86:D2:0E:8E:E6";
			
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private BluetoothGattCharacteristic mNotifyCharacteristic;
	protected BluetoothLeService mBluetoothLeService;

	private BluetoothAdapter mBluetoothAdapter;
	protected final static String TAG = BluetoothHeartRateActivity.class
			.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private boolean mScanning;
    private Handler mHandler;

	// Code to manage Service lifecycle.
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				// finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.

			Log.d(TAG, "onServiceConnected connecting to adress " + mDeviceAddress);
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
			Log.d(TAG, "onServiceConnected disconnected");
		}
	};

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device. This can be a
	// result of read
	// or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				// updateConnectionState(R.string.connected);
				onBluetoothConnected();
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				mConnected = false;
				// updateConnectionState(R.string.disconnected);
				onBluetoothDisconnected();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				// Show all the supported services and characteristics on the
				// user interface.
				displayGattServices(mBluetoothLeService
						.getSupportedGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				 processData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			}
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

      //  final Intent intent = getIntent();
      //  mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mHandler = new Handler();
        
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        reconnectBluetooth();
       
    }
    
    protected void reconnectBluetooth(){
    	 if(bluetoothLECompatible()){
    	        
 	        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
 	        // fire an intent to display a dialog asking the user to grant permission to enable it.
 	       if (!mBluetoothAdapter.isEnabled()) {
 	            if (!mBluetoothAdapter.isEnabled()) {
 	                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
 	                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
 	            }
 	        }
 	        
 	        
 	        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
 	        if (mBluetoothLeService != null) {
 	            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
 	            Log.d(TAG, "Connect request result=" + result);
 	        }else{
 	        	Log.d(TAG, "No BluetoothLeService");
 	        	  scanLeDevice(true);
 	        	
 	        }
         }
    }
    
    protected void disconnectBluetooth(){
    	unregisterReceiver(mGattUpdateReceiver);

    	try{
    		unbindService(mServiceConnection);
    	}catch(IllegalArgumentException e){
    		Log.d(TAG, e.toString());
    	}
    		mBluetoothLeService = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
       // unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBluetooth();
    }
    
	// Demonstrates how to iterate through the supported GATT
	// Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the
	// ExpandableListView
	// on the UI.
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;
		String uuid = null;

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService
					.getCharacteristics();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				 uuid = gattCharacteristic.getUuid().toString();
				 if (uuid.equals(GattAttributes.HEART_RATE_MEASUREMENT)) {
					listenToCharacteristic(gattCharacteristic);
					//stop loop, no need to proceed
					return;
				 }
			}
		}
	}

	private boolean listenToCharacteristic(
			BluetoothGattCharacteristic characteristic) {
			final int charaProp = characteristic.getProperties();
			if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
				// If there is an active notification on a characteristic, clear
				// it first so it doesn't update the data field on the user
				// interface.
				if (mNotifyCharacteristic != null) {
					mBluetoothLeService.setCharacteristicNotification(
							mNotifyCharacteristic, false);
					mNotifyCharacteristic = null;
				}
				mBluetoothLeService.readCharacteristic(characteristic);
			}
			if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
				mNotifyCharacteristic = characteristic;
				mBluetoothLeService.setCharacteristicNotification(
						characteristic, true);
			}
			return true;
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter
				.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}
	
	protected void processData(String data) {
        //process heart rate data
    }
	
	protected void onBluetoothDisconnected(){
		//process on heart rate disconnected event
	}
	
	protected void onBluetoothConnected(){
		//process on heart rate connected event
	}
	
	private boolean bluetoothLECompatible(){
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
	
	 protected void scanLeDevice(final boolean enable) {
	    	if(bluetoothLECompatible()){
		        if (enable) {
		            // Stops scanning after a pre-defined scan period.
		            mHandler.postDelayed(new Runnable() {
		                @Override
		                public void run() {
		                    mScanning = false;
		                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
		                    invalidateOptionsMenu();
		                }
		            }, SCAN_PERIOD);
		
		            mScanning = true;
		            mBluetoothAdapter.startLeScan(mLeScanCallback);
		        } else {
		            mScanning = false;
		            mBluetoothAdapter.stopLeScan(mLeScanCallback);
		        }
		        invalidateOptionsMenu();
	    	}
	    }
	 
	// Device scan callback.
	    private BluetoothAdapter.LeScanCallback mLeScanCallback =
	            new BluetoothAdapter.LeScanCallback() {

	        @Override
	        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
	            runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
	                   // mLeDeviceListAdapter.addDevice(device);
	                   // mLeDeviceListAdapter.notifyDataSetChanged();
	                	if (device == null) return;
	                	Log.d(TAG, "leScanCallBack device address =" + device.getAddress());
	                	if(device.getAddress().equals(mDeviceAddress)){
	                		 registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	             	        if (mBluetoothLeService != null) {
	             	            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
	             	            Log.d(TAG, "Connect request result=" + result);
	             	        }
	                	}
	                	
	                    if (mScanning) {
	                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
	                        mScanning = false;
	                    }
	                }
	            });
	        }
	    };
	 
	 @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	        // User chose not to enable Bluetooth.
	        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
	            finish();
	            return;
	        }
	        super.onActivityResult(requestCode, resultCode, data);
	    }
	 
	 public void setDeviceAdress(String address){
		 mDeviceAddress = address;
	 }
}
