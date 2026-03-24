package com.wattson.domain.model

data class ResolvedEprelProduct(
    val category: String,
    val registrationNumber: String,
    val product: Product
)

val DEFAULT_EPREL_CATEGORY_IDS = listOf(
    "lightsources",
    "electronicdisplays",
    "washingmachines2019",
    "washerdryers",
    "dishwashers2019",
    "refrigeratingappliances2019",
    "tumbledryers",
    "ovens",
    "rangehoods",
    "airconditioners",
    "tyres",
    "spaceheaters",
    "waterheaters",
    "electronicdisplays20232766",
    "smartphonestablets20231669"
)
