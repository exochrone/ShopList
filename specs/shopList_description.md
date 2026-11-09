# shopList : description fonctionnelle et technique

Document de référence décrivant l'application shopList telle qu'elle existe dans le dépôt `exochrone/shopList` (version `2026.04`, versionCode 2). Il sert de contexte pour concevoir et spécifier les évolutions.

---

## 1. Présentation

shopList est une application Android personnelle de **liste de courses persistante**, organisée par catégories.

Son principe central : **le catalogue des produits reste en permanence dans l'application.** On ne recrée pas une liste à chaque course : on coche et décoche les produits existants selon les besoins. Cela évite de ressaisir les mêmes articles d'une semaine à l'autre.

* Un produit **non coché** est à acheter.
* Un produit **coché** est acheté ou pas nécessaire pour le moment. Il reste dans la liste, grisé et barré, et peut être masqué.

L'application gère une seule liste (titre par défaut « Courses », renommable). Elle est utilisée au quotidien par son auteur sur un Samsung Galaxy S23+.

Vocabulaire : l'application est née comme une todo list. Dans le code et dans une partie de l'interface, un produit s'appelle une **tâche** (`TacheEntity`, « Nouvelle tâche », « Tâche supprimée »…). Dans ce document, « produit » et « tâche » désignent la même chose.

---

## 2. Description fonctionnelle

### 2.1 Écran principal : la liste

**Barre du haut**, de gauche à droite :

* Titre de la liste.
* **Annuler le dernier cochage** (icône flèche retour) : actif seulement s'il existe des cochages à annuler.
* **Rechercher** (loupe).
* **Afficher / masquer les produits cochés** (icône œil). Le réglage est mémorisé.
* **Menu** (icône hamburger) : Gérer les catégories, Partager la liste, Importer une liste, Sauvegarde complète, Restaurer une sauvegarde, Renommer la liste, Tout cocher.

**Contenu** : les catégories dans l'ordre défini par l'utilisateur, chacune suivie de ses produits.

* **En-tête de catégorie** : chevron (replié / déplié), nom de la catégorie dans sa couleur, nombre de produits non cochés entre parenthèses, bouton « + » pour ajouter un produit dans cette catégorie.
  * Toucher l'en-tête replie ou déplie la catégorie (état mémorisé en base).
* **Ligne produit** : case à cocher (de la couleur de la catégorie), puis le nom.
  * Produit coché : case et texte grisés, texte barré.
  * Toucher la case : coche ou décoche.
  * Toucher le nom : ouvre le dialogue « Modifier la tâche ».
  * Appui long puis glisser : déplace le produit, y compris vers une autre catégorie (glisser-déposer).
* Un séparateur horizontal termine la liste.

**Affichage des catégories** :

* Produits cochés affichés : toutes les catégories sont affichées, même vides.
* Produits cochés masqués : seules les catégories ayant au moins un produit non coché sont affichées.

### 2.2 Ajouter un produit

Bouton « + » d'une catégorie, puis dialogue « Nouvelle tâche dans « catégorie » » : un champ (placeholder « Nom de la tâche »), boutons Annuler et Ajouter.

* Le nom doit être unique dans **toute** la liste (comparaison sans tenir compte de la casse ni des espaces en début et fin). Sinon, message : « Cet élément existe déjà dans la catégorie X » et le bouton est désactivé.
* Le produit est ajouté non coché, en fin de catégorie.

### 2.3 Modifier ou supprimer un produit

Toucher le nom d'un produit ouvre « Modifier la tâche » : champ prérempli avec le nom, boutons Annuler, Supprimer (rouge) et Valider.

* Même règle d'unicité du nom que pour l'ajout (en excluant le produit lui-même).
* **Supprimer** : suppression immédiate, puis snackbar « Tâche supprimée » avec action **Annuler** qui restaure le produit.

### 2.4 Cocher, décocher, annuler

* Chaque cochage ou décochage est empilé dans une **pile d'annulation** (10 actions maximum). Le bouton « Annuler le dernier cochage » de la barre du haut dépile une action à la fois.
* La pile est vidée par les opérations qui modifient la structure (suppression de produit ou de catégorie, import CSV, tout cocher, restauration).
* **Tout cocher** (menu) : coche tous les produits, réaffiche les produits cochés, puis snackbar « N tâche(s) cochée(s) » avec action **Annuler** qui décoche les produits concernés.

### 2.5 Recherche

* La loupe remplace le titre par un champ de recherche (focus automatique).
* Filtre en direct, **sans tenir compte des accents ni de la casse**, sur le nom du produit **ou** le nom de sa catégorie.
* En recherche, les produits cochés sont toujours inclus, et le glisser-déposer est désactivé.
* Croix : efface le texte ; si le texte est déjà vide, quitte la recherche. Le Retour système quitte aussi la recherche.

### 2.6 Gérer les catégories

Écran « Gestion des catégories » (menu), avec flèche retour et bouton « + » dans la barre du haut.

Chaque catégorie affiche : poignée de déplacement, nom (dans sa couleur), nombre de produits (« Vide » ou « N tâche(s) »), puis trois boutons :

* **Trier A→Z** : trie alphabétiquement les produits de la catégorie.
* **Palette** : choix de la couleur dans une palette fixe de 20 couleurs.
* **Supprimer** : suppression directe si la catégorie est vide ; sinon, confirmation « N tâche(s) dans la catégorie X seront supprimées avec la catégorie ». Snackbar avec **Annuler** qui restaure la catégorie et ses produits.

Autres actions :

* Glisser-déposer pour réordonner les catégories.
* Toucher le nom : dialogue « Modifier la catégorie » (renommer, ou supprimer). Le nom de catégorie est unique (sans tenir compte de la casse). Le renommage met à jour tous les produits de la catégorie.
* « + » : nouvelle catégorie, ajoutée en fin de liste, blanche par défaut.

### 2.7 Partager la liste

Écran « Partager la liste » (menu) :

* Aperçu du texte à partager, modifiable à la main. Après une modification manuelle, un bouton permet de revenir au texte automatique.
* Options : **Masquer les catégories** et **Afficher les éléments cochés** (par défaut, seuls les produits non cochés sont partagés).
* Format : nom de la catégorie, puis ses produits indentés de 4 espaces (sans indentation si les catégories sont masquées). Les catégories sans produit partagé sont omises.
* **Partager** : ouvre le sélecteur de partage Android (texte brut).
* **Export CSV** : voir 2.8.

### 2.8 Import et export CSV

**Export** (depuis l'écran Partager) : fichier `shopList-<titre>-<date>.csv`, UTF-8 avec BOM, séparateur `;`, sans en-tête.

* Une ligne `categorie;produit` par produit, cochés compris.
* Une ligne `categorie;` pour chaque catégorie vide.
* Les valeurs contenant `;`, `"` ou un retour à la ligne sont placées entre guillemets.

**Import** (menu « Importer une liste ») : après un message d'explication, choix d'un fichier CSV.

* Format attendu : `categorie;produit`, UTF-8, sans en-tête, les deux colonnes obligatoires.
* Les catégories inconnues sont créées en fin de liste.
* Un produit n'est créé que s'il n'existe pas déjà un produit de **même nom et même catégorie** (comparaison exacte). Les produits existants ne sont pas modifiés (leur état coché reste inchangé).
* L'import est fait dans une transaction ; un message confirme la réussite ou l'erreur.

### 2.9 Sauvegarde complète et restauration

**Sauvegarde complète** (menu) : fichier JSON `shopList-Backup-<aaMMjj>.json` contenant le titre de la liste, les paramètres, toutes les catégories (nom, ordre, couleur, repliée) et tous les produits (titre, catégorie, cochée, ordre).

**Restaurer une sauvegarde** (menu), en trois temps :

1. Avertissement : la restauration remplacera toutes les données, action irréversible.
2. Après choix du fichier, résumé : titre de la liste, nombre de catégories et de produits, date de la sauvegarde. Confirmation par « Restaurer ».
3. Remplacement complet des données dans une transaction. En cas de fichier invalide (JSON incorrect, version non supportée, champ manquant, catégorie en doublon, produit rattaché à une catégorie inconnue), un dialogue « Restauration impossible » affiche la raison.

### 2.10 Divers

* **Renommer la liste** (menu) : change le titre affiché.
* **Premier lancement** (base vide) : création d'une catégorie « Personnel » avec trois tâches d'exemple (héritage de la todo list d'origine).
* Les messages de résultat (import, export, sauvegarde, restauration) s'affichent en Toast ; les actions annulables en snackbar.
* Application en **portrait uniquement**, **thème sombre unique**.

---

## 3. Description technique

### 3.1 Environnement

| Élément | Valeur |
|---|---|
| Langage | Kotlin 2.0.21 |
| UI | Jetpack Compose, BOM 2024.12.01, Material 3, icônes étendues |
| Build | AGP 8.7.3, Gradle 8.10.2, KSP 2.0.21-1.0.28, catalogue `gradle/libs.versions.toml` |
| SDK | compileSdk 35, targetSdk 35, minSdk 31, Java 11 |
| Persistance | Room 2.6.1 (KSP) |
| Navigation | Navigation Compose 2.8.4 |
| Glisser-déposer | `sh.calvin.reorderable:reorderable` 2.4.0 |
| JSON | `org.json` (inclus dans Android) |
| Package | `com.jb.shopList` |
| Release | minification activée (R8, ProGuard par défaut) |

Points de configuration Gradle à ne pas casser (leçons des premières versions) :

* Le plugin `kotlin-compose` est utilisé ; le build racine déclare tous les plugins avec `apply false`.
* `android.useAndroidX=true` est obligatoire dans `gradle.properties`.
* Les BOM Compose 2026.x ne sont pas compatibles avec Kotlin 2.0.21 ; AGP 9.x n'est pas compatible avec la configuration actuelle.

### 3.2 Architecture

Architecture **MVVM simple, une seule activité**, sans couche Repository ni injection de dépendances :

```
MainActivity (shopListTheme + TodoApp)
 └─ TodoApp : Scaffold + SnackbarHost, collecte des StateFlow et des événements
     └─ shopListNavHost (3 routes)
         ├─ TodoListScreen      (route "liste", départ)
         ├─ CategoriesScreen    (route "categories")
         └─ PartagerScreen      (route "partager")

TodoViewModel (AndroidViewModel, unique, partagé par les 3 écrans)
 └─ TacheDao / CategorieDao / ParametreDao
     └─ TodoDatabase (Room, singleton)
```

* Les écrans sont **sans état métier** : ils reçoivent l'état (`TodoUiState` et autres flux) et des callbacks vers le ViewModel. Seul l'état d'interface local (dialogues ouverts, mode recherche, saisies) vit dans les composables.
* Le ViewModel lit et écrit directement via les DAO, dans `viewModelScope`. Les opérations multi-tables utilisent `database.withTransaction { }`.

### 3.3 Arborescence des sources

```
app/src/main/java/com/jb/shopList/
├── MainActivity.kt            Activité, composable racine TodoApp (Toast, snackbar)
├── Entities.kt                Entités Room
├── Daos.kt                    DAO Room
├── TodoDatabase.kt            Base Room (singleton)
├── TodoViewModel.kt           ViewModel unique, états, sauvegarde, CSV
└── ui/
    ├── navigation/
    │   ├── Routes.kt          Constantes de routes
    │   └── shopListNavHost.kt NavHost et câblage écrans ↔ ViewModel
    ├── screens/
    │   ├── liste/
    │   │   ├── TodoListScreen.kt   Écran principal et ses dialogues
    │   │   ├── TodoListTopBar.kt   Barre du haut, recherche, menu
    │   │   └── LigneAffichee.kt    Modèle des lignes affichées (en-tête / produit)
    │   ├── categories/
    │   │   └── CategoriesScreen.kt Gestion des catégories
    │   └── partage/
    │       └── PartagerScreen.kt   Partage texte et export CSV
    ├── components/
    │   ├── TacheItem.kt            Ligne produit
    │   ├── SaisieDialog.kt         Dialogue générique de saisie d'un texte
    │   └── ColorPickerDialog.kt    Choix de couleur
    └── theme/
        ├── Color.kt, Type.kt, Theme.kt   Thème sombre et couleurs personnalisées
        └── PaletteCategories.kt          Palette des 20 couleurs de catégorie
```

### 3.4 Modèle de données

Base Room `todo_database`, **version 3**, `exportSchema = true` (dossier `app/schemas`, non versionné dans le dépôt).

```kotlin
@Entity(tableName = "taches")
data class TacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titre: String,
    val categorie: String,   // nom de la catégorie (pas de clé étrangère)
    val faite: Boolean = false,
    val ordre: Int = 0
)

@Entity(tableName = "categories")
data class CategorieEntity(
    @PrimaryKey val nom: String,
    val ordre: Int = 0,
    val couleur: Long = 0xFFFFFFFF,  // ARGB
    val repliee: Boolean = false
)

@Entity(tableName = "parametres")
data class ParametreEntity(
    @PrimaryKey val cle: String,
    val valeur: String
)
```

* Le lien produit → catégorie se fait **par le nom** de la catégorie, sans clé étrangère. Renommer une catégorie = insérer la nouvelle, mettre à jour les produits, supprimer l'ancienne (en transaction).
* `ordre` des produits : un ordre **global** (`ORDER BY ordre` sur toute la table) ; l'affichage regroupe ensuite par catégorie.
* Paramètres utilisés : `titreListe`, `afficherFaites`.

**DAO principaux** :

* `TacheDao` : `observerToutes(): Flow`, `inserer` (REPLACE), `mettreAJour`, `mettreAJourToutes`, `supprimer`, `renommerCategorie`, `compterParCategorie`, `trouverParTitreEtCategorie`, `obtenirOrdreMax`, `supprimerParCategorie`, `supprimerToutes`.
* `CategorieDao` : `observerToutes(): Flow`, `inserer` (IGNORE), `mettreAJour`, `mettreAJourToutes`, `supprimer`, `supprimerToutes`.
* `ParametreDao` : `observer(cle): Flow<String?>`, `enregistrer` (REPLACE), `supprimerTous`.

### 3.5 ViewModel et états

`TodoViewModel` expose :

* `uiState: StateFlow<TodoUiState>` : combinaison des flux Room (produits, catégories, `titreListe`, `afficherFaites`) et du terme de recherche, via `combine` + `stateIn(WhileSubscribed(5000))`.

```kotlin
data class TodoUiState(
    val taches: List<TacheEntity> = emptyList(),
    val categories: List<CategorieEntity> = emptyList(),
    val titreListe: String = "Courses",
    val afficherFaites: Boolean = true,
    val termeRecherche: String = ""
)
```

* `actionAnnulable: StateFlow<ActionAnnulable?>` : dernière action annulable par snackbar (`SuppressionTache`, `SuppressionCategorie`, `ToutCocher`), chacune avec son message.
* `pileCochages: StateFlow<List<ActionCochage>>` : pile d'annulation des cochages (`tacheId`, `etatPrecedent`), limitée à 10.
* `etatRestauration: StateFlow<EtatRestauration>` : `Inactif`, `FichierLu(ContenuSauvegarde)`, `Erreur(message)`.
* `evenements: SharedFlow<String>` : messages ponctuels affichés en Toast par `TodoApp`.

Principales fonctions publiques : `ajouterTache`, `basculerFait`, `annulerDernierCochage`, `modifierTitreTache`, `supprimerTache`, `reordonnerTaches`, `trierTachesParAlpha`, `toutCocher`, `ajouterCategorie`, `supprimerCategorie`, `reordonnerCategories`, `renommerCategorie`, `modifierCouleurCategorie`, `toggleCategorieRepliee`, `modifierTitreListe`, `setAfficherFaites`, `setTermeRecherche`, `importerDepuisCsv`, `exporterCsv`, `sauvegardeComplete`, `lireFichierSauvegarde`, `confirmerRestauration`, `annulerRestauration`, `consommerErreurRestauration`, `annulerDerniereAction`, `consommerAction`.

### 3.6 Interface

* **Construction des lignes** : `TodoListScreen` calcule une `List<LigneAffichee>` (`Header` ou `ItemTache`, avec clé unique stable) à partir de l'état, en appliquant le filtre des cochés, la recherche et le repli des catégories. Une seule `LazyColumn` affiche ces lignes.
* **Glisser-déposer** : bibliothèque Reorderable (`rememberReorderableLazyListState`, `ReorderableItem`, `longPressDraggableHandle`). Déposer un produit sur un en-tête ou un produit d'une autre catégorie change sa catégorie ; la liste complète est réordonnée puis enregistrée.
* **Dialogues** : `SaisieDialog` est le dialogue générique (titre, champ unique, placeholder, validation avec message d'erreur, actions supplémentaires comme Supprimer). Les confirmations utilisent `AlertDialog`.
* **Fichiers** : Storage Access Framework via `rememberLauncherForActivityResult` (`OpenDocument`, `CreateDocument`), aucune permission de stockage.
* **Thème** : `shopListTheme`, thème sombre unique (fond `#2A3135`, texte blanc). Couleurs personnalisées exposées par `CompositionLocal` : `shopListTheme.customColors.tacheNonFaite` et `.tacheFaite` (gris `#787D7F`).

### 3.7 Format de sauvegarde JSON (formatVersion 1)

```json
{
  "formatVersion": 1,
  "exportDate": "2026-04-19T15:27:11",
  "appVersion": "2026.04",
  "titreListe": "Courses",
  "parametres": [ { "cle": "afficherFaites", "valeur": "true" } ],
  "categories": [ { "nom": "Frais", "ordre": 0, "couleur": 4294967295, "repliee": false } ],
  "taches":     [ { "titre": "Lait", "categorie": "Frais", "faite": false, "ordre": 0 } ]
}
```

À la restauration, seule la version 1 est acceptée ; les `id` des produits sont régénérés par Room.

### 3.8 Limites connues et dette technique

À garder en tête lors des évolutions :

* **Migrations Room** : aucune migration n'est déclarée dans `TodoDatabase` et le schéma exporté n'est pas versionné. Toute évolution du modèle doit ajouter une migration explicite pour préserver les données.
* **Import CSV** : découpage par `split(";")` qui ne gère pas les guillemets produits par l'export ; correspondance de doublon exacte (casse et catégorie comprises).
* **Pas de clé étrangère** entre produits et catégories (cohérence assurée par le code).
* **ViewModel unique** assez volumineux (environ 700 lignes) : états, CSV et sauvegarde y sont regroupés.
* **Libellés** mêlant « tâche » et vocabulaire de courses.
* **Données d'exemple** du premier lancement héritées de la todo list.
* **Tests** : seuls les tests d'exemple générés par Android Studio sont présents.
* `StatusBarColor` défini via une API dépréciée dans `Theme.kt`.

---

## 4. Méthode de développement

* Développement assisté par LLM : les évolutions sont décrites dans des **spécifications par module** au format Markdown (structure : Objectif, Fonctionnalités, Écrans, Comportement, Règles métier, Modèle de données, Architecture, Composants UI, Cas limites, Tests, Critères d'acceptation, Travail attendu), puis implémentées par Gemini dans Android Studio.
* Consignes habituelles pour l'implémentation : ne créer que les classes nécessaires, ne pas modifier les autres modules, ne pas refactoriser l'application sans demande explicite, compiler après chaque étape, résumer à la fin les fichiers créés, les fichiers modifiés et les points restant à faire.
* Nommage du code en **français** (`ajouterTache`, `basculerFait`, `CategorieEntity`…), à conserver.
