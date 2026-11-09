package com.jb.shoplist

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "taches")
data class TacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titre: String,
    val categorie: String,
    val faite: Boolean = false,
    val ordre: Int = 0,
    @ColumnInfo(defaultValue = "")
    val quantite: String = ""
)

@Entity(tableName = "categories")
data class CategorieEntity(
    @PrimaryKey val nom: String,
    val ordre: Int = 0,
    val couleur: Long = 0xFFFFFFFF, // Blanc par défaut
    val repliee: Boolean = false
)

@Entity(tableName = "parametres")
data class ParametreEntity(
    @PrimaryKey val cle: String,
    val valeur: String
)