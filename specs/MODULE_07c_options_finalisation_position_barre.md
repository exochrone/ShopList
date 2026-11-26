# MODULE 07c
Écran « Options » : finalisation et position de la barre de l'écran principal

==========================
OBJECTIF
==========================

Suite des MODULES 07 et 07b (écran « Options », implémentés et validés).

Ce module :

1. **retire du menu principal** « Importer une liste », « Sauvegarde complète » et « Restaurer une sauvegarde », désormais accessibles uniquement depuis l'écran Options ;
2. **supprime le code en double** de l'import, de la sauvegarde et de la restauration dans l'écran principal (`TodoListScreen`) : seule la version de `OptionsScreen` est conservée ;
3. **corrige le libellé** de l'option en « Position de la barre de titre et d'options » ;
4. **revient à l'écran principal** après un import CSV réussi ou une restauration réussie lancés depuis Options ;
5. **applique la position choisie** (« Haut » / « Bas ») à la barre de l'écran principal, et **enregistre ce choix** en base et dans la sauvegarde JSON.

Barre concernée : la barre de l'écran principal (`TodoListTopBar`), avec tout son contenu :

* hors recherche : titre de la liste, boutons « Annuler le dernier cochage » (flèche retour), « Rechercher » (loupe), « Afficher / masquer les cochés » (œil), menu (hamburger) ;
* en recherche : flèche « Quitter la recherche », champ de recherche, croix « Effacer la recherche ».

* **Haut** : position actuelle (barre en haut de l'écran), comportement inchangé.
* **Bas** : la barre est affichée tout en bas de l'écran ; la liste commence tout en haut de l'écran (juste sous la barre d'état Android).

Ce que le module ne fait pas :

* il ne change pas la position des barres des écrans Options, Gestion des catégories et Partager la liste (toujours en haut) ;
* il ne modifie pas le fonctionnement de l'import CSV, de la sauvegarde ni de la restauration (formats, textes des dialogues, règles) ;
* il ne revient pas à l'écran principal après une « Sauvegarde complète » (on reste sur Options), ni après un import ou une restauration en erreur ou annulés ;
* il ne modifie pas le contenu de la barre (mêmes boutons, mêmes icônes, mêmes comportements), ni la recherche (MODULES 05, 05b, 06) ;
* il ne modifie pas la base Room (pas de migration : nouveau paramètre dans la table `parametres` existante).

Modules dont il dépend : MODULES 07 et 07b.

==========================
FONCTIONNALITÉS
==========================

L'utilisateur peut :

* choisir dans Options la position « Haut » ou « Bas » de la barre de l'écran principal, choix appliqué immédiatement et conservé à la fermeture de l'application ;
* importer une liste ou restaurer une sauvegarde depuis Options, puis revenir automatiquement sur la liste en cas de réussite.

Il ne peut plus importer, sauvegarder ni restaurer depuis le menu principal.

==========================
ÉCRANS
==========================

**1. Écran principal (`TodoListScreen`)**

* Position « Haut » : inchangé.
* Position « Bas » : barre (`TodoListTopBar`) en bas de l'écran ; liste au-dessus, depuis le haut de l'écran.
* Menu principal (hamburger), entrées dans cet ordre :
  * Partager la liste
  * Tout cocher
  * Options

**2. Écran « Options » (`OptionsScreen`)**

* Libellé de l'option : « Position de la barre de titre et d'options » (avec « s »).
* Le choix sélectionné à l'ouverture est le choix enregistré (« Haut » par défaut si rien n'est enregistré).
* Le reste de l'écran est inchangé.

Navigation :

* Options → « Importer une liste » → import réussi → retour sur l'écran principal.
* Options → « Restaurer une sauvegarde » → restauration réussie → retour sur l'écran principal.
* Dans tous les autres cas, on reste sur Options.

==========================
COMPORTEMENT
==========================

1. **Choix de la position dans Options**

   * Toucher « Haut » ou « Bas » enregistre immédiatement le choix (paramètre `positionBarre`).
   * Le choix est conservé à la fermeture de l'application et au changement d'écran.
   * En revenant sur l'écran principal, la barre est à la position choisie.

2. **Position « Bas » sur l'écran principal**

   * La barre est placée dans le `bottomBar` du `Scaffold` de `TodoListScreen` au lieu du `topBar`.
   * La liste occupe l'espace au-dessus de la barre ; son premier élément est juste sous la barre d'état Android (pas sous elle).
   * La barre reste au-dessus de la barre de navigation Android (gestes ou boutons).
   * **Recherche** : quand le clavier s'ouvre (loupe), la barre (champ, flèche, croix) remonte au-dessus du clavier et reste visible ; la liste se réduit en conséquence. Quand le clavier se ferme, la barre redescend en bas de l'écran.
   * Le menu hamburger s'ouvre vers le haut (comportement automatique de `DropdownMenu` quand il manque de place en dessous).
   * La snackbar (« Tâche supprimée », « N tâche(s) cochée(s) »…) s'affiche au-dessus de la barre (comportement automatique du `Scaffold` avec un `bottomBar`).
   * Tous les comportements de la liste sont inchangés : glisser-déposer, défilement, menu ⋮ des catégories, dialogues, fermeture de la recherche en touchant sous la liste (MODULE 05b), défilement vers la cible après la recherche.

3. **Retour à la liste après un import ou une restauration réussis**

   * Le ViewModel signale chaque réussite par un événement dédié (`operationReussie`), émis juste après le Toast de réussite existant :
     * import : après « Importation réussie ! » ;
     * restauration : après « Restauration réussie ».
   * Sur l'écran Options, cet événement ramène à l'écran principal : `navController.popBackStack(Routes.LISTE, inclusive = false)`.
   * Le Toast de réussite s'affiche toujours.
   * En cas d'erreur (Toast d'erreur, dialogue « Restauration impossible ») ou d'annulation, aucun événement n'est émis : on reste sur Options.

4. **Restauration et position**

   * Le paramètre `positionBarre` fait partie de la sauvegarde JSON (tableau `parametres`).
   * Restaurer une sauvegarde qui contient `positionBarre` applique sa valeur.
   * Restaurer une sauvegarde qui ne le contient pas (sauvegardes antérieures à ce module) remet « Haut » (la restauration vide la table `parametres` avant d'insérer ceux du fichier ; l'absence du paramètre donne la valeur par défaut).

==========================
RÈGLES MÉTIER
==========================

* Valeurs possibles du paramètre `positionBarre` : `"HAUT"` ou `"BAS"` (nom de l'`enum`).
* Valeur absente ou inconnue : « Haut ».
* La position ne concerne que la barre de l'écran principal.
* Seuls un import réussi et une restauration réussie ramènent à l'écran principal.
* Import, sauvegarde et restauration ne sont accessibles que depuis Options.

==========================
MODÈLE DE DONNÉES
==========================

**Base Room** : aucune modification de schéma, version 4 conservée, **aucune migration**. Nouveau paramètre dans la table `parametres` existante :

| cle | valeur |
|---|---|
| `positionBarre` | `"HAUT"` ou `"BAS"` |

**Enum** : l'`enum` privée `PositionBarre` de `OptionsScreen.kt` est **déplacée** dans `TodoViewModel.kt`, à côté de `EtatRestauration`, et rendue publique (elle sert au ViewModel, à la navigation et aux deux écrans) :

```kotlin
enum class PositionBarre { HAUT, BAS }
```

**Sauvegarde JSON** : format inchangé (`formatVersion` 2). Seul le contenu du tableau `parametres` s'enrichit d'une entrée `{ "cle": "positionBarre", "valeur": "BAS" }` quand le paramètre existe en base. Les anciennes sauvegardes restent restaurables sans changement de la lecture (tous les paramètres présents dans le fichier sont déjà relus).

**CSV, partage** : non concernés.

==========================
ARCHITECTURE
==========================

Aucune classe nouvelle (l'`enum` est déplacée, pas créée). Fichiers modifiés :

**1. `TodoViewModel.kt`**

* Déclarer `enum class PositionBarre { HAUT, BAS }` (voir MODÈLE DE DONNÉES).
* Nouveau flux et nouvelle fonction :

```kotlin
val positionBarre: StateFlow<PositionBarre> = parametreDao.observer("positionBarre")
    .map { valeur -> PositionBarre.entries.firstOrNull { it.name == valeur } ?: PositionBarre.HAUT }
    .stateIn(viewModelScope, SharingStarted.Eagerly, PositionBarre.HAUT)

fun setPositionBarre(position: PositionBarre) {
    viewModelScope.launch {
        parametreDao.enregistrer(ParametreEntity(cle = "positionBarre", valeur = position.name))
    }
}
```

  (Ajouter l'import `kotlinx.coroutines.flow.map`. `SharingStarted.Eagerly` pour que la valeur soit lue dès le démarrage et limiter l'affichage furtif de la barre en haut au lancement. Ne pas ajouter `positionBarre` à `TodoUiState`.)
* Nouvel événement de réussite :

```kotlin
private val _operationReussie = MutableSharedFlow<Unit>()
val operationReussie = _operationReussie.asSharedFlow()
```

  * `importerDepuisCsv` : `_operationReussie.emit(Unit)` juste après `_evenements.emit("Importation réussie !")` ;
  * `confirmerRestauration` : `_operationReussie.emit(Unit)` juste après `_evenements.emit("Restauration réussie")`.
  * Aucune émission dans les branches d'erreur.
* `construireJsonSauvegarde` : liste des clés sauvegardées `listOf("titreListe", "afficherFaites", "positionBarre")`.

Ne rien changer d'autre dans le ViewModel.

**2. `ui/navigation/ShopListNavHost.kt`**

* Collecter la position : `val positionBarre by viewModel.positionBarre.collectAsStateWithLifecycle()`.
* `composable(Routes.LISTE)` :
  * passer `positionBarre = positionBarre` à `TodoListScreen` ;
  * ne plus passer `onImporterCsv`, `onSauvegardeComplete`, `etatRestauration`, `onLireFichierSauvegarde`, `onConfirmerRestauration`, `onAnnulerRestauration`, `onConsommerErreurRestauration` (paramètres supprimés).
* `composable(Routes.OPTIONS)` :
  * passer `positionBarre = positionBarre` et `onChangerPositionBarre = viewModel::setPositionBarre` à `OptionsScreen` ;
  * ajouter le retour à la liste :

```kotlin
LaunchedEffect(Unit) {
    viewModel.operationReussie.collect {
        navController.popBackStack(Routes.LISTE, inclusive = false)
    }
}
```

**3. `ui/screens/liste/TodoListTopBar.kt`**

* Supprimer les `DropdownMenuItem` « Importer une liste », « Sauvegarde complète » et « Restaurer une sauvegarde », et les paramètres devenus inutiles `onOuvrirImport`, `onSauvegardeComplete`, `onRestaurerSauvegarde`.
* Ajouter le paramètre `enBas: Boolean = false` et l'utiliser uniquement pour les marges système de la `TopAppBar` :

```kotlin
TopAppBar(
    // ... paramètres existants inchangés ...
    windowInsets = if (enBas) {
        WindowInsets.navigationBars.union(WindowInsets.ime)
    } else {
        TopAppBarDefaults.windowInsets
    }
)
```

  En bas, la barre ne doit pas réserver la hauteur de la barre d'état (sinon un grand vide apparaît au-dessus du titre) ; elle réserve la barre de navigation et, quand il est ouvert, le clavier. Aucun autre changement dans la barre (contenu, couleurs, icônes, comportements).

**4. `ui/screens/liste/TodoListScreen.kt`**

* Nouveau paramètre `positionBarre: PositionBarre`.
* Supprimer les paramètres `onImporterCsv`, `onSauvegardeComplete`, `etatRestauration`, `onLireFichierSauvegarde`, `onConfirmerRestauration`, `onAnnulerRestauration`, `onConsommerErreurRestauration`.
* Supprimer les états `afficherDialogImport` et `afficherDialogPreRestauration`, les trois lanceurs `launcherImport`, `launcherSauvegardeComplete`, `launcherRestauration`, le calcul du nom de fichier de sauvegarde, et les quatre dialogues « Import CSV », « Restaurer une sauvegarde », « Confirmer la restauration », « Restauration impossible ».
* Ne plus passer `onOuvrirImport`, `onSauvegardeComplete`, `onRestaurerSauvegarde` à `TodoListTopBar`.
* Placement de la barre :

```kotlin
val barre: @Composable () -> Unit = {
    TodoListTopBar(
        // ... paramètres existants ...
        enBas = positionBarre == PositionBarre.BAS
    )
}

Scaffold(
    topBar = { if (positionBarre == PositionBarre.HAUT) barre() },
    bottomBar = { if (positionBarre == PositionBarre.BAS) barre() },
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
) { innerPadding ->
    // contenu inchangé (LazyColumn avec .padding(innerPadding))
}
```

  Sans `topBar`, le `innerPadding` du `Scaffold` contient déjà la hauteur de la barre d'état : la liste commence juste dessous, sans autre réglage.
* Supprimer les imports devenus inutilisés, et seulement ceux-là.
* **Ne rien changer d'autre** (recherche, glisser-déposer, menu ⋮, dialogues produit et catégorie, `afficherDialogCategorie`, `onAjouterCategorie`).

**5. `ui/screens/options/OptionsScreen.kt`**

* Supprimer l'`enum` privée `PositionBarre` (déplacée dans `TodoViewModel.kt`, importer `com.jb.shoplist.PositionBarre`) et l'état local `positionBarre`.
* Nouveaux paramètres, placés après `onOuvrirCategories` :

```kotlin
positionBarre: PositionBarre,
onChangerPositionBarre: (PositionBarre) -> Unit,
```

* Les `SegmentedButton` utilisent `selected = positionBarre == PositionBarre.HAUT` (ou `BAS`) et `onClick = { onChangerPositionBarre(PositionBarre.HAUT) }` (ou `BAS`).
* Libellé corrigé : `"Position de la barre de titre et d'options"`.
* Ne rien changer d'autre.

**6. `app/src/androidTest/java/com/jb/shoplist/OptionsScreenTest.kt`**

* Adapter les tests existants : nouveaux paramètres, libellé corrigé.
* Le test de sélection Haut / Bas devient (l'état n'est plus local à l'écran) :
  * avec `positionBarre = PositionBarre.HAUT` : « Haut » sélectionné, « Bas » non sélectionné ;
  * avec `positionBarre = PositionBarre.BAS` : « Bas » sélectionné, « Haut » non sélectionné ;
  * toucher « Bas » appelle `onChangerPositionBarre(PositionBarre.BAS)`.

Ne pas modifier `CategoriesScreen`, `PartagerScreen`, `SaisieDialog`, `MainActivity`, les DAO, les entités ni `TodoDatabase`.

==========================
COMPOSANTS UI
==========================

* `Scaffold` (`topBar` / `bottomBar`)
* `TopAppBar` (paramètre `windowInsets`)
* `WindowInsets.navigationBars`, `WindowInsets.ime`, `union`, `TopAppBarDefaults.windowInsets`
* `SingleChoiceSegmentedButtonRow`, `SegmentedButton` (existants)
* `LaunchedEffect` (collecte de `operationReussie`)

==========================
CAS LIMITES
==========================

* Premier lancement après mise à jour : paramètre absent, barre en haut.
* Lancement avec « Bas » enregistré : la barre peut apparaître en haut une fraction de seconde avant la lecture du paramètre ; acceptable dans ce module.
* Clavier ouvert en recherche, position « Bas » : barre au-dessus du clavier, champ visible pendant la saisie.
* Fermeture de la recherche en touchant sous la liste (MODULE 05b), position « Bas » : le séparateur final est au-dessus de la barre ; toucher entre le séparateur et la barre ferme la recherche (inchangé dans le principe).
* Import réussi alors qu'un Toast d'erreur précédent est encore affiché : sans effet sur la navigation.
* Import ou restauration lancés depuis Options, puis l'utilisateur revient lui-même sur la liste avant la fin : `popBackStack(Routes.LISTE, inclusive = false)` est sans effet (la liste est déjà au sommet) ; le collecteur n'existe plus de toute façon.
* Restauration d'une sauvegarde antérieure à ce module : position remise à « Haut ».
* Restauration d'une sauvegarde faite en « Bas » : barre en bas au retour sur la liste.
* Rotation : application en portrait uniquement.

==========================
TESTS
==========================

Tests Compose (`./gradlew connectedAndroidTest`, appareil branché) : `OptionsScreenTest` adapté (voir ARCHITECTURE, point 6) ; `ProduitEditionDialogTest` inchangé et passant.

Tests unitaires : `./gradlew test`, `FormatageTest` inchangé et passant.

Tests manuels (faire d'abord une **sauvegarde complète** avant d'installer la nouvelle version) :

1. Menu principal : seulement « Partager la liste », « Tout cocher », « Options ».
2. Options : libellé « Position de la barre de titre et d'options », « Haut » sélectionné.
3. Choisir « Bas », revenir sur la liste : barre en bas, liste depuis le haut de l'écran, rien sous la barre d'état, barre au-dessus de la barre de navigation Android.
4. En « Bas » : loupe, saisir un terme : barre au-dessus du clavier ; croix et flèche fonctionnent ; toucher sous la liste ferme la recherche.
5. En « Bas » : ouvrir le menu hamburger (s'ouvre vers le haut), cocher / annuler un cochage, masquer / afficher les cochés, glisser-déposer un produit, supprimer un produit (snackbar au-dessus de la barre, « Annuler » fonctionne).
6. Fermer complètement l'application et la rouvrir : barre en bas.
7. Repasser en « Haut » : retour à l'affichage d'origine.
8. Depuis Options, importer un CSV valide : Toast « Importation réussie ! » et retour sur la liste. Avec un fichier en erreur ou en annulant le sélecteur : on reste sur Options.
9. En « Bas », sauvegarde complète depuis Options : on reste sur Options ; le fichier contient `"positionBarre"` avec la valeur `"BAS"`.
10. Repasser en « Haut », restaurer la sauvegarde de l'étape 9 : Toast « Restauration réussie », retour sur la liste, barre en bas.
11. Restaurer une sauvegarde antérieure à ce module : retour sur la liste, barre en haut.
12. Restaurer un fichier invalide : « Restauration impossible », on reste sur Options.

==========================
CRITÈRES D'ACCEPTATION
==========================

* compile, aucun nouveau warning, aucun crash ;
* menu principal réduit à « Partager la liste », « Tout cocher », « Options » ;
* aucun code d'import, de sauvegarde ou de restauration restant dans `TodoListScreen` et `TodoListTopBar` ;
* libellé corrigé ;
* position « Bas » conforme (barre en bas, liste depuis le haut, clavier géré), position « Haut » inchangée ;
* choix enregistré, conservé au redémarrage, sauvegardé et restauré ;
* retour à la liste après import ou restauration réussis, et seulement dans ces cas ;
* aucune migration Room ; anciennes sauvegardes restaurables ;
* `OptionsScreenTest`, `ProduitEditionDialogTest` et `FormatageTest` passent.

==========================
TRAVAIL ATTENDU DE GEMINI
==========================

1. `TodoViewModel.kt` : `enum PositionBarre`, `positionBarre`, `setPositionBarre`, `operationReussie` et ses deux émissions, clé `positionBarre` dans `construireJsonSauvegarde`. Compiler.
2. `OptionsScreen.kt` : suppression de l'`enum` et de l'état locaux, nouveaux paramètres, libellé corrigé. Compiler.
3. `TodoListTopBar.kt` : retrait des trois entrées et de leurs paramètres, paramètre `enBas` et `windowInsets`. Compiler.
4. `TodoListScreen.kt` : suppression du code d'import, de sauvegarde et de restauration et des paramètres associés, paramètre `positionBarre`, placement de la barre. Compiler.
5. `ShopListNavHost.kt` : collecte de `positionBarre`, paramètres des deux écrans, retour à la liste sur `operationReussie`. Compiler.
6. `OptionsScreenTest.kt` : adaptation. Compiler, lancer `./gradlew test` et `./gradlew connectedAndroidTest`.

Créer uniquement les classes nécessaires.

Ne pas modifier d'autres modules.

Ne pas refactoriser l'application.

Compiler après chaque étape.

À la fin, résumer :

• fichiers créés

• fichiers modifiés

• points restant à faire
