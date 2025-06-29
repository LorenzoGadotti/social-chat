package com.socialChat;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.lorenzogadotti.socialchat.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText editTextMessage;
    private Button buttonSend, buttonClear;
    private LinearLayout layoutMessages;
    private ScrollView scrollViewChat;

    private MessageDatabaseHelper dbHelper;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private boolean isFirstInput = true;
    private String systemPrompt = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        long userId = prefs.getLong("userId", -1);

        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextMessage  = findViewById(R.id.editTextMessage);
        buttonSend       = findViewById(R.id.buttonSend);
        buttonClear      = findViewById(R.id.buttonClear);
        layoutMessages   = findViewById(R.id.layoutMessages);
        scrollViewChat   = findViewById(R.id.scrollViewChat);

        setupActionButtons();

        dbHelper = new MessageDatabaseHelper(this);

        setupEditTextAnimation();
        setupButtons();

        loadMessages();
    }

    private void setupActionButtons() {
        findViewById(R.id.btnSettings).setOnClickListener(v -> solicitarSenhaPai());

        findViewById(R.id.btnLogout).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Do you really want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    getSharedPreferences("auth", MODE_PRIVATE)
                            .edit()
                            .remove("userId")
                            .apply();

                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show());
    }

    private void solicitarSenhaPai() {
        EditText campoSenha = new EditText(this);
        campoSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("Responsible person's password")
                .setMessage("Enter password to change prompt.")
                .setView(campoSenha)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String senhaDigitada = campoSenha.getText().toString();

                    SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
                    long userId = prefs.getLong("userId", -1);

                    if (userId != -1) {
                        UserDatabaseHelper udb = new UserDatabaseHelper(this);
                        User usuario = udb.getUserById(userId);

                        if (usuario != null && org.mindrot.jbcrypt.BCrypt.checkpw(senhaDigitada, usuario.parentPassword)) {
                            startActivity(new Intent(this, SettingsActivity.class));
                        } else {
                            Toast.makeText(this, "Incorrect password!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        long userId = prefs.getLong("userId", -1);
        if (userId != -1) {
            UserDatabaseHelper udb = new UserDatabaseHelper(this);
            User u = udb.getUserById(userId);
            if (u != null && u.systemPrompt != null) {
                systemPrompt = u.systemPrompt;
            }
        }
    }

    private void setupButtons() {
        buttonSend.setOnClickListener(v -> {
            String text = editTextMessage.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter a message!", Toast.LENGTH_SHORT).show();
                return;
            }
            animateSendButton();
            saveUserMessageAndSendApi(text);
            editTextMessage.setText("");
        });

        buttonClear.setOnClickListener(v -> new
                AlertDialog.Builder(MainActivity.this)
                .setTitle("Clear Chat")
                .setMessage("Are you sure you want to clear all messages?")
                .setPositiveButton("Clear", (d, w) -> {
                    dbHelper.clearAllMessages();
                    loadMessages();
                    Toast.makeText(MainActivity.this, "Chat cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show());
    }

    private void setupEditTextAnimation() {
        editTextMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && isFirstInput) {
                isFirstInput = false;
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(editTextMessage, "scaleX", 1.0f, 1.05f, 1.0f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(editTextMessage, "scaleY", 1.0f, 1.05f, 1.0f);
                ObjectAnimator alpha = ObjectAnimator.ofFloat(editTextMessage, "alpha", 0.7f, 1.0f);

                scaleX.setDuration(300);
                scaleY.setDuration(300);
                alpha.setDuration(300);

                scaleX.setInterpolator(new OvershootInterpolator());
                scaleY.setInterpolator(new OvershootInterpolator());

                scaleX.start();
                scaleY.start();
                alpha.start();
            }
        });
    }

    private void animateSendButton() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(buttonSend, "scaleX", 1.0f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(buttonSend, "scaleY", 1.0f, 1.2f, 1.0f);

        scaleX.setDuration(200);
        scaleY.setDuration(200);

        scaleX.setInterpolator(new OvershootInterpolator());
        scaleY.setInterpolator(new OvershootInterpolator());

        scaleX.start();
        scaleY.start();
    }

    private void saveUserMessageAndSendApi(String userText) {
        Message m = new Message(0, userText, true, System.currentTimeMillis());
        dbHelper.addMessage(m);
        loadMessages();

        executor.execute(() -> {
            String apiResponse = ApiService.getGeminiResponse(systemPrompt, userText);

            runOnUiThread(() -> {
                try {
                    JSONObject json = new JSONObject(apiResponse);
                    if (json.has("error")) { showErrorToast(json.getString("error")); return; }

                    String botReply = json.optString("text", "No response from bot");
                    dbHelper.addMessage(new Message(0, botReply, false, System.currentTimeMillis()));

                    loadMessages();
                } catch (JSONException e) { showErrorToast("Error parsing response"); }
            });
        });
    }

    private void showErrorToast(String msg) { Toast.makeText(this, "Error: " + msg, Toast.LENGTH_LONG).show(); }

    private void loadMessages() {
        layoutMessages.removeAllViews();
        List<Message> messages = dbHelper.getAllMessages();

        for (Message msg : messages) {
            TextView textView = new TextView(this);

            GradientDrawable bgDrawable = new GradientDrawable();
            bgDrawable.setCornerRadii(msg.isUser ?
                    new float[]{30, 30, 8, 30, 30, 30, 30, 8} :
                    new float[]{8, 30, 30, 30, 30, 8, 30, 30});
            bgDrawable.setColor(msg.isUser ?
                    ContextCompat.getColor(this, R.color.user_message) :
                    ContextCompat.getColor(this, R.color.bot_message));
            textView.setBackground(bgDrawable);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.75),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 8, 8, 8);
            params.gravity = msg.isUser ? Gravity.END : Gravity.START;

            textView.setLayoutParams(params);
            textView.setTextColor(Color.WHITE);
            textView.setPadding(24, 16, 24, 16);
            textView.setText(msg.content);
            textView.setTextSize(16);
            textView.setMaxLines(20);

            textView.setAlpha(0f);
            textView.setTranslationY(50f);
            layoutMessages.addView(textView);
            textView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start();
        }

        scrollViewChat.post(() -> scrollViewChat.fullScroll(ScrollView.FOCUS_DOWN));
    }
}