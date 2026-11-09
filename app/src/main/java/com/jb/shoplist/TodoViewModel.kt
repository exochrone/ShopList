package com.jb.shoplist

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val UNDO_STACK_MAX_SIZE = 10
private const val FORMAT_VERSION_BACKUP = 2

sealed class ActionAnnulable {
    abstract val message: String

    data class SuppressionTache(
        val tache: TacheEntity
    ) : ActionAnnulable() {
        override val message = "Tâche supprimée"
    }

    data class SuppressionCategorie(
        val categorie: CategorieEntity,
        val tachesAssociees: List<TacheEntity>
    ) : ActionAnnulable() {
        override val message = "Catégorie \"${categorie.nom}\" supprimée"
    }

    data class ToutCocher(
        val etatsPrecedents: List<TacheEntity>
    ) : ActionAnnulable() {
        override val message = "${etatsPrecedents.size} tâche(s) cochée(s)"
    }
}

/**
 * Représente une action de cochage / décochage potentiellement annulable.
 *
 * @property tacheId identifiant de la tâche concernée
 * @property etatPrecedent valeur de [TacheEntity.faite] avant l'action
 *                        (donc l'état à restaurer en cas d'annulation)
 */
data class ActionCochage(
    val tacheId: Int,
    val etatPrecedent: Boolean
)

/**
 * Résultat de la lecture et validation d'un fichier de sauvegarde.
 * Utilisé pour présenter un résumé avant restauration définitive.
 */
data class ContenuSauvegarde(
    val titreListe: String,
    val nbCategories: Int,
    val nbTaches: Int,
    val dateSauvegarde: String?,
    val versionApp: String?,
    // Données complètes pour la restauration
    val parametres: List<ParametreEntity>,
    val categories: List<CategorieEntity>,
    val taches: List<TacheEntity>
)

/**
 * États possibles de l'opération de restauration.
 */
sealed class EtatRestauration {
    object Inactif : EtatRestauration()
    data class FichierLu(val contenu: ContenuSauvegarde) : EtatRestauration()
    data class Erreur(val message: String) : EtatRestauration()
}

data class TodoUiState(
    val taches: List<TacheEntity> = emptyList(),
    val categories: List<CategorieEntity> = emptyList(),
    val titreListe: String = "Courses",
    val afficherFaites: Boolean = true,
    val termeRecherche: String = ""
)

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = TodoDatabase.obtenir(application)
    private val tacheDao = database.tacheDao()
    private val categorieDao = database.categorieDao()
    private val parametreDao = database.parametreDao()

    private val _evenements = MutableSharedFlow<String>()
    val evenements = _evenements.asSharedFlow()

    private val _afficherFaites = MutableStateFlow(true)
    private val _termeRecherche = MutableStateFlow("")
    private val _actionAnnulable = MutableStateFlow<ActionAnnulable?>(null)

    val actionAnnulable: StateFlow<ActionAnnulable?> = _actionAnnulable

    private val _pileCochages = MutableStateFlow<List<ActionCochage>>(emptyList())
    val pileCochages: StateFlow<List<ActionCochage>> = _pileCochages.asStateFlow()

    private val _etatRestauration = MutableStateFlow<EtatRestauration>(EtatRestauration.Inactif)
    val etatRestauration: StateFlow<EtatRestauration> = _etatRestauration.asStateFlow()

    val uiState: StateFlow<TodoUiState> = combine(
        tacheDao.observerToutes(),
        categorieDao.observerToutes(),
        parametreDao.observer("titreListe"),
        parametreDao.observer("afficherFaites"),
        _termeRecherche
    ) { taches, categories, titre, afficherFaitesParam, termeRecherche ->
        TodoUiState(
            taches = taches,
            categories = categories,
            titreListe = titre ?: "Courses",
            afficherFaites = afficherFaitesParam?.toBoolean() ?: true,
            termeRecherche = termeRecherche
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodoUiState()
    )

    init {
        viewModelScope.launch {
            // Si la base est vide, on insère les données par défaut
            val categoriesInitiales = categorieDao.observerToutes().first()
            if (categoriesInitiales.isEmpty()) {
                categorieDao.inserer(CategorieEntity(nom = "Personnel", ordre = 0))
                tacheDao.inserer(TacheEntity(titre = "Faire les courses", categorie = "Personnel", ordre = 0))
                tacheDao.inserer(TacheEntity(titre = "Appeler le médecin", categorie = "Personnel", ordre = 1))
                tacheDao.inserer(TacheEntity(titre = "Lire 30 minutes", categorie = "Personnel", ordre = 2))
            }
        }
    }

    fun setAfficherFaites(afficher: Boolean) {
        viewModelScope.launch {
            parametreDao.enregistrer(ParametreEntity(cle = "afficherFaites", valeur = afficher.toString()))
        }
    }

    private fun viderPileCochages() {
        _pileCochages.value = emptyList()
    }

    fun setTermeRecherche(terme: String) {
        _termeRecherche.value = terme
    }

    fun ajouterTache(titre: String, categorie: String, quantite: String) {
        viewModelScope.launch {
            try {
                val ordre = (tacheDao.obtenirOrdreMaxParCategorie(categorie) ?: -1) + 1
                tacheDao.inserer(
                    TacheEntity(
                        titre = titre.trim(),
                        categorie = categorie,
                        quantite = quantite.trim(),
                        ordre = ordre
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _evenements.emit("Erreur lors de l'ajout du produit : ${e.message}")
            }
        }
    }

    fun basculerFait(tache: TacheEntity) {
        viewModelScope.launch {
            // 1. Empiler l'état précédent AVANT de modifier
            empilerActionCochage(ActionCochage(tache.id, tache.faite))

            // 2. Modifier la tâche (logique existante)
            tacheDao.mettreAJour(tache.copy(faite = !tache.faite))
        }
    }

    private fun empilerActionCochage(action: ActionCochage) {
        _pileCochages.update { pile ->
            // On limite à UNDO_STACK_MAX_SIZE en supprimant les plus anciennes (en tête)
            val nouvelle = (pile + action)
            if (nouvelle.size > UNDO_STACK_MAX_SIZE) {
                nouvelle.takeLast(UNDO_STACK_MAX_SIZE)
            } else {
                nouvelle
            }
        }
    }

    fun annulerDernierCochage() {
        viewModelScope.launch {
            val pile = _pileCochages.value
            if (pile.isEmpty()) return@launch

            val derniereAction = pile.last()

            // Récupérer la tâche actuelle (peut avoir été modifiée entre-temps)
            val tacheActuelle = uiState.value.taches.firstOrNull { it.id == derniereAction.tacheId }
            if (tacheActuelle != null) {
                tacheDao.mettreAJour(tacheActuelle.copy(faite = derniereAction.etatPrecedent))
            }

            // Retirer cette action de la pile, qu'elle ait été appliquée ou non
            _pileCochages.update { it.dropLast(1) }
        }
    }

    fun modifierProduit(
        tache: TacheEntity,
        nouveauTitre: String,
        nouvelleQuantite: String,
        nouvelleCategorie: String
    ) {
        viewModelScope.launch {
            try {
                val titreNettoye = nouveauTitre.trim()
                val quantiteNettoyee = nouvelleQuantite.trim()

                val nouvelOrdre = if (tache.categorie != nouvelleCategorie) {
                    (tacheDao.obtenirOrdreMaxParCategorie(nouvelleCategorie) ?: -1) + 1
                } else {
                    tache.ordre
                }

                tacheDao.mettreAJour(
                    tache.copy(
                        titre = titreNettoye,
                        quantite = quantiteNettoyee,
                        categorie = nouvelleCategorie,
                        ordre = nouvelOrdre
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _evenements.emit("Erreur lors de la modification du produit : ${e.message}")
            }
        }
    }

    fun supprimerTache(tache: TacheEntity) {
        viewModelScope.launch {
            viderPileCochages()
            tacheDao.supprimer(tache)
            _actionAnnulable.value = ActionAnnulable.SuppressionTache(tache)
        }
    }

    fun reordonnerTaches(nouvellesTaches: List<TacheEntity>) {
        viewModelScope.launch {
            val avecOrdres = nouvellesTaches.mapIndexed { index, tache ->
                tache.copy(ordre = index)
            }
            tacheDao.mettreAJourToutes(avecOrdres)
        }
    }

    fun trierTachesParAlpha(nomCategorie: String) {
        viewModelScope.launch {
            val toutesLesTaches = uiState.value.taches.toMutableList()
            // On extrait les tâches de la catégorie et on les trie
            val tachesDeLaCat = toutesLesTaches.filter { it.categorie == nomCategorie }
                .sortedWith(compareBy(comparateurAlphabetique()) { it.titre })

            // On reconstruit la liste complète en remplaçant les tâches de la catégorie par les triées
            var indexTriee = 0
            val nouvelleListe = toutesLesTaches.map { tache ->
                if (tache.categorie == nomCategorie) {
                    tachesDeLaCat[indexTriee++]
                } else {
                    tache
                }
            }
            reordonnerTaches(nouvelleListe)
        }
    }

    fun ajouterCategorie(nom: String) {
        viewModelScope.launch {
            val categoriesExistantes = uiState.value.categories.size
            categorieDao.inserer(CategorieEntity(nom = nom, ordre = categoriesExistantes))
        }
    }

    fun supprimerCategorie(categorie: CategorieEntity) {
        viewModelScope.launch {
            viderPileCochages()
            val tachesDeLaCat = uiState.value.taches.filter { it.categorie == categorie.nom }
            database.withTransaction {
                // 1. Supprimer toutes les tâches de la catégorie
                tacheDao.supprimerParCategorie(categorie.nom)
                // 2. Supprimer la catégorie elle-même
                categorieDao.supprimer(categorie)
            }
            _actionAnnulable.value = ActionAnnulable.SuppressionCategorie(categorie, tachesDeLaCat)
        }
    }

    fun reordonnerCategories(nouvellesCategories: List<CategorieEntity>) {
        viewModelScope.launch {
            val avecOrdres = nouvellesCategories.mapIndexed { index, cat ->
                cat.copy(ordre = index)
            }
            categorieDao.mettreAJourToutes(avecOrdres)
        }
    }

    // Dans TodoViewModel.kt
    fun renommerCategorie(categorie: CategorieEntity, nouveauNom: String) {
        viewModelScope.launch {
            if (categorie.nom == nouveauNom) return@launch

            database.withTransaction {
                // 1. Créer la nouvelle catégorie avec le même ordre et même couleur
                categorieDao.inserer(CategorieEntity(
                    nom = nouveauNom, 
                    ordre = categorie.ordre,
                    couleur = categorie.couleur
                ))
                // 2. Mettre à jour le nom de la catégorie dans toutes les tâches liées
                tacheDao.renommerCategorie(categorie.nom, nouveauNom)
                // 3. Supprimer l'ancienne catégorie
                categorieDao.supprimer(categorie)
            }
        }
    }

    fun modifierCouleurCategorie(categorie: CategorieEntity, nouvelleCouleur: Long) {
        viewModelScope.launch {
            categorieDao.mettreAJour(categorie.copy(couleur = nouvelleCouleur))
        }
    }

    fun toggleCategorieRepliee(categorie: CategorieEntity) {
        viewModelScope.launch {
            categorieDao.mettreAJour(categorie.copy(repliee = !categorie.repliee))
        }
    }

    fun importerDepuisCsv(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                viderPileCochages()
                val context = getApplication<Application>().applicationContext
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val reader = inputStream?.bufferedReader()

                val lignes = reader?.readLines() ?: emptyList()

                database.withTransaction {
                    // Liste des catégories existantes pour éviter les doublons durant l'import
                    val categoriesActuelles = uiState.value.categories.map { it.nom }.toMutableSet()
                    var ordreMaxCat = uiState.value.categories.maxOfOrNull { it.ordre } ?: -1
                    var ordreMaxTache = tacheDao.obtenirOrdreMax() ?: -1

                    lignes.forEach { ligne ->
                        val colonnes = parserLigneCsv(ligne)
                        if (colonnes.size >= 2) {
                            val nomCat = colonnes[0].trim()
                            val titreTache = colonnes[1].trim()
                            val quantiteImportee = if (colonnes.size >= 3) colonnes[2].trim() else ""

                            if (nomCat.isNotBlank() && titreTache.isNotBlank()) {
                                // 1. Gérer la catégorie
                                if (!categoriesActuelles.contains(nomCat)) {
                                    ordreMaxCat++
                                    categorieDao.inserer(CategorieEntity(nom = nomCat, ordre = ordreMaxCat))
                                    categoriesActuelles.add(nomCat)
                                }

                                // 2. Gérer la tâche
                                val tacheExistante = tacheDao.trouverParTitreEtCategorie(titreTache, nomCat)
                                if (tacheExistante == null) {
                                    ordreMaxTache++
                                    tacheDao.inserer(
                                        TacheEntity(
                                            titre = titreTache,
                                            categorie = nomCat,
                                            ordre = ordreMaxTache,
                                            quantite = quantiteImportee
                                        )
                                    )
                                } else if (quantiteImportee.isNotBlank()) {
                                    tacheDao.mettreAJour(
                                        tacheExistante.copy(quantite = quantiteImportee)
                                    )
                                }
                            }
                        }
                    }
                }
                inputStream?.close()
                _evenements.emit("Importation réussie !")
            } catch (e: Exception) {
                e.printStackTrace()
                _evenements.emit("Erreur lors de l'importation : ${e.message}")
            }
        }
    }

    fun exporterCsv(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    // BOM UTF-8 pour Excel
                    stream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        uiState.value.categories.forEach { cat ->
                            val tachesCat = uiState.value.taches.filter { it.categorie == cat.nom }
                            if (tachesCat.isEmpty()) {
                                writer.write("${escapeCsv(cat.nom)};")
                                writer.newLine()
                            } else {
                                tachesCat.forEach { tache ->
                                    writer.write("${escapeCsv(cat.nom)};${escapeCsv(tache.titre)};${escapeCsv(tache.quantite)}")
                                    writer.newLine()
                                }
                            }
                        }
                    }
                }
                _evenements.emit("Export CSV réussi !")
            } catch (e: Exception) {
                e.printStackTrace()
                _evenements.emit("Erreur lors de l'export : ${e.message}")
            }
        }
    }

    private fun escapeCsv(text: String): String {
        if (text.contains(";") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"${text.replace("\"", "\"\"")}\""
        }
        return text
    }

    fun modifierTitreListe(nouveauTitre: String) {
        viewModelScope.launch {
            parametreDao.enregistrer(ParametreEntity(cle = "titreListe", valeur = nouveauTitre))
        }
    }

    fun sauvegardeComplete(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext

                // 1. Construire le JSON
                val json = construireJsonSauvegarde()

                // 2. Écrire dans le fichier choisi
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.write(json.toString(2))  // indentation 2 espaces
                    }
                }

                _evenements.emit("Sauvegarde réussie")
            } catch (e: Exception) {
                e.printStackTrace()
                _evenements.emit("Erreur lors de la sauvegarde : ${e.message}")
            }
        }
    }

    fun lireFichierSauvegarde(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext

                // 1. Lire le contenu
                val contenuTexte = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: throw Exception("Impossible de lire le fichier")

                // 2. Parser et valider
                val contenu = parserSauvegarde(contenuTexte)

                // 3. Mettre à jour l'état pour déclencher l'affichage du dialog de confirmation
                _etatRestauration.value = EtatRestauration.FichierLu(contenu)

            } catch (e: SauvegardeInvalideException) {
                _etatRestauration.value = EtatRestauration.Erreur(e.message ?: "Fichier invalide")
            } catch (e: Exception) {
                e.printStackTrace()
                _etatRestauration.value = EtatRestauration.Erreur(
                    "Erreur de lecture : ${e.message}"
                )
            }
        }
    }

    private class SauvegardeInvalideException(message: String) : Exception(message)

    private fun parserSauvegarde(json: String): ContenuSauvegarde {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw SauvegardeInvalideException("Le fichier n'est pas un JSON valide")
        }

        // Vérifier formatVersion
        val formatVersion = root.optInt("formatVersion", -1)
        if (formatVersion == -1) {
            throw SauvegardeInvalideException("Champ formatVersion manquant")
        }
        if (formatVersion != 1 && formatVersion != 2) {
            throw SauvegardeInvalideException(
                "Version de format non supportée : $formatVersion (attendu : 1 ou 2)"
            )
        }

        // Champs racines requis
        val titreListe = root.optString("titreListe").takeIf { it.isNotBlank() }
            ?: throw SauvegardeInvalideException("Titre de liste manquant")

        val jsonCategories = root.optJSONArray("categories")
            ?: throw SauvegardeInvalideException("Liste de catégories manquante")

        val jsonTaches = root.optJSONArray("taches")
            ?: throw SauvegardeInvalideException("Liste de tâches manquante")

        val jsonParametres = root.optJSONArray("parametres") ?: JSONArray()

        // Parser catégories
        val categories = mutableListOf<CategorieEntity>()
        val nomsCategories = mutableSetOf<String>()
        for (i in 0 until jsonCategories.length()) {
            val obj = jsonCategories.getJSONObject(i)
            val nom = obj.optString("nom").takeIf { it.isNotBlank() }
                ?: throw SauvegardeInvalideException("Catégorie sans nom à l'index $i")
            if (!nomsCategories.add(nom)) {
                throw SauvegardeInvalideException("Catégorie en doublon : $nom")
            }
            categories.add(
                CategorieEntity(
                    nom = nom,
                    ordre = obj.optInt("ordre", 0),
                    couleur = obj.optLong("couleur", 0xFFFFFFFFL),
                    repliee = obj.optBoolean("repliee", false)
                )
            )
        }

        // Parser tâches
        val taches = mutableListOf<TacheEntity>()
        for (i in 0 until jsonTaches.length()) {
            val obj = jsonTaches.getJSONObject(i)
            val titre = obj.optString("titre").takeIf { it.isNotBlank() }
                ?: throw SauvegardeInvalideException("Tâche sans titre à l'index $i")
            val categorie = obj.optString("categorie").takeIf { it.isNotBlank() }
                ?: throw SauvegardeInvalideException("Tâche \"$titre\" sans catégorie")
            if (categorie !in nomsCategories) {
                throw SauvegardeInvalideException(
                    "Tâche \"$titre\" référence une catégorie inconnue : $categorie"
                )
            }
            val quantite = obj.optString("quantite", "")
            taches.add(
                TacheEntity(
                    id = 0,  // sera regénéré par Room
                    titre = titre,
                    categorie = categorie,
                    faite = obj.optBoolean("faite", false),
                    ordre = obj.optInt("ordre", 0),
                    quantite = quantite
                )
            )
        }

        // Parser paramètres
        val parametres = mutableListOf<ParametreEntity>()
        for (i in 0 until jsonParametres.length()) {
            val obj = jsonParametres.getJSONObject(i)
            val cle = obj.optString("cle").takeIf { it.isNotBlank() } ?: continue
            val valeur = obj.optString("valeur")
            parametres.add(ParametreEntity(cle = cle, valeur = valeur))
        }

        return ContenuSauvegarde(
            titreListe = titreListe,
            nbCategories = categories.size,
            nbTaches = taches.size,
            dateSauvegarde = root.optString("exportDate").takeIf { it.isNotBlank() },
            versionApp = root.optString("appVersion").takeIf { it.isNotBlank() },
            parametres = parametres,
            categories = categories,
            taches = taches
        )
    }

    fun confirmerRestauration() {
        val etat = _etatRestauration.value
        if (etat !is EtatRestauration.FichierLu) return

        viewModelScope.launch {
            try {
                val contenu = etat.contenu

                database.withTransaction {
                    // 1. Vider toutes les tables
                    tacheDao.supprimerToutes()
                    categorieDao.supprimerToutes()
                    parametreDao.supprimerTous()

                    // 2. Insérer les paramètres
                    contenu.parametres.forEach { parametreDao.enregistrer(it) }

                    // 3. Insérer les catégories
                    contenu.categories.forEach { categorieDao.inserer(it) }

                    // 4. Insérer les tâches
                    contenu.taches.forEach { tacheDao.inserer(it) }
                }

                // 5. Réinitialiser les états transitoires
                viderPileCochages()
                _actionAnnulable.value = null
                _termeRecherche.value = ""

                _etatRestauration.value = EtatRestauration.Inactif
                _evenements.emit("Restauration réussie")

            } catch (e: Exception) {
                e.printStackTrace()
                _etatRestauration.value = EtatRestauration.Erreur(
                    "Erreur lors de la restauration : ${e.message}"
                )
                _evenements.emit("Échec de la restauration")
            }
        }
    }

    fun annulerRestauration() {
        _etatRestauration.value = EtatRestauration.Inactif
    }

    fun consommerErreurRestauration() {
        if (_etatRestauration.value is EtatRestauration.Erreur) {
            _etatRestauration.value = EtatRestauration.Inactif
        }
    }

    private suspend fun construireJsonSauvegarde(): JSONObject {
        val state = uiState.value

        // Récupérer tous les paramètres présents dans la base
        val parametres = listOf("titreListe", "afficherFaites").mapNotNull { cle ->
            parametreDao.observer(cle).first()?.let { valeur ->
                JSONObject().apply {
                    put("cle", cle)
                    put("valeur", valeur)
                }
            }
        }

        // Construire le tableau des catégories
        val jsonCategories = JSONArray().apply {
            state.categories.forEach { cat ->
                put(JSONObject().apply {
                    put("nom", cat.nom)
                    put("ordre", cat.ordre)
                    put("couleur", cat.couleur)
                    put("repliee", cat.repliee)
                })
            }
        }

        // Construire le tableau des tâches
        val jsonTaches = JSONArray().apply {
            state.taches.forEach { tache ->
                put(JSONObject().apply {
                    put("titre", tache.titre)
                    put("categorie", tache.categorie)
                    put("faite", tache.faite)
                    put("ordre", tache.ordre)
                    put("quantite", tache.quantite)
                })
            }
        }

        // Construire le tableau des paramètres
        val jsonParametres = JSONArray().apply {
            parametres.forEach { put(it) }
        }

        // Assembler le tout
        return JSONObject().apply {
            put("formatVersion", FORMAT_VERSION_BACKUP)
            put("exportDate", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            put("appVersion", BuildConfig.VERSION_NAME)
            put("titreListe", state.titreListe)
            put("parametres", jsonParametres)
            put("categories", jsonCategories)
            put("taches", jsonTaches)
        }
    }

    fun toutCocher() {
        viewModelScope.launch {
            viderPileCochages()
            val tachesAChanger = uiState.value.taches.filter { !it.faite }
            if (tachesAChanger.isNotEmpty()) {
                val nouvellesTaches = uiState.value.taches.map { it.copy(faite = true) }
                tacheDao.mettreAJourToutes(nouvellesTaches)
                _actionAnnulable.value = ActionAnnulable.ToutCocher(tachesAChanger)
            }
        }
    }

    fun annulerDerniereAction() {
        viewModelScope.launch {
            when (val action = _actionAnnulable.value) {
                is ActionAnnulable.SuppressionTache -> {
                    tacheDao.inserer(action.tache)
                }
                is ActionAnnulable.SuppressionCategorie -> {
                    database.withTransaction {
                        categorieDao.inserer(action.categorie)
                        action.tachesAssociees.forEach { tacheDao.inserer(it) }
                    }
                }
                is ActionAnnulable.ToutCocher -> {
                    action.etatsPrecedents.forEach { tache ->
                        tacheDao.mettreAJour(tache.copy(faite = false))
                    }
                }
                null -> {}
            }
            _actionAnnulable.value = null
        }
    }

    fun consommerAction() {
        _actionAnnulable.value = null
    }
}
