package org.hva.sensei.logger;

import org.hva.cityrunner.sensei.sensors.AccelerometerListener;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	    TextView textView;
	    Button button1;
	    Button button2;
	    SensorManager sensorManager;
	    Sensor accelerometer;
	    Sensor uiAccelerometer;

	    AccelerometerListener accelerometerListener;
	    private int delayInMicroseconds = 45000; //for 20Hz sampling rate

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_main);

	        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	        accelerometer = sensorManager
	                .getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
	        
	        accelerometerListener = new AccelerometerListener(this);
	        sensorManager.registerListener(accelerometerListener, accelerometer, delayInMicroseconds);

	        textView = (TextView) findViewById(R.id.text_view);
	        button1 = (Button) findViewById(R.id.button1);
	        button1.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
				        button1.setEnabled(false);
				        button2.setEnabled(true);
				        textView.setText("Working...");
				        accelerometerListener.startRecording(0);
				}
			});
	        button2 = (Button) findViewById(R.id.button2);
	        button2.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
						button1.setEnabled(true);
				        button2.setEnabled(false);
				        accelerometerListener.stopRecording();
				}
			});
	        
	    }

	    protected void onResume() {
	        super.onResume();
	        sensorManager.registerListener(accelerometerListener, accelerometer,
	                delayInMicroseconds);
	    }

	    protected void onPause() {
	        super.onPause();
	        sensorManager.unregisterListener(accelerometerListener);
	    }

	    public void displayRates() {
	      //  button1.setEnabled(true);

	        textView.setText(String.format(
	                "Sampling rate: %.2f", accelerometerListener.getSamplingRate()));
	    }

	    
	}

