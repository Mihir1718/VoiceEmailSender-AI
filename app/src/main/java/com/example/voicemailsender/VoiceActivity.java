package com.example.voicemailsender;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VoiceActivity extends AppCompatActivity {

    private static final int REQ_EMAIL = 101;
    private static final int REQ_SUBJECT = 102;
    private static final int REQ_WORDS = 103;

    private Button btnSpeak;
    private TextView tvStatus;
    private TextToSpeech tts;

    private String recipient = "", subject = "", message = "";
    private int wordCount = 20;

    private final String OPENAI_API_KEY = "AIzaSyDZ7u3gxG-G0iwKVKESqbiws8bq1kmI4jQ"; // Replace with your key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);

        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.app_name2));


        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_team) {
                Intent intent = new Intent(this, AboutTeamActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_app) {
                Intent intent = new Intent(this, AboutAppActivity.class);
                startActivity(intent);
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        btnSpeak = findViewById(R.id.btnSpeak);
        tvStatus = findViewById(R.id.tvVoiceStatus);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS)
                tts.setLanguage(Locale.US);
        });

        btnSpeak.setOnClickListener(v -> {
            promptAndSpeak("To whom do you want to send the email?");
            new Handler().postDelayed(() -> startVoiceInput(REQ_EMAIL), 2500);
        });

        // Bottom Navigation setup
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_voice);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            } else if (id == R.id.nav_voice) {
                return true; // Already here
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
            }
            overridePendingTransition(0, 0);
            return true;
        });

    }

    private void promptAndSpeak(String text) {
        tvStatus.setText(text);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void startVoiceInput(int reqCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        try {
            startActivityForResult(intent, reqCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Speech not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result == null || result.isEmpty()) return;

            String spoken = result.get(0).toLowerCase();

            switch (requestCode) {
                case REQ_EMAIL:
                    recipient = spoken.replace(" at ", "@").replace(" dot ", ".").replaceAll("\\s+", "");
                    promptAndSpeak("What is the subject of the email?");
                    new Handler().postDelayed(() -> startVoiceInput(REQ_SUBJECT), 2500);
                    break;
                case REQ_SUBJECT:
                    subject = spoken;
                    promptAndSpeak("How many words should the message contain?");
                    new Handler().postDelayed(() -> startVoiceInput(REQ_WORDS), 2500);
                    break;
                case REQ_WORDS:
                    try {
                        wordCount = Integer.parseInt(spoken.replaceAll("[^0-9]", ""));
                    } catch (NumberFormatException e) {
                        wordCount = 20;
                    }
                    tvStatus.setText("Generating message using AI...");
                    generateAIMessage(recipient, subject, wordCount);
                    break;
            }
        }
    }

    private void generateAIMessage(String email, String subject, int wordCount) {
        OkHttpClient client = new OkHttpClient();
        String prompt = "Generate a professional email message of approximately " + wordCount + " words about: " + subject;

        JSONObject json = new JSONObject();
        try {
            json.put("model", "gpt-3.5-turbo");
            json.put("messages", new org.json.JSONArray().put(new JSONObject().put("role", "user").put("content", prompt)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> tvStatus.setText("AI request failed."));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> tvStatus.setText("Error: " + response.code()));
                    return;
                }

                try {
                    JSONObject resObj = new JSONObject(response.body().string());
                    message = resObj.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    runOnUiThread(() -> {
                        tvStatus.setText("Email ready! Sending...");
                        sendEmail(email, subject, message);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> tvStatus.setText("Error parsing AI response."));
                }
            }
        });
    }

    private void sendEmail(String to, String subject, String body) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", to, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}