/*
 *  Child Growth Monitor - quick and accurate data on malnutrition
 *  Copyright (c) $today.year Welthungerhilfe Innovation
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.network.authenticator;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;

import de.welthungerhilfe.cgm.scanner.AppConstants;
import de.welthungerhilfe.cgm.scanner.hardware.io.SessionManager;

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
