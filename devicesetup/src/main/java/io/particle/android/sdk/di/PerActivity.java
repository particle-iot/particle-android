package io.particle.android.sdk.di;

import androidx.annotation.RestrictTo;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Scope;

@Scope
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@RestrictTo({RestrictTo.Scope.LIBRARY})
public @interface PerActivity {
}