package io.particle.android.sdk.accountsetup;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.squareup.phrase.Phrase;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.cloud.SDKGlobals;
import io.particle.android.sdk.cloud.exceptions.ParticleLoginException;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.ui.NextActivitySelector;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;

import static io.particle.android.sdk.utils.Py.truthy;


public class LoginActivity extends BaseActivity {

    private static final TLog log = TLog.get(LoginActivity.class);

    /**
     * Keep track of the login task to ensure we can cancel it if requested, ensure against
     * duplicate requests, etc.
     */
    private Async.AsyncApiWorker<ParticleCloud, Void> loginTask = null;

    // UI references.
    @BindView(R2.id.email)
    protected EditText emailView;
    @BindView(R2.id.password)
    protected EditText passwordView;

    @OnEditorAction(R2.id.password)
    protected boolean onPasswordEditorAction(int id) {
        if (id == R.id.action_log_in || id == EditorInfo.IME_NULL) {
            attemptLogin();
            return true;
        }
        return false;
    }

    @OnTextChanged(value = {R2.id.email, R2.id.password}, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    protected void afterInput() {
        emailView.setError(null);
        passwordView.setError(null);
    }

    @Inject
    protected ParticleCloud sparkCloud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.particle_activity_login);

        ParticleDeviceSetupLibrary
                .getInstance()
                .getApplicationComponent()
                .activityComponentBuilder()
                .apModule(new ApModule())
                .build()
                .inject(this);

        ButterKnife.bind(this);
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(this);
        SEGAnalytics.screen("Auth: Login Screen");

        Ui.setText(this, R.id.log_in_header_text,
                Phrase.from(this, R.string.log_in_header_text)
                        .put("brand_name", getString(R.string.brand_name))
                        .format()
        );

        Ui.setTextFromHtml(this, R.id.user_has_no_account, R.string.msg_no_account)
                .setOnClickListener(v -> {
                    startActivity(new Intent(v.getContext(), CreateAccountActivity.class));
                    finish();
                });

        Ui.setTextFromHtml(this, R.id.forgot_password, R.string.msg_forgot_password);
    }

    public void onPasswordResetClicked(View v) {
        Intent intent;
        intent = PasswordResetActivity.buildIntent(this, emailView.getText().toString());
        startActivity(intent);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    @OnClick(R2.id.action_log_in)
    public void attemptLogin() {
        if (loginTask != null) {
            log.wtf("Login being attempted again even though the button isn't enabled?!");
            return;
        }

        // Reset errors.
        emailView.setError(null);
        passwordView.setError(null);

        // Store values at the time of the login attempt.
        final String email = emailView.getText().toString();
        final String password = passwordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            passwordView.setError(getString(R.string.error_invalid_password));
            focusView = passwordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            emailView.setError(getString(R.string.error_field_required));
            focusView = emailView;
            cancel = true;

        } else if (!isEmailValid(email)) {
            emailView.setError(getString(R.string.error_invalid_email));
            focusView = emailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            login(email, password);
        }
    }

    /**
     * Attempts to sign in the account specified by the login form.
     */
    private void login(String email, String password) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        ParticleUi.showParticleButtonProgress(this, R.id.action_log_in, true);
        loginTask = Async.executeAsync(sparkCloud, new Async.ApiWork<ParticleCloud, Void>() {
            @Override
            public Void callApi(@NonNull ParticleCloud sparkCloud) throws ParticleCloudException {
                sparkCloud.logIn(email, password);
                return null;
            }

            @Override
            public void onTaskFinished() {
                loginTask = null;
            }

            @Override
            public void onSuccess(@NonNull Void result) {
                SEGAnalytics.identify(email);
                SEGAnalytics.track("Auth: Login success");
                log.d("Logged in...");
                if (isFinishing()) {
                    return;
                }
                startActivity(NextActivitySelector.getNextActivityIntent(
                        LoginActivity.this,
                        sparkCloud,
                        SDKGlobals.getSensitiveDataStorage(),
                        null));
                finish();
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException error) {
                ParticleUi.showParticleButtonProgress(LoginActivity.this, R.id.action_log_in, false);
                ParticleLoginException loginException = (ParticleLoginException) error;

                if (loginException.getMfaToken() != null) {
                    Intent intent = TwoFactorActivity.buildIntent(LoginActivity.this, email,
                            password, loginException.getMfaToken());
                    startActivity(intent);
                } else {
                    log.d("onFailed(): " + error.getMessage());
                    SEGAnalytics.track("Auth: Login failure");
                    // FIXME: check specifically for 401 errors
                    // and set a better error message?  (Seems like
                    // this works fine already...)
                    passwordView.setError(error.getBestMessage());
                    passwordView.requestFocus();
                }
            }
        });
    }

    private boolean isEmailValid(String email) {
        return truthy(email) && email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return (password.length() > 0);
    }

}