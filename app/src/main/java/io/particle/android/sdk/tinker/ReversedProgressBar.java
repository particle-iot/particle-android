package io.particle.android.sdk.tinker;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ProgressBar;


public class ReversedProgressBar extends ProgressBar {

    public ReversedProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onDraw(Canvas c) {
        float px = this.getWidth() / 2.0f;
        float py = this.getHeight() / 2.0f;

        c.scale(-1, 1, px, py);

        super.onDraw(c);
    }
}
