package de.welthungerhilfe.cgm.scanner.helper.syncdata;

import android.accounts.Account;
import android.arch.lifecycle.ViewModelProviders;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.perf.metrics.AddTrace;

import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.FileLog;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.MeasureRepository;
import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.MeasureViewModel;
import de.welthungerhilfe.cgm.scanner.datasource.viewmodel.PersonViewModel;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.datasource.models.Measure;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnFileLogLoad;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnMeasureLoad;
import de.welthungerhilfe.cgm.scanner.ui.delegators.OnPersonLoad;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SYNC_FLEXTIME;
import static de.welthungerhilfe.cgm.scanner.helper.AppConstants.SYNC_INTERVAL;

public class SyncAdapter extends AbstractThreadedSyncAdapter implements OnPersonLoad, OnMeasureLoad, OnFileLogLoad {
    private long prevTimestamp;
    private SessionManager session;

    private PersonRepository personRepository;
    private MeasureRepository measureRepository;
    private FileLogRepository fileLogRepository;

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

        AppController.getInstance().firebaseFirestore.collection("persons")
                .whereGreaterThan("timestamp", prevTimestamp)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                Person person = document.toObject(Person.class);

                                if (person.getTimestamp() > prevTimestamp) {
                                    if (!isSyncing) {
                                        isSyncing = !isSyncing;
                                        Crashlytics.setString("sync_data", "app is syncing now");
                                    }

                                    if (prevTimestamp == 0 && person.getDeleted()) {

                                    } else {
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
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    for (DocumentSnapshot snapshot : task.getResult()) {
                                                        if (!isSyncing) {
                                                            isSyncing = !isSyncing;
                                                            Crashlytics.setString("sync_data", "app is syncing now");
                                                        }

                                                        Measure measure = snapshot.toObject(Measure.class);

                                                        if (prevTimestamp == 0 && measure.getDeleted()) {

                                                        } else {
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
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                session.setSyncTimestamp(prevTimestamp);
                                            }
                                        });
                            }

                            session.setSyncTimestamp(Utils.getUniversalTimestamp());
                        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(SYNC_INTERVAL, SYNC_FLEXTIME).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();

            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account, authority, new Bundle(), SYNC_INTERVAL);
        }
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
    public void onPersonLoaded(List<Person> personList) {
        for (int i = 0; i < personList.size(); i++) {
            personList.get(i).setTimestamp(Utils.getUniversalTimestamp());

            int finalI = i;
            AppController.getInstance().firebaseFirestore.collection("persons")
                    .document(personList.get(i).getId())
                    .set(personList.get(i))
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            personList.get(finalI).setTimestamp(prevTimestamp);

                            session.setSyncTimestamp(prevTimestamp);
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            personRepository.updatePerson(personList.get(finalI));

                            session.setSyncTimestamp(Utils.getUniversalTimestamp());
                        }
                    });
        }
    }

    @Override
    @AddTrace(name = "onMeasureLoaded", enabled = true)
    public void onMeasureLoaded(List<Measure> measureList) {
        for (int i = 0; i < measureList.size(); i++) {
            measureList.get(i).setTimestamp(Utils.getUniversalTimestamp());

            int finalI = i;
            AppController.getInstance().firebaseFirestore.collection("persons")
                    .document(measureList.get(i).getPersonId())
                    .collection("measures")
                    .document(measureList.get(i).getId())
                    .set(measureList.get(i))
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            measureList.get(finalI).setTimestamp(prevTimestamp);

                            session.setSyncTimestamp(prevTimestamp);
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            measureRepository.updateMeasure(measureList.get(finalI));

                            session.setSyncTimestamp(Utils.getUniversalTimestamp());
                        }
                    });
        }
    }

    @Override
    public void onFileLogLoaded(List<FileLog> list) {
        for (int i = 0; i < list.size(); i++) {
            AppController.getInstance().firebaseFirestore.collection("artefacts")
                    .document(list.get(i).getId())
                    .set(list.get(i))
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            session.setSyncTimestamp(prevTimestamp);
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            session.setSyncTimestamp(Utils.getUniversalTimestamp());
                        }
                    });
        }
    }
}
