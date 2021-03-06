package de.welthungerhilfe.cgm.scanner;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.room.testing.MigrationTestHelper;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {
    private static final String TEST_DB = "migration-test";

    @Rule
    public MigrationTestHelper helper;

    public MigrationTest() {
        helper = new MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), CgmDatabase.class.getCanonicalName(), new FrameworkSQLiteOpenHelperFactory());
    }

    @Test
    public void migrate1To2() throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DB, 1);

        // db has schema version 1. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.
        database.execSQL("insert into measures (id, personId, date, type, age, height, weight, muac, headCircumference, artifact, visible, oedema, timestamp, createdBy, deleted, deletedBy, latitude, longitude, address) " +
                "values ('9e58cfb935e72628_measure_1529643830026_84tLtJwykTDgyGFU', '9e58cfb935e72628_accountability_1529643805220_IrLNkJ3clIXw50SK', 1529643830026, 'manual', 21, 95.6, 31.5, 23.9, 26.1, 'No Additional Info', 0, 0, 1530962124049, 'zhangnemo34@hotmail.com', 0, '', '', '', '')");

        // Prepare for the next version.
        database.close();

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        database = helper.runMigrationsAndValidate(TEST_DB, 2, true, CgmDatabase.MIGRATION_1_2);

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }
}
