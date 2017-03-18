package com.auth0.android.authenticator;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.AuthenticationCallback;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    public final static String KEY_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String KEY_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String KEY_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";
    public static final String KEY_EXPIRATION_TIME = "EXPIRATION_TIME";
    public static final String DEFAULT_TOKEN_TYPE = "AUTH0_ACCESS_TOKEN";

    private final int REQ_SIGNUP = 1;

    private final String TAG = AuthenticatorActivity.class.getSimpleName();

    private AccountManager accountManager;
    private AuthenticationAPIClient apiClient;

    private ProgressBar progressBar;
    private LinearLayout loginForm;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (true) {
            throw new IllegalStateException("THIS SHOULD NOT BE CALLED");
        }
        setContentView(R.layout.activity_demo);

        Auth0 account = new Auth0(this);
        account.setOIDCConformant(true);
        account.setLoggingEnabled(true);
        this.apiClient = new AuthenticationAPIClient(account);
        this.accountManager = AccountManager.get(this);

        this.progressBar = (ProgressBar) findViewById(R.id.login_progress);
        this.loginForm = (LinearLayout) findViewById(R.id.login_form);
//        final EditText emailField = (EditText) findViewById(R.id.email);
//        final EditText passwordField = (EditText) findViewById(R.id.password);

        String accountName = getIntent().getStringExtra(KEY_ACCOUNT_NAME);
        if (accountName != null) {
//            ((TextView) findViewById(R.id.account_name)).setText("Log in using: " + accountName);
        }

//        findViewById(R.id.submit_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                submit(emailField.getText().toString(), passwordField.getText().toString());
//            }
//        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    public void submit(final String email, final String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(AuthenticatorActivity.this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        Log.d(TAG, "> Started authenticating");
        apiClient.login(email, password, "Username-Password-Authentication")
                .setScope("openid offline_access")
                .start(new AuthenticationCallback<Credentials>() {
                    @Override
                    public void onSuccess(final Credentials credentials) {
                        final String accessToken = credentials.getAccessToken();
                        apiClient.userInfo(accessToken).start(new AuthenticationCallback<UserProfile>() {
                            @Override
                            public void onSuccess(UserProfile user) {
                                String name = user.getName();
                                deliverAuthenticationResult(name, credentials);
                            }

                            @Override
                            public void onFailure(AuthenticationException error) {
                                error.printStackTrace();
                                deliverAuthenticationResult(error.getDescription());
                            }
                        });
                    }

                    @Override
                    public void onFailure(AuthenticationException error) {
                        error.printStackTrace();
                        if (error.isInvalidCredentials() || "invalid_grant".equals(error.getCode())) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(AuthenticatorActivity.this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
                                    showProgress(false);
                                }
                            });
                            return;
                        }

                        deliverAuthenticationResult(error.getDescription());
                    }
                });
    }

    private void deliverAuthenticationResult(String errorMessage) {
        Intent result = new Intent();
        result.putExtra(KEY_ERROR_MESSAGE, errorMessage);
        setAccountAuthenticatorResult(result.getExtras());
        setResult(RESULT_OK, result);
        finish();
    }

    private void deliverAuthenticationResult(String accountName, Credentials credentials) {
        String accountType = getIntent().getStringExtra(KEY_ACCOUNT_TYPE);

        Bundle data = new Bundle();
        data.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
        data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        data.putString(AccountManager.KEY_AUTHTOKEN, credentials.getAccessToken());
        data.putString(AccountManager.KEY_PASSWORD, credentials.getRefreshToken());

        Log.d(TAG, "> finishLogin");
        Account account = new Account(accountName, accountType);
        if (getIntent().getBooleanExtra(KEY_IS_ADDING_NEW_ACCOUNT, false)) {
            Log.d(TAG, "> finishLogin > addAccountExplicitly");
            Bundle userData = new Bundle();
            long tokenDurationInMillis = credentials.getExpiresIn() * 1000;
            long expirationTime = System.currentTimeMillis() + tokenDurationInMillis;
            userData.putLong(KEY_EXPIRATION_TIME, expirationTime);
            accountManager.addAccountExplicitly(account, credentials.getRefreshToken(), userData);
            accountManager.setAuthToken(account, DEFAULT_TOKEN_TYPE, credentials.getAccessToken());
        } else {
            Log.d(TAG, "> finishLogin > setPassword");
            accountManager.setPassword(account, credentials.getRefreshToken());
        }

        Intent result = new Intent();
        result.putExtras(data);
        setAccountAuthenticatorResult(result.getExtras());
        setResult(RESULT_OK, result);
        finish();
    }

}
