package io.particle.android.sdk.ui;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.f2prateek.bundler.FragmentBundlerCompat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
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

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
            final TextView name;

            FunctionViewHolder(View itemView) {
                super(itemView);
                name = Ui.findView(itemView, R.id.function_name);
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

            if (position % 2 == 0) {
                holder.topLevel.setBackgroundResource(R.color.shaded_background);
            } else {
                if (Build.VERSION.SDK_INT >= 16) {
                    holder.topLevel.setBackground(defaultBackground);
                } else {
                    //noinspection deprecation
                    holder.topLevel.setBackgroundDrawable(defaultBackground);
                }
            }

            switch (getItemViewType(position)) {
                case HEADER:
                    String header = (String) data.get(position);
                    HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
                    headerViewHolder.headerText.setText(header);
                    break;
                case VARIABLE:
                    Variable variable = (Variable) data.get(position);
                    VariableViewHolder variableViewHolder = (VariableViewHolder) holder;
                    variableViewHolder.name.setText(variable.name);
                    setupVariableType(variableViewHolder, variable);
                    setupVariableValue(variableViewHolder, variable);
                    break;
                case FUNCTION:
                    Function function = (Function) data.get(position);
                    FunctionViewHolder functionViewHolder = (FunctionViewHolder) holder;
                    functionViewHolder.name.setText(function.name);
                    break;
            }
        }

        private void setupVariableValue(VariableViewHolder holder, Variable variable) {
            Async.executeAsync(device, new Async.ApiWork<ParticleDevice, String>() {
                @Override
                public String callApi(@NonNull ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                    try {
                        return String.valueOf(device.getVariable(variable.name));
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
