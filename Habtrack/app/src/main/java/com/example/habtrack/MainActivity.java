package com.example.habtrack;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.example.habtrack.data.DatabaseHelper;
import com.example.habtrack.ui.HabitsFragment;
import com.example.habtrack.ui.CalendarFragment;
import com.example.habtrack.ui.StatsFragment;
import com.example.habtrack.ui.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private HabitsFragment habitsFragment;
    private List<String> categories = new ArrayList<>();
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Загружаем категории из БД
        loadCategoriesFromDb();

        habitsFragment = new HabitsFragment();

        // Загружаем начальный фрагмент
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, habitsFragment)
                    .commit();
        }

        // Настройка нижней навигации
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_habits) {
                selectedFragment = habitsFragment;
            } else if (itemId == R.id.nav_calendar) {
                selectedFragment = new CalendarFragment();
            } else if (itemId == R.id.nav_stats) {
                selectedFragment = new StatsFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }

    private void loadCategoriesFromDb() {
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        categories = db.getUserCategories();
        // Убираем "Без категории" из основного списка для логики
        categories.remove("Без категории");
    }

    public void refreshCategories() {
        loadCategoriesFromDb();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            showAddHabitDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddHabitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_habit, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.dialog_et_name);
        Spinner spinnerCategory = view.findViewById(R.id.dialog_spinner_category);
        TextView tvAddCategory = view.findViewById(R.id.dialog_tv_add_category);
        Button btnCreate = view.findViewById(R.id.dialog_btn_create);

        List<String> displayCategories = new ArrayList<>();
        displayCategories.add("Без категории");
        for (String cat : categories) {
            if (!cat.equals("Без категории")) {
                displayCategories.add(cat);
            }
        }

        // Адаптер с читаемым текстом
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                displayCategories
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

        AlertDialog dialog = builder.create();

        tvAddCategory.setOnClickListener(v -> {
            showAddCategoryDialog(adapter, spinnerCategory, displayCategories);
        });

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();

            if (name.isEmpty()) {
                etName.setError("Введите название");
                return;
            }

            String finalCategory = category.equals("Без категории") ? "Без категории" : category;
            habitsFragment.addHabit(name, finalCategory);
            dialog.dismiss();
            Toast.makeText(this, "Привычка добавлена", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showAddCategoryDialog(ArrayAdapter<String> adapter, Spinner spinner, List<String> displayCategories) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
        builder.setView(view);

        EditText etCategory = view.findViewById(R.id.dialog_category_et);
        Button btnSave = view.findViewById(R.id.dialog_category_btn);

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String newCategory = etCategory.getText().toString().trim();
            if (!newCategory.isEmpty() && !categories.contains(newCategory)) {
                // Сохраняем категорию в БД
                DatabaseHelper db = DatabaseHelper.getInstance(this);
                db.addCategory(newCategory);

                // ← ОБНОВЛЯЕМ ЛОКАЛЬНЫЙ СПИСОК
                categories.clear();
                categories.addAll(db.getUserCategories());
                categories.remove("Без категории");

                // Обновляем displayCategories
                displayCategories.clear();
                displayCategories.add("Без категории");
                displayCategories.addAll(categories);

                adapter.notifyDataSetChanged();
                spinner.setSelection(displayCategories.size() - 1);

                // ← ОБНОВЛЯЕМ КАТЕГОРИИ В HABITSFRAGMENT
                if (habitsFragment != null) {
                    habitsFragment.refreshCategories();
                }

                dialog.dismiss();
                Toast.makeText(this, "Категория добавлена", Toast.LENGTH_SHORT).show();
            } else if (newCategory.isEmpty()) {
                etCategory.setError("Введите название");
            } else {
                Toast.makeText(this, "Такая категория уже есть", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}