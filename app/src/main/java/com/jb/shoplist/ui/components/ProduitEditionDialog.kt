package com.jb.shoplist.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jb.shoplist.CategorieEntity
import com.jb.shoplist.TacheEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduitEditionDialog(
    tache: TacheEntity?,
    categories: List<CategorieEntity>,
    categorieInitiale: String = tache?.categorie ?: "",
    onCheckErreurTitre: (titre: String, categorie: String) -> String?,
    onValider: (titre: String, quantite: String, categorie: String) -> Unit,
    onSupprimer: () -> Unit = {},
    onFermer: () -> Unit
) {
    val modeAjout = tache == null

    var titre by rememberSaveable { mutableStateOf(tache?.titre ?: "") }
    var quantite by rememberSaveable { mutableStateOf(tache?.quantite ?: "") }
    var categorie by rememberSaveable { mutableStateOf(tache?.categorie ?: categorieInitiale) }
    var menuCategoriesOuvert by remember { mutableStateOf(false) }

    val focusRequesterTitre = remember { FocusRequester() }
    val focusRequesterQuantite = remember { FocusRequester() }

    val titreClean = titre.trim()
    val quantiteClean = quantite.trim()

    val erreurTitre = onCheckErreurTitre(titreClean, categorie)
    val validerActif = titreClean.isNotEmpty() && erreurTitre == null

    val aModifie = modeAjout ||
            titreClean != tache.titre.trim() ||
            quantiteClean != tache.quantite.trim() ||
            categorie != tache.categorie

    fun executerValidation() {
        if (!validerActif) return
        if (aModifie) {
            onValider(titreClean, quantiteClean, categorie)
        } else {
            onFermer()
        }
    }

    val couleurCategorie = categories.find { it.nom == categorie }
        ?.let { Color(it.couleur) }
        ?: Color.Unspecified

    LaunchedEffect(Unit) {
        focusRequesterTitre.requestFocus()
    }

    Dialog(
        onDismissRequest = onFermer,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Barre de titre
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (modeAjout) "Nouveau produit" else "Modifier le produit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onFermer) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer"
                        )
                    }
                }

                // Sélecteur de catégorie
                ExposedDropdownMenuBox(
                    expanded = menuCategoriesOuvert,
                    onExpandedChange = { menuCategoriesOuvert = it }
                ) {
                    OutlinedTextField(
                        value = categorie,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie") },
                        textStyle = LocalTextStyle.current.copy(color = couleurCategorie),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuCategoriesOuvert) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = menuCategoriesOuvert,
                        onDismissRequest = { menuCategoriesOuvert = false }
                    ) {
                        categories.sortedBy { it.ordre }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nom, color = Color(cat.couleur)) },
                                onClick = {
                                    categorie = cat.nom
                                    menuCategoriesOuvert = false
                                }
                            )
                        }
                    }
                }

                // Champ nom du produit
                OutlinedTextField(
                    value = titre,
                    onValueChange = { titre = it },
                    singleLine = true,
                    label = { Text("Nom du produit") },
                    placeholder = { Text("Nom du produit") },
                    isError = erreurTitre != null,
                    supportingText = erreurTitre?.let { msg ->
                        { Text(text = msg, color = MaterialTheme.colorScheme.error) }
                    },
                    trailingIcon = {
                        if (titre.isNotEmpty()) {
                            IconButton(onClick = {
                                titre = ""
                                focusRequesterTitre.requestFocus()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Effacer"
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(onNext = {
                        focusRequesterQuantite.requestFocus()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterTitre)
                )

                // Champ quantité
                OutlinedTextField(
                    value = quantite,
                    onValueChange = { quantite = it },
                    singleLine = true,
                    label = { Text("Quantité (facultatif)") },
                    placeholder = { Text("Quantité (facultatif)") },
                    trailingIcon = {
                        if (quantite.isNotEmpty()) {
                            IconButton(onClick = {
                                quantite = ""
                                focusRequesterQuantite.requestFocus()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Effacer"
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        executerValidation()
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterQuantite)
                )

                // Ligne de boutons
                if (modeAjout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { executerValidation() },
                            enabled = validerActif
                        ) {
                            Text("Ajouter")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onSupprimer,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Supprimer")
                        }
                        TextButton(
                            onClick = { executerValidation() },
                            enabled = validerActif
                        ) {
                            Text("Valider")
                        }
                    }
                }
            }
        }
    }
}
