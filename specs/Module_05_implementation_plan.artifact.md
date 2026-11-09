# Plan d'implémentation - MODULE 05 : Fermeture automatique de la recherche et bouton œil en recherche

Permettre la fermeture automatique de la recherche (avec masquage du clavier) lors d'une interaction utilisateur (ouverture de produit, repli de catégorie, action du menu d'en-tête), faire défiler la liste vers l'élément cible après fermeture, et masquer le bouton œil en mode recherche.

## User Review Required

> [!NOTE]
> - Le cochage/décochage d'un produit ne ferme pas la recherche.
> - L'ouverture/fermeture du menu d'en-tête sans sélection d'action ne ferme pas la recherche.
> - Le bouton œil est totalement masqué en mode recherche (les produits cochés y sont toujours inclus).

## Proposed Changes

### TopBar UI

#### [MODIFY] [TodoListTopBar.kt](file:///home/julien/AndroidStudioProjects/shopList/app/src/main/java/com/jb/shoplist/ui/screens/liste/TodoListTopBar.kt)
- Dans la section `actions` lorsque `isSearchMode == true`, retirer le bouton `IconButton` gérant l'affichage des tâches faites (bouton œil).

---

### Écran Principal & Logique de Défilement

#### [MODIFY] [TodoListScreen.kt](file:///home/julien/AndroidStudioProjects/shopList/app/src/main/java/com/jb/shoplist/ui/screens/liste/TodoListScreen.kt)
- Ajouter la `data class CibleDefilement(val cle: String, val cleRepli: String?)` privée.
- Ajouter la fonction helper `quitterRecherche(cible: CibleDefilement? = null)` qui réinitialise `modeRecherche`, efface le terme de recherche, ferme le clavier/retire le focus et définit la `cibleDefilement`.
- Utiliser `quitterRecherche()` dans le `BackHandler` et `onToggleSearchMode`.
- Ajouter un `LaunchedEffect(lignes, cibleDefilement)` après `lazyListState` pour faire défiler vers l'élément cible (ou son repli) si nécessaire.
- Brancher les 3 déclencheurs pour appeler `quitterRecherche(cible)` avant leur action principale :
  1. Toucher un en-tête (`clickable`).
  2. Sélectionner l'une des 5 actions du menu ⋮ de catégorie.
  3. Toucher un produit (`onClickTache`).

## Verification Plan

### Automated Tests
- Exécuter les tests unitaires : `./gradlew :app:testDebugUnitTest`
- Vérifier la compilation : `./gradlew :app:assembleDebug`

### Manual Verification
1. Saisir une recherche, toucher un produit : la recherche se ferme, le clavier se ferme, le dialogue s'ouvre, le produit cible reste visible.
2. Saisir une recherche, toucher un en-tête de catégorie : la recherche se ferme, la catégorie se replie/déplie et reste visible.
3. Saisir une recherche, ouvrir le menu ⋮ puis choisir une action (ex. Tri alphabétique, Couleur...) : la recherche se ferme et l'action s'exécute.
4. Saisir une recherche, cocher/décocher des éléments : la recherche reste ouverte.
5. Vérifier qu'en mode recherche, le bouton œil n'apparaît plus dans la barre du haut.
