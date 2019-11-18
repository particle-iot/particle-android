package io.particle.android.sdk.utils

import org.junit.Assert.assertTrue
import org.junit.Test


class PyTest {

    @Test
    fun emptyListReturnsEmptyList() {
        val emptyList =  Py.list<Any>()
        assertTrue(emptyList.isEmpty())
    }

}