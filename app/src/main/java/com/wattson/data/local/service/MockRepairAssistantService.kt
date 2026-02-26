package com.wattson.data.local.service

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock LLM service simulating an AI repair assistant.
 * Returns contextual French responses based on keyword matching.
 * Will be replaced by a real API call when the backend is ready.
 */
@Singleton
class MockRepairAssistantService @Inject constructor() {

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
                "Bonjour ! Je suis votre assistant reparation Wattson. " +
                "Je peux vous aider a diagnostiquer et resoudre les problemes de vos appareils.\n\n" +
                "Decrivez-moi le probleme que vous rencontrez, en precisant si possible la marque et le modele de l'appareil."

            msg.containsAny("lave-linge", "machine a laver", "machine à laver", "lave linge") ->
                "Pour votre lave-linge, voici quelques pistes de diagnostic :\n\n" +
                "1. **Verifiez le filtre de vidange** — Un filtre obstrue peut causer des problemes d'essorage ou de vidange.\n" +
                "2. **Inspectez le joint de porte** — Un joint use peut provoquer des fuites.\n" +
                "3. **Nettoyez le bac a detergent** — Des residus peuvent bloquer l'arrivee d'eau.\n" +
                "4. **Verifiez le tuyau d'evacuation** — Il ne doit pas etre plie ou bouche.\n\n" +
                "Quel symptome observez-vous exactement ?"

            msg.containsAny("lave-vaisselle", "lave vaisselle") ->
                "Pour un lave-vaisselle, les problemes courants sont :\n\n" +
                "1. **Vaisselle mal lavee** — Verifiez les bras de lavage (obstruction) et le filtre\n" +
                "2. **Eau stagnante** — Nettoyez le filtre et verifiez la pompe de vidange\n" +
                "3. **Bruit anormal** — Verifiez qu'aucun objet ne bloque les bras\n" +
                "4. **Fuite** — Inspectez le joint de porte et le tuyau d'arrivee d'eau\n\n" +
                "Quel probleme rencontrez-vous ?"

            msg.containsAny("refrigerateur", "frigo", "réfrigérateur", "congelateur", "congélateur") ->
                "Pour votre refrigerateur/congelateur :\n\n" +
                "1. **Ne refroidit plus** — Verifiez le thermostat, nettoyez le condenseur arriere\n" +
                "2. **Givre excessif** — Le joint de porte est peut-etre use\n" +
                "3. **Bruit anormal** — Le compresseur ou le ventilateur peut etre en cause\n" +
                "4. **Fuite d'eau** — Verifiez le bac de degivrage et le tuyau d'evacuation\n\n" +
                "Pouvez-vous preciser le symptome ?"

            msg.containsAny("bruit", "son bizarre", "grince", "claque", "vibre") ->
                "Un bruit anormal peut avoir plusieurs origines :\n\n" +
                "- **Claquement** : objet etranger, amortisseurs uses, ou piece desserree\n" +
                "- **Bourdonnement** : pompe ou moteur — souvent la pompe de vidange obstruee\n" +
                "- **Grincement** : roulements a remplacer ou courroie usee\n" +
                "- **Vibrations** : appareil mal cale ou charge desequilibree\n\n" +
                "Pouvez-vous decrire le type de bruit plus precisement et a quel moment il se produit ?"

            msg.containsAny("fuite", "coule", "eau par terre", "inondation") ->
                "Les fuites d'eau peuvent provenir de :\n\n" +
                "1. **Joint de porte** — Verifiez l'etat et nettoyez-le avec du vinaigre blanc\n" +
                "2. **Tuyau d'arrivee d'eau** — Controlez les raccords et serrez si necessaire\n" +
                "3. **Tuyau de vidange** — Assurez-vous qu'il est bien fixe et non perce\n" +
                "4. **Surdosage de detergent** — Trop de produit peut causer un debordement\n\n" +
                "D'ou semble provenir la fuite ?"

            msg.containsAny("ne s'allume pas", "ne demarre pas", "ne marche plus", "ne fonctionne plus", "en panne") ->
                "Si votre appareil ne s'allume pas :\n\n" +
                "1. **Verifiez la prise** — Testez-la avec un autre appareil\n" +
                "2. **Controlez le disjoncteur** — Il a peut-etre saute\n" +
                "3. **Verifiez le cordon d'alimentation** — Pas de coupure visible ?\n" +
                "4. **Bouton on/off** — Assurez-vous qu'il fonctionne correctement\n" +
                "5. **Fusible interne** — Certains appareils ont un fusible de protection\n\n" +
                "Avez-vous verifie ces points ? De quel appareil s'agit-il ?"

            msg.containsAny("ecran", "affichage", "pixel", "image") ->
                "Pour un probleme d'ecran :\n\n" +
                "- **Ecran noir** : Verifiez l'alimentation et le cable HDMI/DisplayPort\n" +
                "- **Pixels morts** : Souvent irreparable, mais essayez un outil de massage de pixels\n" +
                "- **Scintillement** : Verifiez le cable et les parametres de frequence\n" +
                "- **Lignes sur l'ecran** : Possiblement la nappe ecran a remplacer\n\n" +
                "Quel type de probleme d'affichage avez-vous ?"

            msg.containsAny("batterie", "charge", "autonomie") ->
                "Pour les problemes de batterie :\n\n" +
                "1. **Autonomie reduite** — Apres 2-3 ans, la capacite baisse naturellement. Calibrez la batterie (decharge complete puis charge complete)\n" +
                "2. **Ne charge plus** — Testez un autre cable/chargeur. Nettoyez le port de charge\n" +
                "3. **Chauffe anormale** — Evitez de l'utiliser pendant la charge. Si ca persiste, la batterie est peut-etre a remplacer\n\n" +
                "Quel appareil est concerne ?"

            msg.containsAny("merci", "super", "parfait", "genial") ->
                "Avec plaisir ! N'hesitez pas si vous avez d'autres questions sur l'entretien ou la reparation de vos appareils. " +
                "Je suis la pour vous aider a prolonger la duree de vie de vos equipements."

            msg.containsAny("combien", "cout", "prix", "tarif", "coût") ->
                "Le cout de reparation depend de plusieurs facteurs :\n\n" +
                "- **Type de panne** : piece a remplacer ou simple reglage\n" +
                "- **Piece detachee** : prix variable selon la marque et le modele\n" +
                "- **Main d'oeuvre** : si vous faites appel a un reparateur\n\n" +
                "Decrivez-moi la panne et l'appareil concerne, je pourrai vous donner une estimation."

            else ->
                "Je comprends. Pour mieux vous aider, pourriez-vous me donner plus de details ?\n\n" +
                "- **Quel appareil** est concerne ? (marque, modele si possible)\n" +
                "- **Quel symptome** observez-vous exactement ?\n" +
                "- **Depuis quand** le probleme est-il apparu ?\n\n" +
                "Avec ces informations, je pourrai vous proposer un diagnostic plus precis."
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
