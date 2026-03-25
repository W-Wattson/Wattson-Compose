package com.wattson.ui.i18n

import androidx.annotation.StringRes
import com.wattson.R
import com.wattson.domain.model.DocumentType
import com.wattson.domain.model.PreferenceType
import com.wattson.domain.model.ProductCategory
import com.wattson.domain.model.WarrantyType

@StringRes
fun ProductCategory.labelResId(): Int = when (this) {
    ProductCategory.ELECTRONIQUE -> R.string.cat_electronics
    ProductCategory.ELECTROMENAGER -> R.string.cat_appliances
    ProductCategory.ECLAIRAGE -> R.string.cat_lighting
    ProductCategory.GAMING -> R.string.cat_gaming
    ProductCategory.CLIMATISATION -> R.string.cat_climatisation
    ProductCategory.INFORMATIQUE -> R.string.cat_it
    ProductCategory.TELEPHONIE -> R.string.cat_telephony
    ProductCategory.AUDIO_VIDEO -> R.string.cat_audio_video
    ProductCategory.OTHER -> R.string.cat_other
}

@StringRes
fun DocumentType.labelResId(): Int = when (this) {
    DocumentType.FACTURE -> R.string.type_invoice
    DocumentType.GARANTIE -> R.string.type_warranty
    DocumentType.MANUEL -> R.string.type_manual
    DocumentType.OTHER -> R.string.type_other
}

@StringRes
fun WarrantyType.labelResId(): Int = when (this) {
    WarrantyType.LEGAL -> R.string.warranty_legal
    WarrantyType.MANUFACTURER -> R.string.warranty_manufacturer
    WarrantyType.EXTENDED -> R.string.warranty_extended
    WarrantyType.COMMERCIAL -> R.string.warranty_commercial
}

@StringRes
fun PreferenceType.labelResId(): Int = when (this) {
    PreferenceType.ECOLOGY -> R.string.ecology
    PreferenceType.ECONOMY -> R.string.economy
    PreferenceType.REPAIRABILITY -> R.string.repairability_pref
}
