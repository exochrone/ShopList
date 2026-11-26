package com.jb.shoplist.ui.screens.options

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jb.shoplist.EtatRestauration
import com.jb.shoplist.PositionBarre
import com.jb.shoplist.ui.components.SaisieDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    titreListe: String,
    versionApp: String,
    onModifierTitreListe: (String) -> Unit,
    onOuvrirCategories: () -> Unit,
    positionBarre: PositionBarre,
    onChangerPositionBarre: (PositionBarre) -> Unit,
    onImporterCsv: (Uri) -> Unit,
    onSauvegardeComplete: (Uri) -> Unit,
    etatRestauration: EtatRestauration,
    onLireFichierSauvegarde: (Uri) -> Unit,
    onConfirmerRestauration: () -> Unit,
    onAnnulerRestauration: () -> Unit,
    onConsommerErreurRestauration: () -> Unit,
    onRetour: () -> Unit
) {
    var afficherDialogRenommer by remember { mutableStateOf(false) }
    var afficherDialogImport by remember { mutableStateOf(false) }
    var afficherDialogPreRestauration by remember { mutableStateOf(false) }

    val launcherImport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImporterCsv(it) }
    }

    val launcherSauvegardeComplete = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { onSauvegardeComplete(it) }
    }

    val launcherRestauration = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onLireFichierSauvegarde(it) }
    }

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

            // 1. Zone « Nom de la liste »
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

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )

            // 2. Option « Position de la barre de titre et d'options »
            Text(
                text = "Position de la barre de titre et d'options",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = positionBarre == PositionBarre.HAUT,
                    onClick = { onChangerPositionBarre(PositionBarre.HAUT) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Haut") }
                SegmentedButton(
                    selected = positionBarre == PositionBarre.BAS,
                    onClick = { onChangerPositionBarre(PositionBarre.BAS) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Bas") }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )

            // 3. Bouton « Gérer les catégories »
            OutlinedButton(
                onClick = onOuvrirCategories,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gérer les catégories")
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )

            // 4. Bouton « Importer une liste »
            OutlinedButton(
                onClick = { afficherDialogImport = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Importer une liste")
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )

            // 5. Bouton « Sauvegarde complète »
            OutlinedButton(
                onClick = {
                    val nomFichier = "shopList-Backup-${
                        SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())
                    }.json"
                    launcherSauvegardeComplete.launch(nomFichier)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sauvegarde complète")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Bouton « Restaurer une sauvegarde »
            OutlinedButton(
                onClick = { afficherDialogPreRestauration = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restaurer une sauvegarde")
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )

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

    val etatFichierLu = etatRestauration as? EtatRestauration.FichierLu
    if (etatFichierLu != null) {
        AlertDialog(
            onDismissRequest = onAnnulerRestauration,
            title = { Text("Confirmer la restauration") },
            text = {
                val contenu = etatFichierLu.contenu
                val dateLisible = contenu.dateSauvegarde
                    ?.substringBefore("T")
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
}
