package com.socialChat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lorenzogadotti.socialchat.R;

public class SettingsActivity extends AppCompatActivity {

    private EditText etPromptSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etPromptSettings = findViewById(R.id.etPromptSettings);
        Button btnSavePrompt = findViewById(R.id.btnSavePrompt);

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        long userId = prefs.getLong("userId", -1);


        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        UserDatabaseHelper userDb = new UserDatabaseHelper(this);
        User user = userDb.getUserById(userId);


        if (user != null && user.systemPrompt != null) {
            etPromptSettings.setText(user.systemPrompt);
        }

        if (user == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnSavePrompt.setOnClickListener(v -> {
            String newPrompt = etPromptSettings.getText().toString().trim();
            userDb.updatePrompt(userId, newPrompt);
            Toast.makeText(this, "Prompt updated", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
