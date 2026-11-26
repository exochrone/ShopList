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
                positionBarre = PositionBarre.HAUT,
                onChangerPositionBarre = {},
                onImporterCsv = {},
                onSauvegardeComplete = {},
                etatRestauration = EtatRestauration.Inactif,
                onLireFichierSauvegarde = {},
                onConfirmerRestauration = {},
                onAnnulerRestauration = {},
                onConsommerErreurRestauration = {},
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
                positionBarre = PositionBarre.HAUT,
                onChangerPositionBarre = {},
                onImporterCsv = {},
                onSauvegardeComplete = {},
                etatRestauration = EtatRestauration.Inactif,
                onLireFichierSauvegarde = {},
                onConfirmerRestauration = {},
                onAnnulerRestauration = {},
                onConsommerErreurRestauration = {},
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
                positionBarre = PositionBarre.HAUT,
                onChangerPositionBarre = {},
                onImporterCsv = {},
                onSauvegardeComplete = {},
                etatRestauration = EtatRestauration.Inactif,
                onLireFichierSauvegarde = {},
                onConfirmerRestauration = {},
                onAnnulerRestauration = {},
                onConsommerErreurRestauration = {},
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

    @Test
    fun testAffichageBoutonsEtOptionPositionBarre() {
        composeTestRule.setContent {
            OptionsScreen(
                titreListe = "Courses",
                versionApp = "2026.04",
                onModifierTitreListe = {},
                onOuvrirCategories = {},
                positionBarre = PositionBarre.HAUT,
                onChangerPositionBarre = {},
                onImporterCsv = {},
                onSauvegardeComplete = {},
                etatRestauration = EtatRestauration.Inactif,
                onLireFichierSauvegarde = {},
                onConfirmerRestauration = {},
                onAnnulerRestauration = {},
                onConsommerErreurRestauration = {},
                onRetour = {}
            )
        }

        composeTestRule.onNodeWithText("Position de la barre de titre et d'options").assertIsDisplayed()
        composeTestRule.onNodeWithText("Importer une liste").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sauvegarde complète").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restaurer une sauvegarde").assertIsDisplayed()
    }

    @Test
    fun testSelectionPositionBarreHautEtBas() {
        var nouvellePosition: PositionBarre? = null

        composeTestRule.setContent {
            OptionsScreen(
                titreListe = "Courses",
                versionApp = "2026.04",
                onModifierTitreListe = {},
                onOuvrirCategories = {},
                positionBarre = PositionBarre.HAUT,
                onChangerPositionBarre = { nouvellePosition = it },
                onImporterCsv = {},
                onSauvegardeComplete = {},
                etatRestauration = EtatRestauration.Inactif,
                onLireFichierSauvegarde = {},
                onConfirmerRestauration = {},
                onAnnulerRestauration = {},
                onConsommerErreurRestauration = {},
                onRetour = {}
            )
        }

        composeTestRule.onNodeWithText("Haut").assertIsSelected()
        composeTestRule.onNodeWithText("Bas").assertIsNotSelected()

        composeTestRule.onNodeWithText("Bas").performClick()

        assertEquals(PositionBarre.BAS, nouvellePosition)
    }

    @Test
    fun testToucherImporterListeAfficheDialogueCsv() {
        composeTestRule.setContent {
            OptionsScreen(
                titreListe = "Courses",
                versionApp = "2026.04",
                onModifierTitreListe = {},
                onOuvrirCategories = {},
                positionBarre = PositionBarre.HAUT,
                onChangerPositionBarre = {},
                onImporterCsv = {},
                onSauvegardeComplete = {},
                etatRestauration = EtatRestauration.Inactif,
                onLireFichierSauvegarde = {},
                onConfirmerRestauration = {},
                onAnnulerRestauration = {},
                onConsommerErreurRestauration = {},
                onRetour = {}
            )
        }

        composeTestRule.onNodeWithText("Importer une liste").performClick()
        composeTestRule.onNodeWithText("Import CSV").assertIsDisplayed()
    }

    @Test
    fun testToucherRestaurerSauvegardeAfficheBoutonChoisirFichier() {
        composeTestRule.setContent {
            OptionsScreen(
                titreListe = "Courses",
                versionApp = "2026.04",
                onModifierTitreListe = {},
                onOuvrirCategories = {},
                positionBarre = PositionBarre.HAUT,
                onChangerPositionBarre = {},
                onImporterCsv = {},
                onSauvegardeComplete = {},
                etatRestauration = EtatRestauration.Inactif,
                onLireFichierSauvegarde = {},
                onConfirmerRestauration = {},
                onAnnulerRestauration = {},
                onConsommerErreurRestauration = {},
                onRetour = {}
            )
        }

        composeTestRule.onNodeWithText("Restaurer une sauvegarde").performClick()
        composeTestRule.onNodeWithText("Choisir un fichier").assertIsDisplayed()
    }

    @Test
    fun testEtatRestaurationErreurAfficheDialogueEtAppelleConsommerErreur() {
        var consommerAppele = false

        composeTestRule.setContent {
            OptionsScreen(
                titreListe = "Courses",
                versionApp = "2026.04",
                onModifierTitreListe = {},
                onOuvrirCategories = {},
                positionBarre = PositionBarre.HAUT,
                onChangerPositionBarre = {},
                onImporterCsv = {},
                onSauvegardeComplete = {},
                etatRestauration = EtatRestauration.Erreur("Fichier invalide"),
                onLireFichierSauvegarde = {},
                onConfirmerRestauration = {},
                onAnnulerRestauration = {},
                onConsommerErreurRestauration = { consommerAppele = true },
                onRetour = {}
            )
        }

        composeTestRule.onNodeWithText("Restauration impossible").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fichier invalide").assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").performClick()

        assertTrue(consommerAppele)
    }
}
