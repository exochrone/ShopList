# MODULE 06
Recherche : dépliage automatique des catégories repliées

==========================
OBJECTIF
==========================

Aujourd'hui, en recherche, une catégorie repliée qui contient des produits correspondant à la recherche n'affiche que son en-tête : les produits trouvés sont invisibles.

Ce module introduit deux règles :

1. **Dépliage automatique (affichage seulement)** : pendant une recherche, une catégorie qui contient au moins un produit correspondant à la recherche s'affiche **dépliée**, même si elle est repliée en base. Ce dépliage n'est **jamais écrit en base**. Dès qu'elle ne contient plus de résultat (le terme change), ou à la sortie de la recherche, elle reprend son **état initial**, c'est-à-dire son état `repliee` en base.
2. **Toucher de l'utilisateur = synchronisation avec la base** : quand l'utilisateur touche l'en-tête d'une catégorie, l'état affiché est inversé **et** enregistré en base, de sorte que l'affichage et la base soient de nouveau identiques. En recherche, ce toucher **ne ferme pas la recherche**.

Exemple :

* La catégorie « Boulangerie » est repliée en base et contient « Pain » et « Pain de mie ».
* L'utilisateur saisit « pain » : les 2 produits apparaissent, « Boulangerie » est dépliée automatiquement. Rien n'est écrit en base (`repliee` reste `true`).
* Il continue sa saisie : « pain au ». Plus aucun produit de « Boulangerie » ne correspond : ses 2 produits sont masqués et elle reprend son état initial (repliée). Comme toute catégorie sans résultat pendant une recherche, elle n'est pas affichée.
* S'il revient à « pain », elle est de nouveau dépliée automatiquement.

Ce que le module ne fait pas :

* il ne modifie pas le filtre de recherche (accents, casse, nom du produit ou de la catégorie) ;
* il ne modifie pas le fait qu'une catégorie sans résultat n'est pas affichée pendant une recherche ;
* il ne modifie pas l'affichage ni le toucher des en-têtes hors recherche ;
* il ne modifie ni le modèle Room, ni la sauvegarde JSON, ni l'import/export CSV, ni le partage ;
* il ne touche pas au menu ⋮ des catégories, ni au glisser-déposer (déjà désactivé en recherche).

**Changement d'une règle du MODULE 05** : jusqu'ici, en recherche, toucher un en-tête de catégorie fermait la recherche puis repliait ou dépliait la catégorie. Avec ce module, pendant une recherche active (terme non vide), toucher un en-tête **ne ferme plus la recherche**. Les autres fermetures automatiques du MODULE 05 et du MODULE 05b sont inchangées.

Modules dont il dépend : MODULE 05 et MODULE 05b (fermeture de la recherche), déjà implémentés.

==========================
FONCTIONNALITÉS
==========================

L'utilisateur peut, en recherche :

* voir les produits trouvés d'une catégorie repliée, sans avoir à la déplier ;
* replier ou déplier une catégorie en touchant son en-tête, sans quitter la recherche ; ce choix est enregistré en base comme hors recherche.

==========================
ÉCRANS
==========================

Nom : écran principal (`TodoListScreen`, route `"liste"`).

Objectif : afficher la liste filtrée par la recherche.

Contenu : inchangé, sauf l'état replié / déplié affiché des catégories pendant une recherche active.

Actions possibles : toucher l'en-tête d'une catégorie pendant une recherche active (voir COMPORTEMENT).

Navigation : aucune modification.

==========================
COMPORTEMENT
==========================

Définitions :

* **Recherche active** : `modeRecherche == true` **et** `uiState.termeRecherche.trim()` non vide.
* **État en base** : `CategorieEntity.repliee`.
* **État affiché** : état replié / déplié réellement montré à l'écran (chevron et présence des produits).
* **Catégories touchées en recherche** : ensemble local (noms de catégories) des catégories dont l'utilisateur a touché l'en-tête pendant la recherche en cours. Pour elles, le dépliage automatique ne s'applique plus jusqu'à la sortie de la recherche : leur état affiché est leur état en base.

1. **État affiché**

   | Situation | État affiché |
   |---|---|
   | Hors recherche active | état en base |
   | Recherche active, catégorie avec résultats, non touchée en recherche | **déplié** (dépliage automatique) |
   | Recherche active, catégorie touchée en recherche | état en base |
   | Recherche active, catégorie sans résultat | catégorie non affichée (inchangé) ; elle a retrouvé son état en base |

   * Une catégorie dépliée en base se comporte de la même façon : elle est déjà dépliée, le dépliage automatique ne change rien pour elle.
   * Le chevron de l'en-tête suit l'état affiché : déplié = `Icons.Default.ExpandMore` (description « Replier »), replié = `Icons.Default.ChevronRight` (description « Déplier »).
   * Le compteur `(N)` de l'en-tête reste le nombre de produits non cochés de la catégorie (inchangé).
   * Le dépliage automatique n'écrit jamais en base.

2. **Toucher l'en-tête pendant une recherche active**

   * L'état affiché est inversé et l'état en base devient égal au nouvel état affiché :
     * si l'état affiché est **différent** de l'état en base (cas d'une catégorie dépliée automatiquement alors qu'elle est repliée en base) : la catégorie s'affiche repliée ; l'état en base est déjà « replié », il n'y a **pas d'écriture** en base ;
     * si l'état affiché est **égal** à l'état en base : l'état en base est inversé via `onToggleCategorieRepliee` (écriture en base), l'affichage suit.
   * Dans les deux cas, la catégorie est ajoutée à l'ensemble des catégories touchées en recherche.
   * La recherche **n'est pas fermée** : le terme n'est pas effacé, le clavier n'est pas fermé de force, la liste ne défile pas.

   Exemple : « Boulangerie » repliée en base, recherche « pain ».
   * Affichage : dépliée automatiquement.
   * 1er toucher : elle s'affiche repliée, la base reste « repliée » (aucune écriture).
   * 2e toucher : elle s'affiche dépliée, la base passe à « dépliée » (écriture).
   * Sortie de la recherche : elle reste dépliée (état en base).

3. **Toucher l'en-tête en mode recherche avec un terme vide** : comportement actuel du MODULE 05 inchangé (la recherche se ferme avec `quitterRecherche(CibleDefilement(ligne.cleUnique, null))`, puis `onToggleCategorieRepliee`).

4. **Toucher l'en-tête hors recherche** : inchangé (`onToggleCategorieRepliee`).

5. **Changement du terme pendant la recherche** : l'état affiché est recalculé à chaque changement.
   * Une catégorie non touchée qui gagne des résultats se déplie automatiquement ; une catégorie qui n'a plus de résultat disparaît et retrouve son état en base.
   * L'ensemble des catégories touchées est **conservé** jusqu'à la sortie de la recherche : une catégorie touchée qui retrouve des résultats s'affiche dans son état en base, sans dépliage automatique.

6. **Effacer le terme avec la croix** (mode recherche ouvert, terme vide) : plus de recherche active, toutes les catégories s'affichent dans leur état en base. L'ensemble des catégories touchées n'est pas vidé.

7. **Sortie de la recherche** (flèche retour, Retour système, toucher d'un produit, action du menu ⋮, toucher sous la liste, toucher d'un en-tête avec un terme vide) :
   * l'ensemble des catégories touchées est vidé ;
   * toutes les catégories s'affichent dans leur état en base, qui contient tous les choix faits par toucher pendant la recherche.

==========================
RÈGLES MÉTIER
==========================

* Seul un toucher de l'utilisateur sur un en-tête peut modifier `repliee` en base ; le dépliage automatique ne le modifie jamais.
* Après un toucher, l'état affiché et l'état en base de la catégorie sont identiques.
* Le dépliage automatique ne s'applique qu'en recherche active, aux catégories qui ont au moins un résultat et qui n'ont pas été touchées pendant la recherche en cours.
* Pendant une recherche active, toucher un en-tête ne ferme pas la recherche.

==========================
MODÈLE DE DONNÉES
==========================

Aucune modification de la base Room (version 4 conservée, aucune migration), de la sauvegarde JSON, du CSV ou du partage. L'écriture en base du repli utilise la fonction existante `TodoViewModel.toggleCategorieRepliee`.

Modification de `LigneAffichee.Header` (fichier `ui/screens/liste/LigneAffichee.kt`) : ajout d'un champ indiquant l'état replié **affiché**.

```kotlin
data class Header(
    val categorie: CategorieEntity,
    val nombreNonCoches: Int,
    val replieeAffichee: Boolean = categorie.repliee
) : LigneAffichee() {
    override val cleUnique = "header_${categorie.nom}"
}
```

La clé `cleUnique` ne change pas.

==========================
ARCHITECTURE
==========================

Aucune classe nouvelle. Fichiers modifiés :

1. `app/src/main/java/com/jb/shoplist/Formatage.kt` : ajout d'une fonction pure, testable :

```kotlin
/**
 * État replié à afficher pour une catégorie.
 * En recherche active, une catégorie qui a des résultats est affichée dépliée
 * (sans modification en base), sauf si l'utilisateur a touché son en-tête
 * pendant la recherche : elle suit alors son état en base.
 */
fun categorieReplieeAffichee(
    replieeEnBase: Boolean,
    rechercheActive: Boolean,
    aDesResultats: Boolean,
    toucheeEnRecherche: Boolean
): Boolean =
    if (rechercheActive && aDesResultats && !toucheeEnRecherche) false else replieeEnBase
```

2. `app/src/main/java/com/jb/shoplist/ui/screens/liste/LigneAffichee.kt` : champ `replieeAffichee` ajouté à `Header` (voir MODÈLE DE DONNÉES).

3. `app/src/main/java/com/jb/shoplist/ui/screens/liste/TodoListScreen.kt` :

   * nouvel état local, déclaré près de `modeRecherche` :

     ```kotlin
     var categoriesToucheesEnRecherche by remember { mutableStateOf(setOf<String>()) }
     ```

   * `quitterRecherche(...)` : ajouter `categoriesToucheesEnRecherche = emptySet()`.

   * calcul de `lignes` : ajouter `categoriesToucheesEnRecherche` aux clés du `remember(...)`. Dans la boucle, remplacer l'usage de `cat.repliee` par l'état affiché :

     ```kotlin
     val rechercheActive = recherche.isNotEmpty()
     // ... calcul existant de tachesFinales inchangé ...
     if (tachesFinales.isNotEmpty() || (recherche.isEmpty() && uiState.afficherFaites)) {
         val replieeAffichee = categorieReplieeAffichee(
             replieeEnBase = cat.repliee,
             rechercheActive = rechercheActive,
             aDesResultats = tachesFinales.isNotEmpty(),
             toucheeEnRecherche = cat.nom in categoriesToucheesEnRecherche
         )
         add(LigneAffichee.Header(cat, nombreNonCoches, replieeAffichee))
         if (!replieeAffichee) {
             tachesFinales.forEach { tache ->
                 add(LigneAffichee.ItemTache(tache, cat.couleur))
             }
         }
     }
     ```

     `recherche` est la variable existante (`uiState.termeRecherche.trim().sansAccents().lowercase()`), non vide seulement en mode recherche.

   * en-tête de catégorie : le chevron et sa description utilisent `ligne.replieeAffichee` au lieu de `ligne.categorie.repliee`.

   * `clickable` de l'en-tête :

     ```kotlin
     .clickable {
         val rechercheActive = modeRecherche && uiState.termeRecherche.trim().isNotEmpty()
         if (rechercheActive) {
             // L'état affiché est inversé et la base est alignée dessus.
             // Si l'affichage diffère de la base (dépliage automatique d'une catégorie
             // repliée en base), le nouvel état affiché est l'état en base : pas d'écriture.
             if (ligne.replieeAffichee == ligne.categorie.repliee) {
                 onToggleCategorieRepliee(ligne.categorie)
             }
             categoriesToucheesEnRecherche = categoriesToucheesEnRecherche + ligne.categorie.nom
         } else {
             if (modeRecherche) quitterRecherche(CibleDefilement(ligne.cleUnique, null))
             onToggleCategorieRepliee(ligne.categorie)
         }
     }
     ```

Ne pas modifier `TodoViewModel`, `TodoListTopBar`, `TacheItem`, `CategoriesScreen`, les DAO ni la base.

==========================
COMPOSANTS UI
==========================

* `LazyColumn` existante (aucun changement de structure).
* `Icon` du chevron de l'en-tête (`Icons.Default.ExpandMore` / `Icons.Default.ChevronRight`).
* Aucun nouveau dialogue, Toast ou snackbar.

==========================
CAS LIMITES
==========================

* Catégorie trouvée par son nom (le terme correspond au nom de catégorie) : tous ses produits sont des résultats, elle est dépliée automatiquement avec tous ses produits (cochés compris, comme toute recherche).
* Plusieurs catégories repliées avec résultats : chacune est dépliée automatiquement et peut être touchée indépendamment.
* Toucher rapide deux fois de suite sur une catégorie dépliée automatiquement : 1er toucher sans écriture (affichage replié), 2e toucher avec écriture (`repliee = false`). La décision du 2e toucher repose sur `ligne.replieeAffichee` et `ligne.categorie.repliee` de la ligne recomposée ; aucune écriture n'est faite tant que l'affichage et la base diffèrent.
* Sortie de la recherche par toucher d'un produit d'une catégorie dépliée automatiquement et non touchée : la recherche se ferme, le dialogue « Modifier le produit » s'ouvre, la catégorie redevient repliée, la liste défile vers son en-tête (clé de repli déjà gérée par `CibleDefilement`).
* Cocher / décocher un produit d'une catégorie dépliée automatiquement : la recherche reste ouverte, la catégorie reste dépliée, rien n'est écrit sur la catégorie.
* Retour Android en recherche : ferme la recherche (inchangé), l'ensemble local est vidé.
* Écran recréé : `modeRecherche` et l'ensemble local sont perdus (`remember` simple), comme aujourd'hui pour la recherche. Les choix faits par toucher sont conservés, puisqu'ils sont en base.

==========================
TESTS
==========================

Tests unitaires à ajouter dans `app/src/test/java/com/jb/shoplist/FormatageTest.kt`, pour `categorieReplieeAffichee` :

* hors recherche, repliée en base → `true` ;
* hors recherche, dépliée en base → `false` ;
* recherche active, repliée en base, avec résultats, non touchée → `false` ;
* recherche active, repliée en base, sans résultat, non touchée → `true` ;
* recherche active, repliée en base, avec résultats, touchée → `true` ;
* recherche active, dépliée en base, touchée → `false`.

Tests manuels :

1. Replier « Boulangerie » (qui contient « Pain » et « Pain de mie »). Rechercher « pain » : « Boulangerie » est dépliée, les 2 produits sont visibles.
2. Compléter en « pain au » : « Boulangerie » n'est plus affichée. Revenir à « pain » : elle réapparaît dépliée.
3. Quitter la recherche sans toucher l'en-tête : « Boulangerie » est repliée. Fermer et rouvrir l'application : toujours repliée.
4. Rechercher « pain », toucher l'en-tête « Boulangerie » : elle se replie, la recherche reste ouverte. Quitter la recherche : elle est repliée.
5. Rechercher « pain », toucher deux fois l'en-tête : repliée, puis dépliée. Quitter la recherche : elle est dépliée ; fermer et rouvrir l'application : toujours dépliée.
6. Catégorie dépliée en base, rechercher un de ses produits, toucher son en-tête : elle se replie, la recherche reste ouverte. Quitter la recherche : elle est repliée.
7. Ouvrir la recherche sans rien saisir, toucher un en-tête : la recherche se ferme et la catégorie bascule (inchangé).

==========================
CRITÈRES D'ACCEPTATION
==========================

* compile, aucun nouveau warning, aucun crash ;
* en recherche active, les produits trouvés d'une catégorie repliée sont visibles, sans écriture en base ;
* une catégorie dépliée automatiquement reprend son état en base dès qu'elle n'a plus de résultat et à la sortie de la recherche ;
* un toucher d'en-tête en recherche active inverse l'affichage, aligne la base dessus et ne ferme pas la recherche ;
* les autres comportements de la recherche (MODULE 05, 05b) sont inchangés ;
* `FormatageTest` passe.

==========================
TRAVAIL ATTENDU DE GEMINI
==========================

1. Ajouter `categorieReplieeAffichee` dans `Formatage.kt` et ses tests dans `FormatageTest.kt`. Compiler et lancer les tests unitaires.
2. Ajouter le champ `replieeAffichee` à `LigneAffichee.Header`. Compiler.
3. Modifier `TodoListScreen.kt` (état local, `quitterRecherche`, calcul de `lignes`, chevron, `clickable` de l'en-tête). Compiler.

Créer uniquement les classes nécessaires (aucune classe nouvelle n'est attendue).

Ne pas modifier d'autres modules.

Ne pas refactoriser l'application.

Compiler après chaque étape.

À la fin, résumer :

• fichiers créés

• fichiers modifiés

• points restant à faire
