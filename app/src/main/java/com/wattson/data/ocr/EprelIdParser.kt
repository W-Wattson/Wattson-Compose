package com.wattson.data.ocr

import com.wattson.domain.model.DEFAULT_EPREL_CATEGORY_IDS

data class OcrTextObservation(
    val label: String,
    val text: String,
    val zoneBoost: Int = 0
)

data class EprelTextExtraction(
    val candidates: List<String>,
    val rawText: String,
    val preferredCategories: List<String>
)

object EprelIdParser {

    private val qrcodeRegex = Regex(
        pattern = "(?:eprel\\.[^\\s/]+/(?:qr|label)/|/qr/|registrationNumber=)([0-9OILSBZGQ\\s./-]{4,24})",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val contextualNumberRegex = Regex(
        pattern = "(?:EPREL|REG(?:ISTRATION)?|REFERENCE|REF|QR)[^A-Z0-9]{0,10}([0-9OILSBZGQ\\s./-]{4,24})",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val looseNumberRegex = Regex(
        pattern = "(?<!\\d)([0-9OILSBZGQ][0-9OILSBZGQ\\s./-]{3,22}[0-9OILSBZGQ])(?!\\d)",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val contextualTokens = listOf("EPREL", "REG", "REGISTRATION", "REFERENCE", "REF", "QR")
    private val measurementTokens = listOf("KWH", "DB", "KG", "L/", "L ", "MIN", "H:", "CM", "M3", "W ")

    fun extractFromQrPayload(rawValue: String): String? {
        val directMatch = qrcodeRegex.find(rawValue)?.groupValues?.getOrNull(1)
        if (!directMatch.isNullOrBlank()) {
            return normalizeCandidate(directMatch)
        }

        val digitsOnly = rawValue.filter { it.isDigit() }
        return digitsOnly.takeIf(::isPlausibleRegistrationNumber)
    }

    fun extractFromObservations(observations: List<OcrTextObservation>): EprelTextExtraction {
        val candidateScores = linkedMapOf<String, Int>()
        val rawText = observations.joinToString(separator = "\n\n") { "${it.label}\n${it.text.trim()}" }

        observations.forEach { observation ->
            val lines = observation.text
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toList()

            lines.forEach { line ->
                scoreCandidates(
                    line = line,
                    zoneBoost = observation.zoneBoost,
                    scores = candidateScores
                )
            }
        }

        val sortedCandidates = candidateScores.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenByDescending { it.key.length })
            .map { it.key }
            .take(5)

        return EprelTextExtraction(
            candidates = sortedCandidates,
            rawText = rawText,
            preferredCategories = detectPreferredCategories(rawText)
        )
    }

    fun isPlausibleRegistrationNumber(value: String): Boolean {
        if (value.length !in 5..10) return false
        if (value.all { it == value.first() }) return false
        return value.any { it != '0' }
    }

    private fun scoreCandidates(
        line: String,
        zoneBoost: Int,
        scores: MutableMap<String, Int>
    ) {
        val normalizedLine = normalizeLine(line)
        val upperLine = line.uppercase()
        val hasContext = contextualTokens.any { upperLine.contains(it) }
        val hasMeasurementNoise = measurementTokens.any { upperLine.contains(it) }

        buildList {
            contextualNumberRegex.findAll(normalizedLine).forEach { add(it.groupValues[1]) }
            qrcodeRegex.findAll(normalizedLine).forEach { add(it.groupValues[1]) }
            looseNumberRegex.findAll(normalizedLine).forEach { add(it.groupValues[1]) }
        }.forEach { rawCandidate ->
            val normalizedCandidate = normalizeCandidate(rawCandidate) ?: return@forEach

            var score = zoneBoost
            score += when (normalizedCandidate.length) {
                in 6..9 -> 24
                5, 10 -> 14
                else -> 0
            }
            if (hasContext) score += 40
            if (upperLine.contains("EPREL")) score += 25
            if (hasMeasurementNoise && !hasContext) score -= 25
            if (normalizedLine.contains(rawCandidate, ignoreCase = true)) score += 5

            val currentScore = scores[normalizedCandidate] ?: 0
            scores[normalizedCandidate] = maxOf(currentScore, score) + 1
        }
    }

    private fun normalizeLine(value: String): String {
        return value
            .replace('|', '1')
            .replace('O', '0', ignoreCase = true)
            .replace('I', '1', ignoreCase = true)
            .replace('L', '1', ignoreCase = true)
            .replace('S', '5', ignoreCase = true)
            .replace('B', '8', ignoreCase = true)
            .replace('Z', '2', ignoreCase = true)
            .replace('Q', '0', ignoreCase = true)
    }

    private fun normalizeCandidate(rawValue: String): String? {
        val normalized = normalizeLine(rawValue).filter { it.isDigit() }
        return normalized.takeIf(::isPlausibleRegistrationNumber)
    }

    private fun detectPreferredCategories(rawText: String): List<String> {
        val normalized = rawText.uppercase()
        val detected = linkedSetOf<String>()

        val keywordMap = mapOf(
            "lightsources" to listOf("LIGHT", "LAMP", "LED", "SOURCE LUMINEUSE"),
            "electronicdisplays" to listOf("DISPLAY", "TV", "TELEVISION", "SCREEN", "ECRAN"),
            "electronicdisplays20232766" to listOf("DISPLAY", "TV", "TELEVISION", "SCREEN", "ECRAN"),
            "washingmachines2019" to listOf("WASHING", "LAVE-LINGE", "LAVINGE"),
            "washerdryers" to listOf("DRYER", "SECHANT", "WASHER DRYER"),
            "dishwashers2019" to listOf("DISHWASHER", "LAVE-VAISSELLE"),
            "refrigeratingappliances2019" to listOf("REFRIGERATOR", "FRIDGE", "FREEZER", "REFRIGERATEUR"),
            "tumbledryers" to listOf("TUMBLE", "DRYER", "SECHE-LINGE"),
            "ovens" to listOf("OVEN", "FOUR"),
            "rangehoods" to listOf("HOOD", "HOTTE"),
            "airconditioners" to listOf("AIR CONDITIONER", "CLIMATISEUR"),
            "tyres" to listOf("TYRE", "TIRE", "PNEU"),
            "spaceheaters" to listOf("HEATER", "CHAUFFAGE"),
            "waterheaters" to listOf("WATER HEATER", "CHAUFFE-EAU"),
            "smartphonestablets20231669" to listOf("SMARTPHONE", "TABLET", "TABLETTE")
        )

        keywordMap.forEach { (category, keywords) ->
            if (keywords.any { normalized.contains(it) }) {
                detected += category
            }
        }

        return (detected + DEFAULT_EPREL_CATEGORY_IDS).distinct()
    }
}
