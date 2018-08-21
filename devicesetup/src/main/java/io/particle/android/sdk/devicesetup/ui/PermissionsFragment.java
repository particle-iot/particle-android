package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.util.Log;

import java.util.Arrays;

import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.utils.WorkerFragment;
import io.particle.android.sdk.utils.ui.Ui;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;


public class PermissionsFragment extends Fragment implements OnRequestPermissionsResultCallback {

    public interface Client {
        void onUserAllowedPermission(String permission);

        void onUserDeniedPermission(String permission);
    }


    public static final String TAG = WorkerFragment.buildFragmentTag(PermissionsFragment.class);


    public static <T extends FragmentActivity & Client> PermissionsFragment ensureAttached(
            T callbacksActivity) {
        PermissionsFragment frag = get(callbacksActivity);
        if (frag == null) {
            frag = new PermissionsFragment();
            WorkerFragment.addFragment(callbacksActivity, frag, TAG);
        }
        return frag;
    }

    public static <T extends FragmentActivity & Client> PermissionsFragment get(T callbacksActivity) {
        return Ui.findFrag(callbacksActivity, TAG);
    }

    public static boolean hasPermission(@NonNull Context ctx, @NonNull String permission) {
        int result = ContextCompat.checkSelfPermission(ctx, permission);
        return (result == PERMISSION_GRANTED);
    }

    public void ensurePermission(final @NonNull String permission) {
        Builder dialogBuilder = new AlertDialog.Builder(getActivity())
                .setCancelable(false);

        // FIXME: stop referring to these location permission-specific strings here,
        // try to retrieve them from the client
        if (ActivityCompat.checkSelfPermission(getActivity(), permission) != PERMISSION_GRANTED ||
                ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission)) {
            dialogBuilder.setTitle(R.string.location_permission_dialog_title)
                    .setMessage(R.string.location_permission_dialog_text)
                    .setPositiveButton(R.string.got_it, (dialog, which) -> {
                        dialog.dismiss();
                        requestPermission(permission);
                    });
        } else {
            // user has explicitly denied this permission to setup.
            // show a simple dialog and bail out.
            dialogBuilder.setTitle(R.string.location_permission_denied_dialog_title)
                    .setMessage(R.string.location_permission_denied_dialog_text)
                    .setPositiveButton(R.string.settings, (dialog, which) -> {
                        dialog.dismiss();
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        String pkgName = getActivity().getApplicationInfo().packageName;
                        intent.setData(Uri.parse("package:" + pkgName));
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.exit_setup, (dialog, which) -> {
                        Client client = (Client) getActivity();
                        client.onUserDeniedPermission(permission);
                    });
        }

        dialogBuilder.show();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, String.format("onRequestPermissionsResult(%d, %s, %s)",
                requestCode,
                Arrays.toString(permissions),
                Arrays.toString(grantResults)));
        if (requestCode != REQUEST_CODE) {
            Log.i(TAG, "Unrecognized request code: " + requestCode);
        }

        // we only ever deal with one permission at a time, so we can always safely grab the first
        // member of this array.
        String permission = permissions[0];
        if (hasPermission(getActivity(), permission)) {
            Client client = (Client) getActivity();
            client.onUserAllowedPermission(permission);
        } else {
            this.ensurePermission(permission);
        }
    }

    private void requestPermission(String permission) {
        ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, REQUEST_CODE);
    }

    private static final int REQUEST_CODE = 128;

}
