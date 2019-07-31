package de.welthungerhilfe.cgm.scanner.ui.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.R;

public class ConsentScanActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    @BindView(R.id.cameraPreview)
    SurfaceView cameraPreview;
    private SurfaceHolder cameraHolder;

    protected void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);
        setContentView(R.layout.activity_consent_scan);
        ButterKnife.bind(this);

        initCamera();
    }

    private void initCamera() {
        cameraHolder = cameraPreview.getHolder();
        cameraHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
