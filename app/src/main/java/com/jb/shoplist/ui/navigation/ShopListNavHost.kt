package com.jb.shoplist.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import android.net.Uri
import com.jb.shoplist.ActionCochage
import com.jb.shoplist.EtatRestauration
import com.jb.shoplist.TodoUiState
import com.jb.shoplist.TodoViewModel
import com.jb.shoplist.ui.screens.categories.CategoriesScreen
import com.jb.shoplist.ui.screens.liste.TodoListScreen
import com.jb.shoplist.ui.screens.partage.PartagerScreen

@Composable
fun ShopListNavHost(
    navController: NavHostController,
    uiState: TodoUiState,
    viewModel: TodoViewModel,
    snackbarHostState: SnackbarHostState,
    pileCochages: List<ActionCochage>,
    etatRestauration: EtatRestauration
) {
    NavHost(navController = navController, startDestination = Routes.LISTE) {
        composable(Routes.LISTE) {
            val actionAnnulable by viewModel.actionAnnulable.collectAsStateWithLifecycle()
            TodoListScreen(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                actionAnnulable = actionAnnulable,
                pileCochagesNonVide = pileCochages.isNotEmpty(),
                onAnnulerDernierCochage = viewModel::annulerDernierCochage,
                onAjouterTache = viewModel::ajouterTache,
                onBasculerFait = viewModel::basculerFait,
                onModifierTitreTache = viewModel::modifierTitreTache,
                onSupprimerTache = viewModel::supprimerTache,
                onReordonnerTaches = viewModel::reordonnerTaches,
                onAjouterCategorie = viewModel::ajouterCategorie,
                onModifierTitreListe = viewModel::modifierTitreListe,
                onToggleCategorieRepliee = viewModel::toggleCategorieRepliee,
                onOuvrirCategories = { navController.navigate(Routes.CATEGORIES) },
                onOuvrirPartage = { navController.navigate(Routes.PARTAGER) },
                onImporterCsv = viewModel::importerDepuisCsv,
                onToutCocher = viewModel::toutCocher,
                onBasculerAfficherFaites = viewModel::setAfficherFaites,
                onSetTermeRecherche = viewModel::setTermeRecherche,
                onSauvegardeComplete = viewModel::sauvegardeComplete,
                etatRestauration = etatRestauration,
                onLireFichierSauvegarde = viewModel::lireFichierSauvegarde,
                onConfirmerRestauration = viewModel::confirmerRestauration,
                onAnnulerRestauration = viewModel::annulerRestauration,
                onConsommerErreurRestauration = viewModel::consommerErreurRestauration
            )
        }
        composable(Routes.CATEGORIES) {
            val actionAnnulable by viewModel.actionAnnulable.collectAsStateWithLifecycle()
            CategoriesScreen(
                categories = uiState.categories,
                taches = uiState.taches,
                snackbarHostState = snackbarHostState,
                actionAnnulable = actionAnnulable,
                onAjouterCategorie = viewModel::ajouterCategorie,
                onSupprimerCategorie = viewModel::supprimerCategorie,
                onReordonnerCategories = viewModel::reordonnerCategories,
                onRenommerCategorie = viewModel::renommerCategorie,
                onModifierCouleur = viewModel::modifierCouleurCategorie,
                onTrierTachesParAlpha = viewModel::trierTachesParAlpha,
                onRetour = { navController.popBackStack() },
            )
        }
        composable(Routes.PARTAGER) {
            PartagerScreen(
                categories = uiState.categories,
                taches = uiState.taches,
                titreListe = uiState.titreListe,
                onExporterCsv = viewModel::exporterCsv,
                onRetour = { navController.popBackStack() }
            )
        }
    }
}
