package io.particle.android.sdk.ui.devicelist

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText


fun afterTextChangedListener(afterTextChange: (Editable?) -> Unit): TextWatcher {
    return object : TextWatcher {

        override fun afterTextChanged(editable: Editable?) {
            afterTextChange(editable)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    }
}
