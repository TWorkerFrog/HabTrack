package com.example.habtrack.ui;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habtrack.R;
import com.example.habtrack.data.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.reflect.Method;
import android.util.Log;

public class HabitsFragment extends Fragment {

    private RecyclerView recyclerView;
    private HabitAdapter adapter;
    private List<DatabaseHelper.Habit> habits = new ArrayList<>();
    private Set<Integer> completedToday = new HashSet<>();
    private ProgressBar progressBar;
    private TextView tvProgressPercent;
    private Button btnCheckAll;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private DatabaseHelper db;
    private String todayDate;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_habits, container, false);

        db = DatabaseHelper.getInstance(getContext());
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        recyclerView = view.findViewById(R.id.rv_habits);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        progressBar = view.findViewById(R.id.progress_bar);
        tvProgressPercent = view.findViewById(R.id.tv_progress_percent);
        btnCheckAll = view.findViewById(R.id.btn_check_all);

        loadData();

        adapter = new HabitAdapter(habits, completedToday,
                new HabitAdapter.Callbacks() {
                    @Override
                    public void onToggle(DatabaseHelper.Habit habit) {
                        if (completedToday.contains(habit.getId())) {
                            completedToday.remove(habit.getId());
                        } else {
                            completedToday.add(habit.getId());
                        }
                        db.toggleCompletion(habit.getId(), todayDate);
                        updateProgress();
                        updateCheckAllButtonText();
                        mainHandler.post(() -> adapter.notifyDataSetChanged());
                    }

                    @Override
                    public void onEdit(DatabaseHelper.Habit habit) {
                        showEditHabitDialog(habit);
                    }

                    @Override
                    public void onDelete(DatabaseHelper.Habit habit) {
                        showDeleteConfirmDialog(habit);
                    }
                });

        recyclerView.setAdapter(adapter);

        btnCheckAll.setOnClickListener(v -> {
            if (habits == null || habits.isEmpty()) return;

            boolean allChecked = completedToday.size() == habits.size();

            if (allChecked) {
                // Снимаем все отметки
                for (DatabaseHelper.Habit habit : habits) {
                    if (completedToday.contains(habit.getId())) {
                        db.toggleCompletion(habit.getId(), todayDate);
                    }
                }
            } else {
                // Отмечаем все
                for (DatabaseHelper.Habit habit : habits) {
                    if (!completedToday.contains(habit.getId())) {
                        db.toggleCompletion(habit.getId(), todayDate);
                    }
                }
            }

            // Синхронизируем локальный Set с БД
            syncCompletionsFromDb();

            updateProgress();
            updateCheckAllButtonText();
            mainHandler.post(() -> adapter.notifyDataSetChanged());
        });

        updateProgress();
        updateCheckAllButtonText();

        return view;
    }
    private void syncCompletionsFromDb() {
        List<Integer> completions = db.getCompletionsForDate(todayDate);
        completedToday.clear();
        completedToday.addAll(completions);
    }


    public void refreshData() {
        loadData();
        mainHandler.post(() -> {
            adapter.notifyDataSetChanged();
            updateProgress();
            updateCheckAllButtonText();
        });
    }

    private void updateProgress() {
        if (habits == null || habits.isEmpty()) {
            progressBar.setProgress(0);
            tvProgressPercent.setText("0%");
            return;
        }
        int percent = (completedToday.size() * 100) / habits.size();
        progressBar.setProgress(percent);
        tvProgressPercent.setText(percent + "%");
    }

    private void updateCheckAllButtonText() {
        if (habits == null || habits.isEmpty()) {
            btnCheckAll.setText("Отметить все привычки");
            return;
        }
        if (completedToday.size() == habits.size()) {
            btnCheckAll.setText("Снять все отметки");
        } else {
            btnCheckAll.setText("Отметить все привычки");
        }
    }

    public void addHabit(String title, String category) {
        if (title == null || title.trim().isEmpty()) return;
        db.addHabit(title, category);
        refreshData();
    }

    private void showEditHabitDialog(DatabaseHelper.Habit habit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_habit, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.dialog_et_name);
        Spinner spinnerCategory = view.findViewById(R.id.dialog_spinner_category);
        Button btnSave = view.findViewById(R.id.dialog_btn_save);

        etName.setText(habit.getTitle());

        // Получаем категории из БД
        List<String> categoriesList = db.getUserCategories();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_spinner_item,
                categoriesList
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView tv = (TextView) view;
                    tv.setTextColor(getResources().getColor(R.color.text_primary));
                    tv.setTextSize(14);
                    tv.setPadding(10, 10, 10, 10);
                }
                return view;
            }

            @NonNull
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView tv = (TextView) view;
                    tv.setTextColor(getResources().getColor(R.color.text_primary));
                    tv.setBackgroundColor(getResources().getColor(R.color.white));
                    tv.setPadding(16, 14, 16, 14);
                    tv.setTextSize(14);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        int selectedPosition = categoriesList.indexOf(habit.getCategory());
        if (selectedPosition < 0) selectedPosition = 0;
        spinnerCategory.setSelection(selectedPosition);

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String newTitle = etName.getText().toString().trim();
            String newCategory = spinnerCategory.getSelectedItem().toString();

            if (newTitle.isEmpty()) {
                etName.setError("Введите название");
                return;
            }

            String finalCategory = newCategory.equals("Без категории") ? "" : newCategory;
            updateHabitInDatabase(habit.getId(), newTitle, finalCategory);
            dialog.dismiss();
            refreshData();
        });

        dialog.show();
    }

    private void updateHabitInDatabase(int habitId, String newTitle, String newCategory) {
        SQLiteDatabase database = db.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", newTitle);
        values.put("category", newCategory);
        database.update("habits", values, "habit_id = ?", new String[]{String.valueOf(habitId)});
        database.close();
        refreshData();
    }

    private void showDeleteConfirmDialog(DatabaseHelper.Habit habit) {
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Log.d("DEBUG_UI", "=== Delete dialog ===");
        Log.d("DEBUG_UI", "Habit: id=" + habit.getId() + ", title=" + habit.getTitle());
        Log.d("DEBUG_UI", "Current date: " + currentDate);

        new AlertDialog.Builder(getContext())
                .setTitle("Удалить привычку")
                .setMessage("Как удалить \"" + habit.getTitle() + "\"?")
                .setPositiveButton("Только на сегодня", (dialog, which) -> {
                    Log.d("DEBUG_UI", "User chose: ONLY TODAY");
                    db.hideHabitFromDate(habit.getId(), currentDate);
                    refreshData();
                    Toast.makeText(getContext(), "Привычка скрыта с сегодняшнего дня", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Навсегда", (dialog, which) -> {
                    Log.d("DEBUG_UI", "User chose: FOREVER");
                    db.deleteHabit(habit.getId());
                    refreshData();
                    Toast.makeText(getContext(), "Привычка удалена навсегда", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void loadData() {
        Log.d("DEBUG_UI", "=== loadData called ===");
        habits.clear();
        habits.addAll(db.getAllHabits());
        Log.d("DEBUG_UI", "Loaded " + habits.size() + " habits");

        completedToday.clear();
        List<Integer> completions = db.getCompletionsForDate(todayDate);
        completedToday.addAll(completions);
        Log.d("DEBUG_UI", "Loaded " + completions.size() + " completions for " + todayDate);
    }

    // Адаптер
    public static class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.ViewHolder> {
        private final List<DatabaseHelper.Habit> habits;
        private final Set<Integer> completedToday;
        private final Callbacks callbacks;

        interface Callbacks {
            void onToggle(DatabaseHelper.Habit habit);
            void onEdit(DatabaseHelper.Habit habit);
            void onDelete(DatabaseHelper.Habit habit);
        }

        public HabitAdapter(List<DatabaseHelper.Habit> habits, Set<Integer> completedToday, Callbacks callbacks) {
            this.habits = habits;
            this.completedToday = completedToday;
            this.callbacks = callbacks;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DatabaseHelper.Habit habit = habits.get(position);
            holder.tvTitle.setText(habit.getTitle());
            holder.tvCategory.setText(habit.getCategory());

            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(completedToday.contains(habit.getId()));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (callbacks != null) {
                    callbacks.onToggle(habit);
                }
            });

            holder.btnMenu.setOnClickListener(v -> {
                // Используем ТВОЮ светлую тему вместо Holo.Light
                android.content.Context wrapper = new android.view.ContextThemeWrapper(
                        v.getContext(),
                        R.style.MyPopupMenuTheme  // ← твоя тема
                );
                PopupMenu popupMenu = new PopupMenu(wrapper, holder.btnMenu);
                popupMenu.inflate(R.menu.habit_menu);

                try {
                    java.lang.reflect.Field field = popupMenu.getClass().getDeclaredField("mPopup");
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceShowIcon.invoke(menuPopupHelper, true);

                    // Дополнительно устанавливаем фон через рефлексию
                    Method setBackgroundDrawable = classPopupHelper.getMethod("setBackgroundDrawable", android.graphics.drawable.Drawable.class);
                    android.graphics.drawable.Drawable drawable = v.getContext().getResources().getDrawable(R.drawable.popup_menu_background);
                    setBackgroundDrawable.invoke(menuPopupHelper, drawable);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                popupMenu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_edit) {
                        callbacks.onEdit(habit);
                        return true;
                    } else if (itemId == R.id.menu_delete) {
                        callbacks.onDelete(habit);
                        return true;
                    }
                    return false;
                });
                popupMenu.show();
            });
        }

        @Override
        public int getItemCount() {
            return habits.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCategory;
            CheckBox checkBox;
            ImageButton btnMenu;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_habit_title);
                tvCategory = itemView.findViewById(R.id.tv_habit_category);
                checkBox = itemView.findViewById(R.id.cb_completed);
                btnMenu = itemView.findViewById(R.id.btn_menu);
            }
        }
    }
    public void refreshCategories() {
        // Если в фрагменте есть спиннер с категориями — обновляем
        // Пока просто заглушка
    }
}