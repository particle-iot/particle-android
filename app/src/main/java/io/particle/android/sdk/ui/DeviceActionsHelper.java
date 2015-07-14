package io.particle.android.sdk.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.widget.PopupMenu;

import io.particle.android.sdk.cloud.SparkDevice;
import io.particle.android.sdk.utils.ui.Toaster;
import io.particle.sdk.app.R;


public class DeviceActionsHelper {


    public static PopupMenu.OnMenuItemClickListener buildPopupMenuHelper(final Fragment fragment,
                                                                         final SparkDevice device) {
        return new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return takeActionForDevice(item.getItemId(), fragment.getActivity(), device);
            }
        };
    }


    public static boolean takeActionForDevice(int actionId, FragmentActivity activity,
                                              SparkDevice device) {
        switch (actionId) {
            case R.id.action_device_rename:
                RenameHelper.renameDevice(activity, device);
                return true;

            case R.id.action_device_unclaim:
                UnclaimHelper.unclaimDeviceWithDialog(activity, device);
                return true;

            case R.id.action_device_flash_tinker:
                if (device.getDeviceType() == SparkDevice.SparkDeviceType.CORE) {
                    FlashAppHelper.flashKnownAppWithDialog(activity, device,
                            SparkDevice.KnownApp.TINKER);
                } else {
                    FlashAppHelper.flashPhotonTinkerWithDialog(activity, device);
                }
                return true;

            case R.id.action_device_copy_id_to_clipboard:
                ClipboardManager clipboardMgr;
                clipboardMgr = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardMgr.setPrimaryClip(
                        ClipData.newPlainText("simple text", device.getID()));
                // FIXME: use string rsrc
                Toaster.s(activity, "Copied device ID to clipboard");
                return true;

            default:
                return false;
        }
    }

}
