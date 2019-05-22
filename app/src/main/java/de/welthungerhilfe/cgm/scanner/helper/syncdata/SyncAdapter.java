package de.welthungerhilfe.cgm.scanner.helper.syncdata;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.perf.metrics.AddTrace;

import com.google.gson.Gson;
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.queue.*;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.List;
import java.util.Objects;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
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
    private SessionManager session;

    private PersonRepository personRepository;
    private MeasureRepository measureRepository;
    private FileLogRepository fileLogRepository;

    private CloudQueue personQueue;
    private CloudQueue measureQueue;
    private CloudQueue artifactQueue;

    private boolean isSyncing;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        session = new SessionManager(context);
        isSyncing = false;

        personRepository = PersonRepository.getInstance(context);
        measureRepository = MeasureRepository.getInstance(context);
        fileLogRepository = FileLogRepository.getInstance(context);
    }

    @Override
    @AddTrace(name = "onPerformSync", enabled = true)
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        prevTimestamp = session.getSyncTimestamp();

        // Todo;
        personRepository.getSyncablePerson(this, prevTimestamp);
        measureRepository.getSyncableMeasure(this, prevTimestamp);
        fileLogRepository.getSyncableLog(this, prevTimestamp);

        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(getAzureConnection());
            CloudQueueClient queueClient = storageAccount.createCloudQueueClient();

            personQueue = queueClient.getQueueReference("persons");
            personQueue.createIfNotExists();

            measureQueue = queueClient.getQueueReference("measures");
            measureQueue.createIfNotExists();

            artifactQueue = queueClient.getQueueReference("artifacts");
            artifactQueue.createIfNotExists();

        } catch (URISyntaxException | InvalidKeyException | StorageException e) {
            e.printStackTrace();
        }

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
            personList.get(i).setTimestamp(Utils.getUniversalTimestamp());

            try {
                String content = gson.toJson(personList.get(i));
                CloudQueueMessage message = new CloudQueueMessage(personList.get(i).getId());
                message.setMessageContent(content);
                personQueue.addMessage(message);

                personRepository.updatePerson(personList.get(i));
                session.setSyncTimestamp(Utils.getUniversalTimestamp());
            } catch (StorageException e) {
                personList.get(i).setTimestamp(prevTimestamp);

                session.setSyncTimestamp(prevTimestamp);
            }

            /*
            int finalI = i;
            AppController.getInstance().firebaseFirestore.collection("persons")
                    .document(personList.get(i).getId())
                    .set(personList.get(i))
                    .addOnFailureListener(e -> {
                        personList.get(finalI).setTimestamp(prevTimestamp);

                        session.setSyncTimestamp(prevTimestamp);
                    })
                    .addOnSuccessListener(aVoid -> {
                        personRepository.updatePerson(personList.get(finalI));

                        session.setSyncTimestamp(Utils.getUniversalTimestamp());
                    });
                    */
        }
    }

    @Override
    @AddTrace(name = "onMeasureLoaded", enabled = true)
    public void onMeasuresLoaded(List<Measure> measureList) {
        for (int i = 0; i < measureList.size(); i++) {
            measureList.get(i).setTimestamp(Utils.getUniversalTimestamp());



            int finalI = i;
            AppController.getInstance().firebaseFirestore.collection("persons")
                    .document(measureList.get(i).getPersonId())
                    .collection("measures")
                    .document(measureList.get(i).getId())
                    .set(measureList.get(i))
                    .addOnFailureListener(e -> {
                        measureList.get(finalI).setTimestamp(prevTimestamp);

                        session.setSyncTimestamp(prevTimestamp);
                    })
                    .addOnSuccessListener(aVoid -> {
                        measureRepository.updateMeasure(measureList.get(finalI));

                        session.setSyncTimestamp(Utils.getUniversalTimestamp());
                    });
        }
    }

    @Override
    public void onFileLogsLoaded(List<FileLog> list) {
        Gson gson = new Gson();
        for (int i = 0; i < list.size(); i++) {
            new AsyncTask<FileLog, Void, Void>() {
                @Override
                protected Void doInBackground(FileLog... logs) {
                    try {
                        String content = gson.toJson(logs[0]);
                        CloudQueueMessage message = new CloudQueueMessage(logs[0].getId());
                        message.setMessageContent(content);
                        artifactQueue.addMessage(message);

                        session.setSyncTimestamp(Utils.getUniversalTimestamp());
                    } catch (StorageException e) {
                        logs[0].setUploadDate(prevTimestamp);

                        session.setSyncTimestamp(prevTimestamp);
                    }

                    return null;
                }
            }.execute(list.get(i));

            /*
            AppController.getInstance().firebaseFirestore.collection("artefacts")
                    .document(list.get(i).getId())
                    .set(list.get(i))
                    .addOnFailureListener(e -> session.setSyncTimestamp(prevTimestamp))
                    .addOnSuccessListener(aVoid -> session.setSyncTimestamp(Utils.getUniversalTimestamp()));
                    */
        }
    }

    private String getAzureConnection() {
        return String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s", AZURE_ACCOUNT_NAME, AZURE_ACCOUNT_KEY);
    }
}
