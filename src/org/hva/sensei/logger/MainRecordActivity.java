package org.hva.sensei.logger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.hva.sensei.sensors.record.AudioRecorder;
import org.hva.sensei.sensors.record.VuMeterView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
	private TextView recording_duration_icon;

	private int recording_duration = 30;
	private int recording_frequency = 300;

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
			final TextView durationText = (TextView) findViewById(R.id.recording_timer_info);
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
						onClickRecord(true);
					}
				});

			}

		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_voice);

		record_button = (ToggleButton) findViewById(R.id.button_start);
		record_button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				recording_duration = Integer.parseInt(recording_duration_view
						.getText().toString());
				recording_frequency = Integer.parseInt(recording_frequency_view
						.getText().toString());
				//onClickRecord(((ToggleButton) v).isChecked());
				startRecordingWithFrequency(((ToggleButton) v).isChecked());
			}
		});

		recording_duration_view = (TextView) findViewById(R.id.recording_duration);
		recording_duration_view.setText("" + recording_duration);
		recording_frequency_view = (TextView) findViewById(R.id.recording_frequency);
		recording_frequency_view.setText("" + recording_frequency);
		recording_duration_icon = (TextView) findViewById(R.id.recording_timer_info_icon);

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
		if (record_button.isChecked()) {
			mRecordingFrequencyStartTime = System.currentTimeMillis();

			// force disabled before recording
			record_button.setChecked(false);
			record_button.setEnabled(false);
			//
			frequencyTimer = new Timer();
			frequencyTimer.schedule(new RecordFrequencyTask(), 10, 50);
		} else {
			record_button.setChecked(true);
			record_button.setEnabled(false);

			// stop recording
			onClickRecord(false);

			// stop frequency trigger
			frequencyTimer.cancel();

			record_button.setChecked(false);
			record_button.setEnabled(true);
		}
	}

	private void onClickRecord(boolean start) {
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
//				record_button.setEnabled(true);

				recording_duration_icon.setVisibility(View.VISIBLE);
				
				recordingTimer = new Timer();
				recordingTimer.schedule(new RecordTask(), 10, 50);

				mRecordingStartTime = System.currentTimeMillis();
			}
		} else {
//			record_button.setChecked(true);
//			record_button.setEnabled(false);

			try {
				recordingTimer.cancel();
				mRecorder.stop();

				// ToggleButton playBtn = (ToggleButton)
				// findViewById(R.id.btnPlayLastRecord);
				// playBtn.setEnabled(true);
				// playBtn.setAlpha(1f);
			} catch (IOException e) {
				makeAlert("Error while stopping recording:\n" + e.getMessage());
//				record_button.setChecked(true);
//				record_button.setEnabled(true);
			} finally {
//				record_button.setChecked(false);
//				record_button.setEnabled(true);

				recording_duration_icon.setVisibility(View.GONE);
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

}