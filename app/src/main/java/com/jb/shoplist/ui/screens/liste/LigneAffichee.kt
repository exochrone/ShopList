package com.jb.shoplist.ui.screens.liste

import com.jb.shoplist.CategorieEntity
import com.jb.shoplist.TacheEntity

sealed class LigneAffichee {
    abstract val cleUnique: String
    data class Header(
        val categorie: CategorieEntity,
        val nombreNonCoches: Int
    ) : LigneAffichee() {
        override val cleUnique = "header_${categorie.nom}"
    }
    data class ItemTache(val tache: TacheEntity, val couleur: Long) : LigneAffichee() {
        override val cleUnique = "tache_${tache.id}"
    }
}
