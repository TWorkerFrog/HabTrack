package com.example.habtrack.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "habits.db";
    private static final int DATABASE_VERSION = 8;

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
    private static final String COL_HIDDEN_FROM = "hidden_from";
    private static final String COL_CREATED_DATE = "created_date";
    private static final String COL_ORDER_POSITION = "order_position";

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

    // Таблица категорий
    private static final String TABLE_CATEGORIES = "categories";
    private static final String COL_CATEGORY_ID = "category_id";
    private static final String COL_CATEGORY_NAME = "name";

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

        // Таблица категорий
        String createCategories = "CREATE TABLE " + TABLE_CATEGORIES + " (" +
                COL_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CATEGORY_NAME + " TEXT NOT NULL, " +
                COL_USER_REF + " INTEGER, " +
                "FOREIGN KEY(" + COL_USER_REF + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))";
        db.execSQL(createCategories);

        // Таблица привычек
        String createHabits = "CREATE TABLE " + TABLE_HABITS + " (" +
                COL_HABIT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_HABIT_TITLE + " TEXT NOT NULL, " +
                COL_HABIT_CATEGORY + " TEXT, " +
                COL_USER_REF + " INTEGER, " +
                COL_CREATED_AT + " INTEGER, " +
                COL_CREATED_DATE + " TEXT DEFAULT '', " +
                COL_HIDDEN_FROM + " TEXT DEFAULT '', " +
                COL_ORDER_POSITION + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY(" + COL_USER_REF + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))";
        db.execSQL(createHabits);

        // Таблица выполнений
        String createCompletions = "CREATE TABLE " + TABLE_COMPLETIONS + " (" +
                COL_COMPLETION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_HABIT_REF + " INTEGER, " +
                COL_COMPLETION_DATE + " TEXT NOT NULL, " +
                "UNIQUE(" + COL_HABIT_REF + ", " + COL_COMPLETION_DATE + "), " +
                "FOREIGN KEY(" + COL_HABIT_REF + ") REFERENCES " + TABLE_HABITS + "(" + COL_HABIT_ID + "))";
        db.execSQL(createCompletions);

        // Таблица логов
        String createLogs = "CREATE TABLE " + TABLE_LOGS + " (" +
                COL_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_LOG_ACTION + " TEXT NOT NULL, " +
                COL_LOG_TIMESTAMP + " INTEGER NOT NULL)";
        db.execSQL(createLogs);

        insertTestData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMPLETIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HABITS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    private void insertTestData(SQLiteDatabase db) {
        String testDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        ContentValues userValues = new ContentValues();
        userValues.put(COL_USERNAME, "user");
        userValues.put(COL_PASSWORD, "123");
        long userId = db.insert(TABLE_USERS, null, userValues);

        if (userId == -1) return;

        String[] defaultCategories = {"Здоровье", "Спорт", "Развитие", "Работа", "Творчество", "Дом"};
        for (String category : defaultCategories) {
            ContentValues catValues = new ContentValues();
            catValues.put(COL_CATEGORY_NAME, category);
            catValues.put(COL_USER_REF, userId);
            db.insert(TABLE_CATEGORIES, null, catValues);
        }

        ContentValues habit1 = new ContentValues();
        habit1.put(COL_HABIT_TITLE, "Прочитать книгу");
        habit1.put(COL_HABIT_CATEGORY, "Развитие");
        habit1.put(COL_USER_REF, userId);
        habit1.put(COL_CREATED_AT, System.currentTimeMillis());
        habit1.put(COL_CREATED_DATE, testDate);
        habit1.put(COL_HIDDEN_FROM, "");
        habit1.put(COL_ORDER_POSITION, 0);
        db.insert(TABLE_HABITS, null, habit1);

        ContentValues habit2 = new ContentValues();
        habit2.put(COL_HABIT_TITLE, "Сделать зарядку");
        habit2.put(COL_HABIT_CATEGORY, "Здоровье");
        habit2.put(COL_USER_REF, userId);
        habit2.put(COL_CREATED_AT, System.currentTimeMillis());
        habit2.put(COL_CREATED_DATE, testDate);
        habit2.put(COL_HIDDEN_FROM, "");
        habit2.put(COL_ORDER_POSITION, 1);
        db.insert(TABLE_HABITS, null, habit2);

        ContentValues logValues = new ContentValues();
        logValues.put(COL_LOG_ACTION, "База данных создана с тестовыми данными");
        logValues.put(COL_LOG_TIMESTAMP, System.currentTimeMillis());
        db.insert(TABLE_LOGS, null, logValues);
    }

    //  Логирование 

    public void addLog(String action) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_LOG_ACTION, action);
            values.put(COL_LOG_TIMESTAMP, System.currentTimeMillis());
            db.insert(TABLE_LOGS, null, values);
        } catch (Exception e) {
            Log.e(TAG, "addLog error", e);
        }
    }

    //  Авторизация 

    public boolean login(String username, String password) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_USERS,
                    new String[]{COL_USER_ID},
                    COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?",
                    new String[]{username, password},
                    null, null, null);
            boolean success = cursor.getCount() > 0;
            cursor.close();
            if (success) addLog("Пользователь " + username + " вошёл в систему");
            else addLog("Неудачная попытка входа: " + username);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "login error", e);
            return false;
        }
    }

    public boolean register(String username, String password) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_USERNAME, username);
            values.put(COL_PASSWORD, password);
            long result = db.insert(TABLE_USERS, null, values);
            if (result != -1) {
                addLog("Новый пользователь зарегистрирован: " + username);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "register error", e);
            addLog("Ошибка регистрации: " + e.getMessage());
        }
        return false;
    }

    public int getCurrentUserId() {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                    null, null, null, null, null, "1");
            int id = -1;
            if (cursor.moveToFirst()) id = cursor.getInt(0);
            cursor.close();
            return id;
        } catch (Exception e) {
            Log.e(TAG, "getCurrentUserId error", e);
            return -1;
        }
    }

    public int getUserIdByUsername(String username) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_USERS,
                    new String[]{COL_USER_ID},
                    COL_USERNAME + " = ?",
                    new String[]{username},
                    null, null, null);
            int id = -1;
            if (cursor.moveToFirst()) id = cursor.getInt(0);
            cursor.close();
            return id;
        } catch (Exception e) {
            Log.e(TAG, "getUserIdByUsername error", e);
            return -1;
        }
    }

    //  Привычки 

    public List<Habit> getHabitsForDate(String date) {
        List<Habit> habits = new ArrayList<>();
        try {
            int userId = getCurrentUserId();
            SQLiteDatabase db = getReadableDatabase();

            String sql = "SELECT " + COL_HABIT_ID + ", " + COL_HABIT_TITLE + ", " +
                    COL_HABIT_CATEGORY + ", " + COL_CREATED_AT +
                    " FROM " + TABLE_HABITS +
                    " WHERE " + COL_USER_REF + " = ?" +
                    " AND (" + COL_HIDDEN_FROM + " = '' OR " + COL_HIDDEN_FROM + " > ?)" +
                    " AND " + COL_CREATED_DATE + " <= ?" +
                    " ORDER BY " + COL_ORDER_POSITION + " ASC, " + COL_HABIT_ID + " ASC";

            Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), date, date});

            while (cursor.moveToNext()) {
                habits.add(new Habit(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3),
                        ""
                ));
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "getHabitsForDate error", e);
        }
        return habits;
    }

    public long addHabit(String title, String category) {
        SQLiteDatabase db = getWritableDatabase();
        int userId = getCurrentUserId();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        int maxOrder = 0;
        Cursor cursor = db.rawQuery("SELECT MAX(" + COL_ORDER_POSITION + ") FROM " + TABLE_HABITS +
                " WHERE " + COL_USER_REF + " = ?", new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) maxOrder = cursor.getInt(0);
        cursor.close();

        ContentValues values = new ContentValues();
        values.put(COL_HABIT_TITLE, title);
        values.put(COL_HABIT_CATEGORY, category != null ? category : "Без категории");
        values.put(COL_USER_REF, userId);
        values.put(COL_CREATED_AT, System.currentTimeMillis());
        values.put(COL_CREATED_DATE, todayDate);
        values.put(COL_HIDDEN_FROM, "");
        values.put(COL_ORDER_POSITION, maxOrder + 1);

        long id = db.insert(TABLE_HABITS, null, values);
        addLog("Добавлена привычка: " + title);
        return id;
    }

    public void updateHabitOrder(int habitId, int newOrder) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ORDER_POSITION, newOrder);
        db.update(TABLE_HABITS, values, COL_HABIT_ID + " = ?", new String[]{String.valueOf(habitId)});
    }

    public void hideHabitFromDate(int habitId, String date) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_HIDDEN_FROM, date);
        db.update(TABLE_HABITS, values, COL_HABIT_ID + " = ?", new String[]{String.valueOf(habitId)});

        db.delete(TABLE_COMPLETIONS,
                COL_HABIT_REF + " = ? AND " + COL_COMPLETION_DATE + " >= ?",
                new String[]{String.valueOf(habitId), date});

        addLog("Привычка ID: " + habitId + " скрыта с даты " + date);
    }

    public List<Habit> getAllHabits() {
        List<Habit> habits = new ArrayList<>();
        try {
            int userId = getCurrentUserId();
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            SQLiteDatabase db = getReadableDatabase();

            String sql = "SELECT " + COL_HABIT_ID + ", " + COL_HABIT_TITLE + ", " +
                    COL_HABIT_CATEGORY + ", " + COL_CREATED_AT +
                    " FROM " + TABLE_HABITS +
                    " WHERE " + COL_USER_REF + " = ?" +
                    " AND (" + COL_HIDDEN_FROM + " = '' OR " + COL_HIDDEN_FROM + " > ?)" +
                    " AND " + COL_CREATED_DATE + " <= ?" +
                    " ORDER BY " + COL_ORDER_POSITION + " ASC, " + COL_HABIT_ID + " ASC";

            Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), todayDate, todayDate});

            while (cursor.moveToNext()) {
                habits.add(new Habit(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3),
                        ""
                ));
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "getAllHabits error", e);
        }
        return habits;
    }

    public void deleteHabit(int habitId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_HABITS, COL_HABIT_ID + " = ?", new String[]{String.valueOf(habitId)});
        addLog("Удалена привычка ID: " + habitId);
    }

    public void cleanupOrphanedCompletions() {
        SQLiteDatabase db = getWritableDatabase();
        String sql = "DELETE FROM " + TABLE_COMPLETIONS +
                " WHERE " + COL_HABIT_REF + " NOT IN (SELECT " + COL_HABIT_ID +
                " FROM " + TABLE_HABITS + ")";
        db.execSQL(sql);
    }

    //  Выполнения 

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
            unmarkCompleted(habitId, date);
        } else {
            markCompleted(habitId, date);
        }
    }

    public void markCompleted(int habitId, String date) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_HABIT_REF, habitId);
        values.put(COL_COMPLETION_DATE, date);
        db.insert(TABLE_COMPLETIONS, null, values);
        addLog("Отмечена привычка ID: " + habitId + " за " + date);
    }

    public void unmarkCompleted(int habitId, String date) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_COMPLETIONS,
                COL_HABIT_REF + " = ? AND " + COL_COMPLETION_DATE + " = ?",
                new String[]{String.valueOf(habitId), date});
        addLog("Снята отметка привычки ID: " + habitId + " за " + date);
    }

    public List<Integer> getCompletionsForDate(String date) {
        List<Integer> completedIds = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT " + COL_HABIT_REF +
                " FROM " + TABLE_COMPLETIONS +
                " WHERE " + COL_COMPLETION_DATE + " = ?" +
                " AND " + COL_HABIT_REF + " IN (SELECT " + COL_HABIT_ID + " FROM " + TABLE_HABITS + ")";
        Cursor cursor = db.rawQuery(sql, new String[]{date});
        while (cursor.moveToNext()) {
            completedIds.add(cursor.getInt(0));
        }
        cursor.close();
        return completedIds;
    }

    //  Категории 

    public List<String> getUserCategories() {
        List<String> categoryList = new ArrayList<>();
        categoryList.add("Без категории");
        try {
            int userId = getCurrentUserId();
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_CATEGORIES,
                    new String[]{COL_CATEGORY_NAME},
                    COL_USER_REF + " = ?",
                    new String[]{String.valueOf(userId)},
                    null, null, COL_CATEGORY_NAME + " ASC");
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                if (!categoryList.contains(name)) categoryList.add(name);
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "getUserCategories error", e);
        }
        return categoryList;
    }

    public void addCategory(String categoryName) {
        int userId = getCurrentUserId();
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CATEGORY_NAME, categoryName);
        values.put(COL_USER_REF, userId);
        db.insert(TABLE_CATEGORIES, null, values);
        addLog("Добавлена категория: " + categoryName);
    }

    public void deleteCategory(String categoryName) {
        int userId = getCurrentUserId();
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_CATEGORIES,
                COL_CATEGORY_NAME + " = ? AND " + COL_USER_REF + " = ?",
                new String[]{categoryName, String.valueOf(userId)});
        ContentValues values = new ContentValues();
        values.put(COL_HABIT_CATEGORY, "Без категории");
        db.update(TABLE_HABITS, values,
                COL_HABIT_CATEGORY + " = ? AND " + COL_USER_REF + " = ?",
                new String[]{categoryName, String.valueOf(userId)});
        addLog("Удалена категория: " + categoryName);
    }

    public void updateCategory(String oldName, String newName) {
        int userId = getCurrentUserId();
        SQLiteDatabase db = getWritableDatabase();
        ContentValues catValues = new ContentValues();
        catValues.put(COL_CATEGORY_NAME, newName);
        db.update(TABLE_CATEGORIES, catValues,
                COL_CATEGORY_NAME + " = ? AND " + COL_USER_REF + " = ?",
                new String[]{oldName, String.valueOf(userId)});
        ContentValues habitValues = new ContentValues();
        habitValues.put(COL_HABIT_CATEGORY, newName);
        db.update(TABLE_HABITS, habitValues,
                COL_HABIT_CATEGORY + " = ? AND " + COL_USER_REF + " = ?",
                new String[]{oldName, String.valueOf(userId)});
        addLog("Категория переименована: " + oldName + " → " + newName);
    }

    //  Управление аккаунтом 

    public String getUserJoinDate(int userId) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    public int getTotalHabitsCount(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_HABITS, new String[]{"COUNT(*)"},
                COL_USER_REF + " = ?", new String[]{String.valueOf(userId)}, null, null, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public int getTotalCompletionsCount(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM " + TABLE_COMPLETIONS + " c " +
                "JOIN " + TABLE_HABITS + " h ON c." + COL_HABIT_REF + " = h." + COL_HABIT_ID +
                " WHERE h." + COL_USER_REF + " = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_USER_ID + " = ? AND " + COL_PASSWORD + " = ?",
                new String[]{String.valueOf(userId), oldPassword}, null, null, null);
        boolean passwordValid = cursor.getCount() > 0;
        cursor.close();
        if (!passwordValid) return false;
        ContentValues values = new ContentValues();
        values.put(COL_PASSWORD, newPassword);
        int rows = db.update(TABLE_USERS, values, COL_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        if (rows > 0) addLog("Пароль изменён для пользователя ID: " + userId);
        return rows > 0;
    }

    public boolean deleteAccount(int userId) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.beginTransaction();
            Cursor habitsCursor = db.query(TABLE_HABITS, new String[]{COL_HABIT_ID},
                    COL_USER_REF + " = ?", new String[]{String.valueOf(userId)}, null, null, null);
            List<Integer> habitIds = new ArrayList<>();
            while (habitsCursor.moveToNext()) habitIds.add(habitsCursor.getInt(0));
            habitsCursor.close();
            for (int habitId : habitIds) {
                db.delete(TABLE_COMPLETIONS, COL_HABIT_REF + " = ?", new String[]{String.valueOf(habitId)});
            }
            db.delete(TABLE_HABITS, COL_USER_REF + " = ?", new String[]{String.valueOf(userId)});
            db.delete(TABLE_CATEGORIES, COL_USER_REF + " = ?", new String[]{String.valueOf(userId)});
            int deleted = db.delete(TABLE_USERS, COL_USER_ID + " = ?", new String[]{String.valueOf(userId)});
            db.setTransactionSuccessful();
            db.endTransaction();
            if (deleted > 0) addLog("Аккаунт ID: " + userId + " удалён");
            return deleted > 0;
        } catch (Exception e) {
            Log.e(TAG, "deleteAccount error", e);
            db.endTransaction();
            return false;
        }
    }

    //  Модель привычки 

    public static class Habit {
        private int id;
        private String title;
        private String category;
        private long createdAt;
        private String hiddenFrom;

        public Habit(int id, String title, String category, long createdAt, String hiddenFrom) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.createdAt = createdAt;
            this.hiddenFrom = hiddenFrom;
        }

        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getCategory() { return category; }
        public long getCreatedAt() { return createdAt; }
        public String getHiddenFrom() { return hiddenFrom; }
    }
}