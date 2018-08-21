package io.particle.android.sdk.utils.ui;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Toast;


public class Toaster {

    public static void s(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    public static void l(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    public static void s(Context ctx, String msg, int gravity) {
        Toast t = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT);
        t.setGravity(gravity, 0, 0);
        t.show();
    }

    public static void l(Context ctx, String msg, int gravity) {
        Toast t = Toast.makeText(ctx, msg, Toast.LENGTH_LONG);
        t.setGravity(gravity, 0, 0);
        t.show();
    }

    public static void s(Fragment frag, String msg) {
        s(frag.getActivity(), msg);
    }

    public static void l(Fragment frag, String msg) {
        l(frag.getActivity(), msg);
    }

    public static void s(View view, String msg) {
        s(view.getContext(), msg);
    }

    public static void l(View view, String msg) {
        l(view.getContext(), msg);
    }
}


