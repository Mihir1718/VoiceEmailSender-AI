package com.example.voicemailsender;

import android.accounts.Account;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class GmailSender {

    private static final String TAG = "GmailSender";

    public static void sendEmail(Context context, GoogleSignInAccount account, String to, String subject, String body) {
        new Thread(() -> {
            try {
                // ✅ Get valid access token
                String scope = "oauth2:https://www.googleapis.com/auth/gmail.send";
                String token = GoogleAuthUtil.getToken(context, account.getAccount(), scope);

                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton("https://www.googleapis.com/auth/gmail.send")
                );
                credential.setSelectedAccount(account.getAccount());

                // Build Gmail API client
                Gmail service = new Gmail.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        GsonFactory.getDefaultInstance(),
                        credential
                ).setApplicationName("VoiceEmailSender").build();

                MimeMessage mimeMessage = createEmail(to, account.getEmail(), subject, body);
                Message message = createMessageWithEmail(mimeMessage);

                // Send the email
                Message sentMessage = service.users().messages().send("me", message).execute();
                Log.d(TAG, "✅ Email sent successfully! ID: " + sentMessage.getId());

            } catch (UserRecoverableAuthException e) {
                Log.e(TAG, "❌ Recoverable auth error: " + e.getMessage(), e);
                // Optional: Trigger startActivityForResult with e.getIntent() if needed
            } catch (Exception e) {
                Log.e(TAG, "❌ Email send failed: " + e.getMessage(), e);
            }
        }).start();
    }

    private static MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getInstance(props, null);
        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText, "UTF-8", "plain");
        return email;
    }

    private static Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}
