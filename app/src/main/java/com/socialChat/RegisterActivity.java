package com.socialChat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lorenzogadotti.socialchat.R;

public class RegisterActivity extends AppCompatActivity {

    private UserDatabaseHelper userDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userDb = new UserDatabaseHelper(this);

        EditText etUser = findViewById(R.id.etUsername);
        EditText etPass = findViewById(R.id.etPassword);
        EditText etPrompt = findViewById(R.id.etPrompt);
        EditText etParentPass = findViewById(R.id.etParentPassword);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String u = etUser.getText().toString().trim();
            String p = etPass.getText().toString();
            String prompt = etPrompt.getText().toString().trim();
            String parentPass = etParentPass.getText().toString();

            if (u.isEmpty() || p.isEmpty() || parentPass.isEmpty()) {
                Toast.makeText(this, "Fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            long id = userDb.addUser(u, p, prompt, parentPass);
            if (id == -1) {
                Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
            } else {
                getSharedPreferences("auth", MODE_PRIVATE)
                        .edit()
                        .putLong("userId", id)
                        .apply();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }
}
