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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.databinding.ActivityScanQrBinding;
import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.hardware.gpu.BitmapHelper;
import de.welthungerhilfe.cgm.scanner.hardware.io.FileSystem;
import de.welthungerhilfe.cgm.scanner.network.service.FirebaseService;
import de.welthungerhilfe.cgm.scanner.ui.dialogs.ConfirmDialog;
import de.welthungerhilfe.cgm.scanner.ui.views.QRScanView;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

public class QRScanActivity extends BaseActivity implements ConfirmDialog.OnConfirmListener, QRScanView.QRScanHandler {
    private final String TAG = QRScanActivity.class.getSimpleName();


    public void close(View view) {
        finish();
    }


    String qrCode;

    FileLogRepository fileLogRepository;

    PersonRepository personRepository;

    private static final int CAPTURED_CONSENT_SHEET_STEP = 1;

    private static final int NO_QR_CODE_FOUND = 2;

    private static final int EMPTY_QR_CODE_FOUND = 3;

    private static final int STD_TEST_WILL_STARTED = 4;

    private static final int STD_TEST_START_CONFIRM = 5;

    private static final int STD_TEST_QRCODE_OLDER = 6;

    private static final int STD_TEST_QRCODE_INFUTURE = 7;

    private static final int STD_TEST_QRCODE_INVALID = 8;


    int CONFIRM_DIALOG_STEP = 0;

    private static final int IMAGE_CAPTURED_REQUEST = 100;

    int activityBehaviourType = AppConstants.QR_SCAN_REQUEST;

    ActivityScanQrBinding activityScanQrBinding;

    SessionManager sessionManager;

    FirebaseAnalytics firebaseAnalytics;

    public enum STDTEST {VALID, INVALID, OLDER, INFUTURE}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityScanQrBinding = DataBindingUtil.setContentView(this, R.layout.activity_scan_qr);

        fileLogRepository = FileLogRepository.getInstance(QRScanActivity.this);
        personRepository = PersonRepository.getInstance(QRScanActivity.this);
        activityBehaviourType = getIntent().getIntExtra(AppConstants.ACTIVITY_BEHAVIOUR_TYPE, AppConstants.QR_SCAN_REQUEST);
        sessionManager = new SessionManager(this);
        firebaseAnalytics = FirebaseService.getFirebaseAnalyticsInstance(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CAMERA);
        } else {
            activityScanQrBinding.qrScanView.setResultHandler(this);
            activityScanQrBinding.qrScanView.startCamera();
        }


    }

    @Override
    public void onPause() {
        super.onPause();
        activityScanQrBinding.qrScanView.stopCamera();
    }

    @Override
    public void onConfirm(boolean result) {


        if (result) {
            if (CONFIRM_DIALOG_STEP == STD_TEST_WILL_STARTED) {
                sessionManager.setStdTestQrCode(qrCode);
                showConfirmDialog(R.string.std_test_started, STD_TEST_START_CONFIRM);
                firebaseAnalytics.logEvent(FirebaseService.STD_TEST_START, null);
            } else if (CONFIRM_DIALOG_STEP == NO_QR_CODE_FOUND || CONFIRM_DIALOG_STEP == EMPTY_QR_CODE_FOUND || CONFIRM_DIALOG_STEP == STD_TEST_START_CONFIRM
                    || CONFIRM_DIALOG_STEP == STD_TEST_QRCODE_INFUTURE || CONFIRM_DIALOG_STEP == STD_TEST_QRCODE_OLDER || CONFIRM_DIALOG_STEP == STD_TEST_QRCODE_INVALID) {
                finish();
            } else {
                firebaseAnalytics.logEvent(FirebaseService.SCAN_INFORM_CONSENT_START, null);
                startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE), IMAGE_CAPTURED_REQUEST);
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
                activityScanQrBinding.llQrDetails.setVisibility(View.GONE);
                activityScanQrBinding.qrScanView.setVisibility(View.GONE);
                Bitmap capturedImageBitmap = (Bitmap) data.getExtras().get("data");
                capturedImageBitmap = BitmapHelper.getAcceptableBitmap(capturedImageBitmap);
                ImageSaver(capturedImageBitmap, QRScanActivity.this);
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        finish();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_CAMERA) {
            if (!(grantResults.length > 0 && grantResults[0] >= 0)) {
                Toast.makeText(QRScanActivity.this, R.string.permission_camera, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void handleQRResult(String qrCode, byte[] bitmap) {
        if (qrCode.trim().isEmpty()) {
            showConfirmDialog(R.string.empty_qr_code, EMPTY_QR_CODE_FOUND);
            return;
        }

        activityScanQrBinding.qrScanView.stopCameraPreview();
        activityScanQrBinding.qrScanView.stopCamera();
        this.qrCode = qrCode;

        if (isStdTestQRCode(qrCode)) {
            switch (isValidStdTestQrCode(qrCode)) {
                case VALID:
                    showConfirmDialog(R.string.std_test_will_start, STD_TEST_WILL_STARTED);
                    return;
                case INVALID:
                    showConfirmDialog(R.string.std_test_invalid_qrcode, STD_TEST_QRCODE_INVALID);
                    return;
                case OLDER:
                    showConfirmDialog(R.string.std_test_old_qrcode, STD_TEST_QRCODE_OLDER);
                    return;
                case INFUTURE:
                    showConfirmDialog(R.string.std_test_future_qrcode, STD_TEST_QRCODE_INFUTURE);
                    return;

            }
        }

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
            if (step == STD_TEST_START_CONFIRM) {
                confirmDialog.setCancleButtonInvisible();
            }
            confirmDialog.show();
            CONFIRM_DIALOG_STEP = step;
        } catch (Exception e) {

        }
    }

    void ImageSaver(Bitmap data1, Context context) {


        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                final long timestamp = AppController.getInstance().getUniversalTimestamp();
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

                    BitmapHelper.writeBitmapToFile(data1, consentFile);


                }

                FileOutputStream output = null;
                try {
                    FileLog log = new FileLog();
                    log.setId(AppController.getInstance().getArtifactId("consent"));
                    log.setType("consent");
                    log.setPath(consentFile.getPath());
                    log.setHashValue(FileSystem.getMD5(consentFile.getPath()));
                    log.setFileSize(consentFile.length());
                    log.setUploadDate(0);
                    log.setQrCode(qrCode);
                    log.setDeleted(false);
                    log.setCreateDate(AppController.getInstance().getUniversalTimestamp());
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
                    firebaseAnalytics.logEvent(FirebaseService.SCAN_INFORM_CONSENT_STOP, null);
                    finish();
                } else {
                    Toast.makeText(context, R.string.error, Toast.LENGTH_LONG).show();
                }


            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


    }


    public static boolean isStdTestQRCode(String qrcode) {
        if (qrcode == null) {
            return false;
        }
        return qrcode.toUpperCase().contains("STD_TEST_");
    }

    public static STDTEST isValidStdTestQrCode(String qrcode) {
        try {
            String[] arrOfStr = qrcode.split("_", 5);
            String date = arrOfStr[2];
            Date c = Calendar.getInstance().getTime();

           // SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd",Locale.ENGLISH);
            String formattedDate =  sdf.format(new Date(System.currentTimeMillis()));
            //String formattedDate = df.format(c);

            if (date.equals(formattedDate)) {
                return STDTEST.VALID;
            } else {

                if(Double.parseDouble(date) > Double.parseDouble(formattedDate)){
                    return STDTEST.INFUTURE;
                }
                else if(Double.parseDouble(date) < Double.parseDouble(formattedDate)) {
                    return STDTEST.OLDER;
                }
                else {
                    return STDTEST.INVALID;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return STDTEST.INVALID;
        }
    }
}
