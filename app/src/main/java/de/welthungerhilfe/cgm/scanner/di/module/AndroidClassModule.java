package de.welthungerhilfe.cgm.scanner.di.module;


import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import de.welthungerhilfe.cgm.scanner.helper.service.DeviceService;
import de.welthungerhilfe.cgm.scanner.helper.service.UploadService;
import de.welthungerhilfe.cgm.scanner.helper.syncdata.SyncService;
import de.welthungerhilfe.cgm.scanner.ui.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.MainActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.ScanModeActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.SettingsActivity;
import de.welthungerhilfe.cgm.scanner.ui.activities.UploadManagerActivity;
import de.welthungerhilfe.cgm.scanner.ui.fragments.GrowthDataFragment;
import de.welthungerhilfe.cgm.scanner.ui.fragments.MeasuresDataFragment;
import de.welthungerhilfe.cgm.scanner.ui.fragments.PersonalDataFragment;

@Module
public abstract class AndroidClassModule {

    @ContributesAndroidInjector
    abstract SyncService provideSyncService();

    @ContributesAndroidInjector
    abstract SettingsActivity provideSettingActivity();

    @ContributesAndroidInjector
    abstract CreateDataActivity provideDataActivity();

    @ContributesAndroidInjector
    abstract GrowthDataFragment provideGrowthDataFragment();

    @ContributesAndroidInjector
    abstract PersonalDataFragment providePersonalDataFragment();

    @ContributesAndroidInjector
    abstract MeasuresDataFragment provideMeasuresDataFragment();

    @ContributesAndroidInjector
    abstract DeviceService provideDeviceService();

    @ContributesAndroidInjector
    abstract UploadManagerActivity provideUploadManagerActivity();

    @ContributesAndroidInjector
    abstract ScanModeActivity provideScanModeActivity();

    @ContributesAndroidInjector
    abstract MainActivity provideMainActivity();

    @ContributesAndroidInjector
    abstract UploadService provideUploadService();






}
