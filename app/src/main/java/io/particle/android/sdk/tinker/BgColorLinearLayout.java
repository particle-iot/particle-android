package io.particle.android.sdk.tinker;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;


/**
 * This exists to make a background color "property", complete with setter *and* getter as a numeric
 * value.  This allows the use of PropertyAnimator APIs.
 */
public class BgColorLinearLayout extends LinearLayout {

    public BgColorLinearLayout(Context context) {
        super(context);
    }

    public BgColorLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BgColorLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public int getBackgroundColor() {
        Drawable d = getBackground();
        if (d == null || !(d instanceof ColorDrawable)) {
            return 0x00000000;
        } else {
            return ((ColorDrawable) getBackground()).getColor();
        }
    }

}
