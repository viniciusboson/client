package org.msf.records.updater;

import android.app.Application;

import org.msf.records.ui.PatientSearchActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * A Dagger module that provides bindings for update-related classes.
 */
@Module(
        injects = {
                PatientSearchActivity.class
        },
        complete = false,
        library = true
)
public class UpdateModule {

    @Provides @Singleton UpdateServer provideUpdateServer() {
        return new UpdateServer(null /*rootUrl*/);
    }

    @Provides @Singleton UpdateManager provideUpdateManager(
            Application application, UpdateServer updateServer) {
        return new UpdateManager(application, updateServer);
    }
}