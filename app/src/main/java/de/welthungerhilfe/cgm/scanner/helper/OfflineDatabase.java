package de.welthungerhilfe.cgm.scanner.helper;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.models.Consent;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.models.dao.OfflineDao;

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

@Database(entities = {Person.class, Consent.class, Measure.class, FileLog.class}, version = 4)
public abstract class OfflineDatabase extends RoomDatabase {
    private static OfflineDatabase instance;

    private static final Object sLock = new Object();

    public abstract OfflineDao offlineDao();

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

    public static OfflineDatabase getInstance(Context context) {
        synchronized (sLock) {
            if (instance == null) {
                instance = Room.databaseBuilder(context.getApplicationContext(), OfflineDatabase.class, DbConstants.DATABASE)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                        .build();
            }
            return instance;
        }
    }

    public LiveData<List<Person>> getPersons(int index, int pageSize) {
        return offlineDao().getPersons(index, pageSize);
    }

}
