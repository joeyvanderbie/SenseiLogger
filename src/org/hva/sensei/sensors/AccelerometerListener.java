package org.hva.sensei.sensors;

import java.util.ArrayList;

import org.hva.sensei.data.AccelData;
import org.hva.sensei.data.HeartRateData;
import org.hva.sensei.db.AccelDataSource;
import org.hva.sensei.db.HeartRateDataSource;
import org.hva.sensei.logger.MainMovementActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class AccelerometerListener implements SensorEventListener {

	private int heart_rate = 0;
    private long startTime;
    private int numSamples;
    private boolean isActive = false;
    private double samplingRate = 0.0;
    private MainMovementActivity accelerometerTest;
    private ArrayList<AccelData> samples;
    private AccelDataSource ads;
    private HeartRateDataSource hds;
    public int run_id = 0;
    private long sensorTimeReference = 0l;
    private long myTimeReference = 0l;
    private Handler handler;
    String values[] = new String[5];
    long now = 0l;
    
    public AccelerometerListener(MainMovementActivity accelerometerTest) {
        this.accelerometerTest = accelerometerTest;
        
    }
    public double getSamplingRate() {
        return samplingRate;
    }
    
    public ArrayList<AccelData> getSamples(){
    	return samples;
    }
    
    public void startRecording() {
        startTime = System.currentTimeMillis();
        numSamples = 0;
        sensorTimeReference = 0l;
        myTimeReference = 0l;
        
        isActive = true;
        this.samples = new ArrayList<AccelData>();
        ads = new AccelDataSource(accelerometerTest);
    	hds = new HeartRateDataSource(accelerometerTest);
        ads.open();
        run_id = ads.getLastAccelRunId()+1;
        ads.close();
        handler = new Handler();
        
        
		
    }
    
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isActive) {
            numSamples++;
            now = System.currentTimeMillis();
            
            if((now - startTime) % 500 < 100 ){
            	samplingRate = numSamples / ((now - startTime) / 1000.0);      
            	 accelerometerTest.displayRates();
            	 
            	 //update heartrate
            	 hds.open();
            	 heart_rate =  hds.getLastHeartRate(run_id);
				hds.close();            	 
            }
            
          //  if (now >= startTime + 5000) {
//            if(numSamples >= 10){
//                samplingRate = numSamples / ((now - startTime) / 1000.0);
//                startTime = now;
//                numSamples = 0;
//                
//                accelerometerTest.displayRates();
//                Log.d("AcceleromterTest", "displayrate: "+samplingRate);
//                final ArrayList<AccelData> upload = (ArrayList<AccelData>) samples.clone();
//                //waarschijnlijk moet dit in een aparte thread omdat het teveel invloed heeft op de sampling rate
//                
//                Handler handler = new Handler();
//                Runnable r = new Runnable() {
//					
//					@Override
//					public void run() {
//						//add samples to database
//		                ads.open();
//		               // ads.addAccelDataList(samples, 0, run_id);
//		                ads.addAccelDataListFast(upload, 0, run_id);
//		    			ads.close();
//					}
//				};
//				handler.post(r);
//                
//                samples = new ArrayList<AccelData>();
//                Log.d("AcceleromterTest", "Uploading to database");
//
//            }
            
            if (numSamples >= 100) {
            	 startTime = now;
            	//new ProgressTaskAccelData().execute(samples);
            	
            	numSamples = 0;
            	// samples = new ArrayList<AccelData>();
               //Do Garbage Collection to make sure delay of GC is not longer than 50ms
               System.gc();
            }
            
            if(sensorTimeReference == 0l && myTimeReference == 0l) {
                sensorTimeReference = event.timestamp;
                myTimeReference = System.currentTimeMillis();
            }
            // set event timestamp to current time in milliseconds
            event.timestamp = myTimeReference + 
                Math.round((event.timestamp - sensorTimeReference) / 1000000.0);
           // samples.add(new AccelData(event.timestamp, event.values[0], event.values[1], event.values[2], run_id));

           

//            values[0] =  String.valueOf(event.timestamp);
//            values[1] =  String.valueOf(event.values[0]);
//            values[2] =  String.valueOf(event.values[1]);
//            values[3] =  String.valueOf(event.values[2]);
//            values[4] =  String.valueOf(run_id);
//            
//            
//            new ProgressTask().execute(values);
           // Log.d("AcceleromterTest", event.values[0] + " " + event.values[1] + " " + event.values[2] + " " + event.timestamp );
//            if(samples.size() > 2){
//            Log.d("Delta t", ""+(samples.get(samples.size()-2).getTimestamp()-event.timestamp) );
//            }
        	new UDPThread().execute(event.values[0] + ", " + event.values[1] + ", " + event.values[2] + ", " + event.timestamp + ", "+ heart_rate);
        }
    }
    
    public void submitLastSensorData(){
    	if(ads != null){
        	new ProgressTaskAccelData().execute(samples);
        	numSamples = 0;

			samples = new ArrayList<AccelData>();
    	}
    }
    
    public void stopRecording(){
    	isActive = false;
    	//submitLastSensorData();
    }
    
	public class ProgressTask extends AsyncTask<String, Void, String> {
		@Override
		protected void onPreExecute() {
			
		}

		@Override
		protected String doInBackground(String... params) {
			ads.open();
	        ads.add(new AccelData(Long.parseLong(params[0]), Double.parseDouble(params[1]), Double.parseDouble(params[2]), Double.parseDouble(params[3]), Long.parseLong(params[4])), 0, 0);
			ads.close();
			
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
		}
	}
	
	public class ProgressTaskAccelData extends AsyncTask<ArrayList<AccelData>, Void, String> {
		@Override
		protected void onPreExecute() {
			
		}

		@Override
		protected String doInBackground(ArrayList<AccelData>... params) {
			ads.open();
            Log.d("AcceleromterTest", "Uploading to database");
	        ads.addAccelDataListFast(params[0], 0);
	        ads.close();
			
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
		}
	}

}
