package io.particle.android.sdk.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.ui.barcode_scanner.BarcodeScannerActivity;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.Toaster;
import io.particle.sdk.app.R;
import okio.BufferedSource;
import okio.Okio;


public class ElectronSetupFragment extends Fragment {

    // These are the actions which are used as "hostnames" in special "particle:" URIs
    // that the web framework will try to load.  If any new actions are created, the
    // string "key" for it should go here.
    private interface SetupActions {
        String SCAN_ICCID = "scanIccid";
        String SCAN_CREDIT_CARD = "scanCreditCard";
        String DONE = "done";
        String NOTIFICATION = "notification";
    }

    private static final int REQUEST_CODE_SCAN_ICCID = 1;

    private static final TLog log = TLog.get(ElectronSetupFragment.class);


    @BindView(R.id.electron_setup_webview) WebView webView;
    private Gson gson = new Gson();

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_electron_setup, container, false);
        ButterKnife.bind(this, view);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);

                if (uri.getScheme().equals("particle")) {
                    handleParticleActionUri(uri);
                    return true;

                } else {
                    return super.shouldOverrideUrlLoading(view, url);
                }
            }
        });

        String snippet = String.format(
                readRawString(R.raw.electron_setup_variable_injection_js_template),
                ParticleCloudSDK.getCloud().getLoggedInUsername(),
                ParticleCloudSDK.getCloud().getAccessToken()
        );
        loadJavaScriptSnippet(snippet);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        webView.loadUrl(getString(R.string.electron_setup_uri));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SCAN_ICCID) {
            if (resultCode == CommonStatusCodes.SUCCESS && data != null) {
                Barcode barcode = data.getParcelableExtra(BarcodeScannerActivity.EXTRA_BARCODE);

                log.d("Barcode read: " + barcode.displayValue);

                this.onBarcodeScanningFinished(barcode.displayValue);
            } else {
                Toaster.s(this, "No barcode scanned.");
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadJavaScriptSnippet(String jsSnippet) {
        webView.loadUrl("javascript: " + jsSnippet);
    }

    private String readRawString(@RawRes int resId) {
        BufferedSource buffer = Okio.buffer(Okio.source(getResources().openRawResource(resId)));
        try {
            return buffer.readUtf8();

        } catch (IOException e) {
            // if this happens, we're just doomed.  give up.
            throw new RuntimeException(e);

        } finally {
            try {
                buffer.close();
            } catch (IOException e) {
                // nothing we need to do here
            }
        }
    }

    private void handleParticleActionUri(Uri uri) {
        String action = uri.getHost();
        switch (action) {
            case SetupActions.DONE:
                doElectronSetupDone();
                break;

            case SetupActions.NOTIFICATION:
                String fragment = uri.getFragment();
                fragment = (fragment == null) ? "" : fragment;
                String decoded = Uri.decode(fragment);
                NotificationContent content = gson.fromJson(decoded, NotificationContent.class);
                doElectronSetupShowNotification(content.level.equalsIgnoreCase("info"), content);
                break;

            case SetupActions.SCAN_CREDIT_CARD:
                doElectronSetupScanCreditCard();
                break;

            case SetupActions.SCAN_ICCID:
                doElectronSetupScanICCID();
                break;

            default:
                throw new RuntimeException("Unknown action!");
        }
    }

    private void doElectronSetupShowNotification(boolean isSuccess, NotificationContent content) {
        // doing the same thing for both here, but keeping it split into an if/else just in case...
        View view = getView();
        if (view != null) {
            if (isSuccess) {
                Snackbar.make(view, content.message, Snackbar.LENGTH_LONG)
                        .show();
            } else {
                Snackbar.make(view, content.message, Snackbar.LENGTH_LONG)
                        .show();
            }
        }
    }

    private void doElectronSetupScanICCID() {
        startActivityForResult(
                new Intent(getActivity(), BarcodeScannerActivity.class),
                REQUEST_CODE_SCAN_ICCID);
    }

    private void doElectronSetupScanCreditCard() {
        Toaster.s(this, "Not implemented yet!");
    }

    private void doElectronSetupDone() {
        Objects.requireNonNull(getActivity()).finish();
    }

    private void onBarcodeScanningFinished(@Nullable String barcodeValue) {
        if (barcodeValue == null) {
            // didn't get a barcode value, scanning failed.
            return;
        }

        String snippet = String.format(
                readRawString(R.raw.electron_setup_inject_iccid_js_template),
                barcodeValue
        );
        loadJavaScriptSnippet(snippet);
    }


    public static class NotificationContent {

        final String level;
        public final String title;
        final String message;

        public NotificationContent(String level, String title, String message) {
            this.level = level;
            this.title = title;
            this.message = message;
        }
    }
}
