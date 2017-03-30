# Android Authenticator for Auth0

Provides a simple API to store/get fresh `access_token`'s to make requests to Auth0 APIs.

>Disclaimer: This is a PoC

## Configuration

Create a new file in `res/xml/authenticator.xml` to store the Authenticator configuration with the following contents:

```xml
<?xml version="1.0" encoding="utf-8"?>
<account-authenticator
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accountType="com.project.accounts"
    android:icon="@drawable/ic_account"
    android:label="@string/account_name"
    android:smallIcon="@drawable/ic_account_small" />
```


Some configurable attributes are:

* **android:accountType:** The account type it's the unique identifier for our account among the OS. If we want to access to it from different apps we will need to use the same type in all of them.
* **android:icon:** The big icon used in the account details screen in the device Settings.
* **android:smallIcon:** The small icon used in the account list screen in the device Settings.
* **android:label:** The text to display as name for the account.


Now reference this file from the app's `AndroidManifest.xml` file in order to configure the Authenticator Service. Make sure to add the service declaration inside the application tag:

```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/AppTheme">

    <service android:name="com.auth0.android.authenticator.Auth0AuthenticatorService">
       <intent-filter>
           <action android:name="android.accounts.AccountAuthenticator" />
       </intent-filter>

       <meta-data
           android:name="android.accounts.AccountAuthenticator"
           android:resource="@xml/authenticator" />
    </service>

</application>
```

Then we need to add the following permissions to our app's `AndroidManifest.xml`.

```xml
<uses-permission
    android:name="android.permission.AUTHENTICATE_ACCOUNTS"
    android:maxSdkVersion="22" />
<uses-permission android:name="android.permission.GET_ACCOUNTS" />
<uses-permission
    android:name="android.permission.MANAGE_ACCOUNTS"
    android:maxSdkVersion="22" />
<uses-permission
    android:name="android.permission.USE_CREDENTIALS"
    android:maxSdkVersion="22" />
<uses-permission
    android:name="android.permission.USE_CREDENTIALS"
    android:maxSdkVersion="22" />
```


* `android.permission.AUTHENTICATE_ACCOUNTS`: To read the stored `access_token` and `refresh_token`.
* `android.permission.GET_ACCOUNTS`: To list the available accounts in the OS.
* `android.permission.MANAGE_ACCOUNTS`: To add and remove accounts.
* `android.permission.USE_CREDENTIALS`: To get a fresh `access_token`. Either an existing one if it hasn't expired yet, or a new one using the `refresh_token`.

> Keep in mind if you target API 23 and up, you'll need to request and handle the Android Runtime Permissions yourself. With the new system, all of them are grouped as the "Contacts" permission.


## Usage

Create a new instance of the Authenticator by passing a valid `Activity` context and **the same Account Type** used before in the `authenticator.xml` file. If the types don't match it won't work!

```java
Authenticator authenticator = new Authenticator(MainActivity.this, "com.project.accounts");
```

Next, log in using the **Auth0 Authentication API**. e.g. using the [auth0.android](https://github.com/auth0/auth0.android) library. Request at least the `openid offline_access` scopes, in order to receive valid `access_token`, `refresh_token` and `expires_in` values.

Now store the values in the Authenticator. If an account exists, the values will be replaced. If an account doesn't exists it will be created.

```java
String accessToken = "some.access.token";
String refreshToken = "some.refresh.token";
long expiresIn = 36000;
//Above are the values received in the login call
authenticator.setTokens(accessToken, refreshToken, expiresIn, new ResultCallback<Boolean>() {
  @Override
  public void onResult(Boolean result) {
      //Saved successfully
  }

  @Override
  public void onError(Exception error) {
      //Failed to save
  }
});
```

When you need a fresh `access_token` to call the APIs, you ask for it to the Authenticator

```java
authenticator.getToken(new ResultCallback<String>(){
  @Override
  public void onResult(String accessToken) {
      //Token obtained!
  }

  @Override
  public void onError(Exception error) {
      //Error getting token
  }
});
```

This library also provides a method to remove the existing account.

```java
authenticator.removeAccount(new ResultCallback<Boolean>(){
  @Override
  public void onResult(Boolean result) {
      //Check result value to see if it was actually removed or if the account didn't existed.
  }

  @Override
  public void onError(Exception error) {
      //Failed to remove the account
  }
});
```
