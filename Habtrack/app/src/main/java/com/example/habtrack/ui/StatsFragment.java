package com.example.habtrack.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.habtrack.R;
import com.example.habtrack.data.DatabaseHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    private DatabaseHelper db;
    private TextView tvTotalHabits, tvTotalCompletions, tvStreak;
    private TextView tvPeriodWeek, tvPeriodMonth;
    private LineChart chartProgress;
    private PieChart chartCategories;

    private boolean isWeekMode = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        db = DatabaseHelper.getInstance(getContext());

        tvTotalHabits = view.findViewById(R.id.tv_total_habits);
        tvTotalCompletions = view.findViewById(R.id.tv_total_completions);
        tvStreak = view.findViewById(R.id.tv_streak);
        tvPeriodWeek = view.findViewById(R.id.tv_period_week);
        tvPeriodMonth = view.findViewById(R.id.tv_period_month);
        chartProgress = view.findViewById(R.id.chart_progress);
        chartCategories = view.findViewById(R.id.chart_categories);

        tvPeriodWeek.setOnClickListener(v -> {
            isWeekMode = true;
            updatePeriodButtons();
            loadStats();
        });

        tvPeriodMonth.setOnClickListener(v -> {
            isWeekMode = false;
            updatePeriodButtons();
            loadStats();
        });

        loadStats();

        return view;
    }

    private void updatePeriodButtons() {
        if (isWeekMode) {
            tvPeriodWeek.setBackgroundResource(R.drawable.bg_button_primary);
            tvPeriodWeek.setTextColor(Color.WHITE);
            tvPeriodMonth.setBackgroundResource(R.drawable.bg_button_secondary);
            tvPeriodMonth.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange_primary));
        } else {
            tvPeriodMonth.setBackgroundResource(R.drawable.bg_button_primary);
            tvPeriodMonth.setTextColor(Color.WHITE);
            tvPeriodWeek.setBackgroundResource(R.drawable.bg_button_secondary);
            tvPeriodWeek.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange_primary));
        }
    }

    private void loadStats() {
        List<DatabaseHelper.Habit> allHabits = db.getAllHabits();
        int totalHabits = allHabits.size();

        int totalCompletions = 0;

        Map<String, Integer> categoryCount = new HashMap<>();

        List<DayStats> dayStatsList = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        Calendar startDate = Calendar.getInstance();

        if (isWeekMode) {
            startDate.add(Calendar.DAY_OF_YEAR, -6);
            startDate.set(Calendar.HOUR_OF_DAY, 0);
            startDate.set(Calendar.MINUTE, 0);
            startDate.set(Calendar.SECOND, 0);

            Calendar endDate = Calendar.getInstance();
            endDate.set(Calendar.HOUR_OF_DAY, 23);
            endDate.set(Calendar.MINUTE, 59);
            endDate.set(Calendar.SECOND, 59);

            Calendar current = (Calendar) startDate.clone();
            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("E", new Locale("ru"));

            while (!current.after(endDate)) {
                String dateStr = apiFormat.format(current.getTime());
                List<Integer> completions = db.getCompletionsForDate(dateStr);
                totalCompletions += completions.size();

                List<DatabaseHelper.Habit> habitsForDay = db.getHabitsForDate(dateStr);
                int habitsCount = habitsForDay.size();
                int completionsCount = completions.size();
                int percent = habitsCount > 0 ? (completionsCount * 100) / habitsCount : 0;

                String dayName = displayFormat.format(current.getTime());
                labels.add(dayName);
                dayStatsList.add(new DayStats(percent, completionsCount));

                for (DatabaseHelper.Habit habit : habitsForDay) {
                    String category = habit.getCategory();
                    if (category == null || category.isEmpty()) category = "Без категории";
                    categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
                }

                current.add(Calendar.DAY_OF_YEAR, 1);
            }
        } else {
            startDate.add(Calendar.DAY_OF_YEAR, -29);
            startDate.set(Calendar.HOUR_OF_DAY, 0);
            startDate.set(Calendar.MINUTE, 0);
            startDate.set(Calendar.SECOND, 0);

            Calendar endDate = Calendar.getInstance();
            endDate.set(Calendar.HOUR_OF_DAY, 23);
            endDate.set(Calendar.MINUTE, 59);
            endDate.set(Calendar.SECOND, 59);

            Calendar current = (Calendar) startDate.clone();
            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM", new Locale("ru"));

            int dayIndex = 0;
            while (!current.after(endDate)) {
                String dateStr = apiFormat.format(current.getTime());
                List<Integer> completions = db.getCompletionsForDate(dateStr);
                totalCompletions += completions.size();

                List<DatabaseHelper.Habit> habitsForDay = db.getHabitsForDate(dateStr);
                int habitsCount = habitsForDay.size();
                int completionsCount = completions.size();
                int percent = habitsCount > 0 ? (completionsCount * 100) / habitsCount : 0;

                if (dayIndex % 3 == 0 || dayIndex == 29) {
                    labels.add(displayFormat.format(current.getTime()));
                } else {
                    labels.add("");
                }
                dayStatsList.add(new DayStats(percent, completionsCount));

                for (DatabaseHelper.Habit habit : habitsForDay) {
                    String category = habit.getCategory();
                    if (category == null || category.isEmpty()) category = "Без категории";
                    categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
                }

                current.add(Calendar.DAY_OF_YEAR, 1);
                dayIndex++;
            }
        }

        tvTotalHabits.setText(String.valueOf(totalHabits));
        tvTotalCompletions.setText(String.valueOf(totalCompletions));

        int streak = calculateCurrentStreak();
        tvStreak.setText(String.valueOf(streak));

        drawLineChart(dayStatsList, labels);
        drawPieChart(categoryCount);
    }

    private int calculateCurrentStreak() {
        int streak = 0;
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < 365; i++) {
            String dateStr = apiFormat.format(calendar.getTime());
            List<Integer> completions = db.getCompletionsForDate(dateStr);
            boolean hasCompletions = !completions.isEmpty();

            if (hasCompletions) {
                streak++;
            } else {
                break;
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }
        return streak;
    }

    private void drawLineChart(List<DayStats> dayStatsList, List<String> labels) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < dayStatsList.size(); i++) {
            entries.add(new Entry(i, dayStatsList.get(i).percent));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Прогресс %");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.orange_primary));
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.orange_primary));
        dataSet.setCircleRadius(5f);
        dataSet.setCircleHoleRadius(3f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        dataSet.setDrawValues(true);

        LineData lineData = new LineData(dataSet);
        chartProgress.setData(lineData);

        XAxis xAxis = chartProgress.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        chartProgress.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        chartProgress.getAxisRight().setEnabled(false);
        chartProgress.getDescription().setEnabled(false);
        chartProgress.setTouchEnabled(true);
        chartProgress.setDragEnabled(true);
        chartProgress.setScaleEnabled(true);
        chartProgress.animateX(1000);

        chartProgress.invalidate();
    }

    private void drawPieChart(Map<String, Integer> categoryCount) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "Нет данных"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Категории");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        dataSet.setSliceSpace(3f);

        PieData pieData = new PieData(dataSet);
        chartCategories.setData(pieData);
        chartCategories.setUsePercentValues(true);
        chartCategories.getDescription().setEnabled(false);
        chartCategories.setEntryLabelTextSize(10f);
        chartCategories.setDrawEntryLabels(true);
        chartCategories.animateY(1000);

        chartCategories.invalidate();
    }

    private static class DayStats {
        int percent;
        int completions;

        DayStats(int percent, int completions) {
            this.percent = percent;
            this.completions = completions;
        }
    }
}