package io.particle.android.sdk.utils.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

public class Ui {

    @SuppressWarnings("unchecked")
    public static <T extends View> T findView(FragmentActivity activity, int id) {
        return (T) activity.findViewById(id);
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> T findView(View enclosingView, int id) {
        return (T) enclosingView.findViewById(id);
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> T findView(Fragment frag, int id) {
        return (T) frag.getActivity().findViewById(id);
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> T findView(Dialog dialog, int id) {
        return (T) dialog.findViewById(id);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Fragment> T findFrag(FragmentActivity activity, int id) {
        return (T) activity.getSupportFragmentManager().findFragmentById(id);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Fragment> T findFrag(FragmentActivity activity, String tag) {
        return (T) activity.getSupportFragmentManager().findFragmentByTag(tag);
    }

    public static TextView setText(FragmentActivity activity, int textViewId, CharSequence text) {
        TextView textView = findView(activity, textViewId);
        textView.setText(text);
        return textView;
    }

    public static TextView setText(View view, int textViewId, CharSequence text) {
        TextView textView = findView(view, textViewId);
        textView.setText(text);
        return textView;
    }

    public static TextView setText(Fragment frag, int textViewId, CharSequence text) {
        TextView textView = findView(frag, textViewId);
        textView.setText(text);
        return textView;
    }

    public static String getText(FragmentActivity activity, int textViewId, boolean trim) {
        TextView textView = findView(activity, textViewId);
        String text = textView.getText().toString();
        return trim ? text.trim() : text;
    }

    public static String getText(Fragment frag, int textViewId, boolean trim) {
        TextView textView = findView(frag, textViewId);
        String text = textView.getText().toString();
        return trim ? text.trim() : text;
    }

    public static TextView setTextFromHtml(FragmentActivity activity, int textViewId, int htmlStringId) {
        return setTextFromHtml(activity, textViewId, activity.getString(htmlStringId));
    }

    public static TextView setTextFromHtml(FragmentActivity activity, int textViewId, String htmlString) {
        TextView tv = Ui.findView(activity, textViewId);
        tv.setText(Html.fromHtml(htmlString), TextView.BufferType.SPANNABLE);
        return tv;
    }


    public static void fadeViewVisibility(FragmentActivity activity, int viewId, final boolean show) {
        // Fade-in the progress spinner.
        int shortAnimTime = activity.getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        final View progressView = Ui.findView(activity, viewId);
        progressView.setVisibility(View.VISIBLE);
        progressView.animate()
                .setDuration(shortAnimTime)
                .alpha(show ? 1 : 0)
                .setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
    }

    // Technique taken from:
    // http://blog.danlew.net/2014/08/18/fast-android-asset-theming-with-colorfilter/
    public static Drawable getTintedDrawable(Context ctx, @DrawableRes int drawableResId,
                                             @ColorRes int colorResId) {
        Drawable drawable = ContextCompat.getDrawable(ctx, drawableResId);
        int color = ContextCompat.getColor(ctx, colorResId);
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        return drawable;
    }


}
