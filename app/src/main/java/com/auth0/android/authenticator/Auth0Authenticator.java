package com.auth0.android.authenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.auth0.android.Auth0;
import com.auth0.android.Auth0Exception;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.result.Credentials;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;
import static android.accounts.AccountManager.KEY_ERROR_MESSAGE;

public class Auth0Authenticator extends AbstractAccountAuthenticator {

    private String TAG = Auth0Authenticator.class.getSimpleName();

    private final AuthenticationAPIClient apiClient;
    private final Context context;

    public Auth0Authenticator(Context context) {
        super(context);
        this.context = context;
        Auth0 account = new Auth0(context);
        account.setOIDCConformant(true);
        account.setLoggingEnabled(true);
        this.apiClient = new AuthenticationAPIClient(account);
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "addAccount");

        final Intent intent = new Intent(context, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(AuthenticatorActivity.KEY_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d(TAG, "getAuthToken");

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(context);

        String authToken = am.peekAuthToken(account, authTokenType);
        if (!Authenticator.isTokenExpired(am, account, authToken)) {
            return createAuthBundle(account, authToken);
        }

        //Can we refresh it?
        String refreshToken = am.getPassword(account);
        Log.d(TAG, "getPassword returned - " + refreshToken);
        if (!TextUtils.isEmpty(refreshToken)) {
            try {
                Log.d(TAG, "re-authenticating with the existing refresh token");
                final Credentials credentials = apiClient.renewAuth(refreshToken).execute();
                am.setAuthToken(account, authTokenType, credentials.getAccessToken());
                return createAuthBundle(account, credentials.getAccessToken());
            } catch (Auth0Exception exception) {
                exception.printStackTrace();
            }
        }

        // No valid tokens found. Launch AuthenticatorActivity.
        Bundle missingTokenBundle = new Bundle();
        missingTokenBundle.putString(KEY_ERROR_MESSAGE, "No token found. Invalidate and restart.");
        return missingTokenBundle;
    }

    private Bundle createAuthBundle(Account account, String authToken) {
        final Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
        return result;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return "Auth0 Access Token";
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }
}
