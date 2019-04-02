package de.welthungerhilfe.cgm.scanner.datasource.database;


import android.arch.lifecycle.LiveData;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import de.welthungerhilfe.cgm.scanner.datasource.dao.FileLogDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.MeasureDao;
import de.welthungerhilfe.cgm.scanner.datasource.dao.PersonDao;
import de.welthungerhilfe.cgm.scanner.datasource.datasource.person.PersonDataFactory;
import de.welthungerhilfe.cgm.scanner.datasource.models.Consent;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;

@Database(entities = {Person.class, Consent.class, Measure.class, FileLog.class}, version = 5)
public abstract class CgmDatabase extends RoomDatabase {
    private static final Object sLock = new Object();

    private static CgmDatabase instance;

    public abstract PersonDao personDao();
    public abstract MeasureDao measureDao();
    public abstract FileLogDao fileLogDao();

    public static final String DATABASE = "offline_db";

    public static final String TABLE_PERSON = "persons";
    public static final String TABLE_CONSENT = "consents";
    public static final String TABLE_MEASURE = "measures";
    public static final String TABLE_FILE_LOG = "file_logs";

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

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE file_logs ADD COLUMN status INTEGER DEFAULT '0' NOT NULL;");
        }
    };

    public static CgmDatabase getInstance(Context context) {
        synchronized (sLock) {
            if (instance == null) {
                instance = Room.databaseBuilder(context.getApplicationContext(), CgmDatabase.class, DATABASE)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                        .build();
            }
            return instance;
        }
    }

    public LiveData<PagedList<Person>> getPersons() {
        PagedList.Config pagedListConfig = (new PagedList.Config.Builder()).setEnablePlaceholders(false)
                .setInitialLoadSizeHint(1000)
                .setPageSize(1000)
                .build();

        Executor executor = Executors.newFixedThreadPool(10);
        PersonDataFactory dataFactory = new PersonDataFactory(personDao());
        LivePagedListBuilder livePagedListBuilder = new LivePagedListBuilder(dataFactory, pagedListConfig).setFetchExecutor(executor);

        return livePagedListBuilder.build();
    }
}
