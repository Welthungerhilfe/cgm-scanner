package de.welthungerhilfe.cgm.scanner.syncdata;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

import java.io.IOException;
import java.util.Date;

import de.welthungerhilfe.cgm.scanner.helper.AppConstants;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private final AccountManager mAccountManager;
    private final Context mContext;

    private Date mLastUpdate;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        mContext = context;
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

    }
}
