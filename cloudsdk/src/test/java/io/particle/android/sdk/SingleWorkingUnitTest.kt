package io.particle.android.sdk

import io.particle.android.sdk.utils.UnknownEnumIntValueException
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun messageIsAsExpected() {
        val ex = UnknownEnumIntValueException(8)
        assertEquals("Unknown enum value for 8", ex.message)
    }

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
