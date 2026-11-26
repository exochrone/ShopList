package com.jb.shoplist

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jb.shoplist.ui.screens.options.OptionsScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OptionsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testOptionsScreenAffichageNomEtVersion() {
        composeTestRule.setContent {
            OptionsScreen(
                titreListe = "Mes Courses",
                versionApp = "2026.04",
                onModifierTitreListe = {},
                onOuvrirCategories = {},
                onRetour = {}
            )
        }

        composeTestRule.onNodeWithText("Options").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nom de la liste").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mes Courses").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gérer les catégories").assertIsDisplayed()
        composeTestRule.onNodeWithText("shopList v2026.04").assertIsDisplayed()
    }

    @Test
    fun testClicGererCategoriesAppelleCallback() {
        var gererCategoriesAppele = false

        composeTestRule.setContent {
            OptionsScreen(
                titreListe = "Courses",
                versionApp = "2026.04",
                onModifierTitreListe = {},
                onOuvrirCategories = { gererCategoriesAppele = true },
                onRetour = {}
            )
        }

        composeTestRule.onNodeWithText("Gérer les catégories").performClick()
        assertTrue(gererCategoriesAppele)
    }

    @Test
    fun testClicNomOuvreDialogueEtValidationModifieTitre() {
        var nouveauTitreRecu = ""

        composeTestRule.setContent {
            OptionsScreen(
                titreListe = "Courses",
                versionApp = "2026.04",
                onModifierTitreListe = { nouveauTitreRecu = it },
                onOuvrirCategories = {},
                onRetour = {}
            )
        }

        // Clic sur le nom de la liste pour ouvrir le dialogue
        composeTestRule.onNodeWithText("Courses").performClick()

        // Le dialogue s'affiche
        composeTestRule.onNodeWithText("Renommer la liste").assertIsDisplayed()

        // Saisir un nouveau titre et valider
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("Maison")
        composeTestRule.onNodeWithText("Valider").performClick()

        assertEquals("Maison", nouveauTitreRecu)
    }
}
