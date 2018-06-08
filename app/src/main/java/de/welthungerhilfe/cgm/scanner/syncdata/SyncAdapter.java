package de.welthungerhilfe.cgm.scanner.syncdata;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.List;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.models.tasks.OfflineTask;
import de.welthungerhilfe.cgm.scanner.repositories.OfflineRepository;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class SyncAdapter extends AbstractThreadedSyncAdapter implements OfflineTask.OnLoadPerson {
    private final AccountManager mAccountManager;
    private final Context mContext;

    private Date mLastUpdate;
    private SessionManager session;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        session = new SessionManager(context);

        mContext = context;
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        long timestamp = session.getSyncTimestamp();

        new OfflineTask().getSyncablePerson(this, timestamp);


        AppController.getInstance().firebaseFirestore.collection("persons")
                .whereGreaterThan("timestamp", timestamp)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                Person person = document.toObject(Person.class);

                                String[] arr = person.getId().split("_");
                                if (!arr[0].equals(Utils.getAndroidID(mContext.getContentResolver()))) {    // not created on my device, so should be synced
                                    if (Long.valueOf(arr[2]) > timestamp) {     // person created after sync, so must add to local room
                                        OfflineRepository.getInstance().createPerson(person);
                                    } else {    // created before sync, after sync person was updates, so must update in local room
                                        OfflineRepository.getInstance().updatePerson(person);
                                    }
                                }
                            }

                            session.setSyncTimestamp(Utils.getUniversalTimestamp());
                        }
                    }
                });
    }

    @Override
    public void onLoaded(List<Person> personList) {
        for (int i = 0; i < personList.size(); i++) {
            AppController.getInstance().firebaseFirestore.collection("persons")
                    .document(personList.get(i).getId())
                    .set(personList.get(i));

        }
    }
}
