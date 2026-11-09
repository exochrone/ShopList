package com.jb.shoplist.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jb.shoplist.TacheEntity
import com.jb.shoplist.ui.theme.ShopListTheme
import kotlin.math.max

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
                if (tache.quantite.isBlank()) {
                    Text(
                        text = tache.titre,
                        fontSize = 14.sp,
                        textDecoration = if (tache.faite) TextDecoration.LineThrough else TextDecoration.None,
                        color = couleurTexteActive,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Layout(
                        content = {
                            Text(
                                text = tache.titre,
                                fontSize = 14.sp,
                                textDecoration = if (tache.faite) TextDecoration.LineThrough else TextDecoration.None,
                                color = couleurTexteActive
                            )
                            Text(
                                text = tache.quantite,
                                fontSize = 14.sp,
                                textDecoration = if (tache.faite) TextDecoration.LineThrough else TextDecoration.None,
                                color = couleurTexteActive,
                                textAlign = TextAlign.End
                            )
                        }
                    ) { measurables, constraints ->
                        val nomMeasurable = measurables[0]
                        val quantiteMeasurable = measurables[1]

                        val quantitePlaceableTemp = quantiteMeasurable.measure(constraints.copy(minWidth = 0))
                        val largeurQuantite = quantitePlaceableTemp.width
                        val nomIntrinsicWidth = nomMeasurable.maxIntrinsicWidth(constraints.maxHeight)
                        val ecartPx = 8.dp.roundToPx()

                        if (nomIntrinsicWidth + ecartPx + largeurQuantite <= constraints.maxWidth) {
                            // Même ligne
                            val nomPlaceable = nomMeasurable.measure(
                                constraints.copy(
                                    minWidth = 0,
                                    maxWidth = max(0, constraints.maxWidth - largeurQuantite - ecartPx)
                                )
                            )
                            val quantitePlaceable = quantiteMeasurable.measure(
                                constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth)
                            )

                            val nomBaseline = if (nomPlaceable[FirstBaseline] != AlignmentLine.Unspecified) nomPlaceable[FirstBaseline] else 0
                            val quantiteBaseline = if (quantitePlaceable[FirstBaseline] != AlignmentLine.Unspecified) quantitePlaceable[FirstBaseline] else 0

                            val maxBaseline = max(nomBaseline, quantiteBaseline)
                            val nomY = maxBaseline - nomBaseline
                            val quantiteY = maxBaseline - quantiteBaseline

                            val totalHeight = max(nomY + nomPlaceable.height, quantiteY + quantitePlaceable.height)

                            layout(constraints.maxWidth, totalHeight) {
                                nomPlaceable.placeRelative(0, nomY)
                                quantitePlaceable.placeRelative(constraints.maxWidth - quantitePlaceable.width, quantiteY)
                            }
                        } else {
                            // Empilé
                            val nomPlaceable = nomMeasurable.measure(
                                constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth)
                            )
                            val quantitePlaceable = quantiteMeasurable.measure(
                                constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth)
                            )

                            val totalHeight = nomPlaceable.height + quantitePlaceable.height

                            layout(constraints.maxWidth, totalHeight) {
                                nomPlaceable.placeRelative(0, 0)
                                quantitePlaceable.placeRelative(constraints.maxWidth - quantitePlaceable.width, nomPlaceable.height)
                            }
                        }
                    }
                }
            }
        }
    }
}
