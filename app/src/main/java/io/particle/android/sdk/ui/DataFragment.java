package io.particle.android.sdk.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.f2prateek.bundler.FragmentBundlerCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.utils.AnimationUtil;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

import static android.content.Context.CLIPBOARD_SERVICE;
import static io.particle.android.sdk.utils.Py.list;
import static java.util.Objects.requireNonNull;

public class DataFragment extends Fragment {

    public static DataFragment newInstance(ParticleDevice device) {
        return FragmentBundlerCompat.create(new DataFragment())
                .put(DataFragment.ARG_DEVICE, device)
                .build();
    }

    // The device that this fragment represents
    public static final String ARG_DEVICE = "ARG_DEVICE";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View top = inflater.inflate(R.layout.fragment_data, container, false);
        ParticleDevice device = requireNonNull(getArguments()).getParcelable(ARG_DEVICE);

        RecyclerView rv = Ui.findView(top, R.id.data_list);
        rv.setHasFixedSize(true);  // perf. optimization
        LinearLayoutManager layoutManager = new LinearLayoutManager(inflater.getContext());
        rv.setLayoutManager(layoutManager);
        DataListAdapter adapter = new DataListAdapter(requireNonNull(device));
        rv.setAdapter(adapter);
        rv.addItemDecoration(new DividerItemDecoration(requireNonNull(getContext()), LinearLayout.VERTICAL));
        return top;
    }

    private static class Variable {
        final String name;
        final ParticleDevice.VariableType variableType;

        Variable(String name, ParticleDevice.VariableType variableType) {
            this.name = name;
            this.variableType = variableType;
        }
    }

    private static class Function {
        final String name;

        Function(String name) {
            this.name = name;
        }
    }

    private static class DataListAdapter extends RecyclerView.Adapter<DataListAdapter.BaseViewHolder> {
        static final int HEADER = 0;
        static final int FUNCTION = 1;
        static final int VARIABLE = 2;

        static class BaseViewHolder extends RecyclerView.ViewHolder {
            final View topLevel;

            BaseViewHolder(View itemView) {
                super(itemView);
                topLevel = itemView;
            }
        }

        static class HeaderViewHolder extends BaseViewHolder {
            final TextView headerText, emptyText;

            HeaderViewHolder(View itemView) {
                super(itemView);
                headerText = Ui.findView(itemView, R.id.header_text);
                emptyText = Ui.findView(itemView, R.id.header_empty);
            }
        }

        static class FunctionViewHolder extends BaseViewHolder {
            final TextView name, value;
            final EditText argument;
            final ImageView toggle, argumentIcon;
            final ProgressBar progressBar;

            FunctionViewHolder(View itemView) {
                super(itemView);
                name = Ui.findView(itemView, R.id.function_name);
                value = Ui.findView(itemView, R.id.function_value);
                argument = Ui.findView(itemView, R.id.function_argument);
                argumentIcon = Ui.findView(itemView, R.id.function_argument_icon);
                toggle = Ui.findView(itemView, R.id.function_toggle);
                progressBar = Ui.findView(itemView, R.id.function_progress);
            }
        }

        static class VariableViewHolder extends BaseViewHolder {
            final TextView name, type, value;
            final ProgressBar progressBar;

            VariableViewHolder(View itemView) {
                super(itemView);
                name = Ui.findView(itemView, R.id.variable_name);
                type = Ui.findView(itemView, R.id.variable_type);
                value = Ui.findView(itemView, R.id.variable_value);
                progressBar = Ui.findView(itemView, R.id.variable_progress);
            }
        }

        private final List<Object> data = list();
        private Drawable defaultBackground;
        private final ParticleDevice device;

        DataListAdapter(ParticleDevice device) {
            this.device = device;
            data.add("Particle.function()");
            for (String function : device.getFunctions()) {
                data.add(new Function(function));
            }
            data.add("Particle.variable()");
            for (Map.Entry<String, ParticleDevice.VariableType> entry : device.getVariables().entrySet()) {
                data.add(new Variable(entry.getKey(), entry.getValue()));
            }
        }

        @NonNull
        @Override
        public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case FUNCTION: {
                    View v = LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_function_list, parent, false);
                    return new FunctionViewHolder(v);
                }
                case VARIABLE: {
                    View v = LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.row_variable_list, parent, false);
                    return new VariableViewHolder(v);
                }
                default: {
                    View v = LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.data_header_list, parent, false);
                    return new HeaderViewHolder(v);
                }
            }
        }

        @Override
        public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
            if (defaultBackground == null) {
                defaultBackground = holder.topLevel.getBackground();
            }

            if (Build.VERSION.SDK_INT >= 16) {
                holder.topLevel.setBackground(defaultBackground);
            } else {
                //noinspection deprecation
                holder.topLevel.setBackgroundDrawable(defaultBackground);
            }

            switch (getItemViewType(position)) {
                case HEADER:
                    String header = (String) data.get(position);
                    HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
                    headerViewHolder.headerText.setText(header);
                    //check if there's any data
                    if (device.getVariables().size() == 0 && position != 0) {
                        headerViewHolder.emptyText.setText(R.string.no_exposed_variable_msg);
                        headerViewHolder.emptyText.setVisibility(View.VISIBLE);
                    } else if (device.getFunctions().size() == 0 && position == 0) {
                        headerViewHolder.emptyText.setText(R.string.no_exposed_function_msg);
                        headerViewHolder.emptyText.setVisibility(View.VISIBLE);
                    } else {
                        headerViewHolder.emptyText.setVisibility(View.GONE);
                    }
                    break;
                case VARIABLE:
                    Variable variable = (Variable) data.get(position);
                    VariableViewHolder variableViewHolder = (VariableViewHolder) holder;
                    variableViewHolder.name.setText(variable.name);
                    variableViewHolder.topLevel.setBackgroundResource(R.color.device_item_bg);
                    setupVariableType(variableViewHolder, variable);
                    setupVariableValue(variableViewHolder, variable);
                    variableViewHolder.name.setOnClickListener(v -> setupVariableValue(variableViewHolder, variable));
                    variableViewHolder.type.setOnClickListener(v -> setupVariableValue(variableViewHolder, variable));
                    break;
                case FUNCTION:
                    Function function = (Function) data.get(position);
                    FunctionViewHolder functionViewHolder = (FunctionViewHolder) holder;
                    functionViewHolder.name.setText(function.name);
                    functionViewHolder.topLevel.setBackgroundResource(R.color.device_item_bg);
                    setupArgumentSend(functionViewHolder, function);
                    setupArgumentExpandAndCollapse(functionViewHolder);
                    break;
            }
        }

        private void createValuePopup(Context context, String title, String message) {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.action_clipboard, (dialog, which) -> {
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(title, message);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                        }
                        Toast.makeText(context, R.string.clipboard_copy_variable, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.close, ((dialogInterface, i) -> dialogInterface.dismiss()))
                    .show();
        }

        private void setupArgumentExpandAndCollapse(FunctionViewHolder functionViewHolder) {
            functionViewHolder.toggle.setOnClickListener(v -> {
                if (functionViewHolder.argument.getVisibility() == View.VISIBLE) {
                    AnimationUtil.collapse(functionViewHolder.argument);
                    AnimationUtil.collapse(functionViewHolder.argumentIcon);
                    functionViewHolder.toggle.setImageResource(R.drawable.ic_expand);
                } else {
                    AnimationUtil.expand(functionViewHolder.argument);
                    AnimationUtil.expand(functionViewHolder.argumentIcon);
                    functionViewHolder.toggle.setImageResource(R.drawable.ic_collapse);
                }
            });
        }

        private void setupArgumentSend(FunctionViewHolder holder, Function function) {
            Context context = holder.itemView.getContext();
            holder.argument.setOnEditorActionListener((v, actionId, event) -> {

                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    holder.progressBar.setVisibility(View.VISIBLE);
                    holder.value.setVisibility(View.GONE);

                    try {
                        Async.executeAsync(device, new Async.ApiWork<ParticleDevice, Integer>() {
                            @Override
                            public Integer callApi(@NonNull ParticleDevice particleDevice)
                                    throws ParticleCloudException, IOException {
                                try {
                                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

                                    if (imm != null) {
                                        imm.hideSoftInputFromWindow(holder.argument.getWindowToken(), 0);
                                    }
                                    return particleDevice.callFunction(function.name, new ArrayList<>(Collections.
                                            singletonList(holder.argument.getText().toString())));
                                } catch (ParticleDevice.FunctionDoesNotExistException | IllegalArgumentException e) {
                                    e.printStackTrace();
                                }
                                return -1;
                            }

                            @Override
                            public void onSuccess(@NonNull Integer value) {
                                holder.value.setText(String.valueOf(value));
                                holder.progressBar.setVisibility(View.GONE);
                                holder.value.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onFailure(@NonNull ParticleCloudException exception) {
                                holder.value.setText("");
                                Toast.makeText(context, R.string.sending_argument_failed, Toast.LENGTH_SHORT).show();
                                holder.progressBar.setVisibility(View.GONE);
                                holder.value.setVisibility(View.VISIBLE);
                            }
                        });
                    } catch (ParticleCloudException e) {
                        holder.value.setText("");
                        Toast.makeText(context, R.string.sending_argument_failed, Toast.LENGTH_SHORT).show();
                        holder.progressBar.setVisibility(View.GONE);
                        holder.value.setVisibility(View.VISIBLE);
                    }
                    return true;
                }
                return false;
            });
        }

        private void setupVariableValue(VariableViewHolder holder, Variable variable) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.value.setVisibility(View.GONE);
            try {
                Async.executeAsync(device, new Async.ApiWork<ParticleDevice, String>() {
                    @Override
                    public String callApi(@NonNull ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                        try {
                            if (variable.variableType == ParticleDevice.VariableType.INT) {
                                String value = String.valueOf(device.getVariable(variable.name));
                                int dotIndex = value.indexOf(".");
                                return value.substring(0, dotIndex > 0 ? dotIndex : value.length());
                            } else {
                                return String.valueOf(device.getVariable(variable.name));
                            }
                        } catch (ParticleDevice.VariableDoesNotExistException e) {
                            throw new ParticleCloudException(e);
                        }
                    }

                    @Override
                    public void onSuccess(@NonNull String value) {
                        holder.value.setText(value);
                        holder.progressBar.setVisibility(View.GONE);
                        holder.value.setVisibility(View.VISIBLE);
                        holder.value.setOnClickListener(view -> createValuePopup(view.getContext(), variable.name, value));
                    }

                    @Override
                    public void onFailure(@NonNull ParticleCloudException exception) {
                        holder.value.setText("");
                        holder.progressBar.setVisibility(View.GONE);
                        holder.value.setVisibility(View.VISIBLE);
                    }
                });
            } catch (ParticleCloudException e) {
                holder.value.setText("");
                holder.progressBar.setVisibility(View.GONE);
                holder.value.setVisibility(View.VISIBLE);
            }
        }

        private void setupVariableType(VariableViewHolder holder, Variable variable) {
            String type = "";
            switch (variable.variableType) {
                case INT:
                    type = "(Integer)";
                    break;
                case DOUBLE:
                    type = "(Double)";
                    break;
                case STRING:
                    type = "(String)";
                    break;
            }
            holder.type.setText(type);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public int getItemViewType(int position) {
            Object item = data.get(position);
            if (item instanceof Variable) {
                return VARIABLE;
            } else if (item instanceof Function) {
                return FUNCTION;
            } else {
                return HEADER;
            }
        }
    }
}
