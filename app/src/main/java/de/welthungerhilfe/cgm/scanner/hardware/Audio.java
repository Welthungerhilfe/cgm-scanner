/*
 *  Child Growth Monitor - quick and accurate data on malnutrition
 *  Copyright (c) $today.year Welthungerhilfe Innovation
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.hardware;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaActionSound;
import android.media.MediaRecorder;

import java.io.File;

public class Audio {

    private static MediaActionSound sound = null;

    public static void playShooterSound(Context context, int sample) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        switch (audio.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                if (sound == null) {
                    sound = new MediaActionSound();
                }
                sound.play(sample);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                break;
        }
    }


    public static MediaRecorder startRecording(File file) {
        MediaRecorder audioEncoder = new MediaRecorder();
        audioEncoder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioEncoder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        audioEncoder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        audioEncoder.setAudioEncodingBitRate(128000);
        audioEncoder.setAudioSamplingRate(44100);
        audioEncoder.setOutputFile(file.getAbsolutePath());
        try {
            audioEncoder.prepare();
            audioEncoder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return audioEncoder;
    }

    public static void stopRecording(MediaRecorder audioEncoder) {
        try {
            audioEncoder.stop();
            audioEncoder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
