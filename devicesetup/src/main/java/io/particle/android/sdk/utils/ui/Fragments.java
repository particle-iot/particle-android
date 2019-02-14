package io.particle.android.sdk.utils.ui;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

@RestrictTo(Scope.LIBRARY)
public class Fragments {

    /**
     * Return the callbacks for a fragment or throw an exception.
     * <p/>
     * Inspired by: https://gist.github.com/keyboardr/5455206
     */
    @SuppressWarnings("unchecked")
    public static <T> T getCallbacksOrThrow(Fragment frag, Class<T> callbacks) {
        Fragment parent = frag.getParentFragment();

        if (parent != null && callbacks.isInstance(parent)) {
            return (T) parent;

        } else {
            FragmentActivity activity = frag.getActivity();
            if (activity != null && callbacks.isInstance(activity)) {
                return (T) activity;
            }
        }

        // We haven't actually failed a class cast thanks to the checks above, but that's the
        // idiomatic approach for this pattern with fragments.
        throw new ClassCastException("This fragment's activity or parent fragment must implement "
                + callbacks.getCanonicalName());
    }


}
