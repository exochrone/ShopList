package com.jb.shoplist

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatageTest {

    @Test
    fun testLibelleAvecQuantite() {
        assertEquals("Aubergine : 3", libelleAvecQuantite("Aubergine", "3"))
        assertEquals("Farine : 1kg", libelleAvecQuantite("Farine", "1kg"))
        assertEquals("Sel", libelleAvecQuantite("Sel", ""))
        assertEquals("Poivre", libelleAvecQuantite("Poivre", "   "))
    }

    @Test
    fun testParserLigneCsvSimples() {
        assertEquals(listOf("Frais", "Lait", "1 L"), parserLigneCsv("Frais;Lait;1 L"))
        assertEquals(listOf("Frais", "Lait"), parserLigneCsv("Frais;Lait"))
        assertEquals(listOf("Frais", "Sel", ""), parserLigneCsv("Frais;Sel;"))
    }

    @Test
    fun testParserLigneCsvAvecGuillemetsEtPointVirgule() {
        val ligne = "Épicerie;\"Riz; basmati\";\"2 \"\"gros\"\" sacs\""
        val resultat = parserLigneCsv(ligne)
        assertEquals(listOf("Épicerie", "Riz; basmati", "2 \"gros\" sacs"), resultat)
    }

    @Test
    fun comparateurAlphabetique_rangeLesAccentsAvecLaLettreDeBase() {
        val entres = listOf("Tomates", "Épinards", "Carottes", "Échalotes", "Endives", "Zucchini")
        val attendu = listOf("Carottes", "Échalotes", "Endives", "Épinards", "Tomates", "Zucchini")
        assertEquals(attendu, entres.sortedWith(comparateurAlphabetique()))
    }

    @Test
    fun comparateurAlphabetique_ignoreLaCasse() {
        val entres = listOf("banane", "Ananas", "carotte")
        val attendu = listOf("Ananas", "banane", "carotte")
        assertEquals(attendu, entres.sortedWith(comparateurAlphabetique()))
    }

    @Test
    fun comparateurAlphabetique_ligatureOe() {
        val entres = listOf("Oignons", "Œufs", "Navets", "Poireaux")
        val attendu = listOf("Navets", "Œufs", "Oignons", "Poireaux")
        assertEquals(attendu, entres.sortedWith(comparateurAlphabetique()))
    }

    @Test
    fun comparateurAlphabetique_accentsMajusculesEtMinuscules() {
        val entres = listOf("Fraises", "Élan", "Dattes", "écrou", "Eau")
        val resultat = entres.sortedWith(comparateurAlphabetique())
        assertEquals(listOf("Dattes", "Eau", "écrou", "Élan", "Fraises"), resultat)
    }

    @Test
    fun comparateurAlphabetique_motsNeDifferantQueParUnAccent() {
        val entres = listOf("Pâte", "Pate", "Orange", "Radis")
        val resultat = entres.sortedWith(comparateurAlphabetique())
        assertEquals(listOf("Orange", "Pate", "Pâte", "Radis"), resultat)
    }
}
