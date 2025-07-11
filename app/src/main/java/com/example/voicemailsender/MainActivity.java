package com.example.voicemailsender;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Patterns;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button btnSpeak, btnSendEmail;
    private TextView tvEmail, tvSubject, tvMessage;

    private int step = 0;
    private String email = "", subject = "", message = "";
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Initialize views AFTER setContentView
        tvEmail = findViewById(R.id.tvEmail);
        tvSubject = findViewById(R.id.tvSubject);
        tvMessage = findViewById(R.id.tvMessage);
        ImageButton btnSpeak = findViewById(R.id.btnSpeak);
        ImageButton btnSendEmail = findViewById(R.id.btnSendEmail);


        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_app) {
                Intent intent = new Intent(this, AboutAppActivity.class);
                startActivity(intent);
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        tvEmail.setText("To: ");
        tvSubject.setText("Subject: ");
        tvMessage.setText("");

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

        btnSpeak.setOnClickListener(view -> {
            step = 0;
            promptAndSpeak();
        });

        btnSendEmail.setOnClickListener(view -> {
            if (!email.isEmpty() && !subject.isEmpty() && !message.isEmpty()) {
                if (isValidEmail(email)) {
                    sendEmail();
                } else {
                    Toast.makeText(this, "Invalid email address.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please complete voice input first.", Toast.LENGTH_SHORT).show();
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_voice) {
                startActivity(new Intent(this, VoiceActivity.class));
                finish(); // ✅ Prevent Voice and Main overlapping
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });



    }

    private void promptAndSpeak() {
        String prompt;
        switch (step) {
            case 0:
                prompt = "To whom do you want to send email?";
                break;
            case 1:
                prompt = "What is the subject of the email?";
                break;
            case 2:
                prompt = "What message do you want to send?";
                break;
            default:
                prompt = "";
        }

        tts.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, null);
        new Handler().postDelayed(this::startVoiceInput, 2000);
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                step == 0 ? "Speak Email ID" :
                        step == 1 ? "Speak Subject" :
                                "Speak Message");

        try {
            startActivityForResult(intent, 100);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                switch (step) {
                    case 0:
                        email = result.get(0).toLowerCase().replaceAll("\\s+", "").replace("attherate", "@").replace("dot", ".").replace("mirror","miral" +
                                "" +
                                "" +
                                "");
                        Toast.makeText(this, "Email: " + email, Toast.LENGTH_SHORT).show();
                        step++;
                        promptAndSpeak();
                        break;
                    case 1:
                        subject = result.get(0);
                        Toast.makeText(this, "Subject: " + subject, Toast.LENGTH_SHORT).show();
                        step++;
                        promptAndSpeak();
                        break;
                    case 2:
                        message = result.get(0);
                        Toast.makeText(this, "Message: " + message, Toast.LENGTH_SHORT).show();
                        break;
                }
                tvEmail.setText("To: " + email);
                tvSubject.setText("Subject: " + subject);
                tvMessage.setText(message);
            }
        }
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void sendEmail() {
        if (email != null && subject != null && message != null) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

            if (account == null) {
                Toast.makeText(this, "User not signed in with Google", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Sending email via Gmail API...", Toast.LENGTH_SHORT).show();
            GmailSender.sendEmail(MainActivity.this, account, email, subject, message);
        } else {
            Toast.makeText(this, "Please complete voice input first.", Toast.LENGTH_SHORT).show();
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
