package com.auth0.android.authenticator;

/**
 * Generic callback class
 *
 * @param <T> the type of Result when the call is successful.
 */
public interface ResultCallback<T> {
    /**
     * Called with the result when the request was successful.
     *
     * @param result the result obtained on the request.
     */
    void onResult(T result);

    /**
     * Called when an error is thrown in the request execution
     *
     * @param error the exception.
     */
    void onError(Exception error);
}
