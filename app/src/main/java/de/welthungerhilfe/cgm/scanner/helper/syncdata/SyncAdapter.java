package de.welthungerhilfe.cgm.scanner.helper.syncdata;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.firebase.perf.metrics.AddTrace;

import com.google.gson.Gson;
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.queue.*;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnFileLogsLoad;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnMeasuresLoad;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnPersonsLoad;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.AZURE_ACCOUNT_KEY;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.AZURE_ACCOUNT_NAME;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SYNC_FLEXTIME;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SYNC_INTERVAL;

public class SyncAdapter extends AbstractThreadedSyncAdapter implements OnPersonsLoad, OnMeasuresLoad, OnFileLogsLoad {
    private long prevTimestamp;

    private CloudQueue personQueue;
    private CloudQueue measureQueue;
    private CloudQueue artifactQueue;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    @AddTrace(name = "onPerformSync", enabled = true)
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        new MessageTask().execute();
        /*
        AppController.getInstance().firebaseFirestore.collection("persons")
                .whereGreaterThan("timestamp", prevTimestamp)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                            Person person = document.toObject(Person.class);

                            if (person != null && person.getTimestamp() > prevTimestamp) {
                                if (!isSyncing) {
                                    isSyncing = !isSyncing;
                                    Crashlytics.setString("sync_data", "app is syncing now");
                                }

                                if (prevTimestamp > 0 && !person.getDeleted()) {
                                    String[] arr = person.getId().split("_");
                                    try {
                                        if (Long.valueOf(arr[2]) > prevTimestamp) {     // person created after sync, so must add to local room
                                            personRepository.insertPerson(person);
                                        } else {    // created before sync, after sync person was updates, so must update in local room
                                            personRepository.updatePerson(person);
                                        }
                                    } catch (NumberFormatException e) {
                                        Crashlytics.log(0, "sync_adapter", String.format("could not get timestamp because of underline in personId: %s", person.getId()));
                                    }
                                }
                            }

                            document.getReference().collection("measures")
                                    .whereGreaterThan("timestamp", prevTimestamp)
                                    .get()
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            for (DocumentSnapshot snapshot : Objects.requireNonNull(task1.getResult())) {
                                                if (!isSyncing) {
                                                    isSyncing = !isSyncing;
                                                    Crashlytics.setString("sync_data", "app is syncing now");
                                                }

                                                Measure measure = snapshot.toObject(Measure.class);

                                                if (measure != null && prevTimestamp > 0 && !measure.getDeleted()) {
                                                    try {
                                                        String[] arr = measure.getId().split("_");
                                                        if (Long.valueOf(arr[2]) > prevTimestamp) {     // person created after sync, so must add to local room
                                                            measureRepository.insertMeasure(measure);
                                                        } else {    // created before sync, after sync person was updates, so must update in local room
                                                            measureRepository.updateMeasure(measure);
                                                        }
                                                    } catch (NumberFormatException e) {
                                                        Crashlytics.log(0, "sync_adapter", String.format("could not get timestamp because of underline in measureId: %s", measure.getId()));
                                                    }
                                                }
                                            }

                                            session.setSyncTimestamp(Utils.getUniversalTimestamp());
                                        }
                                    })
                                    .addOnFailureListener(e -> session.setSyncTimestamp(prevTimestamp));
                        }

                        session.setSyncTimestamp(Utils.getUniversalTimestamp());
                    }
                });
                */
    }

    private void startSyncing() {
        prevTimestamp = AppController.getInstance().session.getSyncTimestamp();

        AppController.getInstance().personRepository.getSyncablePerson(this, prevTimestamp);
        AppController.getInstance().measureRepository.getSyncableMeasure(this, prevTimestamp);
        AppController.getInstance().fileLogRepository.getSyncableLog(this, prevTimestamp);
    }

    @AddTrace(name = "syncImmediately", enabled = true)
    private static void syncImmediately(Account account, Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(account, context.getString(R.string.sync_authority), bundle);
    }

    @AddTrace(name = "configurePeriodicSync", enabled = true)
    private static void configurePeriodicSync(Account account, Context context) {

        String authority = context.getString(R.string.sync_authority);

        SyncRequest request = new SyncRequest.Builder().
                syncPeriodic(SYNC_INTERVAL, SYNC_FLEXTIME).
                setSyncAdapter(account, authority).
                setExtras(new Bundle()).build();

        ContentResolver.requestSync(request);
    }

    @AddTrace(name = "startPeriodicSync", enabled = true)
    public static void startPeriodicSync(Account newAccount, Context context) {

        configurePeriodicSync(newAccount, context);

        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.sync_authority), true);

        syncImmediately(newAccount, context);

    }

    @AddTrace(name = "startImmediateSync", enabled = true)
    public static void startImmediateSync(Account newAccount, Context context) {

        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.sync_authority), true);

        syncImmediately(newAccount, context);
    }

    @Override
    @AddTrace(name = "onPersonLoaded", enabled = true)
    public void onPersonsLoaded(List<Person> personList) {
        Gson gson = new Gson();

        for (int i = 0; i < personList.size(); i++) {
            String content = gson.toJson(personList.get(i));
            CloudQueueMessage message = new CloudQueueMessage(personList.get(i).getId());
            message.setMessageContent(content);

            new WriteTask(personQueue, message).execute();
        }
    }

    @Override
    @AddTrace(name = "onMeasureLoaded", enabled = true)
    public void onMeasuresLoaded(List<Measure> measureList) {
        Gson gson = new Gson();

        for (int i = 0; i < measureList.size(); i++) {
            String content = gson.toJson(measureList.get(i));
            CloudQueueMessage message = new CloudQueueMessage(measureList.get(i).getId());
            message.setMessageContent(content);

            new WriteTask(measureQueue, message).execute();
        }
    }

    @Override
    public void onFileLogsLoaded(List<FileLog> list) {
        Gson gson = new Gson();
        for (int i = 0; i < list.size(); i++) {
            String content = gson.toJson(list.get(i));
            CloudQueueMessage message = new CloudQueueMessage(list.get(i).getId());
            message.setMessageContent(content);

            new WriteTask(artifactQueue, message).execute();
        }
    }

    @SuppressLint("StaticFieldLeak")
    class MessageTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                personQueue = AppController.getInstance().queueClient.getQueueReference("persons");
                personQueue.createIfNotExists();

                measureQueue = AppController.getInstance().queueClient.getQueueReference("measures");
                measureQueue.createIfNotExists();

                artifactQueue = AppController.getInstance().queueClient.getQueueReference("artifacts");
                artifactQueue.createIfNotExists();
            } catch (URISyntaxException | StorageException e) {
                e.printStackTrace();
            }

            return null;
        }

        public void onPostExecute(Void result) {
            startSyncing();
        }
    }

    class WriteTask extends AsyncTask<Void, Void, Void> {
        CloudQueue queue;
        CloudQueueMessage message;

        WriteTask(CloudQueue queue, CloudQueueMessage message) {
            this.queue = queue;
            this.message = message;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                queue.addMessage(message);

                AppController.getInstance().session.setSyncTimestamp(Utils.getUniversalTimestamp());
            } catch (StorageException e) {
                e.printStackTrace();

                AppController.getInstance().session.setSyncTimestamp(prevTimestamp);
            }
            return null;
        }
    }
}
