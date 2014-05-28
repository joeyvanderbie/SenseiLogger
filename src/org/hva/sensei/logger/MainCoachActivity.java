package org.hva.sensei.logger;

import java.util.ArrayList;

import org.hva.sensei.coach.beacon.AbstractBeacon;
import org.hva.sensei.coach.beacon.BeaconMessage;
import org.hva.sensei.coach.beacon.GlimwormBeacon;
import org.hva.sensei.coach.configuring.BeaconConnection;
import org.hva.sensei.coach.configuring.BeaconConnectionListener;
import org.hva.sensei.coach.scanning.BLEScan;
import org.hva.sensei.coach.scanning.beaconListener;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainCoachActivity extends FragmentActivity implements
		beaconListener, BeaconConnectionListener {
	Button start, stop;
	LinearLayout start_layout, instructions_layout;
	TextView instructions_view;
	ToggleButton show_instructions_button;
	boolean show_instructions = true;

	Handler stimulusHandler;
	Vibrator vibrator;

	final int GA_HARDER = 1;
	final int BLIJFT_GELIJK = 0;
	final int GA_ZACHTER = -1;

	long stimulus_short = 500;
	long stimulus_median = 1750;
	long stimulus_long = 3000;

	long pause_ms = 500;
	long tekst_ms = 2000;
	long long_pause_ms = 3000;

	int[][] program = new int[][] { { GA_HARDER, BLIJFT_GELIJK, GA_ZACHTER },
			{ GA_HARDER, GA_ZACHTER, BLIJFT_GELIJK },
			{ GA_ZACHTER, BLIJFT_GELIJK, GA_HARDER },
			{ GA_ZACHTER, GA_HARDER, BLIJFT_GELIJK },
			{ BLIJFT_GELIJK, GA_HARDER, GA_ZACHTER },
			{ BLIJFT_GELIJK, GA_ZACHTER, GA_HARDER }, };

	// Sven import
	BLEScan leScanner;
	AbstractBeacon lfb = null;
	BeaconConnection beaconConnection;

	TextView statusLabel = null;
	boolean connected = false;
	boolean vibrating = false;
	boolean leach = false;
	TextView distance = null;
	TextView battery = null;
	
	final String TAG = "MainCoachActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_coach);

		stimulusHandler = new StimulusHandler();
		vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

		start_layout = (LinearLayout) findViewById(R.id.coach_start_layout);
		instructions_layout = (LinearLayout) findViewById(R.id.coach_instructions_layout);
		instructions_view = (TextView) findViewById(R.id.coach_instructions_view);

		show_instructions_button = (ToggleButton) findViewById(R.id.show_instructions);
		show_instructions_button.setChecked(show_instructions);
		show_instructions_button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				show_instructions = ((ToggleButton) v).isChecked();
			}
		});

		start = (Button) findViewById(R.id.button1);
		start.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startCoach(true);
			}
		});

		stop = (Button) findViewById(R.id.button2);
		stop.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startCoach(false);
			}
		});

		statusLabel = (TextView) findViewById(R.id.status_label);
		distance = (TextView) findViewById(R.id.distance);
		battery = (TextView) findViewById(R.id.battery);
		statusLabel.setText("Disconnected");
		Uri notification = RingtoneManager
				.getDefaultUri(RingtoneManager.TYPE_ALARM);
		r = RingtoneManager.getRingtone(getApplicationContext(), notification);
		if (savedInstanceState == null) {
		}

		SeekBar seek = (SeekBar) findViewById(R.id.vibratespeed);
		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			int seekbar_value = 0;

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				seekbar_value = progress;
				onStopTrackingTouch(seekBar);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see
			 * android.widget.SeekBar.OnSeekBarChangeListener#onStopTrackingTouch
			 * (android.widget.SeekBar) 0 = uit 1 = aan 2-9 = pwm 2 laagst, 9
			 * hoogst
			 */
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				beaconConnection.transmitDataWithoutResponse("AT+PIO2"
						+ seekbar_value);
			}

		});

	}

	private void startCoach(boolean start) {
		if (start) {
			// start
			start_layout.setVisibility(View.GONE);
			instructions_layout.setVisibility(View.VISIBLE);
			startProgramma(show_instructions);

		} else {
			// stop
			start_layout.setVisibility(View.VISIBLE);
			instructions_layout.setVisibility(View.GONE);
		}
	}

	private void startProgramma(boolean show_instructions) {
		if (show_instructions) {
			// programma met instructies

			/*
			 * - Op het scherm verschijnt 2000ms de tekst ÔDe volgende stimulus
			 * betekent Ôga harderÕ
			 * 
			 * - Tekst verdwijnt, 500ms pauze
			 * 
			 * - De stimulus ga harder (met lengte 1000ms) wordt gegeven
			 * 
			 * - 3000ms pauze
			 * 
			 * - Op het scherm verschijnt 2000ms de tekst: ÔDe volgende stimulus
			 * betekent Ôblijf gelijkÕ
			 * 
			 * - etc etc
			 */
			Message m = new Message();
			m.obj = new Stimulus(R.string.stimulus_ga_harder, tekst_ms,
					pause_ms, GA_HARDER, stimulus_long);// stimulus as parameter
			stimulusHandler.sendMessageDelayed(m, 0);

			// showStimulus(R.string.stimulus_ga_harder, tekst_ms, pause_ms,
			// GA_HARDER, stimulus_long);
			// long nextAfter = tekst_ms + pause_ms + stimulus_long
			// + long_pause_ms;
			//
			// final Handler handler = new Handler();
			// handler.postDelayed(new Runnable() {
			//
			// @Override
			// public void run() {
			// showStimulus(R.string.stimulus_blijft_gelijk, tekst_ms,
			// pause_ms, BLIJFT_GELIJK, stimulus_long);
			// }
			//
			// }, nextAfter);
			//
			// nextAfter += nextAfter;
			//
			// handler.postDelayed(new Runnable() {
			//
			// @Override
			// public void run() {
			// showStimulus(R.string.stimulus_ga_zachter, tekst_ms,
			// pause_ms, GA_ZACHTER, stimulus_long);
			//
			// }
			//
			// }, nextAfter);
			//
			// nextAfter += tekst_ms + pause_ms + stimulus_long + long_pause_ms;
			//
			// handler.postDelayed(new Runnable() {
			//
			// @Override
			// public void run() {
			// startCoach(false);
			// }
			//
			// }, nextAfter);

		} else {
			// programma zonder instructies

			/*
			 * - Op het scherm verschijnt 2000ms de tekst ÔVolgende
			 * stimulus....Õ
			 * 
			 * - Tekst verdwijnt, 500ms pauze
			 * 
			 * - Een stimulus (met lengte 1000ms) wordt gegeven
			 * 
			 * - 3000ms pauze
			 * 
			 * - Op het scherm verschijnt 2000ms de tekst: ÔVolgende
			 * stimulus....Õ
			 * 
			 * - etc etc
			 */
			showStimulus(R.string.stimulus_generiek, tekst_ms, pause_ms,
					GA_HARDER, stimulus_long);
			long nextAfter = tekst_ms + pause_ms + stimulus_long
					+ long_pause_ms;

			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					showStimulus(R.string.stimulus_generiek, tekst_ms,
							pause_ms, BLIJFT_GELIJK, stimulus_long);
				}

			}, nextAfter);

			nextAfter += nextAfter;

			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					showStimulus(R.string.stimulus_generiek, tekst_ms,
							pause_ms, GA_ZACHTER, stimulus_long);

				}

			}, nextAfter);

			nextAfter += tekst_ms + pause_ms + stimulus_long + long_pause_ms;

			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					startCoach(false);
				}

			}, nextAfter);
		}
	}

	class StimulusHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Stimulus s = (Stimulus) msg.obj;
			switch (s.stimulus_type) {
			case GA_ZACHTER:
				if (show_instructions) {
					this.postDelayed(new Runnable() {

						@Override
						public void run() {
							showStimulus(R.string.stimulus_ga_zachter,
									tekst_ms, pause_ms, GA_ZACHTER,
									stimulus_long);

						}

					}, long_pause_ms);
				} else {
					this.postDelayed(new Runnable() {

						@Override
						public void run() {
							showStimulus(R.string.stimulus_generiek, tekst_ms,
									pause_ms, GA_ZACHTER, stimulus_long);

						}

					}, long_pause_ms);
				}

				this.postDelayed(new Runnable() {

					@Override
					public void run() {
						startCoach(false);
					}
				}, long_pause_ms + tekst_ms + pause_ms + stimulus_long);

				break;
			case GA_HARDER:
				if (show_instructions) {
					this.postDelayed(new Runnable() {

						@Override
						public void run() {
							showStimulus(R.string.stimulus_ga_harder, tekst_ms,
									pause_ms, GA_HARDER, stimulus_long);

						}

					}, long_pause_ms);
				} else {
					this.postDelayed(new Runnable() {

						@Override
						public void run() {
							showStimulus(R.string.stimulus_generiek, tekst_ms,
									pause_ms, GA_HARDER, stimulus_long);

						}

					}, long_pause_ms);
				}

				Message m = new Message();
				m.obj = new Stimulus(R.string.stimulus_blijft_gelijk, tekst_ms,
						pause_ms, BLIJFT_GELIJK, stimulus_long);// stimulus as
				// parameter
				this.sendMessageDelayed(m, long_pause_ms + tekst_ms + pause_ms
						+ stimulus_long);

				break;
			case BLIJFT_GELIJK:
				if (show_instructions) {
					this.postDelayed(new Runnable() {

						@Override
						public void run() {
							showStimulus(R.string.stimulus_blijft_gelijk,
									tekst_ms, pause_ms, BLIJFT_GELIJK,
									stimulus_long);

						}

					}, long_pause_ms);
				} else {
					this.postDelayed(new Runnable() {

						@Override
						public void run() {
							showStimulus(R.string.stimulus_generiek, tekst_ms,
									pause_ms, BLIJFT_GELIJK, stimulus_long);

						}

					}, long_pause_ms);
				}

				Message me = new Message();
				me.obj = new Stimulus(R.string.stimulus_ga_zachter, tekst_ms,
						pause_ms, GA_ZACHTER, stimulus_long);// stimulus as
																// parameter
				this.sendMessageDelayed(me, long_pause_ms + tekst_ms + pause_ms
						+ stimulus_long);

				break;
			default:
				break;
			}
		}

	}

	class Stimulus {
		public int stimulus_tekst, stimulus_type;
		public long tekst_ms, pause_ms, stimulus_ms;

		public Stimulus(int stimulus_tekst, long tekst_ms, long pause_ms,
				int stimulus_type, long stimulus_ms) {
			this.stimulus_tekst = stimulus_tekst;
			this.tekst_ms = tekst_ms;
			this.pause_ms = pause_ms;
			this.stimulus_type = stimulus_type;
			this.stimulus_ms = stimulus_ms;
		}
	}

	// returns true when finished with scheduled message
	private void showStimulus(int stimulus_tekst, long tekst_ms, long pause_ms,
			final int stimulus_type, final long stimulus_ms) {

		instructions_view.setText(stimulus_tekst);
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				instructions_view.setText("");

			}
		}, tekst_ms);

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				activateStimulus(stimulus_type, stimulus_ms);
			}
		}, tekst_ms + pause_ms);
	}

	private boolean activateStimulus(int stimulus_type, long stimulus_ms) {
		switch (stimulus_type) {
		case BLIJFT_GELIJK:
			vibrator.vibrate(stimulus_ms);
			vibrate(stimulus_ms);
			break;
		case GA_ZACHTER:
			vibrate(stimulus_ms, 9, 5);
			break;
		case GA_HARDER:
			vibrate(stimulus_ms, 2, 5);
			break;
		}
		return true;
	}

	Ringtone r;

	@Override
	public void beaconFound(AbstractBeacon b) {
		if (b.getDevice().getAddress().trim().equals("20:CD:39:AD:67:ED")) {
			statusLabel.setText("Found your bracelet");
			GlimwormBeacon glb = (GlimwormBeacon) b;
			battery.setText("Battery level:" + glb.getBatteryLevel() + "%");
			lfb = b;
			if (leach) {
				distance.setText("Distance to bracelet: " + lfb.getDistance()
						+ "");
				if (lfb.getDistance() > 15) {
					try {

						r.play();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					r.stop();
				}
			}
		}

	}

	public void startScan(View v) {
		if (!connected) {
			leScanner = new BLEScan(this, 2000);
			leScanner.addBeaconListener(this);
			leScanner.startScan();
		}
	}

	public void startConnect(View v) {
		if (connected) {
			beaconConnection.Disconnect();
		} else {
			if (lfb != null) {
				leScanner.stopScan();
				beaconConnection = new BeaconConnection(this, lfb.getDevice()
						.getAddress().trim());
				beaconConnection.addListener(this, 0);
				beaconConnection.Connect();
			}
		}
	}

	public void vibrate(View v) {
		if (connected) {
			if (!vibrating) {
				beaconConnection.transmitDataWithoutResponse("AT+PIO21");
				statusLabel.setText("Shaking your bracelet");
				vibrating = true;
			} else {
				beaconConnection.transmitDataWithoutResponse("AT+PIO20");
				statusLabel.setText("Connected to your bracelet");
				vibrating = false;
			}
		}
	}

	private void vibrate(long ms) {
		stimulusHandler.post(new Runnable() {

			@Override
			public void run() {
				if (connected){
					beaconConnection.transmitDataWithoutResponse("AT+PIO21");
				}
				statusLabel.setText("Shaking your bracelet");
				vibrating = true;
			}
		});

		stimulusHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (connected){
					beaconConnection.transmitDataWithoutResponse("AT+PIO20");
				}
				statusLabel.setText("Connected to your bracelet");
				vibrating = false;
			}
		}, ms);

	}

	// intensity_start en _end kan van 1-9, 0 is uit
	private void vibrate(long ms, int intensity_start, int intensity_end) {
		int steps = (Math.abs(intensity_start - intensity_end) + 1);
		final long ms_per_intensity = ms / steps;

		if (intensity_start < intensity_end) {
			for (int i = 0; i < steps; i++) {
				int current_step = intensity_start + i;
				vibrate(current_step, ms_per_intensity, ms_per_intensity * i);
				Log.d(TAG, "intensity:"+current_step + " ms_per_intensity:"+ms_per_intensity);
			}
		} else {
			for (int i = 0; i < steps; i++) {
				int current_step = intensity_start - i;
				vibrate(current_step, ms_per_intensity, ms_per_intensity * i);
				Log.d(TAG, "intensity:"+current_step + " ms_per_intensity:"+ms_per_intensity);
			}
		}

	}
	
	private void vibrate(final int intensity, final long ms, long start_ms) {
		stimulusHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (connected){
					Log.d(TAG, "Vibrate at intensity:"+intensity + " duraction:"+ms);
					beaconConnection.transmitDataWithoutResponse("AT+PIO2"
							+ intensity);
				}
				statusLabel.setText("Shaking your bracelet");
				vibrating = true;
				vibrator.vibrate(ms);
			}
		}, start_ms);

		stimulusHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (connected){

					Log.d(TAG, "Stop vibrate at intensity:"+intensity);
				beaconConnection.transmitDataWithoutResponse("AT+PIO20");
				}
				statusLabel.setText("Connected to your bracelet");
				vibrating = false;
			}
		}, ms + start_ms);
	}
	
	private void vibrate(final int intensity, final long ms) {
		stimulusHandler.post(new Runnable() {

			@Override
			public void run() {
				if (connected){
					beaconConnection.transmitDataWithoutResponse("AT+PIO2"
							+ intensity);
				statusLabel.setText("Shaking your bracelet");
				}
				vibrating = true;
				vibrator.vibrate(ms);
			}
		});

		stimulusHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (connected){
					beaconConnection.transmitDataWithoutResponse("AT+PIO20");
				}
				statusLabel.setText("Connected to your bracelet");
				vibrating = false;
			}
		}, ms);

	}

	public void leach(View v) {
		if (leScanner != null)
			leScanner.stopScan();
		leScanner = new BLEScan(this, 2000);
		leScanner.addBeaconListener(this);
		leScanner.startIntervalScan(5000);
		leach = true;
	}

	@Override
	public void beaconConnected() {
		statusLabel.setText("Connected to your bracelet");
		connected = true;
		TextView tv = (TextView) findViewById(R.id.connect);
		tv.setText("Disconnect");
	}

	@Override
	public void beaconSystemDisconnected() {
		statusLabel.setText("Disconnected");
		connected = false;
		TextView tv = (TextView) findViewById(R.id.connect);
		tv.setText("Connect");
	}

	@Override
	public void beaconUserDisconnected() {
		statusLabel.setText("Disconnected");
		connected = false;
		TextView tv = (TextView) findViewById(R.id.connect);
		tv.setText("Connect");
	}

	@Override
	public void dataReceived(BeaconMessage bm) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scanningStarted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void scanningStopped() {
		// TODO Auto-generated method stub

	}

	boolean led1on = false, led2on = false, led3on = false, led4on = false,
			led5on = false, led6on = false, led7on = false, led8on = false;

	public void ledOne(View v) {
		if (connected) {
			if (!led1on) {
				beaconConnection.transmitData("AT+PIO31");
				led1on = true;
			} else {
				beaconConnection.transmitData("AT+PIO30");
				led1on = false;
			}
		}
	}

	public void ledTwo(View v) {
		if (connected) {
			if (!led2on) {
				beaconConnection.transmitData("AT+PIO41");
				led2on = true;
			} else {
				beaconConnection.transmitData("AT+PIO40");
				led2on = false;
			}
		}
	}

	public void ledThree(View v) {
		if (connected) {
			if (!led3on) {
				beaconConnection.transmitData("AT+PIO51");
				led3on = true;
			} else {
				beaconConnection.transmitData("AT+PIO50");
				led3on = false;
			}
		}
	}

	public void ledFour(View v) {
		if (connected) {
			if (!led4on) {
				beaconConnection.transmitData("AT+PIO61");
				led4on = true;
			} else {
				beaconConnection.transmitData("AT+PIO60");
				led4on = false;
			}
		}
	}

	public void ledFive(View v) {
		if (connected) {
			if (!led5on) {
				beaconConnection.transmitData("AT+PIO71");
				led5on = true;
			} else {
				beaconConnection.transmitData("AT+PIO70");
				led5on = false;
			}
		}
	}

	public void ledSix(View v) {
		if (connected) {
			if (!led6on) {
				beaconConnection.transmitData("AT+PIO81");
				led6on = true;
			} else {
				beaconConnection.transmitData("AT+PIO80");
				led6on = false;
			}
		}
	}

	public void ledSeven(View v) {
		if (connected) {
			if (!led7on) {
				beaconConnection.transmitData("AT+PIO91");
				led7on = true;
			} else {
				beaconConnection.transmitData("AT+PIO90");
				led7on = false;
			}
		}
	}

	public void ledEight(View v) {
		if (connected) {
			if (!led8on) {
				beaconConnection.transmitData("AT+PIOA1");
				led8on = true;
			} else {
				beaconConnection.transmitData("AT+PIOA0");
				led8on = false;
			}
		}
	}

}
