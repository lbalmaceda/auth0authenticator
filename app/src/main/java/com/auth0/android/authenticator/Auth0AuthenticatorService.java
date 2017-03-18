package com.auth0.android.authenticator;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class Auth0AuthenticatorService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        Auth0Authenticator authenticator = new Auth0Authenticator(this);
        return authenticator.getIBinder();
    }
}
