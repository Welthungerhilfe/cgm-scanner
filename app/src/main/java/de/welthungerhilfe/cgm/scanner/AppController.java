/**
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

package de.welthungerhilfe.cgm.scanner;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.Intent;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.novoda.merlin.Merlin;
import com.novoda.merlin.registerable.connection.Connectable;
import com.novoda.merlin.registerable.disconnection.Disconnectable;

import de.welthungerhilfe.cgm.scanner.activities.MainActivity;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.offline.DbConstants;
import de.welthungerhilfe.cgm.scanner.helper.offline.OfflineDatabase;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class AppController extends Application implements Connectable, Disconnectable {
    public static final String TAG = AppController.class.getSimpleName();

    private static AppController mInstance;

    public FirebaseAuth firebaseAuth;
    public FirebaseUser firebaseUser;

    public FirebaseStorage firebaseStorage;
    public StorageReference storageRootRef;

    public FirebaseFirestore firebaseFirestore;

    public OfflineDatabase offlineDb;

    public boolean networkStatus = false;

    private Merlin merlin;


    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Utils.overrideFont(getApplicationContext(), "SERIF", "roboto.ttf");

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        firebaseStorage = FirebaseStorage.getInstance(AppConstants.STORAGE_ROOT_URL);
        storageRootRef = firebaseStorage.getReference();

        firebaseFirestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firebaseFirestore.setFirestoreSettings(settings);

        offlineDb = Room.databaseBuilder(getApplicationContext(), OfflineDatabase.class, DbConstants.DATABASE)
                .fallbackToDestructiveMigration()
                .build();

        merlin = new Merlin.Builder().withConnectableCallbacks().withDisconnectableCallbacks().build(this);
        merlin.registerConnectable(this);
        merlin.registerDisconnectable(this);

        merlin.bind();

        mInstance = this;
    }

    public static synchronized AppController getInstance() {
        return mInstance;
    }

    public void prepareFirebaseUser() {
        firebaseUser = firebaseAuth.getCurrentUser();
    }

    @Override
    public void onConnect() {
        networkStatus = true;

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
                .setContentText("Network Connected").build();

        mNotificationManager.notify(0, notification);
    }

    @Override
    public void onDisconnect() {
        networkStatus = false;

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

        mNotificationManager.notify(0, notification);
    }
}
