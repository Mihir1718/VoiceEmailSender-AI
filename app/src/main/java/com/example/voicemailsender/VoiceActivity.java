package com.example.voicemailsender;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
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
    private Button btnSpeak;

    private String email = "", subject = "", message = "";
    private String greeting = "", closing = "", attachmentPath = "";
    private int wordCount = 50;
    private boolean isAttachmentRequired = false;

    private final String GEMINI_API_KEY = "AIzaSyBAbHtk5n-mtcQfPOsclzS9hlNFoqc1fks";

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

        txtTo = findViewById(R.id.txtTo);
        txtSubject = findViewById(R.id.txtSubject);
        txtMessage = findViewById(R.id.txtMessage);
        btnSpeak = findViewById(R.id.btnSpeak);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS)
                tts.setLanguage(Locale.US);
        });

        btnSpeak.setOnClickListener(v -> promptEmail());
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_voice);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_voice) {
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });



    }
    private void shareAppLink() {
        String shareBody = "Check out this amazing Voice Email Sender app:\n\n" +
                "https://drive.google.com/file/d/your_apk_file_id/view?usp=sharing"; // 🔁 Replace with your APK link

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Voice Email Sender App");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Share App via"));
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
        speak("Please say the greeting like Dear Mihir or Respected Sir.");
        new Handler().postDelayed(() -> listen(REQ_GREETING), 3000);
    }

    private void promptClosing() {
        speak("Please say the closing like Sincerely Mihir or Thankfully Dip.");
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
            Toast.makeText(getApplicationContext(), "Speech not supported", Toast.LENGTH_SHORT).show();
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

                    // Validate email using Android's built-in pattern
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
//greeting
                case REQ_GREETING:
                    greeting = result.get(0).trim();
                    promptClosing();
                    break;
//closing
                case REQ_CLOSING:
                    closing = result.get(0).trim();
                    promptAttachmentConfirmation();
                    break;
// attachment
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
                Log.d("GEMINI_RESPONSE", responseData);

                try {
                    JSONObject obj = new JSONObject(responseData);
                    if (obj.has("error")) {
                        String errorMsg = obj.getJSONObject("error").optString("message", "Unknown error");
                        throw new Exception("Gemini Error: " + errorMsg);
                    }

                    JSONArray candidates = obj.getJSONArray("candidates");
                    JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
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
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("*/*");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);

        if (isAttachmentRequired && !attachmentPath.isEmpty()) {
            File file = new File(attachmentPath);
            if (file.exists()) {
                Uri fileUri = Uri.fromFile(file);
                emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            } else {
                Toast.makeText(this, "Attachment file not found at provided path.", Toast.LENGTH_SHORT).show();
            }
        }

        emailIntent.setPackage("com.google.android.gm");

        try {
            startActivity(emailIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Gmail app not installed", Toast.LENGTH_SHORT).show();
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
