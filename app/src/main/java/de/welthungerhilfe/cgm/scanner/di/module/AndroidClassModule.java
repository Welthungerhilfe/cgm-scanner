package de.welthungerhilfe.cgm.scanner.di.module;


import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import de.welthungerhilfe.cgm.scanner.helper.syncdata.SyncService;

@Module
public abstract class AndroidClassModule {

    @ContributesAndroidInjector
    abstract SyncService provideSyncService();
}
