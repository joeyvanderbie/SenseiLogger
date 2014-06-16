package org.hva.sensei.logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.hva.sensei.coach.beacon.AbstractBeacon;
import org.hva.sensei.coach.beacon.BeaconMessage;
import org.hva.sensei.coach.beacon.GlimwormBeacon;
import org.hva.sensei.coach.configuring.BeaconConnection;
import org.hva.sensei.coach.configuring.BeaconConnectionListener;
import org.hva.sensei.coach.scanning.BLEScan;
import org.hva.sensei.coach.scanning.beaconListener;
import org.hva.sensei.data.CoachData;
import org.hva.sensei.db.CoachDataSource;
import org.hva.sensei.db.DatabaseHelper;
import org.hva.sensei.logger.MainMovementActivity.makeZip;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainCoachActivity extends FragmentActivity implements
		beaconListener, BeaconConnectionListener {
	Button start_fase_1, start_fase_2, start_fase_3, stop;
	LinearLayout start_layout, instructions_layout;
	LinearLayout stimulus_feedback_layout;
	TextView instructions_view;
	ToggleButton show_instructions_button;
	boolean show_instructions = true;

	Handler stimulusHandler;
	StimulusHandler2 stimulusHandler2;
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

	String beaconID = "20:CD:39:AD:68:B8";
	SeekBar start_intensity_seekbar, stop_intensity_seekbar, duration_seekbar;
	int start_intensity, stop_intensity, vibrate_duration;
	TextView start_intensity_label, stop_intensity_label,
			vibrate_duration_label;

	CoachDataSource cds;
	int run_id = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_coach);

		cds = new CoachDataSource(this);

		stimulusHandler = new StimulusHandler();
		stimulusHandler2 = new StimulusHandler2();

		vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

		start_layout = (LinearLayout) findViewById(R.id.coach_start_layout);
		instructions_layout = (LinearLayout) findViewById(R.id.coach_instructions_layout);
		instructions_view = (TextView) findViewById(R.id.coach_instructions_view);

		stimulus_feedback_layout = (LinearLayout) findViewById(R.id.stimulus_feedback_layout);

		show_instructions_button = (ToggleButton) findViewById(R.id.show_instructions);
		show_instructions_button.setChecked(show_instructions);
		show_instructions_button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				show_instructions = ((ToggleButton) v).isChecked();
			}
		});

		start_fase_1 = (Button) findViewById(R.id.start_fase_1);
		start_fase_1.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startCoach(true, 1);
			}
		});

		start_fase_2 = (Button) findViewById(R.id.start_fase_2);
		start_fase_2.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startCoach(true, 2);
			}
		});

		start_fase_3 = (Button) findViewById(R.id.start_fase_3);
		start_fase_3.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// magic of fase 3
				startCoach(true, 3);
			}
		});

		stop = (Button) findViewById(R.id.button2);
		stop.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startCoach(false, 1);
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

		start_intensity_label = (TextView) findViewById(R.id.startspeed_label);
		stop_intensity_label = (TextView) findViewById(R.id.stopspeed_label);
		vibrate_duration_label = (TextView) findViewById(R.id.vibrateduration_label);

		start_intensity_seekbar = (SeekBar) findViewById(R.id.startspeed);
		start_intensity_seekbar
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						start_intensity = progress;
						start_intensity_label.setText("" + start_intensity);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}

				});

		stop_intensity_seekbar = (SeekBar) findViewById(R.id.endspeed);
		stop_intensity_seekbar
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						stop_intensity = progress;

						stop_intensity_label.setText("" + stop_intensity);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}

				});

		duration_seekbar = (SeekBar) findViewById(R.id.durationseekbar);
		duration_seekbar
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						vibrate_duration = progress;

						vibrate_duration_label.setText("" + vibrate_duration);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}

				});

		Button vibrateButton = (Button) this.findViewById(R.id.vibrate);
		vibrateButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				vibrate(start_intensity, stop_intensity, vibrate_duration);

			}
		});

		Button backup = (Button) findViewById(R.id.backup_db_button);
		backup.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String backupLocation = Environment
						.getExternalStorageDirectory().getAbsolutePath()
						+ "/Sensei/backup"
						+ System.currentTimeMillis()
						+ ".zip";

				ArrayList<String> uploadData = new ArrayList<String>();
				uploadData.add(backupLocation);
				makeZip mz = new makeZip(backupLocation);
				mz.addZipFile(getDatabasePath(DatabaseHelper.DATABASE_NAME)
						.getAbsolutePath());
				mz.closeZip();

			}
		});

	}

	private void startCoach(boolean start, int fase) {
		if (start) {
			// start
			start_layout.setVisibility(View.GONE);
			instructions_layout.setVisibility(View.VISIBLE);
			startProgramma(show_instructions, fase);

		} else {
			// stop
			start_layout.setVisibility(View.VISIBLE);
			instructions_layout.setVisibility(View.GONE);
			stimulus_feedback_layout.setVisibility(View.GONE);
		}
	}

	int instructions = 0;

	private void startProgramma(boolean show_instructions, int fase) {

		switch (fase) {
		case 1:
			if (show_instructions) {
				instructions = 1;
				// programma met instructies
				/*
				 * - Op het scherm verschijnt 2000ms de tekst ÔDe volgende
				 * stimulus betekent Ôga harderÕ
				 * 
				 * - Tekst verdwijnt, 500ms pauze
				 * 
				 * - De stimulus ga harder (met lengte 1000ms) wordt gegeven
				 * 
				 * - 3000ms pauze
				 * 
				 * - Op het scherm verschijnt 2000ms de tekst: ÔDe volgende
				 * stimulus betekent Ôblijf gelijkÕ
				 * 
				 * - etc etc
				 */
				Message m = new Message();
				m.obj = new Stimulus(R.string.stimulus_ga_harder, tekst_ms,
						pause_ms, GA_HARDER, stimulus_long);// stimulus as
															// parameter
				stimulusHandler.sendMessageDelayed(m, 0);

			} else {
				instructions = 1;
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

				nextAfter += tekst_ms + pause_ms + stimulus_long
						+ long_pause_ms;

				handler.postDelayed(new Runnable() {

					@Override
					public void run() {
						startCoach(false, 1);
					}

				}, nextAfter);
			}
			break;
		case 2:
			// fase 2 programma zonder instructies

			run_id++;
			startRandomNextStimulus();

			break;
		case 3:
			// kleine aanpassingen van fase 2
			break;

		}

	}

	// StimulusHandler voor fase 1
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
						startCoach(true, 2);
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

	// StimulusHandler voor fase 2
	class StimulusHandler2 extends Handler {
		@Override
		public void handleMessage(Message msg) {
			final Stimulus s = (Stimulus) msg.obj;
			if (s.stimulus_type == GA_ZACHTER || s.stimulus_type == GA_HARDER
					|| s.stimulus_type == BLIJFT_GELIJK) {
				this.postDelayed(new Runnable() {

					@Override
					public void run() {
						showStimulus(R.string.stimulus_generiek, tekst_ms,
								pause_ms, s.stimulus_type, s.stimulus_ms);
					}

				}, long_pause_ms);

				this.postDelayed(new Runnable() {

					@Override
					public void run() {
						// invoerveld laten zien
						showStimulusInvoerveld(s.stimulus_type, s.stimulus_ms);
					}
				}, long_pause_ms + tekst_ms + pause_ms + s.stimulus_ms);
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

	private void showStimulusInvoerveld(final int stimuls_type,
			final long stimulus_ms) {
		// geven invoerveld weer
		stimulus_feedback_layout.setVisibility(View.VISIBLE);

		Button ga_harder_feedback = (Button) findViewById(R.id.button_harder);
		ga_harder_feedback.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				stimulus_feedback_layout.setVisibility(View.GONE);
				// sla feedback ga harder op in db met stimulus type en stimulus
				// long
				cds.open();
				// todo instructions from fase 1
				// run id
				cds.add(new CoachData(run_id, GA_HARDER, System
						.currentTimeMillis(), stimuls_type, stimulus_ms, 9, 9,
						instructions));
				cds.close();

				// ga door naar volgende stimulus
				startRandomNextStimulus();
			}
		});

		Button ga_zachter_feedback = (Button) findViewById(R.id.button_zachter);
		ga_zachter_feedback.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				stimulus_feedback_layout.setVisibility(View.GONE);
				// sla feedback ga harder op in db met stimulus type en stimulus
				// long
				cds.open();
				// todo instructions from fase 1
				// run id
				cds.add(new CoachData(run_id, GA_ZACHTER, System
						.currentTimeMillis(), stimuls_type, stimulus_ms, 9, 9,
						instructions));
				cds.close();

				// ga door naar volgende stimulus
				startRandomNextStimulus();
			}
		});

		Button blijft_gelijk_feedback = (Button) findViewById(R.id.button_blijf_gelijk);
		blijft_gelijk_feedback.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				stimulus_feedback_layout.setVisibility(View.GONE);
				// sla feedback ga harder op in db met stimulus type en stimulus
				// long
				cds.open();
				// todo instructions from fase 1
				// run id
				cds.add(new CoachData(run_id, BLIJFT_GELIJK, System
						.currentTimeMillis(), stimuls_type, stimulus_ms, 9, 9,
						instructions));
				cds.close();

				// ga door naar volgende stimulus
				startRandomNextStimulus();
			}
		});

	}

	int stimulus_teller = -1;

	private void startRandomNextStimulus() {
		// hier gaat random een stimulus geselecteerd worden
		// en wordt bijgehouden of de stimulus al gegeven is of neit
		int random_stimulus = stimulus_teller;
		long stimulus_ms = stimulus_long;

		stimulus_teller++;
		if (stimulus_teller > 2) {
			// reset random teller
			stimulus_teller = -1;
			// einde programma
			startCoach(false, 2);
		} else {
			Message m = new Message();
			m.obj = new Stimulus(R.string.stimulus_generiek, tekst_ms,
					pause_ms, random_stimulus, stimulus_ms);// stimulus as
															// parameter
			stimulusHandler2.sendMessageDelayed(m, 0);
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
			vibrate(179, 180, (int) stimulus_ms);
			break;
		case GA_ZACHTER:
			vibrate(255, 0, (int) stimulus_ms);
			break;
		case GA_HARDER:
			vibrate(0, 255, (int) stimulus_ms);
			break;
		default:
			vibrate(180, 180, (int) stimulus_ms);
		}
		return true;
	}

	Ringtone r;

	@Override
	public void beaconFound(AbstractBeacon b) {
		if (b.getDevice().getAddress().trim().equals(beaconID)) {
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
			} else {
				if (leScanner != null) {
					leScanner.stopScan();
				}
				beaconConnection = new BeaconConnection(this, beaconID);
				beaconConnection.addListener(this, 0);
				beaconConnection.Connect();
			}
		}
	}

	// public void vibrate(View v) {
	// if (connected) {
	// if (!vibrating) {
	// beaconConnection.transmitDataWithoutResponse("AT+PIO21");
	// statusLabel.setText("Shaking your bracelet");
	// vibrating = true;
	// } else {
	// beaconConnection.transmitDataWithoutResponse("AT+PIO20");
	// statusLabel.setText("Connected to your bracelet");
	// vibrating = false;
	// }
	// }
	// }
	//
	// private void vibrate(long ms) {
	// stimulusHandler.post(new Runnable() {
	//
	// @Override
	// public void run() {
	// if (connected){
	// beaconConnection.transmitDataWithoutResponse("AT+PIO21");
	// }
	// statusLabel.setText("Shaking your bracelet");
	// vibrating = true;
	// }
	// });
	//
	// stimulusHandler.postDelayed(new Runnable() {
	//
	// @Override
	// public void run() {
	// if (connected){
	// beaconConnection.transmitDataWithoutResponse("AT+PIO20");
	// }
	// statusLabel.setText("Connected to your bracelet");
	// vibrating = false;
	// }
	// }, ms);
	//
	// }

	// intensity_start en _end kan van 1-9, 0 is uit
	// private void vibrate(long ms, int intensity_start, int intensity_end) {
	// int steps = (Math.abs(intensity_start - intensity_end) + 1);
	// final long ms_per_intensity = ms / steps;
	//
	// if (intensity_start < intensity_end) {
	// for (int i = 0; i < steps; i++) {
	// int current_step = intensity_start + i;
	// vibrate(current_step, ms_per_intensity, ms_per_intensity * i);
	// Log.d(TAG, "intensity:"+current_step +
	// " ms_per_intensity:"+ms_per_intensity);
	// }
	// } else {
	// for (int i = 0; i < steps; i++) {
	// int current_step = intensity_start - i;
	// vibrate(current_step, ms_per_intensity, ms_per_intensity * i);
	// Log.d(TAG, "intensity:"+current_step +
	// " ms_per_intensity:"+ms_per_intensity);
	// }
	// }
	//
	// }

	// private void vibrate(final int intensity, final long ms, long start_ms) {
	// stimulusHandler.postDelayed(new Runnable() {
	//
	// @Override
	// public void run() {
	// if (connected){
	// Log.d(TAG, "Vibrate at intensity:"+intensity + " duraction:"+ms);
	// beaconConnection.transmitDataWithoutResponse("AT+PIO2"
	// + intensity);
	// }
	// statusLabel.setText("Shaking your bracelet");
	// vibrating = true;
	// vibrator.vibrate(ms);
	// }
	// }, start_ms);
	//
	// stimulusHandler.postDelayed(new Runnable() {
	//
	// @Override
	// public void run() {
	// if (connected){
	//
	// Log.d(TAG, "Stop vibrate at intensity:"+intensity);
	// beaconConnection.transmitDataWithoutResponse("AT+PIO20");
	// }
	// statusLabel.setText("Connected to your bracelet");
	// vibrating = false;
	// }
	// }, ms + start_ms);
	// }

	// private void vibrate(final int intensity, final long ms) {
	// stimulusHandler.post(new Runnable() {
	//
	// @Override
	// public void run() {
	// if (connected){
	// beaconConnection.transmitDataWithoutResponse("AT+PIO2"
	// + intensity);
	// statusLabel.setText("Shaking your bracelet");
	// }
	// vibrating = true;
	// vibrator.vibrate(ms);
	// }
	// });
	//
	// stimulusHandler.postDelayed(new Runnable() {
	//
	// @Override
	// public void run() {
	// if (connected){
	// beaconConnection.transmitDataWithoutResponse("AT+PIO20");
	// }
	// statusLabel.setText("Connected to your bracelet");
	// vibrating = false;
	// }
	// }, ms);
	//
	// }

	/*
	 * start, stop 0...255 intensity of vibrate
	 */
	public void vibrate(int start, int stop, int duration) {
		if (connected) {
			// beaconConnection.transmitDataWithoutResponse(ttv.getText().toString());
			byte bytearr[] = new byte[8];
			bytearr[0] = '0';
			bytearr[1] = '1';
			bytearr[2] = '1';
			bytearr[3] = '1';
			bytearr[4] = (byte) (start & 0xFF);
			bytearr[5] = (byte) (stop & 0xFF);
			bytearr[6] = (byte) ((duration >> 8) & 0xFF);
			bytearr[7] = (byte) (duration & 0xFF);
			beaconConnection.transmitHexWithoutResponse(bytearr);
			System.out.println("Sending: " + new String(bytearr));
			System.out.println(((int) bytearr[4] & 0xFF) + "");
			System.out.println(((int) bytearr[5] & 0xFF) + "");
			System.out.println(((bytearr[6] & 0xFF) << 8 | (bytearr[7] & 0xFF))
					+ "");
			System.out.println("DONE");

			// 30 31 31 31 FF 20 0B B8
		}
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

	boolean led1on = false, led2on = false, led3on = false;

	public void ledOne(View v) {
		if (connected) {
			if (!led1on) {
				beaconConnection.transmitData("AT+PIOB1");
				led1on = true;
			} else {
				beaconConnection.transmitData("AT+PIOB0");
				led1on = false;
			}
		}
	}

	public void ledTwo(View v) {
		if (connected) {
			if (!led2on) {
				beaconConnection.transmitData("AT+PIO71");
				led2on = true;
			} else {
				beaconConnection.transmitData("AT+PIO70");
				led2on = false;
			}
		}
	}

	public void ledThree(View v) {
		if (connected) {
			if (!led3on) {
				beaconConnection.transmitData("AT+PIO21");
				led3on = true;
			} else {
				beaconConnection.transmitData("AT+PIO20");
				led3on = false;
			}
		}
	}

	public class makeZip {
		static final int BUFFER = 2048;

		ZipOutputStream out;
		byte data[];

		public makeZip(String name) {
			FileOutputStream dest = null;
			try {
				dest = new FileOutputStream(name);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			out = new ZipOutputStream(new BufferedOutputStream(dest));
			data = new byte[BUFFER];
		}

		public void addZipFile(String name) {
			Log.v("addFile", "Adding: ");
			FileInputStream fi = null;
			try {
				fi = new FileInputStream(name);
				Log.v("addFile", "Adding: ");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.v("atch", "Adding: ");
			}
			BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
			ZipEntry entry = new ZipEntry(name);
			try {
				out.putNextEntry(entry);
				Log.v("put", "Adding: ");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int count;
			try {
				while ((count = origin.read(data, 0, BUFFER)) != -1) {
					out.write(data, 0, count);
					// Log.v("Write", "Adding: "+origin.read(data, 0, BUFFER));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.v("catch", "Adding: ");
			}
			try {
				origin.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void closeZip() {
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
