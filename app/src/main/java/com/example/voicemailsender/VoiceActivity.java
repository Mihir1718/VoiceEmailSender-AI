package com.example.voicemailsender;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import okhttp3.*;

public class VoiceActivity extends AppCompatActivity {

    private static final int REQ_EMAIL = 100;
    private static final int REQ_SUBJECT = 101;
    private static final int REQ_WORDS = 102;
    private static final int REQ_GREETING = 103;
    private static final int REQ_CLOSING = 104;
    private static final int REQ_ATTACHMENT_CONFIRM = 105;
    private static final int REQ_ATTACHMENT_PATH = 106;

    private TextToSpeech tts;
    private TextView txtTo, txtSubject, txtMessage;
    private ImageButton btnSpeak;

    private String email = "", subject = "", message = "";
    private String greeting = "", closing = "", attachmentPath = "";
    private int wordCount = 50;
    private boolean isAttachmentRequired = false;

    private final String GEMINI_API_KEY = "YOUR_API_KEY_HERE"; // 🔒 Replace with your real API key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.nav_app) {
                startActivity(new Intent(this, AboutAppActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        txtTo = findViewById(R.id.txtTo);
        txtSubject = findViewById(R.id.txtSubject);
        txtMessage = findViewById(R.id.txtMessage);
        btnSpeak = findViewById(R.id.voiceButton);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

        btnSpeak.setOnClickListener(v -> promptEmail());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_voice);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_voice) {
                return true;
            } else if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish(); // ✅ THIS IS REQUIRED
                return true;
            } else if (itemId == R.id.nav_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                finish(); // ✅ THIS TOO
                return true;
            }
            return false;
        });


    }

    private void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void promptEmail() {
        speak("Please say the recipient's email address.");
        new Handler().postDelayed(() -> listen(REQ_EMAIL), 3000);
    }

    private void promptSubject() {
        speak("Now say the subject of the email.");
        new Handler().postDelayed(() -> listen(REQ_SUBJECT), 3000);
    }

    private void promptWordCount() {
        speak("How many words should the message be?");
        new Handler().postDelayed(() -> listen(REQ_WORDS), 3000);
    }

    private void promptGreeting() {
        speak("Please say the greeting like Dear or Respected Sir or Maam.");
        new Handler().postDelayed(() -> listen(REQ_GREETING), 3000);
    }

    private void promptClosing() {
        speak("Please say the closing like Sincerely or Thankfully.");
        new Handler().postDelayed(() -> listen(REQ_CLOSING), 3000);
    }

    private void promptAttachmentConfirmation() {
        speak("Is there any attachment? Please say Yes or No.");
        new Handler().postDelayed(() -> listen(REQ_ATTACHMENT_CONFIRM), 3000);
    }

    private void promptAttachmentPath() {
        speak("Please say or paste the full path of the file to attach.");
        new Handler().postDelayed(() -> listen(REQ_ATTACHMENT_PATH), 3000);
    }

    private void listen(int requestCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(this, "Speech not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result == null || result.isEmpty()) return;

            switch (requestCode) {
                case REQ_EMAIL:
                    email = result.get(0)
                            .replaceAll(" ", "")
                            .replace("attherate", "@")
                            .replace("at the rate", "@")
                            .replace("dot", ".");
                    if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        txtTo.setText("To: " + email);
                        promptSubject();
                    } else {
                        txtTo.setText("Invalid email: " + email);
                        speak("That doesn't seem like a valid email. Please try again.");
                        new Handler().postDelayed(() -> listen(REQ_EMAIL), 4000);
                    }
                    break;
                case REQ_SUBJECT:
                    subject = result.get(0);
                    txtSubject.setText("Subject: " + subject);
                    promptWordCount();
                    break;
                case REQ_WORDS:
                    try {
                        wordCount = Integer.parseInt(result.get(0).replaceAll("[^0-9]", ""));
                    } catch (Exception e) {
                        wordCount = 50;
                    }
                    promptGreeting();
                    break;
                case REQ_GREETING:
                    greeting = result.get(0).trim();
                    promptClosing();
                    break;
                case REQ_CLOSING:
                    closing = result.get(0).trim();
                    promptAttachmentConfirmation();
                    break;
                case REQ_ATTACHMENT_CONFIRM:
                    String confirmation = result.get(0).toLowerCase();
                    if (confirmation.contains("yes")) {
                        isAttachmentRequired = true;
                        promptAttachmentPath();
                    } else {
                        isAttachmentRequired = false;
                        fetchAIMessage();
                    }
                    break;
                case REQ_ATTACHMENT_PATH:
                    attachmentPath = result.get(0).trim();
                    fetchAIMessage();
                    break;
            }
        }
    }

    private void fetchAIMessage() {
        speak("Generating your email using Gemini AI. Please wait.");
        OkHttpClient client = new OkHttpClient();

        String prompt = "Write a professional email to " + email +
                " on the subject: " + subject +
                " in about " + wordCount + " words. " +
                "Start the email with the greeting: '" + greeting +
                "' and end with the closing: '" + closing + "'.";

        String jsonBody = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" + prompt + "\" }] }] }";

        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    speak("Failed to connect to Gemini.");
                    txtMessage.setText("Connection Error: " + e.getMessage());
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONObject obj = new JSONObject(responseData);
                    JSONArray parts = obj.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts");

                    message = parts.getJSONObject(0).getString("text");

                    runOnUiThread(() -> {
                        txtMessage.setText("Message:\n" + message);
                        speak("Email is ready. It will be sent in 15 seconds.");
                        new Handler().postDelayed(VoiceActivity.this::sendEmail, 15000);
                    });

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        speak("Error reading AI response.");
                        txtMessage.setText("Parsing Error: " + e.getMessage() + "\nRaw:\n" + responseData);
                    });
                }
            }
        });
    }

    private void sendEmail() {
        if (email != null && subject != null && message != null) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account == null) {
                Toast.makeText(this, "User not signed in with Google", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Sending email via Gmail API...", Toast.LENGTH_SHORT).show();
            GmailSender.sendEmail(this, account, email, subject, message);
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
