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
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.app.DatePickerDialog;

public class StatsFragment extends Fragment {

    private DatabaseHelper db;
    private TextView tvTotalHabits, tvTotalCompletions, tvStreak;
    private TextView tvSelectPeriod;
    private LineChart chartProgress;
    private PieChart chartCategories;

    private Calendar customStartDate = null;
    private Calendar customEndDate = null;

    private final SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        db = DatabaseHelper.getInstance(getContext());

        tvTotalHabits = view.findViewById(R.id.tv_total_habits);
        tvTotalCompletions = view.findViewById(R.id.tv_total_completions);
        tvStreak = view.findViewById(R.id.tv_streak);
        tvSelectPeriod = view.findViewById(R.id.tv_select_period);
        chartProgress = view.findViewById(R.id.chart_progress);
        chartCategories = view.findViewById(R.id.chart_categories);

        setupCharts();

        tvSelectPeriod.setOnClickListener(v -> showPeriodBottomSheet());

        loadWeekData();

        return view;
    }

    // Настройка графиков
    private void setupCharts() {
        chartProgress.getDescription().setEnabled(false);
        chartProgress.setTouchEnabled(true);
        chartProgress.setDragEnabled(true);
        chartProgress.setScaleEnabled(true);
        chartProgress.getAxisRight().setEnabled(false);
        chartProgress.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        XAxis xAxis = chartProgress.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        chartCategories.getDescription().setEnabled(false);
        chartCategories.setUsePercentValues(true);
        chartCategories.setEntryLabelTextSize(11f);
        chartCategories.setDrawEntryLabels(true);
    }

    // Нижний лист с выбором периода
    private void showPeriodBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_period, null);
        bottomSheet.setContentView(sheetView);

        sheetView.findViewById(R.id.btn_today).setOnClickListener(v -> {
            loadTodayData();
            tvSelectPeriod.setText("Сегодня");
            bottomSheet.dismiss();
        });

        sheetView.findViewById(R.id.btn_yesterday).setOnClickListener(v -> {
            loadYesterdayData();
            tvSelectPeriod.setText("Вчера");
            bottomSheet.dismiss();
        });

        sheetView.findViewById(R.id.btn_week).setOnClickListener(v -> {
            loadWeekData();
            tvSelectPeriod.setText("Неделя");
            bottomSheet.dismiss();
        });

        sheetView.findViewById(R.id.btn_month).setOnClickListener(v -> {
            loadMonthData();
            tvSelectPeriod.setText("Месяц");
            bottomSheet.dismiss();
        });

        sheetView.findViewById(R.id.btn_custom).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showCustomDatePicker();
        });

        bottomSheet.show();
    }

    // Выбор произвольного диапазона дат
    private void showCustomDatePicker() {
        Calendar tempStart = customStartDate != null ? customStartDate : Calendar.getInstance();
        tempStart.add(Calendar.DAY_OF_YEAR, -6);

        DatePickerDialog startDialog = new DatePickerDialog(
                requireContext(),
                R.style.CustomDatePickerTheme,
                (view, year, month, dayOfMonth) -> {
                    customStartDate = Calendar.getInstance();
                    customStartDate.set(year, month, dayOfMonth);

                    DatePickerDialog endDialog = new DatePickerDialog(
                            requireContext(),
                            R.style.CustomDatePickerTheme,
                            (view2, year2, month2, dayOfMonth2) -> {
                                customEndDate = Calendar.getInstance();
                                customEndDate.set(year2, month2, dayOfMonth2);

                                if (customStartDate.after(customEndDate)) {
                                    Calendar temp = customStartDate;
                                    customStartDate = customEndDate;
                                    customEndDate = temp;
                                }

                                loadCustomData();
                                tvSelectPeriod.setText(displayFormat.format(customStartDate.getTime()) + " - " + displayFormat.format(customEndDate.getTime()));
                            },
                            tempStart.get(Calendar.YEAR),
                            tempStart.get(Calendar.MONTH),
                            tempStart.get(Calendar.DAY_OF_MONTH)
                    );
                    endDialog.show();
                },
                tempStart.get(Calendar.YEAR),
                tempStart.get(Calendar.MONTH),
                tempStart.get(Calendar.DAY_OF_MONTH)
        );
        startDialog.show();
    }

    // Загрузка данных за сегодня
    private void loadTodayData() {
        Calendar today = Calendar.getInstance();
        customStartDate = (Calendar) today.clone();
        customEndDate = (Calendar) today.clone();
        loadCustomData();
    }

    // Загрузка данных за вчера
    private void loadYesterdayData() {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        customStartDate = (Calendar) yesterday.clone();
        customEndDate = (Calendar) yesterday.clone();
        loadCustomData();
    }

    // Загрузка данных за последние 7 дней
    private void loadWeekData() {
        customStartDate = Calendar.getInstance();
        customStartDate.add(Calendar.DAY_OF_YEAR, -6);
        customStartDate.set(Calendar.HOUR_OF_DAY, 0);
        customEndDate = Calendar.getInstance();
        loadCustomData();
    }

    // Загрузка данных за последние 30 дней
    private void loadMonthData() {
        customStartDate = Calendar.getInstance();
        customStartDate.add(Calendar.DAY_OF_YEAR, -29);
        customStartDate.set(Calendar.HOUR_OF_DAY, 0);
        customEndDate = Calendar.getInstance();
        loadCustomData();
    }

    // Загрузка данных за выбранный период
    private void loadCustomData() {
        if (customStartDate == null || customEndDate == null) return;

        List<DatabaseHelper.Habit> allHabits = db.getAllHabits();
        int totalHabits = allHabits.size();

        int totalCompletions = 0;
        Map<String, Integer> categoryCount = new HashMap<>();
        List<DayStat> dayStats = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        Calendar current = (Calendar) customStartDate.clone();
        int daysCount = 0;

        while (!current.after(customEndDate)) {
            String dateStr = apiFormat.format(current.getTime());
            List<Integer> completions = db.getCompletionsForDate(dateStr);
            totalCompletions += completions.size();

            List<DatabaseHelper.Habit> habitsForDay = db.getHabitsForDate(dateStr);
            int habitsCount = habitsForDay.size();
            int percent = habitsCount > 0 ? (completions.size() * 100) / habitsCount : 0;

            dayStats.add(new DayStat(percent));

            // Формирование подписей для оси X
            if ((customStartDate.getTimeInMillis() == customEndDate.getTimeInMillis()) ||
                    (customEndDate.getTimeInMillis() - customStartDate.getTimeInMillis() <= 7 * 24 * 60 * 60 * 1000)) {
                labels.add(new SimpleDateFormat("dd.MM", Locale.getDefault()).format(current.getTime()));
            } else if (daysCount % 3 == 0) {
                labels.add(new SimpleDateFormat("dd.MM", Locale.getDefault()).format(current.getTime()));
            } else {
                labels.add("");
            }

            for (DatabaseHelper.Habit habit : habitsForDay) {
                String cat = habit.getCategory();
                if (cat == null || cat.isEmpty()) cat = "Без категории";
                categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + 1);
            }

            current.add(Calendar.DAY_OF_YEAR, 1);
            daysCount++;
        }

        tvTotalHabits.setText(String.valueOf(totalHabits));
        tvTotalCompletions.setText(String.valueOf(totalCompletions));
        tvStreak.setText(String.valueOf(calculateCurrentStreak()));

        drawLineChart(dayStats, labels);
        drawPieChart(categoryCount);
    }

    // Расчёт текущей серии выполнений
    private int calculateCurrentStreak() {
        int streak = 0;
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 365; i++) {
            String dateStr = apiFormat.format(calendar.getTime());
            List<Integer> completions = db.getCompletionsForDate(dateStr);
            if (!completions.isEmpty()) {
                streak++;
            } else {
                break;
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }
        return streak;
    }

    // Построение линейного графика прогресса
    private void drawLineChart(List<DayStat> dayStats, List<String> labels) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < dayStats.size(); i++) {
            entries.add(new Entry(i, dayStats.get(i).percent));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Прогресс %");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.orange_primary));
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.orange_primary));
        dataSet.setCircleRadius(5f);
        dataSet.setCircleHoleRadius(3f);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        chartProgress.setData(lineData);

        chartProgress.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartProgress.getXAxis().setLabelRotationAngle(labels.size() > 15 ? -45f : 0f);
        chartProgress.animateX(800);
        chartProgress.invalidate();
    }

    // Построение круговой диаграммы по категориям
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

        int[] colors = {
                ContextCompat.getColor(requireContext(), R.color.orange_primary),
                ContextCompat.getColor(requireContext(), R.color.orange_dark),
                0xFFFF8C42, 0xFFE8A070, 0xFFC2AC98,
                0xFF9E8E7A, 0xFF7A6B5C, 0xFF5C5248,
                0xFF403A35, 0xFFB0A59C
        };

        PieDataSet dataSet = new PieDataSet(entries, "Категории");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        PieData pieData = new PieData(dataSet);
        chartCategories.setData(pieData);
        chartCategories.animateY(800);
        chartCategories.invalidate();
    }

    // Вспомогательный класс для хранения дневной статистики
    private static class DayStat {
        int percent;
        DayStat(int percent) { this.percent = percent; }
    }
}