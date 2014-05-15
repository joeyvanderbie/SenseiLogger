package org.hva.sensei.sensors;

import java.util.ArrayList;

import org.hva.sensei.data.AccelData;
import org.hva.sensei.db.AccelDataSource;
import org.hva.sensei.logger.MainMovementActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

public class AccelerometerListener implements SensorEventListener {

    private long startTime;
    private int numSamples;
    private boolean isActive = false;
    private double samplingRate = 0.0;
    private MainMovementActivity accelerometerTest;
    private ArrayList<AccelData> samples;
    private AccelDataSource ads;
    public int run_id = 0;
    private long sensorTimeReference = 0l;
    private long myTimeReference = 0l;
    
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
        isActive = true;
        this.samples = new ArrayList<AccelData>();
        ads = new AccelDataSource(accelerometerTest);
        ads.open();
        run_id = ads.getLastAccelRunId()+1;
		
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
            long now = System.currentTimeMillis();
            
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
                Log.d("AcceleromterTest", "displayrate: "+samplingRate);
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
                Log.d("AcceleromterTest", "Uploading to database");

            }
//            if(sensorTimeReference == 0l && myTimeReference == 0l) {
//                sensorTimeReference = event.timestamp;
//                myTimeReference = System.currentTimeMillis();
//            }
            // set event timestamp to current time in milliseconds
//            event.timestamp = myTimeReference + 
//                Math.round((event.timestamp - sensorTimeReference) / 1000000.0);
            
            //dit moet misschien toch de code hierboven zijn
            samples.add(new AccelData(now, event.values[0], event.values[1], event.values[2], run_id));

            Log.d("AcceleromterTest", event.values[0] + " " + event.values[1] + " " + event.values[2] + " " + run_id);
        	new UDPThread().execute(event.values[0] + ", " + event.values[1] + ", " + event.values[2] + ", " + now + ", "+ run_id);
        }
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
    
    public void stopRecording(){
    	isActive = false;
    	submitLastSensorData();
    }
}
