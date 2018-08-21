package io.particle.android.sdk.utils;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass used as a marker for "worker" fragments, while bundling in the
 * fundamental behavior that makes them worker fragments.
 */
public class WorkerFragment extends Fragment {

    // produce a standardized, unique tag
    public static String buildFragmentTag(Class<? extends Fragment> fragClass) {
        // return a unique name
        return "FRAG_" + fragClass.getCanonicalName();
    }

    // Syntactic sugar for simply adding a WorkerFragment
    public static void addFragment(FragmentActivity activity, Fragment frag, String tag) {
        activity.getSupportFragmentManager()
                .beginTransaction()
                .add(frag, tag)
                .commit();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.setRetainInstance(true);
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                   Bundle savedInstanceState) {
        return null;
    }

}
