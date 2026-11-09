package com.jb.shoplist

import java.text.Collator
import java.util.Locale

/**
 * Comparateur alphabétique français : lettres accentuées rangées avec
 * leur lettre de base, casse ignorée au premier niveau.
 * Une nouvelle instance de Collator est créée à chaque appel
 * (Collator n'est pas garanti thread-safe).
 */
fun comparateurAlphabetique(): Comparator<String> {
    val collator = Collator.getInstance(Locale.FRENCH)
    collator.strength = Collator.TERTIARY
    return Comparator { a, b -> collator.compare(a, b) }
}

/** Vrai si deux noms de produit sont considérés identiques (casse et espaces de début/fin ignorés, accents comptés). */
fun nomsProduitsIdentiques(a: String, b: String): Boolean =
    a.trim().lowercase() == b.trim().lowercase()

/** "Aubergine : 3", ou "Aubergine" si la quantité est vide. */
fun libelleAvecQuantite(titre: String, quantite: String): String {
    return if (quantite.isNotBlank()) {
        "$titre : $quantite"
    } else {
        titre
    }
}

/** Découpe une ligne CSV séparée par ';' en respectant les guillemets. */
fun parserLigneCsv(ligne: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < ligne.length) {
        val c = ligne[i]
        if (c == '"') {
            if (inQuotes && i + 1 < ligne.length && ligne[i + 1] == '"') {
                current.append('"')
                i++
            } else {
                inQuotes = !inQuotes
            }
        } else if (c == ';' && !inQuotes) {
            result.add(current.toString())
            current.setLength(0)
        } else {
            current.append(c)
        }
        i++
    }
    result.add(current.toString())
    return result
}
