package org.hva.sensei.logger;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainCoachActivity extends FragmentActivity {
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
			 * - Op het scherm verschijnt 2000ms de tekst ‘De volgende stimulus
			 * betekent ‘ga harder’
			 * 
			 * - Tekst verdwijnt, 500ms pauze
			 * 
			 * - De stimulus ga harder (met lengte 1000ms) wordt gegeven
			 * 
			 * - 3000ms pauze
			 * 
			 * - Op het scherm verschijnt 2000ms de tekst: ‘De volgende stimulus
			 * betekent ‘blijf gelijk’
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
			 * - Op het scherm verschijnt 2000ms de tekst ‘Volgende
			 * stimulus....’
			 * 
			 * - Tekst verdwijnt, 500ms pauze
			 * 
			 * - Een stimulus (met lengte 1000ms) wordt gegeven
			 * 
			 * - 3000ms pauze
			 * 
			 * - Op het scherm verschijnt 2000ms de tekst: ‘Volgende
			 * stimulus....’
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
				}, long_pause_ms+tekst_ms+pause_ms+stimulus_long);

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
				this.sendMessageDelayed(m, long_pause_ms+tekst_ms+pause_ms+stimulus_long);

				break;
			case BLIJFT_GELIJK:
				if (show_instructions) {
					this.postDelayed(new Runnable() {

						@Override
						public void run() {
							showStimulus(R.string.stimulus_blijft_gelijk, tekst_ms,
									pause_ms, BLIJFT_GELIJK, stimulus_long);

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
				
				Message m = new Message();
				m.obj = new Stimulus(R.string.stimulus_ga_zachter, tekst_ms,
						pause_ms, GA_ZACHTER, stimulus_long);// stimulus as
															// parameter
				this.sendMessageDelayed(m, long_pause_ms+tekst_ms+pause_ms+stimulus_long);

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
		// code van Sven
		vibrator.vibrate(stimulus_ms);

		return true;
	}
}
