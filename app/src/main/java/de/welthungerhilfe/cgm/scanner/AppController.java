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

import android.app.ActivityManager;
import android.app.Application;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

//import com.amitshekhar.DebugDB;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.BuildConfig;
import com.crashlytics.android.core.CrashlyticsCore;
import com.crashlytics.android.core.CrashlyticsListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.DbConstants;
import de.welthungerhilfe.cgm.scanner.helper.LanguageHelper;
import de.welthungerhilfe.cgm.scanner.helper.OfflineDatabase;
import de.welthungerhilfe.cgm.scanner.utils.Utils;
import io.fabric.sdk.android.Fabric;

public class AppController extends Application {
    public static final String TAG = AppController.class.getSimpleName();

    private static AppController mInstance;

    public FirebaseAuth firebaseAuth;
    public FirebaseUser firebaseUser;

    public FirebaseStorage firebaseStorage;
    public StorageReference storageRootRef;

    public FirebaseFirestore firebaseFirestore;

    public FirebaseRemoteConfig firebaseConfig;

    public OfflineDatabase offlineDb;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measures ADD COLUMN createdBy TEXT;");
            database.execSQL("ALTER TABLE measures ADD COLUMN latitude REAL;");
            database.execSQL("ALTER TABLE measures ADD COLUMN longitude REAL;");
            database.execSQL("ALTER TABLE measures ADD COLUMN address TEXT;");
            database.execSQL("ALTER TABLE measures ADD COLUMN oedema INTEGER DEFAULT '0' NOT NULL;");

            database.execSQL("ALTER TABLE persons ADD COLUMN createdBy TEXT;");
            database.execSQL("ALTER TABLE persons ADD COLUMN latitude REAL;");
            database.execSQL("ALTER TABLE persons ADD COLUMN longitude REAL;");
            database.execSQL("ALTER TABLE persons ADD COLUMN address TEXT;");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE file_logs (" +
                    " id TEXT NOT NULL PRIMARY KEY," +
                    " type TEXT," +
                    " path TEXT," +
                    " hashValue TEXT," +
                    " fileSize INTEGER NOT NULL," +
                    " uploadDate INTEGER NOT NULL," +
                    " deleted INTEGER NOT NULL," +
                    " qrCode TEXT," +
                    " createDate INTEGER NOT NULL," +
                    " createdBy TEXT" +
                    ");");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measures ADD COLUMN deleted INTEGER DEFAULT '0' NOT NULL;");
            database.execSQL("ALTER TABLE measures ADD COLUMN deletedBy TEXT;");

            database.execSQL("ALTER TABLE persons ADD COLUMN deleted INTEGER DEFAULT '0' NOT NULL;");
            database.execSQL("ALTER TABLE persons ADD COLUMN deletedBy TEXT;");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());

        CrashlyticsCore core = new CrashlyticsCore
                .Builder()
                .listener(new CrashlyticsListener() {
                    @Override
                    public void crashlyticsDidDetectCrashDuringPreviousExecution() {
                        // TODO: do something when crash occurs
                    }
                })
                .build();

        final Fabric fabric = new Fabric.Builder(this)
                .kits(new Crashlytics.Builder().core(core).build())
                .debuggable(true)
                .build();
        Fabric.with(fabric);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Utils.overrideFont(getApplicationContext(), "SERIF", "roboto.ttf");

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        // firebase_database_url is generated by google services plugin
        // https://developers.google.com/android/guides/google-services-plugin
        firebaseStorage = FirebaseStorage.getInstance("gs://"+R.string.google_storage_bucket);

        storageRootRef = firebaseStorage.getReference();

        firebaseFirestore = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        firebaseFirestore.setFirestoreSettings(settings);

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setDeveloperModeEnabled(true).build();
        firebaseConfig = FirebaseRemoteConfig.getInstance();
        firebaseConfig.setConfigSettings(configSettings);
        firebaseConfig.setDefaults(R.xml.remoteconfig);

        offlineDb = Room.databaseBuilder(getApplicationContext(), OfflineDatabase.class, DbConstants.DATABASE).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING).build();
        /*
        offlineDb = Room.databaseBuilder(getApplicationContext(), OfflineDatabase.class, DbConstants.DATABASE).fallbackToDestructiveMigration().addCallback(new RoomDatabase.Callback() {
            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);

                SessionManager session = new SessionManager(getApplicationContext());
                session.setSyncTimestamp(0);

                AccountManager accountManager = AccountManager.get(getApplicationContext());
                Account[] accounts = accountManager.getAccounts();

                if (accounts.length > 0) {
                    SyncAdapter.startImmediateSync(accounts[0], getApplicationContext());
                }
            }
        }).build();
        */


        //Log.e("Offline DB", DebugDB.getAddressLog());

        mInstance = this;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LanguageHelper.onAttach(base));
    }

    public boolean isAdmin() {
        boolean isAdmin = false;

        String currentUser = AppController.getInstance().firebaseAuth.getCurrentUser().getEmail();
        String[] admins = AppController.getInstance().firebaseConfig.getString(AppConstants.CONFIG_ADMINS).split(",");

        for (int i = 0; i < admins.length; i++) {
            if (admins[i].equals(currentUser)) {
                isAdmin = true;
                break;
            }
        }

        return isAdmin;
    }

    public static synchronized AppController getInstance() {
        return mInstance;
    }

    public void prepareFirebaseUser() {
        firebaseUser = firebaseAuth.getCurrentUser();
    }

    public String getPersonId(String name) {
        return Utils.getAndroidID(getContentResolver()) + "_" + name + "_" + Utils.getUniversalTimestamp() + "_" + Utils.getSaltString(16);
    }

    public String getMeasureId() {
        return Utils.getAndroidID(getContentResolver()) + "_measure_" + Utils.getUniversalTimestamp() + "_" + Utils.getSaltString(16);
    }

    public String getArtefactId(String type) {
        return Utils.getAndroidID(getContentResolver()) + "_" + type + "_" + Utils.getUniversalTimestamp() + "_" + Utils.getSaltString(16);
    }

    public String getArtefactId(String type, long timestamp) {
        return Utils.getAndroidID(getContentResolver()) + "_" + type + "_" + String.valueOf(timestamp) + "_" + Utils.getSaltString(16);
    }
}
