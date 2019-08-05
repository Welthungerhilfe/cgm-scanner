package de.welthungerhilfe.cgm.scanner.datasource.database;


import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import de.welthungerhilfe.cgm.scanner.datasource.dao.Artifact_qualityDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.FileLogDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.MeasureDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.PersonDao;
import de.welthungerhilfe.cgm.scanner.datasource.models.Artifact_quality;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

@Database(entities = {Person.class, Measure.class, FileLog.class, Artifact_quality.class}, version = 2)
public abstract class CgmDatabase extends RoomDatabase {
    private static final Object sLock = new Object();

    private static CgmDatabase instance;

    public abstract PersonDao personDao();
    public abstract MeasureDao measureDao();
    public abstract FileLogDao fileLogDao();
    public abstract Artifact_qualityDao artifact_qualityDao();

    public static final String DATABASE = "offline_db";

    public static final String TABLE_PERSON = "persons";
    public static final String TABLE_CONSENT = "consents";
    public static final String TABLE_MEASURE = "measures";
    public static final String TABLE_FILE_LOG = "file_logs";
    public static final String TABLE_ARTIFACT_QUALITY="artifact_quality";

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measures ADD COLUMN qrCode TEXT;");

            database.execSQL("ALTER TABLE file_logs ADD COLUMN age REAL;");
        }
    };

    public static CgmDatabase getInstance(Context context) {
        synchronized (sLock) {
            if (instance == null) {
                instance = Room.databaseBuilder(context.getApplicationContext(), CgmDatabase.class, DATABASE)
                        .addMigrations(MIGRATION_1_2)
                        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                        .build();
            }
            return instance;
        }
    }
}
