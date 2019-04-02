package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.datasource.models.Person;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class SplashActivity extends AppCompatActivity {
    private int a = 0;

    public void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));

            finish();
        }, 1000);
    }

    private void createThoundPersons(String name) {
        Person person = new Person();
        person.setId(AppController.getInstance().getPersonId(name));
        person.setQrcode(name);
        person.setCreated(System.currentTimeMillis());
        person.setName(name);
        person.setSurname(name);
        person.setBirthday(System.currentTimeMillis());
        person.setGuardian("guaridan");
        person.setSex("female");
        person.setAgeEstimated(false);
        person.setTimestamp(Utils.getUniversalTimestamp());
        person.setCreatedBy("zhangnemo34@gmail.com");

        AppController.getInstance().firebaseFirestore.collection("persons")
                .document(person.getId())
                .set(person)
                .addOnFailureListener(e -> {
                    a ++;
                    Log.e("PersonCount", String.valueOf(a));
                })
                .addOnSuccessListener(aVoid -> {
                    a ++;
                    Log.e("PersonCount", String.valueOf(a));
                });
    }
}
