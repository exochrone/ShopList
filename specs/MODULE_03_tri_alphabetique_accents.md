# MODULE 03
Tri alphabétique respectant les lettres accentuées

==========================
OBJECTIF
==========================

Corriger le tri alphabétique des produits d'une catégorie pour que les noms commençant par une lettre accentuée (« Échalotes », « Épinards », « Œufs »…) soient classés avec la lettre de base correspondante, et non en fin de liste.

Cause du bug : dans `TodoViewModel.trierTachesParAlpha`, le tri est fait par :

```kotlin
.sortedBy { it.titre.lowercase() }
```

Ce tri compare les codes Unicode des caractères. Or « é » (U+00E9) a un code supérieur à « z » (U+007A) : tous les mots commençant par une lettre accentuée passent après « Z ».

Correction : trier avec un `java.text.Collator` configuré pour le français, qui applique l'ordre alphabétique usuel (lettres accentuées rangées avec leur lettre de base, casse ignorée au premier niveau de comparaison).

Ce que le module ne fait PAS :

* il ne modifie pas la recherche (déjà insensible aux accents via `sansAccents()` dans `TodoListScreen.kt`, à ne pas toucher) ;
* il ne modifie pas l'ordre des catégories (il n'existe pas de tri alphabétique des catégories) ;
* il ne modifie pas la règle d'unicité des noms de produits, l'import/export CSV, le partage, la sauvegarde/restauration ;
* il ne modifie ni le modèle de données ni la base Room ;
* il ne trie pas automatiquement : le tri reste déclenché uniquement par l'utilisateur.

Dépendances : aucune nouvelle dépendance. `java.text.Collator` fait partie du JDK et d'Android.

==========================
FONCTIONNALITÉS
==========================

L'utilisateur peut, comme aujourd'hui, trier alphabétiquement les produits d'une catégorie :

* depuis le menu ⋮ d'un en-tête de catégorie, entrée « Tri alphabétique » (`TodoListScreen.kt`) ;
* depuis l'écran « Gestion des catégories », bouton « Trier A→Z » (`CategoriesScreen.kt`).

Les deux points d'entrée appellent la même fonction `TodoViewModel.trierTachesParAlpha(nomCategorie)`. Seule cette fonction change. Aucun libellé de l'interface ne change.

==========================
ÉCRANS
==========================

Aucun écran nouveau ni modifié.

==========================
COMPORTEMENT
==========================

Après un tri alphabétique d'une catégorie, ses produits sont ordonnés selon l'ordre alphabétique français :

* les lettres accentuées sont rangées avec leur lettre de base : « Échalotes » et « Épinards » se placent parmi les mots en « E » ;
* majuscules et minuscules sont rangées ensemble : « ail » et « Ail » se placent au même endroit que « Ail » ;
* les ligatures sont rangées comme les lettres qu'elles représentent : « Œufs » se place comme « Oeufs » ;
* la comparaison porte uniquement sur le nom du produit (`titre`), pas sur la quantité.

Exemple attendu, catégorie contenant : `Tomates`, `Épinards`, `ail`, `Carottes`, `Échalotes`, `Œufs`, `Endives`, `Oignons`, `Zucchini`, `Ananas`

Résultat attendu :

```
ail
Ananas
Carottes
Échalotes
Endives
Épinards
Œufs
Oignons
Tomates
Zucchini
```

Le reste du comportement de `trierTachesParAlpha` est inchangé : seules les positions des produits de la catégorie triée sont modifiées, les produits des autres catégories gardent leur ordre, l'état coché et la quantité de chaque produit sont conservés, puis l'ordre global est enregistré via `reordonnerTaches`.

==========================
RÈGLES MÉTIER
==========================

* L'ordre de tri est l'ordre alphabétique français fourni par `Collator.getInstance(Locale.FRENCH)`, avec la force par défaut `Collator.TERTIARY`.
* Deux noms qui ne diffèrent que par un accent (« Pate » et « Pâte », autorisés car la règle d'unicité ne tient compte que de la casse et des espaces) sont tous deux rangés à la lettre « P », dans un ordre stable et déterministe défini par le `Collator` (la version sans accent en premier).
* Deux noms ne peuvent pas différer seulement par la casse (règle d'unicité existante) : ce cas n'a pas à être traité spécialement.

==========================
MODÈLE DE DONNÉES
==========================

Aucun changement. Pas de migration Room, pas de changement du format de sauvegarde JSON (version 2 inchangée), pas d'impact sur le CSV ni sur le partage.

==========================
ARCHITECTURE
==========================

Aucune classe à créer.

Fichiers à modifier :

1. `app/src/main/java/com/jb/shoplist/Formatage.kt`

   Ajouter une fonction pure, testable en test unitaire JVM :

   ```kotlin
   import java.text.Collator
   import java.util.Locale

   /**
    * Comparateur alphabétique français : lettres accentuées rangées avec
    * leur lettre de base, casse ignorée au premier niveau.
    * Une nouvelle instance de Collator est créée à chaque appel
    * (Collator n'est pas garanti thread-safe).
    */
   fun comparateurAlphabetique(): Comparator<String> {
       val collator = Collator.getInstance(Locale.FRENCH)
       collator.strength = Collator.TERTIARY
       return Comparator { a, b -> collator.compare(a, b) }
   }
   ```

   Utiliser `java.text.Collator` et non `android.icu.text.Collator`, pour que la fonction reste testable par les tests unitaires JVM (`app/src/test`).

2. `app/src/main/java/com/jb/shoplist/TodoViewModel.kt`, fonction `trierTachesParAlpha`

   Remplacer uniquement la ligne de tri :

   ```kotlin
   .sortedBy { it.titre.lowercase() }
   ```

   par :

   ```kotlin
   .sortedWith(compareBy(comparateurAlphabetique()) { it.titre })
   ```

   Le reste de la fonction (extraction, reconstruction de la liste, appel à `reordonnerTaches`) ne change pas.

3. `app/src/test/java/com/jb/shoplist/FormatageTest.kt`

   Ajouter les tests décrits dans la section TESTS.

==========================
COMPOSANTS UI
==========================

Aucun.

==========================
CAS LIMITES
==========================

* Catégorie vide : le tri ne fait rien, sans erreur (comportement actuel conservé).
* Catégorie à un seul produit : aucun changement visible.
* Produits cochés : triés avec les autres, comme aujourd'hui.
* Noms commençant par un chiffre (« 4 yaourts ») : rangés avant les lettres, comme le fait le `Collator`.
* Noms ne différant que par un accent (« Pate » / « Pâte ») : tous deux à la lettre « P », ordre déterministe.
* Lettres majuscules accentuées (« É », « È », « Ê », « À », « Ô »…) et minuscules accentuées : même traitement.
* Liste très longue : performance négligeable (une seule instance de `Collator` par tri).

==========================
TESTS
==========================

Tests unitaires à ajouter dans `FormatageTest.kt` :

1. `comparateurAlphabetique_rangeLesAccentsAvecLaLettreDeBase` : trier `listOf("Tomates", "Épinards", "Carottes", "Échalotes", "Endives", "Zucchini")` avec `sortedWith(comparateurAlphabetique())` donne `listOf("Carottes", "Échalotes", "Endives", "Épinards", "Tomates", "Zucchini")`.
2. `comparateurAlphabetique_ignoreLaCasse` : trier `listOf("banane", "Ananas", "carotte")` donne `listOf("Ananas", "banane", "carotte")`.
3. `comparateurAlphabetique_ligatureOe` : dans le tri de `listOf("Oignons", "Œufs", "Navets", "Poireaux")`, « Œufs » est placé après « Navets » et avant « Oignons ».
4. `comparateurAlphabetique_accentsMajusculesEtMinuscules` : « écrou », « Élan » et « Eau » sont tous placés avant « Fraises » et après « Dattes ».
5. `comparateurAlphabetique_motsNeDifferantQueParUnAccent` : le tri de `listOf("Pâte", "Pate", "Orange", "Radis")` place « Pate » et « Pâte » entre « Orange » et « Radis ».

Test manuel sur l'appareil :

* Créer dans une catégorie les produits de l'exemple de la section COMPORTEMENT, lancer « Tri alphabétique » depuis le menu ⋮, vérifier l'ordre attendu.
* Refaire le test avec « Trier A→Z » depuis « Gestion des catégories ».
* Vérifier que les autres catégories n'ont pas bougé et que quantités et états cochés sont conservés.

==========================
CRITÈRES D'ACCEPTATION
==========================

* compile
* aucun warning nouveau
* aucun crash
* les tests unitaires de `FormatageTest` passent (anciens et nouveaux)
* « Échalotes » et « Épinards » sont classés à la lettre E après un tri, depuis les deux points d'entrée
* l'ordre des autres catégories, les quantités et les états cochés sont inchangés après un tri

==========================
TRAVAIL ATTENDU DE GEMINI
==========================

1. Ajouter `comparateurAlphabetique()` dans `Formatage.kt`. Compiler.
2. Remplacer la ligne de tri dans `TodoViewModel.trierTachesParAlpha`. Compiler.
3. Ajouter les tests dans `FormatageTest.kt` et les exécuter (`./gradlew test`).

Contraintes :

* Créer uniquement les classes nécessaires (aucune ici).
* Ne pas modifier d'autres modules ni d'autres fonctions que celles citées. En particulier, ne pas toucher à `sansAccents()` dans `TodoListScreen.kt`, à la recherche, à la règle d'unicité, au CSV, au partage ni à la sauvegarde.
* Ne pas refactoriser l'application.
* Compiler après chaque étape.

À la fin, résumer :

* fichiers créés
* fichiers modifiés
* points restant à faire
