# MODULE 04
Unicité des produits par catégorie

==========================
OBJECTIF
==========================

Aujourd'hui, le nom d'un produit doit être unique dans **toute** la liste : impossible d'avoir « Lait » dans « Frais » et « Lait » dans « Drive ».

Ce module remplace cette règle par une **unicité par catégorie** : un même nom de produit peut apparaître plusieurs fois dans la liste, mais **jamais deux fois dans la même catégorie**.

La règle s'applique partout où un produit peut arriver dans une catégorie :

1. dialogue « Nouveau produit » ;
2. dialogue « Modifier le produit » (renommage et changement de catégorie) ;
3. glisser-déposer d'un produit vers une autre catégorie ;
4. import CSV.

Comparaison des noms (inchangée par rapport à l'existant) : sans tenir compte de la casse ni des espaces en début et fin. Les accents **comptent** : « Epinards » et « Épinards » sont deux noms différents (comportement actuel conservé).

Ce que le module ne fait pas :

* Aucune modification du modèle Room, aucune migration, aucun changement de version de base.
* Aucune modification de la sauvegarde / restauration JSON (format, validation).
* Aucune modification de l'export CSV ni du partage texte : si un produit existe dans deux catégories et que l'option « Masquer les catégories » est active, il apparaît deux fois dans le texte partagé (comportement voulu).
* Aucune modification de la règle d'unicité des **catégories**.
* Aucune modification de la recherche (les homonymes s'affichent chacun sous sa catégorie).
* Aucune correction des données existantes (les données actuelles respectent déjà la règle globale, donc a fortiori la règle par catégorie).
* Pas de changement de la correspondance des **catégories** à l'import CSV (comparaison exacte du nom de catégorie, inchangée).

Modules dont il dépend : MODULE 01 (quantités), MODULE 02 (`ProduitEditionDialog`).

==========================
FONCTIONNALITÉS
==========================

L'utilisateur peut :

* créer un produit portant le même nom qu'un produit d'une **autre** catégorie ;
* renommer un produit avec le nom d'un produit d'une **autre** catégorie ;
* déplacer (dialogue ou glisser-déposer) un produit vers une catégorie qui ne contient pas déjà un produit du même nom.

L'utilisateur ne peut pas :

* avoir deux produits de même nom dans une même catégorie, quel que soit le chemin (dialogue, glisser-déposer, import CSV).

==========================
ÉCRANS
==========================

Aucun nouvel écran. Écrans et composants concernés :

Nom : dialogue « Nouveau produit » / « Modifier le produit » (`ProduitEditionDialog`)
Objectif : contrôler l'unicité du nom dans la catégorie **sélectionnée dans le dialogue**.
Contenu : inchangé.
Actions possibles : inchangées.
Navigation : inchangée.

Nom : écran principal (`TodoListScreen`), glisser-déposer des produits
Objectif : annuler un déplacement qui créerait un doublon dans la catégorie d'arrivée.

==========================
COMPORTEMENT
==========================

### 1. Dialogues produit (ajout et modification)

* Le contrôle porte sur le couple (nom saisi, catégorie sélectionnée dans la liste déroulante du dialogue).
* Le contrôle est réévalué en direct quand le nom change **et** quand la catégorie sélectionnée change : choisir une catégorie qui contient déjà ce nom désactive le bouton et affiche le message ; revenir à une autre catégorie le réactive.
* Message d'erreur affiché sous le champ « Nom du produit » (nouveau libellé, exact, point final compris) :

```
Ce produit existe déjà dans cette catégorie.
```

* Le bouton « Ajouter » (mode ajout) ou « Valider » (mode modification) est désactivé tant que le message est affiché. La touche « OK » du clavier ne fait rien dans ce cas (comportement actuel de `executerValidation`).
* Mode modification : le produit en cours d'édition est exclu du contrôle (on peut valider sans changer de nom, ou changer seulement la casse : « lait » → « Lait »).
* Exemple : « Lait » existe dans « Frais ». Ajouter « lait » dans « Frais » : refusé. Ajouter « Lait » dans « Drive » : accepté. Modifier le « Lait » de « Drive » en le déplaçant vers « Frais » : refusé.

### 2. Glisser-déposer (annulation au dépôt)

Le glisser-déposer actuel enregistre la nouvelle position à chaque pas (`onReordonnerTaches` appelé dans le callback de `rememberReorderableLazyListState`). Ce fonctionnement est **conservé** pendant le glisser. Le contrôle se fait **au lâcher** :

1. Au début du glisser d'un produit, mémoriser :
   * l'`id` du produit glissé ;
   * sa catégorie d'origine ;
   * une copie de la liste complète des produits telle qu'elle est avant le glisser (`uiState.taches`).
2. Pendant le glisser, à chaque pas, mémoriser la catégorie courante du produit glissé (`nouvelleCategorieName` déjà calculée dans le callback existant).
3. Au lâcher (ou à l'interruption du geste) :
   * si la catégorie finale est différente de la catégorie d'origine **et** que la copie mémorisée contient un **autre** produit (id différent) de cette catégorie finale portant le même nom (règle de comparaison du module) :
     * restaurer la liste mémorisée en appelant `onReordonnerTaches(copieAvantGlissement)` : le produit revient à sa place et dans sa catégorie d'origine, et tous les ordres reviennent à leur état d'avant le glisser ;
     * afficher un Toast court, libellé exact :
       ```
       Ce produit existe déjà dans la catégorie <catégorie finale> : déplacement annulé.
       ```
   * sinon : rien de plus (la position est déjà enregistrée, comportement actuel).
   * Dans tous les cas, effacer les valeurs mémorisées.
4. Pendant le glisser, le produit peut traverser visuellement une catégorie contenant un homonyme : seule la catégorie au moment du lâcher compte.
5. Un glisser à l'intérieur de la même catégorie n'est jamais annulé.

### 3. Import CSV

* La détection d'un produit existant se fait désormais avec la règle du module (casse et espaces ignorés), dans la catégorie de la ligne (nom de catégorie comparé exactement, comme aujourd'hui).
* Produit trouvé : comportement inchangé (quantité remplacée seulement si le fichier en fournit une non vide, état coché inchangé, nom existant conservé tel quel).
* Produit non trouvé : création, comportement inchangé.
* La détection tient compte des produits créés **plus haut dans le même fichier** : un fichier contenant `Frais;Lait;1` puis `Frais;lait;2` crée un seul produit « Lait » dont la quantité finale est `2`.

==========================
RÈGLES MÉTIER
==========================

* R1. Deux produits d'une même catégorie ne peuvent pas avoir le même nom.
* R2. Deux produits de catégories différentes peuvent avoir le même nom.
* R3. « Même nom » : égalité après `trim()` des deux côtés et passage en minuscules (`lowercase()`), accents comptés. Implémenté par une seule fonction `nomsProduitsIdentiques` (voir Architecture), utilisée partout.
* R4. La comparaison de catégorie se fait sur le nom exact de la catégorie (`TacheEntity.categorie == nom`), comme dans le reste du code.
* R5. Un produit n'est jamais comparé à lui-même (exclusion par `id`) en modification et en glisser-déposer.
* R6. La restauration d'une sauvegarde JSON n'effectue aucun contrôle de doublon de produit (inchangé).

==========================
MODÈLE DE DONNÉES
==========================

Aucun changement : `TacheEntity`, `CategorieEntity`, `ParametreEntity`, version Room 4, schémas exportés, format JSON version 2 : tout est inchangé. Pas de migration.

La requête DAO `TacheDao.trouverParTitreEtCategorie` n'est plus utilisée par l'import. **La laisser en place** (ne pas la supprimer).

==========================
ARCHITECTURE
==========================

Aucune classe nouvelle. Fichiers modifiés :

### 1. `Formatage.kt`

Ajouter la fonction pure :

```kotlin
/** Vrai si deux noms de produit sont considérés identiques (casse et espaces de début/fin ignorés, accents comptés). */
fun nomsProduitsIdentiques(a: String, b: String): Boolean =
    a.trim().lowercase() == b.trim().lowercase()
```

### 2. `ui/components/ProduitEditionDialog.kt`

* Changer le type du paramètre :

```kotlin
onCheckErreurTitre: (titre: String, categorie: String) -> String?,
```

* Appel : `val erreurTitre = onCheckErreurTitre(titreClean, categorie)` où `categorie` est l'état local de la catégorie sélectionnée (déjà existant). Comme il s'agit d'un état Compose, le contrôle est réévalué automatiquement au changement de catégorie.
* Rien d'autre ne change dans ce composant.

### 3. `ui/screens/liste/TodoListScreen.kt`

a) Les deux appels à `ProduitEditionDialog` :

```kotlin
// Mode ajout
onCheckErreurTitre = { titre, cat ->
    val existe = uiState.taches.any {
        it.categorie == cat && nomsProduitsIdentiques(it.titre, titre)
    }
    if (existe) "Ce produit existe déjà dans cette catégorie." else null
},

// Mode modification
onCheckErreurTitre = { titre, cat ->
    val existe = uiState.taches.any {
        it.id != tache.id && it.categorie == cat && nomsProduitsIdentiques(it.titre, titre)
    }
    if (existe) "Ce produit existe déjà dans cette catégorie." else null
},
```

b) Glisser-déposer :

* Ajouter dans `TodoListScreen` des états locaux (non sauvegardés, `remember`) :

```kotlin
var idTacheGlissee by remember { mutableStateOf<Int?>(null) }
var categorieOrigineGlissement by remember { mutableStateOf<String?>(null) }
var categorieFinaleGlissement by remember { mutableStateOf<String?>(null) }
var tachesAvantGlissement by remember { mutableStateOf<List<TacheEntity>?>(null) }
```

* Dans le callback existant de `rememberReorderableLazyListState`, juste après le calcul de `nouvelleCategorieName` : `categorieFinaleGlissement = nouvelleCategorieName`. Ne rien changer d'autre dans ce callback.
* Sur la ligne produit (`is LigneAffichee.ItemTache`), remplacer `Modifier.longPressDraggableHandle()` par la version avec callbacks de la bibliothèque Reorderable 2.4.0 :

```kotlin
Modifier.longPressDraggableHandle(
    onDragStarted = {
        idTacheGlissee = ligne.tache.id
        categorieOrigineGlissement = ligne.tache.categorie
        categorieFinaleGlissement = ligne.tache.categorie
        tachesAvantGlissement = uiState.taches
    },
    onDragStopped = {
        verifierDoublonApresGlissement()
    }
)
```

* `verifierDoublonApresGlissement()` : fonction locale dans `TodoListScreen` qui applique la règle de la section Comportement 2.3, utilise `nomsProduitsIdentiques`, appelle `onReordonnerTaches(copie)` en cas de doublon, affiche le Toast avec `Toast.makeText(context, …, Toast.LENGTH_SHORT).show()` (`val context = LocalContext.current` au niveau de `TodoListScreen`), puis remet les quatre états à `null`. Le nom du produit à comparer est celui de la copie mémorisée (`tachesAvantGlissement.first { it.id == idTacheGlissee }.titre`).
* Le Toast est affiché directement depuis l'écran (et non via `TodoViewModel.evenements`), pour ne pas modifier le ViewModel.
* **Si la signature de `longPressDraggableHandle` dans la version installée ne propose pas `onDragStarted` / `onDragStopped`, s'arrêter et me le signaler** au lieu de changer de version de bibliothèque ou d'inventer un autre mécanisme.
* Le glisser-déposer reste désactivé en mode recherche (inchangé).

### 4. `TodoViewModel.kt`, fonction `importerDepuisCsv`

Remplacer uniquement la recherche du produit existant. Dans le bloc `database.withTransaction { }` :

```kotlin
val tachesConnues = uiState.value.taches.toMutableList()
...
// 2. Gérer la tâche
val indexExistant = tachesConnues.indexOfFirst {
    it.categorie == nomCat && nomsProduitsIdentiques(it.titre, titreTache)
}
if (indexExistant == -1) {
    ordreMaxTache++
    val nouvelle = TacheEntity(
        titre = titreTache,
        categorie = nomCat,
        ordre = ordreMaxTache,
        quantite = quantiteImportee
    )
    val nouvelId = tacheDao.inserer(nouvelle)
    tachesConnues.add(nouvelle.copy(id = nouvelId.toInt()))
} else if (quantiteImportee.isNotBlank()) {
    val majee = tachesConnues[indexExistant].copy(quantite = quantiteImportee)
    tacheDao.mettreAJour(majee)
    tachesConnues[indexExistant] = majee
}
```

Le reste de `importerDepuisCsv` (catégories, transaction, messages, vidage de la pile de cochages) est inchangé.

==========================
COMPOSANTS UI
==========================

* `ProduitEditionDialog` (paramètre modifié, apparence inchangée).
* `Toast` (nouveau message d'annulation de glisser-déposer).
* `longPressDraggableHandle` avec `onDragStarted` / `onDragStopped`.

==========================
CAS LIMITES
==========================

* Mode modification, seule la casse change (« lait » → « Lait ») dans la même catégorie : accepté (exclusion par `id`).
* Mode modification, on change de catégorie vers une catégorie contenant un homonyme, puis on revient à la catégorie d'origine dans la liste déroulante : le bouton redevient actif.
* Homonyme coché dans la catégorie cible : il compte comme doublon (les produits cochés font partie de la liste).
* Homonyme dans une catégorie repliée ou dont les produits cochés sont masqués : il compte aussi (le contrôle porte sur `uiState.taches`, pas sur les lignes affichées).
* Glisser lâché dans la catégorie d'origine après avoir traversé une catégorie contenant un homonyme : pas d'annulation, pas de Toast.
* Glisser interrompu (geste annulé par le système) : même traitement qu'un lâcher.
* Glisser sans aucun déplacement (appui long puis lâcher) : pas d'annulation, pas de Toast.
* Import d'un fichier contenant deux fois le même produit dans la même catégorie, avec une casse différente : un seul produit créé.
* Import d'un produit déjà présent dans une autre catégorie : un nouveau produit est créé dans la catégorie du fichier (R2).
* Restauration d'une ancienne sauvegarde : inchangée, pas d'erreur.
* Partage avec catégories masquées : un produit présent dans deux catégories apparaît deux fois (voulu).
* Rotation : sans objet (portrait uniquement).

==========================
TESTS
==========================

### Tests unitaires (`app/src/test/java/com/jb/shoplist/FormatageTest.kt`)

Ajouter des tests pour `nomsProduitsIdentiques` :

* `"Lait"` / `"lait"` → vrai ;
* `"  Lait "` / `"Lait"` → vrai ;
* `"Lait"` / `"Lait demi-écrémé"` → faux ;
* `"Épinards"` / `"épinards"` → vrai ;
* `"Epinards"` / `"Épinards"` → faux.

### Tests Compose (`app/src/androidTest/java/com/jb/shoplist/ProduitEditionDialogTest.kt`)

* Adapter tous les appels existants : `onCheckErreurTitre = { _, _ -> null }`.
* Ajouter un test : catégories « Frais » et « Drive », `categorieInitiale = "Frais"`, `onCheckErreurTitre = { _, cat -> if (cat == "Frais") "Ce produit existe déjà dans cette catégorie." else null }`, saisie d'un nom : le message est affiché et « Ajouter » est désactivé.
* Ajouter un test symétrique avec `categorieInitiale = "Drive"` : pas de message, « Ajouter » activé.

### Tests manuels

1. Ajouter « Lait » dans « Drive » alors qu'il existe dans « Frais » : accepté, deux lignes « Lait » visibles.
2. Ajouter « lait » dans « Frais » : message « Ce produit existe déjà dans cette catégorie. », bouton désactivé.
3. Modifier le « Lait » de « Drive », choisir « Frais » dans la liste : message et bouton désactivé ; revenir à « Drive » : bouton actif.
4. Glisser le « Lait » de « Drive » dans « Frais » et lâcher : il revient à sa place dans « Drive », Toast « Ce produit existe déjà dans la catégorie Frais : déplacement annulé. ».
5. Glisser un autre produit sans homonyme dans « Frais » : déplacement conservé, pas de Toast.
6. Importer un CSV contenant `Frais;lait;3` : pas de nouveau produit, quantité du « Lait » de « Frais » passée à `3`.
7. Recherche « lait » : les deux produits apparaissent, chacun sous sa catégorie.

==========================
CRITÈRES D'ACCEPTATION
==========================

* compile
* aucun warning nouveau
* aucun crash
* un même nom est accepté dans deux catégories différentes
* un doublon dans une même catégorie est refusé par les deux dialogues, avec le message exact
* le contrôle du dialogue suit la catégorie sélectionnée en direct
* un glisser-déposer créant un doublon est annulé au lâcher, avec le Toast exact, et l'ordre d'origine est restauré
* l'import CSV ne crée pas de doublon par différence de casse ou d'espaces
* aucune modification du modèle Room, du format de sauvegarde, de l'export CSV ou du partage
* tests `FormatageTest` et `ProduitEditionDialogTest` à jour et au vert

==========================
TRAVAIL ATTENDU DE GEMINI
==========================

Étapes, en compilant après chacune :

1. Ajouter `nomsProduitsIdentiques` dans `Formatage.kt` et ses tests dans `FormatageTest.kt`.
2. Modifier la signature de `onCheckErreurTitre` dans `ProduitEditionDialog.kt` et l'appel interne.
3. Mettre à jour les deux appels de `ProduitEditionDialog` dans `TodoListScreen.kt` (nouvelle règle et nouveau message).
4. Mettre à jour `ProduitEditionDialogTest.kt` (adaptation des appels et deux nouveaux tests).
5. Ajouter l'annulation au dépôt du glisser-déposer dans `TodoListScreen.kt`.
6. Modifier la détection des produits existants dans `importerDepuisCsv` (`TodoViewModel.kt`).

Consignes :

* Créer uniquement les classes nécessaires (aucune n'est attendue).
* Ne pas modifier d'autres modules : sauvegarde / restauration, export CSV, partage, gestion des catégories, recherche, entités et DAO restent inchangés.
* Ne pas supprimer `TacheDao.trouverParTitreEtCategorie`.
* Ne pas refactoriser l'application, ne pas renommer, ne pas déplacer de fichiers, ne pas changer de version de bibliothèque.
* Conserver le nommage en français.
* Compiler après chaque étape.

À la fin, résumer :

• fichiers créés

• fichiers modifiés

• points restant à faire
