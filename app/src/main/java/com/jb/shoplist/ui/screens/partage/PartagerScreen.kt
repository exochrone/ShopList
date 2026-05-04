package com.jb.shoplist.ui.screens.partage

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jb.shoplist.CategorieEntity
import com.jb.shoplist.TacheEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartagerScreen(
    categories: List<CategorieEntity>,
    taches: List<TacheEntity>,
    titreListe: String,
    onExporterCsv: (android.net.Uri) -> Unit,
    onRetour: () -> Unit
) {
    var masquerCategories by remember { mutableStateOf(false) }
    var afficherCoches by remember { mutableStateOf(false) }
    var afficherDialogExport by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val launcherExportCsv = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: android.net.Uri? ->
        uri?.let { onExporterCsv(it) }
    }

    val texteCalcule = remember(categories, taches, masquerCategories, afficherCoches) {
        buildString {
            categories.forEach { cat ->
                val tachesDeLaCat = taches.filter { it.categorie == cat.nom }
                val tachesAffichees = if (afficherCoches) {
                    tachesDeLaCat
                } else {
                    tachesDeLaCat.filter { !it.faite }
                }

                if (tachesAffichees.isNotEmpty()) {
                    if (!masquerCategories) {
                        appendLine(cat.nom)
                    }
                    tachesAffichees.forEach { tache ->
                        val prefixe = if (masquerCategories) "" else "    "
                        appendLine("$prefixe${tache.titre}")
                    }
                }
            }
        }.trimEnd()
    }

    var texteModifie by remember { mutableStateOf<String?>(null) }
    val texteAafficher = texteModifie ?: texteCalcule
    val estModifieManuel = texteModifie != null

    val clavierVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partager la liste") },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (estModifieManuel) {
                        IconButton(onClick = { texteModifie = null }) {
                            Icon(Icons.Default.Undo, contentDescription = "Rétablir l'automatique")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (!clavierVisible) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(30.dp)
                    ) {
                        Checkbox(
                            checked = masquerCategories,
                            onCheckedChange = { masquerCategories = it },
                            enabled = !estModifieManuel
                        )
                        Text(
                            text = "Masquer les catégories",
                            fontSize = 14.sp,
                            color = if (estModifieManuel) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(30.dp)
                    ) {
                        Checkbox(
                            checked = afficherCoches,
                            onCheckedChange = { afficherCoches = it },
                            enabled = !estModifieManuel
                        )
                        Text(
                            text = "Afficher les éléments cochés",
                            fontSize = 14.sp,
                            color = if (estModifieManuel) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = texteAafficher,
                onValueChange = { texteModifie = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imePadding(),
                label = { Text(if (estModifieManuel) "Contenu (modifié manuellement)" else "Contenu à partager") }
            )

            if (!clavierVisible) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { afficherDialogExport = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Export CSV")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onRetour) {
                        Text("Annuler")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, texteAafficher)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                        onRetour()
                    }) {
                        Text("Partager")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (afficherDialogExport) {
        AlertDialog(
            onDismissRequest = { afficherDialogExport = false },
            title = { Text("Export CSV") },
            text = { Text("Création d'un fichier CSV contenant toutes les tâches de l'application (cochées ou pas) avec leur catégorie.\n\nValider pour choisir l'emplacement de sauvegarde du fichier.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        afficherDialogExport = false
                        val dateString = android.text.format.DateFormat.format("yyMMdd-HHmmss", java.util.Date())
                        val nomFichier = "shopList-${titreListe.replace(" ", "_")}-$dateString.csv"
                        launcherExportCsv.launch(nomFichier)
                    }
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { afficherDialogExport = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
