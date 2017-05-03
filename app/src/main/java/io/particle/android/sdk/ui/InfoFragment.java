package io.particle.android.sdk.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.f2prateek.bundler.FragmentBundlerCompat;

import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

import static android.content.Context.CLIPBOARD_SERVICE;

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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View top = inflater.inflate(R.layout.fragment_info, container, false);
        device = getArguments().getParcelable(ARG_DEVICE);
        displayDeviceInformation(top);
        return top;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void displayDeviceInformation(View rootView) {
        ImageView deviceImage = Ui.findView(rootView, R.id.device_image);
        TextView deviceType = Ui.findView(rootView, R.id.device_type_name);

        switch (device.getDeviceType()) {
            case CORE:
                deviceType.setText(R.string.core);
                deviceImage.setImageResource(R.drawable.core_vector);
                break;
            case ELECTRON:
                deviceType.setText(R.string.electron);
                deviceImage.setImageResource(R.drawable.electron_vector_small);
                //show extra fields
//                Ui.findView(rootView, R.id.device_iccid_label).setVisibility(View.VISIBLE);
//                Ui.findView(rootView, R.id.device_iccid).setVisibility(View.VISIBLE);
//                Ui.findView(rootView, R.id.device_imei_label).setVisibility(View.VISIBLE);
//                Ui.findView(rootView, R.id.device_imei).setVisibility(View.VISIBLE);
//                Ui.findView(rootView, R.id.device_data_usage_label).setVisibility(View.VISIBLE);
//                Ui.findView(rootView, R.id.device_data_usage).setVisibility(View.VISIBLE);
//                Ui.findView(rootView, R.id.device_iccid_copy).setVisibility(View.VISIBLE);
                break;
            default:
                deviceType.setText(R.string.photon);
                deviceImage.setImageResource(R.drawable.photon_vector_small);
                break;
        }

        populateInfoFields(rootView);
    }

    private void populateInfoFields(View rootView) {
        TextView id = Ui.findView(rootView, R.id.device_id);
        id.setText(device.getID());

        TextView lastHeard = Ui.findView(rootView, R.id.device_last_heard);
        long now = System.currentTimeMillis();
        lastHeard.setText(DateUtils.getRelativeTimeSpanString(device.getLastHeard().getTime(), now,
                DateUtils.DAY_IN_MILLIS));

        TextView ipAddress = Ui.findView(rootView, R.id.device_ip_address);
        ipAddress.setText(device.getIpAddress());

        rootView.findViewById(R.id.device_id_copy).setOnClickListener(v -> {
            Context context = getContext();
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Device ID", id.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, R.string.clipboard_copy_id_msg, Toast.LENGTH_SHORT).show();
        });
    }

}
