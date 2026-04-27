package com.example.habtrack.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.*;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "habits.db";
    private static final int DATABASE_VERSION = 2;

    // Таблица пользователей
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "user_id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";

    // Таблица привычек
    private static final String TABLE_HABITS = "habits";
    private static final String COL_HABIT_ID = "habit_id";
    private static final String COL_HABIT_TITLE = "title";
    private static final String COL_HABIT_CATEGORY = "category";
    private static final String COL_USER_REF = "user_id";
    private static final String COL_CREATED_AT = "created_at";

    // Таблица выполнений
    private static final String TABLE_COMPLETIONS = "completions";
    private static final String COL_COMPLETION_ID = "completion_id";
    private static final String COL_HABIT_REF = "habit_id";
    private static final String COL_COMPLETION_DATE = "completion_date";

    // Таблица логов
    private static final String TABLE_LOGS = "logs";
    private static final String COL_LOG_ID = "log_id";
    private static final String COL_LOG_ACTION = "log_action";
    private static final String COL_LOG_TIMESTAMP = "log_timestamp";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Таблица пользователей
        String createUsers = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                COL_PASSWORD + " TEXT NOT NULL)";
        db.execSQL(createUsers);

        // Таблица привычек
        String createHabits = "CREATE TABLE " + TABLE_HABITS + " (" +
                COL_HABIT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_HABIT_TITLE + " TEXT NOT NULL, " +
                COL_HABIT_CATEGORY + " TEXT, " +
                COL_USER_REF + " INTEGER, " +
                COL_CREATED_AT + " INTEGER, " +
                "FOREIGN KEY(" + COL_USER_REF + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))";
        db.execSQL(createHabits);

        // Таблица выполнений
        String createCompletions = "CREATE TABLE " + TABLE_COMPLETIONS + " (" +
                COL_COMPLETION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_HABIT_REF + " INTEGER, " +
                COL_COMPLETION_DATE + " TEXT NOT NULL, " +
                "FOREIGN KEY(" + COL_HABIT_REF + ") REFERENCES " + TABLE_HABITS + "(" + COL_HABIT_ID + "))";
        db.execSQL(createCompletions);

        // Таблица логов
        String createLogs = "CREATE TABLE " + TABLE_LOGS + " (" +
                COL_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LOG_ACTION + " TEXT NOT NULL, " +
                COL_LOG_TIMESTAMP + " INTEGER NOT NULL)";
        db.execSQL(createLogs);

        // ========== ГЛАВНОЕ: Таблица категорий ==========
        String createCategories = "CREATE TABLE categories (" +
                "category_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "user_id INTEGER, " +
                "FOREIGN KEY(user_id) REFERENCES users(user_id))";
        db.execSQL(createCategories);
        // ================================================

        // Добавляем тестового пользователя
        addTestUser(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMPLETIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HABITS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    private void addTestUser(SQLiteDatabase db) {
        // Проверяем, есть ли уже пользователь
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_USERNAME + " = ?", new String[]{"user"}, null, null, null);

        if (cursor.getCount() == 0) {
            ContentValues values = new ContentValues();
            values.put(COL_USERNAME, "user");
            values.put(COL_PASSWORD, "123");
            db.insert(TABLE_USERS, null, values);

            // Логируем
            ContentValues logValues = new ContentValues();
            logValues.put(COL_LOG_ACTION, "Тестовый пользователь создан: user/123");
            logValues.put(COL_LOG_TIMESTAMP, System.currentTimeMillis());
            db.insert(TABLE_LOGS, null, logValues);
        }
        cursor.close();

        // Проверяем, есть ли тестовые привычки
        cursor = db.query(TABLE_HABITS, new String[]{COL_HABIT_ID}, null, null, null, null, null);
        if (cursor.getCount() == 0) {
            ContentValues habit1 = new ContentValues();
            habit1.put(COL_HABIT_TITLE, "Прочитать книгу");
            habit1.put(COL_HABIT_CATEGORY, "Развитие");
            habit1.put(COL_USER_REF, 1);
            habit1.put(COL_CREATED_AT, System.currentTimeMillis());
            db.insert(TABLE_HABITS, null, habit1);

            ContentValues habit2 = new ContentValues();
            habit2.put(COL_HABIT_TITLE, "Сделать зарядку");
            habit2.put(COL_HABIT_CATEGORY, "Здоровье");
            habit2.put(COL_USER_REF, 1);
            habit2.put(COL_CREATED_AT, System.currentTimeMillis());
            db.insert(TABLE_HABITS, null, habit2);
        }
        cursor.close();
    }

    // ============= ЛОГИРОВАНИЕ =============
    public void addLog(String action) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LOG_ACTION, action);
        values.put(COL_LOG_TIMESTAMP, System.currentTimeMillis());
        db.insert(TABLE_LOGS, null, values);
        Log.d(TAG, action);
    }

    public List<String> getLogs() {
        List<String> logs = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_LOGS,
                new String[]{COL_LOG_ACTION, COL_LOG_TIMESTAMP},
                null, null, null, null, COL_LOG_TIMESTAMP + " DESC", "50");

        while (cursor.moveToNext()) {
            String action = cursor.getString(0);
            long time = cursor.getLong(1);
            String date = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(time));
            logs.add(date + " - " + action);
        }
        cursor.close();
        return logs;
    }

    // ============= АВТОРИЗАЦИЯ =============
    public boolean login(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?",
                new String[]{username, password},
                null, null, null);

        boolean success = cursor.getCount() > 0;
        cursor.close();

        if (success) {
            addLog("Пользователь " + username + " вошёл в систему");
        } else {
            addLog("Неудачная попытка входа: " + username);
        }
        return success;
    }

    public boolean register(String username, String password) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COL_USERNAME, username);
            values.put(COL_PASSWORD, password);
            long result = db.insert(TABLE_USERS, null, values);
            if (result != -1) {
                addLog("Новый пользователь зарегистрирован: " + username);
                return true;
            }
        } catch (Exception e) {
            addLog("Ошибка регистрации: " + e.getMessage());
        }
        return false;
    }

    public int getCurrentUserId() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID}, null, null, null, null, null, "1");
        int id = 1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        return id;
    }

    // ============= ПРИВЫЧКИ =============
    public List<Habit> getHabits() {
        List<Habit> habits = new ArrayList<>();
        int userId = getCurrentUserId();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(TABLE_HABITS,
                new String[]{COL_HABIT_ID, COL_HABIT_TITLE, COL_HABIT_CATEGORY},
                COL_USER_REF + " = ?",
                new String[]{String.valueOf(userId)},
                null, null, COL_HABIT_ID + " DESC");

        while (cursor.moveToNext()) {
            Habit habit = new Habit(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2)
            );
            habits.add(habit);
        }
        cursor.close();
        return habits;
    }

    public long addHabit(String title, String category) {
        SQLiteDatabase db = getWritableDatabase();
        int userId = getCurrentUserId();
        ContentValues values = new ContentValues();
        values.put(COL_HABIT_TITLE, title);
        values.put(COL_HABIT_CATEGORY, category != null ? category : "Без категории");
        values.put(COL_USER_REF, userId);
        values.put(COL_CREATED_AT, System.currentTimeMillis());

        long id = db.insert(TABLE_HABITS, null, values);
        addLog("Добавлена привычка: " + title);
        return id;
    }

    public void deleteHabit(int habitId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_HABITS, COL_HABIT_ID + " = ?", new String[]{String.valueOf(habitId)});
        addLog("Удалена привычка ID: " + habitId);
    }

    // ============= ВЫПОЛНЕНИЯ =============
    public boolean isCompleted(int habitId, String date) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_COMPLETIONS,
                new String[]{COL_COMPLETION_ID},
                COL_HABIT_REF + " = ? AND " + COL_COMPLETION_DATE + " = ?",
                new String[]{String.valueOf(habitId), date},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public void toggleCompletion(int habitId, String date) {
        if (isCompleted(habitId, date)) {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(TABLE_COMPLETIONS,
                    COL_HABIT_REF + " = ? AND " + COL_COMPLETION_DATE + " = ?",
                    new String[]{String.valueOf(habitId), date});
            addLog("Снята отметка привычки ID: " + habitId);
        } else {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_HABIT_REF, habitId);
            values.put(COL_COMPLETION_DATE, date);
            db.insert(TABLE_COMPLETIONS, null, values);
            addLog("Отмечена привычка ID: " + habitId);
        }
    }

    public List<Integer> getCompletionsForDate(String date) {
        List<Integer> completedIds = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_COMPLETIONS,
                new String[]{COL_HABIT_REF},
                COL_COMPLETION_DATE + " = ?",
                new String[]{date},
                null, null, null);

        while (cursor.moveToNext()) {
            completedIds.add(cursor.getInt(0));
        }
        cursor.close();
        return completedIds;
    }

    // Модель привычки
    public static class Habit {
        private int id;
        private String title;
        private String category;

        public Habit(int id, String title, String category) {
            this.id = id;
            this.title = title;
            this.category = category;
        }

        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getCategory() { return category; }
    }

    // Получить категории пользователя
    public List<String> getUserCategories() {
        List<String> categoryList = new ArrayList<>();
        categoryList.add("Без категории");

        int userId = getCurrentUserId();
        SQLiteDatabase db = getReadableDatabase();

        // Проверяем, существует ли таблица categories
        try {
            Cursor cursor = db.query("categories",
                    new String[]{"name"},
                    "user_id = ?",
                    new String[]{String.valueOf(userId)},
                    null, null, null);

            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                if (!categoryList.contains(name)) {
                    categoryList.add(name);
                }
            }
            cursor.close();
        } catch (Exception e) {
            // Таблицы ещё нет — используем дефолтные
            e.printStackTrace();
        }

        // Если нет своих категорий — добавляем дефолтные
        if (categoryList.size() == 1) {
            addDefaultCategories(userId);
            return getUserCategories(); // рекурсивно вызываем после добавления
        }

        return categoryList;
    }

    private void addDefaultCategories(int userId) {
        String[] defaults = {"Здоровье", "Спорт", "Развитие", "Работа", "Творчество", "Дом"};
        SQLiteDatabase db = getWritableDatabase();
        for (String cat : defaults) {
            ContentValues values = new ContentValues();
            values.put("name", cat);
            values.put("user_id", userId);
            db.insert("categories", null, values);
        }
    }

    public void addCategory(String categoryName) {
        int userId = getCurrentUserId();
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", categoryName);
        values.put("user_id", userId);
        db.insert("categories", null, values);
        addLog("Добавлена категория: " + categoryName);
    }
}