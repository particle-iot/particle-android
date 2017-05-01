package io.particle.android.sdk.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.f2prateek.bundler.FragmentBundlerCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.AnimationUtil;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

import static io.particle.android.sdk.utils.Py.list;

public class DataFragment extends Fragment {

    public static DataFragment newInstance(ParticleDevice device) {
        return FragmentBundlerCompat.create(new DataFragment())
                .put(DataFragment.ARG_DEVICE, device)
                .build();
    }

    // The device that this fragment represents
    public static final String ARG_DEVICE = "ARG_DEVICE";

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View top = inflater.inflate(R.layout.fragment_data, container, false);
        ParticleDevice device = getArguments().getParcelable(ARG_DEVICE);

        RecyclerView rv = Ui.findView(top, R.id.data_list);
        rv.setHasFixedSize(true);  // perf. optimization
        LinearLayoutManager layoutManager = new LinearLayoutManager(inflater.getContext());
        rv.setLayoutManager(layoutManager);
        DataListAdapter adapter = new DataListAdapter(device);
        rv.setAdapter(adapter);
        return top;
    }

    private static class Variable {
        String name;
        ParticleDevice.VariableType variableType;

        Variable(String name, ParticleDevice.VariableType variableType) {
            this.name = name;
            this.variableType = variableType;
        }
    }

    private static class Function {
        String name;

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
            final TextView headerText;

            HeaderViewHolder(View itemView) {
                super(itemView);
                headerText = Ui.findView(itemView, R.id.header_text);
            }
        }

        static class FunctionViewHolder extends BaseViewHolder {
            final TextView name, value;
            final EditText argument;
            final ImageView toggle;

            FunctionViewHolder(View itemView) {
                super(itemView);
                name = Ui.findView(itemView, R.id.function_name);
                value = Ui.findView(itemView, R.id.function_value);
                argument = Ui.findView(itemView, R.id.function_argument);
                toggle = Ui.findView(itemView, R.id.function_toggle);
            }
        }

        static class VariableViewHolder extends BaseViewHolder {
            final TextView name, type, value;

            VariableViewHolder(View itemView) {
                super(itemView);
                name = Ui.findView(itemView, R.id.variable_name);
                type = Ui.findView(itemView, R.id.variable_type);
                value = Ui.findView(itemView, R.id.variable_value);
            }
        }

        private final List<Object> data = list();
        private Drawable defaultBackground;
        private ParticleDevice device;

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

        @Override
        public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == FUNCTION) {
                View v = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_function_list, parent, false);
                return new FunctionViewHolder(v);
            } else if (viewType == VARIABLE) {
                View v = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.row_variable_list, parent, false);
                return new VariableViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.data_header_list, parent, false);
                return new HeaderViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(BaseViewHolder holder, int position) {
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
                    headerViewHolder.topLevel.setBackgroundResource(R.color.shaded_background);
                    break;
                case VARIABLE:
                    Variable variable = (Variable) data.get(position);
                    VariableViewHolder variableViewHolder = (VariableViewHolder) holder;
                    variableViewHolder.name.setText(variable.name);
                    setupVariableType(variableViewHolder, variable);
                    setupVariableValue(variableViewHolder, variable);
                    variableViewHolder.itemView.setOnClickListener(v -> setupVariableValue(variableViewHolder, variable));
                    break;
                case FUNCTION:
                    Function function = (Function) data.get(position);
                    FunctionViewHolder functionViewHolder = (FunctionViewHolder) holder;
                    functionViewHolder.name.setText(function.name);
                    setupArgumentSend(functionViewHolder, function);
                    setupArgumentExpandAndCollapse(functionViewHolder);
                    break;
            }
        }

        private void setupArgumentExpandAndCollapse(FunctionViewHolder functionViewHolder) {
            functionViewHolder.toggle.setOnClickListener(v -> {
                if (functionViewHolder.argument.getVisibility() == View.VISIBLE) {
                    AnimationUtil.collapse(functionViewHolder.argument);
                    functionViewHolder.toggle.setImageResource(R.drawable.ic_expand);
                } else {
                    AnimationUtil.expand(functionViewHolder.argument);
                    functionViewHolder.toggle.setImageResource(R.drawable.ic_collapse);
                }
            });
        }

        private void setupArgumentSend(FunctionViewHolder holder, Function function) {
            Context context = holder.itemView.getContext();
            holder.argument.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    Async.executeAsync(device, new Async.ApiWork<ParticleDevice, Integer>() {
                        @Override
                        public Integer callApi(@NonNull ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                            try {
                                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(holder.argument.getWindowToken(), 0);
                                return particleDevice.callFunction(function.name, new ArrayList<>(Collections.
                                        singletonList(holder.argument.getText().toString())));
                            } catch (ParticleDevice.FunctionDoesNotExistException e) {
                                e.printStackTrace();
                            }
                            return -1;
                        }

                        @Override
                        public void onSuccess(@NonNull Integer value) {
                            holder.value.setText(String.valueOf(value));
                        }

                        @Override
                        public void onFailure(@NonNull ParticleCloudException exception) {
                            holder.value.setText("");
                            Toast.makeText(context, R.string.sending_argument_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                    return true;
                }
                return false;
            });
        }

        private void setupVariableValue(VariableViewHolder holder, Variable variable) {
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
                }

                @Override
                public void onFailure(@NonNull ParticleCloudException exception) {
                    holder.value.setText("");
                }
            });
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
