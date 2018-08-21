package io.particle.android.sdk.utils.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.utils.SEGAnalytics;


public class WebViewActivity extends AppCompatActivity {


    private static final String EXTRA_CONTENT_URI = "EXTRA_CONTENT_URI";
    private static final String EXTRA_PAGE_TITLE = "EXTRA_PAGE_TITLE";


    public static Intent buildIntent(Context ctx, Uri uri) {
        return new Intent(ctx, WebViewActivity.class)
                .putExtra(EXTRA_CONTENT_URI, uri);
    }


    public static Intent buildIntent(Context ctx, Uri uri, CharSequence pageTitle) {
        return buildIntent(ctx, uri)
                .putExtra(EXTRA_PAGE_TITLE, pageTitle.toString());
    }


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        SEGAnalytics.track("Device Setup: Webview Screen");
        Toolbar toolbar = Ui.findView(this, R.id.toolbar);
        toolbar.setNavigationIcon(
                Ui.getTintedDrawable(this, R.drawable.ic_clear_black_24dp, R.color.element_tint_color));

        toolbar.setNavigationOnClickListener(view -> finish());

        if (getIntent().hasExtra(EXTRA_PAGE_TITLE)) {
            toolbar.setTitle(getIntent().getStringExtra(EXTRA_PAGE_TITLE));
        }

        WebView webView = Ui.findView(this, R.id.web_content);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // handle redirects in the same view
                view.loadUrl(url);
                // return false to indicate that we do not want to leave the webview
                return false; // then it is not handled by default action
            }
        });

        WebSettings webSettings = webView.getSettings();
        // this has to be enabled or else some pages don't render *at all.*
        webSettings.setJavaScriptEnabled(true);

        Uri uri = getIntent().getParcelableExtra(EXTRA_CONTENT_URI);
        webView.loadUrl(uri.toString());
    }

}
