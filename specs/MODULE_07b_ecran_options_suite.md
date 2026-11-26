# MODULE 07b
Écran « Options » : suite (menu allégé, import, sauvegarde, restauration, position de la barre)

==========================
OBJECTIF
==========================

Suite du MODULE 07 (écran « Options », implémenté et validé).

Ce module :

1. **retire du menu principal** les entrées « Gérer les catégories » et « Renommer la liste », désormais accessibles depuis l'écran Options ;
2. **ajoute à l'écran Options** trois boutons qui reproduisent à l'identique les entrées du menu principal du même nom :
   * « Importer une liste »,
   * « Sauvegarde complète »,
   * « Restaurer une sauvegarde » ;
3. **ajoute à l'écran Options** une option « Position de la barre de titre et d'option » avec deux choix « Haut » / « Bas », « Haut » par défaut. Cette option n'a **aucun effet** pour l'instant et **n'est pas enregistrée**.

Ce que le module ne fait pas :

* il ne retire pas du menu principal « Importer une liste », « Sauvegarde complète » et « Restaurer une sauvegarde » : ces entrées restent dans le menu le temps des tests ;
* il ne modifie pas le fonctionnement de l'import CSV, de la sauvegarde ni de la restauration (fonctions du ViewModel, formats de fichier, textes des dialogues) ;
* il ne déplace pas la barre du haut et n'enregistre pas le choix « Haut » / « Bas » (ni en base, ni dans la sauvegarde JSON) ;
* il ne crée pas de composant partagé : le code de l'import, de la sauvegarde et de la restauration est **copié** de `TodoListScreen` dans `OptionsScreen` (choix délibéré, voir ARCHITECTURE) ;
* il ne modifie ni la base Room, ni la sauvegarde JSON, ni le CSV, ni le partage, ni `CategoriesScreen`.

Modules dont il dépend : MODULE 07 (écran Options).

==========================
FONCTIONNALITÉS
==========================

L'utilisateur peut, depuis l'écran Options :

* renommer la liste (inchangé, MODULE 07) ;
* choisir la position de la barre de titre (« Haut » / « Bas »), sans effet pour l'instant ;
* ouvrir la gestion des catégories (inchangé, MODULE 07) ;
* importer une liste CSV ;
* faire une sauvegarde complète ;
* restaurer une sauvegarde.

Depuis le menu principal, il ne peut plus ni ouvrir la gestion des catégories, ni renommer la liste.

==========================
ÉCRANS
==========================

**1. Menu principal (`TodoListTopBar`)**

Entrées, dans cet ordre :

* Partager la liste
* Importer une liste
* Sauvegarde complète
* Restaurer une sauvegarde
* Tout cocher
* Options

(« Gérer les catégories » et « Renommer la liste » sont retirées ; l'ordre des autres entrées est inchangé.)

**2. Écran « Options » (`OptionsScreen`)**

Contenu, de haut en bas :

1. Barre du haut : inchangée (flèche retour, « Options »).
2. Zone « Nom de la liste » : inchangée.
3. **Option « Position de la barre de titre et d'option »** (nouveau) :
   * libellé « Position de la barre de titre et d'option » (12 sp, `MaterialTheme.colorScheme.onSurfaceVariant`), même style que le libellé « Nom de la liste » ;
   * en dessous, un `SingleChoiceSegmentedButtonRow` pleine largeur, avec deux `SegmentedButton` : « Haut » (à gauche) et « Bas » (à droite) ;
   * « Haut » est sélectionné à l'ouverture de l'écran.
4. Bouton « Gérer les catégories » : inchangé.
5. **Bouton « Importer une liste »** (nouveau) : `OutlinedButton` pleine largeur.
6. **Bouton « Sauvegarde complète »** (nouveau) : `OutlinedButton` pleine largeur.
7. **Bouton « Restaurer une sauvegarde »** (nouveau) : `OutlinedButton` pleine largeur.
8. Zone d'information « shopList v… » calée en bas : inchangée.

Espacements : `16.dp` entre la zone « Nom de la liste », l'option de position et le premier bouton ; `8.dp` entre deux boutons consécutifs.

Navigation : inchangée (Options ↔ Gestion des catégories ↔ Liste). Les sélecteurs de fichiers Android s'ouvrent par-dessus l'écran Options et y reviennent.

==========================
COMPORTEMENT
==========================

1. **Retrait des entrées du menu**

   * « Gérer les catégories » et « Renommer la liste » n'apparaissent plus dans le menu principal.
   * Le dialogue « Renommer la liste » de `TodoListScreen` n'a plus de moyen d'être ouvert : il est supprimé avec son état (voir ARCHITECTURE). Le renommage se fait uniquement depuis Options.

2. **« Importer une liste » depuis Options** : déroulement identique à l'entrée du menu :

   * ouverture du dialogue d'explication « Import CSV », avec **exactement le même texte** que dans `TodoListScreen` (y compris la mention de deux colonnes, inchangée), et les boutons « Valider » / « Annuler » ;
   * « Valider » ouvre le sélecteur de fichier avec les mêmes types : `arrayOf("text/csv", "text/comma-separated-values", "text/plain")` ;
   * le fichier choisi est passé à `TodoViewModel.importerDepuisCsv` ; le Toast de résultat (« Importation réussie ! » ou erreur) s'affiche comme aujourd'hui (via `evenements`, collecté par `TodoApp`) ;
   * l'utilisateur reste sur l'écran Options.

3. **« Sauvegarde complète » depuis Options** : identique au menu :

   * ouverture directe du sélecteur de création de fichier (`CreateDocument("application/json")`) avec le nom `shopList-Backup-<aaMMjj>.json`, calculé de la même façon que dans `TodoListScreen` ;
   * le fichier choisi est passé à `TodoViewModel.sauvegardeComplete` ; Toast « Sauvegarde réussie » ou erreur ;
   * l'utilisateur reste sur l'écran Options.

4. **« Restaurer une sauvegarde » depuis Options** : identique au menu, en trois temps :

   1. dialogue d'avertissement « Restaurer une sauvegarde » (même texte, boutons « Choisir un fichier » / « Annuler ») ;
   2. « Choisir un fichier » ouvre le sélecteur (`OpenDocument`, type `arrayOf("application/json")`), le fichier est passé à `TodoViewModel.lireFichierSauvegarde` ;
   3. selon `etatRestauration` :
      * `EtatRestauration.FichierLu` : dialogue « Confirmer la restauration » (même texte et mêmes boutons « Restaurer » en rouge / « Annuler »), branché sur `confirmerRestauration` / `annulerRestauration` ;
      * `EtatRestauration.Erreur` : dialogue « Restauration impossible » (même texte, bouton « OK »), branché sur `consommerErreurRestauration`.
   * Après une restauration réussie, l'utilisateur reste sur l'écran Options ; le nom de la liste affiché est celui de la sauvegarde ; Toast « Restauration réussie ».

5. **Option « Position de la barre de titre et d'option »**

   * Toucher « Haut » ou « Bas » sélectionne ce choix ; l'autre est désélectionné.
   * Aucune autre action : la barre du haut ne bouge pas, rien n'est enregistré.
   * Le choix est un état local de l'écran : en quittant Options puis en y revenant, « Haut » est de nouveau sélectionné.

==========================
RÈGLES MÉTIER
==========================

* Les actions « Importer une liste », « Sauvegarde complète » et « Restaurer une sauvegarde » d'Options appellent les mêmes fonctions du ViewModel que le menu, avec les mêmes dialogues et les mêmes textes.
* Le choix « Haut » / « Bas » n'a aucun effet et n'est jamais enregistré.
* « Haut » est la valeur par défaut.

==========================
MODÈLE DE DONNÉES
==========================

Aucune modification de la base Room (version 4 conservée, aucune migration), de la sauvegarde JSON, du CSV ou du partage. Aucun nouvel état dans `TodoViewModel`.

Nouvel état local dans `OptionsScreen.kt` :

```kotlin
private enum class PositionBarre { HAUT, BAS }

// dans OptionsScreen :
var positionBarre by remember { mutableStateOf(PositionBarre.HAUT) }
var afficherDialogImport by remember { mutableStateOf(false) }
var afficherDialogPreRestauration by remember { mutableStateOf(false) }
```

==========================
ARCHITECTURE
==========================

Aucune classe nouvelle hors l'`enum` privée ci-dessus. Fichiers modifiés :

**1. `ui/screens/options/OptionsScreen.kt`**

* Nouvelle signature (paramètres ajoutés après `onOuvrirCategories`) :

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    titreListe: String,
    versionApp: String,
    onModifierTitreListe: (String) -> Unit,
    onOuvrirCategories: () -> Unit,
    onImporterCsv: (Uri) -> Unit,
    onSauvegardeComplete: (Uri) -> Unit,
    etatRestauration: EtatRestauration,
    onLireFichierSauvegarde: (Uri) -> Unit,
    onConfirmerRestauration: () -> Unit,
    onAnnulerRestauration: () -> Unit,
    onConsommerErreurRestauration: () -> Unit,
    onRetour: () -> Unit
)
```

* **Copier depuis `TodoListScreen.kt`**, sans modification de texte ni de comportement :
  * les trois `rememberLauncherForActivityResult` : `launcherImport` (`OpenDocument`), `launcherSauvegardeComplete` (`CreateDocument("application/json")`), `launcherRestauration` (`OpenDocument`), branchés sur les nouveaux callbacks ;
  * le calcul du nom de fichier de sauvegarde (`"shopList-Backup-${SimpleDateFormat("yyMMdd", Locale.getDefault()).format(Date())}.json"`) ;
  * le dialogue `AlertDialog` « Import CSV » (bloc `if (afficherDialogImport)`) ;
  * les trois dialogues de restauration (« Restaurer une sauvegarde », « Confirmer la restauration », « Restauration impossible »), pilotés par `afficherDialogPreRestauration` et `etatRestauration`.
* Ajouter l'option de position (libellé + `SingleChoiceSegmentedButtonRow`) et les trois `OutlinedButton`, dans l'ordre de la section ÉCRANS.

Exemple pour l'option de position :

```kotlin
Text(
    text = "Position de la barre de titre et d'option",
    fontSize = 12.sp,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
Spacer(modifier = Modifier.height(4.dp))
SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    SegmentedButton(
        selected = positionBarre == PositionBarre.HAUT,
        onClick = { positionBarre = PositionBarre.HAUT },
        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
    ) { Text("Haut") }
    SegmentedButton(
        selected = positionBarre == PositionBarre.BAS,
        onClick = { positionBarre = PositionBarre.BAS },
        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
    ) { Text("Bas") }
}
```

  `SingleChoiceSegmentedButtonRow` et `SegmentedButton` sont disponibles dans Material 3 de la BOM Compose 2024.12.01 ; l'écran a déjà `@OptIn(ExperimentalMaterial3Api::class)`. **Ne pas changer la version de la BOM Compose.**

**2. `ui/navigation/ShopListNavHost.kt`**

* `composable(Routes.OPTIONS)` : passer les nouveaux paramètres :

```kotlin
onImporterCsv = viewModel::importerDepuisCsv,
onSauvegardeComplete = viewModel::sauvegardeComplete,
etatRestauration = etatRestauration,
onLireFichierSauvegarde = viewModel::lireFichierSauvegarde,
onConfirmerRestauration = viewModel::confirmerRestauration,
onAnnulerRestauration = viewModel::annulerRestauration,
onConsommerErreurRestauration = viewModel::consommerErreurRestauration,
```

  (`etatRestauration` est déjà un paramètre de `ShopListNavHost`.)
* `composable(Routes.LISTE)` : ne plus passer `onOuvrirCategories` ni `onModifierTitreListe` à `TodoListScreen` (paramètres supprimés, voir ci-dessous).

**3. `ui/screens/liste/TodoListTopBar.kt`**

* Supprimer les `DropdownMenuItem` « Gérer les catégories » et « Renommer la liste ».
* Supprimer les paramètres devenus inutiles `onOuvrirCategories` et `onRenommerListe`.

**4. `ui/screens/liste/TodoListScreen.kt`**

* Supprimer les paramètres devenus inutiles `onOuvrirCategories` et `onModifierTitreListe`, et ne plus passer `onOuvrirCategories` ni `onRenommerListe` à `TodoListTopBar`.
* Supprimer l'état `afficherDialogRenommer` et le bloc `if (afficherDialogRenommer) { SaisieDialog(titre = "Renommer la liste", …) }`, devenus inaccessibles.
* **Ne rien changer d'autre** : l'import, la sauvegarde, la restauration, et le reste de l'écran restent tels quels (ils servent toujours aux entrées du menu). Ne pas toucher à l'état `afficherDialogCategorie` ni à `onAjouterCategorie`.

**5. `app/src/androidTest/java/com/jb/shoplist/OptionsScreenTest.kt`**

* Adapter les 3 tests existants à la nouvelle signature (callbacks vides, `etatRestauration = EtatRestauration.Inactif`).
* Ajouter les tests de la section TESTS.

Ne pas modifier `TodoViewModel`, `CategoriesScreen`, `PartagerScreen`, `SaisieDialog`, les DAO ni la base. Ne pas créer de composant partagé pour l'import ou la restauration.

==========================
COMPOSANTS UI
==========================

* `SingleChoiceSegmentedButtonRow`, `SegmentedButton`, `SegmentedButtonDefaults.itemShape`
* `OutlinedButton` (×3)
* `AlertDialog` (×4, copiés de `TodoListScreen`)
* `rememberLauncherForActivityResult` (`OpenDocument`, `CreateDocument`)

==========================
CAS LIMITES
==========================

* Annuler le sélecteur de fichier (import, sauvegarde ou restauration) : retour sur Options, rien ne se passe (`uri == null`).
* Fichier de restauration invalide : dialogue « Restauration impossible » affiché sur Options.
* Restauration réussie : le nom de la liste de la zone « Nom de la liste » est mis à jour aussitôt (lu depuis `uiState.titreListe`).
* Un seul écran est affiché à la fois : les dialogues de restauration copiés dans Options et ceux de `TodoListScreen` ne s'affichent jamais en même temps.
* Retour Android avec un dialogue ouvert : ferme le dialogue (comportement des `AlertDialog`, identique au menu), reste sur Options.
* Écran Options plus haut que l'écran (petit téléphone) : non traité dans ce module (le Samsung Galaxy S23+ affiche tout le contenu).
* Ancienne habitude « Renommer la liste » ou « Gérer les catégories » dans le menu : entrées absentes, passer par Options.

==========================
TESTS
==========================

Tests Compose dans `OptionsScreenTest.kt` (à lancer avec `./gradlew connectedAndroidTest`, appareil branché) :

* les 3 tests existants, adaptés à la nouvelle signature, passent toujours ;
* les boutons « Importer une liste », « Sauvegarde complète » et « Restaurer une sauvegarde » et le libellé « Position de la barre de titre et d'option » sont affichés ;
* « Haut » est sélectionné à l'ouverture (`assertIsSelected()`), « Bas » ne l'est pas ; après un toucher sur « Bas », « Bas » est sélectionné et « Haut » ne l'est plus ;
* toucher « Importer une liste » affiche le dialogue « Import CSV » ;
* toucher « Restaurer une sauvegarde » affiche le bouton « Choisir un fichier » (ne pas chercher le titre du dialogue : il porte le même texte que le bouton, `onNodeWithText` trouverait deux nœuds) ;
* avec `etatRestauration = EtatRestauration.Erreur("Fichier invalide")`, le dialogue « Restauration impossible » et le texte « Fichier invalide » sont affichés ; « OK » appelle `onConsommerErreurRestauration`.

Tests manuels :

1. Menu principal : « Gérer les catégories » et « Renommer la liste » ont disparu ; les autres entrées sont là, « Options » en dernier.
2. Options : option de position (« Haut » sélectionné), puis les 4 boutons dans l'ordre prévu, version en bas.
3. Toucher « Bas » : sélectionné, aucun autre effet. Quitter Options et revenir : « Haut » est sélectionné.
4. Importer un CSV depuis Options : même dialogue que depuis le menu, Toast de réussite, produits importés visibles au retour sur la liste.
5. Sauvegarde complète depuis Options : nom de fichier `shopList-Backup-<aaMMjj>.json`, Toast « Sauvegarde réussie ».
6. Restaurer la sauvegarde de l'étape 5 depuis Options : avertissement, résumé, « Restaurer », Toast « Restauration réussie », reste sur Options.
7. Restaurer un fichier non JSON depuis Options : « Restauration impossible ».
8. Refaire 4, 5 et 6 depuis le menu principal : comportement inchangé.

==========================
CRITÈRES D'ACCEPTATION
==========================

* compile, aucun nouveau warning, aucun crash ;
* « Gérer les catégories » et « Renommer la liste » ne sont plus dans le menu principal, sans code mort laissé derrière (paramètres et dialogue de renommage supprimés) ;
* Options affiche l'option de position et les trois nouveaux boutons ;
* import, sauvegarde et restauration depuis Options se comportent exactement comme depuis le menu ;
* l'option de position n'a aucun effet et n'est pas enregistrée ;
* `OptionsScreenTest` et `ProduitEditionDialogTest` passent.

==========================
TRAVAIL ATTENDU DE GEMINI
==========================

1. `OptionsScreen.kt` : nouvelle signature, option de position, trois boutons, copie des lanceurs et des dialogues depuis `TodoListScreen`. Compiler.
2. `ShopListNavHost.kt` : nouveaux paramètres de `OptionsScreen`. Compiler.
3. `TodoListTopBar.kt` puis `TodoListScreen.kt` : retrait des deux entrées du menu, des paramètres et du dialogue de renommage devenus inutiles ; mise à jour de `ShopListNavHost.kt` en conséquence. Compiler.
4. `OptionsScreenTest.kt` : adaptation et nouveaux tests. Compiler, lancer `./gradlew connectedAndroidTest`.

Créer uniquement les classes nécessaires.

Ne pas modifier d'autres modules.

Ne pas refactoriser l'application.

Compiler après chaque étape.

À la fin, résumer :

• fichiers créés

• fichiers modifiés

• points restant à faire
