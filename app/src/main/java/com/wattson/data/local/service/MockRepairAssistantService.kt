package com.wattson.data.local.service

import android.content.Context
import com.wattson.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock LLM service simulating an AI repair assistant.
 * Returns localized contextual responses based on keyword matching.
 * Will be replaced by a real API call when the backend is ready.
 */
@Singleton
class MockRepairAssistantService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Generate a simulated AI response for the given user message.
     * Includes a random delay to simulate LLM processing time.
     */
    suspend fun generateResponse(userMessage: String): String {
        delay((1000L..2000L).random())
        return selectResponse(userMessage)
    }

    private fun selectResponse(userMessage: String): String {
        val msg = userMessage.lowercase()
        return when {
            msg.containsAny("bonjour", "salut", "hello", "bonsoir") ->
                context.getString(R.string.repair_mock_greeting_response)

            msg.containsAny("lave-linge", "machine a laver", "machine à laver", "lave linge") ->
                context.getString(R.string.repair_mock_washing_machine_response)

            msg.containsAny("lave-vaisselle", "lave vaisselle") ->
                context.getString(R.string.repair_mock_dishwasher_response)

            msg.containsAny("refrigerateur", "frigo", "réfrigérateur", "congelateur", "congélateur") ->
                context.getString(R.string.repair_mock_refrigerator_response)

            msg.containsAny("bruit", "son bizarre", "grince", "claque", "vibre") ->
                context.getString(R.string.repair_mock_noise_response)

            msg.containsAny("fuite", "coule", "eau par terre", "inondation") ->
                context.getString(R.string.repair_mock_leak_response)

            msg.containsAny("ne s'allume pas", "ne demarre pas", "ne marche plus", "ne fonctionne plus", "en panne") ->
                context.getString(R.string.repair_mock_power_response)

            msg.containsAny("ecran", "affichage", "pixel", "image", "screen", "display") ->
                context.getString(R.string.repair_mock_display_response)

            msg.containsAny("batterie", "charge", "autonomie", "battery") ->
                context.getString(R.string.repair_mock_battery_response)

            msg.containsAny("merci", "super", "parfait", "genial", "thanks", "thank you") ->
                context.getString(R.string.repair_mock_thanks_response)

            msg.containsAny("combien", "cout", "prix", "tarif", "coût", "cost", "price") ->
                context.getString(R.string.repair_mock_cost_response)

            else ->
                context.getString(R.string.repair_mock_default_response)
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
