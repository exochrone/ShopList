# MODULE 07e
Écran « Options » : lignes de séparation et version 2026.05

==========================
OBJECTIF
==========================

1. Ajouter des lignes de séparation horizontales entre les éléments de l'écran Options, pour regrouper visuellement les réglages.
2. Passer la version de l'application en **2026.05**.

Ce que le module ne fait pas :

* il ne modifie ni l'ordre, ni les libellés, ni le comportement des éléments de l'écran Options ;
* il n'ajoute **pas** de ligne entre « Sauvegarde complète » et « Restaurer une sauvegarde » (ces deux boutons restent groupés) ;
* il ne modifie aucun autre écran, ni le ViewModel, ni la base Room, ni la sauvegarde JSON (le champ `appVersion` des sauvegardes vaudra automatiquement `"2026.05"`, il est déjà lu depuis `BuildConfig.VERSION_NAME`), ni le CSV.

Modules dont il dépend : MODULES 07, 07b, 07c (écran Options).

==========================
FONCTIONNALITÉS
==========================

L'utilisateur voit l'écran Options découpé en groupes séparés par des lignes, et la version « shopList v2026.05 » en bas de l'écran.

==========================
ÉCRANS
==========================

Nom : écran « Options » (`OptionsScreen`, route `"options"`).

Contenu, de haut en bas (les lignes ajoutées sont en gras) :

1. Zone « Nom de la liste »
2. **Ligne de séparation**
3. Option « Position de la barre de titre et d'options » (libellé + boutons Haut / Bas)
4. **Ligne de séparation**
5. Bouton « Gérer les catégories »
6. **Ligne de séparation**
7. Bouton « Importer une liste »
8. **Ligne de séparation**
9. Bouton « Sauvegarde complète »
10. Bouton « Restaurer une sauvegarde » (8 dp sous « Sauvegarde complète », comme aujourd'hui, sans ligne)
11. **Ligne de séparation**
12. Espace libre, puis « shopList v2026.05 » calé en bas (inchangé)

Style d'une ligne de séparation (identique au séparateur de fin de liste de l'écran principal) :

```kotlin
HorizontalDivider(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
    thickness = 1.dp,
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
)
```

Chaque ligne **remplace** l'espacement vertical actuel (`Spacer`) à son emplacement ; sa marge verticale de 12 dp fait l'espacement. Les autres espacements sont inchangés (16 dp en haut de l'écran, 4 dp entre un libellé et sa valeur, 8 dp entre « Sauvegarde complète » et « Restaurer une sauvegarde », 16 dp sous la version).

Navigation : inchangée.

==========================
COMPORTEMENT
==========================

Aucun changement de comportement : les lignes sont purement décoratives (non cliquables). La zone « Nom de la liste » reste cliquable sur la même surface qu'aujourd'hui.

La version affichée est lue depuis `BuildConfig.VERSION_NAME` (inchangé) : elle devient « shopList v2026.05 » après modification de `versionName`.

==========================
RÈGLES MÉTIER
==========================

* Cinq lignes de séparation exactement, aux emplacements de la section ÉCRANS.
* Pas de ligne entre « Sauvegarde complète » et « Restaurer une sauvegarde ».
* Version de l'application : `versionName = "2026.05"`, `versionCode = 3`.

==========================
MODÈLE DE DONNÉES
==========================

Aucune modification.

==========================
ARCHITECTURE
==========================

Aucune classe nouvelle. Fichiers modifiés :

**1. `app/src/main/java/com/jb/shoplist/ui/screens/options/OptionsScreen.kt`**

Dans la `Column` de contenu :

* entre la zone « Nom de la liste » et le libellé « Position de la barre de titre et d'options » : remplacer `Spacer(modifier = Modifier.height(16.dp))` par la ligne de séparation ;
* entre le `SingleChoiceSegmentedButtonRow` et le bouton « Gérer les catégories » : remplacer `Spacer(modifier = Modifier.height(16.dp))` par la ligne ;
* entre « Gérer les catégories » et « Importer une liste » : remplacer `Spacer(modifier = Modifier.height(8.dp))` par la ligne ;
* entre « Importer une liste » et « Sauvegarde complète » : remplacer `Spacer(modifier = Modifier.height(8.dp))` par la ligne ;
* entre « Sauvegarde complète » et « Restaurer une sauvegarde » : **conserver** `Spacer(modifier = Modifier.height(8.dp))` ;
* après « Restaurer une sauvegarde », avant `Spacer(modifier = Modifier.weight(1f))` : ajouter la ligne.

`HorizontalDivider` est couvert par l'import existant `androidx.compose.material3.*`. Ne rien modifier d'autre dans le fichier (dialogues, lanceurs, paramètres, textes).

**2. `app/build.gradle.kts`**

Dans `defaultConfig` :

```kotlin
versionCode = 3
versionName = "2026.05"
```

Ne modifier aucun autre réglage Gradle (versions de Kotlin, AGP, BOM Compose, dépendances).

==========================
COMPOSANTS UI
==========================

* `HorizontalDivider` (×5)

==========================
CAS LIMITES
==========================

* Écran Options plus haut que l'écran sur un petit téléphone : non traité (le Samsung Galaxy S23+ affiche tout le contenu avec les lignes).
* Installation par-dessus la version 2026.04 : `versionCode` 3 supérieur à 2, mise à jour normale, données conservées (aucune migration).
* Sauvegardes : celles faites avec la nouvelle version portent `"appVersion": "2026.05"` ; les anciennes restent restaurables (le champ n'est pas contrôlé).

==========================
TESTS
==========================

Tests automatisés : `./gradlew test` et `./gradlew connectedAndroidTest` passent sans modification (`OptionsScreenTest` passe `versionApp = "2026.04"` en paramètre, indépendamment de la version réelle).

Tests manuels :

1. Ouvrir Options : cinq lignes aux emplacements prévus, aucune entre « Sauvegarde complète » et « Restaurer une sauvegarde ».
2. Version affichée en bas : « shopList v2026.05 ».
3. Toucher la zone « Nom de la liste » : le dialogue « Renommer la liste » s'ouvre (inchangé). Les boutons fonctionnent comme avant.
4. Faire une sauvegarde complète : le fichier contient `"appVersion": "2026.05"`.

==========================
CRITÈRES D'ACCEPTATION
==========================

* compile, aucun nouveau warning, aucun crash ;
* cinq lignes de séparation conformes ;
* version 2026.05 affichée, `versionCode` 3 ;
* aucun autre changement ;
* tests existants passants.

==========================
TRAVAIL ATTENDU DE GEMINI
==========================

1. `OptionsScreen.kt` : remplacement des espacements par les lignes de séparation et ajout de la ligne finale. Compiler.
2. `app/build.gradle.kts` : `versionCode = 3`, `versionName = "2026.05"`. Synchroniser Gradle et compiler.
3. Lancer `./gradlew test` et `./gradlew connectedAndroidTest`.

Créer uniquement les classes nécessaires.

Ne pas modifier d'autres modules.

Ne pas refactoriser l'application.

Compiler après chaque étape.

À la fin, résumer :

• fichiers créés

• fichiers modifiés

• points restant à faire
