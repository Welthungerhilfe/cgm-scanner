/**
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.welthungerhilfe.cgm.scanner.network.module;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import de.welthungerhilfe.cgm.scanner.network.service.DeviceService;
import de.welthungerhilfe.cgm.scanner.network.service.UploadService;
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
