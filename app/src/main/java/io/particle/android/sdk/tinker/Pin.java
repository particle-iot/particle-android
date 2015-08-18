package io.particle.android.sdk.tinker;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.FloatEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

import static io.particle.android.sdk.utils.Py.list;


public class Pin {

    private static final int ANALOG_WRITE_MAX = 255;
    private static final int ANALOG_READ_MAX = 4095;

    public final TextView view;
    public final String name;
    public final String label;

    private final PinType pinType;
    private final Set<PinAction> functions;
    private final int maxAnalogWriteValue;

    private boolean muted = false;
    private PinAction configuredAction;
    private ObjectAnimator pinBackgroundAnim;
    private Animator endAnimation;

    private View analogReadView;
    private View analogWriteView;
    private View digitalWriteView;
    private View digitalReadView;

    private int analogValue = 0;
    private DigitalValue digitalValue;

    public Pin(TextView view, PinType pinType, String name, EnumSet<PinAction> functions) {
        this(view, pinType, name, functions, name, ANALOG_WRITE_MAX);
    }

    // for some pins, the label and the name are not the same, and on A3 and DAC, the
    // analog write value is different
    public Pin(TextView view, PinType pinType, String name, EnumSet<PinAction> functions,
               String label, int maxAnalogWriteValue) {
        this.view = view;
        this.pinType = pinType;
        this.name = name;
        this.maxAnalogWriteValue = maxAnalogWriteValue;
        this.configuredAction = PinAction.NONE;
        this.label = label;
        this.functions = Collections.unmodifiableSet(functions);
        this.view.setText(label);
        reset();
    }

    public Set<PinAction> getFunctions() {
        // made immutable in the constructor, doesn't need a defensive copy
        return functions;
    }

    public int getAnalogValue() {
        return analogValue;
    }

    public void reset() {
        if (analogReadView != null) {
            analogReadView.setVisibility(View.GONE);
            // Reset the values
            ProgressBar barGraph = Ui.findView(analogReadView,
                    R.id.tinker_analog_read_progress);
            TextView readValue = Ui.findView(analogReadView,
                    R.id.tinker_analog_read_value);
            barGraph.setMax(100);
            barGraph.setProgress(0);
            readValue.setText("0");
            analogReadView = null;
        }
        if (analogWriteView != null) {
            // Reset the values
            analogWriteView.setVisibility(View.GONE);
            final SeekBar seekBar = Ui.findView(analogWriteView,
                    R.id.tinker_analog_write_seekbar);
            final TextView value = Ui.findView(analogWriteView,
                    R.id.tinker_analog_write_value);
            seekBar.setProgress(0);
            value.setText("0");
            analogWriteView = null;
        }
        if (digitalWriteView != null) {
            digitalWriteView.setVisibility(View.GONE);
            digitalWriteView = null;
        }
        if (digitalReadView != null) {
            digitalReadView.setVisibility(View.GONE);
            digitalReadView = null;
        }
        if (!stopAnimating()) {
            ((View) view.getParent()).setBackgroundColor(0);
        }
        muted = false;
        analogValue = 0;
        digitalValue = DigitalValue.NONE;
    }

    private void updatePinColor() {
        view.setTextColor(view.getContext().getResources().getColor(android.R.color.white));

        switch (configuredAction) {
            case ANALOG_READ:
                view.setBackgroundResource(R.drawable.tinker_pin_emerald);
                break;
            case ANALOG_WRITE:
                view.setBackgroundResource(R.drawable.tinker_pin_sunflower);
                break;
            case DIGITAL_READ:
                if (digitalValue == DigitalValue.HIGH) {
                    view.setBackgroundResource(R.drawable.tinker_pin_read_high);
                    view.setTextColor(view.getContext().getResources()
                            .getColor(R.color.tinker_pin_text_dark));
                } else {
                    view.setBackgroundResource(R.drawable.tinker_pin_cyan);
                }
                break;
            case DIGITAL_WRITE:
                if (digitalValue == DigitalValue.HIGH) {
                    view.setBackgroundResource(R.drawable.tinker_pin_write_high);
                    view.setTextColor(view.getContext().getResources()
                            .getColor(R.color.tinker_pin_text_dark));
                } else {
                    view.setBackgroundResource(R.drawable.tinker_pin_alizarin);
                }
                break;
            case NONE:
                view.setBackgroundResource(R.drawable.tinker_pin);
                break;
        }
    }

    public PinAction getConfiguredAction() {
        return configuredAction;
    }

    public void setConfiguredAction(PinAction action) {
        this.configuredAction = action;
        // Clear out any views
        updatePinColor();
    }

    public boolean isAnalogWriteMode() {
        return (analogWriteView != null && analogWriteView.getVisibility() == View.VISIBLE);
    }

    public void mute() {
        muted = true;
        view.setBackgroundResource(R.drawable.tinker_pin_muted);
        view.setTextColor(view.getContext().getResources().getColor(
                R.color.tinker_pin_text_muted));
        hideExtraViews();
    }

    public void unmute() {
        muted = false;
        updatePinColor();
        showExtraViews();
    }

    private void hideExtraViews() {
        if (analogReadView != null) {
            analogReadView.setVisibility(View.GONE);
        }
        if (analogWriteView != null) {
            analogWriteView.setVisibility(View.GONE);
        }
        if (digitalWriteView != null) {
            digitalWriteView.setVisibility(View.GONE);
        }
        if (digitalReadView != null) {
            digitalReadView.setVisibility(View.GONE);
        }

        if (pinBackgroundAnim != null) {
            pinBackgroundAnim.end();
        }
        View parent = (View) view.getParent();
        parent.setBackgroundColor(0);
    }

    private void showExtraViews() {
        if (analogReadView != null) {
            analogReadView.setVisibility(View.VISIBLE);
        } else if (analogWriteView != null) {
            analogWriteView.setVisibility(View.VISIBLE);
        } else if (digitalWriteView != null) {
            digitalWriteView.setVisibility(View.VISIBLE);
        } else if (digitalReadView != null) {
            digitalReadView.setVisibility(View.VISIBLE);
        }
    }

    public void showAnalogValue(int value) {
        analogValue = value;
        doShowAnalogValue(value);
    }

    private void doShowAnalogValue(int newValue) {
        if (analogWriteView != null) {
            analogWriteView.setVisibility(View.GONE);
            analogWriteView = null;
        }

        ViewGroup parent = (ViewGroup) view.getParent();

        if (pinBackgroundAnim != null) {
            pinBackgroundAnim.cancel();
        }

        if (analogReadView == null) {
            analogReadView = Ui.findView(parent, R.id.tinker_analog_read_main);
        }

        // If the view does not exist, inflate it
        if (analogReadView == null) {
            LayoutInflater inflater = (LayoutInflater) view.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (pinType == PinType.A) {
                analogReadView = inflater.inflate(R.layout.tinker_analog_read_left, parent, false);
                parent.addView(analogReadView);
            } else if (pinType == PinType.D) {
                analogReadView = inflater.inflate(R.layout.tinker_analog_read_right, parent, false);
                parent.addView(analogReadView, 0);
            }
        }

        analogReadView.setVisibility(View.VISIBLE);
        // Find the existing views and set the values
        ProgressBar barGraph = Ui.findView(analogReadView,
                R.id.tinker_analog_read_progress);
        TextView readValue = Ui.findView(analogReadView,
                R.id.tinker_analog_read_value);

        if (PinAction.ANALOG_READ.equals(configuredAction)) {
            barGraph.setProgressDrawable(ContextCompat.getDrawable(
                    view.getContext(), R.drawable.progress_emerald));
        } else {
            barGraph.setProgressDrawable(ContextCompat.getDrawable(
                    view.getContext(), R.drawable.progress_sunflower));
        }

        int max = getAnalogMax();
        barGraph.setMax(max);
        barGraph.setProgress(newValue);
        readValue.setText(String.valueOf(newValue));
    }

    public void showAnalogWrite(final OnAnalogWriteListener listener) {
        if (analogReadView != null) {
            analogReadView.setVisibility(View.GONE);
            analogReadView = null;
        }

        final ViewGroup parent = (ViewGroup) view.getParent();
        if (analogWriteView == null) {
            analogWriteView = Ui.findView(parent, R.id.tinker_analog_write_main);
        }

        // If the view does not exist, inflate it
        if (analogWriteView == null) {
            LayoutInflater inflater = (LayoutInflater) view.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (pinType == PinType.A) {
                analogWriteView = inflater.inflate(R.layout.tinker_analog_write_left,
                        parent, false);
                parent.addView(analogWriteView);
            } else if (pinType == PinType.D) {
                analogWriteView = inflater.inflate(R.layout.tinker_analog_write_right, parent,
                        false);
                parent.addView(analogWriteView, 0);
            }
        }

        analogWriteView.setVisibility(View.VISIBLE);
        final SeekBar seekBar = Ui.findView(analogWriteView,
                R.id.tinker_analog_write_seekbar);
        final TextView valueText = Ui.findView(analogWriteView,
                R.id.tinker_analog_write_value);
        if (pinBackgroundAnim != null) {
            pinBackgroundAnim.cancel();
            pinBackgroundAnim = null;
        }
        parent.setBackgroundColor(0x4C000000);

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int value = seekBar.getProgress();
                parent.setBackgroundColor(0);
                showAnalogWriteValue();
                listener.onAnalogWrite(value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                valueText.setText(String.valueOf(progress));
            }
        });
        seekBar.setMax(getAnalogMax());
    }

    public void showAnalogWriteValue() {
        doShowAnalogValue(analogValue);
    }

    public void showDigitalWrite(DigitalValue newValue) {
        this.digitalValue = newValue;
        ViewGroup parent = (ViewGroup) view.getParent();
        if (digitalWriteView == null) {
            digitalWriteView = Ui.findView(parent, R.id.tinker_digital_write_main);
        }

        // If the view does not exist, inflate it
        if (digitalWriteView == null) {
            LayoutInflater inflater = (LayoutInflater) view.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            digitalWriteView = inflater.inflate(R.layout.tinker_digital_write, parent, false);
            if (pinType == PinType.A) {
                parent.addView(digitalWriteView);
            } else if (pinType == PinType.D) {
                parent.addView(digitalWriteView, 0);
            }
        }

        digitalWriteView.setVisibility(View.VISIBLE);
        final TextView value = Ui.findView(digitalWriteView,
                R.id.tinker_digital_write_value);
        value.setText(newValue.name());
        updatePinColor();
    }

    public void showDigitalRead(DigitalValue newValue) {
        this.digitalValue = newValue;
        ViewGroup parent = (ViewGroup) view.getParent();
        if (digitalReadView == null) {
            digitalReadView = Ui.findView(parent,
                    R.id.tinker_digital_write_main);
        }

        // If the view does not exist, inflate it
        if (digitalReadView == null) {
            LayoutInflater inflater = (LayoutInflater) view.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            digitalReadView = inflater.inflate(R.layout.tinker_digital_read, parent, false);
            if (pinType == PinType.A) {
                parent.addView(digitalReadView);
            } else if (pinType == PinType.D) {
                parent.addView(digitalReadView, 0);
            }
        }

        digitalReadView.setVisibility(View.VISIBLE);
        final TextView value = Ui.findView(digitalReadView,
                R.id.tinker_digital_read_value);
        value.setText(newValue.name());
        // fade(value, newValue);
        updatePinColor();
        if (!stopAnimating()) {
            getCancelAnimator().start();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((configuredAction == null) ? 0 : configuredAction.hashCode());
        result = prime * result + ((view == null) ? 0 : view.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Pin other = (Pin) obj;
        if (configuredAction != other.configuredAction)
            return false;
        if (view == null) {
            if (other.view != null)
                return false;
        } else if (!view.equals(other.view))
            return false;
        return true;
    }

    public DigitalValue getDigitalValue() {
        return digitalValue;
    }

    public void animateYourself() {
        final ViewGroup parent = (ViewGroup) view.getParent();

        if (pinBackgroundAnim != null) {
            pinBackgroundAnim.end();
            pinBackgroundAnim = null;
        }

        pinBackgroundAnim = (ObjectAnimator) AnimatorInflater.loadAnimator(
                view.getContext(), R.animator.pin_background_start);

        pinBackgroundAnim.setTarget(parent);
        pinBackgroundAnim.setEvaluator(new CastCheckArgbEvaluator());
        pinBackgroundAnim.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                // NO OP
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                endAnimation = getCancelAnimator();
                endAnimation.start();
            }
        });

        pinBackgroundAnim.start();
    }

    private Animator getCancelAnimator() {
        ObjectAnimator backToTransparent1 = (ObjectAnimator) AnimatorInflater
                .loadAnimator(view.getContext(), R.animator.pin_background_end);
        ObjectAnimator goDark = (ObjectAnimator) AnimatorInflater
                .loadAnimator(view.getContext(), R.animator.pin_background_go_dark);
        ObjectAnimator backToTransparent2 = (ObjectAnimator) AnimatorInflater
                .loadAnimator(view.getContext(), R.animator.pin_background_end);

        ViewGroup parent = (ViewGroup) view.getParent();
//        ArgbEvaluator evaluator = new ArgbEvaluator();
        CastCheckArgbEvaluator evaluator = new CastCheckArgbEvaluator();
        for (ObjectAnimator animator : list(backToTransparent1, goDark, backToTransparent2)) {
            animator.setTarget(parent);
            animator.setEvaluator(evaluator);
        }

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setTarget(parent);
        animatorSet.playSequentially(backToTransparent1, goDark, backToTransparent2);
        return animatorSet;
    }

    public boolean stopAnimating() {
        if (pinBackgroundAnim != null) {
            pinBackgroundAnim.cancel();
            pinBackgroundAnim = null;
            return true;
        } else {
            return false;
        }
    }

    private int getAnalogMax() {
        int max = 1;
        if (configuredAction == PinAction.ANALOG_READ) {
            max = ANALOG_READ_MAX;
        } else if (configuredAction == PinAction.ANALOG_WRITE) {
            max = maxAnalogWriteValue;
        }
        return max;
    }


    private static class CastCheckArgbEvaluator implements TypeEvaluator {

        final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
        final FloatEvaluator floatEvaluator = new FloatEvaluator();

        public Object evaluate(float fraction, Object startValue, Object endValue) {
            try {
                return argbEvaluator.evaluate(fraction, startValue, endValue);
            } catch (ClassCastException e) {
                // FIXME: broken on M
                return floatEvaluator.evaluate(fraction, (Number) startValue, (Number) endValue);
            }
        }
    }

    public interface OnAnalogWriteListener {

        void onAnalogWrite(int value);
    }
}
