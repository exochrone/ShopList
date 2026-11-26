package com.jb.shoplist.ui.screens.options

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jb.shoplist.ui.components.SaisieDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    titreListe: String,
    versionApp: String,
    onModifierTitreListe: (String) -> Unit,
    onOuvrirCategories: () -> Unit,
    onRetour: () -> Unit
) {
    var afficherDialogRenommer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Options", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Zone « Nom de la liste »
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { afficherDialogRenommer = true }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Nom de la liste",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = titreListe,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bouton « Gérer les catégories »
            OutlinedButton(
                onClick = onOuvrirCategories,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gérer les catégories")
            }

            Spacer(modifier = Modifier.weight(1f))

            // Zone d'information (version)
            Text(
                text = "shopList v$versionApp",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (afficherDialogRenommer) {
        SaisieDialog(
            titre = "Renommer la liste",
            valeurInitiale = titreListe,
            placeholder = "Nouveau titre",
            onValider = { titre ->
                onModifierTitreListe(titre)
                afficherDialogRenommer = false
            },
            onAnnuler = { afficherDialogRenommer = false }
        )
    }
}
