# MODULE 01b
Affichage de la quantité dans la liste (complément au MODULE 01)

============================================================
OBJECTIF
============================================================

Modifier l'affichage de la quantité sur les lignes produit de l'écran principal :

* la quantité suit directement le nom du produit (elle n'est plus alignée à droite) ;
* elle est séparée du nom par un petit espace ;
* elle est plus petite (12.sp au lieu de 14.sp) ;
* elle prend la couleur de la catégorie du produit ;
* elle est masquée quand le produit est coché.

Ce document **remplace**, dans `MODULE_01_quantites.md`, la section COMPORTEMENT « Disposition d'une ligne produit », la ligne « Quantité » de l'Écran 1 et le composant « `Layout` personnalisé dans `TacheItem` ». Le reste du MODULE 01 est inchangé.

Ce que ce module ne fait PAS :

* Pas de changement de la taille ni de la couleur du nom du produit (14.sp, couleurs actuelles).
* Pas de changement du dialogue « Modifier le produit » : la quantité d'un produit coché y reste visible et modifiable.
* Pas de changement du partage, du CSV, de la sauvegarde ni de la restauration : la quantité y figure toujours, que le produit soit coché ou non.
* Aucune modification des données : masquer la quantité n'efface rien en base.

Modules dont il dépend : MODULE 01 (Quantités des produits), déjà implémenté.

============================================================
FONCTIONNALITÉS
============================================================

L'utilisateur voit :

* pour un produit **non coché** avec une quantité : le nom, un petit espace, puis la quantité en plus petit, dans la couleur de la catégorie ;
* pour un produit **non coché** sans quantité : le nom seul (inchangé) ;
* pour un produit **coché** : le nom seul, grisé et barré (inchangé), sans quantité, même si une quantité est enregistrée.

============================================================
ÉCRANS
============================================================

Nom : Liste principale (`TodoListScreen`), ligne produit (`TacheItem`).

Objectif : rendre la quantité plus discrète et plus lisible, collée au nom.

Contenu d'une ligne produit :

* Case à cocher : inchangée.
* Texte du produit : un seul composant `Text`, qui contient :
  * le nom du produit (14.sp, couleur et barré actuels) ;
  * si le produit n'est pas coché et que la quantité n'est pas vide : un espace, puis la quantité (12.sp, couleur de la catégorie, non barrée).

Exemple (produit non coché de la catégorie verte « Légumes ») :

```
☐ Aubergines 3
☐ Farine de blé T65 bio 1 kg
```
« 3 » et « 1 kg » apparaissent en vert et en plus petit.

Actions possibles : inchangées (toucher la case, toucher le texte pour ouvrir le dialogue, appui long pour glisser-déposer).

Navigation : inchangée.

============================================================
COMPORTEMENT
============================================================

------------------------------------------------------------
Construction du texte
------------------------------------------------------------

Le texte de la ligne est un `AnnotatedString` construit avec `buildAnnotatedString` :

1. Ajouter le nom du produit, avec le style actuel du `Text` (14.sp, couleur `tacheNonFaite` ou `tacheFaite`, barré si coché).
2. Si `!tache.faite && tache.quantite.isNotBlank()` :
   * ajouter le séparateur `" "` (espace demi-cadratin, environ un demi-caractère : « un peu d'espace », plus visible qu'un espace simple) ;
   * ajouter la quantité dans un `withStyle(SpanStyle(fontSize = 12.sp, color = couleurQuantite))`, en remplaçant ses espaces par des espaces insécables (`" "`) pour qu'elle ne soit jamais coupée en deux (« 1 kg » reste d'un bloc).

`couleurQuantite` = la couleur de la catégorie reçue par `TacheItem` (paramètre existant `couleurCase`). Si cette couleur vaut `Color.Unspecified`, utiliser `ShopListTheme.customColors.tacheNonFaite`.

------------------------------------------------------------
Retour à la ligne
------------------------------------------------------------

* Nom et quantité forment un seul texte : quand ils ne tiennent pas sur une ligne, le texte passe à la ligne naturellement.
* La quantité vient toujours juste après le dernier mot du nom ; si elle ne tient pas en fin de ligne, elle passe en début de ligne suivante, d'un seul bloc.
* Il n'y a plus d'alignement à droite ni de disposition « empilée ».

------------------------------------------------------------
Produit coché
------------------------------------------------------------

* La quantité n'est pas affichée. Le nom est affiché comme aujourd'hui (grisé, barré).
* Décocher le produit réaffiche sa quantité (elle n'a jamais été effacée).

------------------------------------------------------------
Zone cliquable
------------------------------------------------------------

* Toute la zone de texte (nom et quantité) reste cliquable et ouvre « Modifier le produit », comme aujourd'hui.

============================================================
RÈGLES MÉTIER
============================================================

1. La quantité est affichée seulement si le produit n'est pas coché et si elle n'est pas vide.
2. Masquer la quantité d'un produit coché ne modifie jamais la donnée en base.
3. La quantité affichée utilise la couleur de la catégorie du produit.
4. Taille de police : nom 14.sp (inchangé), quantité 12.sp.
5. Un changement de couleur de catégorie se répercute immédiatement sur la couleur des quantités de ses produits (l'état est déjà observé, aucun code spécifique).

============================================================
MODÈLE DE DONNÉES
============================================================

Aucun changement (entité, base, DAO, JSON et CSV inchangés).

UI State : inchangé.

============================================================
ARCHITECTURE
============================================================

Fichier à modifier : `ui/components/TacheItem.kt` uniquement.

* Supprimer le `Layout` personnalisé (et le branchement `if (tache.quantite.isBlank()) … else Layout(…)`) ainsi que les imports devenus inutiles (`Layout`, `AlignmentLine`, `FirstBaseline`, `TextAlign`, `kotlin.math.max`…).
* Les remplacer par un seul `Text(text = annotatedString, …)` avec les paramètres actuels du nom (14.sp, couleur, `textDecoration`), dans la `Box` cliquable existante (`weight(1f)`, `clickable { onClickTache() }`).
* Extraire la construction du texte dans une fonction privée du même fichier :

```kotlin
private fun libelleProduit(
    titre: String,
    quantite: String,
    faite: Boolean,
    couleurQuantite: Color
): AnnotatedString = buildAnnotatedString {
    append(titre)
    if (!faite && quantite.isNotBlank()) {
        append(" ")
        withStyle(SpanStyle(fontSize = 12.sp, color = couleurQuantite)) {
            append(quantite.trim().replace(' ', ' '))
        }
    }
}
```

* Signature publique de `TacheItem` : inchangée (`couleurCase` sert déjà à transmettre la couleur de la catégorie depuis `TodoListScreen`).

Fichiers à ne pas modifier : tous les autres, y compris `TodoListScreen.kt`, `ProduitEditionDialog.kt`, `PartagerScreen.kt`, `TodoViewModel.kt`.

============================================================
COMPOSANTS UI
============================================================

* `Text` avec `AnnotatedString`.
* `buildAnnotatedString`, `withStyle`, `SpanStyle`.

============================================================
CAS LIMITES
============================================================

* **Quantité vide** : nom seul, sans séparateur.
* **Quantité avec espaces** (« 2 boîtes ») : affichée d'un bloc, jamais coupée.
* **Nom très long** : le texte passe à la ligne, la quantité suit le dernier mot.
* **Quantité très longue** (plus large que la ligne) : passe à la ligne ; le système la coupe en dernier recours, sans plantage.
* **Catégorie blanche** (couleur par défaut) : quantité en blanc.
* **Produit coché** : aucune quantité affichée ; décoché : quantité réaffichée.
* **Recherche active** : affichage identique (règles inchangées).
* **Glisser-déposer** : inchangé.

============================================================
TESTS
============================================================

Pas de nouveau test automatisé obligatoire. Vérifier que `FormatageTest` et le reste des tests existants passent toujours (`./gradlew test`).

Vérifications manuelles : voir les critères d'acceptation.

============================================================
CRITÈRES D'ACCEPTATION
============================================================

* Le projet compile, sans nouveau warning.
* Aucun crash à l'ouverture, avec ou sans produits ayant une quantité.
* La quantité s'affiche juste après le nom, séparée par un petit espace.
* La quantité est en 12.sp, le nom reste en 14.sp.
* La quantité a la couleur de la catégorie ; changer la couleur de la catégorie change celle de la quantité.
* Un nom long fait passer le texte à la ligne, la quantité suit le dernier mot, sans chevauchement.
* « 2 boîtes » n'est jamais coupé entre « 2 » et « boîtes ».
* Produit coché : quantité absente ; après décochage, quantité de nouveau visible et identique.
* Le dialogue « Modifier le produit » d'un produit coché montre toujours sa quantité.
* Le partage d'un produit coché (option « Afficher les éléments cochés ») inclut toujours sa quantité.

============================================================
TRAVAIL ATTENDU DE GEMINI
============================================================

Modifier uniquement `ui/components/TacheItem.kt`.

Ne créer aucune classe.

Ne pas modifier d'autres fichiers ni refactoriser l'application.

Compiler (`./gradlew app:assembleDebug`) et lancer `./gradlew test`.

À la fin, résumer :

• fichiers créés

• fichiers modifiés

• points restant à faire
