package com.jb.shoplist.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jb.shoplist.TacheEntity
import com.jb.shoplist.ui.theme.ShopListTheme

@Composable
fun TacheItem(
    tache: TacheEntity,
    onCheckedChange: () -> Unit,
    onClickTache: () -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false,
    couleurCase: Color = Color.Unspecified
) {
    val couleurFaite = ShopListTheme.customColors.tacheFaite
    val couleurTexte = ShopListTheme.customColors.tacheNonFaite

    val couleurTexteActive = if (tache.faite) couleurFaite else couleurTexte
    val couleurCaseActive = if (tache.faite) couleurFaite else couleurCase
    val couleurQuantite = if (couleurCase != Color.Unspecified) couleurCase else couleurTexte

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Checkbox(
                checked = tache.faite,
                onCheckedChange = { onCheckedChange() },
                modifier = Modifier
                    .size(32.dp)
                    .padding(0.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = couleurFaite,
                    uncheckedColor = couleurCaseActive,
                    checkmarkColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClickTache() }
                    .padding(vertical = 0.dp)
            ) {
                Text(
                    text = libelleProduit(
                        titre = tache.titre,
                        quantite = tache.quantite,
                        faite = tache.faite,
                        couleurQuantite = couleurQuantite
                    ),
                    fontSize = 14.sp,
                    textDecoration = if (tache.faite) TextDecoration.LineThrough else TextDecoration.None,
                    color = couleurTexteActive,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun libelleProduit(
    titre: String,
    quantite: String,
    faite: Boolean,
    couleurQuantite: Color
): AnnotatedString = buildAnnotatedString {
    append(titre)
    if (!faite && quantite.isNotBlank()) {
        append(" ")
        withStyle(SpanStyle(fontSize = 12.sp, color = couleurQuantite)) {
            append(quantite.trim().replace(' ', ' '))
        }
    }
}
