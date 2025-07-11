package com.example.voicemailsender;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InboxActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<String> emailSubjects = new ArrayList<>();
    private ArrayList<String> emailIds = new ArrayList<>();
    private Gmail gmailService;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

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
            } else if (id == R.id.nav_voice) {
                startActivity(new Intent(this, VoiceActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_inbox);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                return true;
            } else if (itemId == R.id.nav_voice) {
                startActivity(new Intent(getApplicationContext(), VoiceActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;
            }
            return false;
        });

        listView = findViewById(R.id.inboxListView);

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                this, Collections.singleton("https://www.googleapis.com/auth/gmail.readonly"));
        credential.setSelectedAccount(account.getAccount());

        try {
            gmailService = new Gmail.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
            ).setApplicationName("VoiceEmailSender").build();
        } catch (GeneralSecurityException | IOException e) {
            Toast.makeText(this, "Failed to initialize Gmail API", Toast.LENGTH_SHORT).show();
            Log.e("InboxActivity", "Initialization error", e);
            return;
        }

        loadEmails();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String messageId = emailIds.get(position);
            Intent intent = new Intent(InboxActivity.this, EmailDetailActivity.class);
            intent.putExtra("MESSAGE_ID", messageId);
            startActivity(intent);
        });
    }

    private void loadEmails() {
        progressDialog = ProgressDialog.show(this, "Loading", "Fetching emails...", true);
        new Thread(() -> {
            try {
                ListMessagesResponse response = gmailService.users().messages()
                        .list("me")
                        .setMaxResults(30L)
                        .setQ("in:inbox")
                        .execute();

                List<Message> messages = response.getMessages();
                if (messages == null) return;

                for (Message msg : messages) {
                    Message fullMessage = gmailService.users().messages()
                            .get("me", msg.getId()).setFormat("full").execute();
                    List<MessagePartHeader> headers = fullMessage.getPayload().getHeaders();
                    String subject = "(No Subject)";
                    for (MessagePartHeader header : headers) {
                        if ("Subject".equalsIgnoreCase(header.getName())) {
                            subject = header.getValue();
                            break;
                        }
                    }
                    emailSubjects.add(subject);
                    emailIds.add(fullMessage.getId());
                }

                runOnUiThread(() -> {
                    listView.setAdapter(new EmailAdapter(this, emailSubjects));
                    progressDialog.dismiss();
                });

            } catch (IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(InboxActivity.this, "❌ Failed to load inbox", Toast.LENGTH_LONG).show();
                });
                Log.e("InboxActivity", "❌ Error loading emails", e);
            }
        }).start();
    }
}
