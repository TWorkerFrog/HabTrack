package com.example.habtrack.ui;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habtrack.R;
import com.example.habtrack.data.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.*;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import com.example.habtrack.auth.AuthManager;

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
    private LinearLayout layoutStreak;
    private TextView tvStreakDays;
    private TextView tvStreakMessage;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("STREAK_DEBUG", "todayDate = " + todayDate);
        View view = inflater.inflate(R.layout.fragment_habits, container, false);

        db = DatabaseHelper.getInstance(getContext());
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        recyclerView = view.findViewById(R.id.rv_habits);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        progressBar = view.findViewById(R.id.progress_bar);
        tvProgressPercent = view.findViewById(R.id.tv_progress_percent);
        btnCheckAll = view.findViewById(R.id.btn_check_all);
        layoutStreak = view.findViewById(R.id.layout_streak);
        tvStreakDays = view.findViewById(R.id.tv_streak_days);
        tvStreakMessage = view.findViewById(R.id.tv_streak_message);

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
                        new Handler(Looper.getMainLooper()).postDelayed(() -> updateStreakDisplay(), 200);
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

        // Drag-and-drop для перетаскивания привычек
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder dragged,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = dragged.getAdapterPosition();
                int to = target.getAdapterPosition();
                if (from == to) return true;

                DatabaseHelper.Habit habit = habits.get(from);
                habits.remove(from);
                habits.add(to, habit);

                for (int i = 0; i < habits.size(); i++) {
                    db.updateHabitOrder(habits.get(i).getId(), i);
                }

                adapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Не используется
            }
        });
        touchHelper.attachToRecyclerView(recyclerView);

        btnCheckAll.setOnClickListener(v -> {
            if (habits == null || habits.isEmpty()) return;

            int validCompletionsCount = 0;
            for (int id : completedToday) {
                for (DatabaseHelper.Habit h : habits) {
                    if (h.getId() == id) {
                        validCompletionsCount++;
                        break;
                    }
                }
            }

            boolean allChecked = validCompletionsCount == habits.size();

            if (allChecked) {
                for (DatabaseHelper.Habit habit : habits) {
                    if (completedToday.contains(habit.getId())) {
                        db.toggleCompletion(habit.getId(), todayDate);
                    }
                }
            } else {
                for (DatabaseHelper.Habit habit : habits) {
                    if (!completedToday.contains(habit.getId())) {
                        db.toggleCompletion(habit.getId(), todayDate);
                    }
                }
            }

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
        new Handler(Looper.getMainLooper()).postDelayed(() -> updateStreakDisplay(), 200);
    }

    public void refreshData() {
        loadData();

        mainHandler.post(() -> {
            adapter.notifyDataSetChanged();
            updateProgress();
            updateCheckAllButtonText();
            updateStreakDisplay();
        });
    }

    private void updateProgress() {
        Log.d("STREAK_DEBUG", "updateProgress: completedToday.size() = " + completedToday.size());
        Log.d("STREAK_DEBUG", "completedToday = " + completedToday.toString());
        if (habits == null || habits.isEmpty()) {
            progressBar.setProgress(0);
            tvProgressPercent.setText("0%");
            return;
        }

        Set<Integer> validCompletions = new HashSet<>(completedToday);
        validCompletions.retainAll(habits.stream().map(h -> h.getId()).collect(Collectors.toSet()));

        int percent = (validCompletions.size() * 100) / habits.size();
        progressBar.setProgress(percent);
        tvProgressPercent.setText(percent + "%");
    }

    private void updateCheckAllButtonText() {
        if (habits == null || habits.isEmpty()) {
            btnCheckAll.setText("Отметить все привычки");
            return;
        }

        int validCompletionsCount = 0;
        for (int id : completedToday) {
            for (DatabaseHelper.Habit h : habits) {
                if (h.getId() == id) {
                    validCompletionsCount++;
                    break;
                }
            }
        }

        if (validCompletionsCount == habits.size()) {
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

        new AlertDialog.Builder(getContext())
                .setTitle("Удалить привычку")
                .setMessage("Как удалить \"" + habit.getTitle() + "\"?")
                .setPositiveButton("Только на сегодня", (dialog, which) -> {
                    db.hideHabitFromDate(habit.getId(), currentDate);
                    refreshData();
                    Toast.makeText(getContext(), "Привычка скрыта с сегодняшнего дня", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Навсегда", (dialog, which) -> {
                    db.deleteHabit(habit.getId());
                    refreshData();
                    Toast.makeText(getContext(), "Привычка удалена навсегда", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void loadData() {
        habits.clear();
        habits.addAll(db.getAllHabits());

        completedToday.clear();
        List<Integer> completions = db.getCompletionsForDate(todayDate);
        completedToday.addAll(completions);
    }

    // Адаптер для списка привычек
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
                android.content.Context wrapper = new android.view.ContextThemeWrapper(
                        v.getContext(), R.style.MyPopupMenuTheme);
                PopupMenu popupMenu = new PopupMenu(wrapper, holder.btnMenu);
                popupMenu.inflate(R.menu.habit_menu);

                try {
                    java.lang.reflect.Field field = popupMenu.getClass().getDeclaredField("mPopup");
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceShowIcon.invoke(menuPopupHelper, true);

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

        // ViewHolder для карточки привычки
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
        // Заглушка для обновления категорий из MainActivity
    }


    private long lastStreakUpdate = 0;
    private static final long STREAK_UPDATE_DELAY = 3000; // 3 секунды

    private void updateStreakDisplay() {
        if (layoutStreak == null) return;

        // Если сегодня нет привычек — не показываем
        if (habits == null || habits.isEmpty()) {
            layoutStreak.setVisibility(View.GONE);
            return;
        }

        new Thread(() -> {
            int userId = new AuthManager(requireContext()).getCurrentUserId();
            int streak = db.calculateStreak(userId);

            requireActivity().runOnUiThread(() -> {
                if (streak > 0) {
                    layoutStreak.setVisibility(View.VISIBLE);
                    tvStreakDays.setText(streak + " " + getDaysString(streak));
                    if (streak >= 30) {
                        tvStreakMessage.setText("Ты 🔥 легенда! Так держать!");
                    } else if (streak >= 14) {
                        tvStreakMessage.setText("2 недели — это мощно! Не останавливайся!");
                    } else if (streak >= 7) {
                        tvStreakMessage.setText("Целая неделя! Ты крут!");
                    } else if (streak >= 3) {
                        tvStreakMessage.setText("Отличная серия! Продолжай!");
                    } else {
                        tvStreakMessage.setText("Уже " + streak + " " + getDaysString(streak) + "! Так держать!");
                    }
                } else {
                    layoutStreak.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private String getDaysString(int days) {
        if (days % 10 == 1 && days % 100 != 11) return "день";
        if (days % 10 >= 2 && days % 10 <= 4 && (days % 100 < 10 || days % 100 >= 20)) return "дня";
        return "дней";
    }
    @Override
    public void onResume() {
        super.onResume();
        // Задержка, чтобы БД успела обновиться
        new Handler(Looper.getMainLooper()).postDelayed(() -> updateStreakDisplay(), 50);
    }

}