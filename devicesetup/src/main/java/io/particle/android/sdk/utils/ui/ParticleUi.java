package io.particle.android.sdk.utils.ui;

import android.support.v4.app.FragmentActivity;
import android.view.View;

import io.particle.android.sdk.devicesetup.R;


public class ParticleUi {

    // since it's specific to the SDK UI, this method assumes that the id of the
    // progress spinner in the button layout is "R.id.button_progress_indicator"
    public static void showParticleButtonProgress(FragmentActivity activity, int buttonId,
                                                  final boolean show) {
        Ui.fadeViewVisibility(activity, R.id.button_progress_indicator, show);
        Ui.findView(activity, buttonId).setEnabled(!show);
    }


    public static void enableBrandLogoInverseVisibilityAgainstSoftKeyboard(FragmentActivity activity) {
        SoftKeyboardVisibilityDetectingLinearLayout detectingLayout;
        detectingLayout = Ui.findView(activity, R.id.keyboard_change_detector_layout);
        detectingLayout.setOnSoftKeyboardVisibilityChangeListener(new BrandImageHeaderHider(activity));
    }


    public static class BrandImageHeaderHider
            implements SoftKeyboardVisibilityDetectingLinearLayout.SoftKeyboardVisibilityChangeListener {

        final View logoView;

        public BrandImageHeaderHider(FragmentActivity activity) {
            logoView = Ui.findView(activity, R.id.brand_image_header);
        }

        @Override
        public void onSoftKeyboardShown() {
            logoView.setVisibility(View.GONE);
        }

        @Override
        public void onSoftKeyboardHidden() {
            logoView.setVisibility(View.VISIBLE);
        }

    }
}
