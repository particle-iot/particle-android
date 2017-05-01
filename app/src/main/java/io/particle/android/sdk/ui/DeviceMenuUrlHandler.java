package io.particle.android.sdk.ui;

import android.app.Activity;
import android.net.Uri;
import android.util.SparseIntArray;

import io.particle.android.sdk.utils.ui.WebViewActivity;
import io.particle.sdk.app.R;

public class DeviceMenuUrlHandler {

    private static final SparseIntArray menuIdsToUris = new SparseIntArray();

    static {
        menuIdsToUris.put(R.id.action_show_docs_particle_app_tinker, R.string.uri_docs_particle_app_tinker);
        menuIdsToUris.put(R.id.action_show_docs_setting_up_your_device, R.string.uri_docs_setting_up_your_device);
        menuIdsToUris.put(R.id.action_show_docs_create_your_own_android_app, R.string.uri_docs_create_your_own_android_app);
        menuIdsToUris.put(R.id.action_support_show_community, R.string.uri_support_community);
        menuIdsToUris.put(R.id.action_support_show_support_site, R.string.uri_support_site);
    }

    /**
     * Attempt to handle the action item with the given ID.
     *
     * @return true if action item was handled, else false.
     */
    public static boolean handleActionItem(Activity activity, int actionItemId, CharSequence titleToShow) {
        if (menuIdsToUris.indexOfKey(actionItemId) < 0) {
            // we don't handle this ID.
            return false;
        }
        Uri uri = Uri.parse(activity.getString(menuIdsToUris.get(actionItemId)));
        activity.startActivity(WebViewActivity.buildIntent(activity, uri, titleToShow));
        return true;
    }
}