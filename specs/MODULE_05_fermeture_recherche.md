# MODULE 05
Fermeture automatique de la recherche et bouton œil en recherche

==========================
OBJECTIF
==========================

Aujourd'hui, la recherche reste ouverte quand l'utilisateur fait autre chose dans la liste (ouvrir un produit, replier une catégorie, utiliser le menu ⋮ d'une catégorie). Il doit ensuite la quitter à la main. De plus, le bouton œil affiché pendant la recherche n'a aucun effet visible : en recherche, les produits cochés sont toujours inclus, mais le bouton modifie quand même le réglage mémorisé `afficherFaites`.

Ce module :

1. ferme automatiquement la recherche quand l'utilisateur :
   * touche le texte d'un produit (ouverture du dialogue « Modifier le produit ») ;
   * touche un en-tête de catégorie (replier / déplier) ;
   * choisit une action dans le menu ⋮ d'une catégorie ;
2. fait défiler la liste complète pour que l'élément touché reste visible après la fermeture ;
3. masque le bouton œil pendant la recherche.

Ce que le module ne fait pas :

* Il ne modifie pas le filtrage de la recherche (accents et casse ignorés, nom du produit ou de la catégorie, produits cochés toujours inclus).
* Cocher ou décocher un produit en recherche **ne ferme pas** la recherche (permet de cocher plusieurs résultats d'affilée).
* Ouvrir le menu ⋮ puis le refermer sans choisir d'action **ne ferme pas** la recherche. L'entrée d'information « N élément(s) » (désactivée) ne déclenche rien.
* Pas de changement du ViewModel, de Room, de la sauvegarde JSON, du CSV ni du partage.
* Le menu hamburger n'est pas concerné : il est déjà masqué en mode recherche et le reste.

Modules dont il dépend : aucun nouveau. S'appuie sur l'existant de `TodoListScreen.kt`, `TodoListTopBar.kt`, `LigneAffichee.kt`.

==========================
FONCTIONNALITÉS
==========================

En mode recherche, l'utilisateur peut :

* toucher le nom ou la quantité d'un produit : la recherche se ferme, puis le dialogue « Modifier le produit » s'ouvre sur ce produit ;
* toucher un en-tête de catégorie : la recherche se ferme, puis la catégorie est repliée ou dépliée ;
* choisir dans le menu ⋮ d'une catégorie « Ajout d'élément », « Tri alphabétique », « Modifier la catégorie », « Choix de la couleur » ou « Supprimer » : la recherche se ferme, puis l'action se déroule exactement comme aujourd'hui ;
* cocher / décocher un produit : comportement inchangé, la recherche reste ouverte.

Le bouton œil n'est plus affiché en mode recherche. Il reste affiché, et inchangé, hors recherche.

==========================
ÉCRANS
==========================

Nom : écran principal (`TodoListScreen`, route "liste")

Objectif : inchangé.

Contenu en mode recherche, barre du haut (`TodoListTopBar`, branche `isSearchMode == true`) :
* flèche retour « Quitter la recherche » (inchangée) ;
* champ de recherche (inchangé) ;
* croix « Effacer la recherche » si le champ n'est pas vide (inchangée) ;
* **plus de bouton œil**.

Navigation : inchangée.

==========================
COMPORTEMENT
==========================

« Fermer la recherche » signifie exactement ce que font aujourd'hui la flèche retour et le `BackHandler` :
* `modeRecherche = false` ;
* `onSetTermeRecherche("")` ;
* en plus : masquer le clavier et retirer le focus (`focusManager.clearFocus()` et `keyboardController?.hide()`), pour que le clavier ne reste pas affiché sous un dialogue.

Ordre des opérations pour chaque déclencheur (en mode recherche uniquement) :

1. **Toucher le texte d'un produit** (`TacheItem.onClickTache`) :
   fermer la recherche, demander le défilement vers ce produit, puis `tacheEnEdition = ligne.tache` (comme aujourd'hui). Le dialogue s'ouvre normalement au-dessus de la liste complète.
2. **Toucher un en-tête de catégorie** (`clickable` du `Row` de `LigneAffichee.Header`) :
   fermer la recherche, demander le défilement vers cet en-tête, puis `onToggleCategorieRepliee(ligne.categorie)` (comme aujourd'hui).
3. **Action du menu ⋮** (« Ajout d'élément », « Tri alphabétique », « Modifier la catégorie », « Choix de la couleur », « Supprimer ») :
   `menuOuvertPourCategorie = null` (comme aujourd'hui), fermer la recherche, demander le défilement vers l'en-tête de la catégorie, puis le reste du `onClick` actuel inchangé.

Hors mode recherche, ces trois déclencheurs gardent exactement leur comportement actuel (aucun défilement ajouté).

**Défilement après fermeture** :

* La cible est mémorisée dans un état local de `TodoListScreen`, puis traitée une fois la liste complète reconstruite (le terme de recherche passe par le ViewModel, donc `lignes` est recalculée de façon asynchrone).
* Cible produit : clé `"tache_<id>"`, avec repli sur l'en-tête de sa catégorie `"header_<nom>"` si le produit n'est pas affiché (catégorie repliée, ou produit coché alors que les cochés sont masqués).
* Cible en-tête : clé `"header_<nom>"`, sans repli.
* Si aucune des clés n'est trouvée dans `lignes` (par exemple catégorie sans produit non coché et cochés masqués), on ne fait rien et on abandonne la cible.
* Si l'élément trouvé est déjà entièrement visible, on ne fait pas défiler. Sinon `lazyListState.scrollToItem(index)` (sans animation).
* La cible est remise à `null` après traitement, qu'il y ait eu défilement ou non. Elle n'est jamais traitée tant que `uiState.termeRecherche` n'est pas vide.

**Annulation / retour** : fermer le dialogue ouvert après la fermeture de la recherche (croix ou Retour) ramène sur la liste complète ; la recherche n'est pas rouverte.

==========================
RÈGLES MÉTIER
==========================

* La fermeture automatique n'a lieu que si `modeRecherche == true`.
* Le cochage et le décochage ne ferment jamais la recherche.
* Ouvrir ou refermer le menu ⋮ sans choisir d'action ne ferme pas la recherche.
* Le réglage `afficherFaites` ne peut plus être modifié pendant la recherche (bouton masqué). Sa valeur mémorisée n'est pas modifiée par l'entrée ou la sortie de recherche.
* Le filtrage de `lignes` n'est pas modifié.

==========================
MODÈLE DE DONNÉES
==========================

Aucun changement Room, aucune migration, aucun changement du format de sauvegarde JSON ni du CSV.

Nouvel état local dans `TodoListScreen` (privé au fichier) :

```kotlin
private data class CibleDefilement(
    val cle: String,          // "tache_<id>" ou "header_<nom>"
    val cleRepli: String?     // "header_<nom>" pour un produit, null pour un en-tête
)
```

```kotlin
var cibleDefilement by remember { mutableStateOf<CibleDefilement?>(null) }
```

==========================
ARCHITECTURE
==========================

Aucune classe à créer en dehors de la `data class CibleDefilement` privée ci-dessus.

Fichiers à modifier :

1. `ui/screens/liste/TodoListScreen.kt`
2. `ui/screens/liste/TodoListTopBar.kt`

**TodoListScreen.kt**

a) Ajouter, après la déclaration de `modeRecherche` :

```kotlin
val focusManager = LocalFocusManager.current
val keyboardController = LocalSoftwareKeyboardController.current
var cibleDefilement by remember { mutableStateOf<CibleDefilement?>(null) }

fun quitterRecherche(cible: CibleDefilement? = null) {
    focusManager.clearFocus()
    keyboardController?.hide()
    modeRecherche = false
    onSetTermeRecherche("")
    cibleDefilement = cible
}
```

b) Utiliser `quitterRecherche()` dans le `BackHandler` existant et dans la branche « fermeture » de `onToggleSearchMode` (comportement identique à l'actuel, sans cible) :

```kotlin
BackHandler(enabled = modeRecherche) { quitterRecherche() }
...
onToggleSearchMode = {
    if (modeRecherche) quitterRecherche() else modeRecherche = true
},
```

c) Ajouter, après la déclaration de `lazyListState` :

```kotlin
LaunchedEffect(lignes, cibleDefilement) {
    val cible = cibleDefilement ?: return@LaunchedEffect
    if (uiState.termeRecherche.isNotEmpty()) return@LaunchedEffect
    var index = lignes.indexOfFirst { it.cleUnique == cible.cle }
    if (index == -1 && cible.cleRepli != null) {
        index = lignes.indexOfFirst { it.cleUnique == cible.cleRepli }
    }
    if (index != -1) {
        val info = lazyListState.layoutInfo
        val visible = info.visibleItemsInfo.firstOrNull { it.index == index }
        val entierementVisible = visible != null &&
            visible.offset >= info.viewportStartOffset &&
            visible.offset + visible.size <= info.viewportEndOffset
        if (!entierementVisible) lazyListState.scrollToItem(index)
    }
    cibleDefilement = null
}
```

d) Déclencheurs (ne rien changer d'autre dans ces blocs) :

```kotlin
// En-tête de catégorie
.clickable {
    if (modeRecherche) quitterRecherche(CibleDefilement(ligne.cleUnique, null))
    onToggleCategorieRepliee(ligne.categorie)
}
```

```kotlin
// Chacune des 5 actions du menu ⋮ (pas l'entrée "N élément(s)")
onClick = {
    menuOuvertPourCategorie = null
    if (modeRecherche) quitterRecherche(CibleDefilement(ligne.cleUnique, null))
    // ... suite actuelle inchangée
}
```

```kotlin
// Produit
onClickTache = {
    if (modeRecherche) quitterRecherche(
        CibleDefilement("tache_${ligne.tache.id}", "header_${ligne.tache.categorie}")
    )
    tacheEnEdition = ligne.tache
},
```

Ne pas modifier `onCheckedChange` de `TacheItem`.

Imports à ajouter si absents : `androidx.compose.ui.platform.LocalFocusManager`, `androidx.compose.ui.platform.LocalSoftwareKeyboardController`, `androidx.compose.runtime.LaunchedEffect`.

**TodoListTopBar.kt**

Dans la branche `if (isSearchMode)` de `actions`, supprimer le `IconButton(onClick = onToggleAfficherFaites) { ... }`. Garder la croix « Effacer la recherche ». La branche hors recherche (avec son bouton œil) reste inchangée. Ne pas modifier la signature de `TodoListTopBar`.

==========================
COMPOSANTS UI
==========================

* `TopAppBar` (actions en recherche réduites à la croix d'effacement)
* `LazyColumn` + `LazyListState.scrollToItem`
* `DropdownMenu` / `DropdownMenuItem` (menu ⋮, inchangé visuellement)
* `ProduitEditionDialog` (inchangé)
* `LocalFocusManager`, `LocalSoftwareKeyboardController`

==========================
CAS LIMITES
==========================

* Recherche ouverte sans texte saisi : les déclencheurs ferment aussi la recherche ; le défilement fonctionne (la clé `cibleDefilement` déclenche le `LaunchedEffect` même si `lignes` ne change pas).
* Produit coché touché en recherche alors que les cochés sont masqués : après fermeture, le produit n'est plus affiché ; défilement vers l'en-tête de sa catégorie si celui-ci est affiché, sinon rien. Le dialogue s'ouvre normalement.
* Produit d'une catégorie repliée : n'apparaît pas en recherche (comportement actuel), donc non concerné.
* Repli d'une catégorie en recherche : après fermeture, la catégorie est repliée / dépliée dans la liste complète, l'en-tête reste visible.
* « Supprimer » depuis le menu ⋮ en recherche : la recherche se ferme, la confirmation s'affiche ; « Non » laisse la liste complète, sans rouvrir la recherche.
* « Ajout d'élément » en recherche : le produit ajouté apparaît dans la liste complète, en fin de catégorie (il peut être hors écran, pas de défilement supplémentaire).
* Clavier affiché au moment du toucher : il se ferme.
* Rotation : application en portrait uniquement, non concerné.
* Retour Android pendant le dialogue : ferme le dialogue (la recherche est déjà fermée).
* Cochage en recherche : la recherche reste ouverte, le bouton d'annulation du cochage n'est toujours pas visible en recherche (inchangé).

==========================
TESTS
==========================

Pas de nouveau test unitaire (aucune fonction pure ajoutée). Pas de test Compose obligatoire.

Tests manuels sur appareil :

1. Rechercher « lait », toucher le produit : la recherche se ferme, le clavier disparaît, le dialogue s'ouvre ; à la fermeture, la liste complète est affichée avec le produit visible.
2. Rechercher, toucher un en-tête : recherche fermée, catégorie repliée / dépliée, en-tête visible.
3. Rechercher, ouvrir ⋮ puis toucher hors du menu : la recherche reste ouverte.
4. Rechercher, ouvrir ⋮ et choisir chacune des 5 actions : la recherche se ferme, l'action se déroule comme hors recherche.
5. Rechercher, cocher puis décocher plusieurs produits : la recherche reste ouverte.
6. En recherche, le bouton œil n'est plus affiché ; en sortant, il est affiché avec l'état d'avant la recherche.
7. Cochés masqués, rechercher un produit coché, le toucher : dialogue ouvert, pas de crash, défilement vers l'en-tête si visible.
8. Hors recherche : toucher un produit, un en-tête, une action ⋮ : comportement strictement identique à avant (pas de défilement).

==========================
CRITÈRES D'ACCEPTATION
==========================

* compile
* aucun nouveau warning
* aucun crash
* fermeture automatique sur les 3 déclencheurs, et seulement eux
* cochage sans effet sur la recherche
* défilement vers l'élément touché après fermeture
* bouton œil absent en recherche, inchangé hors recherche
* aucun changement de comportement hors recherche

==========================
TRAVAIL ATTENDU DE GEMINI
==========================

1. Modifier `TodoListTopBar.kt` : retirer le bouton œil de la branche recherche. Compiler.
2. Modifier `TodoListScreen.kt` : ajouter `CibleDefilement`, `quitterRecherche()`, utiliser cette fonction dans `BackHandler` et `onToggleSearchMode`. Compiler.
3. Ajouter le `LaunchedEffect` de défilement. Compiler.
4. Brancher les trois déclencheurs (produit, en-tête, 5 actions du menu ⋮). Compiler.

Consignes :

* Créer uniquement les classes nécessaires.
* Ne pas modifier d'autres modules ni d'autres fichiers que les deux cités (ni ViewModel, ni DAO, ni `TacheItem`, ni `ProduitEditionDialog`).
* Ne pas refactoriser l'application.
* Compiler après chaque étape.
* À la fin, résumer :
  • fichiers créés
  • fichiers modifiés
  • points restant à faire
