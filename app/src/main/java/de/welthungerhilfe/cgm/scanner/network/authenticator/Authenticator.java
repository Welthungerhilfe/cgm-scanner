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
package de.welthungerhilfe.cgm.scanner.network.authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.ui.activities.LoginActivity;
import de.welthungerhilfe.cgm.scanner.AppConstants;

public class Authenticator extends AbstractAccountAuthenticator {
    private final Context mContext;

    public Authenticator(Context context) {
        super(context);

        mContext = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) {
        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AppConstants.PARAM_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
        if (options != null && options.containsKey(AccountManager.KEY_PASSWORD)) {
            final String password = options.getString(AccountManager.KEY_PASSWORD);
            final boolean verified = true;
            final Bundle result = new Bundle();
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, verified);
            return result;
        }

        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AppConstants.PARAM_AUTH_NAME, account.name);
        intent.putExtra(AppConstants.PARAM_AUTH_CONFIRM, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
        if (!authTokenType.equals(AppConstants.AUTHTOKEN_TYPE)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");

            return result;
        }
        final AccountManager am = AccountManager.get(mContext);
        final String password = am.getPassword(account);
        if (password != null) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, AppConstants.ACCOUNT_TYPE);
            result.putString(AccountManager.KEY_AUTHTOKEN, password);

            return result;
        }

        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AppConstants.PARAM_AUTH_NAME, account.name);
        intent.putExtra(AppConstants.PARAM_AUTHTOKEN_TYPE, authTokenType);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if (authTokenType.equals(AppConstants.AUTHTOKEN_TYPE)) {
            return mContext.getString(R.string.app_name_long);
        }
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AppConstants.PARAM_AUTH_NAME, account.name);
        intent.putExtra(AppConstants.AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AppConstants.PARAM_AUTH_CONFIRM, false);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }
}
