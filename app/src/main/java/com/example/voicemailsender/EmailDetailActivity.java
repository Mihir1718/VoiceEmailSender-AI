package com.example.voicemailsender;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import java.util.Collections;
import java.util.Locale;

public class EmailDetailActivity extends AppCompatActivity {

    TextView emailContentView;
    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_detail);

        emailContentView = findViewById(R.id.emailContentText);

        String messageId = getIntent().getStringExtra("MESSAGE_ID");
        if (messageId == null) {
            finish();
            return;
        }

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                fetchAndSpeakEmail(messageId);
            }
        });
    }

    private void fetchAndSpeakEmail(String messageId) {
        new Thread(() -> {
            try {
                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        this, Collections.singleton("https://www.googleapis.com/auth/gmail.readonly"));
                credential.setSelectedAccount(GoogleSignIn.getLastSignedInAccount(this).getAccount());

                Gmail service = new Gmail.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        credential
                ).setApplicationName("VoiceEmailSender").build();

                Message message = service.users().messages().get("me", messageId).setFormat("full").execute();
                String bodyData = message.getPayload().getBody().getData();

                if (bodyData == null && !message.getPayload().getParts().isEmpty()) {
                    bodyData = message.getPayload().getParts().get(0).getBody().getData();
                }

                if (bodyData != null) {
                    String decoded = new String(Base64.decode(bodyData, Base64.URL_SAFE));
                    runOnUiThread(() -> {
                        emailContentView.setText(decoded);
                        tts.speak(decoded, TextToSpeech.QUEUE_FLUSH, null, null);
                    });
                }

            } catch (Exception e) {
                Log.e("EmailDetail", "Failed to load email", e);
            }
        }).start();
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
