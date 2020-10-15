package io.particle.android.sdk.di;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.RestrictTo;

import dagger.Module;
import dagger.Provides;
import io.particle.android.sdk.devicesetup.apconnector.ApConnector;
import io.particle.android.sdk.devicesetup.apconnector.ApConnectorApi21;
import io.particle.android.sdk.devicesetup.apconnector.ApConnectorApi29;
import io.particle.android.sdk.devicesetup.commands.CommandClientFactory;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepsFactory;
import io.particle.android.sdk.devicesetup.ui.DiscoverProcessWorker;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.WifiFacade;

@Module
@RestrictTo({RestrictTo.Scope.LIBRARY})
public class ApModule {

    @Provides
    protected SoftAPConfigRemover providesSoftApConfigRemover(Context context, WifiFacade wifiFacade) {
        return new SoftAPConfigRemover(context, wifiFacade);
    }

    @Provides
    protected WifiFacade providesWifiFacade(Context context) {
        return WifiFacade.get(context);
    }

    @Provides
    protected DiscoverProcessWorker providesDiscoverProcessWorker() {
        return new DiscoverProcessWorker();
    }

    @Provides
    protected CommandClientFactory providesCommandClientFactory() {
        return new CommandClientFactory();
    }

    @Provides
    protected SetupStepsFactory providesSetupStepsFactory() {
        return new SetupStepsFactory();
    }

    @Provides
    protected ApConnector providesApConnector(Context context, SoftAPConfigRemover softAPConfigRemover,
                                              WifiFacade wifiFacade) {
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            return new ApConnectorApi29(context, wifiFacade);
        } else {
            return new ApConnectorApi21(context, softAPConfigRemover, wifiFacade);
        }
    }
}
