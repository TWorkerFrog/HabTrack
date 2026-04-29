package com.example.habtrack.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habtrack.R;
import com.example.habtrack.auth.AuthManager;
import com.example.habtrack.auth.LoginActivity;
import com.example.habtrack.data.DatabaseHelper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private DatabaseHelper db;
    private AuthManager authManager;
    private RecyclerView rvCategories;
    private CategoriesAdapter categoriesAdapter;
    private List<String> categories = new ArrayList<>();
    private Button btnLogout;
    private LinearLayout layoutCategoriesHeader;
    private LinearLayout layoutCategoriesList;
    private ImageView ivArrow;
    private TextView tvCategoriesCount;
    private boolean isCategoriesExpanded = false;

    // Элементы профиля
    private TextView tvUsername, tvJoinDate, tvTotalStats;
    private LinearLayout layoutChangePassword, layoutDeleteAccount;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        db = DatabaseHelper.getInstance(getContext());
        authManager = new AuthManager(getContext());

        // Инициализация элементов
        rvCategories = view.findViewById(R.id.rv_categories);
        btnLogout = view.findViewById(R.id.btn_logout);
        layoutCategoriesHeader = view.findViewById(R.id.layout_categories_header);
        layoutCategoriesList = view.findViewById(R.id.layout_categories_list);
        ivArrow = view.findViewById(R.id.iv_arrow);
        tvCategoriesCount = view.findViewById(R.id.tv_categories_count);

        tvUsername = view.findViewById(R.id.tv_username);
        tvJoinDate = view.findViewById(R.id.tv_join_date);
        tvTotalStats = view.findViewById(R.id.tv_total_stats);
        layoutChangePassword = view.findViewById(R.id.layout_change_password);
        layoutDeleteAccount = view.findViewById(R.id.layout_delete_account);

        // Заполнение данными
        loadUserInfo();
        loadUserStats();

        // Настройка списка категорий
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        loadCategories();

        categoriesAdapter = new CategoriesAdapter(categories, new CategoriesAdapter.Callbacks() {
            @Override
            public void onEdit(String category, int position) {
                showEditCategoryDialog(category, position);
            }

            @Override
            public void onDelete(String category, int position) {
                showDeleteCategoryDialog(category, position);
            }
        });
        rvCategories.setAdapter(categoriesAdapter);

        // Раскрытие/скрытие списка категорий
        layoutCategoriesHeader.setOnClickListener(v -> toggleCategoriesList());

        // Кнопка выхода
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Выход")
                    .setMessage("Вы уверены, что хотите выйти?")
                    .setPositiveButton("Выйти", (dialog, which) -> {
                        authManager.logout();
                        Intent intent = new Intent(getContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        // Смена пароля
        layoutChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Удаление аккаунта
        layoutDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());

        // Добавление категории
        Button btnAddCategory = view.findViewById(R.id.btn_add_category);
        if (btnAddCategory != null) {
            btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
        }

        return view;
    }

    // Загрузка информации о пользователе
    private void loadUserInfo() {
        int userId = authManager.getCurrentUserId();
        String username = getUsernameById(userId);
        tvUsername.setText(username != null ? username : "Пользователь");

        String joinDate = db.getUserJoinDate(userId);
        tvJoinDate.setText(joinDate != null ? "с " + joinDate : "");
    }

    // Загрузка статистики пользователя
    private void loadUserStats() {
        int userId = authManager.getCurrentUserId();
        int totalHabits = db.getTotalHabitsCount(userId);
        int totalCompletions = db.getTotalCompletionsCount(userId);
        tvTotalStats.setText(totalHabits + " привычек, " + totalCompletions + " выполнено");
    }

    // Получение имени пользователя по ID
    private String getUsernameById(int userId) {
        try {
            SQLiteDatabase database = db.getReadableDatabase();
            Cursor cursor = database.query("users", new String[]{"username"},
                    "user_id = ?", new String[]{String.valueOf(userId)}, null, null, null);
            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                cursor.close();
                return name;
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Пользователь";
    }

    // Диалог смены пароля
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(view);

        EditText etOldPassword = view.findViewById(R.id.et_old_password);
        EditText etNewPassword = view.findViewById(R.id.et_new_password);
        EditText etConfirmPassword = view.findViewById(R.id.et_confirm_password);
        Button btnSave = view.findViewById(R.id.btn_save);

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String oldPass = etOldPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            } else if (newPass.length() < 4) {
                etNewPassword.setError("Пароль должен быть не менее 4 символов");
                Toast.makeText(getContext(), "Пароль должен быть не менее 4 символов", Toast.LENGTH_SHORT).show();
            } else if (newPass.length() > 20) {
                etNewPassword.setError("Пароль не должен превышать 20 символов");
                Toast.makeText(getContext(), "Пароль не должен превышать 20 символов", Toast.LENGTH_SHORT).show();
            } else if (!newPass.equals(confirmPass)) {
                etConfirmPassword.setError("Пароли не совпадают");
                Toast.makeText(getContext(), "Новые пароли не совпадают", Toast.LENGTH_SHORT).show();
            } else {
                int userId = authManager.getCurrentUserId();
                if (db.changePassword(userId, oldPass, newPass)) {
                    Toast.makeText(getContext(), "Пароль успешно изменён", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    etOldPassword.setError("Неверный пароль");
                    Toast.makeText(getContext(), "Неверный старый пароль", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    // Диалог удаления аккаунта
    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Удалить аккаунт")
                .setMessage("Вы уверены? Все привычки, категории и статистика будут удалены безвозвратно.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    int userId = authManager.getCurrentUserId();
                    if (db.deleteAccount(userId)) {
                        authManager.logout();
                        Intent intent = new Intent(getContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                        Toast.makeText(getContext(), "Аккаунт удалён", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // Раскрытие/скрытие списка категорий
    private void toggleCategoriesList() {
        if (isCategoriesExpanded) {
            layoutCategoriesList.setVisibility(View.GONE);
            ivArrow.setImageResource(R.drawable.ic_chevron_right);
        } else {
            layoutCategoriesList.setVisibility(View.VISIBLE);
            ivArrow.setImageResource(R.drawable.ic_chevron_down);
            loadCategories();
            categoriesAdapter.notifyDataSetChanged();
        }
        isCategoriesExpanded = !isCategoriesExpanded;
    }

    // Загрузка категорий из БД
    private void loadCategories() {
        categories.clear();
        List<String> userCategories = db.getUserCategories();
        for (String cat : userCategories) {
            if (!cat.equals("Без категории")) {
                categories.add(cat);
            }
        }
        tvCategoriesCount.setText(String.valueOf(categories.size()));
    }

    // Диалог добавления категории
    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
        builder.setView(view);

        EditText etCategory = view.findViewById(R.id.dialog_category_et);
        Button btnSave = view.findViewById(R.id.dialog_category_btn);

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String newCategory = etCategory.getText().toString().trim();
            if (!newCategory.isEmpty() && !categories.contains(newCategory)) {
                db.addCategory(newCategory);
                loadCategories();
                categoriesAdapter.notifyDataSetChanged();
                dialog.dismiss();
                Toast.makeText(getContext(), "Категория добавлена", Toast.LENGTH_SHORT).show();
            } else if (newCategory.isEmpty()) {
                etCategory.setError("Введите название");
            } else {
                Toast.makeText(getContext(), "Такая категория уже есть", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    // Диалог редактирования категории
    private void showEditCategoryDialog(String oldName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_category, null);
        builder.setView(view);

        EditText etCategory = view.findViewById(R.id.dialog_et_category);
        Button btnSave = view.findViewById(R.id.dialog_btn_save);

        etCategory.setText(oldName);
        etCategory.setSelection(oldName.length());

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String newName = etCategory.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(oldName)) {
                db.updateCategory(oldName, newName);
                loadCategories();
                categoriesAdapter.notifyItemChanged(position);
                dialog.dismiss();
                Toast.makeText(getContext(), "Категория переименована", Toast.LENGTH_SHORT).show();
            } else if (newName.isEmpty()) {
                etCategory.setError("Введите название");
            } else {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // Диалог удаления категории
    private void showDeleteCategoryDialog(String category, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Удалить категорию")
                .setMessage("Удалить категорию \"" + category + "\"?\nПривычки с этой категорией будут перемещены в 'Без категории'.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    db.deleteCategory(category);
                    loadCategories();
                    categoriesAdapter.notifyItemRemoved(position);
                    Toast.makeText(getContext(), "Категория удалена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // Адаптер для списка категорий
    static class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.ViewHolder> {
        private final List<String> categories;
        private final Callbacks callbacks;

        interface Callbacks {
            void onEdit(String category, int position);
            void onDelete(String category, int position);
        }

        CategoriesAdapter(List<String> categories, Callbacks callbacks) {
            this.categories = categories;
            this.callbacks = callbacks;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String category = categories.get(position);
            holder.tvCategoryName.setText(category);

            holder.btnMenu.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), holder.btnMenu);
                popupMenu.inflate(R.menu.habit_menu);

                popupMenu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_edit) {
                        callbacks.onEdit(category, position);
                        return true;
                    } else if (itemId == R.id.menu_delete) {
                        callbacks.onDelete(category, position);
                        return true;
                    }
                    return false;
                });
                popupMenu.show();
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCategoryName;
            ImageButton btnMenu;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCategoryName = itemView.findViewById(R.id.tv_category_name);
                btnMenu = itemView.findViewById(R.id.btn_category_menu);
            }
        }
    }
}