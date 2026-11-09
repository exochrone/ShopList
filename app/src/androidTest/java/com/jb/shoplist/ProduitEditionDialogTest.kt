package com.jb.shoplist

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.jb.shoplist.ui.components.ProduitEditionDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProduitEditionDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val categories = listOf(
        CategorieEntity(nom = "Frais", ordre = 0),
        CategorieEntity(nom = "Épicerie", ordre = 1)
    )

    private val tacheTest = TacheEntity(
        id = 1,
        titre = "Lait",
        categorie = "Frais",
        quantite = "2 L"
    )

    @Test
    fun testCroixEffacementVideLeChamp() {
        composeTestRule.setContent {
            ProduitEditionDialog(
                tache = tacheTest,
                categories = categories,
                onCheckErreurTitre = { _, _ -> null },
                onValider = { _, _, _ -> },
                onSupprimer = { },
                onFermer = { }
            )
        }

        // Le nom "Lait" a une croix d'effacement
        composeTestRule.onAllNodesWithContentDescription("Effacer")[0].performClick()
        // Après clic, le champ titre est vide, donc sa croix d'effacement n'est plus visible (il reste seulement la croix sur la quantité "2 L")
        assertEquals(1, composeTestRule.onAllNodesWithContentDescription("Effacer").fetchSemanticsNodes().size)
    }

    @Test
    fun testValiderDesactiveQuandNomVide() {
        composeTestRule.setContent {
            ProduitEditionDialog(
                tache = tacheTest.copy(titre = ""),
                categories = categories,
                onCheckErreurTitre = { _, _ -> null },
                onValider = { _, _, _ -> },
                onSupprimer = { },
                onFermer = { }
            )
        }

        composeTestRule.onNodeWithText("Valider").assertIsNotEnabled()
    }

    @Test
    fun testChangementCategorieAppelleOnValider() {
        var validerAppele = false
        var nouvelleCatRecue = ""

        composeTestRule.setContent {
            ProduitEditionDialog(
                tache = tacheTest,
                categories = categories,
                onCheckErreurTitre = { _, _ -> null },
                onValider = { _, _, cat ->
                    validerAppele = true
                    nouvelleCatRecue = cat
                },
                onSupprimer = { },
                onFermer = { }
            )
        }

        // Ouvrir le menu des catégories
        composeTestRule.onNodeWithText("Frais").performClick()
        // Choisir "Épicerie"
        composeTestRule.onNodeWithText("Épicerie").performClick()
        // Valider
        composeTestRule.onNodeWithText("Valider").performClick()

        assertTrue(validerAppele)
        assertEquals("Épicerie", nouvelleCatRecue)
    }

    @Test
    fun testCroixFermetureAppelleOnFermer() {
        var fermerAppele = false
        var validerAppele = false

        composeTestRule.setContent {
            ProduitEditionDialog(
                tache = tacheTest,
                categories = categories,
                onCheckErreurTitre = { _, _ -> null },
                onValider = { _, _, _ -> validerAppele = true },
                onSupprimer = { },
                onFermer = { fermerAppele = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Fermer").performClick()

        assertTrue(fermerAppele)
        assertFalse(validerAppele)
    }

    @Test
    fun testModeAjoutAfficheTitreEtBoutons() {
        composeTestRule.setContent {
            ProduitEditionDialog(
                tache = null,
                categories = categories,
                categorieInitiale = "Frais",
                onCheckErreurTitre = { _, _ -> null },
                onValider = { _, _, _ -> },
                onFermer = { }
            )
        }

        composeTestRule.onNodeWithText("Nouveau produit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ajouter").assertIsDisplayed()
        composeTestRule.onNodeWithText("Frais").assertIsDisplayed()
        composeTestRule.onNodeWithText("Supprimer").assertDoesNotExist()
        composeTestRule.onNodeWithText("Valider").assertDoesNotExist()
    }

    @Test
    fun testModeAjoutAjouterDesactiveQuandNomVide() {
        composeTestRule.setContent {
            ProduitEditionDialog(
                tache = null,
                categories = categories,
                categorieInitiale = "Frais",
                onCheckErreurTitre = { _, _ -> null },
                onValider = { _, _, _ -> },
                onFermer = { }
            )
        }

        composeTestRule.onNodeWithText("Ajouter").assertIsNotEnabled()
    }

    @Test
    fun testModeAjoutAppelleOnValiderAvecValeursNettoyees() {
        var titreRecu = ""
        var quantiteRecue = ""
        var catRecue = ""

        composeTestRule.setContent {
            ProduitEditionDialog(
                tache = null,
                categories = categories,
                categorieInitiale = "Frais",
                onCheckErreurTitre = { _, _ -> null },
                onValider = { t, q, c ->
                    titreRecu = t
                    quantiteRecue = q
                    catRecue = c
                },
                onFermer = { }
            )
        }

        composeTestRule.onNodeWithText("Nom du produit").performTextInput(" Lait ")
        composeTestRule.onNodeWithText("Quantité (facultatif)").performTextInput(" 1 L ")
        composeTestRule.onNodeWithText("Ajouter").performClick()

        assertEquals("Lait", titreRecu)
        assertEquals("1 L", quantiteRecue)
        assertEquals("Frais", catRecue)
    }

    @Test
    fun testModeAjoutChangementCategorie() {
        var catRecue = ""

        composeTestRule.setContent {
            ProduitEditionDialog(
                tache = null,
                categories = categories,
                categorieInitiale = "Frais",
                onCheckErreurTitre = { _, _ -> null },
                onValider = { _, _, c -> catRecue = c },
                onFermer = { }
            )
        }

        composeTestRule.onNodeWithText("Frais").performClick()
        composeTestRule.onNodeWithText("Épicerie").performClick()
        composeTestRule.onNodeWithText("Nom du produit").performTextInput("Riz")
        composeTestRule.onNodeWithText("Ajouter").performClick()

        assertEquals("Épicerie", catRecue)
    }

    @Test
    fun testModeAjoutCroixFermetureAppelleOnFermer() {
        var fermerAppele = false
        var validerAppele = false

        composeTestRule.setContent {
            ProduitEditionDialog(
                tache = null,
                categories = categories,
                categorieInitiale = "Frais",
                onCheckErreurTitre = { _, _ -> null },
                onValider = { _, _, _ -> validerAppele = true },
                onFermer = { fermerAppele = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Fermer").performClick()

        assertTrue(fermerAppele)
        assertFalse(validerAppele)
    }

    @Test
    fun testErreurUniciteCategorieAfficheMessageEtDesactiveAjouter() {
        composeTestRule.setContent {
            ProduitEditionDialog(
                tache = null,
                categories = categories,
                categorieInitiale = "Frais",
                onCheckErreurTitre = { _, cat ->
                    if (cat == "Frais") "Ce produit existe déjà dans cette catégorie." else null
                },
                onValider = { _, _, _ -> },
                onFermer = { }
            )
        }

        composeTestRule.onNodeWithText("Nom du produit").performTextInput("Lait")
        composeTestRule.onNodeWithText("Ce produit existe déjà dans cette catégorie.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ajouter").assertIsNotEnabled()
    }

    @Test
    fun testChangementCategorieEffaceErreurUniciteEtActiveAjouter() {
        composeTestRule.setContent {
            ProduitEditionDialog(
                tache = null,
                categories = categories,
                categorieInitiale = "Frais",
                onCheckErreurTitre = { _, cat ->
                    if (cat == "Frais") "Ce produit existe déjà dans cette catégorie." else null
                },
                onValider = { _, _, _ -> },
                onFermer = { }
            )
        }

        composeTestRule.onNodeWithText("Nom du produit").performTextInput("Lait")
        composeTestRule.onNodeWithText("Ce produit existe déjà dans cette catégorie.").assertIsDisplayed()

        // Sélectionner "Épicerie"
        composeTestRule.onNodeWithText("Frais").performClick()
        composeTestRule.onNodeWithText("Épicerie").performClick()

        composeTestRule.onNodeWithText("Ce produit existe déjà dans cette catégorie.").assertDoesNotExist()
        composeTestRule.onNodeWithText("Ajouter").assertIsEnabled()
    }
}
