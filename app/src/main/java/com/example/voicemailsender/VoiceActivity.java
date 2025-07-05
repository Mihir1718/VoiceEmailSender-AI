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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import okhttp3.*;

public class VoiceActivity extends AppCompatActivity {

    private static final int REQ_EMAIL = 101;
    private static final int REQ_SUBJECT = 102;
    private static final int REQ_WORDS = 103;

    private TextToSpeech tts;
    private TextView txtSubject, txtMessage, txtTo;
    private Button btnStart;

    private String email = "", subject = "", message = "";
    private int wordCount = 50;

    private final String GEMINI_API_KEY = "AIzaSyBAbHtk5n-mtcQfPOsclzS9hlNFoqc1fks";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);

        txtTo = findViewById(R.id.txtTo);
        txtSubject = findViewById(R.id.txtSubject);
        txtMessage = findViewById(R.id.txtMessage);
        btnStart = findViewById(R.id.btnSpeak);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS)
                tts.setLanguage(Locale.US);
        });

        btnStart.setOnClickListener(v -> promptEmail());
    }

    private void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void promptEmail() {
        speak("Please say the recipient's email address.");
        new Handler().postDelayed(this::startVoiceRecognitionEmail, 3000);
    }

    private void startVoiceRecognitionEmail() {
        listen(REQ_EMAIL);
    }

    private void promptSubject() {
        speak("Now, say the subject of your email.");
        new Handler().postDelayed(() -> listen(REQ_SUBJECT), 3000);
    }

    private void promptWordCount() {
        speak("How many words should the message be?");
        new Handler().postDelayed(() -> listen(REQ_WORDS), 3000);
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
            if (result == null || result.size() == 0) return;

            switch (requestCode) {
                case REQ_EMAIL:
                    email = result.get(0).replaceAll(" ", "").replace("at", "@").replace("dot", ".");
                    txtTo.setText("To: " + email);
                    promptSubject();
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
                    fetchAIMessage();
                    break;
            }
        }
    }

    private void fetchAIMessage() {
        speak("Generating your email using Gemini AI. Please wait.");
        OkHttpClient client = new OkHttpClient();

        String prompt = "Write an email on the subject: " + subject + " in about " + wordCount + " words.";
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
                int statusCode = response.code();
                Log.e("GEMINI_RESPONSE_CODE", "HTTP " + statusCode);
                Log.e("GEMINI_RESPONSE", responseData);

                try {
                    JSONObject obj = new JSONObject(responseData);
                    if (obj.has("error")) {
                        JSONObject error = obj.getJSONObject("error");
                        String errorMsg = error.optString("message", "Unknown Gemini error");
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
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);
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
