package org.hva.sensei.sensors.record;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.util.Log;

public class ToneGenerator {
	    // originally from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html
	    // and modified by Steve Pomeroy <steve@staticfree.info>
	   // private final int duration = 3; // seconds
	  //  private final int sampleRate = 8000;
	//    private final int numSamples = duration * sampleRate;
	  //  private final double sample[] = new double[numSamples];
	  //  private final double freqOfTone = 440; // hz

	  //  private final byte generatedSnd[] = new byte[2 * numSamples];

	    //Handler handler = new Handler();
	private double rampMinTime = 0.3;//seconds
	private double minBeepTime = 0.8;

	    public byte[] genTone(){
	    	return genTone(1, 8000, 600);
	    }

	    public byte[] genTone(double duration, int sampleRate, int freqOfTone){
	    	duration =   duration < minBeepTime+rampMinTime*2 ? minBeepTime+rampMinTime*2 : duration;
	    	int numSamples = (int) (duration * sampleRate);
	    	double sample[] = new double[numSamples];
	    	byte generatedSnd[] = new byte[2 * numSamples];
	    	
	        // fill out the array
	        for (int i = 0; i < numSamples; ++i) {
	            //sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
	            sample[i] = Math.sin((2 * Math.PI - .001) * i / (sampleRate/freqOfTone));

	        }

	        // convert to 16 bit pcm sound array
	        // assumes the sample buffer is normalised.
	        int idx = 0;
	        int ramp = (int) (sampleRate * rampMinTime);
	     
	        for (int i = 0; i < ramp; i++) {
	            // scale to maximum amplitude
	            final short val = (short) ((sample[i] * 32767) * i / ramp);
	            // in 16 bit wav PCM, first byte is the low order byte
	            generatedSnd[idx++] = (byte) (val & 0x00ff);
	            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
	        }
	     
	        for (int i = ramp; i < numSamples - ramp; i++) {
	            // scale to maximum amplitude
	            final short val = (short) ((sample[i] * 32767));
	            // in 16 bit wav PCM, first byte is the low order byte
	            generatedSnd[idx++] = (byte) (val & 0x00ff);
	            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
	        }
	     
	        for (int i = numSamples - ramp; i < numSamples; i++) {
	            // scale to maximum amplitude
	            final short val = (short) ((sample[i] * 32767) * (numSamples - i) / ramp);
	            // in 16 bit wav PCM, first byte is the low order byte
	            generatedSnd[idx++] = (byte) (val & 0x00ff);
	            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
	        }

	        
	        
	        return generatedSnd;
	    }
	    
	    public void playSound(int sampleRate, byte[] generatedSnd){
	        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
	        		sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
	                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
	                AudioTrack.MODE_STATIC);
	        audioTrack.write(generatedSnd, 0, generatedSnd.length);
	       
	        audioTrack.setNotificationMarkerPosition( generatedSnd.length);
	        audioTrack.setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListener() {
	            @Override
	            public void onPeriodicNotification(AudioTrack track) {
	                // nothing to do
	            }
	            @Override
	            public void onMarkerReached(AudioTrack track) {
	              //  Log.d("ToneGenerator", "Audio track end of file reached...");
	               
	            }
	        });
	        audioTrack.play();

	    }
}
