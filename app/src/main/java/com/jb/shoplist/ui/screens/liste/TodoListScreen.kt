package com.jb.shoplist.ui.screens.liste

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.ui.unit.Dp
import com.jb.shoplist.ActionAnnulable
import com.jb.shoplist.CategorieEntity
import com.jb.shoplist.EtatRestauration
import com.jb.shoplist.TacheEntity
import com.jb.shoplist.TodoUiState
import com.jb.shoplist.ui.components.ProduitEditionDialog
import com.jb.shoplist.ui.components.SaisieDialog
import com.jb.shoplist.ui.components.TacheItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.Normalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    uiState: TodoUiState,
    snackbarHostState: SnackbarHostState,
    actionAnnulable: ActionAnnulable?,
    pileCochagesNonVide: Boolean,
    onAnnulerDernierCochage: () -> Unit,
    onAjouterTache: (String, String) -> Unit,
    onBasculerFait: (TacheEntity) -> Unit,
    onModifierProduit: (TacheEntity, String, String, String) -> Unit,
    onSupprimerTache: (TacheEntity) -> Unit,
    onReordonnerTaches: (List<TacheEntity>) -> Unit,
    onAjouterCategorie: (String) -> Unit,
    onRenommerCategorie: (CategorieEntity, String) -> Unit,
    onModifierTitreListe: (String) -> Unit,
    onToggleCategorieRepliee: (CategorieEntity) -> Unit,
    onOuvrirCategories: () -> Unit,
    onOuvrirPartage: () -> Unit,
    onImporterCsv: (android.net.Uri) -> Unit,
    onToutCocher: () -> Unit,
    onBasculerAfficherFaites: (Boolean) -> Unit,
    onSetTermeRecherche: (String) -> Unit,
    onTrierTachesParAlpha: (String) -> Unit,
    onModifierCouleurCategorie: (CategorieEntity, Long) -> Unit,
    onSupprimerCategorie: (CategorieEntity) -> Unit,
    onSauvegardeComplete: (Uri) -> Unit,
    etatRestauration: EtatRestauration,
    onLireFichierSauvegarde: (Uri) -> Unit,
    onConfirmerRestauration: () -> Unit,
    onAnnulerRestauration: () -> Unit,
    onConsommerErreurRestauration: () -> Unit
) {
    var modeRecherche by remember { mutableStateOf(false) }

    BackHandler(enabled = modeRecherche) {
        modeRecherche = false
        onSetTermeRecherche("")
    }
    var afficherDialogCategorie by remember { mutableStateOf(false) }

    var afficherDialogRenommer by remember { mutableStateOf(false) }

    var afficherDialogImport by remember { mutableStateOf(false) }

    var categorieCibleAjout by remember { mutableStateOf<String?>(null) }

    var tacheEnEdition by remember { mutableStateOf<TacheEntity?>(null) }

    var menuOuvertPourCategorie by remember { mutableStateOf<CategorieEntity?>(null) }
    var categoriePourCouleur by remember { mutableStateOf<CategorieEntity?>(null) }
    var categorieASupprimer by remember { mutableStateOf<Pair<CategorieEntity, Int>?>(null) }
    var categorieARenommer by remember { mutableStateOf<CategorieEntity?>(null) }

    val launcherImport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        uri?.let { onImporterCsv(it) }
    }

    val launcherSauvegardeComplete = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { onSauvegardeComplete(it) }
    }

    var afficherDialogPreRestauration by remember { mutableStateOf(false) }

    val launcherRestauration = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onLireFichierSauvegarde(it) }
    }

    val lignes: List<LigneAffichee> = remember(uiState.categories, uiState.taches, uiState.afficherFaites, uiState.termeRecherche) {
        val recherche = uiState.termeRecherche.trim().sansAccents().lowercase()
        buildList {
            uiState.categories.forEach { cat ->
                val tachesDeLaCat = uiState.taches.filter { it.categorie == cat.nom }
                val nombreNonCoches = tachesDeLaCat.count { !it.faite }

                val tachesFiltreesParFait = if (uiState.afficherFaites) {
                    tachesDeLaCat
                } else {
                    tachesDeLaCat.filter { !it.faite }
                }

                val tachesFinales = if (recherche.isEmpty()) {
                    tachesFiltreesParFait
                } else {
                    tachesDeLaCat.filter { tache ->
                        tache.titre.sansAccents().lowercase().contains(recherche) ||
                                cat.nom.sansAccents().lowercase().contains(recherche)
                    }
                }

                if (tachesFinales.isNotEmpty() || (recherche.isEmpty() && uiState.afficherFaites)) {
                    add(LigneAffichee.Header(cat, nombreNonCoches))
                    if (!cat.repliee) {
                        tachesFinales.forEach { tache ->
                            add(LigneAffichee.ItemTache(tache, cat.couleur))
                        }
                    }
                }
            }
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val itemDeplace = lignes[from.index]
        val cible = lignes[to.index]

        if (itemDeplace !is LigneAffichee.ItemTache) return@rememberReorderableLazyListState

        val tacheDeplacee = itemDeplace.tache

        val nouvelleCategorieName = when (cible) {
            is LigneAffichee.Header -> cible.categorie.nom
            is LigneAffichee.ItemTache -> cible.tache.categorie
        }

        val sansTache = uiState.taches.filter { it.id != tacheDeplacee.id }
        val tacheMiseAJour = tacheDeplacee.copy(categorie = nouvelleCategorieName)

        val indexCible = when (cible) {
            is LigneAffichee.Header -> {
                val premiereTacheCat = sansTache.indexOfFirst { it.categorie == nouvelleCategorieName }
                if (premiereTacheCat == -1) sansTache.size else premiereTacheCat
            }
            is LigneAffichee.ItemTache -> {
                val indexDansSansTache = sansTache.indexOfFirst { it.id == cible.tache.id }
                if (from.index < to.index) indexDansSansTache + 1 else indexDansSansTache
            }
        }

        val nouvelleListe = sansTache.toMutableList().apply {
            add(indexCible.coerceIn(0, size), tacheMiseAJour)
        }
        onReordonnerTaches(nouvelleListe)
    }

    Scaffold(
        topBar = {
            TodoListTopBar(
                titre = uiState.titreListe,
                afficherFaites = uiState.afficherFaites,
                onToggleAfficherFaites = { onBasculerAfficherFaites(!uiState.afficherFaites) },
                undoCochageActif = pileCochagesNonVide,
                onAnnulerCochage = onAnnulerDernierCochage,
                onOuvrirCategories = onOuvrirCategories,
                onOuvrirPartage = onOuvrirPartage,
                onOuvrirImport = { afficherDialogImport = true },
                onRenommerListe = {
                    afficherDialogRenommer = true
                },
                onToutCocher = {
                    onBasculerAfficherFaites(true)
                    onToutCocher()
                },
                onSauvegardeComplete = {
                    val nomFichier = "shopList-Backup-${
                        java.text.SimpleDateFormat("yyMMdd", java.util.Locale.getDefault())
                            .format(java.util.Date())
                    }.json"
                    launcherSauvegardeComplete.launch(nomFichier)
                },
                onRestaurerSauvegarde = { afficherDialogPreRestauration = true },
                isSearchMode = modeRecherche,
                onToggleSearchMode = {
                    modeRecherche = !modeRecherche
                    if (!modeRecherche) onSetTermeRecherche("")
                },
                searchTerm = uiState.termeRecherche,
                onSearchTermChange = onSetTermeRecherche
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(
                count = lignes.size,
                key = { lignes[it].cleUnique }
            ) { index ->
                val ligne = lignes[index]
                ReorderableItem(reorderableState, key = ligne.cleUnique) { isDragging ->
                    when (ligne) {
                        is LigneAffichee.Header -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clickable { onToggleCategorieRepliee(ligne.categorie) }
                            ) {
                                Icon(
                                    imageVector = if (ligne.categorie.repliee) Icons.Default.ChevronRight else Icons.Default.ExpandMore,
                                    contentDescription = if (ligne.categorie.repliee) "Déplier" else "Replier",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = ligne.categorie.nom,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color(ligne.categorie.couleur)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${ligne.nombreNonCoches})",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Box {
                                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                        IconButton(
                                            onClick = {
                                                menuOuvertPourCategorie = ligne.categorie
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.MoreVert,
                                                contentDescription = "Options de la catégorie ${ligne.categorie.nom}",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = menuOuvertPourCategorie == ligne.categorie,
                                        onDismissRequest = { menuOuvertPourCategorie = null }
                                    ) {
                                        val nbTaches = uiState.taches.count { it.categorie == ligne.categorie.nom }
                                        DropdownMenuItem(
                                            text = { Text("$nbTaches élément${if (nbTaches > 1) "s" else ""}") },
                                            onClick = { },
                                            enabled = false
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Ajout d'élément") },
                                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                            onClick = {
                                                menuOuvertPourCategorie = null
                                                categorieCibleAjout = ligne.categorie.nom
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Tri alphabétique") },
                                            leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) },
                                            onClick = {
                                                menuOuvertPourCategorie = null
                                                onTrierTachesParAlpha(ligne.categorie.nom)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Modifier la catégorie") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                menuOuvertPourCategorie = null
                                                categorieARenommer = ligne.categorie
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Choix de la couleur") },
                                            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                            onClick = {
                                                menuOuvertPourCategorie = null
                                                categoriePourCouleur = ligne.categorie
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Supprimer") },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                            onClick = {
                                                menuOuvertPourCategorie = null
                                                val nbTaches = uiState.taches.count { it.categorie == ligne.categorie.nom }
                                                categorieASupprimer = ligne.categorie to nbTaches
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        is LigneAffichee.ItemTache -> {
                            val dragModifier = if (modeRecherche) Modifier else Modifier.longPressDraggableHandle()
                            TacheItem(
                                tache = ligne.tache,
                                onCheckedChange = { onBasculerFait(ligne.tache) },
                                onClickTache = {
                                    tacheEnEdition = ligne.tache
                                },
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .then(dragModifier),
                                isDragging = isDragging,
                                couleurCase = Color(ligne.couleur)
                            )
                        }
                    }
                }
            }
            item {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            }
        }
    }

    // Dialog 1 : avertissement avant ouverture du sélecteur
    if (afficherDialogPreRestauration) {
        AlertDialog(
            onDismissRequest = { afficherDialogPreRestauration = false },
            title = { Text("Restaurer une sauvegarde") },
            text = {
                Text(
                    "La restauration va remplacer toutes les données actuelles " +
                            "(catégories, tâches, paramètres). Cette action est irréversible.\n\n" +
                            "Voulez-vous continuer ?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    afficherDialogPreRestauration = false
                    launcherRestauration.launch(arrayOf("application/json"))
                }) { Text("Choisir un fichier") }
            },
            dismissButton = {
                TextButton(onClick = { afficherDialogPreRestauration = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialog 2 : confirmation après lecture du fichier
    val etatFichierLu = etatRestauration as? EtatRestauration.FichierLu
    if (etatFichierLu != null) {
        AlertDialog(
            onDismissRequest = onAnnulerRestauration,
            title = { Text("Confirmer la restauration") },
            text = {
                val contenu = etatFichierLu.contenu
                val dateLisible = contenu.dateSauvegarde
                    ?.substringBefore("T")  // garde juste la date
                    ?: "inconnue"
                Text(
                    "Cette sauvegarde contient :\n" +
                            "- Liste : ${contenu.titreListe}\n" +
                            "- ${contenu.nbCategories} catégorie${if (contenu.nbCategories > 1) "s" else ""}\n" +
                            "- ${contenu.nbTaches} tâche${if (contenu.nbTaches > 1) "s" else ""}\n" +
                            "- Datée du : $dateLisible\n\n" +
                            "Toutes les données actuelles seront remplacées."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmerRestauration,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Restaurer") }
            },
            dismissButton = {
                TextButton(onClick = onAnnulerRestauration) { Text("Annuler") }
            }
        )
    }

    // Dialog 3 : message d'erreur si la lecture a échoué
    val etatErreur = etatRestauration as? EtatRestauration.Erreur
    if (etatErreur != null) {
        AlertDialog(
            onDismissRequest = onConsommerErreurRestauration,
            title = { Text("Restauration impossible") },
            text = { Text(etatErreur.message) },
            confirmButton = {
                TextButton(onClick = onConsommerErreurRestauration) { Text("OK") }
            }
        )
    }

    if (afficherDialogImport) {
        AlertDialog(
            onDismissRequest = { afficherDialogImport = false },
            title = { Text("Import CSV") },
            text = { Text("Import d'un fichier CSV contenant les tâches à ajouter dans l'application avec leur catégorie. Utiliser un fichier (sans entête, UTF-8) avec la catégorie dans la 1ère colonne et la tâche dans la seconde. Les deux données sont obligatoires.\n\nValider pour choisir le fichier.") },
            confirmButton = {
                TextButton(onClick = {
                    afficherDialogImport = false
                    launcherImport.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain"))
                }) { Text("Valider") }
            },
            dismissButton = {
                TextButton(onClick = { afficherDialogImport = false }) { Text("Annuler") }
            }
        )
    }

    if (afficherDialogCategorie) {
        SaisieDialog(
            titre = "Nouvelle catégorie",
            placeholder = "Nom de la catégorie",
            libelleValider = "Ajouter",
            onCheckErreur = { nom ->
                val existeDeja = uiState.categories.any { it.nom.lowercase().trim() == nom.lowercase().trim() }
                if (existeDeja) "Cette catégorie existe déjà" else null
            },
            onValider = { nom ->
                onAjouterCategorie(nom)
                afficherDialogCategorie = false
            },
            onAnnuler = { afficherDialogCategorie = false }
        )
    }

    if (afficherDialogRenommer) {
        SaisieDialog(
            titre = "Renommer la liste",
            valeurInitiale = uiState.titreListe,
            placeholder = "Nouveau titre",
            onValider = { titre ->
                onModifierTitreListe(titre)
                afficherDialogRenommer = false
            },
            onAnnuler = { afficherDialogRenommer = false }
        )
    }

    categorieCibleAjout?.let { categorie ->
        SaisieDialog(
            titre = "Nouvelle tâche dans « $categorie »",
            placeholder = "Nom de la tâche",
            libelleValider = "Ajouter",
            onCheckErreur = { titre ->
                val tacheExistante = uiState.taches.find {
                    it.titre.lowercase().trim() == titre.lowercase().trim()
                }
                if (tacheExistante != null) "Cet élément existe déjà dans la catégorie ${tacheExistante.categorie}" else null
            },
            onValider = { titre ->
                onAjouterTache(titre, categorie)
                categorieCibleAjout = null
            },
            onAnnuler = { categorieCibleAjout = null }
        )
    }

    tacheEnEdition?.let { tache ->
        ProduitEditionDialog(
            tache = tache,
            categories = uiState.categories,
            onCheckErreurTitre = { titre ->
                val tacheExistante = uiState.taches.find {
                    it.id != tache.id &&
                    it.titre.lowercase().trim() == titre.lowercase().trim()
                }
                if (tacheExistante != null) "Cet élément existe déjà dans la catégorie ${tacheExistante.categorie}" else null
            },
            onValider = { nouveauTitre, nouvelleQuantite, nouvelleCategorie ->
                onModifierProduit(tache, nouveauTitre, nouvelleQuantite, nouvelleCategorie)
                tacheEnEdition = null
            },
            onSupprimer = {
                onSupprimerTache(tache)
                tacheEnEdition = null
            },
            onFermer = {
                tacheEnEdition = null
            }
        )
    }

    if (categoriePourCouleur != null) {
        com.jb.shoplist.ui.components.ColorPickerDialog(
            onDismiss = { categoriePourCouleur = null },
            onColorSelected = { couleur ->
                categoriePourCouleur?.let { onModifierCouleurCategorie(it, couleur.toArgb().toLong()) }
                categoriePourCouleur = null
            }
        )
    }

    categorieARenommer?.let { cat ->
        SaisieDialog(
            titre = "Modifier la catégorie",
            valeurInitiale = cat.nom,
            placeholder = "Nom de la catégorie",
            onCheckErreur = { nom ->
                val existeDeja = uiState.categories.any { it.nom.lowercase().trim() == nom.lowercase().trim() && it.nom != cat.nom }
                if (existeDeja) "Cette catégorie existe déjà" else null
            },
            onValider = { nom ->
                onRenommerCategorie(cat, nom)
                categorieARenommer = null
            },
            onAnnuler = { categorieARenommer = null }
        )
    }

    categorieASupprimer?.let { (cat, nb) ->
        AlertDialog(
            onDismissRequest = { categorieASupprimer = null },
            title = { Text("Confirmation de suppression") },
            text = {
                Text("$nb tâche${if (nb > 1) "s" else ""} dans la catégorie ${cat.nom} seront supprimées avec la catégorie. Voulez-vous continuer ?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSupprimerCategorie(cat)
                        categorieASupprimer = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Oui") }
            },
            dismissButton = {
                TextButton(onClick = { categorieASupprimer = null }) { Text("Non") }
            }
        )
    }
}

private fun String.sansAccents(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(temp, "")
}
