package de.welthungerhilfe.cgm.scanner.activities;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.R;

public class TutorialActivity extends AppCompatActivity {

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_tutorial);
        ButterKnife.bind(this);


    }
}
