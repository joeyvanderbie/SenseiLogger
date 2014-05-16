package org.hva.sensei.logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.hva.sensei.sensors.record.AudioRecorder;
import org.hva.sensei.sensors.record.ToneGenerator;
import org.hva.sensei.sensors.record.VuMeterView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainRecordActivity extends Activity {
	private AudioRecorder mRecorder = null;
	private Timer recordingTimer = null;
	private Timer frequencyTimer = null;
	private RecordTriggerAfterSpeech rc = null;
	private TextToSpeech ttobj;
	private long mRecordingStartTime = 0;
	private long mRecordingFrequencyStartTime = 0;
	private String mLastFileName = "";
	private MediaPlayer mMediaPlayer = null;
	String PATH_TO_FILES = Environment.getExternalStorageDirectory()
			+ "/Sensei/Recording/";
	String fileExt = ".wav";
	private ToggleButton record_button;
	private TextView recording_duration_view;
	private TextView recording_frequency_view;
	private TextView recording_frequency_count_view;
	private TextView recording_duration_icon;
	private TextView recording_message;

	private int recording_duration = 5;
	private int recording_frequency = 5;
	private int recording_frequency_count = 5;
	private int current_frequency_count = 0;

    Handler audioHandler = new Handler();

	class RecordTask extends TimerTask {
		public void run() {
			final VuMeterView vuMeter = (VuMeterView) findViewById(R.id.vuMeterView);
			final TextView durationText = (TextView) findViewById(R.id.recording_timer_info);

			vuMeter.setValue(mRecorder.getAmplitude());

			final long duration = (recording_duration * 1000)
					- (System.currentTimeMillis() - mRecordingStartTime);

			runOnUiThread(new Runnable() {
				public void run() {
					durationText.setText(String.format(
							"%02d:%02d",
							TimeUnit.MILLISECONDS.toMinutes(duration),
							TimeUnit.MILLISECONDS.toSeconds(duration)
									- TimeUnit.MINUTES
											.toSeconds(TimeUnit.MILLISECONDS
													.toMinutes(duration))));
					vuMeter.invalidate();
				}
			});

			if (duration <= 0) {
				runOnUiThread(new Runnable() {
					public void run() {
						onClickRecord(false);
						startRecordingWithFrequency(true);
					}
				});

			}

		}
	}

	class RecordFrequencyTask extends TimerTask {
		public void run() {
			final TextView durationText = (TextView) findViewById(R.id.time_info);
			final long duration = (recording_frequency * 1000)
					- (System.currentTimeMillis() - mRecordingFrequencyStartTime);

			runOnUiThread(new Runnable() {
				public void run() {
					durationText.setText(String.format(
							"%02d:%02d",
							TimeUnit.MILLISECONDS.toMinutes(duration),
							TimeUnit.MILLISECONDS.toSeconds(duration)
									- TimeUnit.MINUTES
											.toSeconds(TimeUnit.MILLISECONDS
													.toMinutes(duration))));
				}
			});

			if (duration <= 0) {
				runOnUiThread(new Runnable() {
					public void run() {
						//onClickRecord(true);
						startRecordingWithQuestion();
					}
				});

			}

		}
	}
	
	class RecordTriggerAfterSpeech extends UtteranceProgressListener{
		public String currentId;

		@Override
		public void onDone(String utteranceId) {
			ToneGenerator tone = new ToneGenerator();
			playBeepAndStartRecording(8000, tone.genTone());
		}

		@Override
		public void onError(String utteranceId) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStart(String utteranceId) {
			// TODO Auto-generated method stub
			
		}
		
		
	}
	
	
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_voice);

		rc = new RecordTriggerAfterSpeech();
	     ttobj=new TextToSpeech(getApplicationContext(), 
	 	        new TextToSpeech.OnInitListener() {
	 	        @Override
	 	        public void onInit(int status) {
	 	           if(status != TextToSpeech.ERROR){
	 	               ttobj.setLanguage(Locale.UK);
	 	              }	
	 	           }
	 	        });
	     ttobj.setOnUtteranceProgressListener(rc);
		
		record_button = (ToggleButton) findViewById(R.id.button_start);
		record_button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				recording_duration = Integer.parseInt(recording_duration_view
						.getText().toString());
				recording_frequency = Integer.parseInt(recording_frequency_view
						.getText().toString());
				recording_frequency_count = Integer.parseInt(recording_frequency_count_view
						.getText().toString());
				current_frequency_count = 0;
				//onClickRecord(((ToggleButton) v).isChecked());
				startRecordingWithFrequency(((ToggleButton) v).isChecked());
			}
		});

		recording_duration_view = (TextView) findViewById(R.id.recording_duration);
		recording_duration_view.setText("" + recording_duration);
		recording_frequency_view = (TextView) findViewById(R.id.recording_frequency);
		recording_frequency_view.setText("" + recording_frequency);
		recording_frequency_count_view = (TextView) findViewById(R.id.recording_frequency_count);
		recording_frequency_count_view.setText("" + recording_frequency_count);
		
		
		recording_message = (TextView) findViewById(R.id.recording_message);
		
		updateFileList();


	}

	public void makeAlert(final String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message).setCancelable(false)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void onClickPlay(View v) {
		if (mMediaPlayer == null) {
			mMediaPlayer = new MediaPlayer();
			String PATH_TO_FILE = PATH_TO_FILES + mLastFileName;
			try {
				mMediaPlayer.setDataSource(PATH_TO_FILE);
				mMediaPlayer.prepare();
			} catch (IllegalArgumentException e) {
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			} catch (IllegalStateException e) {
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			}

			mMediaPlayer.start();
		} else {
			mMediaPlayer.stop();
			mMediaPlayer = null;
		}
	}

	private void startRecordingWithFrequency(boolean start) {
		if (start && current_frequency_count < recording_frequency_count) {
			current_frequency_count++;
			mRecordingFrequencyStartTime = System.currentTimeMillis();

			// force disabled before recording
			record_button.setChecked(true);
			

			record_button.setEnabled(false);
			frequencyTimer = new Timer();
			record_button.setEnabled(true);
			frequencyTimer.schedule(new RecordFrequencyTask(), 10, 50);
		} else {
			record_button.setChecked(false);

			// stop recording
			onClickRecord(false);

			// stop frequency trigger
			frequencyTimer.cancel();

			record_button.setChecked(false);
			record_button.setEnabled(true);
		}
	}
	
	private void startRecordingWithQuestion(){
		frequencyTimer.cancel();
		
		//tts play
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UniqueID");

		String toSpeak = recording_message.getText().toString();
           ttobj.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, map);
	}

	protected void onClickRecord(boolean start) {
//		record_button.setChecked(start);
		//if (record_button.isChecked()) {
		if(start){
			// force disabled before recording
//			record_button.setChecked(false);
//			record_button.setEnabled(false);

			// prepare recording
			SimpleDateFormat fileNameDate = new SimpleDateFormat();
			fileNameDate.applyPattern("yyyy-MM-dd HH.mm.ss");
			// mLastFileName = fileNameDate.format(new Date()) + ".3gp";
			mLastFileName = fileNameDate.format(new Date()) + fileExt;
			mRecorder = new AudioRecorder(PATH_TO_FILES + mLastFileName);

			// start recording
			try {
				mRecorder.start();
			} catch (IOException e) {
				makeAlert("Error while starting recording:\n" + e.getMessage());
//				record_button.setChecked(false);
//				record_button.setEnabled(true);
			} finally {
//				record_button.setChecked(true);
				record_button.setEnabled(true);

				//recording_duration_icon.setVisibility(View.VISIBLE);
				
				recordingTimer = new Timer();
				recordingTimer.schedule(new RecordTask(), 10, 50);

				mRecordingStartTime = System.currentTimeMillis();
			}
		} else {
//			record_button.setChecked(true);
//			record_button.setEnabled(false);

			try {
				
				
				if(recordingTimer != null){
					recordingTimer.cancel();
				}
				if(mRecorder.isActive()){
					mRecorder.stop();

					ToneGenerator tone = new ToneGenerator();
					tone.playSound(8000, tone.genTone());
				}
				
				
				// ToggleButton playBtn = (ToggleButton)
				// findViewById(R.id.btnPlayLastRecord);
				// playBtn.setEnabled(true);
				// playBtn.setAlpha(1f);
			} catch (IOException e) {
				makeAlert("Error while stopping recording:\n" + e.getMessage());
//				record_button.setChecked(true);
//				record_button.setEnabled(true);
			} catch (NullPointerException e) {
				Log.d("RecordingActivity","No recorder object");
			}finally {
//				record_button.setChecked(false);
//				record_button.setEnabled(true);

				//recording_duration_icon.setVisibility(View.GONE);
				updateFileList();
			}
		}
	}

	private void updateFileList() {
		File dir = new File(PATH_TO_FILES);
		List<String> list = Util.getSortedFilenames(dir, fileExt);
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
					sharingIntent.setType("audio/wav");
					sharingIntent.putExtra(
							android.content.Intent.EXTRA_SUBJECT,
							getResources().getString(R.string.share_title));
					sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT,
							getResources().getString(R.string.share_body));
					sharingIntent.putExtra(Intent.EXTRA_STREAM,
							Uri.parse("file:///" + PATH_TO_FILES + "/" + file));
					startActivity(Intent.createChooser(
							sharingIntent,
							getResources().getString(
									R.string.share_dialog_title)));
				}
			});
			files.addView(b);
		}
	}
	
	 public void playBeepAndStartRecording(int sampleRate, byte[] generatedSnd){
	        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
	        		sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
	                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
	                AudioTrack.MODE_STATIC);

	        audioTrack.write(generatedSnd, 0, generatedSnd.length);
	        audioTrack.play();
	        
	        audioHandler.postDelayed(new Runnable(){
	            public void run() {
	            	onClickRecord(true);
	          }}, generatedSnd.length / sampleRate * 1000);

	    }

}