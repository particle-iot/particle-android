package io.particle.android.sdk.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.f2prateek.bundler.FragmentBundlerCompat;

import java.util.Date;

import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

import static android.content.Context.CLIPBOARD_SERVICE;
import static java.util.Objects.requireNonNull;

/**
 * Created by Julius.
 */
public class InfoFragment extends Fragment {

    public static InfoFragment newInstance(ParticleDevice device) {
        return FragmentBundlerCompat.create(new InfoFragment())
                .put(InfoFragment.ARG_DEVICE, device)
                .build();
    }

    // The device that this fragment represents
    public static final String ARG_DEVICE = "ARG_DEVICE";

    private ParticleDevice device;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View top = inflater.inflate(R.layout.fragment_info, container, false);
        device = requireNonNull(getArguments()).getParcelable(ARG_DEVICE);
        displayDeviceInformation(top);
        return top;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void displayDeviceInformation(View rootView) {
        ImageView deviceImage = Ui.findView(rootView, R.id.device_image);
        TextView deviceType = Ui.findView(rootView, R.id.device_type_name);

        // FIXME: this and several other spots are doing the same work.  Make it generic.
        switch (device.getDeviceType()) {
            case CORE:
                deviceType.setText(R.string.core);
                deviceImage.setImageResource(R.drawable.core_vector);
                break;
            case ELECTRON:
                deviceType.setText(R.string.electron);
                deviceImage.setImageResource(R.drawable.electron_vector_small);
                //show extra fields
                Ui.findView(rootView, R.id.device_iccid_label).setVisibility(View.VISIBLE);
                Ui.findView(rootView, R.id.device_iccid).setVisibility(View.VISIBLE);
                Ui.findView(rootView, R.id.device_imei_label).setVisibility(View.VISIBLE);
                Ui.findView(rootView, R.id.device_imei).setVisibility(View.VISIBLE);
                Ui.findView(rootView, R.id.device_data_usage_label).setVisibility(View.VISIBLE);
                Ui.findView(rootView, R.id.device_data_usage).setVisibility(View.VISIBLE);
                Ui.findView(rootView, R.id.device_iccid_copy).setVisibility(View.VISIBLE);
                populateElectronInfoFields(rootView);
                break;
            case PHOTON:
                deviceType.setText(R.string.photon);
                deviceImage.setImageResource(R.drawable.photon_vector_small);
                break;
            case RASPBERRY_PI:
                deviceType.setText(R.string.raspberry);
                deviceImage.setImageResource(R.drawable.pi_vector);
                break;
            case P1:
                deviceType.setText(R.string.p1);
                deviceImage.setImageResource(R.drawable.p1_vector);
                break;
            case RED_BEAR_DUO:
                deviceType.setText(R.string.red_bear_duo);
                deviceImage.setImageResource(R.drawable.red_bear_duo_vector);
                break;

            case ARGON:
            case A_SERIES:
                deviceType.setText(R.string.product_name_argon);
                deviceImage.setImageResource(R.drawable.argon_vector);
                break;

            case BORON:
            case B_SERIES:
                deviceType.setText(R.string.product_name_boron);
                deviceImage.setImageResource(R.drawable.boron_vector);
                break;

            case XENON:
            case X_SERIES:
                deviceType.setText(R.string.product_name_xenon);
                deviceImage.setImageResource(R.drawable.xenon_vector);
                break;

            default:
                deviceType.setText(R.string.unknown);
                deviceImage.setImageResource(R.drawable.unknown_vector);
                break;
        }

        populateInfoFields(rootView);
    }

    private void populateInfoFields(View rootView) {
        TextView id = Ui.findView(rootView, R.id.device_id);
        id.setText(device.getId());

        TextView lastHeard = Ui.findView(rootView, R.id.device_last_heard);
        long now = System.currentTimeMillis();
        Date lastDate = device.getLastHeard();
        if (lastDate == null) {
            lastHeard.setText("-");
        } else {
            lastHeard.setText(DateUtils.getRelativeTimeSpanString(lastDate.getTime(), now,
                    DateUtils.DAY_IN_MILLIS));
        }

        TextView ipAddress = Ui.findView(rootView, R.id.device_ip_address);
        ipAddress.setText(device.getIpAddress());

        Ui.findView(rootView, R.id.device_id_copy).setOnClickListener(v -> {
            Context context = getContext();
            ClipboardManager clipboard = (ClipboardManager) requireNonNull(context).getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Device ID", id.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, R.string.clipboard_copy_id_msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void populateElectronInfoFields(View rootView) {
        pollDataUsage(rootView);

        TextView iccid = Ui.findView(rootView, R.id.device_iccid);
        iccid.setText(device.getIccid());

        TextView imei = Ui.findView(rootView, R.id.device_imei);
        imei.setText(device.getImei());

        Ui.findView(rootView, R.id.device_iccid_copy).setOnClickListener(v -> {
            Context context = getContext();
            ClipboardManager clipboard = (ClipboardManager) requireNonNull(context).getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Device ICCID", iccid.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, R.string.clipboard_copy_iccid_msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void pollDataUsage(View rootView) {
        TextView dataUsage = Ui.findView(rootView, R.id.device_data_usage);

        try {
            Async.executeAsync(device, new Async.ApiWork<ParticleDevice, Float>() {
                @Override
                public Float callApi(@NonNull ParticleDevice particleDevice) throws ParticleCloudException {
                    return particleDevice.getCurrentDataUsage();
                }

                @Override
                public void onSuccess(@NonNull Float value) {
                    if (!isDetached()) {
                        // apparently checking "isDetached()" doesn't work if you're on a Samsung 😭
                        try {
                            dataUsage.setText(getString(R.string.value_mbs, value));
                        } catch (Exception ex) {
                            // pass: we just don't want to crash.
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull ParticleCloudException exception) {
                    if (!isDetached()) {
                        // apparently checking "isDetached()" doesn't work if you're on a Samsung 😭
                        try {
                            dataUsage.setText(R.string.default_mbs);
                        } catch (Exception ex) {
                            // pass: we just don't want to crash.
                        }
                    }
                }
            });
        } catch (ParticleCloudException e) {
            dataUsage.setText(R.string.default_mbs);
        }
    }

}
