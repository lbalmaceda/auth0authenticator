package com.auth0.android.authenticator;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.AuthenticationCallback;
import com.auth0.android.result.UserProfile;


/**
 * Class that stores the Auth0 Access Token and knows how to refresh them for use in the Auth0 APIs.
 */
public class Authenticator {

    private static final String TAG = Authenticator.class.getSimpleName();
    private static final String AUTH0_TOKEN_TYPE = "default";
    private static final String KEY_EXPIRATION_TIME = "expiration_time";

    private final Activity activity;
    private final AccountManager am;
    private final String accountType;

    /**
     * Creates a new instance of the Authenticator for the given Account Type.
     *
     * @param activity    a valid activity context.
     * @param accountType the Account Type that represents the shared credentials among this and other applications. Must be the same as the one defined in the 'res/xml/authenticator.xml' file.
     */
    public Authenticator(@NonNull Activity activity, @NonNull String accountType) {
        this.activity = activity;
        this.accountType = accountType;
        this.am = AccountManager.get(activity);
    }

    /**
     * Obtain a fresh Access Token ready to use against Auth0 APIs. An Account with the token's set must be present in the system.
     *
     * @param callback the callback that will get this call result.
     */
    public void getToken(final ResultCallback<String> callback) {
        pickAccount(am, new ResultCallback<Account>() {

            @Override
            public void onResult(Account account) {
                //Check for available accounts
                if (account == null) {
                    IllegalStateException err = new IllegalStateException("There are no accounts for this authenticator. Save a token first!");
                    callback.onError(err);
                    return;
                }

                //Invalidate token if expired
                final String authToken = am.peekAuthToken(account, AUTH0_TOKEN_TYPE);
                boolean isTokenExpired = isTokenExpired(am, account, authToken);
                if (isTokenExpired) {
                    Log.d(TAG, "getToken > token has expired.. calling invalidate");
                    am.invalidateAuthToken(accountType, authToken);
                }

                //Get the token
                am.getAuthToken(account, AUTH0_TOKEN_TYPE, null, activity, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        try {
                            final Bundle result = future.getResult();
                            Log.d(TAG, "getToken Bundle is " + result);
                            final String accessToken = result.getString(AccountManager.KEY_AUTHTOKEN);
                            callback.onResult(accessToken);
                        } catch (Exception e) {
                            e.printStackTrace();
                            callback.onError(e);
                        }
                    }
                }, null);
            }

            @Override
            public void onError(Exception error) {

            }

            @Override
            public void onCanceled() {
                callback.onCanceled();
            }
        });
    }

    /**
     * Saves the Auth0 Access Token in a new or existing Account on the system.
     *
     * @param accessToken  the access token to store.
     * @param refreshToken the refresh token to store. Will be used to get new tokens in the future.
     * @param expiresIn    the time at which the given access token will expire.
     * @param setCallback  the callback that will get this call result.
     */
    public void setTokens(final String accessToken, final String refreshToken, final long expiresIn, final ResultCallback<Boolean> setCallback) {
        pickAccount(am, new ResultCallback<Account>() {
            @Override
            public void onResult(Account account) {
                long tokenDurationInMillis = expiresIn * 1000;
                long expirationTime = System.currentTimeMillis() + tokenDurationInMillis;
                if (account == null) {
                    createAccount(am, accessToken, refreshToken, expirationTime, setCallback);
                    return;
                }
                am.setAuthToken(account, AUTH0_TOKEN_TYPE, accessToken);
                am.setPassword(account, refreshToken);
                String expString = String.valueOf(expirationTime);
                am.setUserData(account, KEY_EXPIRATION_TIME, expString);
                setCallback.onResult(true);
            }

            @Override
            public void onError(Exception error) {
                setCallback.onError(error);
            }

            @Override
            public void onCanceled() {
                setCallback.onCanceled();
            }
        });
    }

    /**
     * Removes the Account if exists or does nothing.
     *
     * @param removeCallback the callback that will get this call result.
     */
    @SuppressWarnings("deprecation")
    public void removeAccount(final ResultCallback<Boolean> removeCallback) {
        pickAccount(am, new ResultCallback<Account>() {
            @Override
            public void onResult(Account account) {
                if (account != null) {
                    am.removeAccount(account, new AccountManagerCallback<Boolean>() {
                        @Override
                        public void run(AccountManagerFuture<Boolean> future) {
                            try {
                                removeCallback.onResult(future.getResult());
                            } catch (Exception e) {
                                e.printStackTrace();
                                removeCallback.onError(e);
                            }
                        }
                    }, null);
                }
            }

            @Override
            public void onError(Exception error) {
                removeCallback.onError(error);
            }

            @Override
            public void onCanceled() {
                removeCallback.onCanceled();
            }
        });
    }

    /*
     *
     * INNER METHODS
     *
     */

    private void createAccount(final AccountManager accountManager, final String accessToken, final String refreshToken, final long expirationTime, final ResultCallback<Boolean> setCallback) {
        Auth0 auth0 = new Auth0(activity);
        auth0.setLoggingEnabled(true);
        auth0.setOIDCConformant(true);
        final AuthenticationAPIClient apiClient = new AuthenticationAPIClient(auth0);

        apiClient.userInfo(accessToken).start(new AuthenticationCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile user) {
                String name = user.getName();
                final Account account = new Account(name, accountType);
                Bundle userData = new Bundle();
                userData.putString(KEY_EXPIRATION_TIME, String.valueOf(expirationTime));
                accountManager.addAccountExplicitly(account, refreshToken, userData);
                accountManager.setAuthToken(account, AUTH0_TOKEN_TYPE, accessToken);
                setCallback.onResult(true);
            }

            @Override
            public void onFailure(AuthenticationException error) {
                error.printStackTrace();
                setCallback.onError(error);
            }
        });
    }

    static boolean isTokenExpired(AccountManager accountManager, Account account, String authToken) {
        final String expirationTime = accountManager.getUserData(account, KEY_EXPIRATION_TIME);
        return TextUtils.isEmpty(authToken) || TextUtils.isEmpty(expirationTime) || System.currentTimeMillis() > Long.parseLong(expirationTime);
    }

    private void pickAccount(final AccountManager accountManager, ResultCallback<Account> callback) {
        final Account[] accounts = accountManager.getAccountsByType(accountType);
        if (accounts.length == 0) {
            Log.d(TAG, "getAccount > no accounts found");
            callback.onResult(null);
        } else if (accounts.length == 1) {
            Log.d(TAG, "getAccount > one account found");
            callback.onResult(accounts[0]);
        }
    }

}
