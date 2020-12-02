package de.welthungerhilfe.cgm.scanner.network.module;

import android.app.Application;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import de.welthungerhilfe.cgm.scanner.AppController;

@Component(modules = {AndroidInjectionModule.class,NetworkModule.class,AndroidClassModule.class})
public interface AppComponent extends AndroidInjector<AppController> {

    @Component.Builder
    interface Builder
    {
        @BindsInstance
        public Builder bindInstance(Application application);

        public AppComponent build();

    }
}
