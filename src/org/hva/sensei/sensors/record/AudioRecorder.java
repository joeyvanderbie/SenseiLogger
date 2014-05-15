/**
 * Copyright (C) 2014 The Amsterdam University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hva.sensei.sensors.record;

import java.io.File;
import java.io.IOException;

import android.media.MediaRecorder;
import android.os.Environment;

/**
 * @author XpLoDWilD, Ben McCann
 */
public class AudioRecorder {
    final MediaRecorder recorder = new MediaRecorder();
    final String path;
    private boolean isActive = false;

    /**
     * Creates a new audio recording at the given path (relative to root of SD
     * card).
     */
    public AudioRecorder(String path) {
        this.path  = path;//= sanitizePath(path);
    }

    public int getAmplitude() {
        return recorder.getMaxAmplitude();
    }

    private String sanitizePath(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.contains(".")) {
//            path += ".3gp";
            path += ".wav";
        }
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Recording" + path;
    }

    /**
     * Starts a new recording.
     */
    public void start() throws IOException {
        String state = android.os.Environment.getExternalStorageState();
        if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            throw new IOException("SD Card is not mounted.  It is " + state
                    + ".");
        }

        // make sure the directory we plan to store the recording in exists
        File directory = new File(path).getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Path to file could not be created.");
        }

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        recorder.setAudioSamplingRate(); set to 16khz, 8 also ok, not 44, downsample to 16.
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
        recorder.setOutputFile(path);
        recorder.prepare();
        recorder.start();

        isActive = true;
    }

    /**
     * Stops a recording that has been previously started.
     */
    public void stop() throws IOException {
        recorder.stop();
        recorder.release();
        isActive = false;
    }
    
    public boolean isActive(){
    	return isActive;
    }

}