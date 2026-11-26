# MODULE 08
Écran principal : corrections et ajustements (snackbar, recherche, barre en bas, menus)

==========================
OBJECTIF
==========================

Ce module regroupe six corrections et ajustements de l'écran principal (`TodoListScreen`) :

1. **Snackbar en double** : les snackbars « Annuler » (« Tâche supprimée », « N tâche(s) cochée(s) », suppression de catégorie) s'affichent deux fois. Une seule doit s'afficher.
2. **Recherche quittée en dépliant / repliant une catégorie** : en mode recherche avec le champ vide, toucher un en-tête de catégorie ferme la recherche. La recherche doit rester ouverte, quel que soit le contenu du champ.
3. **Liste calée en bas** quand la barre est en position « Bas » : si la liste est plus courte que l'écran, elle est collée au-dessus de la barre (le séparateur de fin de liste juste au-dessus de la barre).
4. **Ordre du menu principal inversé** quand la barre est en position « Bas ».
5. **Icônes dans le menu principal** : Share, Checklist, Settings.
6. **Menu ⋮ d'une catégorie** : « Ajout d'élément » renommé « Ajouter un produit » ; icône de « Choix de la couleur » dans la couleur de la catégorie.

Ce que le module ne fait pas :

* il ne change pas les actions du menu ⋮ d'une catégorie en recherche : elles **ferment toujours la recherche** (MODULE 05) ;
* il ne change pas le dépliage automatique ni le toucher d'en-tête quand un terme est saisi (MODULE 06) ;
* il n'ouvre pas la liste en bas quand elle est plus longue que l'écran : à l'ouverture, on voit le **début** de la liste, comme aujourd'hui ;
* il ne crée pas de zone de fermeture de la recherche au-dessus de la liste en position « Bas » (la fermeture par toucher sous la liste du MODULE 05b ne s'applique qu'en position « Haut ») ;
* il ne modifie pas les barres et snackbars des écrans Options, Gestion des catégories et Partager la liste ;
* il ne modifie ni la base Room, ni la sauvegarde JSON, ni le CSV, ni le partage, ni le ViewModel.

Modules dont il dépend : MODULES 05, 05b, 06, 07c (implémentés).

==========================
FONCTIONNALITÉS
==========================

L'utilisateur :

* voit une seule snackbar « Annuler » à la fois ;
* peut déplier ou replier une catégorie en recherche sans quitter la recherche, même avec un champ vide ;
* en position « Bas », a une liste courte collée au-dessus de la barre ;
* en position « Bas », trouve « Options » en premier dans le menu (au plus près de la barre) ;
* reconnaît les entrées du menu principal à leur icône ;
* lit « Ajouter un produit » dans le menu ⋮ d'une catégorie et voit la couleur de la catégorie sur l'icône de la palette.

==========================
ÉCRANS
==========================

Nom : écran principal (`TodoListScreen`, route `"liste"`), barre `TodoListTopBar`.

**Menu principal (hamburger)**

| Position de la barre | Ordre des entrées |
|---|---|
| Haut | Partager la liste, Tout cocher, Options |
| Bas | Options, Tout cocher, Partager la liste |

Icônes (`leadingIcon` de chaque `DropdownMenuItem`, couleur par défaut du menu, `contentDescription = null`) :

| Entrée | Icône |
|---|---|
| Partager la liste | `Icons.Default.Share` |
| Tout cocher | `Icons.Default.Checklist` |
| Options | `Icons.Default.Settings` |

**Menu ⋮ d'une catégorie**

* « Ajout d'élément » devient **« Ajouter un produit »** (icône `Icons.Default.Add` inchangée, action inchangée).
* « Choix de la couleur » : l'icône `Icons.Default.Palette` prend la couleur de la catégorie, `tint = Color(ligne.categorie.couleur)`.
* Les autres entrées sont inchangées.

**Liste, position « Bas »**

* Liste plus courte que l'espace disponible : les lignes sont regroupées en bas, la dernière (séparateur de fin de liste, avec sa marge actuelle de 16 dp) juste au-dessus de la barre ; l'espace vide est en haut.
* Liste plus longue : affichage inchangé ; à l'ouverture, le début de la liste est visible, on fait défiler vers le bas pour atteindre le séparateur.

**Liste, position « Haut »** : inchangée.

==========================
COMPORTEMENT
==========================

1. **Snackbar unique**

   * Cause : la même `SnackbarHostState` est affichée par deux `SnackbarHost` : celui du `Scaffold` de `TodoApp` (`MainActivity.kt`) et celui du `Scaffold` de `TodoListScreen` (de même, `CategoriesScreen` a le sien). En position « Haut », les deux se superposent exactement et la duplication ne se voit pas ; en position « Bas », l'un est au-dessus de la barre, l'autre en bas de l'écran.
   * Correction : supprimer le `SnackbarHost` du `Scaffold` de `TodoApp`. Les snackbars restent affichées par les `Scaffold` de `TodoListScreen` (au-dessus de la barre en position « Bas ») et de `CategoriesScreen`. La logique `showSnackbar` / `annulerDerniereAction` / `consommerAction` de `TodoApp` est inchangée.
   * Aucun autre écran ne produit d'action annulable (Options, Partager) : rien à afficher ailleurs.

2. **Toucher un en-tête en mode recherche**

   * Terme saisi (recherche active) : inchangé (MODULE 06 : inversion de l'état affiché, alignement de la base, la recherche reste ouverte).
   * **Champ vide** (mode recherche ouvert, aucun terme) : la catégorie est repliée ou dépliée en base (`onToggleCategorieRepliee`), **la recherche reste ouverte** (le champ garde le focus, le clavier n'est pas fermé de force, pas de défilement).
   * Hors recherche : inchangé.
   * Les actions du menu ⋮ d'une catégorie (Ajouter un produit, Tri alphabétique, Modifier la catégorie, Choix de la couleur, Supprimer) ferment toujours la recherche avant leur action (MODULE 05, inchangé).
   * Le toucher d'un en-tête ne ferme donc plus jamais la recherche.

3. **Liste calée en bas (position « Bas »)**

   * `LazyColumn` : `verticalArrangement = Arrangement.Bottom` en position « Bas », `Arrangement.spacedBy(0.dp)` (valeur actuelle) en position « Haut ».
   * Conséquence normale : en position « Bas », replier une catégorie d'une liste courte fait descendre les lignes situées au-dessus (la liste reste collée à la barre).
   * Le défilement, le glisser-déposer, le défilement vers la cible après la recherche (`CibleDefilement`) et le dépliage automatique sont inchangés.

4. **Fermeture de la recherche par toucher sous la liste (MODULE 05b)**

   * Position « Haut » : inchangée.
   * Position « Bas » : désactivée (le `Modifier.pointerInput` n'est pas appliqué). On quitte la recherche par la flèche de la barre, le Retour système ou les autres fermetures automatiques du MODULE 05.

5. **Menu principal** : l'ordre suit la position de la barre (voir ÉCRANS), au moment où le menu est ouvert. Les actions des entrées sont inchangées.

==========================
RÈGLES MÉTIER
==========================

* Une seule snackbar visible à la fois.
* En mode recherche, toucher un en-tête de catégorie ne ferme jamais la recherche.
* Les actions du menu ⋮ d'une catégorie ferment la recherche.
* La liste n'est calée en bas qu'en position « Bas ».
* L'ordre du menu principal est inversé en position « Bas ».

==========================
MODÈLE DE DONNÉES
==========================

Aucune modification : base Room (version 4, aucune migration), sauvegarde JSON, CSV, partage, `TodoUiState`, `TodoViewModel` inchangés.

==========================
ARCHITECTURE
==========================

Aucune classe nouvelle. Fichiers modifiés :

**1. `MainActivity.kt`** (`TodoApp`)

* Supprimer le paramètre `snackbarHost = { SnackbarHost(hostState = snackbarHostState) }` du `Scaffold`. Conserver le `Scaffold`, `snackbarHostState` et le `LaunchedEffect(actionAnnulable)`.
* Supprimer l'import `androidx.compose.material3.SnackbarHost` s'il devient inutilisé.

**2. `ui/screens/liste/TodoListScreen.kt`**

* `clickable` de l'en-tête de catégorie :

```kotlin
.clickable {
    val rechercheActive = modeRecherche && uiState.termeRecherche.trim().isNotEmpty()
    if (rechercheActive) {
        // Inchangé (MODULE 06)
        if (ligne.replieeAffichee == ligne.categorie.repliee) {
            onToggleCategorieRepliee(ligne.categorie)
        }
        categoriesToucheesEnRecherche = categoriesToucheesEnRecherche + ligne.categorie.nom
    } else {
        // Hors recherche, ou recherche ouverte avec un champ vide : la recherche n'est pas fermée
        onToggleCategorieRepliee(ligne.categorie)
    }
}
```

* `LazyColumn` :
  * `verticalArrangement = if (positionBarre == PositionBarre.BAS) Arrangement.Bottom else Arrangement.spacedBy(0.dp)` ;
  * fermeture par toucher sous la liste : remplacer la condition `if (modeRecherche)` du `Modifier.pointerInput` par `if (modeRecherche && positionBarre == PositionBarre.HAUT)`. Le contenu du `detectTapGestures` est inchangé.
* Menu ⋮ de la catégorie :
  * texte `"Ajout d'élément"` remplacé par `"Ajouter un produit"` ;
  * `Icon(Icons.Default.Palette, contentDescription = null, tint = Color(ligne.categorie.couleur))`.
* Ne rien changer d'autre (en particulier les appels `quitterRecherche(...)` des actions du menu ⋮ et de `onClickTache`).

**3. `ui/screens/liste/TodoListTopBar.kt`**

* Ajouter les `leadingIcon` des trois entrées (imports `Icons.Default.Share`, `Icons.Default.Checklist`, `Icons.Default.Settings` couverts par `androidx.compose.material.icons.filled.*`, déjà importé ; icônes étendues déjà présentes dans le projet).
* Ordre selon `enBas` (paramètre existant) :

```kotlin
val entrees: List<@Composable () -> Unit> = listOf(
    {
        DropdownMenuItem(
            text = { Text("Partager la liste") },
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
            onClick = {
                afficherMenu = false
                onOuvrirPartage()
            }
        )
    },
    {
        DropdownMenuItem(
            text = { Text("Tout cocher") },
            leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null) },
            onClick = {
                afficherMenu = false
                onToutCocher()
            }
        )
    },
    {
        DropdownMenuItem(
            text = { Text("Options") },
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
            onClick = {
                afficherMenu = false
                onOuvrirOptions()
            }
        )
    }
)

DropdownMenu(
    expanded = afficherMenu,
    onDismissRequest = { afficherMenu = false }
) {
    (if (enBas) entrees.reversed() else entrees).forEach { entree -> entree() }
}
```

  Ne pas créer de nouveau fichier ni de composant public.

Ne pas modifier `TodoViewModel`, `ShopListNavHost`, `OptionsScreen`, `CategoriesScreen`, `PartagerScreen`, les DAO ni la base.

==========================
COMPOSANTS UI
==========================

* `Scaffold` (`snackbarHost`)
* `LazyColumn` (`verticalArrangement`, `Arrangement.Bottom`)
* `DropdownMenuItem` (`leadingIcon`)
* `Icon` (`Icons.Default.Share`, `Checklist`, `Settings`, `Palette` avec `tint`)

==========================
CAS LIMITES
==========================

* Snackbar dans Gestion des catégories (suppression de catégorie) : une seule, affichée par `CategoriesScreen`.
* Snackbar déclenchée sur la liste puis navigation vers Options avant sa fin : elle n'est plus visible sur Options (aucun `SnackbarHost`) ; elle se termine en arrière-plan (`Dismissed` → `consommerAction`), comme une snackbar ignorée. Acceptable.
* Catégorie blanche : icône palette blanche (couleur de la catégorie, sans exception).
* Recherche ouverte, champ vide, toucher un en-tête puis saisir un terme : la catégorie a l'état choisi en base ; le dépliage automatique s'applique ensuite normalement (elle n'est pas dans `categoriesToucheesEnRecherche`).
* Position « Bas », liste vide de produits mais catégories affichées : les en-têtes et le séparateur sont collés au-dessus de la barre.
* Position « Bas », recherche sans résultat : seul le séparateur est affiché, juste au-dessus de la barre.
* Position « Bas », clavier ouvert : la liste se réduit, son contenu reste collé au-dessus de la barre (elle-même au-dessus du clavier).
* Passage de « Haut » à « Bas » (ou l'inverse) : à l'arrivée sur la liste, le calage et l'ordre du menu suivent la nouvelle position.

==========================
TESTS
==========================

Tests automatisés : `./gradlew test` (`FormatageTest`) et `./gradlew connectedAndroidTest` (`OptionsScreenTest`, `ProduitEditionDialogTest`) passent sans modification. Pas de nouveau test automatisé.

Tests manuels :

1. Position « Haut » : supprimer un produit : une seule snackbar « Tâche supprimée », « Annuler » restaure le produit.
2. Position « Bas » : même test, une seule snackbar, au-dessus de la barre. Idem avec « Tout cocher ».
3. Gestion des catégories : supprimer une catégorie : une seule snackbar.
4. Ouvrir la recherche sans rien saisir, toucher un en-tête : la catégorie se replie (ou se déplie), la recherche reste ouverte. Retoucher : elle bascule de nouveau. Quitter la recherche : l'état est celui du dernier toucher.
5. Avec un terme saisi : comportement du MODULE 06 inchangé.
6. En recherche, choisir « Tri alphabétique » dans le menu ⋮ : la recherche se ferme (inchangé).
7. Position « Bas », liste courte (masquer les cochés ou replier des catégories) : lignes collées au-dessus de la barre, séparateur juste au-dessus de la barre.
8. Position « Bas », liste longue : à l'ouverture, début de la liste visible ; défiler jusqu'en bas : séparateur juste au-dessus de la barre.
9. Position « Bas », en recherche : toucher la zone vide au-dessus de la liste ne ferme pas la recherche.
10. Position « Haut », en recherche : toucher sous le séparateur ferme la recherche (MODULE 05b inchangé).
11. Menu principal en « Haut » : Partager la liste, Tout cocher, Options, avec leurs icônes. En « Bas » : Options, Tout cocher, Partager la liste.
12. Menu ⋮ d'une catégorie : « Ajouter un produit » ; icône palette dans la couleur de la catégorie (vérifier avec deux catégories de couleurs différentes).

==========================
CRITÈRES D'ACCEPTATION
==========================

* compile, aucun nouveau warning, aucun crash ;
* une seule snackbar sur la liste (« Haut » et « Bas ») et sur Gestion des catégories ;
* toucher un en-tête en mode recherche ne ferme jamais la recherche ;
* actions du menu ⋮ : fermeture de la recherche inchangée ;
* position « Bas » : liste courte calée en bas, liste longue ouverte au début, pas de fermeture par toucher de la zone vide ;
* position « Haut » : affichage et fermeture par toucher sous la liste inchangés ;
* ordre et icônes du menu principal conformes ;
* libellé « Ajouter un produit » et icône palette colorée ;
* tests existants passants.

==========================
TRAVAIL ATTENDU DE GEMINI
==========================

1. `MainActivity.kt` : suppression du `SnackbarHost` de `TodoApp`. Compiler.
2. `TodoListScreen.kt` : toucher d'en-tête, `verticalArrangement`, condition du toucher sous la liste, libellé « Ajouter un produit », couleur de l'icône palette. Compiler.
3. `TodoListTopBar.kt` : icônes et ordre du menu selon `enBas`. Compiler.
4. Lancer `./gradlew test` et `./gradlew connectedAndroidTest`.

Créer uniquement les classes nécessaires.

Ne pas modifier d'autres modules.

Ne pas refactoriser l'application.

Compiler après chaque étape.

À la fin, résumer :

• fichiers créés

• fichiers modifiés

• points restant à faire
