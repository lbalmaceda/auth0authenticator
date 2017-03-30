package com.auth0.android.authenticator.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.authenticator.Authenticator;
import com.auth0.android.authenticator.ResultCallback;
import com.auth0.android.callback.AuthenticationCallback;
import com.auth0.android.result.Credentials;

public class DemoActivity extends Activity {

    private static final String ACCOUNT_TYPE = "com.auth0.account";
    private static final String TAG = DemoActivity.class.getSimpleName();

    private AuthenticationAPIClient apiClient;
    private Authenticator authenticator;
    private ProgressBar progressBar;
    private LinearLayout buttonForm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        final Auth0 auth0 = new Auth0(this);
        auth0.setOIDCConformant(true);
        auth0.setLoggingEnabled(true);
        apiClient = new AuthenticationAPIClient(auth0);
        authenticator = new Authenticator(this, ACCOUNT_TYPE);

        progressBar = (ProgressBar) findViewById(R.id.login_progress);
        buttonForm = (LinearLayout) findViewById(R.id.login_form);
        final Button logInButton = (Button) findViewById(R.id.logInButton);
        final Button getTokenButton = (Button) findViewById(R.id.getTokenButton);
        final Button removeAccountButton = (Button) findViewById(R.id.removeAccountButton);

        logInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginAndSetToken();
            }
        });
        getTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getToken();
            }
        });
        removeAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                authenticator.removeAccount(new ResultCallback<Boolean>() {
                    @Override
                    public void onResult(Boolean result) {
                        if (result) {
                            showToast("Account removed!");
                        } else {
                            showToast("Failed to remove account!");
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        showToast("Failed to remove account: " + error.getMessage());
                    }
                });
            }
        });
    }

    private void getToken() {
        showProgress(true);
        authenticator.getToken(new ResultCallback<String>() {
            @Override
            public void onResult(String accessToken) {
                String part = accessToken.substring(accessToken.length() - 10, accessToken.length());
                showToast("Token obtained! " + part);
            }

            @Override
            public void onError(Exception error) {
                showToast("Error getting token: " + error.getMessage());
            }
        });
    }

    private void loginAndSetToken() {
        showProgress(true);
        apiClient.login("asdasd", "asdasd", "Username-Password-Authentication")
                .setScope("openid offline_access")
                .start(new AuthenticationCallback<Credentials>() {
                    @Override
                    public void onSuccess(Credentials payload) {
                        authenticator.setTokens(
                                payload.getAccessToken(),
                                payload.getRefreshToken(),
                                payload.getExpiresIn(),
                                new ResultCallback<Boolean>() {
                                    @Override
                                    public void onResult(Boolean result) {
                                        showToast("Token set");
                                    }

                                    @Override
                                    public void onError(Exception error) {
                                        showToast("Failed to set token: " + error.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void onFailure(AuthenticationException error) {
                        showToast("Failed to set token: " + error.getDescription());
                    }
                });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonForm.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showToast(final String message) {
        Log.e(TAG, message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DemoActivity.this, message, Toast.LENGTH_SHORT).show();
                showProgress(false);
            }
        });
    }

}
