/*
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
package de.welthungerhilfe.cgm.scanner.datasource.database;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;

import android.content.Context;

import androidx.annotation.NonNull;

import de.welthungerhilfe.cgm.scanner.datasource.dao.DeviceDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.FileLogDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.LanguageSelectedDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.MeasureDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.PersonDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.PostScanResultDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.WorkfolwDao;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.LanguageSelected;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.PostScanResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.Workflow;

@Database(entities = {Person.class, Measure.class, FileLog.class, Device.class, PostScanResult.class, Workflow.class, LanguageSelected.class}, version = 33)
public abstract class CgmDatabase extends RoomDatabase {
    private static final Object sLock = new Object();

    private static CgmDatabase instance;

    public abstract PersonDao personDao();

    public abstract MeasureDao measureDao();

    public abstract FileLogDao fileLogDao();

    public abstract DeviceDao deviceDao();

    public abstract PostScanResultDao postScanResultDao();

    public abstract WorkfolwDao workfolwDao();

    public abstract LanguageSelectedDao languageSelectedDao();

    public static final int version = 33;

    public static final String DATABASE = "offline_db";

    public static final String TABLE_PERSON = "persons";
    public static final String TABLE_MEASURE = "measures";
    public static final String TABLE_FILE_LOG = "file_logs";
    public static final String TABLE_DEVICE = "devices";
    public static final String TABLE_POST_SCAN_RESULT = "post_scan_result";
    public static final String TABLE_WORKFLOWS = "workflows";
    public static final String TABLE_LANGUAGE_SELECTED ="language_selected";



    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `measures` ADD COLUMN `qrCode` TEXT;");

            database.execSQL("ALTER TABLE `file_logs` ADD COLUMN `age` REAL;");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE persons ADD COLUMN schema_version INTEGER NOT NULL DEFAULT 0;");
            database.execSQL("ALTER TABLE measures ADD COLUMN schema_version INTEGER NOT NULL DEFAULT 0;");
            database.execSQL("ALTER TABLE file_logs ADD COLUMN schema_version INTEGER NOT NULL DEFAULT 0;");


            database.execSQL("CREATE TABLE `artifact_result` (`artifact_id` TEXT NOT NULL, `measure_id` TEXT NOT NULL, PRIMARY KEY(`artifact_id`))");

            database.execSQL("ALTER TABLE `artifact_result` ADD COLUMN `type` TEXT;");
            database.execSQL("ALTER TABLE `artifact_result` ADD COLUMN `key` INTEGER DEFAULT 0 NOT NULL;");
            database.execSQL("ALTER TABLE `artifact_result` ADD COLUMN `real` REAL DEFAULT 0 NOT NULL;");
            database.execSQL("ALTER TABLE `artifact_result` ADD COLUMN `confidence_value` TEXT;");
            database.execSQL("ALTER TABLE `artifact_result` ADD COLUMN `misc` TEXT;");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `devices` (`id` TEXT NOT NULL, `uuid` TEXT, `create_timestamp` INTEGER NOT NULL, `sync_timestamp` INTEGER NOT NULL, `new_artifact_file_size_mb` REAL NOT NULL, `new_artifacts` INTEGER NOT NULL, `deleted_artifacts` INTEGER NOT NULL, `total_artifact_file_size_mb` REAL NOT NULL, `total_artifacts` INTEGER NOT NULL, `own_measures` INTEGER NOT NULL, `own_persons` INTEGER NOT NULL, `created_by` TEXT, `total_measures` INTEGER NOT NULL, `total_persons` INTEGER NOT NULL, `app_version` TEXT, `schema_version` INTEGER NOT NULL, PRIMARY KEY(`id`))");

            database.execSQL("ALTER TABLE `file_logs` ADD COLUMN `measureId` TEXT;");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `measure_result` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `measure_id` TEXT, `model_id` TEXT, `key` TEXT, `confidence_value` REAL NOT NULL, `float_value` REAL NOT NULL, `json_value` TEXT)");
        }
    };

    public static final Migration MIGRATION_5_4 = new Migration(5, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS `measure_result`");
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measures ADD COLUMN artifact_synced INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measures ADD COLUMN uploaded_at INTEGER NOT NULL DEFAULT 0;");
            database.execSQL("ALTER TABLE measures ADD COLUMN resulted_at INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measures ADD COLUMN received_at INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measures ADD COLUMN heightConfidence REAL NOT NULL DEFAULT 0;");
            database.execSQL("ALTER TABLE measures ADD COLUMN weightConfidence REAL NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measures ADD COLUMN scannedBy TEXT;");
        }
    };

    public static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE persons ADD COLUMN serverId TEXT;");
        }
    };

    public static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measures ADD COLUMN measureServerKey TEXT;");
            database.execSQL("ALTER TABLE measures ADD COLUMN personServerKey TEXT;");

        }
    };
    public static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE persons ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0;");
            database.execSQL("ALTER TABLE measures ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0;");
        }
    };
    public static final Migration MIGRATION_13_14 = new Migration(13, 14) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE file_logs ADD COLUMN step INTEGER NOT NULL DEFAULT 0;");
        }
    };
    public static final Migration MIGRATION_14_15 = new Migration(14, 15) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE file_logs ADD COLUMN serverId TEXT;");
        }
    };

    public static final Migration MIGRATION_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE persons ADD COLUMN environment INTEGER NOT NULL DEFAULT 0;");
            database.execSQL("ALTER TABLE measures ADD COLUMN environment INTEGER NOT NULL DEFAULT 0;");
            database.execSQL("ALTER TABLE file_logs ADD COLUMN environment INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `post_scan_result` (`id` TEXT PRIMARY KEY NOT NULL, `measure_id` TEXT, `isSynced` INTEGER NOT NULL DEFAULT 0, `timestamp` INTEGER NOT NULL, `environment` INTEGER NOT NULL DEFAULT 0)");

        }
    };

    public static final Migration MIGRATION_17_18 = new Migration(17, 18) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS `artifact_result`");
            database.execSQL("DROP TABLE IF EXISTS `measure_result`");
        }
    };

    public static final Migration MIGRATION_18_19 = new Migration(18, 19) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE persons ADD COLUMN isDenied INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_19_20 = new Migration(19, 20) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE devices ADD COLUMN issues TEXT;");
        }
    };

    public static final Migration MIGRATION_20_21 = new Migration(20, 21) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE persons ADD COLUMN device_updated_at_timestamp INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_21_22 = new Migration(21, 22) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measures ADD COLUMN std_test_qr_code TEXT;");
        }
    };

    public static final Migration MIGRATION_22_23 = new Migration(22, 23) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE persons ADD COLUMN last_sync_measurments INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_23_24 = new Migration(23, 24) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE file_logs ADD COLUMN childDetected INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_24_25 = new Migration(24, 25) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `workflows` (`id` TEXT PRIMARY KEY NOT NULL, `eligible` INTEGER NOT NULL DEFAULT 0, `name` TEXT, `result_binding` TEXT, `result_format` TEXT, `version` TEXT, `input_format` TEXT, `model` TEXT)");
        }
    };

    public static final Migration MIGRATION_25_26 = new Migration(25, 26) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE file_logs ADD COLUMN childHeight REAL NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_26_27 = new Migration(26, 27) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE file_logs ADD COLUMN scanServerId TEXT;");
            database.execSQL("ALTER TABLE file_logs ADD COLUMN artifactId TEXT;");
            database.execSQL("ALTER TABLE file_logs ADD COLUMN autoDetectSynced INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_27_28 = new Migration(27, 28) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE file_logs ADD COLUMN childHeightSynced INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_28_29 = new Migration(28, 29) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measures ADD COLUMN positive_height_error REAL NOT NULL DEFAULT 0;");
            database.execSQL("ALTER TABLE measures ADD COLUMN negative_height_error REAL NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_29_30 = new Migration(29, 30) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE workflows ADD COLUMN environment INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_30_31 = new Migration(30, 31) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `language_selected` (`id` TEXT PRIMARY KEY NOT NULL, `selectedLanguage` TEXT)");
        }
    };

    public static final Migration MIGRATION_31_32 = new Migration(31, 32) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE persons ADD COLUMN last_updated INTEGER NOT NULL DEFAULT 0;");
        }
    };

    public static final Migration MIGRATION_32_33 = new Migration(32, 33) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE file_logs ADD COLUMN poseScore REAL NOT NULL DEFAULT 0;");
            database.execSQL("ALTER TABLE file_logs ADD COLUMN poseCoordinates TEXT;");
            database.execSQL("ALTER TABLE file_logs ADD COLUMN poseScoreSynced INTEGER NOT NULL DEFAULT 0;");
        }
    };


    public static CgmDatabase getInstance(Context context) {
        synchronized (sLock) {
            if (instance == null) {
                instance = Room.databaseBuilder(context.getApplicationContext(), CgmDatabase.class, DATABASE)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_4,
                                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                                MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
                                MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18,MIGRATION_18_19, MIGRATION_19_20,
                                MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25,
                                MIGRATION_25_26,MIGRATION_26_27,MIGRATION_27_28,MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31,
                                MIGRATION_31_32,MIGRATION_32_33)
                        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                        .allowMainThreadQueries()
                        .build();
            }
            return instance;
        }
    }
}