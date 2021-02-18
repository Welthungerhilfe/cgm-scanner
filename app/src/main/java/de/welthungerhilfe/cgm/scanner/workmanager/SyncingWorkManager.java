package de.welthungerhilfe.cgm.scanner.workmanager;

import android.accounts.Account;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


import de.welthungerhilfe.cgm.scanner.BuildConfig;
import de.welthungerhilfe.cgm.scanner.network.authenticator.AccountUtils;
import de.welthungerhilfe.cgm.scanner.network.syncdata.SyncAdapter;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;

public class SyncingWorkManager extends Worker {

    Context context;
    SessionManager sessionManager;

    public SyncingWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        sessionManager = new SessionManager(context.getApplicationContext());
        Account accountData = null;
        if (BuildConfig.DEBUG) {
            accountData = AccountUtils.getAccount(context.getApplicationContext(), "test@test.com", "kjjhhj");
        } else {
            accountData = AccountUtils.getAccount(context.getApplicationContext(), sessionManager);
        }
        SyncAdapter.startPeriodicSync(accountData, getApplicationContext());
        return null;
    }
}
