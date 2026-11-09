# Epic : Sauvegarde complète au format JSON

| | |
|---|---|
| **Complexité** | Faible à Moyenne |
| **Type** | Nouvelle fonctionnalité |
| **Priorité** | Haute |
| **Estimation** | 3 à 5 heures |

---

## Objectif

Ajouter une fonctionnalité de **sauvegarde complète** permettant à l'utilisateur d'exporter l'intégralité de l'état de l'application dans un fichier JSON unique. Ce fichier contient toutes les données nécessaires pour reconstituer l'app à l'identique : nom de la liste, catégories (avec couleur, ordre, état replié), tâches (avec catégorie, état coché, ordre), paramètres.

Cette fonctionnalité s'ajoute aux exports existants (CSV via PartagerScreen) sans les remplacer. L'export CSV reste utile pour l'interopérabilité avec d'autres outils ; le JSON couvre le besoin de **sauvegarde complète et fidèle** des données.

## Contexte

L'application gère actuellement plusieurs types de données :
- `TacheEntity` : id, titre, catégorie, faite, ordre
- `CategorieEntity` : nom, ordre, couleur, repliée
- `ParametreEntity` : clé, valeur (notamment `titreListe` et `afficherFaites`)

L'export CSV existant ne capture que les couples (catégorie, titre tâche). Toutes les autres informations (couleurs, ordre, état coché, repliage, paramètres) sont perdues. Pour un utilisateur qui souhaite migrer vers un nouvel appareil, sauvegarder avant une mise à jour majeure, ou simplement archiver son état, il manque une option de **sauvegarde fidèle**.

Cette Epic couvre uniquement l'**export**. La restauration depuis un fichier JSON fera l'objet d'une Epic ultérieure.

## Parcours utilisateur

1. L'utilisateur ouvre le menu burger depuis la TopAppBar
2. Il clique sur la nouvelle entrée **"Sauvegarde complète"**
3. Le sélecteur de fichiers Android s'ouvre, lui permettant de choisir un emplacement (Downloads, Drive, USB...)
4. Le nom de fichier proposé par défaut est `shopList-Backup-AAMMJJ.json` (où `AAMMJJ` est la date du jour)
5. L'utilisateur valide ; le fichier est créé à l'emplacement choisi
6. Un Toast confirme : **"Sauvegarde réussie"**
7. En cas d'erreur, un Toast informe l'utilisateur : **"Erreur lors de la sauvegarde : [détail]"**

## Critères d'acceptation

- [ ] Une nouvelle entrée **"Sauvegarde complète"** est présente dans le menu burger de `TodoListTopBar`
- [ ] L'entrée se positionne **après "Importer une liste"** et **avant "Renommer la liste"** (ordre logique : import → backup → édition)
- [ ] Le clic ouvre le sélecteur de fichiers natif Android (`ActivityResultContracts.CreateDocument`)
- [ ] Le nom de fichier proposé par défaut respecte le format `shopList-Backup-AAMMJJ.json` (format de date court, à 6 chiffres)
- [ ] Le type MIME proposé est `application/json`
- [ ] Le fichier généré est un JSON **valide et lisible humainement** (indentation 2 espaces minimum)
- [ ] Le fichier contient **toutes les données** nécessaires pour reconstituer l'app à l'identique
- [ ] Le fichier inclut un champ `formatVersion` permettant l'évolution future du format
- [ ] Le fichier inclut un champ `exportDate` (timestamp ISO 8601) pour traçabilité
- [ ] L'opération est gérée dans le `TodoViewModel` (pas de logique I/O dans les composables)
- [ ] Un retour utilisateur (Toast via le SharedFlow `evenements`) confirme le succès ou signale l'erreur
- [ ] Les caractères spéciaux et accents sont correctement préservés (encodage UTF-8)

---

## Spécifications techniques

### Format du fichier JSON

Le fichier suit cette structure :

```json
{
  "formatVersion": 1,
  "exportDate": "2026-05-04T14:30:00",
  "appVersion": "2026.04",
  "titreListe": "Courses",
  "parametres": [
    { "cle": "titreListe", "valeur": "Courses" },
    { "cle": "afficherFaites", "valeur": "true" }
  ],
  "categories": [
    {
      "nom": "Fruits et légumes",
      "ordre": 0,
      "couleur": 4283215696,
      "repliee": false
    },
    {
      "nom": "Conserves",
      "ordre": 1,
      "couleur": 4290624957,
      "repliee": false
    }
  ],
  "taches": [
    {
      "titre": "Carottes",
      "categorie": "Fruits et légumes",
      "faite": false,
      "ordre": 0
    },
    {
      "titre": "Tomates",
      "categorie": "Fruits et légumes",
      "faite": true,
      "ordre": 1
    }
  ]
}
```

### Choix de design

**Pourquoi un wrapper avec métadonnées ?**

Les champs `formatVersion`, `exportDate` et `appVersion` ne servent pas pour cette Epic mais sont indispensables pour :
- La future fonctionnalité de **restauration** (qui devra savoir quelle version de format elle lit)
- Le diagnostic en cas de problème (savoir quand et avec quelle version a été créée la sauvegarde)
- L'évolution future du format (ajout de nouveaux champs)

**Pourquoi exclure les `id` des tâches ?**

Les identifiants auto-générés sont des détails techniques, non porteurs de sens fonctionnel. Lors d'une future restauration, les tâches recevront de nouveaux `id` (autoGenerate). Les exclure du JSON :
- Réduit la taille du fichier
- Évite des problèmes de conflit lors d'une éventuelle fusion
- Rend le fichier plus lisible humainement

**Pourquoi inclure `parametres` séparément alors que `titreListe` apparaît au niveau racine ?**

`titreListe` au niveau racine sert d'**information rapide** lisible humainement (pratique pour identifier la sauvegarde sans parser tout le JSON). La table `parametres` complète assure que **tous** les paramètres sont sauvegardés, y compris les futurs (préférences thème, etc. qui seraient ajoutés ultérieurement).

### Bibliothèque JSON

Utiliser **`org.json`** (intégrée à Android, aucune dépendance à ajouter) plutôt que de tirer Gson ou Moshi.

Avantages :
- Zéro impact sur le `build.gradle.kts`
- Suffisant pour ce besoin simple
- API stable et bien documentée
- Moins de risques de conflits de version

Si une évolution future nécessite une sérialisation plus complexe, le passage à `kotlinx.serialization` se fera dans une Epic dédiée.

### Modifications du ViewModel

#### Ajout de la méthode `sauvegardeComplete`

Dans `TodoViewModel.kt` :

```kotlin
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val FORMAT_VERSION_BACKUP = 1

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
```

> Note : `BuildConfig.VERSION_NAME` est généré automatiquement par Gradle. Vérifier que `buildFeatures { buildConfig = true }` est activé dans `app/build.gradle.kts`. Si ce n'est pas le cas, l'ajouter ou utiliser une constante manuelle.

### Modifications de la navigation

#### `ShopListNavHost.kt`

Propager le callback `sauvegardeComplete` du ViewModel à `TodoListScreen` :

```kotlin
composable(Routes.LISTE) {
    // ... existant
    TodoListScreen(
        // ... existant
        onSauvegardeComplete = viewModel::sauvegardeComplete,  // ← nouveau
    )
}
```

### Modifications de l'écran Liste

#### `TodoListScreen.kt`

Ajouter le paramètre, créer le launcher dédié, et propager à `TodoListTopBar` :

```kotlin
@Composable
fun TodoListScreen(
    // ... existant
    onSauvegardeComplete: (Uri) -> Unit,  // ← nouveau
) {
    // ... existant

    val launcherSauvegardeComplete = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { onSauvegardeComplete(it) }
    }

    // ... existant

    Scaffold(
        topBar = {
            TodoListTopBar(
                // ... existant
                onSauvegardeComplete = {
                    val nomFichier = "shopList-Backup-${
                        java.text.SimpleDateFormat("yyMMdd", java.util.Locale.getDefault())
                            .format(java.util.Date())
                    }.json"
                    launcherSauvegardeComplete.launch(nomFichier)
                }
            )
        },
        // ...
    )
}
```

> Note : on utilise `SimpleDateFormat` pour rester cohérent avec le code existant de `PartagerScreen` qui utilise `android.text.format.DateFormat`. Les deux fonctionnent. Pour aller plus loin, une harmonisation vers `java.time` serait souhaitable mais hors-scope.

### Modifications du TopBar

#### `TodoListTopBar.kt`

Ajouter le paramètre et l'entrée de menu :

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListTopBar(
    // ... existant
    onSauvegardeComplete: () -> Unit,  // ← nouveau
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
        // ── Nouvelle entrée ──
        DropdownMenuItem(
            text = { Text("Sauvegarde complète") },
            onClick = {
                afficherMenu = false
                onSauvegardeComplete()
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

### Test 1 : Sauvegarde nominale

**Procédure** :
1. Ouvrir l'app avec des données existantes (plusieurs catégories avec couleurs, tâches cochées et non cochées)
2. Ouvrir le menu burger
3. Cliquer sur "Sauvegarde complète"
4. Choisir l'emplacement (Downloads par exemple)
5. Valider
6. Ouvrir le fichier généré avec un éditeur de texte ou un visualiseur JSON

**Résultat attendu** :
- Toast "Sauvegarde réussie"
- Fichier créé avec le nom `shopList-Backup-AAMMJJ.json` (date du jour)
- Fichier valide JSON, lisible avec indentation
- Toutes les catégories présentes avec leurs couleurs (entiers Long)
- Toutes les tâches présentes avec leur état coché et leur ordre
- `formatVersion: 1` et `exportDate` présents

### Test 2 : Caractères spéciaux

**Procédure** :
1. Créer une catégorie avec des accents : "Épicerie & maraîchage"
2. Créer une tâche avec emoji et apostrophe : "L'œuf 🥚 du jour"
3. Effectuer une sauvegarde
4. Ouvrir le fichier

**Résultat attendu** : tous les caractères sont préservés correctement (UTF-8). Les emojis s'affichent. Les guillemets et caractères spéciaux JSON (`"`, `\`, etc.) sont correctement échappés par `org.json`.

### Test 3 : Liste vide

**Procédure** :
1. Supprimer toutes les catégories et tâches (ou utiliser une nouvelle installation)
2. Effectuer une sauvegarde

**Résultat attendu** :
- Sauvegarde réussit sans erreur
- Le JSON contient `"categories": []` et `"taches": []` (tableaux vides, pas null)
- Les métadonnées (`formatVersion`, `exportDate`, `appVersion`, `titreListe`) sont présentes

### Test 4 : Annulation par l'utilisateur

**Procédure** :
1. Cliquer sur "Sauvegarde complète"
2. Dans le sélecteur de fichiers, appuyer sur retour ou annuler

**Résultat attendu** :
- Aucun Toast affiché (ni succès ni erreur)
- L'app revient à l'état précédent sans erreur
- Aucun fichier n'est créé

> Note : ce comportement est garanti par le `uri?.let { ... }` dans le launcher, qui ignore le cas où l'utilisateur annule.

### Test 5 : Format du nom de fichier

**Procédure** :
1. Vérifier la date du jour (ex : 4 mai 2026)
2. Effectuer une sauvegarde

**Résultat attendu** : le nom proposé par défaut est `shopList-Backup-260504.json` (yy=26, MM=05, dd=04).

### Test 6 : Données volumineuses

**Procédure** :
1. Importer une grosse liste (200+ tâches) via l'import CSV existant
2. Effectuer une sauvegarde
3. Mesurer le temps et vérifier le fichier

**Résultat attendu** :
- L'opération doit se terminer en moins de 1 seconde sur appareil moderne
- Le fichier reste lisible (pas de corruption malgré la taille)
- Aucune fuite mémoire

### Test 7 : Préservation de l'ordre

**Procédure** :
1. Créer 5 catégories avec un ordre spécifique (A, B, C, D, E)
2. Réordonner manuellement (par drag-and-drop) en E, A, D, B, C
3. Effectuer une sauvegarde
4. Inspecter le JSON

**Résultat attendu** : les catégories apparaissent dans l'ordre des `ordre` croissants (donc l'ordre défini par l'utilisateur). Idem pour les tâches dans chaque catégorie.

---

## Points d'attention

### Encodage des couleurs

Les couleurs sont stockées en `Long` dans `CategorieEntity` (valeur ARGB sur 4 octets). Dans le JSON, elles apparaissent comme des entiers décimaux (ex: `4283215696`). C'est volontaire : préserve la valeur exacte sans risque de perte due à la conversion en hexadécimal.

Pour la future restauration, il suffira de relire `getLong("couleur")` et de l'assigner directement à la nouvelle `CategorieEntity`.

### Pas de notion de tâches "id" préservées

Comme indiqué dans la section "Choix de design", les `id` ne sont pas exportés. Conséquence : lors d'une future restauration, on ne pourra pas retrouver les "mêmes" tâches au sens technique. Mais comme rien dans l'app ne référence ces id en externe (pas de partage, pas de synchronisation cloud), c'est sans impact fonctionnel.

### Pas de chiffrement

Le fichier JSON est en clair. C'est acceptable car l'utilisateur choisit délibérément l'emplacement de sauvegarde (donc maîtrise la confidentialité). Aucun mot de passe ou donnée sensible n'est stocké dans l'app.

Si une évolution future devait embarquer des données sensibles, un chiffrement (AES-256 avec clé dérivée d'un mot de passe utilisateur) pourrait être ajouté. Hors-scope.

### Comportement sur Android < API 29

L'app cible déjà `minSdk 31`, donc aucun problème avec l'API `CreateDocument` (Storage Access Framework). Pas de gestion de permission `WRITE_EXTERNAL_STORAGE` nécessaire.

### Différence avec l'export CSV

| Aspect | Export CSV | Sauvegarde complète |
|---|---|---|
| **Couvre** | Tâches + nom de catégorie | Tout (paramètres, couleurs, ordre, état coché, repliage) |
| **Format** | Lisible humainement | Lisible mais structuré |
| **Interopérabilité** | Excel, autres apps | ShopList uniquement |
| **Usage type** | Partage rapide, analyse | Migration, archivage |

Les deux fonctions coexistent et répondent à des besoins différents.

### Buffer et fermeture des streams

L'utilisation de `.use { }` garantit la fermeture du stream même en cas d'exception. C'est crucial pour éviter les fuites de descripteurs de fichiers, surtout sur les appareils où l'utilisateur sauvegarde régulièrement.

### Cas d'erreur

Les erreurs courantes à gérer (déjà couvertes par le `try/catch`) :
- Espace de stockage insuffisant
- Permissions refusées par le SAF (rare, généralement résolu par le sélecteur)
- Fichier verrouillé par un autre process
- Problème transitoire de l'OS

Le message d'erreur transmis via `_evenements` inclut `e.message`, ce qui facilite le diagnostic. Pour une version plus polie, on pourrait mapper certains types d'exceptions vers des messages utilisateur plus clairs, mais c'est un raffinement (hors-scope).

---

## Hors-scope

Les éléments suivants pourraient faire l'objet d'Epics ultérieures :

- **Restauration** depuis un fichier JSON (pendant naturel de cette Epic, à prioriser ensuite)
- **Sauvegarde automatique** périodique (cf. Epic 7 du document de spécifications initial)
- **Chiffrement** du fichier de sauvegarde
- **Sauvegarde sélective** (seulement certaines catégories ou tâches)
- **Migration** entre formats `formatVersion` futurs

---

## Bénéfices attendus

- Permet à l'utilisateur de **changer d'appareil** sans perdre ses données
- Sécurise contre une suppression accidentelle de l'app ou de ses données
- Facilite le **diagnostic** en cas de problème (l'utilisateur peut envoyer son JSON au développeur)
- Prépare le terrain pour une future fonctionnalité de **synchronisation** entre appareils

---

## Références

- [Storage Access Framework - CreateDocument](https://developer.android.com/training/data-storage/shared/documents-files#create-file)
- [org.json package documentation](https://developer.android.com/reference/org/json/package-summary)
- [DateTimeFormatter ISO_LOCAL_DATE_TIME](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_LOCAL_DATE_TIME)
