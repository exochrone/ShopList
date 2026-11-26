# MODULE 07
Écran « Options »

==========================
OBJECTIF
==========================

Créer un écran « Options », ouvert depuis une nouvelle entrée « Options » du menu principal (icône hamburger). Cet écran regroupe :

* le nom de la liste, renommable au toucher (même fonction que l'entrée « Renommer la liste » du menu) ;
* un bouton « Gérer les catégories » qui ouvre l'écran « Gestion des catégories » existant, sans le modifier ; sa flèche retour ramène à l'écran Options ;
* en bas de l'écran, l'information « shopList v<version> ».

Ce que le module ne fait pas :

* il ne retire pas les entrées « Renommer la liste » et « Gérer les catégories » du menu principal : elles sont conservées le temps des tests et seront retirées dans un module ultérieur ;
* il ne modifie pas l'écran « Gestion des catégories » (`CategoriesScreen`) ;
* il ne crée aucun nouveau paramètre ni réglage enregistré ;
* il ne modifie ni la base Room, ni la sauvegarde JSON, ni l'import/export CSV, ni le partage.

Modules dont il dépend : aucun module en cours. S'appuie sur l'existant (`SaisieDialog`, `TodoViewModel.modifierTitreListe`, `CategoriesScreen`, `ShopListNavHost`).

==========================
FONCTIONNALITÉS
==========================

L'utilisateur peut :

* ouvrir l'écran Options depuis le menu principal ;
* voir et renommer le nom de la liste ;
* ouvrir la gestion des catégories, puis revenir à l'écran Options ;
* voir la version de l'application ;
* revenir à la liste.

==========================
ÉCRANS
==========================

**1. Menu principal (`TodoListTopBar`)**

Nouvelle entrée **« Options »**, placée **en dernier**, sous « Tout cocher ». Les autres entrées sont inchangées (y compris « Gérer les catégories » et « Renommer la liste »).

**2. Écran « Options » (nouveau)**

Nom : `OptionsScreen`, route `"options"`.

Objectif : regrouper les réglages de l'application.

Contenu, de haut en bas :

* **Barre du haut** (`TopAppBar`) : flèche retour (`Icons.AutoMirrored.Filled.ArrowBack`, description « Retour ») à gauche, titre « Options » en gras, couleur de fond `MaterialTheme.colorScheme.surfaceVariant` (comme `CategoriesScreen`).
* **Zone « Nom de la liste »** : une ligne pleine largeur, cliquable, contenant :
  * le libellé « Nom de la liste » (12 sp, `MaterialTheme.colorScheme.onSurfaceVariant`) ;
  * en dessous, le nom actuel de la liste (`uiState.titreListe`, « Courses » par défaut ou nom personnalisé), 16 sp, couleur du texte normale, 2 lignes maximum avec points de suspension.
* **Bouton « Gérer les catégories »** : `OutlinedButton` pleine largeur, texte « Gérer les catégories ».
* **Zone d'information**, calée en bas de l'écran, centrée horizontalement : texte « shopList v2026.04 » (la valeur suit `BuildConfig.VERSION_NAME`), 12 sp, `MaterialTheme.colorScheme.onSurfaceVariant`.

Marges : `padding(horizontal = 16.dp)` pour le contenu, `16.dp` d'espace entre la zone « Nom de la liste » et le bouton, `16.dp` sous la zone d'information.

Actions possibles :

* toucher la zone « Nom de la liste » : ouvre le dialogue « Renommer la liste » (voir COMPORTEMENT) ;
* toucher « Gérer les catégories » : ouvre l'écran « Gestion des catégories » ;
* flèche retour ou Retour système : revient à l'écran principal.

Navigation :

* Liste → menu « Options » → Options.
* Options → « Gérer les catégories » → Gestion des catégories → flèche retour ou Retour système → Options.
* Options → flèche retour ou Retour système → Liste.
* Liste → menu « Gérer les catégories » → Gestion des catégories → retour → Liste (inchangé).

==========================
COMPORTEMENT
==========================

1. **Renommer la liste depuis Options**

   * Le toucher de la zone « Nom de la liste » ouvre `SaisieDialog` avec **exactement les mêmes paramètres** que l'entrée « Renommer la liste » de `TodoListScreen` :
     * `titre = "Renommer la liste"`,
     * `valeurInitiale` = nom actuel de la liste,
     * `placeholder = "Nouveau titre"`,
     * `onValider = { titre -> onModifierTitreListe(titre); fermeture du dialogue }`,
     * `onAnnuler` = fermeture du dialogue.
   * Comportement hérité de `SaisieDialog`, inchangé : valeur nettoyée (`trim`) ; valider un champ vide ferme le dialogue sans renommer ; boutons « Valider » et « Annuler » ; toucher hors du dialogue ou Retour ferme sans enregistrer.
   * Après validation, le nouveau nom s'affiche immédiatement dans la zone « Nom de la liste » (lu depuis `uiState.titreListe`) et dans le titre de l'écran principal.

2. **Gérer les catégories depuis Options**

   * Le bouton appelle `navController.navigate(Routes.CATEGORIES)`.
   * L'écran « Gestion des catégories » est le même composable, avec les mêmes paramètres que depuis le menu. Son `onRetour` existant (`navController.popBackStack()`) ramène à l'écran Options, puisqu'il est au-dessus dans la pile de navigation. Aucune modification de `CategoriesScreen` n'est nécessaire.

3. **Retour depuis Options** : `navController.popBackStack()`, retour à l'écran principal dans l'état où il était (la recherche est fermée, voir CAS LIMITES).

4. **Version** : texte construit comme `"shopList v$versionApp"`, où `versionApp` vaut `BuildConfig.VERSION_NAME` (aujourd'hui « 2026.04 »). Le `versionCode` n'est pas affiché.

==========================
RÈGLES MÉTIER
==========================

* Le nom de la liste affiché dans Options et celui de l'écran principal sont la même donnée (paramètre `titreListe`) : un renommage depuis l'un est visible dans l'autre.
* Le renommage depuis Options et depuis le menu passent par la même fonction `TodoViewModel.modifierTitreListe`.
* La version affichée vient uniquement de `BuildConfig.VERSION_NAME` (aucune valeur écrite en dur).

==========================
MODÈLE DE DONNÉES
==========================

Aucune modification de la base Room (version 4 conservée, aucune migration), de la sauvegarde JSON (le titre de la liste y est déjà), du CSV ou du partage.

Aucun nouvel état dans `TodoViewModel` ni dans `TodoUiState`. Seul état local de `OptionsScreen` : `afficherDialogRenommer: Boolean` (`remember`).

==========================
ARCHITECTURE
==========================

Fichier à créer :

* `app/src/main/java/com/jb/shoplist/ui/screens/options/OptionsScreen.kt`, package `com.jb.shoplist.ui.screens.options` :

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    titreListe: String,
    versionApp: String,
    onModifierTitreListe: (String) -> Unit,
    onOuvrirCategories: () -> Unit,
    onRetour: () -> Unit
)
```

  Structure : `Scaffold(topBar = { TopAppBar(...) })`, puis une `Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp))` contenant la zone « Nom de la liste », un `Spacer(16.dp)`, le bouton, un `Spacer(Modifier.weight(1f))`, puis le texte de version centré (`Modifier.fillMaxWidth()`, `textAlign = TextAlign.Center`) et un `Spacer(16.dp)`. Le dialogue `SaisieDialog` est affiché si `afficherDialogRenommer`.

  L'écran reste sans état métier : il ne connaît ni le ViewModel ni `BuildConfig`.

Fichiers à modifier :

1. `ui/navigation/Routes.kt` : ajouter `const val OPTIONS = "options"`.

2. `ui/navigation/ShopListNavHost.kt` :
   * dans `composable(Routes.LISTE)`, passer `onOuvrirOptions = { navController.navigate(Routes.OPTIONS) }` à `TodoListScreen` ;
   * ajouter :

     ```kotlin
     composable(Routes.OPTIONS) {
         OptionsScreen(
             titreListe = uiState.titreListe,
             versionApp = BuildConfig.VERSION_NAME,
             onModifierTitreListe = viewModel::modifierTitreListe,
             onOuvrirCategories = { navController.navigate(Routes.CATEGORIES) },
             onRetour = { navController.popBackStack() }
         )
     }
     ```

     avec l'import `com.jb.shoplist.BuildConfig` (`buildConfig = true` est déjà activé dans `app/build.gradle.kts`).

3. `ui/screens/liste/TodoListScreen.kt` : ajouter le paramètre `onOuvrirOptions: () -> Unit` (à côté de `onOuvrirPartage`) et le transmettre à `TodoListTopBar`.

4. `ui/screens/liste/TodoListTopBar.kt` : ajouter le paramètre `onOuvrirOptions: () -> Unit` et, en dernier dans le `DropdownMenu`, après « Tout cocher » :

   ```kotlin
   DropdownMenuItem(
       text = { Text("Options") },
       onClick = {
           afficherMenu = false
           onOuvrirOptions()
       }
   )
   ```

Ne pas modifier `CategoriesScreen`, `PartagerScreen`, `TodoViewModel`, `SaisieDialog`, les DAO ni la base. Ne pas retirer les entrées « Renommer la liste » et « Gérer les catégories » du menu.

==========================
COMPOSANTS UI
==========================

* `Scaffold`, `TopAppBar`, `IconButton` + `Icon` (flèche retour)
* `Column`, `Spacer`, `Text`
* `Modifier.clickable` pour la zone « Nom de la liste »
* `OutlinedButton`
* `SaisieDialog` (existant)
* `DropdownMenuItem` (menu principal)

==========================
CAS LIMITES
==========================

* Nom de liste très long : 2 lignes maximum dans la zone, puis points de suspension (`maxLines = 2`, `TextOverflow.Ellipsis`).
* Renommer avec un champ vide : le dialogue se ferme sans renommer (comportement de `SaisieDialog`).
* Retour Android avec le dialogue ouvert : ferme le dialogue, reste sur Options.
* Ouverture d'Options alors que la recherche était ouverte : impossible, le menu hamburger n'est pas affiché en recherche.
* Actions annulables depuis Gestion des catégories ouverte depuis Options (suppression de catégorie) : la snackbar « Annuler » fonctionne comme aujourd'hui (même `snackbarHostState`).
* Double toucher rapide sur « Gérer les catégories » ou sur l'entrée « Options » : peut empiler deux fois l'écran (comportement identique aux entrées existantes du menu, non traité dans ce module).
* Écran en portrait uniquement : pas de rotation à gérer.

==========================
TESTS
==========================

Pas de test unitaire nouveau (aucune logique pure).

Test Compose facultatif (`app/src/androidTest/java/com/jb/shoplist/OptionsScreenTest.kt`) si simple à écrire :

* le nom passé en paramètre et « shopList v2026.04 » (avec `versionApp = "2026.04"`) sont affichés ;
* le toucher de « Gérer les catégories » appelle `onOuvrirCategories` ;
* le toucher du nom ouvre le dialogue « Renommer la liste » ; valider « Courses maison » appelle `onModifierTitreListe("Courses maison")`.

Tests manuels :

1. Menu : « Options » apparaît en dernier ; « Renommer la liste » et « Gérer les catégories » sont toujours présents.
2. Ouvrir Options : nom de la liste, bouton et « shopList v2026.04 » en bas de l'écran.
3. Renommer depuis Options : le nouveau nom s'affiche dans Options, puis dans le titre de la liste au retour.
4. Renommer depuis le menu « Renommer la liste » : le nom affiché dans Options est le même.
5. Options → Gérer les catégories → flèche retour : retour sur Options. Idem avec le Retour système.
6. Options → flèche retour : retour sur la liste.
7. Menu → Gérer les catégories → retour : retour sur la liste (inchangé).

==========================
CRITÈRES D'ACCEPTATION
==========================

* compile, aucun nouveau warning, aucun crash ;
* entrée « Options » en dernier dans le menu, autres entrées inchangées ;
* écran Options conforme (titre, nom de la liste, bouton, version en bas) ;
* renommage depuis Options identique au renommage depuis le menu ;
* navigation Options ↔ Gestion des catégories ↔ Liste conforme ;
* version lue depuis `BuildConfig.VERSION_NAME`.

==========================
TRAVAIL ATTENDU DE GEMINI
==========================

1. Ajouter `Routes.OPTIONS`. Compiler.
2. Créer `OptionsScreen.kt`. Compiler.
3. Ajouter `onOuvrirOptions` à `TodoListTopBar` (entrée « Options » en dernier) et à `TodoListScreen`. Compiler.
4. Brancher la route `Routes.OPTIONS` et `onOuvrirOptions` dans `ShopListNavHost`. Compiler.
5. Facultatif : `OptionsScreenTest.kt`.

Créer uniquement les classes nécessaires.

Ne pas modifier d'autres modules.

Ne pas refactoriser l'application.

Compiler après chaque étape.

À la fin, résumer :

• fichiers créés

• fichiers modifiés

• points restant à faire
