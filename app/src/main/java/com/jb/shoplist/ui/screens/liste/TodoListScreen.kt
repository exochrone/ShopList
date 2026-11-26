package com.jb.shoplist.ui.screens.liste

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.ui.unit.Dp
import com.jb.shoplist.ActionAnnulable
import com.jb.shoplist.CategorieEntity
import com.jb.shoplist.PositionBarre
import com.jb.shoplist.TacheEntity
import com.jb.shoplist.TodoUiState
import com.jb.shoplist.categorieReplieeAffichee
import com.jb.shoplist.nomsProduitsIdentiques
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
    positionBarre: PositionBarre,
    onAnnulerDernierCochage: () -> Unit,
    onAjouterTache: (titre: String, categorie: String, quantite: String) -> Unit,
    onBasculerFait: (TacheEntity) -> Unit,
    onModifierProduit: (TacheEntity, String, String, String) -> Unit,
    onSupprimerTache: (TacheEntity) -> Unit,
    onReordonnerTaches: (List<TacheEntity>) -> Unit,
    onAjouterCategorie: (String) -> Unit,
    onRenommerCategorie: (CategorieEntity, String) -> Unit,
    onToggleCategorieRepliee: (CategorieEntity) -> Unit,
    onOuvrirPartage: () -> Unit,
    onOuvrirOptions: () -> Unit,
    onToutCocher: () -> Unit,
    onBasculerAfficherFaites: (Boolean) -> Unit,
    onSetTermeRecherche: (String) -> Unit,
    onTrierTachesParAlpha: (String) -> Unit,
    onModifierCouleurCategorie: (CategorieEntity, Long) -> Unit,
    onSupprimerCategorie: (CategorieEntity) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var modeRecherche by remember { mutableStateOf(false) }
    var categoriesToucheesEnRecherche by remember { mutableStateOf(setOf<String>()) }
    var cibleDefilement by remember { mutableStateOf<CibleDefilement?>(null) }

    fun quitterRecherche(cible: CibleDefilement? = null) {
        focusManager.clearFocus()
        keyboardController?.hide()
        modeRecherche = false
        onSetTermeRecherche("")
        cibleDefilement = cible
        categoriesToucheesEnRecherche = emptySet()
    }

    var idTacheGlissee by remember { mutableStateOf<Int?>(null) }
    var categorieOrigineGlissement by remember { mutableStateOf<String?>(null) }
    var categorieFinaleGlissement by remember { mutableStateOf<String?>(null) }
    var tachesAvantGlissement by remember { mutableStateOf<List<TacheEntity>?>(null) }

    fun verifierDoublonApresGlissement() {
        val idGlisse = idTacheGlissee
        val catOrigine = categorieOrigineGlissement
        val catFinale = categorieFinaleGlissement
        val avantGlissement = tachesAvantGlissement

        if (idGlisse != null && catOrigine != null && catFinale != null && avantGlissement != null) {
            if (catFinale != catOrigine) {
                val tacheOriginale = avantGlissement.firstOrNull { it.id == idGlisse }
                if (tacheOriginale != null) {
                    val existeDoublon = avantGlissement.any {
                        it.id != idGlisse && it.categorie == catFinale && nomsProduitsIdentiques(it.titre, tacheOriginale.titre)
                    }
                    if (existeDoublon) {
                        onReordonnerTaches(avantGlissement)
                        Toast.makeText(
                            context,
                            "Ce produit existe déjà dans la catégorie $catFinale : déplacement annulé.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        idTacheGlissee = null
        categorieOrigineGlissement = null
        categorieFinaleGlissement = null
        tachesAvantGlissement = null
    }

    BackHandler(enabled = modeRecherche) {
        quitterRecherche()
    }
    var afficherDialogCategorie by remember { mutableStateOf(false) }

    var categorieCibleAjout by remember { mutableStateOf<String?>(null) }

    var tacheEnEdition by remember { mutableStateOf<TacheEntity?>(null) }

    var menuOuvertPourCategorie by remember { mutableStateOf<CategorieEntity?>(null) }
    var categoriePourCouleur by remember { mutableStateOf<CategorieEntity?>(null) }
    var categorieASupprimer by remember { mutableStateOf<Pair<CategorieEntity, Int>?>(null) }
    var categorieARenommer by remember { mutableStateOf<CategorieEntity?>(null) }

    val lignes: List<LigneAffichee> = remember(uiState.categories, uiState.taches, uiState.afficherFaites, uiState.termeRecherche, categoriesToucheesEnRecherche) {
        val recherche = uiState.termeRecherche.trim().sansAccents().lowercase()
        val rechercheActive = recherche.isNotEmpty()
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
                        tache.titre.sansAccents().lowercase().contains(recherche)
                    }
                }

                if (tachesFinales.isNotEmpty() || (recherche.isEmpty() && uiState.afficherFaites)) {
                    val replieeAffichee = categorieReplieeAffichee(
                        replieeEnBase = cat.repliee,
                        rechercheActive = rechercheActive,
                        aDesResultats = tachesFinales.isNotEmpty(),
                        toucheeEnRecherche = cat.nom in categoriesToucheesEnRecherche
                    )
                    add(LigneAffichee.Header(cat, nombreNonCoches, replieeAffichee))
                    if (!replieeAffichee) {
                        tachesFinales.forEach { tache ->
                            add(LigneAffichee.ItemTache(tache, cat.couleur))
                        }
                    }
                }
            }
        }
    }

    val lazyListState = rememberLazyListState()

    LaunchedEffect(lignes, cibleDefilement) {
        val cible = cibleDefilement ?: return@LaunchedEffect
        if (uiState.termeRecherche.isNotEmpty()) return@LaunchedEffect
        var index = lignes.indexOfFirst { it.cleUnique == cible.cle }
        if (index == -1 && cible.cleRepli != null) {
            index = lignes.indexOfFirst { it.cleUnique == cible.cleRepli }
        }
        if (index != -1) {
            val info = lazyListState.layoutInfo
            val visible = info.visibleItemsInfo.firstOrNull { it.index == index }
            val entierementVisible = visible != null &&
                visible.offset >= info.viewportStartOffset &&
                visible.offset + visible.size <= info.viewportEndOffset
            if (!entierementVisible) lazyListState.scrollToItem(index)
        }
        cibleDefilement = null
    }
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val itemDeplace = lignes[from.index]
        val cible = lignes[to.index]

        if (itemDeplace !is LigneAffichee.ItemTache) return@rememberReorderableLazyListState

        val tacheDeplacee = itemDeplace.tache

        val nouvelleCategorieName = when (cible) {
            is LigneAffichee.Header -> cible.categorie.nom
            is LigneAffichee.ItemTache -> cible.tache.categorie
        }
        categorieFinaleGlissement = nouvelleCategorieName

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

    val barre: @Composable () -> Unit = {
        TodoListTopBar(
            titre = uiState.titreListe,
            afficherFaites = uiState.afficherFaites,
            onToggleAfficherFaites = { onBasculerAfficherFaites(!uiState.afficherFaites) },
            undoCochageActif = pileCochagesNonVide,
            onAnnulerCochage = onAnnulerDernierCochage,
            onOuvrirPartage = onOuvrirPartage,
            onOuvrirOptions = onOuvrirOptions,
            onToutCocher = {
                onBasculerAfficherFaites(true)
                onToutCocher()
            },
            isSearchMode = modeRecherche,
            onToggleSearchMode = {
                if (modeRecherche) quitterRecherche() else modeRecherche = true
            },
            searchTerm = uiState.termeRecherche,
            onSearchTermChange = onSetTermeRecherche,
            enBas = positionBarre == PositionBarre.BAS
        )
    }

    Scaffold(
        topBar = { if (positionBarre == PositionBarre.HAUT) barre() },
        bottomBar = { if (positionBarre == PositionBarre.BAS) barre() },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .then(
                    if (modeRecherche && positionBarre == PositionBarre.HAUT) Modifier.pointerInput(Unit) {
                        detectTapGestures(onTap = { position ->
                            val info = lazyListState.layoutInfo
                            val dernier = info.visibleItemsInfo.lastOrNull()
                            if (dernier != null &&
                                dernier.index == info.totalItemsCount - 1 &&
                                position.y > dernier.offset + dernier.size
                            ) {
                                quitterRecherche()
                            }
                        })
                    } else Modifier
                ),
            verticalArrangement = if (positionBarre == PositionBarre.BAS) Arrangement.Bottom else Arrangement.spacedBy(0.dp)
        ) {
            items(
                count = lignes.size,
                key = { lignes[it].cleUnique }
            ) { index ->
                val ligne = lignes[index]
                ReorderableItem(
                    reorderableState,
                    key = ligne.cleUnique,
                    animateItemModifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                ) { isDragging ->
                    when (ligne) {
                        is LigneAffichee.Header -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clickable {
                                        val rechercheActive = modeRecherche && uiState.termeRecherche.trim().isNotEmpty()
                                        if (rechercheActive) {
                                            if (ligne.replieeAffichee == ligne.categorie.repliee) {
                                                onToggleCategorieRepliee(ligne.categorie)
                                            }
                                            categoriesToucheesEnRecherche = categoriesToucheesEnRecherche + ligne.categorie.nom
                                        } else {
                                            onToggleCategorieRepliee(ligne.categorie)
                                        }
                                    }
                            ) {
                                Icon(
                                    imageVector = if (ligne.replieeAffichee) Icons.Default.ChevronRight else Icons.Default.ExpandMore,
                                    contentDescription = if (ligne.replieeAffichee) "Déplier" else "Replier",
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
                                            text = { Text("Ajouter un produit") },
                                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                            onClick = {
                                                menuOuvertPourCategorie = null
                                                if (modeRecherche) quitterRecherche(CibleDefilement(ligne.cleUnique, null))
                                                categorieCibleAjout = ligne.categorie.nom
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Tri alphabétique") },
                                            leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) },
                                            onClick = {
                                                menuOuvertPourCategorie = null
                                                if (modeRecherche) quitterRecherche(CibleDefilement(ligne.cleUnique, null))
                                                onTrierTachesParAlpha(ligne.categorie.nom)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Modifier la catégorie") },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            onClick = {
                                                menuOuvertPourCategorie = null
                                                if (modeRecherche) quitterRecherche(CibleDefilement(ligne.cleUnique, null))
                                                categorieARenommer = ligne.categorie
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Choix de la couleur") },
                                            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, tint = Color(ligne.categorie.couleur)) },
                                            onClick = {
                                                menuOuvertPourCategorie = null
                                                if (modeRecherche) quitterRecherche(CibleDefilement(ligne.cleUnique, null))
                                                categoriePourCouleur = ligne.categorie
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Supprimer") },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                            onClick = {
                                                menuOuvertPourCategorie = null
                                                if (modeRecherche) quitterRecherche(CibleDefilement(ligne.cleUnique, null))
                                                val nbTaches = uiState.taches.count { it.categorie == ligne.categorie.nom }
                                                categorieASupprimer = ligne.categorie to nbTaches
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        is LigneAffichee.ItemTache -> {
                            val dragModifier = if (modeRecherche) Modifier else Modifier.longPressDraggableHandle(
                                onDragStarted = {
                                    idTacheGlissee = ligne.tache.id
                                    categorieOrigineGlissement = ligne.tache.categorie
                                    categorieFinaleGlissement = ligne.tache.categorie
                                    tachesAvantGlissement = uiState.taches
                                },
                                onDragStopped = {
                                    verifierDoublonApresGlissement()
                                }
                            )
                            TacheItem(
                                tache = ligne.tache,
                                onCheckedChange = { onBasculerFait(ligne.tache) },
                                onClickTache = {
                                    if (modeRecherche) quitterRecherche(
                                        CibleDefilement("tache_${ligne.tache.id}", "header_${ligne.tache.categorie}")
                                    )
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

    categorieCibleAjout?.let { categorie ->
        ProduitEditionDialog(
            tache = null,
            categories = uiState.categories,
            categorieInitiale = categorie,
            onCheckErreurTitre = { titre, cat ->
                val existe = uiState.taches.any {
                    it.categorie == cat && nomsProduitsIdentiques(it.titre, titre)
                }
                if (existe) "Ce produit existe déjà dans cette catégorie." else null
            },
            onValider = { titre, quantite, cat ->
                onAjouterTache(titre, cat, quantite)
                categorieCibleAjout = null
            },
            onFermer = { categorieCibleAjout = null }
        )
    }

    tacheEnEdition?.let { tache ->
        ProduitEditionDialog(
            tache = tache,
            categories = uiState.categories,
            onCheckErreurTitre = { titre, cat ->
                val existe = uiState.taches.any {
                    it.id != tache.id && it.categorie == cat && nomsProduitsIdentiques(it.titre, titre)
                }
                if (existe) "Ce produit existe déjà dans cette catégorie." else null
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

private data class CibleDefilement(
    val cle: String,
    val cleRepli: String?
)

private fun String.sansAccents(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(temp, "")
}
