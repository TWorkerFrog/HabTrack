package com.example.habtrack.ui;

import android.app.AlertDialog;
import android.os.Bundle;
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;

public class ProfileFragment extends Fragment {

    private RecyclerView rvCategories;
    private Button btnAddCategory;
    private LinearLayout layoutCategoriesList;
    private LinearLayout layoutCategoriesHeader;
    private ImageView ivArrow;
    private TextView tvCategoriesCount;
    private DatabaseHelper db;
    private CategoryAdapter adapter;
    private List<String> categories = new ArrayList<>();
    private boolean isExpanded = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        db = DatabaseHelper.getInstance(getContext());

        rvCategories = view.findViewById(R.id.rv_categories);
        btnAddCategory = view.findViewById(R.id.btn_add_category);
        layoutCategoriesList = view.findViewById(R.id.layout_categories_list);
        layoutCategoriesHeader = view.findViewById(R.id.layout_categories_header);
        ivArrow = view.findViewById(R.id.iv_arrow);
        tvCategoriesCount = view.findViewById(R.id.tv_categories_count);

        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));

        loadCategories();

        layoutCategoriesHeader.setOnClickListener(v -> toggleCategoriesList());

        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        return view;
    }

    private void toggleCategoriesList() {
        if (isExpanded) {
            layoutCategoriesList.setVisibility(View.GONE);
            ivArrow.setImageResource(R.drawable.ic_chevron_right);
        } else {
            layoutCategoriesList.setVisibility(View.VISIBLE);
            ivArrow.setImageResource(R.drawable.ic_chevron_down);
        }
        isExpanded = !isExpanded;
    }

    private void loadCategories() {
        categories.clear();
        List<String> allCategories = db.getUserCategories();
        for (String cat : allCategories) {
            if (!cat.equals("Без категории")) {
                categories.add(cat);
            }
        }
        tvCategoriesCount.setText(String.valueOf(categories.size()));
        adapter = new CategoryAdapter(categories, new CategoryAdapter.Callbacks() {
            @Override
            public void onEdit(String category, int position) {
                showEditCategoryDialog(category, position);
            }
            @Override
            public void onDelete(String category, int position) {
                showDeleteCategoryDialog(category, position);
            }
        });
        rvCategories.setAdapter(adapter);
    }

    private void showEditCategoryDialog(String oldCategory, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_category, null);
        builder.setView(view);

        EditText etName = view.findViewById(R.id.dialog_et_category);
        Button btnSave = view.findViewById(R.id.dialog_btn_save);

        etName.setText(oldCategory);
        etName.setSelection(oldCategory.length());

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String newCategory = etName.getText().toString().trim();
            if (newCategory.isEmpty()) {
                etName.setError("Введите название");
                return;
            }
            if (newCategory.equals(oldCategory)) {
                dialog.dismiss();
                return;
            }
            if (categories.contains(newCategory)) {
                etName.setError("Такая категория уже есть");
                return;
            }

            db.updateCategory(oldCategory, newCategory);
            loadCategories();
            dialog.dismiss();
            Toast.makeText(getContext(), "Категория переименована", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showDeleteCategoryDialog(String category, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Удалить категорию")
                .setMessage("Вы уверены, что хотите удалить категорию \"" + category + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    db.deleteCategory(category);
                    loadCategories();
                    Toast.makeText(getContext(), "Категория удалена", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

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
                if (!isExpanded) {
                    toggleCategoriesList();
                }
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

    // Адаптер для категорий
    static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final List<String> categories;
        private final Callbacks callbacks;

        interface Callbacks {
            void onEdit(String category, int position);
            void onDelete(String category, int position);
        }

        CategoryAdapter(List<String> categories, Callbacks callbacks) {
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
            holder.tvName.setText(category);

            holder.btnMenu.setOnClickListener(v -> {
                // Используем wrapper с твоей темой
                android.content.Context wrapper = new android.view.ContextThemeWrapper(
                        v.getContext(),
                        R.style.MyPopupMenuTheme
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

                    // Устанавливаем фон
                    Method setBackgroundDrawable = classPopupHelper.getMethod("setBackgroundDrawable", android.graphics.drawable.Drawable.class);
                    android.graphics.drawable.Drawable drawable = v.getContext().getResources().getDrawable(R.drawable.popup_menu_background);
                    setBackgroundDrawable.invoke(menuPopupHelper, drawable);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                popupMenu.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_edit) {
                        if (callbacks != null) callbacks.onEdit(category, position);
                        return true;
                    } else if (itemId == R.id.menu_delete) {
                        if (callbacks != null) callbacks.onDelete(category, position);
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
            TextView tvName;
            ImageButton btnMenu;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_category_name);
                btnMenu = itemView.findViewById(R.id.btn_category_menu);
            }
        }
    }
}