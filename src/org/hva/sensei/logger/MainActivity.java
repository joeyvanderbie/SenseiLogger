package org.hva.sensei.logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hva.cityrunner.sensei.data.AccelData;
import org.hva.cityrunner.sensei.db.AccelDataSource;
import org.hva.cityrunner.sensei.sensors.AccelerometerListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

	    TextView textView;
	    Button button1;
	    Button button2;
	    Button button3;
	    SensorManager sensorManager;
	    Sensor accelerometer;
	    Sensor uiAccelerometer;
	    String accelPath = Environment.getExternalStorageDirectory()
                +"/Sensei/Accelerometer";

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
				        accelerometerListener.startRecording();
				}
			});
	        button2 = (Button) findViewById(R.id.button2);
	        button2.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
						button1.setEnabled(true);
				        button2.setEnabled(false);
				        accelerometerListener.stopRecording();
				        try {
							exportAcceltoCSV(accelerometerListener.run_id);
							updateFileList();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
			});
	        
			updateFileList();


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

	    private void updateFileList(){
	        File dir = new File(accelPath);
	        List<String> list = getSortedFilenames(dir);
	        LinearLayout files = (LinearLayout) findViewById(R.id.fileList);
	        files.removeAllViews();
	        for(final String file : list){
	        	Button b = new Button(this);
	        	b.setText(this.getString(R.string.share_prefix_button) + file);
	        	b.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						 Intent sharingIntent = new Intent(
				                    android.content.Intent.ACTION_SEND);
				            sharingIntent.setType("text/csv");
				            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
				                    getResources().getString(R.string.share_title));
				            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT,
				                    getResources().getString(R.string.share_body));
				            sharingIntent.putExtra(Intent.EXTRA_STREAM,
				                    Uri.parse("file:///"+accelPath + "/" + file));
				            startActivity(Intent.createChooser(sharingIntent, getResources()
				                    .getString(R.string.share_dialog_title)));
					}
				});
	        	files.addView(b);
	        }
	    }
	    
	    public void displayRates() {
	      //  button1.setEnabled(true);

	        textView.setText(String.format(
	                "Sampling rate: %.2f", accelerometerListener.getSamplingRate()));
	    }
	    
	    public void exportAcceltoCSV(final int runId) throws IOException {
	        {

	            File folder = new File(accelPath);

	            folder.mkdirs();
	           


	            final String filename = folder.toString() + "/" + "Accel"+runId+".csv";

	            // show waiting screen
	            CharSequence contentTitle = getString(R.string.app_name);
	            final ProgressDialog progDailog = ProgressDialog.show(
	                    this, contentTitle, "even geduld aub...",
	                    true);//please wait
	            final Handler handler = new Handler() {
	                @Override
	                public void handleMessage(Message msg) {




	                }
	            };

	            new Thread() {
	                public void run() {
	                    try {

	                       // FileWriter fw = new FileWriter(filename);
	        	            FileOutputStream fOut = new FileOutputStream(filename);

	        	            OutputStreamWriter osw = new OutputStreamWriter(fOut); 
	                    	
	                    	
	                        AccelDataSource ads = new AccelDataSource(MainActivity.this);
	                        ads.open();
	                       ArrayList<AccelData> allAccel = ads.getAllAccel(runId, ads.getAllAccelCount(runId), 0);
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

	                        for(AccelData oneAccel : allAccel){
	                                osw.append(""+oneAccel.getX());
	                                osw.append(',');

	                                osw.append(""+oneAccel.getY());
	                                osw.append(',');

	                                osw.append(""+oneAccel.getZ());
	                                osw.append(',');

	                                osw.append(""+oneAccel.getTimestamp());
	                                osw.append(',');

	                                osw.append(""+oneAccel.getRun_id());
	                                osw.append(',');

	                                osw.append(""+oneAccel.getId());

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
		
		private List<String> getSortedFilenames(File dir,String sub) {
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
		
		private void readDirectory(File dir, final List<String> list, String parent, String extension) {
			if (dir != null && dir.canRead()) {
				File[] files = dir.listFiles();
				if (files != null) {
					for (File f : files) {
						if (f.getName().toLowerCase().endsWith(extension)) { //$NON-NLS-1$
							list.add(parent + f.getName());
						} else if (f.isDirectory()) {
							readDirectory(f, list, parent + f.getName() + "/", extension);
						}
					}
				}
			}
		}
	}

