package com.example.habtrack.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habtrack.R;
import com.example.habtrack.data.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.*;

public class CalendarFragment extends Fragment {

    // Views
    private RecyclerView rvWeekdays;
    private RecyclerView rvCalendar;
    private RecyclerView rvDayHabits;
    private ScrollView svDayDetails;
    private ProgressBar progressBar;
    private TextView tvSelectedDate;
    private TextView tvProgressPercent;
    private TextView tvMonthYear;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;

    // Data
    private DatabaseHelper db;
    private Calendar currentCalendar = Calendar.getInstance();
    private Map<String, Integer> dayProgressMap = new HashMap<>();
    private Map<String, List<HabitWithStatus>> dayHabitsMap = new HashMap<>();

    // Formatters
    private SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
    private SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat monthFormat = new SimpleDateFormat("LLLL yyyy", new Locale("ru"));

    // Adapters
    private CalendarAdapter calendarAdapter;
    private int selectedPosition = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        db = DatabaseHelper.getInstance(getContext());

        // Инициализация views
        rvWeekdays = view.findViewById(R.id.rv_weekdays);
        rvCalendar = view.findViewById(R.id.rv_calendar);
        rvDayHabits = view.findViewById(R.id.rv_day_habits);
        svDayDetails = view.findViewById(R.id.sv_day_details);
        progressBar = view.findViewById(R.id.calendar_progress_bar);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        tvProgressPercent = view.findViewById(R.id.tv_calendar_progress_percent);
        tvMonthYear = view.findViewById(R.id.tv_month_year);
        btnPrevMonth = view.findViewById(R.id.btn_prev_month);
        btnNextMonth = view.findViewById(R.id.btn_next_month);

        // Настройка RecyclerView для привычек дня
        rvDayHabits.setLayoutManager(new LinearLayoutManager(getContext()));

        // Настройка дней недели
        setupWeekdays();

        // Загрузка данных
        loadAllData();

        // Обновление отображения месяца
        updateMonthDisplay();

        // Обработчики нажатий
        tvMonthYear.setOnClickListener(v -> showMonthYearPicker());

        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadAllData();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadAllData();
        });

        return view;
    }

    /* Показывает диалог выбора месяца и года*/
    private void showMonthYearPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_month_year_picker, null);
        builder.setView(dialogView);

        NumberPicker npYear = dialogView.findViewById(R.id.np_year);
        NumberPicker npMonth = dialogView.findViewById(R.id.np_month);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        int currentYear = currentCalendar.get(Calendar.YEAR);

        npYear.setMinValue(currentYear - 5);
        npYear.setMaxValue(currentYear + 5);
        npYear.setValue(currentYear);

        npMonth.setMinValue(1);
        npMonth.setMaxValue(12);
        npMonth.setValue(currentCalendar.get(Calendar.MONTH) + 1);
        npMonth.setDisplayedValues(new String[]{"Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"});

        AlertDialog dialog = builder.create();
        dialog.setCancelable(true);

        btnConfirm.setOnClickListener(v -> {
            int year = npYear.getValue();
            int month = npMonth.getValue() - 1;
            currentCalendar.set(year, month, 1);
            updateMonthDisplay();
            loadAllData();
            dialog.dismiss();
        });

        dialog.show();
    }

    /* Настройка отображения дней недели*/
    private void setupWeekdays() {
        String[] weekDays = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        WeekdayAdapter adapter = new WeekdayAdapter(weekDays);
        rvWeekdays.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvWeekdays.setAdapter(adapter);
    }

    /* Загрузка всех данных за выбранный месяц*/
    private void loadAllData() {
        dayProgressMap.clear();
        dayHabitsMap.clear();

        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);

        Calendar startCal = Calendar.getInstance();
        startCal.set(year, month, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.set(year, month, startCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);

        List<DatabaseHelper.Habit> allHabits = db.getHabits();
        if (allHabits == null) allHabits = new ArrayList<>();

        Calendar dayCal = (Calendar) startCal.clone();

        while (dayCal.before(endCal) || dayCal.equals(endCal)) {
            String dateStr = apiFormat.format(dayCal.getTime());

            List<Integer> completions = db.getCompletionsForDate(dateStr);
            if (completions == null) completions = new ArrayList<>();

            // Процент выполнения за день
            int percent = allHabits.isEmpty() ? 0 : (completions.size() * 100) / allHabits.size();
            dayProgressMap.put(dateStr, percent);

            // Список привычек с отметками
            List<HabitWithStatus> habitList = new ArrayList<>();
            for (DatabaseHelper.Habit habit : allHabits) {
                boolean isCompleted = completions.contains(habit.getId());
                habitList.add(new HabitWithStatus(habit.getTitle(), habit.getCategory(), isCompleted));
            }
            dayHabitsMap.put(dateStr, habitList);

            dayCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        setupCalendarGrid();
    }

    /* Построение сетки календаря*/
    private void setupCalendarGrid() {
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);

        Calendar firstDay = Calendar.getInstance();
        firstDay.set(year, month, 1);
        int firstDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK) - 2;
        if (firstDayOfWeek < 0) firstDayOfWeek = 6;

        int daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH);

        List<CalendarDayItem> items = new ArrayList<>();

        // Пустые ячейки в начале месяца
        for (int i = 0; i < firstDayOfWeek; i++) {
            items.add(new CalendarDayItem(-1, null, -1, 0));
        }

        // Дни месяца
        for (int i = 1; i <= daysInMonth; i++) {
            String dateStr = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, i);
            Integer percent = dayProgressMap.get(dateStr);
            if (percent == null) percent = 0;

            int color;
            if (percent == 100) {
                color = 0xFF28A745; // зелёный
            } else if (percent > 0) {
                color = 0xFFFF6B35; // оранжевый
            } else {
                color = -1; // нет фона
            }

            items.add(new CalendarDayItem(i, dateStr, color, percent));
        }

        rvCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));
        calendarAdapter = new CalendarAdapter(items, (position, dateStr, percent) -> {
            if (dateStr != null) {
                if (selectedPosition != -1) {
                    calendarAdapter.setSelectedPosition(-1);
                }
                selectedPosition = position;
                calendarAdapter.setSelectedPosition(position);
                showDayInfo(dateStr, percent);
            }
        });
        rvCalendar.setAdapter(calendarAdapter);
    }

    /* Отображение информации о выбранном дне*/
    private void showDayInfo(String date, int percent) {
        try {
            java.util.Date parsedDate = apiFormat.parse(date);
            tvSelectedDate.setText(displayFormat.format(parsedDate));
        } catch (Exception e) {
            tvSelectedDate.setText(date);
        }

        progressBar.setProgress(percent);
        tvProgressPercent.setText(percent + "%");

        List<HabitWithStatus> habits = dayHabitsMap.get(date);
        if (habits == null) habits = new ArrayList<>();

        DayHabitsAdapter adapter = new DayHabitsAdapter(habits);
        rvDayHabits.setAdapter(adapter);

        // Показываем нижнюю плашку
        svDayDetails.setVisibility(View.VISIBLE);
    }

    /* Обновление отображения месяца на экране*/
    private void updateMonthDisplay() {
        tvMonthYear.setText(monthFormat.format(currentCalendar.getTime()));
    }

    // АДАПТЕРЫ И МОДЕЛИ

    /* Адаптер для дней недели*/
    static class WeekdayAdapter extends RecyclerView.Adapter<WeekdayAdapter.ViewHolder> {
        private final String[] days;

        WeekdayAdapter(String[] days) {
            this.days = days;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(13);
            tv.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.orange_primary));
            tv.setPadding(8, 12, 8, 12);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(days[position]);
        }

        @Override
        public int getItemCount() {
            return days.length;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(TextView tv) {
                super(tv);
                textView = tv;
            }
        }
    }

    /* Модель дня календаря*/
    static class CalendarDayItem {
        int day;
        String date;
        int color;
        int percent;

        CalendarDayItem(int day, String date, int color, int percent) {
            this.day = day;
            this.date = date;
            this.color = color;
            this.percent = percent;
        }
    }

    /* Адаптер для сетки календаря */
    static class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private final List<CalendarDayItem> items;
        private final OnDayClickListener listener;
        private int selectedPosition = -1;

        interface OnDayClickListener {
            void onDayClick(int position, String date, int percent);
        }

        CalendarAdapter(List<CalendarDayItem> items, OnDayClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        void setSelectedPosition(int pos) {
            int oldPos = selectedPosition;
            selectedPosition = pos;
            if (oldPos != -1) notifyItemChanged(oldPos);
            if (pos != -1) notifyItemChanged(pos);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dpToPx(parent.getContext(), 56)));
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(16);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CalendarDayItem item = items.get(position);

            if (item.day > 0) {
                holder.textView.setText(String.valueOf(item.day));
                holder.textView.setTextColor(Color.parseColor("#212529"));

                if (position == selectedPosition) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(ContextCompat.getColor(holder.textView.getContext(), R.color.orange_primary));
                    bg.setShape(GradientDrawable.OVAL);
                    holder.textView.setBackground(bg);
                    holder.textView.setTextColor(Color.WHITE);
                } else if (item.color != -1) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(item.color);
                    bg.setShape(GradientDrawable.OVAL);
                    holder.textView.setBackground(bg);
                    if (item.color == 0xFFFF6B35) {
                        holder.textView.setTextColor(Color.WHITE);
                    } else {
                        holder.textView.setTextColor(Color.parseColor("#212529"));
                    }
                } else {
                    holder.textView.setBackgroundColor(Color.TRANSPARENT);
                }

                holder.textView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDayClick(position, item.date, item.percent);
                    }
                });
            } else {
                holder.textView.setText("");
                holder.textView.setBackgroundColor(Color.TRANSPARENT);
                holder.textView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(TextView tv) {
                super(tv);
                textView = tv;
            }
        }

        private int dpToPx(android.content.Context context, int dp) {
            return (int) (dp * context.getResources().getDisplayMetrics().density);
        }
    }

    /* Модель привычки с отметкой для отображения в деталях дня */

    private static class HabitWithStatus {
        String title;
        String category;
        boolean completed;

        HabitWithStatus(String title, String category, boolean completed) {
            this.title = title;
            this.category = category;
            this.completed = completed;
        }
    }

    /*  Адаптер для списка привычек в деталях дня */
    private static class DayHabitsAdapter extends RecyclerView.Adapter<DayHabitsAdapter.ViewHolder> {
        private final List<HabitWithStatus> habits;

        DayHabitsAdapter(List<HabitWithStatus> habits) {
            this.habits = habits;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_habit, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HabitWithStatus habit = habits.get(position);
            holder.tvTitle.setText(habit.title);
            holder.tvCategory.setText(habit.category);
            holder.checkBox.setChecked(habit.completed);
        }

        @Override
        public int getItemCount() {
            return habits.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvCategory;
            CheckBox checkBox;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_calendar_habit_title);
                tvCategory = itemView.findViewById(R.id.tv_calendar_habit_category);
                checkBox = itemView.findViewById(R.id.cb_completed);
            }
        }
    }
}