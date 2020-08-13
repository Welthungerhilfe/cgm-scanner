package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.annotation.NonNull;

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
