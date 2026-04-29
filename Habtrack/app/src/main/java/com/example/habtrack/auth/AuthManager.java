package com.example.habtrack.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthManager {
    private static final String PREF_NAME = "habtrack_prefs";
    private static final String KEY_USER_ID = "current_user_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final SharedPreferences prefs;

    public AuthManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Сохранение данных пользователя
    public void saveUser(int userId) {
        prefs.edit()
                .putInt(KEY_USER_ID, userId)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }

    // Получение ID текущего пользователя
    public int getCurrentUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    // Проверка авторизации
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Выход из аккаунта
    public void logout() {
        prefs.edit()
                .clear()
                .apply();
    }
}