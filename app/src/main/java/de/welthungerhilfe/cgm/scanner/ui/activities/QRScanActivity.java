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
package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ConfirmDialog;
import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.ui.views.QRScanView;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;
import de.welthungerhilfe.cgm.scanner.utils.IO;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import id.zelory.compressor.Compressor;

public class QRScanActivity extends AppCompatActivity implements ConfirmDialog.OnConfirmListener, QRScanView.QRScanHandler {
    private final String TAG = QRScanActivity.class.getSimpleName();
    private final int PERMISSION_LOCATION = 0x1000;

    @BindView(R.id.qrScanView)
    QRScanView qrScanView;

    @OnClick(R.id.imgClose)
    void close(ImageView imgClose) {
        finish();
    }

    @BindView(R.id.rlt_consent_form)
    RelativeLayout rlt_consent_form;

    @BindView(R.id.ll_qr_details)
    LinearLayout ll_qr_details;

    @BindView(R.id.iv_consent_form)
    ImageView iv_consent_form;

    String qrCode;

    FileLogRepository fileLogRepository;

    PersonRepository personRepository;

    ByteArrayOutputStream out;

    Bitmap capturedImageBitmap;

    Uri imageUri;

    private static final int SCAN_QR_CODE_STEP = 0;

    private static final int CAPTURED_CONSENT_SHEET_STEP = 1;

    private static final int NO_QR_CODE_FOUND = 2;


    int CONFIRM_DIALOG_STEP = 0;

    private static final int IMAGE_CAPTURED_REQUEST = 100;

    int activityBehaviourType = AppConstants.QR_SCAN_REQUEST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan_qr);
        ButterKnife.bind(this);
        fileLogRepository = FileLogRepository.getInstance(QRScanActivity.this);
        personRepository = PersonRepository.getInstance(QRScanActivity.this);
        activityBehaviourType = getIntent().getIntExtra(AppConstants.ACTIVITY_BEHAVIOUR_TYPE, AppConstants.QR_SCAN_REQUEST);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_LOCATION);
        } else {
            showConfirmDialog(R.string.message_legal, SCAN_QR_CODE_STEP);
        }


    }

    @Override
    public void onPause() {
        super.onPause();
        qrScanView.stopCamera();
    }

    @Override
    public void onConfirm(boolean result) {


        if (result) {
            if (CONFIRM_DIALOG_STEP == SCAN_QR_CODE_STEP) {

                qrScanView.setResultHandler(this);
                qrScanView.startCamera();

            } else if (CONFIRM_DIALOG_STEP == NO_QR_CODE_FOUND) {
                finish();
            } else {
                startCaptureImage();
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_CAPTURED_REQUEST && resultCode == RESULT_OK) {
            try {
                capturedImageBitmap = MediaStore.Images.Media.getBitmap(
                        getContentResolver(), imageUri);
                showCapturedImage(capturedImageBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] >= 0) {
                showConfirmDialog(R.string.message_legal, SCAN_QR_CODE_STEP);
            } else {
                Toast.makeText(QRScanActivity.this, R.string.permission_camera, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void handleQRResult(String qrCode, byte[] bitmap) {

        qrScanView.stopCameraPreview();
        qrScanView.stopCamera();
        this.qrCode = qrCode;

        if (activityBehaviourType == AppConstants.QR_SCAN_REQUEST) {
            if (personRepository.findPersonByQr(qrCode) == null) {
                showConfirmDialog(R.string.message_legal, CAPTURED_CONSENT_SHEET_STEP);
            } else {
                Intent intent = new Intent(QRScanActivity.this, CreateDataActivity.class);
                intent.putExtra(AppConstants.EXTRA_QR, qrCode);
                startActivity(intent);
                finish();
            }
        } else {
            showConfirmDialog(R.string.message_legal, CAPTURED_CONSENT_SHEET_STEP);

        }


    }

    private void showConfirmDialog(int message, int step) {
        try {
            ConfirmDialog confirmDialog = new ConfirmDialog(this);
            confirmDialog.setMessage(message);
            confirmDialog.setConfirmListener(this);
            confirmDialog.show();
            CONFIRM_DIALOG_STEP = step;
        } catch (Exception e) {

        }
    }

    @OnClick(R.id.btn_retake)
    void startCaptureImage() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Picture");
        imageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, IMAGE_CAPTURED_REQUEST);
    }

    @OnClick(R.id.btn_ok)
    void saveCapturedImage() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (capturedImageBitmap != null) {
            ImageSaver(capturedImageBitmap, QRScanActivity.this);

        }
    }

    void showCapturedImage(Bitmap bitmap) {
        ll_qr_details.setVisibility(View.GONE);
        qrScanView.setVisibility(View.GONE);
        rlt_consent_form.setVisibility(View.VISIBLE);
        iv_consent_form.setImageBitmap(bitmap);
    }

    void ImageSaver(Bitmap data1, Context context) {


        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                //  byte[] data = data1.clone();
                final long timestamp = Utils.getUniversalTimestamp();
                final String consentFileString = timestamp + "_" + qrCode + ".jpg";

                File extFileDir = AppController.getInstance().getRootDirectory(context);
                File consentFileFolder = new File(extFileDir, AppConstants.LOCAL_CONSENT_URL.replace("{qrcode}", qrCode).replace("{scantimestamp}", String.valueOf(timestamp)));
                File consentFile = new File(consentFileFolder, consentFileString);
                if (!consentFileFolder.exists()) {
                    boolean created = consentFileFolder.mkdirs();
                    if (created) {
                        Log.i(TAG, "Folder: \"" + consentFileFolder + "\" created\n");
                    } else {
                        Log.e(TAG, "Folder: \"" + consentFileFolder + "\" could not be created!\n");
                    }

                    BitmapUtils.writeBitmapToFile(data1, consentFile);


                }

                FileOutputStream output = null;
                try {
                    consentFile = new Compressor(context).compressToFile(consentFile);

                    FileLog log = new FileLog();
                    log.setId(AppController.getInstance().getArtifactId("consent"));
                    log.setType("consent");
                    log.setPath(consentFile.getPath());
                    log.setHashValue(IO.getMD5(consentFile.getPath()));
                    log.setFileSize(consentFile.length());
                    log.setUploadDate(0);
                    log.setQrCode(qrCode);
                    log.setDeleted(false);
                    log.setCreateDate(Utils.getUniversalTimestamp());
                    log.setCreatedBy(new SessionManager(QRScanActivity.this).getUserEmail());
                    log.setEnvironment(new SessionManager(QRScanActivity.this).getEnvironment());
                    log.setSchema_version(CgmDatabase.version);
                    fileLogRepository.insertFileLog(log);
                    return true;

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (output != null) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return false;

            }


            @Override
            protected void onPostExecute(Boolean flag) {
                super.onPostExecute(flag);
                if (flag) {
                    Intent intent = new Intent(QRScanActivity.this, CreateDataActivity.class);
                    intent.putExtra(AppConstants.EXTRA_QR, qrCode);
                    startActivity(intent);
                    finish();
                } else {
                    //add german string for error
                    Toast.makeText(context, R.string.error, Toast.LENGTH_LONG).show();
                }


            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


    }


}
