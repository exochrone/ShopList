package com.jb.shoplist.ui.screens.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jb.shoplist.ActionAnnulable
import com.jb.shoplist.CategorieEntity
import com.jb.shoplist.TacheEntity
import com.jb.shoplist.ui.components.ColorPickerDialog
import com.jb.shoplist.ui.components.SaisieDialog
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    categories: List<CategorieEntity>,
    taches: List<TacheEntity>,
    actionAnnulable: ActionAnnulable?,
    onAjouterCategorie: (String) -> Unit,
    onSupprimerCategorie: (CategorieEntity) -> Unit,
    onReordonnerCategories: (List<CategorieEntity>) -> Unit,
    onRenommerCategorie: (CategorieEntity, String) -> Unit,
    onModifierCouleur: (CategorieEntity, Long) -> Unit,
    onTrierTachesParAlpha: (String) -> Unit,
    onRetour: () -> Unit,
    onAnnulerAction: () -> Unit,
    onConsommerAction: () -> Unit
) {
    var afficherDialogAjout by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionAnnulable) {
        actionAnnulable?.let { action ->
            val result = snackbarHostState.showSnackbar(
                message = action.message,
                actionLabel = "Annuler",
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.ActionPerformed -> onAnnulerAction()
                SnackbarResult.Dismissed -> onConsommerAction()
            }
        }
    }

    var categorieEnEdition by remember { mutableStateOf<CategorieEntity?>(null) }

    var categorieASupprimer by remember { mutableStateOf<Pair<CategorieEntity, Int>?>(null) }
    var categoriePourCouleur by remember { mutableStateOf<CategorieEntity?>(null) }
    val categoriesTriees = remember { mutableStateListOf<String>() }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReordonnerCategories(
            categories.toMutableList().apply { add(to.index, removeAt(from.index)) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestion des catégories", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        afficherDialogAjout = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter une catégorie")
                    }
                }
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                count = categories.size,
                key = { categories[it].nom }
            ) { index ->
                val categorie = categories[index]
                val nbTaches = taches.count { it.categorie == categorie.nom }
                val couleurCat = Color(categorie.couleur)

                ReorderableItem(reorderableState, key = categorie.nom) { isDragging ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .longPressDraggableHandle(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDragging)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                Color.Transparent
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = "Déplacer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        categorieEnEdition = categorie
                                    }
                            ) {
                                Text(
                                    text = categorie.nom,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = couleurCat
                                )
                                Text(
                                    text = if (nbTaches == 0) "Vide"
                                    else "$nbTaches tâche${if (nbTaches > 1) "s" else ""}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val estTriee = categoriesTriees.contains(categorie.nom)
                            IconButton(
                                onClick = {
                                    onTrierTachesParAlpha(categorie.nom)
                                    categoriesTriees.add(categorie.nom)
                                },
                                enabled = !estTriee
                            ) {
                                Icon(
                                    Icons.Default.SortByAlpha,
                                    contentDescription = "Trier par ordre alphabétique",
                                    tint = if (estTriee) 
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { categoriePourCouleur = categorie }) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = "Couleur",
                                    tint = couleurCat
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (nbTaches == 0) {
                                        onSupprimerCategorie(categorie)
                                    } else {
                                        categorieASupprimer = categorie to nbTaches
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (categoriePourCouleur != null) {
        ColorPickerDialog(
            onDismiss = { categoriePourCouleur = null },
            onColorSelected = { couleur ->
                categoriePourCouleur?.let { onModifierCouleur(it, couleur.toArgb().toLong()) }
                categoriePourCouleur = null
            }
        )
    }

    if (afficherDialogAjout) {
        SaisieDialog(
            titre = "Nouvelle catégorie",
            placeholder = "Nom de la catégorie",
            libelleValider = "Ajouter",
            onValider = { nom ->
                if (categories.none { it.nom == nom }) {
                    onAjouterCategorie(nom)
                }
                afficherDialogAjout = false
            },
            onAnnuler = { afficherDialogAjout = false }
        )
    }

    categorieEnEdition?.let { cat ->
        SaisieDialog(
            titre = "Modifier la catégorie",
            valeurInitiale = cat.nom,
            placeholder = "Nom de la catégorie",
            onValider = { nom ->
                onRenommerCategorie(cat, nom)
                categorieEnEdition = null
            },
            onAnnuler = { categorieEnEdition = null },
            actionsSupplementaires = {
                TextButton(
                    onClick = {
                        val nbTaches = taches.count { it.categorie == cat.nom }
                        if (nbTaches == 0) {
                            onSupprimerCategorie(cat)
                            categorieEnEdition = null
                        } else {
                            categorieASupprimer = cat to nbTaches
                            categorieEnEdition = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Supprimer") }
            }
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
