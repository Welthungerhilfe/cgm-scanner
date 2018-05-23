package de.welthungerhilfe.cgm.scanner.helper.service;

/**
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.novoda.merlin.Merlin;
import com.novoda.merlin.registerable.connection.Connectable;
import com.novoda.merlin.registerable.disconnection.Disconnectable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.MainActivity;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.models.Consent;
import de.welthungerhilfe.cgm.scanner.models.task.ConsentOfflineTask;
import de.welthungerhilfe.cgm.scanner.models.task.PersonOfflineTask;
import de.welthungerhilfe.cgm.scanner.models.Person;

public class NetworkMonitorService extends Service implements Connectable, Disconnectable {
    private Merlin merlin;

    public void onCreate() {
        merlin = new Merlin.Builder().withConnectableCallbacks().withDisconnectableCallbacks().build(this);
        merlin.registerConnectable(this);
        merlin.registerDisconnectable(this);

        merlin.bind();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful
        return Service.START_STICKY;
    }

    @Override
    public void onConnect() {
        new PersonOfflineTask().loadAll(new PersonOfflineTask.OnLoadAll() {
            @Override
            public void onLoadAll(List<Person> personList) {
                Log.e("Offline Data Sync", "Sync started");

                for (int i = 0; i < personList.size(); i++) {
                    final Person person = personList.get(i);
                    AppController.getInstance().firebaseFirestore.collection("persons")
                            .add(personList.get(i))
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(final DocumentReference documentReference) {
                                    new PersonOfflineTask().delete(person, null);

                                    person.setId(documentReference.getId());
                                    Map<String, Object> personID = new HashMap<>();
                                    personID.put("id", person.getId());
                                    documentReference.update(personID);

                                    Log.e("Offline Data Sync", personID + " synced");
                                }
                            });
                }
            }
        });
        new ConsentOfflineTask().loadAll(new ConsentOfflineTask.OnLoadAll() {
            @Override
            public void onLoadAll(List<Consent> consentList) {
                for (int i = 0; i < consentList.size(); i++) {
                    final Consent consent = consentList.get(i);

                    try {
                        InputStream is = new FileInputStream(consent.getConsent());

                        final String consentPath = AppConstants.STORAGE_CONSENT_URL.replace("{qrcode}",  consent.getQrcode()) + consent.getCreated() + "_" + consent.getQrcode() + ".png";
                        StorageReference consentRef = AppController.getInstance().storageRootRef.child(consentPath);

                        UploadTask uploadTask = consentRef.putStream(is);
                        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                consent.setConsent(downloadUrl.toString());
                            }
                        });
                    } catch (FileNotFoundException e) {
                        new ConsentOfflineTask().delete(consent, null);
                    }
                }
            }
        });
    }

    @Override
    public void onDisconnect() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int icon = R.drawable.logo;
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        Notification notification = builder.setContentIntent(contentIntent)
                .setSmallIcon(icon)
                .setTicker("Firebase" + Math.random())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentTitle("Network Connectivity")
                .setContentText("Network Disconnected").build();

        mNotificationManager.notify(AppConstants.NOTIF_NETWORK, notification);
    }
}
