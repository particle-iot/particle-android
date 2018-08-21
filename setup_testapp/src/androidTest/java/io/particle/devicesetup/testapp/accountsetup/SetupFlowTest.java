package io.particle.devicesetup.testapp.accountsetup;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.Html;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.CommandClientFactory;
import io.particle.android.sdk.devicesetup.commands.ConfigureApCommand;
import io.particle.android.sdk.devicesetup.commands.ConnectAPCommand;
import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.devicesetup.setupsteps.CheckIfDeviceClaimedStep;
import io.particle.android.sdk.devicesetup.setupsteps.ConfigureAPStep;
import io.particle.android.sdk.devicesetup.setupsteps.ConnectDeviceToNetworkStep;
import io.particle.android.sdk.devicesetup.setupsteps.EnsureSoftApNotVisible;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStep;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepApReconnector;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepException;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepsFactory;
import io.particle.android.sdk.devicesetup.setupsteps.StepConfig;
import io.particle.android.sdk.devicesetup.setupsteps.WaitForCloudConnectivityStep;
import io.particle.android.sdk.devicesetup.setupsteps.WaitForDisconnectionFromDeviceStep;
import io.particle.android.sdk.devicesetup.ui.DiscoverProcessWorker;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.devicesetup.testapp.EspressoDaggerMockRule;
import io.particle.devicesetup.testapp.MainActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SetupFlowTest {

    @Rule public EspressoDaggerMockRule rule = new EspressoDaggerMockRule();

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, false, false);

    @Mock private WifiFacade wifiFacade;

    @Mock private DiscoverProcessWorker discoverProcessWorker;

    @Mock private CommandClientFactory commandClientFactory;

    @Mock private SetupStepsFactory setupStepsFactory;

    @Test
    public void testSetupFlow() {
        //create/mock photon device SSID
        String ssid = (InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getApplicationContext().getString(R.string.network_name_prefix) + "-") + "TestSSID";
        //mock scan results object to fake photon device SSID as scan result
        mockDeviceDiscoveryResult(ssid);
        //fake scan wifi screen results by creating mock wifi scan result
        CommandClient commandClient = mock(CommandClient.class);
        when(commandClientFactory.newClientUsingDefaultsForDevices(any(WifiFacade.class), any(SSID.class))).thenReturn(commandClient);
        String wifiSSID = "fakeWifiToConnectTo";
        mockWifiResult(wifiSSID, commandClient);
        //fake configuration steps
        mockSetupSteps();
        mockConfigureApStep(commandClient);
        mockConnectApStep(commandClient);
        //launch test activity
        activityRule.launchActivity(null);
        //launch setup process
        ParticleDeviceSetupLibrary.startDeviceSetup(activityRule.getActivity(), MainActivity.class);
        try {
            setupFlow(ssid, wifiSSID);
        } catch (NoMatchingViewException e) {
//            loginFlow(ssid, wifiSSID);
            loginFlow();
        }
    }

    public void setupFlow(String photonSSID, String wifiSSID) {
        assertGetReadyScreen();
        onView(withText(R.string.enable_wifi)).perform(click());
        assertDeviceDiscoveryScreen(photonSSID);
        assertNetworkScreen(wifiSSID);

        onView(withId(R.id.result_image)).check(matches(isDisplayed()));
        onView(withId(R.id.result_summary)).check(matches(isDisplayed()));
        onView(withId(R.id.result_details)).check(matches(isDisplayed()));
        onView(withId(R.id.action_done)).check(matches(isDisplayed()));
        onView(withId(R.id.action_done)).perform(click());
    }

    public void loginFlow() {
        onView(withId(R.id.email)).perform(typeText(""));
        onView(withId(R.id.password)).perform(typeText(""));
        closeSoftKeyboard();
        onView(withId(R.id.action_log_in)).perform(click());
//        setupFlow(photonSSID, wifiSSID);
    }

    public String getNonHtmlString(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(activityRule.getActivity().getString(resId), Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            //noinspection deprecation
            return Html.fromHtml(activityRule.getActivity().getString(resId)).toString();
        }
    }

    private void assertNetworkScreen(String wifiSSID) {
        onView(withText(wifiSSID)).check(matches(isDisplayed()));
        onView(withId(R.id.brand_image_header)).check(matches(isDisplayed()));
        onView(withText(R.string.select_your_wifi_network)).check(matches(isDisplayed()));
        onView(withId(R.id.wifi_list_fragment)).check(matches(isDisplayed()));
        onView(withId(R.id.action_manual_network_entry)).check(matches(isDisplayed()));
        onView(withId(R.id.action_rescan)).check(matches(isDisplayed()));
        onView(withText(wifiSSID)).perform(click());
    }

    private void assertDeviceDiscoveryScreen(String photonSSID) {
        onView(withText(photonSSID)).check(matches(isDisplayed()));
        onView(withId(R.id.brand_image_header)).check(matches(isDisplayed()));
        onView(withId(R.id.imageView)).check(matches(isDisplayed()));
        onView(withId(R.id.wifi_list_header)).check(matches(isDisplayed()));
        onView(withId(R.id.wifi_list_fragment)).check(matches(isDisplayed()));
        onView(withId(R.id.msg_device_not_listed)).check(matches(isDisplayed()));
        onView(withId(R.id.logged_in_as)).check(matches(isDisplayed()));
        onView(withId(R.id.action_log_out)).check(matches(isDisplayed()));
        onView(withId(R.id.action_cancel)).check(matches(isDisplayed()));
        onView(withText(photonSSID)).perform(click());
    }

    private void assertGetReadyScreen() {
        onView(withId(R.id.action_im_ready)).check(matches(isDisplayed()));
        onView(withId(R.id.get_ready_text)).check(matches(isDisplayed()));
        onView(withId(R.id.get_ready_text_title)).check(matches(isDisplayed()));
        onView(withId(R.id.brand_image_header)).check(matches(isDisplayed()));
        onView(withId(R.id.action_im_ready)).perform(click());
    }

    private void mockSetupSteps() {
        ConfigureAPStep configureAPStep = mock(ConfigureAPStep.class);
        ConnectDeviceToNetworkStep connectDeviceToNetworkStep = mock(ConnectDeviceToNetworkStep.class);
        CheckIfDeviceClaimedStep checkIfDeviceClaimedStep = mock(CheckIfDeviceClaimedStep.class);
        EnsureSoftApNotVisible ensureSoftApNotVisible = mock(EnsureSoftApNotVisible.class);
        WaitForCloudConnectivityStep waitForCloudConnectivityStep = mock(WaitForCloudConnectivityStep.class);
        WaitForDisconnectionFromDeviceStep waitForDisconnectionFromDeviceStep = mock(WaitForDisconnectionFromDeviceStep.class);
        //step configuration for each step
        mockStepConfig(configureAPStep, R.id.configure_device_wifi_credentials);
        mockStepConfig(connectDeviceToNetworkStep, R.id.connect_to_wifi_network);
        mockStepConfig(checkIfDeviceClaimedStep, R.id.verify_product_ownership);
        mockStepConfig(ensureSoftApNotVisible, R.id.wait_for_device_cloud_connection);
        mockStepConfig(waitForCloudConnectivityStep, R.id.check_for_internet_connectivity);
        mockStepConfig(waitForDisconnectionFromDeviceStep, R.id.reconnect_to_wifi_network);

        mockStep(configureAPStep, connectDeviceToNetworkStep, checkIfDeviceClaimedStep, ensureSoftApNotVisible,
                waitForCloudConnectivityStep, waitForDisconnectionFromDeviceStep);
        //return mocked setup steps from factory
        when(setupStepsFactory.newConfigureApStep(any(CommandClient.class), any(SetupStepApReconnector.class),
                any(ScanApCommand.Scan.class), isNull(), isNull()))
                .thenReturn(configureAPStep);
        when(setupStepsFactory.newConnectDeviceToNetworkStep(any(CommandClient.class), any(SetupStepApReconnector.class)))
                .thenReturn(connectDeviceToNetworkStep);
        when(setupStepsFactory.newCheckIfDeviceClaimedStep(any(ParticleCloud.class), isNull()))
                .thenReturn(checkIfDeviceClaimedStep);
        when(setupStepsFactory.newEnsureSoftApNotVisible(any(SSID.class), any(WifiFacade.class)))
                .thenReturn(ensureSoftApNotVisible);
        when(setupStepsFactory.newWaitForCloudConnectivityStep(any(Context.class)))
                .thenReturn(waitForCloudConnectivityStep);
        when(setupStepsFactory.newWaitForDisconnectionFromDeviceStep(any(SSID.class), any(WifiFacade.class)))
                .thenReturn(waitForDisconnectionFromDeviceStep);
    }

    private void mockStepConfig(SetupStep setupStep, int stepId) {
        StepConfig setupConfig = mock(StepConfig.class);
        when(setupConfig.getStepId()).thenReturn(stepId);
        when(setupStep.getStepConfig()).thenReturn(setupConfig);
    }

    private void mockStep(SetupStep... setupStep) {
        for (SetupStep step : setupStep) {
            when(step.isStepFulfilled()).thenReturn(true);
            when(step.getLog()).thenReturn(TLog.get(this.getClass()));
        }
    }

    private void mockDeviceDiscoveryResult(String ssid) {
        ScanResult scanResult = mock(ScanResult.class);
        scanResult.SSID = ssid;
        scanResult.capabilities = "WEP";
        //mock wifi wrapper to return fake photon device in scan results
        List<ScanResult> scanResultList = new ArrayList<>();
        scanResultList.add(scanResult);
        when(wifiFacade.getScanResults()).thenReturn(scanResultList);
        //create fake wifi info object for fake photon device
        WifiInfo wifiInfo = mock(WifiInfo.class);
        when(wifiInfo.getSSID()).thenReturn(ssid);
        when(wifiInfo.getNetworkId()).thenReturn(5);
        //mock wifi wrapper to return fake photon device as "connected" network
        when(wifiFacade.getCurrentlyConnectedSSID()).thenReturn(SSID.from(ssid));
        when(wifiFacade.getConnectionInfo()).thenReturn(wifiInfo);
        //mock device discovery process worker to do nothing (would throw exception),
        //doing nothing would mean successful connection to ap
        try {
            doNothing().when(discoverProcessWorker).doTheThing();
        } catch (SetupStepException e) {
            e.printStackTrace();
        }
    }

    private void mockConnectApStep(CommandClient commandClient) {
        ConnectAPCommand.Response response = mock(ConnectAPCommand.Response.class);
        when(response.isOK()).thenReturn(true);
        try {
            //noinspection unchecked
            when(commandClient.sendCommand(any(ConnectAPCommand.class), any(Class.class))).thenReturn(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mockConfigureApStep(CommandClient commandClient) {
        ConfigureApCommand.Response response = mock(ConfigureApCommand.Response.class);
        when(response.isOk()).thenReturn(true);
        try {
            //noinspection unchecked
            when(commandClient.sendCommand(any(ConfigureApCommand.class), any(Class.class))).thenReturn(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mockWifiResult(String wifiSSID, CommandClient commandClient) {
        ScanApCommand.Scan scan = new ScanApCommand.Scan(wifiSSID, 0, 0);
        ScanApCommand.Response response = mock(ScanApCommand.Response.class);
        when(response.getScans()).thenReturn(Collections.singletonList(scan));
        try {
            //noinspection unchecked
            when(commandClient.sendCommand(any(ScanApCommand.class), any(Class.class))).thenReturn(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}