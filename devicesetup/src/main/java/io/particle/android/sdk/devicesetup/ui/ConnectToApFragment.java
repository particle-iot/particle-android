package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;

import javax.inject.Inject;

import io.particle.android.sdk.devicesetup.apconnector.ApConnector;
import io.particle.android.sdk.devicesetup.apconnector.ApConnector.Client;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.android.sdk.utils.WorkerFragment;
import io.particle.android.sdk.utils.ui.Fragments;
import io.particle.android.sdk.utils.ui.Ui;


// reconsider if this even needs to be a fragment at all
public class ConnectToApFragment extends WorkerFragment {

    public static final String TAG = WorkerFragment.buildFragmentTag(ConnectToApFragment.class);


    public static ConnectToApFragment get(FragmentActivity activity) {
        return Ui.findFrag(activity, TAG);
    }

    public static ConnectToApFragment ensureAttached(FragmentActivity activity) {
        ConnectToApFragment frag = get(activity);
        if (frag == null) {
            frag = new ConnectToApFragment();
            WorkerFragment.addFragment(activity, frag, TAG);
        }
        return frag;
    }

    @Inject protected ApConnector apConnector;
    @Inject protected WifiFacade wifiFacade;
    private Client apConnectorClient;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        apConnectorClient = Fragments.getCallbacksOrThrow(this, Client.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        apConnector.stop();
    }

    /**
     * Connect this Android device to the specified AP.
     *
     * @param config the WifiConfiguration defining which AP to connect to
     */
    public void connectToAP(final WifiConfiguration config) {
        apConnector.connectToAP(config, apConnectorClient);
    }

}
