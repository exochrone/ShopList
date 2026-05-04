package com.jb.shoplist.ui.screens.liste

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListTopBar(
    titre: String,
    afficherFaites: Boolean,
    onToggleAfficherFaites: () -> Unit,
    onAjouterCategorie: () -> Unit,
    onOuvrirCategories: () -> Unit,
    onOuvrirPartage: () -> Unit,
    onOuvrirImport: () -> Unit,
    onRenommerListe: () -> Unit,
    onToutCocher: () -> Unit,
    isSearchMode: Boolean,
    onToggleSearchMode: () -> Unit,
    searchTerm: String,
    onSearchTermChange: (String) -> Unit
) {
    var afficherMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            focusRequester.requestFocus()
        }
    }

    TopAppBar(
        title = {
            if (isSearchMode) {
                TextField(
                    value = searchTerm,
                    onValueChange = onSearchTermChange,
                    placeholder = { Text("Rechercher...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
            } else {
                Text(titre, fontWeight = FontWeight.Bold)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        actions = {
            if (isSearchMode) {
                IconButton(onClick = {
                    if (searchTerm.isEmpty()) {
                        onToggleSearchMode()
                    } else {
                        onSearchTermChange("")
                    }
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer la recherche")
                }
            } else {
                IconButton(onClick = onToggleSearchMode) {
                    Icon(Icons.Default.Search, contentDescription = "Rechercher")
                }
                IconButton(onClick = onToggleAfficherFaites) {
                    Icon(
                        imageVector = if (afficherFaites) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (afficherFaites) "Masquer les tâches faites" else "Afficher toutes les tâches"
                    )
                }
                Box {
                    IconButton(onClick = { afficherMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = afficherMenu,
                        onDismissRequest = { afficherMenu = false }
                    ) {
                        /*
                        DropdownMenuItem(
                            text = { Text("Ajouter une catégorie") },
                            onClick = {
                                afficherMenu = false
                                onAjouterCategorie()
                            }
                        )
                        */
                        DropdownMenuItem(
                            text = { Text("Gérer les catégories") },
                            onClick = {
                                afficherMenu = false
                                onOuvrirCategories()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Partager la liste") },
                            onClick = {
                                afficherMenu = false
                                onOuvrirPartage()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Importer une liste") },
                            onClick = {
                                afficherMenu = false
                                onOuvrirImport()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Renommer la liste") },
                            onClick = {
                                afficherMenu = false
                                onRenommerListe()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Tout cocher") },
                            onClick = {
                                afficherMenu = false
                                onToutCocher()
                            }
                        )
                    }
                }
            }
        }
    )
}
