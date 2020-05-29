package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.support.annotation.NonNull;

import de.welthungerhilfe.cgm.scanner.datasource.models.UploadStatus;
import de.welthungerhilfe.cgm.scanner.datasource.repository.FileLogRepository;

public class UploadManagerViewModel extends AndroidViewModel {
    private LiveData<UploadStatus> uploadStatusLiveData;
    private LiveData<Double> speedLiveData;
    private MutableLiveData<Double> expectTimeLiveData;

    private long previousTimestamp = 0;
    private double previousUploadedSize;

    public UploadManagerViewModel(@NonNull Application application) {
        super(application);

        FileLogRepository repository = FileLogRepository.getInstance(application);
        speedLiveData = new MutableLiveData<>();
        expectTimeLiveData = new MutableLiveData<>();
        uploadStatusLiveData = repository.getMeasureUploadProgress("%");

        speedLiveData = Transformations.switchMap(uploadStatusLiveData, status -> {
            double speed = 0;
            long timestamp = System.currentTimeMillis();
            if (previousTimestamp == 0) {
                previousTimestamp = timestamp;
                previousUploadedSize = status.getUploaded();
            } else if (timestamp > previousTimestamp) {
                speed = (status.getUploaded() - previousUploadedSize) / (timestamp - previousTimestamp ) * 1000;
                double expected = (status.getTotal() - status.getUploaded()) / speed;
                previousTimestamp = timestamp;
            }

            MutableLiveData<Double> live = new MutableLiveData<>();
            live.setValue(speed);
            return live;
        });
    }

    public LiveData<UploadStatus> getUploadStatusLiveData() {
        return uploadStatusLiveData;
    }

    public LiveData<Double> getSpeedLiveData() {
        return speedLiveData;
    }

    public MutableLiveData<Double> getExpectTimeLiveData() {
        return expectTimeLiveData;
    }
}
