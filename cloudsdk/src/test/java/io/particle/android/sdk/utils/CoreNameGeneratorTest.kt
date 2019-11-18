package io.particle.android.sdk.utils

import org.junit.Test
import org.junit.Assert.*


class CoreNameGeneratorTest {

    @Test
    fun nameNotNull() {
        val name = CoreNameGenerator.generateUniqueName(setOf())
        assertNotNull(name)
    }

    @Test
    fun nameContainsUnderscore() {
        val name = CoreNameGenerator.generateUniqueName(setOf())
        assertTrue(name.contains("_"))
    }

    @Test
    fun nameSegmentsNotRepeated() {
        val name = CoreNameGenerator.generateUniqueName(setOf())
        val (first ,second) = name.split("_")
        assertNotEquals(first, second)
    }

}