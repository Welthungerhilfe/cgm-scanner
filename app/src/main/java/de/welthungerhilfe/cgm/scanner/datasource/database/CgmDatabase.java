package de.welthungerhilfe.cgm.scanner.datasource.database;


import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import de.welthungerhilfe.cgm.scanner.datasource.dao.ArtifactResultDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.FileLogDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.MeasureDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.PersonDao;
import de.welthungerhilfe.cgm.scanner.datasource.models.ArtifactResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

@Database(entities = {Person.class, Measure.class, FileLog.class, ArtifactResult.class}, version = 4)
public abstract class CgmDatabase extends RoomDatabase {
    private static final Object sLock = new Object();

    private static CgmDatabase instance;

    public abstract PersonDao personDao();
    public abstract MeasureDao measureDao();
    public abstract FileLogDao fileLogDao();
    public abstract ArtifactResultDao artifactResultDao();

    public static final int version = 4;

    private static final String DATABASE = "offline_db";

    public static final String TABLE_PERSON = "persons";
    public static final String TABLE_CONSENT = "consents";
    public static final String TABLE_MEASURE = "measures";
    public static final String TABLE_FILE_LOG = "file_logs";
    public static final String TABLE_ARTIFACT_RESULT="artifact_result";

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
            database.execSQL("ALTER TABLE `file_logs` ADD COLUMN `measureId` TEXT;");
        }
    };

    public static CgmDatabase getInstance(Context context) {
        synchronized (sLock) {
            if (instance == null) {
                instance = Room.databaseBuilder(context.getApplicationContext(), CgmDatabase.class, DATABASE)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                        .build();
            }
            return instance;
        }
    }
}
