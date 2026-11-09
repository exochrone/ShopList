# Epic : Restauration depuis sauvegarde JSON

| | |
|---|---|
| **Complexité** | Moyenne |
| **Type** | Nouvelle fonctionnalité |
| **Priorité** | Haute |
| **Estimation** | 5 à 7 heures |
| **Prérequis** | Epic "Sauvegarde complète" implémentée |

---

## Objectif

Ajouter une fonctionnalité de **restauration complète** permettant à l'utilisateur de réintégrer dans l'application un fichier JSON précédemment exporté via la fonction "Sauvegarde complète". L'opération remplace **intégralement** l'état actuel par celui du fichier de sauvegarde.

Cette fonctionnalité est le pendant naturel de la sauvegarde complète. Elle permet à l'utilisateur de :
- Migrer ses données vers un nouvel appareil
- Restaurer après une suppression accidentelle ou un bug
- Revenir à un état antérieur archivé

## Contexte

L'Epic précédente a mis en place la sauvegarde complète au format JSON, capturant nom de liste, paramètres, catégories (avec couleur, ordre, repliage) et tâches (avec catégorie, état coché, ordre).

La restauration doit être **fidèle** : après l'opération, l'app doit présenter exactement le même état que celui de la sauvegarde, comme si l'utilisateur n'avait jamais quitté ce point dans le temps.

L'opération est par nature **destructive** : elle écrase l'état actuel. Il est donc crucial de :
- Demander une **confirmation explicite** avant exécution
- Effectuer la restauration de manière **atomique** (tout ou rien)
- Donner un **retour clair** en cas de succès ou d'échec

## Parcours utilisateur

1. L'utilisateur ouvre le menu burger depuis la TopAppBar
2. Il clique sur la nouvelle entrée **"Restaurer une sauvegarde"**
3. Un **dialog d'avertissement** s'affiche : *"La restauration va remplacer toutes les données actuelles (catégories, tâches, paramètres). Cette action est irréversible. Continuer ?"* avec deux boutons : **Annuler** / **Choisir un fichier**
4. L'utilisateur clique sur **Choisir un fichier**
5. Le sélecteur de fichiers Android s'ouvre, filtré sur les fichiers JSON
6. L'utilisateur sélectionne son fichier de sauvegarde
7. L'app valide le fichier (parse, vérifie la structure)
8. Si le fichier est valide : un second dialog résume le contenu (*"Cette sauvegarde contient 12 catégories et 287 tâches, datée du 04/05/2026. Restaurer ?"*) avec **Annuler** / **Restaurer**
9. L'utilisateur confirme : la restauration s'exécute (transaction atomique)
10. Toast : **"Restauration réussie"**
11. L'app se rafraîchit automatiquement avec les données restaurées

En cas d'erreur (fichier corrompu, format invalide, etc.), un Toast explicite informe l'utilisateur, et **aucune modification** n'est appliquée.

## Critères d'acceptation

- [ ] Une nouvelle entrée **"Restaurer une sauvegarde"** est présente dans le menu burger
- [ ] L'entrée se positionne **après "Sauvegarde complète"** et **avant "Renommer la liste"**
- [ ] Un **premier dialog de confirmation** prévient du caractère destructif avant l'ouverture du sélecteur
- [ ] Le sélecteur de fichiers est filtré sur le type MIME `application/json`
- [ ] Le fichier est **validé** avant toute modification de la base : version, structure, champs requis
- [ ] Un **second dialog de confirmation** présente un résumé du contenu après lecture
- [ ] La restauration est **atomique** : utilisation de `withTransaction` Room
- [ ] En cas d'échec à n'importe quelle étape, **aucune donnée actuelle n'est altérée**
- [ ] Les Toast d'événements informent clairement de l'issue (succès/erreur avec détail)
- [ ] La restauration efface **toutes** les données existantes avant insertion (pas de fusion)
- [ ] La pile d'undo des cochages est vidée après restauration
- [ ] L'action annulable courante (Snackbar) est consommée après restauration

---

## Spécifications techniques

### Stratégie de restauration

**Choix : remplacement complet, pas de fusion.**

Justification :
- La sauvegarde représente un état cohérent à un instant donné
- La fusion introduirait des cas de bord complexes (que faire si une catégorie existe avec un nom identique mais une couleur différente ? Si deux tâches ont le même titre mais un ordre différent ?)
- Le besoin utilisateur est avant tout la **migration** ou la **récupération**, pas la consolidation

L'utilisateur qui voudrait fusionner deux listes devrait passer par un export CSV de l'une, puis import dans l'autre (l'import CSV existant gère déjà la fusion intelligemment).

### Validation du fichier

Avant toute modification, le fichier doit passer plusieurs étapes de validation :

#### Niveau 1 : parsing JSON

- Le fichier est-il un JSON valide ?
- L'objet racine est-il bien un objet JSON (pas un tableau ou primitive) ?

#### Niveau 2 : structure attendue

- Le champ `formatVersion` est-il présent ?
- Sa valeur est-elle supportée (`1` pour cette version) ?
- Les champs racines requis sont-ils présents : `titreListe`, `categories`, `taches`, `parametres` ?

#### Niveau 3 : cohérence des données

- Les catégories ont-elles toutes les champs requis : `nom`, `ordre`, `couleur`, `repliee` ?
- Les tâches ont-elles toutes les champs requis : `titre`, `categorie`, `faite`, `ordre` ?
- Chaque tâche référence-t-elle une catégorie qui existe dans le tableau `categories` ?
- Les noms de catégories sont-ils uniques dans le tableau ?

Si une étape échoue, l'opération est interrompue avec un message d'erreur explicite.

### Modélisation

Ajouter dans `TodoViewModel.kt` une data class pour représenter le résultat de la lecture du fichier :

```kotlin
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
```

### Modifications du ViewModel

#### Ajout de l'état

```kotlin
class TodoViewModel(application: Application) : AndroidViewModel(application) {
    // ... existant

    private val _etatRestauration = MutableStateFlow<EtatRestauration>(EtatRestauration.Inactif)
    val etatRestauration: StateFlow<EtatRestauration> = _etatRestauration.asStateFlow()

    // ... existant
}
```

#### Lecture et validation du fichier

```kotlin
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
    if (formatVersion != FORMAT_VERSION_BACKUP) {
        throw SauvegardeInvalideException(
            "Version de format non supportée : $formatVersion (attendu : $FORMAT_VERSION_BACKUP)"
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
        taches.add(
            TacheEntity(
                id = 0,  // sera regénéré par Room
                titre = titre,
                categorie = categorie,
                faite = obj.optBoolean("faite", false),
                ordre = obj.optInt("ordre", 0)
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
```

#### Restauration effective

```kotlin
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
```

### Ajouts dans les DAOs

Ajouter les méthodes de suppression complète dans `Daos.kt` :

```kotlin
@Dao
interface TacheDao {
    // ... existant
    
    @Query("DELETE FROM taches")
    suspend fun supprimerToutes()
}

@Dao
interface CategorieDao {
    // ... existant
    
    @Query("DELETE FROM categories")
    suspend fun supprimerToutes()
}

@Dao
interface ParametreDao {
    // ... existant
    
    @Query("DELETE FROM parametres")
    suspend fun supprimerTous()
}
```

### Modifications de la navigation

#### `MainActivity.kt`

Collecter `etatRestauration` :

```kotlin
@Composable
fun TodoApp() {
    // ... existant
    val etatRestauration by viewModel.etatRestauration.collectAsStateWithLifecycle()
    // ... existant

    ShopListNavHost(
        // ... existant
        etatRestauration = etatRestauration
    )
}
```

#### `ShopListNavHost.kt`

Propager au `TodoListScreen` :

```kotlin
composable(Routes.LISTE) {
    // ... existant
    TodoListScreen(
        // ... existant
        etatRestauration = etatRestauration,
        onLireFichierSauvegarde = viewModel::lireFichierSauvegarde,
        onConfirmerRestauration = viewModel::confirmerRestauration,
        onAnnulerRestauration = viewModel::annulerRestauration,
        onConsommerErreurRestauration = viewModel::consommerErreurRestauration
    )
}
```

### Modifications de l'écran Liste

#### `TodoListScreen.kt`

Ajouter les paramètres, le launcher, et les dialogs :

```kotlin
@Composable
fun TodoListScreen(
    // ... existant
    etatRestauration: EtatRestauration,
    onLireFichierSauvegarde: (Uri) -> Unit,
    onConfirmerRestauration: () -> Unit,
    onAnnulerRestauration: () -> Unit,
    onConsommerErreurRestauration: () -> Unit
) {
    // ... existant

    var afficherDialogPreRestauration by remember { mutableStateOf(false) }

    val launcherRestauration = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onLireFichierSauvegarde(it) }
    }

    // ... existant

    Scaffold(
        topBar = {
            TodoListTopBar(
                // ... existant
                onRestaurerSauvegarde = { afficherDialogPreRestauration = true }
            )
        },
        // ...
    )

    // Dialog 1 : avertissement avant ouverture du sélecteur
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

    // Dialog 2 : confirmation après lecture du fichier
    val etatFichierLu = etatRestauration as? EtatRestauration.FichierLu
    if (etatFichierLu != null) {
        AlertDialog(
            onDismissRequest = onAnnulerRestauration,
            title = { Text("Confirmer la restauration") },
            text = {
                val contenu = etatFichierLu.contenu
                val dateLisible = contenu.dateSauvegarde
                    ?.substringBefore("T")  // garde juste la date
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

    // Dialog 3 : message d'erreur si la lecture a échoué
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
```

### Modifications du TopBar

#### `TodoListTopBar.kt`

Ajouter le paramètre et l'entrée de menu :

```kotlin
@Composable
fun TodoListTopBar(
    // ... existant
    onRestaurerSauvegarde: () -> Unit
) {
    // ...
    DropdownMenu(...) {
        DropdownMenuItem(
            text = { Text("Gérer les catégories") },
            onClick = { afficherMenu = false; onOuvrirCategories() }
        )
        DropdownMenuItem(
            text = { Text("Partager la liste") },
            onClick = { afficherMenu = false; onOuvrirPartage() }
        )
        DropdownMenuItem(
            text = { Text("Importer une liste") },
            onClick = { afficherMenu = false; onOuvrirImport() }
        )
        DropdownMenuItem(
            text = { Text("Sauvegarde complète") },
            onClick = { afficherMenu = false; onSauvegardeComplete() }
        )
        // ── Nouvelle entrée ──
        DropdownMenuItem(
            text = { Text("Restaurer une sauvegarde") },
            onClick = {
                afficherMenu = false
                onRestaurerSauvegarde()
            }
        )
        // ─────────────────────
        DropdownMenuItem(
            text = { Text("Renommer la liste") },
            onClick = { afficherMenu = false; onRenommerListe() }
        )
        DropdownMenuItem(
            text = { Text("Tout cocher") },
            onClick = { afficherMenu = false; onToutCocher() }
        )
    }
}
```

---

## Tests à effectuer

### Test 1 : Restauration nominale

**Procédure** :
1. Créer plusieurs catégories avec des couleurs spécifiques
2. Créer une dizaine de tâches dans différentes catégories, en cocher quelques-unes
3. Effectuer une "Sauvegarde complète" vers Downloads
4. Modifier l'état de l'app : ajouter des tâches, supprimer une catégorie, changer le titre de la liste
5. Lancer "Restaurer une sauvegarde", choisir le fichier précédemment créé
6. Confirmer les deux dialogs

**Résultat attendu** :
- Le second dialog affiche correctement le nombre de catégories, tâches, et la date
- Après confirmation : Toast "Restauration réussie"
- Les catégories et tâches d'origine sont restaurées avec leur couleur, ordre, état coché
- Le titre de liste est restauré
- Les modifications faites entre la sauvegarde et la restauration sont perdues (comportement attendu)

### Test 2 : Annulation aux différentes étapes

**Procédure** :
1. Cliquer sur "Restaurer une sauvegarde"
2. Sur le premier dialog, cliquer "Annuler" → vérifier que rien ne se passe
3. Recommencer, cliquer "Choisir un fichier"
4. Dans le sélecteur, faire retour Android → vérifier que rien ne se passe
5. Recommencer, sélectionner un fichier valide
6. Sur le second dialog (résumé), cliquer "Annuler" → vérifier que les données actuelles sont intactes

**Résultat attendu** : à chaque annulation, l'app revient à l'état précédent sans aucune modification de données.

### Test 3 : Fichier invalide

**Procédure** :
1. Créer un fichier `.json` contenant du texte aléatoire (ex: "ceci n'est pas du JSON")
2. Sauvegarder dans Downloads
3. Tenter de restaurer ce fichier

**Résultat attendu** :
- Dialog d'erreur : "Le fichier n'est pas un JSON valide"
- Données actuelles intactes
- Possibilité de réessayer

### Test 4 : Fichier JSON valide mais structure incorrecte

**Procédure** :
1. Créer un fichier JSON `{"hello": "world"}`
2. Tenter la restauration

**Résultat attendu** :
- Dialog d'erreur : "Champ formatVersion manquant" (ou message équivalent)
- Données actuelles intactes

### Test 5 : Version non supportée

**Procédure** :
1. Modifier manuellement un fichier de sauvegarde valide pour mettre `"formatVersion": 99`
2. Tenter la restauration

**Résultat attendu** :
- Dialog d'erreur : "Version de format non supportée : 99 (attendu : 1)"
- Données actuelles intactes

### Test 6 : Tâche orpheline (référence catégorie inexistante)

**Procédure** :
1. Modifier manuellement un fichier valide en supprimant une catégorie référencée par des tâches
2. Tenter la restauration

**Résultat attendu** :
- Dialog d'erreur : 'Tâche "X" référence une catégorie inconnue : Y'
- Données actuelles intactes

### Test 7 : Catégories en doublon

**Procédure** :
1. Modifier manuellement un fichier pour avoir deux catégories avec le même nom
2. Tenter la restauration

**Résultat attendu** :
- Dialog d'erreur : "Catégorie en doublon : ..."
- Données actuelles intactes

### Test 8 : Atomicité

**Procédure** :
1. Créer un fichier de sauvegarde **volumineux** (200+ tâches)
2. Lancer la restauration
3. **Pendant l'opération**, simuler un crash si possible (mode développeur, ou simplement un fichier qui passerait la validation puis poserait problème à l'insertion)

**Résultat attendu** : si quelque chose échoue à l'insertion (cas très rare grâce à la validation préalable), grâce à `withTransaction` la base reste dans son état original. Aucun état intermédiaire (mi-ancien, mi-nouveau) ne doit subsister.

### Test 9 : Volumes importants

**Procédure** :
1. Importer un fichier avec 500+ tâches via la sauvegarde
2. Mesurer le temps de restauration

**Résultat attendu** :
- Opération inférieure à 3 secondes sur appareil moderne
- Pas de freeze visible de l'UI (l'opération se fait en `viewModelScope`)
- Toast de succès affiché à la fin

### Test 10 : États transitoires réinitialisés

**Procédure** :
1. Avant restauration : cocher 5 tâches (la pile d'undo contient 5 actions)
2. Avant restauration : taper du texte dans la barre de recherche
3. Effectuer une restauration

**Résultat attendu** :
- Après restauration : l'icône undo est grisée (pile vidée)
- Le mode recherche est désactivé (terme vidé)
- Aucun Snackbar résiduel

### Test 11 : Restauration sur app vide

**Procédure** :
1. Désinstaller et réinstaller l'app (état initial : catégorie "Personnel" avec 3 tâches d'exemple)
2. Lancer la restauration avec un fichier valide

**Résultat attendu** :
- Restauration réussie
- L'état initial (catégorie d'exemple) est entièrement remplacé par le contenu du fichier

---

## Points d'attention

### Atomicité de la restauration

La séquence DELETE + INSERT doit impérativement se faire dans une `withTransaction`. Sans cela, en cas d'interruption (crash, kill par l'OS, batterie morte), l'utilisateur se retrouverait avec une base partiellement vidée et des données mélangées entre l'ancien et le nouveau.

`withTransaction` garantit le tout ou rien : soit la transaction commit entièrement, soit elle rollback à l'état initial.

### Pourquoi un sealed class `EtatRestauration` plutôt que plusieurs Flow ?

Le processus de restauration a plusieurs phases (lecture, validation, confirmation, exécution) avec des données différentes selon la phase. Un `sealed class` exprime clairement ces transitions :

- `Inactif` : état repos
- `FichierLu` : un fichier a été lu et validé, en attente de confirmation utilisateur
- `Erreur` : la lecture ou validation a échoué

C'est plus expressif et plus sûr qu'un ensemble de booléens et nullables séparés.

### Tâches sans `id` à l'insertion

Dans le JSON, les `id` ne sont pas exportés (cf. Epic Sauvegarde complète). À la restauration, on insère les `TacheEntity` avec `id = 0`, ce qui déclenche l'auto-génération de Room. Les nouveaux ids n'ont aucune importance fonctionnelle.

### Validation stricte vs permissive

Le parsing applique une validation **stricte** : tout fichier mal formé est rejeté avec un message d'erreur précis. Alternative envisageable : tolérer certaines erreurs (ex: ignorer une tâche sans catégorie au lieu d'échouer).

Choix retenu : **strict**. Raisons :
- Une sauvegarde corrompue est probablement le symptôme d'un problème plus large (le fichier ne vient pas vraiment de l'app)
- Mieux vaut un échec explicite qu'une restauration "presque correcte" qui surprendrait l'utilisateur
- La validation stricte facilite le diagnostic

### Conservation de la version actuelle en cas d'échec

En cas d'erreur après le clic "Restaurer" final (cas extrêmement rare grâce à la validation préalable), l'utilisateur conserve ses données actuelles intactes. C'est la garantie de `withTransaction`.

L'état `EtatRestauration.Erreur` est défini pour communiquer ce cas, même si en pratique on ne devrait jamais y arriver après validation.

### Compatibilité ascendante

Le champ `formatVersion = 1` permet la future évolution. Si une v2 du format est introduite (par exemple pour ajouter un champ "tags" aux tâches), la restauration devra :
- Soit refuser les v1 (force l'utilisateur à migrer manuellement)
- Soit appliquer une logique de migration v1 → v2 lors de la lecture

À ce stade, la stratégie est simple : seule la v1 est supportée. Le jour où une v2 apparaîtra, cette Epic devra être étendue.

### Pas de prévisualisation des données

Le second dialog présente un **résumé numérique** (nombres et date), pas le contenu détaillé. Afficher la liste complète serait peu utile (l'utilisateur a déjà vu ces données quand il a fait la sauvegarde) et alourdirait l'UI.

Si un besoin émerge plus tard (ex: aperçu des 5 premières catégories), c'est facile à ajouter sans toucher au reste.

### Chemin du fichier non préservé

L'API `OpenDocument` ne donne pas accès au chemin réel du fichier. C'est par design (sécurité Android). On ne peut donc pas afficher "Restaurer depuis : /Downloads/shopList-Backup-260504.json", juste un Uri opaque. Pas un problème puisque l'utilisateur sait quel fichier il vient de sélectionner.

---

## Hors-scope

Les éléments suivants pourraient faire l'objet d'Epics ultérieures :

- **Fusion** entre la sauvegarde et l'état actuel (au lieu du remplacement)
- **Restauration sélective** (seulement certaines catégories ou tâches)
- **Migration automatique** entre formats `formatVersion` futurs
- **Aperçu détaillé** du contenu avant restauration
- **Sauvegarde automatique avant restauration** (snapshot de l'état actuel pour annulation)

---

## Bénéfices attendus

- **Migration entre appareils** facilitée et fiable
- **Récupération** après suppression accidentelle des données
- **Confiance utilisateur** : les actions destructives sont réversibles via une sauvegarde préalable
- **Support technique** : permet de partager un état complet pour diagnostic

---

## Références

- [Storage Access Framework - OpenDocument](https://developer.android.com/training/data-storage/shared/documents-files#open-file)
- [Room transactions with withTransaction](https://developer.android.com/reference/kotlin/androidx/room/RoomDatabase#withTransaction(kotlin.coroutines.SuspendFunction0))
- [Sealed classes Kotlin](https://kotlinlang.org/docs/sealed-classes.html)
