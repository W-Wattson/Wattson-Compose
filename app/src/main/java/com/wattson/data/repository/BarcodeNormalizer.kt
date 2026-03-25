package com.wattson.data.repository

/**
 * Normalizes supported barcode formats to the EAN-13 shape expected by the backend.
 */
internal object BarcodeNormalizer {
    /**
     * Converts UPC-A and EAN-8 barcodes into EAN-13 while rejecting unsupported inputs.
     *
     * Only numeric barcodes are accepted.
     */
    fun normalizeToEan13(barcode: String): String? {
        if (!barcode.all(Char::isDigit)) {
            return null
        }

        return when (barcode.length) {
            13 -> barcode
            12 -> "0$barcode"
            8 -> "00000$barcode"
            else -> null
        }
    }
}
