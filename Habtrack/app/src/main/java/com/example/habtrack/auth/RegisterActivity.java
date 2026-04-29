package com.example.habtrack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.habtrack.R;
import com.example.habtrack.data.DatabaseHelper;
import com.example.habtrack.MainActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etLogin, etPassword, etConfirm;
    private Button btnRegister;
    private TextView  btnBackToLogin;
    private TextView tvError;
    private DatabaseHelper db;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = DatabaseHelper.getInstance(this);
        authManager = new AuthManager(this);

        etLogin = findViewById(R.id.et_register_login);
        etPassword = findViewById(R.id.et_register_password);
        etConfirm = findViewById(R.id.et_register_confirm);
        btnRegister = findViewById(R.id.btn_register);
        btnBackToLogin = findViewById(R.id.btn_back_to_login);
        tvError = findViewById(R.id.tv_error);

        btnRegister.setOnClickListener(v -> attemptRegister());
        btnBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister() {
        String login = etLogin.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(login)) {
            showError("Введите логин");
            return;
        }

        if (login.length() < 3) {
            showError("Логин должен быть не менее 3 символов");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError("Введите пароль");
            return;
        }

        if (password.length() < 3) {
            showError("Пароль должен быть не менее 3 символов");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Пароли не совпадают");
            return;
        }

        btnRegister.setEnabled(false);

        if (db.register(login, password)) {
            // Автоматически логиним после регистрации
            if (db.login(login, password)) {
                int userId = db.getUserIdByUsername(login);
                authManager.saveUser(userId);
                startMainActivity();
            } else {
                btnRegister.setEnabled(true);
                showError("Ошибка при входе после регистрации");
            }
        } else {
            btnRegister.setEnabled(true);
            showError("Пользователь с таким логином уже существует");
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void startMainActivity() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}