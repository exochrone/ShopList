package com.jb.shoplist.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

/**
 * Dialog générique de saisie d'une valeur textuelle.
 *
 * Gère automatiquement :
 * - Le focus initial sur le champ
 * - La validation du texte (trim + isNotBlank)
 * - Les boutons Valider et Annuler
 * - Les actions supplémentaires optionnelles (ex: bouton Supprimer)
 */
@Composable
fun SaisieDialog(
    titre: String,
    valeurInitiale: String = "",
    placeholder: String = "",
    libelleValider: String = "Valider",
    validerSiVide: Boolean = false,
    onCheckErreur: (String) -> String? = { null },
    onValider: (String) -> Unit,
    onAnnuler: () -> Unit,
    actionsSupplementaires: (@Composable RowScope.() -> Unit)? = null
) {
    var valeur by remember { mutableStateOf(valeurInitiale) }
    val erreur = remember(valeur) { onCheckErreur(valeur) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(titre) },
        text = {
            OutlinedTextField(
                value = valeur,
                onValueChange = { valeur = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                isError = erreur != null,
                supportingText = {
                    if (erreur != null) {
                        Text(erreur)
                    }
                },
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                actionsSupplementaires?.invoke(this)
                TextButton(
                    onClick = {
                        val v = valeur.trim()
                        if (validerSiVide || v.isNotBlank()) {
                            onValider(v)
                        } else {
                            onAnnuler()
                        }
                    },
                    enabled = erreur == null
                ) { Text(libelleValider) }
            }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    )
}
