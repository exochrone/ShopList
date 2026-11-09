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
}
