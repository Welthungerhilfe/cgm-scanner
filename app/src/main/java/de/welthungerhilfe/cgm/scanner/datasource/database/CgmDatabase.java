package de.welthungerhilfe.cgm.scanner.datasource.database;


import android.arch.lifecycle.LiveData;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.dao.MeasureDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.PersonDao;
import de.welthungerhilfe.cgm.scanner.helper.DbConstants;
import de.welthungerhilfe.cgm.scanner.datasource.models.Consent;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

@Database(entities = {Person.class, Consent.class, Measure.class, FileLog.class}, version = 4)
public abstract class CgmDatabase extends RoomDatabase {
    private static final Object sLock = new Object();

    private static CgmDatabase instance;

    public abstract PersonDao personDao();
    public abstract MeasureDao measureDao();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
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

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
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

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE measures ADD COLUMN deleted INTEGER DEFAULT '0' NOT NULL;");
            database.execSQL("ALTER TABLE measures ADD COLUMN deletedBy TEXT;");

            database.execSQL("ALTER TABLE persons ADD COLUMN deleted INTEGER DEFAULT '0' NOT NULL;");
            database.execSQL("ALTER TABLE persons ADD COLUMN deletedBy TEXT;");
        }
    };

    public static CgmDatabase getInstance(Context context) {
        synchronized (sLock) {
            if (instance == null) {
                instance = Room.databaseBuilder(context.getApplicationContext(), CgmDatabase.class, DbConstants.DATABASE)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                        .build();
            }
            return instance;
        }
    }

    public LiveData<List<Person>> getPersons(int offset, int size) {
        return personDao().getPersons(offset, size);
    }

    public LiveData<Person> getPerson(String key) {
        return personDao().findPerson(key);
    }

    public void insertPerson(Person person) {
        personDao().insertPerson(person);
    }
}
