package io.particle.android.sdk.di;

import androidx.annotation.RestrictTo;

import dagger.Subcomponent;
import io.particle.android.sdk.accountsetup.LoginActivity;
import io.particle.android.sdk.accountsetup.PasswordResetActivity;
import io.particle.android.sdk.accountsetup.TwoFactorActivity;
import io.particle.android.sdk.devicesetup.ui.ConnectToApFragment;
import io.particle.android.sdk.devicesetup.ui.ConnectingActivity;
import io.particle.android.sdk.devicesetup.ui.ConnectingProcessWorkerTask;
import io.particle.android.sdk.devicesetup.ui.DiscoverDeviceActivity;
import io.particle.android.sdk.devicesetup.ui.GetReadyActivity;
import io.particle.android.sdk.devicesetup.ui.ManualNetworkEntryActivity;
import io.particle.android.sdk.devicesetup.ui.PasswordEntryActivity;
import io.particle.android.sdk.devicesetup.ui.SelectNetworkActivity;
import io.particle.android.sdk.devicesetup.ui.SuccessActivity;

@PerActivity
@Subcomponent(modules = {ApModule.class})
@RestrictTo({RestrictTo.Scope.LIBRARY})
public interface ActivityInjectorComponent {
    void inject(GetReadyActivity activity);

    void inject(LoginActivity loginActivity);

    void inject(PasswordResetActivity passwordResetActivity);

    void inject(SuccessActivity successActivity);

    void inject(DiscoverDeviceActivity discoverDeviceActivity);

    void inject(ConnectingActivity connectingActivity);

    void inject(PasswordEntryActivity passwordEntryActivity);

    void inject(ManualNetworkEntryActivity manualNetworkEntryActivity);

    void inject(SelectNetworkActivity selectNetworkActivity);

    void inject(ConnectToApFragment connectToApFragment);

    void inject(ConnectingProcessWorkerTask connectingProcessWorkerTask);

    void inject(TwoFactorActivity twoFactorActivity);

    @Subcomponent.Builder
    interface Builder {
        Builder apModule(ApModule module);

        ActivityInjectorComponent build();
    }

}
