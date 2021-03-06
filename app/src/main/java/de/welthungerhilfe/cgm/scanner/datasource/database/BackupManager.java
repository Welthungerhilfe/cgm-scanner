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

import android.os.AsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.datasource.models.Device;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.datasource.repository.DeviceRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;

public class BackupManager {

    public static void doBackup(BaseActivity context, File folder, long timestamp, Runnable onFinished) {
        folder.mkdirs();

        DeviceRepository dRepo = DeviceRepository.getInstance(context);
        FileLogRepository flRepo = FileLogRepository.getInstance(context);
        MeasureRepository mRepo = MeasureRepository.getInstance(context);
        PersonRepository pRepo = PersonRepository.getInstance(context);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    File dbFile = context.getDatabasePath(CgmDatabase.DATABASE);
                    FileInputStream fis = new FileInputStream(dbFile);

                    String outFileName = folder + File.separator + String.format(Locale.US, "db-%d.db", timestamp);
                    OutputStream output = new FileOutputStream(outFileName);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }

                    output.flush();
                    output.close();
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                List<Device> dAll = dRepo.getAll();
                String dCsv = folder + File.separator + String.format(Locale.US, "%s-%d.csv", CgmDatabase.TABLE_DEVICE, timestamp);
                try {
                    FileWriter writer = new FileWriter(dCsv);
                    writer.append(new Device().getCsvHeaderString());
                    writer.append('\n');

                    for (int i = 0; i < dAll.size(); i++) {
                        writer.append(dAll.get(i).getCsvFormattedString());
                        writer.append('\n');
                    }

                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                List<FileLog> flAll = flRepo.getAll();
                String fCsv = folder + File.separator + String.format(Locale.US, "%s-%d.csv", CgmDatabase.TABLE_FILE_LOG, timestamp);
                try {
                    FileWriter writer = new FileWriter(fCsv);
                    writer.append(new FileLog().getCsvHeaderString());
                    writer.append('\n');

                    for (int i = 0; i < flAll.size(); i++) {
                        writer.append(flAll.get(i).getCsvFormattedString());
                        writer.append('\n');
                    }

                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                List<Measure> mAll = mRepo.getAll();
                String mCsv = folder + File.separator + String.format(Locale.US, "%s-%d.csv", CgmDatabase.TABLE_MEASURE, timestamp);
                try {
                    FileWriter writer = new FileWriter(mCsv);
                    writer.append(new Measure().getCsvHeaderString());
                    writer.append('\n');

                    for (int i = 0; i < mAll.size(); i++) {
                        writer.append(mAll.get(i).getCsvFormattedString());
                        writer.append('\n');
                    }

                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                List<Person> pAll = pRepo.getAll();
                String pCsv = folder + File.separator + String.format(Locale.US, "%s-%d.csv", CgmDatabase.TABLE_PERSON, timestamp);
                try {
                    FileWriter writer = new FileWriter(pCsv);
                    writer.append(new Person().getCsvHeaderString());
                    writer.append('\n');

                    for (int i = 0; i < pAll.size(); i++) {
                        writer.append(pAll.get(i).getCsvFormattedString());
                        writer.append('\n');
                    }

                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            public void onPostExecute(Void result) {
                context.runOnUiThread(onFinished);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
