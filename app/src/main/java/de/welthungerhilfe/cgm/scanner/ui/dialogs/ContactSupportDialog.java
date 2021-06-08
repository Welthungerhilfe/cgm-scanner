/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.welthungerhilfe.cgm.scanner.ui.dialogs;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import java.io.File;
import java.util.ArrayList;


import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.DialogContactSupportBinding;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;
import de.welthungerhilfe.cgm.scanner.hardware.io.IO;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class ContactSupportDialog extends Dialog implements View.OnClickListener {

    private static final int PERMISSION_STORAGE = 0x0003;
    private static final int PERMISSION_AUDIO = 0x0004;

    private static final String SUPPORT_APP = "com.google.android.gm";
    private static final String SUPPORT_EMAIL = "support@childgrowthmonitor.org";
    private static final String SUPPORT_MIME = "application/zip";

    private File audioFile = null;
    private MediaRecorder audioEncoder;
    private static Runnable runnable = null;
    private boolean recording = false;

    private BaseActivity context;
    public String footer;
    private String type;
    private File screenshot;
    private File zip;


    DialogContactSupportBinding dialogContactSupportBinding;


    void onRecord() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            runnable = () -> onRecord();
            context.addResultListener(PERMISSION_AUDIO, listener);
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_AUDIO);
            return;
        }

        if (!recording) {
            Utils.playShooterSound(context, MediaActionSound.START_VIDEO_RECORDING);
            dialogContactSupportBinding.recordAudio.setImageDrawable(context.getDrawable(R.drawable.stop));
            recording = true;

            audioFile = new File(AppController.getInstance().getPublicAppDirectory(context), "voice_record.wav");
            audioEncoder = new MediaRecorder();
            audioEncoder.setAudioSource(MediaRecorder.AudioSource.MIC);
            audioEncoder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            audioEncoder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            audioEncoder.setAudioEncodingBitRate(128000);
            audioEncoder.setAudioSamplingRate(44100);
            audioEncoder.setOutputFile(audioFile.getAbsolutePath());
            try {
                audioEncoder.prepare();
                audioEncoder.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                audioEncoder.stop();
                audioEncoder.release();
                audioEncoder = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

            Utils.playShooterSound(context, MediaActionSound.STOP_VIDEO_RECORDING);
            dialogContactSupportBinding.recordAudio.setImageDrawable(context.getDrawable(R.drawable.ic_record_audio));
            recording = false;
        }
    }


    void onConfirm() {
        if (recording) {
            onRecord();
        }
        dismiss();

        if (type == null) {
            type = "";
        } else {
            type = " - " + type;
        }
        String subject = "CGM-Scanner version " + Utils.getAppVersion(context) + type;
        String message = dialogContactSupportBinding.inputMessage.getText().toString();
        if (footer != null) {
            message += "\n\n" + footer;
        }

        Intent sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        sendIntent.setType(SUPPORT_MIME);
        sendIntent.setPackage(SUPPORT_APP);
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{SUPPORT_EMAIL});
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

        ArrayList<Uri> uris = new ArrayList<Uri>();
        if (audioFile != null) uris.add(Uri.fromFile(audioFile));
        if (screenshot != null) uris.add(Uri.fromFile(screenshot));
        if (zip != null) uris.add(Uri.fromFile(zip));
        addLoggingFilesZip(uris);
        sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        context.startActivity(sendIntent);

    }

    void addLoggingFilesZip(ArrayList<Uri> uris) {
        File extFileDir = AppController.getInstance().getRootDirectory(context);
        File logFilesFolder = new File(extFileDir, AppConstants.LOG_FILE_FOLDER);
        if (logFilesFolder.exists()) {
            File file[] = logFilesFolder.listFiles();
            uris.add(attachFiles(file));
        }
    }

    void onCancel() {
        dismiss();
    }

    public ContactSupportDialog(@NonNull BaseActivity context) {
        super(context);
        this.context = context;

        this.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogContactSupportBinding = DataBindingUtil.inflate(LayoutInflater.from(context),R.layout.dialog_contact_support,null,false);
        this.setContentView(dialogContactSupportBinding.getRoot());
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationScale;
        this.setCancelable(false);
        dialogContactSupportBinding.recordAudio.setOnClickListener(this);
        dialogContactSupportBinding.txtCancel.setOnClickListener(this);
        dialogContactSupportBinding.txtOK.setOnClickListener(this);

    }

    public void attachScreenshot(File file) {
        screenshot = file;
    }

    public Uri attachFiles(File[] files) {
        String[] paths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            paths[i] = files[i].getAbsolutePath();
        }
        zip = new File(AppController.getInstance().getPublicAppDirectory(context), "report.zip");
        IO.zip(paths, zip.getAbsolutePath());
        return Uri.fromFile(zip);
    }

    public void setFooter(String value) {
        footer = value;
    }

    public void setType(String value) {
        type = value;
    }

    public static void show(BaseActivity activity, String type, String footer) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            runnable = () -> show(activity, type, footer);
            activity.addResultListener(PERMISSION_STORAGE, listener);
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
            return;
        }

        File screenshot = new File(AppController.getInstance().getPublicAppDirectory(activity), "screenshot.png");
        IO.takeScreenshot(activity, screenshot);
        ContactSupportDialog contactSupportDialog = new ContactSupportDialog(activity);
        contactSupportDialog.attachScreenshot(screenshot);
        contactSupportDialog.setFooter(footer);
        contactSupportDialog.setType(type);
        contactSupportDialog.show();
    }

    private static BaseActivity.ResultListener listener = new BaseActivity.ResultListener() {
        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if (grantResults.length > 0) {
                runnable.run();
                runnable = null;
            }
        }
    };

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.recordAudio){
            onRecord();
        }else if(v.getId() == R.id.txtOK){
            onConfirm();
        }else if(v.getId() == R.id.txtCancel){
            onCancel();
        }
    }
}
