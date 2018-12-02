package io.particle.mesh.setup.ui

import io.particle.mesh.setup.ui.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.ui.BarcodeData.PartialBarcodeData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test


private const val SERIAL_NUM = "XENHAB838ABCDEF"
private const val FULL_MOBILE_SECRET_SIZE = 15
private const val FULL_MOBILE_SECRET = "123456789012345"


class BarcodeDataTest {

    @Before
    fun verifyTestData() {
        assertEquals(FULL_MOBILE_SECRET.length, FULL_MOBILE_SECRET_SIZE)
    }

    @Test
    fun returnFullBarcodeData() {
        val barcode = buildBarcode(FULL_MOBILE_SECRET)
        assertEquals(barcode!!.javaClass, CompleteBarcodeData::class.java)
    }

    @Test
    fun returnPartialBarcodeData() {
        val barcode = buildBarcode("abcd")
        assertEquals(barcode!!.javaClass, PartialBarcodeData::class.java)
    }

    @Test
    fun returnNullForTooSmallSerialNumber() {
        val barcode = BarcodeData.fromRawData("abc $FULL_MOBILE_SECRET")
        assertNull(barcode)
    }

    @Test
    fun returnNullForTooLargeSerialNumber() {
        val barcode = BarcodeData.fromRawData("12345678901234567890 $FULL_MOBILE_SECRET")
        assertNull(barcode)
    }

    @Test
    fun returnNullForLackOfSpaceCharacter() {
        val barcode = BarcodeData.fromRawData(SERIAL_NUM + FULL_MOBILE_SECRET)
        assertNull(barcode)
    }

    @Test
    fun returnNullForTooLongMobileSecret() {
        val barcode = buildBarcode(FULL_MOBILE_SECRET + "123")
        assertNull(barcode)
    }

    private fun buildBarcode(
        mobileSecret: String,
        serialNumber: String = SERIAL_NUM
    ): BarcodeData? {
        return BarcodeData.fromRawData("$serialNumber $mobileSecret")
    }
}
