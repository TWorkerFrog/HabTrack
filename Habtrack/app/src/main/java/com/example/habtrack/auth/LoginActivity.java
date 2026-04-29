package com.example.habtrack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.habtrack.MainActivity;
import com.example.habtrack.R;
import com.example.habtrack.data.DatabaseHelper;



public class LoginActivity extends AppCompatActivity {

    private EditText etLogin, etPassword;
    private Button btnLogin;
    private TextView btnGoToRegister;
    private TextView tvError;
    private DatabaseHelper db;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = DatabaseHelper.getInstance(this);
        authManager = new AuthManager(this);

        // Проверяем, не залогинен ли уже пользователь
        if (authManager.isLoggedIn()) {
            startMainActivity();
            return;
        }

        etLogin = findViewById(R.id.et_login);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoToRegister = findViewById(R.id.btn_go_to_register);
        tvError = findViewById(R.id.tv_error);

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void attemptLogin() {
        String login = etLogin.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(login)) {
            showError("Введите логин");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError("Введите пароль");
            return;
        }

        btnLogin.setEnabled(false);

        if (db.login(login, password)) {
            // Получаем ID пользователя по логину
            int userId = db.getUserIdByUsername(login);
            authManager.saveUser(userId);
            startMainActivity();
        } else {
            btnLogin.setEnabled(true);
            showError("Неверный логин или пароль");
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}