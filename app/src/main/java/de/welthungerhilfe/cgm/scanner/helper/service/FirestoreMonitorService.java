package de.welthungerhilfe.cgm.scanner.helper.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.MainActivity;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;

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

public class FirestoreMonitorService extends Service {

    public void onCreate() {
        AppController.getInstance().firebaseFirestore.collection("persons")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {

                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        int icon = R.drawable.logo;
                        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
                        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(FirestoreMonitorService.this);

                        Gson gson = new Gson();

                        List<DocumentChange> changeList = documentSnapshots.getDocumentChanges();
                        for (int i = 0; i < changeList.size(); i++) {
                            Log.e("FIRESTORE_UPDATES", gson.toJson(changeList.get(i).getDocument().getData()));
                        }

                        Notification notification = builder.setContentIntent(contentIntent)
                                .setSmallIcon(icon)
                                .setTicker("Firebase" + Math.random())
                                .setWhen(System.currentTimeMillis())
                                .setAutoCancel(true)
                                .setContentTitle("Firestore Updated")
                                .setContentText(documentSnapshots.getMetadata().toString()).build();

                        mNotificationManager.notify(AppConstants.NOTIF_FIRESTORE, notification);
                    }
                });

        AppController.getInstance().firebaseFirestore.collection("persons")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {

                    @Override
                    public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                        final Gson gson = new Gson();

                        final List<DocumentChange> changeList = documentSnapshots.getDocumentChanges();
                        for (int i = 0; i < changeList.size(); i++) {
                            DocumentSnapshot snapshot = changeList.get(i).getDocument();
                            snapshot.getReference().collection("consents")
                                    .addSnapshotListener(new EventListener<QuerySnapshot>() {

                                        @Override
                                        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                                            List<DocumentChange> changeList = documentSnapshots.getDocumentChanges();
                                            for (int i = 0; i < changeList.size(); i++) {
                                                Log.e("CONSENTS_UPDATES", gson.toJson(changeList.get(i).getDocument().getData()));
                                            }
                                        }
                                    });

                            snapshot.getReference().collection("measures")
                                    .addSnapshotListener(new EventListener<QuerySnapshot>() {

                                        @Override
                                        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                                            List<DocumentChange> changeList = documentSnapshots.getDocumentChanges();
                                            for (int i = 0; i < changeList.size(); i++) {
                                                Log.e("MEASURE_UPDATES", gson.toJson(changeList.get(i).getDocument().getData()));
                                            }
                                        }
                                    });

                            Log.e("PERSON_UPDATES", gson.toJson(changeList.get(i).getDocument().getData()));
                        }
                    }
                });
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
}
