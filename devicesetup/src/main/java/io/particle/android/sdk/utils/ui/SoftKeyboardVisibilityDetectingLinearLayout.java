package io.particle.android.sdk.utils.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.LinearLayout;

/**
 * How can Android have gone this long without this ability being built-in...?
 *
 * Derived from: https://gist.github.com/mrleolink/8823150
 *
 */
public class SoftKeyboardVisibilityDetectingLinearLayout extends LinearLayout {


    public interface SoftKeyboardVisibilityChangeListener {

        void onSoftKeyboardShown();

        void onSoftKeyboardHidden();

    }


    private boolean isKeyboardShown;
    private SoftKeyboardVisibilityChangeListener listener;

    public SoftKeyboardVisibilityDetectingLinearLayout(Context context) {
        super(context);
    }

    public SoftKeyboardVisibilityDetectingLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SoftKeyboardVisibilityDetectingLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            // Keyboard is hidden
            if (isKeyboardShown) {
                isKeyboardShown = false;
                listener.onSoftKeyboardHidden();
            }
        }
        return super.dispatchKeyEventPreIme(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int proposedheight = MeasureSpec.getSize(heightMeasureSpec);
        final int actualHeight = getHeight();
        if (actualHeight > proposedheight) {
            // Keyboard is shown
            if (!isKeyboardShown) {
                isKeyboardShown = true;
                listener.onSoftKeyboardShown();
            }
        } else {
            // Keyboard is hidden <<< this doesn't work sometimes, so I don't use it
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setOnSoftKeyboardVisibilityChangeListener(SoftKeyboardVisibilityChangeListener listener) {
        this.listener = listener;
    }

}
