package de.welthungerhilfe.cgm.scanner.datasource.viewmodel;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import retrofit2.Retrofit;

public class CreateDataViewModelProvideFactory implements ViewModelProvider.Factory {

    Context context;
    Retrofit retrofit;

    public  CreateDataViewModelProvideFactory(Context context, Retrofit retrofit)
    {
        this.context = context;
        this.retrofit = retrofit;
    }
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new CreateDataViewModel(context,retrofit);
    }
}
