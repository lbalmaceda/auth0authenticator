package com.auth0.android.authenticator;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.AuthenticationCallback;
import com.auth0.android.result.UserProfile;

import java.util.ArrayList;
import java.util.List;

import static com.auth0.android.authenticator.AuthenticatorActivity.KEY_EXPIRATION_TIME;

public class Authenticator {

    private static final String TAG = Authenticator.class.getSimpleName();
    private static final String AUTH0_ACCOUNT_TYPE = "com.auth0.account";
    private static final String AUTH0_TOKEN_TYPE = "default";

    private final Activity activity;
    private final AccountManager am;

    public Authenticator(Activity activity) {
        this.activity = activity;
        this.am = AccountManager.get(activity);
    }

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
                if (!TextUtils.isEmpty(authToken) && isTokenExpired) {
                    Log.d(TAG, "getToken > token has expired.. calling invalidate");
                    am.invalidateAuthToken(AUTH0_ACCOUNT_TYPE, authToken);
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
                final Account account = new Account(name, AUTH0_ACCOUNT_TYPE);
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
        final Account[] accounts = accountManager.getAccountsByType(AUTH0_ACCOUNT_TYPE);
        if (accounts.length == 0) {
            Log.d(TAG, "getAccount > no accounts found");
            callback.onResult(null);
        } else if (accounts.length == 1) {
            Log.d(TAG, "getAccount > one account found");
            callback.onResult(accounts[0]);
        } else {
            Log.d(TAG, "getAccount > multiple accounts found");
            showAccountChooser(accounts, callback);
        }
    }

    private void showAccountChooser(final Account[] accounts, final ResultCallback<Account> callback) {
        final List<String> accountNames = new ArrayList<>();
        for (Account account : accounts) {
            accountNames.add(account.name);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Choose your account");

        final ListView lv = new ListView(activity);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, android.R.id.text1, accountNames);
        lv.setAdapter(adapter);

        builder.setView(lv);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final int selectedPos = lv.getSelectedItemPosition();
                if (selectedPos != AdapterView.INVALID_POSITION) {
                    callback.onResult(accounts[selectedPos]);
                    dialog.dismiss();
                }
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                callback.onCanceled();
            }
        });
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Dialog dialog = builder.create();
                dialog.show();
            }
        });

    }


}
