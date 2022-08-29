package org.gtp.cocacolaproject.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.gtp.cocacolaproject.data.LoginData;

public final class SharedPreferencesUtils {
    /**
     * Logging tag.
     */
    @SuppressWarnings("unused")
    private static final String TAG = SharedPreferencesUtils.class.getSimpleName();

    /**
     * Ensure this class is only used as a utility.
     */
    private SharedPreferencesUtils() {
        throw new AssertionError();
    }

    private final static String LOGIN_DATA_SHARED_PREFERENCES_NAME = "login_data_shared_preferences_name";
    private final static String LOGIN_DATA_KEY_USERNAME = "login_data_key_username";
    private final static String LOGIN_DATA_KEY_PASSWORD = "login_data_key_password";

    public static LoginData getLoginData(Context context) {
        SharedPreferences settings = context.getSharedPreferences(LOGIN_DATA_SHARED_PREFERENCES_NAME, 0);

        return new LoginData(
                settings.getString(LOGIN_DATA_KEY_USERNAME, null),
                settings.getString(LOGIN_DATA_KEY_PASSWORD, null));
    }

    public static void putLoginData(Context context, LoginData loginData) {

        SharedPreferences settings = context.getSharedPreferences(LOGIN_DATA_SHARED_PREFERENCES_NAME, 0);

        settings.edit()
                .putString(LOGIN_DATA_KEY_USERNAME, loginData.getUsername())
                .putString(LOGIN_DATA_KEY_PASSWORD, loginData.getPassword())
                .apply();
    }

    public static void resetLoginData(Context context) {
        context.getSharedPreferences(LOGIN_DATA_SHARED_PREFERENCES_NAME, 0)
                .edit()
                .clear()
                .apply();
    }
}
