package com.jb.shoplist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TacheDao {

    @Query("SELECT * FROM taches ORDER BY ordre ASC")
    fun observerToutes(): Flow<List<TacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserer(tache: TacheEntity): Long

    @Update
    suspend fun mettreAJour(tache: TacheEntity)

    @Update
    suspend fun mettreAJourToutes(taches: List<TacheEntity>)

    @Delete
    suspend fun supprimer(tache: TacheEntity)

    @Query("UPDATE taches SET categorie = :nouveauNom WHERE categorie = :ancienNom")
    suspend fun renommerCategorie(ancienNom: String, nouveauNom: String)

    @Query("SELECT COUNT(*) FROM taches WHERE categorie = :nom")
    suspend fun compterParCategorie(nom: String): Int

    @Query("SELECT * FROM taches WHERE titre = :titre AND categorie = :categorie LIMIT 1")
    suspend fun trouverParTitreEtCategorie(titre: String, categorie: String): TacheEntity?

    @Query("SELECT MAX(ordre) FROM taches")
    suspend fun obtenirOrdreMax(): Int?

    @Query("DELETE FROM taches WHERE categorie = :nomCategorie")
    suspend fun supprimerParCategorie(nomCategorie: String)

    @Query("DELETE FROM taches")
    suspend fun supprimerToutes()
}

@Dao
interface CategorieDao {

    @Query("SELECT * FROM categories ORDER BY ordre ASC")
    fun observerToutes(): Flow<List<CategorieEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserer(categorie: CategorieEntity)

    @Update
    suspend fun mettreAJour(categorie: CategorieEntity)

    @Update
    suspend fun mettreAJourToutes(categories: List<CategorieEntity>)

    @Delete
    suspend fun supprimer(categorie: CategorieEntity)

    @Query("DELETE FROM categories")
    suspend fun supprimerToutes()
}

@Dao
interface ParametreDao {

    @Query("SELECT valeur FROM parametres WHERE cle = :cle LIMIT 1")
    fun observer(cle: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enregistrer(parametre: ParametreEntity)

    @Query("DELETE FROM parametres")
    suspend fun supprimerTous()
}