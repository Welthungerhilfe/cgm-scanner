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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.repository.BackupManager;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;
import de.welthungerhilfe.cgm.scanner.utils.IO;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class ContactSupportDialog extends Dialog {

    private static final int PERMISSION_STORAGE = 0x0003;

    private static final String SUPPORT_APP = "com.google.android.gm";
    private static final String SUPPORT_EMAIL = "support@childgrowthmonitor.org";
    private static final String SUPPORT_MIME = "application/zip";

    private Context context;
    private String type;
    private File screenshot;
    private File zip;

    @BindView(R.id.inputMessage)
    EditText inputMessage;

    @OnClick(R.id.txtOK)
    void onConfirm(TextView txtOK) {
        dismiss();

        if (type == null) {
            type = "";
        } else {
            type = " - " + type;
        }
        String subject = "CGM-Scanner version " + Utils.getAppVersion(context) + type;

        Intent sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        sendIntent.setType(SUPPORT_MIME);
        sendIntent.setPackage(SUPPORT_APP);
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { SUPPORT_EMAIL });
        sendIntent.putExtra(Intent.EXTRA_TEXT, inputMessage.getText().toString());
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

        ArrayList<Uri> uris = new ArrayList<Uri>();
        if (screenshot != null) uris.add(Uri.fromFile(screenshot));
        if (zip != null) uris.add(Uri.fromFile(zip));
        sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        context.startActivity(sendIntent);
    }

    @OnClick(R.id.txtCancel)
    void onCancel(TextView txtCancel) {
        dismiss();
    }

    public ContactSupportDialog(@NonNull BaseActivity context) {
        super(context);
        this.context = context;

        this.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_contact_support);
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationScale;
        this.setCancelable(false);

        ButterKnife.bind(this);
    }

    public void attachScreenshot(File file) {
        screenshot = file;
    }

    public void attachFiles(File[] files) {
        String[] paths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            paths[i] = files[i].getAbsolutePath();
        }

        zip = new File(AppController.getInstance().getRootDirectory(), "report.zip");
        IO.zip(paths, zip.getAbsolutePath());
    }

    public void setType(String value) {
        type = value;
    }

    public static void show(BaseActivity activity, String type) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
            return;
        }

        long timestamp = System.currentTimeMillis();
        File dir = new File(activity.getApplicationInfo().dataDir, "temp");
        File screenshot = new File(AppController.getInstance().getRootDirectory(), "screenshot.png");
        IO.deleteDirectory(dir);
        BackupManager.doBackup(activity, dir, timestamp, () -> {
            IO.takeScreenshot(activity, screenshot);
            ContactSupportDialog contactSupportDialog = new ContactSupportDialog(activity);
            contactSupportDialog.attachFiles(dir.listFiles());
            contactSupportDialog.attachScreenshot(screenshot);
            contactSupportDialog.setType(type);
            contactSupportDialog.show();
        });
    }
}
