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

    public UploadManagerViewModel(@NonNull Application application) {
        super(application);

        FileLogRepository repository = FileLogRepository.getInstance(application);
        uploadStatusLiveData = repository.getMeasureUploadProgress("%");
    }

    public LiveData<UploadStatus> getUploadStatusLiveData() {
        return uploadStatusLiveData;
    }
}
