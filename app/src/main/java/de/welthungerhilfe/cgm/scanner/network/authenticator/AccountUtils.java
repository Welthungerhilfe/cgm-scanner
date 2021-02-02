package de.welthungerhilfe.cgm.scanner.network.authenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.SessionManager;

public class AccountUtils {

    public static Account getAccount(Context context, String email, String token) {
        AccountManager accountManager = AccountManager.get(context);
        Account accountData = new Account(email, AppConstants.ACCOUNT_TYPE);
        accountManager.addAccountExplicitly(accountData, token, null);
        return accountData;
    }

    public static Account getAccount(Context context, SessionManager session) {
        AccountManager accountManager = AccountManager.get(context);
        for (Account account : accountManager.getAccounts()) {
            if (account.name.compareTo(session.getUserEmail()) == 0) {
                return account;
            }
        }
        return getAccount(context, session.getUserEmail(), session.getAuthToken());
    }

    public static void removeAccount(Activity context) {
        AccountManager accountManager = AccountManager.get(context);
        try {
            for (Account account : accountManager.getAccounts()) {
                accountManager.removeAccount(account, context, null, null);
            }
        } catch (Exception e) {
            //no rights to remove the account from system settings
            e.printStackTrace();
        }
    }
}
