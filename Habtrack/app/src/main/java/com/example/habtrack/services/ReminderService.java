package com.example.habtrack.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import com.example.habtrack.MainActivity;
import com.example.habtrack.R;
import com.example.habtrack.data.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ReminderService extends BroadcastReceiver {

    private static final String CHANNEL_ID = "habit_reminder_channel";
    private static final int NOTIFICATION_ID = 1001;

    private final String[] FUNNY_MESSAGES = {
            "Эй, герой! Ты сегодня ещё не отметил свои привычки. Скоро серия погаснет!",
            "Ваша серия в опасности! Сделайте это сейчас, пока не поздно!",
            "Псс... Твои привычки скучают по тебе. Вернись и отметь их!",
            "Ты что, забыл? А я напомню. Бегом отмечать привычки! ️",
            "Огонёк гаснет... Разве ты этого хочешь?",
            "Даже Бэтмен каждый день тренируется. А ты?",
            "Слабо выполнить сегодняшние привычки?",
            "Прогресс не любит пропусков. Давай, ты сможешь!",
            "Твои привычки: 'Мы тебя ждём...' Ну, ты понял",
            "Это не напоминание, это крик души! Отмечай привычки!"
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        // Проверяем, выполнены ли все привычки сегодня
        if (areAllHabitsCompletedToday(context)) {
            // Всё выполнено — никаких напоминаний
            return;
        }

        // Отправляем уведомление
        sendNotification(context);
    }

    private boolean areAllHabitsCompletedToday(Context context) {
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        List<DatabaseHelper.Habit> habits = db.getAllHabits();
        List<Integer> completions = db.getCompletionsForDate(todayDate);

        if (habits.isEmpty()) return true;

        for (DatabaseHelper.Habit habit : habits) {
            if (!completions.contains(habit.getId())) {
                return false;
            }
        }
        return true;
    }

    private void sendNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Создаём канал (для Android 8+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Напоминания о привычках",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Ежедневные напоминания для поддержания серии");
            manager.createNotificationChannel(channel);
        }

        // Случайное смешное сообщение
        Random random = new Random();
        String message = FUNNY_MESSAGES[random.nextInt(FUNNY_MESSAGES.length)];

        // Intent при нажатии на уведомление
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_habit)
                .setContentTitle("🔥 Внимание! Серия под угрозой!")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        manager.notify(NOTIFICATION_ID, builder.build());
    }
}