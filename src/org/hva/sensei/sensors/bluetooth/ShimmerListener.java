//v0.2 -  8 January 2013

/*
 * Copyright (c) 2010, Shimmer Research, Ltd.
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:

 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of Shimmer Research, Ltd. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.

 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Jong Chern Lim
 * @date   October, 2013
 */

//Future updates needed
//- the handler should be converted to static 

package org.hva.sensei.sensors.bluetooth;

import java.util.ArrayList;
import java.util.Collection;

import org.hva.sensei.data.AccelData;
import org.hva.sensei.db.AccelDataSource;
import org.hva.sensei.logger.MainMovementActivity;
import org.hva.sensei.sensors.UDPThread;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.shimmerresearch.android.Shimmer;
import com.shimmerresearch.driver.FormatCluster;
import com.shimmerresearch.driver.ObjectCluster;


public class ShimmerListener {
    /** Called when the activity is first created. */
    String bluetoothAddress="00:06:66:A0:3B:33";
    private Shimmer mShimmerDevice1 = null;
    private long startTime;
    private long startTimeStamp = 0;
    private int numSamples;
    private double samplingRate = 0.0;
    private MainMovementActivity accelerometerTest;
    private ArrayList<AccelData> samples;
    private AccelDataSource ads;
    public int run_id = 0;
    private String TAG = "ShimmerListener";
    
    public ShimmerListener(MainMovementActivity activ){
    	this.accelerometerTest = activ;
        mShimmerDevice1 = new Shimmer(activ, mHandler,"RightArm", 100, 0, 0, Shimmer.SENSOR_ACCEL, false);  

    }
    
    
 // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) { // handlers have a what identifier which is used to identify the type of msg
            case Shimmer.MESSAGE_READ:
            	if ((msg.obj instanceof ObjectCluster)){	// within each msg an object can be include, objectclusters are used to represent the data structure of the shimmer device

					double x = 0;
					double y = 0;
					double z = 0;
					double timestamp = 0;

					  numSamples++;
			            long now = System.currentTimeMillis();

            		ObjectCluster objectCluster =  (ObjectCluster) msg.obj; 
            	    Collection<FormatCluster> accelXFormats = objectCluster.mPropertyCluster.get("Accelerometer X");  // first retrieve all the possible formats for the current sensor device
					FormatCluster formatCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(accelXFormats,"CAL")); // retrieve the calibrated data
					if (formatCluster!=null){
						Log.d("CalibratedData",objectCluster.mMyName + " AccelX: " + formatCluster.mData + " "+ formatCluster.mUnits);
						x = formatCluster.mData;
					}
					Collection<FormatCluster> accelYFormats = objectCluster.mPropertyCluster.get("Accelerometer Y");  // first retrieve all the possible formats for the current sensor device
					formatCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(accelYFormats,"CAL")); // retrieve the calibrated data
					if (formatCluster!=null){
						Log.d("CalibratedData",objectCluster.mMyName + " AccelY: " + formatCluster.mData + " "+formatCluster.mUnits);
						y = formatCluster.mData;
					}
					Collection<FormatCluster> accelZFormats = objectCluster.mPropertyCluster.get("Accelerometer Z");  // first retrieve all the possible formats for the current sensor device
					formatCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(accelZFormats,"CAL")); // retrieve the calibrated data
					if (formatCluster!=null){
						Log.d("CalibratedData",objectCluster.mMyName + " AccelZ: " + formatCluster.mData + " "+formatCluster.mUnits);
						z = formatCluster.mData;
					}

//					accelXFormats = objectCluster.mPropertyCluster.get("Low Noise Accelerometer X");  // first retrieve all the possible formats for the current sensor device
//					formatCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(accelXFormats,"CAL")); // retrieve the calibrated data
//					if (formatCluster!=null){
//						Log.d("CalibratedData",objectCluster.mMyName + " AccelLNX: " + formatCluster.mData + " "+ formatCluster.mUnits);
//						x = formatCluster.mData;
//					}
//					accelYFormats = objectCluster.mPropertyCluster.get("Low Noise Accelerometer Y");  // first retrieve all the possible formats for the current sensor device
//					formatCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(accelYFormats,"CAL")); // retrieve the calibrated data
//					if (formatCluster!=null){
//						Log.d("CalibratedData",objectCluster.mMyName + " AccelLNY: " + formatCluster.mData + " "+formatCluster.mUnits);
//						y = formatCluster.mData;
//					}
//					accelZFormats = objectCluster.mPropertyCluster.get("Low Noise Accelerometer Z");  // first retrieve all the possible formats for the current sensor device
//					formatCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(accelZFormats,"CAL")); // retrieve the calibrated data
//					if (formatCluster!=null){
//						Log.d("CalibratedData",objectCluster.mMyName + " AccelLNZ: " + formatCluster.mData + " "+formatCluster.mUnits);
//						z = formatCluster.mData;
//					}
					
					Collection<FormatCluster> datetimeFormat = objectCluster.mPropertyCluster.get("Timestamp");  // first retrieve all the possible formats for the current sensor devic
					datetimeFormat = objectCluster.mPropertyCluster.get("Timestamp");  // first retrieve all the possible formats for the current sensor device
					formatCluster = ((FormatCluster)ObjectCluster.returnFormatCluster(datetimeFormat,"CAL")); // retrieve the calibrated data
					if (formatCluster!=null){
						Log.d("CalibratedData",objectCluster.mMyName + " Datetimestamp: " + formatCluster.mData + " "+formatCluster.mUnits);
						timestamp = formatCluster.mData;
					}
					
					
					//update x, y, z, timestamp, datetimestamp
					
			            
			            if((now - startTime) % 500 < 100 ){
			            	samplingRate = numSamples / ((now - startTime) / 1000.0);      
			            	accelerometerTest.displayRates();
			            }
			            
			          //  if (now >= startTime + 5000) {
			            if(numSamples >= 100){
			                samplingRate = numSamples / ((now - startTime) / 1000.0);
			                startTime = now;
			                numSamples = 0;
			                
			                accelerometerTest.displayRates();
			                Log.d(TAG, "displayrate: "+samplingRate);
			                final ArrayList<AccelData> upload = (ArrayList<AccelData>) samples.clone();
			                //waarschijnlijk moet dit in een aparte thread omdat het teveel invloed heeft op de sampling rate
			                
			                new Runnable() {
								
								@Override
								public void run() {
									//add samples to database
					                ads.open();
					               // ads.addAccelDataList(samples, 0, run_id);
					                ads.addAccelDataListFast(upload, 0, run_id);
					    			ads.close();
								}
							}.run();
			                
			                samples = new ArrayList<AccelData>();
			                Log.d(TAG, "Uploading to database");

			            }
//			            if(sensorTimeReference == 0l && myTimeReference == 0l) {
//			                sensorTimeReference = event.timestamp;
//			                myTimeReference = System.currentTimeMillis();
//			            }
			            // set event timestamp to current time in milliseconds
//			            event.timestamp = myTimeReference + 
//			                Math.round((event.timestamp - sensorTimeReference) / 1000000.0);
			            samples.add(new AccelData((long)(startTimeStamp + timestamp),x, y, z, run_id));

			            Log.d(TAG, x + " " + y + " " + z + " "+ run_id);
			        	new UDPThread().execute(x + ", " +y + ", " + z + ", " + startTimeStamp + timestamp + ", "+ run_id);

            	}
                break;
                 case Shimmer.MESSAGE_TOAST:
                	Log.d("toast",msg.getData().getString(Shimmer.TOAST));
                break;

                 case Shimmer.MESSAGE_STATE_CHANGE:
                	 switch (msg.arg1) {
                     	case Shimmer.MSG_STATE_FULLY_INITIALIZED:
                    	    if (mShimmerDevice1.getShimmerState()==Shimmer.STATE_CONNECTED){
                    	        Log.d("ConnectionStatus","Successful");
                    	        
                    	        startTime = System.currentTimeMillis();
                    	        startTimeStamp = System.currentTimeMillis();
                    	        numSamples = 0;
                    	        samples = new ArrayList<AccelData>();
                    	        ads = new AccelDataSource(accelerometerTest);
                    	        ads.open();
                    	        run_id = ads.getLastAccelRunId()+1;
                    	        
                    	        mShimmerDevice1.startStreaming();
                    	       // shimmerTimer(10); //Disconnect in 30 seconds
                    	     }
                    	    break;
	                    case Shimmer.STATE_CONNECTING:
	                    	Log.d("ConnectionStatus","Connecting");
                	        break;
	                    case Shimmer.STATE_NONE:
	                    	Log.d("ConnectionStatus","No State");
	                    	break;
                     }
                break;
                
            }
        }
    };
	    
	    public void start(){
	        Log.d("ConnectionStatus","Trying"); 
	         mShimmerDevice1.connect(bluetoothAddress,"default"); 
	    }
	    
	    public void stop(){
	    	mShimmerDevice1.stopStreaming(); 
        	mShimmerDevice1.stop(); 
        	 submitLastSensorData();
	    }
	    
	    public void submitLastSensorData(){
	    	if(ads != null){
	    		ads.open();
		    		// ads.addAccelDataList(samples, 0, run_id);
	    		ads.addAccelDataListFast(samples, 0, run_id);
				ads.close();
				samples = new ArrayList<AccelData>();
	    	}
	    }
	    
	    public double getSamplingRate() {
	        return samplingRate;
	    }
	    
	    public ArrayList<AccelData> getSamples(){
	    	return samples;
	    }
    }
    



    
    