package org.hva.sensei.sensors.record;


import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;

import org.hva.sensei.logger.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class RecordService extends Service
{
	static final private String TAG = RecordService.class.getSimpleName(); 
	static final private String PLAY_ACTION = "PlayMusic";
	static final private String STOP_PLAY_ACTION = "stoprecording";
	static final int record_msg = 1;
	static final int stop_record_msg = 0;
	
	private AudioRecorder mRecorder = null;
	String PATH_TO_FILES = Environment.getExternalStorageDirectory()
			+ "/Sensei/Recording/";
	String fileExt = ".wav";
	private String mLastFileName = "";
	private Timer recordingTimer = null;
	private long mRecordingStartTime = 0;
	PendingIntent startRecordIntent;
	PendingIntent stopRecordIntent;
	private int recording_duration = 10 * 1000;
	private int recording_frequency = 30 * 1000; //180
	int beep_duration = 2000;
	private PlayActionReceiver playActionReceiver;
	private StopRecordActionReceiver stopRecordActionReceiver;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{		
		startPeriodicUpdates();
		registerReceiver(playActionReceiver, new IntentFilter(PLAY_ACTION));
		registerReceiver(stopRecordActionReceiver, new IntentFilter(STOP_PLAY_ACTION));
		return START_STICKY;
	}
	
	@Override
	public void onCreate()
	{
		playActionReceiver = new PlayActionReceiver();
		Intent playIntent = new Intent(PLAY_ACTION);              
		startRecordIntent = PendingIntent.getBroadcast(getApplicationContext(), 1,  
													 playIntent, 
													 PendingIntent.FLAG_UPDATE_CURRENT);
		stopRecordActionReceiver = new StopRecordActionReceiver();
		Intent stopIntent = new Intent(STOP_PLAY_ACTION);
		stopRecordIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		super.onCreate();
	}
	
	@Override
	public void onDestroy()
	{		
		stopPeriodicUpdates();
		unregisterReceiver(playActionReceiver);
		unregisterReceiver(stopRecordActionReceiver);
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent arg0)
	{
		return null;
	}
	
	class PlayActionReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context ctx, Intent intent)
		{
			PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
			final WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "RecordStartBeep");
			wakeLock.acquire();
						
			Log.d(TAG, "Broadcast received!");
			Thread thread = new Thread(new Runnable()
			{				
				@Override
				public void run()
				{															
					onPlay();
					wakeLock.release();					
				}
			});
			
			thread.start();
		}
	}
	
	class StopRecordActionReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context ctx, Intent intent)
		{
			PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
			final WakeLock wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "recordStopBeep");
			wakeLock.acquire();
						
			Log.d(TAG, "Broadcast received!");
			Thread thread = new Thread(new Runnable()
			{				
				@Override
				public void run()
				{															
					stopRecord();
					wakeLock.release();					
				}
			});
			
			thread.start();
		}
	}
	
	private void startPeriodicUpdates()
	{
		Log.d(TAG, "Start periodic updates");
		AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, recording_frequency+1000, recording_frequency, startRecordIntent);	
		

		//stop recording after x seconds
		Log.d(TAG, "Start alarm to stop recording");
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, recording_frequency+1000+recording_duration+beep_duration, recording_frequency, stopRecordIntent);	
	}
	
	private void stopPeriodicUpdates()
	{
		Log.d(TAG, "Stop periodic updates");
		AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarmManager.cancel(startRecordIntent);
		alarmManager.cancel(stopRecordIntent)
;	}
	
	private void onPlay()
	{
		try
		{
			Log.d(TAG, "Playback started!");
			MediaPlayer player = new MediaPlayer();
			player.setOnErrorListener(new OnErrorListener()
			{				
				@Override
				public boolean onError(MediaPlayer arg0, int arg1, int arg2)
				{
					Log.e(TAG, "Player failed!");
					return false;
				}
			});
			
			player.setOnCompletionListener(new OnCompletionListener()
			{				
				@Override
				public void onCompletion(MediaPlayer arg0)
				{
					Log.d(TAG, "Playback stopped!");	
					record();
				}
			});			
			
			FileInputStream stream = new FileInputStream("/mnt/sdcard/beep.wav");
			player.setDataSource(stream.getFD());
			player.prepare();
			player.start();		
		}
		catch(IOException ex)
		{
			Log.e(TAG, "IOException", ex);
		}
	}
	
	public void record(){
		if(mRecorder !=null){
					if(mRecorder.isActive()){
						return;
					}
				}

				// prepare recording
				SimpleDateFormat fileNameDate = new SimpleDateFormat();
				fileNameDate.applyPattern("yyyy-MM-dd HH.mm.ss");
				// mLastFileName = fileNameDate.format(new Date()) + ".3gp";
				mLastFileName = fileNameDate.format(new Date()) + fileExt;
				mRecorder = new AudioRecorder(PATH_TO_FILES + mLastFileName);

				// start recording
				try {
					mRecorder.start();

					Log.d(TAG, "recording started");
				} catch (IOException e) {
					Log.e(TAG,"Error while starting recording:\n" + e.getMessage());
					// record_button.setChecked(false);
					// record_button.setEnabled(true);
				} finally {
					//update GUI
					//...
					
				}
	}
	
	public void stopRecord(){
				try {

					if (recordingTimer != null) {
						recordingTimer.cancel();
					}
					if (mRecorder.isActive()) {
						mRecorder.stop();

						Log.d(TAG, "recording stoppped");
					}
				} catch (IOException e) {
					Log.e(TAG,"Error while stopping recording:\n" + e.getMessage());
				} catch (NullPointerException e) {
					Log.d(TAG, "No recorder object");
				} 
				playBeep();
	}
			
	
	public void playBeep(){
		try{
		Log.d(TAG, "Playback started!");
		MediaPlayer player = new MediaPlayer();
		player.setOnErrorListener(new OnErrorListener()
		{				
			@Override
			public boolean onError(MediaPlayer arg0, int arg1, int arg2)
			{
				Log.e(TAG, "Player failed!");
				return false;
			}
		});
		
		player.setOnCompletionListener(new OnCompletionListener()
		{				
			@Override
			public void onCompletion(MediaPlayer arg0)
			{
				Log.d(TAG, "Playback stopped!");	
			}
		});			
		
		FileInputStream stream = new FileInputStream("/mnt/sdcard/beep.wav");
		player.setDataSource(stream.getFD());
		player.prepare();
		player.start();	
	}
	catch(IOException ex)
	{
		Log.e(TAG, "IOException", ex);
	}
	}
	
}
