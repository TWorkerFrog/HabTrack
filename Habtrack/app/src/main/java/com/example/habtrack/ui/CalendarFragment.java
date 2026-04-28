package com.example.habtrack.ui;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habtrack.R;
import com.example.habtrack.data.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.*;

public class CalendarFragment extends Fragment {

    private RecyclerView rvCalendar;
    private RecyclerView rvDayHabits;
    private com.google.android.material.card.MaterialCardView cardDayInfo;
    private ProgressBar progressBar;
    private TextView tvSelectedDate, tvProgressPercent, tvMonthYear, tvExpandHint;
    private ImageButton btnPrevMonth, btnNextMonth;
    private LinearLayout btnExpand;
    private ImageView ivExpandArrow;
    private DatabaseHelper db;
    private Calendar currentCalendar = Calendar.getInstance();
    private Map<String, Integer> dayProgressMap = new HashMap<>();
    private Map<String, List<HabitWithStatus>> dayHabitsMap = new HashMap<>();
    private SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
    private SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat monthFormat = new SimpleDateFormat("LLLL yyyy", new Locale("ru"));
    private boolean isExpanded = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        db = DatabaseHelper.getInstance(getContext());

        rvCalendar = view.findViewById(R.id.rv_calendar);
        rvDayHabits = view.findViewById(R.id.rv_day_habits);
        cardDayInfo = view.findViewById(R.id.card_day_info);
        progressBar = view.findViewById(R.id.calendar_progress_bar);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        tvProgressPercent = view.findViewById(R.id.tv_calendar_progress_percent);
        tvMonthYear = view.findViewById(R.id.tv_month_year);
        tvExpandHint = view.findViewById(R.id.tv_expand_hint);
        btnPrevMonth = view.findViewById(R.id.btn_prev_month);
        btnNextMonth = view.findViewById(R.id.btn_next_month);
        btnExpand = view.findViewById(R.id.btn_expand);
        ivExpandArrow = view.findViewById(R.id.iv_expand_arrow);

        rvDayHabits.setLayoutManager(new LinearLayoutManager(getContext()));

        setupWeekDays(view);
        loadAllData();
        updateMonthDisplay();

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

        btnExpand.setOnClickListener(v -> {
            if (isExpanded) {
                rvDayHabits.setVisibility(View.GONE);
                tvExpandHint.setText("▶ Подробнее");
                ivExpandArrow.setImageResource(R.drawable.ic_chevron_down);
            } else {
                rvDayHabits.setVisibility(View.VISIBLE);
                tvExpandHint.setText("▼ Скрыть");
                ivExpandArrow.setImageResource(R.drawable.ic_chevron_up);
            }
            isExpanded = !isExpanded;
        });

        return view;
    }

    private void showMonthYearPicker() {
        int currentYear = currentCalendar.get(Calendar.YEAR);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_month_year_picker, null);
        builder.setView(dialogView);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        NumberPicker npYear = dialogView.findViewById(R.id.np_year);
        NumberPicker npMonth = dialogView.findViewById(R.id.np_month);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        tvDialogTitle.setText("Выберите месяц и год");

        npYear.setMinValue(currentYear - 5);
        npYear.setMaxValue(currentYear + 5);
        npYear.setValue(currentYear);

        npMonth.setMinValue(1);
        npMonth.setMaxValue(12);
        npMonth.setValue(currentCalendar.get(Calendar.MONTH) + 1);
        npMonth.setDisplayedValues(new String[]{"Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"});

        AlertDialog dialog = builder.create();

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

    private void setupWeekDays(View view) {
        GridLayout gridWeekdays = view.findViewById(R.id.grid_weekdays);
        if (gridWeekdays == null) return;
        gridWeekdays.removeAllViews();

        String[] weekDays = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

        for (String day : weekDays) {
            TextView tv = new TextView(getContext());
            tv.setText(day);
            tv.setTextSize(12);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextColor(getResources().getColor(R.color.orange_primary));
            // БЕЗ ШРИФТА
            tv.setPadding(8, 8, 8, 8);
            gridWeekdays.addView(tv);
        }
    }

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

            int percent = allHabits.isEmpty() ? 0 : (completions.size() * 100) / allHabits.size();
            dayProgressMap.put(dateStr, percent);

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

    private void setupCalendarGrid() {
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);

        Calendar firstDay = Calendar.getInstance();
        firstDay.set(year, month, 1);
        int firstDayOfWeek = firstDay.get(Calendar.DAY_OF_WEEK) - 2;
        if (firstDayOfWeek < 0) firstDayOfWeek = 6;

        int daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH);

        List<CalendarDay> days = new ArrayList<>();

        for (int i = 0; i < firstDayOfWeek; i++) {
            days.add(new CalendarDay(-1, null, -1));
        }

        for (int i = 1; i <= daysInMonth; i++) {
            String dateStr = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, i);
            Integer percent = dayProgressMap.get(dateStr);
            int color;
            if (percent == null) color = -1;
            else if (percent == 100) color = 0xFF28A745;
            else if (percent > 0) color = 0xFFFF6B35;
            else color = 0xFFE0E0E0;
            days.add(new CalendarDay(i, dateStr, color, percent));
        }

        rvCalendar.setLayoutManager(new GridLayoutManager(getContext(), 7));
        CalendarAdapter adapter = new CalendarAdapter(days, (date, percent) -> {
            if (date != null) {
                showDayInfo(date, percent);
            }
        });
        rvCalendar.setAdapter(adapter);
    }

    private void showDayInfo(String date, int percent) {
        try {
            java.util.Date parsedDate = apiFormat.parse(date);
            tvSelectedDate.setText(displayFormat.format(parsedDate));
        } catch (Exception e) {
            tvSelectedDate.setText(date);
        }

        progressBar.setProgress(percent);
        tvProgressPercent.setText(percent + "%");

        rvDayHabits.setVisibility(View.GONE);
        isExpanded = false;
        tvExpandHint.setText("▶ Подробнее");
        ivExpandArrow.setImageResource(R.drawable.ic_chevron_down);

        List<HabitWithStatus> habits = dayHabitsMap.get(date);
        if (habits == null) habits = new ArrayList<>();
        DayHabitsAdapter adapter = new DayHabitsAdapter(habits);
        rvDayHabits.setAdapter(adapter);

        cardDayInfo.setVisibility(View.VISIBLE);
    }

    private void updateMonthDisplay() {
        tvMonthYear.setText(monthFormat.format(currentCalendar.getTime()));
    }

    private static class CalendarDay {
        int day;
        String date;
        int color;
        int percent;
        CalendarDay(int day, String date, int color) {
            this.day = day;
            this.date = date;
            this.color = color;
        }
        CalendarDay(int day, String date, int color, int percent) {
            this.day = day;
            this.date = date;
            this.color = color;
            this.percent = percent;
        }
    }

    private static class HabitWithStatus {
        String title, category;
        boolean completed;
        HabitWithStatus(String title, String category, boolean completed) {
            this.title = title;
            this.category = category;
            this.completed = completed;
        }
    }

    private static class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        private final List<CalendarDay> days;
        private final OnDayClickListener listener;

        interface OnDayClickListener {
            void onDayClick(String date, int percent);
        }

        CalendarAdapter(List<CalendarDay> days, OnDayClickListener listener) {
            this.days = days;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dpToPx(parent.getContext(), 56)));
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(16);
            // БЕЗ ШРИФТА
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CalendarDay day = days.get(position);
            if (day.day > 0) {
                holder.textView.setText(String.valueOf(day.day));
                holder.textView.setTextColor(Color.parseColor("#212529"));
                if (day.color != -1) {
                    holder.textView.setBackgroundColor(day.color);
                    holder.textView.setPadding(8, 8, 8, 8);
                } else {
                    holder.textView.setBackgroundColor(Color.TRANSPARENT);
                }
                holder.textView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDayClick(day.date, day.percent);
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
            return days.size();
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

            if (habit.completed) {
                holder.ivStatus.setImageResource(android.R.drawable.checkbox_on_background);
                holder.ivStatus.setColorFilter(Color.parseColor("#28A745"));
            } else {
                holder.ivStatus.setImageResource(android.R.drawable.checkbox_off_background);
                holder.ivStatus.setColorFilter(Color.parseColor("#9E9E9E"));
            }
        }

        @Override
        public int getItemCount() {
            return habits.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCategory;
            ImageView ivStatus;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_calendar_habit_title);
                tvCategory = itemView.findViewById(R.id.tv_calendar_habit_category);
                ivStatus = itemView.findViewById(R.id.iv_calendar_habit_status);
            }
        }
    }
}