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
package de.welthungerhilfe.cgm.scanner.datasource.repository;

import android.annotation.SuppressLint;

import androidx.lifecycle.LiveData;

import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.datasource.database.CgmDatabase;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.UploadStatus;

public class FileLogRepository {

    public interface OnFileLogsLoad {
        void onFileLogsLoaded(List<FileLog> list);
    }

    private static FileLogRepository instance;

    private CgmDatabase database;

    private FileLogRepository(Context context) {
        database = CgmDatabase.getInstance(context);
    }

    public static FileLogRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FileLogRepository(context);
        }
        return instance;
    }

    @SuppressLint("StaticFieldLeak")
    public void loadQueuedData(OnFileLogsLoad listener, int environment) {
        new AsyncTask<Void, Void, List<FileLog>>() {
            @Override
            protected List<FileLog> doInBackground(Void... voids) {
                return database.fileLogDao().loadQueuedData(environment);
            }

            @Override
            public void onPostExecute(List<FileLog> data) {
                listener.onFileLogsLoaded(data);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public List<FileLog> loadConsentFile(int environment) {
        return database.fileLogDao().loadConsentFile(environment);
    }

    public void insertFileLog(FileLog log) {
        database.fileLogDao().saveFileLog(log);
    }

    public void updateFileLog(FileLog log) {
        database.fileLogDao().updateFileLog(log);
    }

    public long getArtifactCount() {
        return database.fileLogDao().getArtifactCount();
    }

    public long getDeletedArtifactCount() {
        return database.fileLogDao().getDeletedArtifactCount();
    }

    public long getTotalArtifactCount() {
        return database.fileLogDao().getTotalArtifactCount();
    }

    public double getArtifactFileSize() {
        return database.fileLogDao().getArtifactFileSize();
    }

    public double getTotalArtifactFileSize() {
        return database.fileLogDao().getTotalArtifactFileSize();
    }

    public List<FileLog> getAll() {
        return database.fileLogDao().getAll();
    }

    public List<FileLog> getArtifactsForMeasure(String measureId, int environment) {
        return database.fileLogDao().getArtifactsForMeasure(measureId, environment);
    }

    public LiveData<UploadStatus> getMeasureUploadProgress(String measureId) {
        return database.fileLogDao().getMeasureUploadProgress(measureId);
    }

    public FileLog getFileLogByFileId(String fileserverId){
        return database.fileLogDao().getFileLogByFileServerId(fileserverId);
    }

    public FileLog getFileLogByArtifactId(String artifactId){
        return database.fileLogDao().getFileLogByArtifactId(artifactId);
    }

    public List<FileLog> loadAutoDetectedFileLog(int environment) {
        return database.fileLogDao().loadAutoDetectedFileLog(environment);
    }

    public List<FileLog> loadAppHeightFileLog(int environment) {
        return database.fileLogDao().loadAppHeightFileLog(environment);
    }

    public List<FileLog> loadAppPoseScoreFileLog(int environment) {
        return database.fileLogDao().loadAppPoseScoreFileLog(environment);
    }

    public List<FileLog> loadChildDistanceFileLog(int environment) {
        return database.fileLogDao().loadChildDistanceFileLog(environment);
    }
}
