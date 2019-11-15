package io.particle.android.sdk.utils

import org.junit.Test

import org.junit.Assert.*


class LangUtilsTest {

    @Test
    fun passDoesNothing() {
        // ensure that we actually hit the "pass" line but that it does nothing
        var foo = false
        pass
        foo = true
        assertTrue(foo)
    }

}
