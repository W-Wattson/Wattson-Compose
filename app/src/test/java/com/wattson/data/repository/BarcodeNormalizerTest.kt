package com.wattson.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeNormalizerTest {

    @Test
    fun `returns EAN-13 unchanged`() {
        assertEquals("1234567890123", BarcodeNormalizer.normalizeToEan13("1234567890123"))
    }

    @Test
    fun `converts UPC-A to EAN-13`() {
        assertEquals("0123456789012", BarcodeNormalizer.normalizeToEan13("123456789012"))
    }

    @Test
    fun `converts EAN-8 to EAN-13`() {
        assertEquals("0000012345678", BarcodeNormalizer.normalizeToEan13("12345678"))
    }

    @Test
    fun `rejects non numeric or unsupported barcodes`() {
        assertNull(BarcodeNormalizer.normalizeToEan13("ABC123"))
        assertNull(BarcodeNormalizer.normalizeToEan13("12345"))
    }
}
