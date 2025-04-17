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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.DialogContactSupportBinding;
import de.welthungerhilfe.cgm.scanner.hardware.Audio;
import de.welthungerhilfe.cgm.scanner.hardware.io.FileSystem;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;

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
    private File zip, zip1;


    DialogContactSupportBinding dialogContactSupportBinding;


    void onRecord() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            runnable = () -> onRecord();
            context.addResultListener(PERMISSION_AUDIO, listener);
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_AUDIO);
            return;
        }

        if (!recording) {
            Audio.playShooterSound(context, MediaActionSound.START_VIDEO_RECORDING);
            dialogContactSupportBinding.recordAudio.setImageDrawable(context.getDrawable(R.drawable.stop));
            recording = true;

            audioFile = new File(AppController.getInstance().getPublicAppDirectory(context), "voice_record.wav");
            audioEncoder = Audio.startRecording(audioFile);
        } else {
            Audio.stopRecording(audioEncoder);
            Audio.playShooterSound(context, MediaActionSound.STOP_VIDEO_RECORDING);
            dialogContactSupportBinding.recordAudio.setImageDrawable(context.getDrawable(R.drawable.ic_record_audio));
            audioEncoder = null;
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
        String subject = "CGM-Scanner version " + AppController.getInstance().getAppVersion() + type;
        String message = dialogContactSupportBinding.inputMessage.getText().toString();
        if (footer != null) {
            message += "\n\n" + footer;
        }

        Intent sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        sendIntent.setType("*/*");
        sendIntent.setPackage("com.google.android.gm");
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{SUPPORT_EMAIL});
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

        ArrayList<Uri> uris = new ArrayList<>();
        if (audioFile != null && audioFile.exists() && audioFile.canRead()) {
            uris.add(FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", audioFile));
        }
        if (screenshot != null && screenshot.exists() && screenshot.canRead()) {
            uris.add(FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", screenshot));
        }
        if (zip != null && zip.exists() && zip.canRead()) {
            uris.add(FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", zip));
        }
        if (zip1 != null && zip1.exists() && zip1.canRead()) {
            uris.add(FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", zip1));
        }
        addLoggingFilesZip(uris);
        addLoggingFilesZip1(uris);

        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

        try {
            context.startActivity(sendIntent);
        } catch (Exception e) {
            sendIntent.setPackage(null);
            context.startActivity(sendIntent);
        }
    }

    void addLoggingFilesZip(ArrayList<Uri> uris) {
        File extFileDir = AppController.getInstance().getRootDirectory(context);
        File logFilesFolder = new File(extFileDir, AppConstants.LOG_FILE_FOLDER);
        if (logFilesFolder.exists()) {
            File[] files = logFilesFolder.listFiles();
            if (files != null && files.length > 0) {
                Uri zipUri = createZipFile(files, "logs.zip");
                if (zipUri != null) {
                    uris.add(zipUri);
                } else {
                    Log.e("Email", "Failed to create zip file for logs");
                }
            } else {
                Log.d("Email", "No files found in log folder: " + logFilesFolder.getAbsolutePath());
            }
        } else {
            Log.d("Email", "Log folder does not exist: " + logFilesFolder.getAbsolutePath());
        }
    }

    void addLoggingFilesZip1(ArrayList<Uri> uris) {
        File extFileDir = AppController.getInstance().getRootDirectory(context);
        File logFilesFolder = new File(extFileDir, AppConstants.LOG_FILE_FOLDER_OFFLINE);
        if (logFilesFolder.exists()) {
            File[] files = logFilesFolder.listFiles();
            if (files != null && files.length > 0) {
                Uri zipUri = createZipFile(files, "logs_offline.zip");
                if (zipUri != null) {
                    uris.add(zipUri);
                } else {
                    Log.e("Email", "Failed to create zip file for offline logs");
                }
            } else {
                Log.d("Email", "No files found in offline log folder: " + logFilesFolder.getAbsolutePath());
            }
        } else {
            Log.d("Email", "Offline log folder does not exist: " + logFilesFolder.getAbsolutePath());
        }
    }

    Uri createZipFile(File[] files, String zipFileName) {
        try {
            // Create a temporary zip file in the cache directory
            File zipFile = new File(context.getCacheDir(), zipFileName);
            if (zipFile.exists()) {
                zipFile.delete(); // Delete existing file to avoid conflicts
            }

            // Create zip file
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (File file : files) {
                if (file.isFile() && file.canRead()) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zos.putNextEntry(zipEntry);

                    // Read the file and write to zip
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    fis.close();
                    zos.closeEntry();
                }
            }
            zos.close();
            fos.close();

            // Generate FileProvider URI for the zip file
            return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", zipFile);
        } catch (IOException e) {
            Log.e("Email", "Error creating zip file: " + e.getMessage());
            return null;
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
        FileSystem.zip(paths, zip.getAbsolutePath());
        return Uri.fromFile(zip);
    }

    public Uri attachFiles1(File[] files) {
        String[] paths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            paths[i] = files[i].getAbsolutePath();
        }
        zip1 = new File(AppController.getInstance().getPublicAppDirectory(context), "report1.zip");
        FileSystem.zip(paths, zip1.getAbsolutePath());
        return Uri.fromFile(zip1);
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
        FileSystem.takeScreenshot(activity, screenshot);
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

