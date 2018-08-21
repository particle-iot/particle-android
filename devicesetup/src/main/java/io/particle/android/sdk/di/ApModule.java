package io.particle.android.sdk.di;

import android.content.Context;
import android.support.annotation.RestrictTo;

import dagger.Module;
import dagger.Provides;
import io.particle.android.sdk.devicesetup.ApConnector;
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
        return new ApConnector(context, softAPConfigRemover, wifiFacade);
    }
}
