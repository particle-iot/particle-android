package io.particle.mesh.setup

import org.junit.Assert.assertTrue
import org.junit.Test


class WiFiStrengthTest {

    @Test
    fun fromIntReturnsCorrectEnum() {
        val results = mapOf(
            0 to WiFiStrength.STRONG,
            -55 to WiFiStrength.STRONG,
            -56 to WiFiStrength.MEDIUM,
            -71 to WiFiStrength.MEDIUM,
            -72 to WiFiStrength.WEAK
        )

        for ((input, expectedResult) in results.entries) {
            val actual = WiFiStrength.fromInt(input)
            assertTrue(
                "Bad WiFiStrength result. value=$input, expected=$expectedResult, actual=$actual",
                expectedResult == actual
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun positiveIntThrowsException() {
        WiFiStrength.fromInt(1)
    }

}