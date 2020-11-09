package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.ArtifactResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.Device;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.MeasureResult;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.ui.activities.BaseActivity;

public class BackupManager {

    public static void doBackup(BaseActivity context, File folder, long timestamp, Runnable onFinished) {
        folder.mkdirs();

        ArtifactResultRepository arRepo = ArtifactResultRepository.getInstance(context);
        DeviceRepository dRepo = DeviceRepository.getInstance(context);
        FileLogRepository flRepo = FileLogRepository.getInstance(context);
        MeasureRepository mRepo = MeasureRepository.getInstance(context);
        MeasureResultRepository mrRepo = MeasureResultRepository.getInstance(context);
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

                List<ArtifactResult> arAll = arRepo.getAll();
                String arCsv = folder + File.separator + String.format(Locale.US, "%s-%d.csv", CgmDatabase.TABLE_ARTIFACT_RESULT, timestamp);
                try {
                    FileWriter writer = new FileWriter(arCsv);
                    writer.append(new ArtifactResult().getCsvHeaderString());
                    writer.append('\n');

                    for (int i = 0; i < arAll.size(); i++) {
                        writer.append(arAll.get(i).getCsvFormattedString());
                        writer.append('\n');
                    }

                    writer.close();
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


                List<MeasureResult> mrAll = mrRepo.getAll();
                String mrCsv = folder + File.separator + String.format(Locale.US, "%s-%d.csv", CgmDatabase.TABLE_MEASURE_RESULT, timestamp);
                try {
                    FileWriter writer = new FileWriter(mrCsv);
                    writer.append(new MeasureResult().getCsvHeaderString());
                    writer.append('\n');

                    for (int i = 0; i < mrAll.size(); i++) {
                        writer.append(mrAll.get(i).getCsvFormattedString());
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
