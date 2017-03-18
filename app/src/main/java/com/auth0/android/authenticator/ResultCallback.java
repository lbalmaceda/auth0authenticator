package com.auth0.android.authenticator;

public interface ResultCallback<T> {
    void onResult(T result);

    void onError(Exception error);

    void onCanceled();
}
