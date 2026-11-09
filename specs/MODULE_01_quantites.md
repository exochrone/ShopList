# MODULE 01
Quantités des produits

============================================================
OBJECTIF
============================================================

Ajouter à chaque produit de la liste une **quantité facultative en texte libre** (exemples : `2`, `2 boîtes`, `200 g`, ou vide), l'afficher dans la liste, la rendre modifiable, et la propager dans le partage, l'export/import CSV et la sauvegarde/restauration.

Ce module refond aussi le dialogue « Modifier la tâche », qui devient « Modifier le produit » : ajout de la quantité, choix de la catégorie, boutons d'effacement, nouvelle disposition des boutons.

Vocabulaire : dans le code, un produit est une `TacheEntity`. Ce module **ne renomme aucune classe, fonction ni table**. Seuls les libellés affichés dans le dialogue d'édition passent de « tâche » à « produit ».

Ce que ce module ne fait PAS :

* Pas de gestion d'unité ni de calcul sur les quantités (aucune addition, aucune conversion). La quantité est une simple chaîne.
* Pas de modification du dialogue d'ajout d'un produit (« Nouvelle tâche dans « … » »). Il reste identique. Un produit créé a une quantité vide.
* Pas de recherche sur la quantité : la recherche porte toujours sur le nom seul.
* Pas d'import de liste externe (Hermes, Tandoor, etc.). Ce sera un module ultérieur.
* Pas de renommage global « tâche » vers « produit » dans le reste de l'application.

Modules dont il dépend : aucun. Il s'appuie sur le code existant (Room, `TodoViewModel`, `TodoListScreen`, `PartagerScreen`, sauvegarde JSON, import/export CSV).

============================================================
FONCTIONNALITÉS
============================================================

L'utilisateur peut :

* voir la quantité de chaque produit dans la liste, alignée à droite sur la ligne du produit ;
* saisir, modifier ou effacer la quantité d'un produit depuis le dialogue « Modifier le produit » ;
* changer un produit de catégorie depuis ce même dialogue (liste déroulante) ;
* effacer le contenu du champ nom ou du champ quantité d'un clic sur une croix ;
* supprimer le produit depuis le dialogue (avec l'annulation existante) ;
* fermer le dialogue sans enregistrer par la croix en haut à droite ou par le geste/bouton Retour du système Android ;
* partager la liste avec les quantités (`Aubergine : 3`) ;
* exporter et réimporter les quantités en CSV (3e colonne facultative) ;
* retrouver les quantités après une sauvegarde complète puis une restauration.

============================================================
ÉCRANS
============================================================

------------------------------------------------------------
Écran 1 : Liste des produits (existant, `TodoListScreen`)
------------------------------------------------------------

Nom : Liste principale.

Objectif : afficher la quantité de chaque produit.

Contenu (modification de chaque ligne produit, composable `TacheItem`) :

* Case à cocher (inchangée).
* Nom du produit, aligné à gauche.
* Quantité, alignée à droite, uniquement si elle n'est pas vide.

Règle de disposition (voir COMPORTEMENT, section « Disposition d'une ligne ») :

* si le nom (sur une seule ligne) et la quantité tiennent ensemble dans la largeur disponible : même ligne, nom à gauche, quantité à droite, alignés sur la ligne de base du texte ;
* sinon : le nom occupe toute la largeur (retour à la ligne autorisé comme aujourd'hui), et la quantité est placée **en dessous**, alignée à droite.

Actions possibles :

* toucher la case : coche/décoche (inchangé) ;
* toucher le nom **ou la quantité** : ouvre le dialogue « Modifier le produit » ;
* glisser-déposer : inchangé.

Navigation : aucune nouvelle navigation.

------------------------------------------------------------
Écran 2 : Dialogue « Modifier le produit » (remplace « Modifier la tâche »)
------------------------------------------------------------

Nom : `ProduitEditionDialog` (nouveau composable).

Objectif : modifier le nom, la quantité et la catégorie d'un produit, ou le supprimer.

Contenu, de haut en bas :

1. **Barre de titre** : texte « Modifier le produit » à gauche, bouton icône croix (`Icons.Default.Close`, description d'accessibilité « Fermer ») à droite.
2. **Liste déroulante « Catégorie »** : affiche la catégorie actuelle du produit. Au toucher, déroule toutes les catégories dans l'ordre d'affichage de la liste (`uiState.categories`, triées par `ordre`). La catégorie actuelle est présélectionnée.
3. **Champ « nom »** : `OutlinedTextField` sur une ligne, prérempli avec le nom actuel. Placeholder (affiché quand le champ est vide) : « Nom du produit ». Icône croix à droite (voir règles). Message d'erreur sous le champ si doublon (voir RÈGLES MÉTIER).
4. **Champ « quantité »** : `OutlinedTextField` sur une ligne, prérempli avec la quantité actuelle (vide si aucune). Placeholder : « Quantité (facultatif) ». Icône croix à droite.
5. **Ligne de boutons** : « Supprimer » à gauche (texte rouge, comme aujourd'hui), « Valider » à droite. Les deux sur la même ligne (`Arrangement.SpaceBetween`).

Il n'y a **plus de bouton « Annuler »**.

Actions possibles :

* changer la catégorie dans la liste déroulante ;
* modifier ou effacer le nom ;
* modifier ou effacer la quantité ;
* Valider : enregistre et ferme ;
* Supprimer : supprime le produit et ferme ;
* croix en haut à droite, ou geste/bouton Retour du système Android : ferme sans enregistrer.

Navigation : dialogue modal au-dessus de la liste ; à la fermeture, retour à la liste.

------------------------------------------------------------
Écran 3 : Partager la liste (existant, `PartagerScreen`)
------------------------------------------------------------

Seul le texte généré change : chaque produit dont la quantité n'est pas vide est suivi de ` : ` puis de sa quantité. Les options existantes (masquer les catégories, afficher les cochés, édition manuelle du texte, export CSV) sont conservées.

============================================================
COMPORTEMENT
============================================================

------------------------------------------------------------
Disposition d'une ligne produit
------------------------------------------------------------

* Quantité vide : la ligne s'affiche exactement comme aujourd'hui (le nom occupe toute la largeur).
* Quantité non vide :
  * On mesure la largeur du nom sur une seule ligne (largeur intrinsèque maximale) et la largeur de la quantité.
  * Si `largeurNom + 8.dp + largeurQuantite <= largeurDisponible` : nom et quantité sur la même ligne, nom collé à gauche, quantité collée à droite, alignés sur la première ligne de base.
  * Sinon : le nom est mesuré avec toute la largeur disponible (il peut passer sur plusieurs lignes) ; la quantité est placée sur une ligne en dessous, alignée à droite.
  * Si la quantité est elle-même plus large que la ligne, elle passe à la ligne et reste alignée à droite (`textAlign = TextAlign.End`).
* Style de la quantité : même taille de police et même couleur que le nom (14.sp).
* Produit coché : la quantité est **grisée et barrée** exactement comme le nom (même couleur `tacheFaite`, `TextDecoration.LineThrough`).
* Toute la zone nom et quantité est cliquable et ouvre le dialogue d'édition.

------------------------------------------------------------
Ouverture du dialogue
------------------------------------------------------------

* Toucher un produit ouvre « Modifier le produit » prérempli avec le nom, la quantité et la catégorie du produit.
* Le focus est donné au champ nom à l'ouverture (comportement actuel conservé). Le clavier s'ouvre.

------------------------------------------------------------
Croix d'effacement des champs
------------------------------------------------------------

* Chaque champ (nom, quantité) affiche une icône croix (`Icons.Default.Clear`, description « Effacer ») en `trailingIcon`, **uniquement quand le champ n'est pas vide**.
* Au toucher : le champ est vidé et reçoit le focus.
* Après effacement du nom, le placeholder « Nom du produit » s'affiche et le bouton Valider est désactivé.

------------------------------------------------------------
Liste déroulante des catégories
------------------------------------------------------------

* Composant `ExposedDropdownMenuBox` Material 3, champ en lecture seule avec le libellé « Catégorie » et l'icône de déroulement standard.
* Sélectionner une catégorie met à jour l'état du dialogue uniquement ; rien n'est écrit en base avant Valider.

------------------------------------------------------------
Clavier
------------------------------------------------------------

* Champ nom : action clavier « Suivant » (`ImeAction.Next`), qui place le focus sur le champ quantité.
* Champ quantité : action clavier « OK » (`ImeAction.Done`), qui déclenche Valider si Valider est actif.

------------------------------------------------------------
Valider
------------------------------------------------------------

* Le nom et la quantité sont nettoyés (`trim()`).
* Valider est **désactivé** si le nom nettoyé est vide ou si le nom est en doublon.
* Si rien n'a changé (nom, quantité et catégorie identiques), le dialogue se ferme sans écriture en base.
* Sinon, un seul appel au ViewModel `modifierProduit(tache, nouveauTitre, nouvelleQuantite, nouvelleCategorie)` enregistre les trois valeurs, puis le dialogue se ferme.
* Changement de catégorie : le produit est placé **à la fin** de la catégorie cible (voir RÈGLES MÉTIER). Son état coché/décoché et sa quantité sont conservés.
* Si la catégorie cible est repliée, le produit y est déplacé sans dépliage automatique.

------------------------------------------------------------
Supprimer
------------------------------------------------------------

* Comportement actuel conservé : appel à `supprimerTache(tache)`, fermeture du dialogue, snackbar avec annulation (`ActionAnnulable.SuppressionTache`).
* L'annulation restaure le produit **avec sa quantité** (l'entité complète est conservée).
* Pas de confirmation supplémentaire.

------------------------------------------------------------
Fermer sans enregistrer
------------------------------------------------------------

* Croix en haut à droite : ferme le dialogue, modifications abandonnées, sans confirmation.
* Geste ou bouton Retour **du système Android** (balayage depuis le bord de l'écran, ou bouton de la barre de navigation selon le réglage du téléphone) : idem. Il s'agit du Retour système géré par `dismissOnBackPress`. **Ne pas ajouter de flèche ni de bouton Retour dans le dialogue** : la seule commande de fermeture visible est la croix en haut à droite.
* Toucher hors du dialogue : **ne fait rien** (le dialogue reste ouvert). Utiliser `DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true)`.

------------------------------------------------------------
Cocher / décocher
------------------------------------------------------------

* Cocher ou décocher un produit **ne modifie jamais** sa quantité.
* Idem pour « Tout cocher » et pour l'annulation d'un cochage.

------------------------------------------------------------
Partage
------------------------------------------------------------

* Ligne d'un produit : `titre` si la quantité est vide, sinon `titre + " : " + quantite`.
  * Exemples : `Aubergine : 3`, `Farine : 1kg`, `Sel`.
* Le préfixe d'indentation existant (4 espaces quand les catégories sont affichées) est conservé devant la ligne.

------------------------------------------------------------
Export CSV
------------------------------------------------------------

* Chaque produit est exporté sur 3 colonnes : `categorie;titre;quantite`. La 3e colonne est vide si la quantité est vide (`Frais;Sel;`).
* Les catégories sans produit restent exportées comme aujourd'hui (`categorie;`).
* Chaque champ passe par `escapeCsv` (existant), quantité comprise.
* Le BOM UTF-8 est conservé.

------------------------------------------------------------
Import CSV
------------------------------------------------------------

* Chaque ligne est découpée par une fonction `parserLigneCsv` qui **respecte les guillemets** produits par `escapeCsv` (un `;` entre guillemets ne sépare pas ; `""` entre guillemets donne `"`).
* Lignes à 2 colonnes : comportement actuel, quantité vide.
* Lignes à 3 colonnes ou plus : la 3e colonne (nettoyée) est la quantité ; les colonnes suivantes sont ignorées.
* Produit nouveau (aucun produit avec le même titre et la même catégorie) : créé avec la quantité lue.
* Produit déjà existant (même titre et même catégorie, règle actuelle) : sa quantité est remplacée **seulement si la quantité lue n'est pas vide** ; sinon il reste inchangé. Son état coché n'est pas modifié.

------------------------------------------------------------
Sauvegarde complète
------------------------------------------------------------

* Chaque objet du tableau `taches` reçoit un champ `"quantite"` (chaîne, `""` si vide).
* `formatVersion` passe à **2**.

------------------------------------------------------------
Restauration
------------------------------------------------------------

* Les fichiers en `formatVersion` 1 **et** 2 sont acceptés.
* Version 1 (ou champ absent) : quantité vide pour tous les produits.
* Version 2 : `quantite = obj.optString("quantite", "")`.
* Toute autre version : erreur existante « Version de format non supportée ».

============================================================
RÈGLES MÉTIER
============================================================

1. La quantité est facultative. Elle est stockée comme une chaîne non nulle ; « pas de quantité » vaut `""`.
2. La quantité est enregistrée nettoyée (`trim()`). Une quantité composée uniquement d'espaces est enregistrée comme `""`.
3. Aucun format n'est imposé à la quantité : chiffres, lettres, unités, tout est accepté. Pas de longueur maximale.
4. Le nom du produit est obligatoire (non vide après `trim()`).
5. Le nom reste unique dans toute la liste, sans tenir compte de la casse ni des espaces en début et fin, en excluant le produit en cours d'édition. Message d'erreur inchangé : « Cet élément existe déjà dans la catégorie X ».
6. Cocher, décocher, tout cocher et annuler un cochage ne modifient jamais la quantité.
7. Un changement de catégorie place le produit en dernière position de la catégorie cible : `ordre = (ordre max des produits de la catégorie cible) + 1`, ou `0` si la catégorie cible est vide.
8. Un changement de catégorie conserve l'état coché et la quantité.
9. Le partage affiche `nom : quantité`, ou `nom` seul si la quantité est vide.
10. Un produit créé par le dialogue d'ajout existant a une quantité vide.
11. Les données existantes sont conservées à la mise à jour de l'application : tous les produits existants reçoivent une quantité vide.

============================================================
MODÈLE DE DONNÉES
============================================================

------------------------------------------------------------
Entity : `TacheEntity` (modifiée, `Entities.kt`)
------------------------------------------------------------

```kotlin
@Entity(tableName = "taches")
data class TacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titre: String,
    val categorie: String,
    val faite: Boolean = false,
    val ordre: Int = 0,
    @ColumnInfo(defaultValue = "")
    val quantite: String = ""
)
```

------------------------------------------------------------
Base : `TodoDatabase` (modifiée)
------------------------------------------------------------

* `version = 4` (actuellement 3).
* Migration manuelle 3 vers 4 (ne pas utiliser `fallbackToDestructiveMigration`, les données de l'utilisateur doivent être conservées) :

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE taches ADD COLUMN quantite TEXT NOT NULL DEFAULT ''")
    }
}
```

* Enregistrer la migration : `.addMigrations(MIGRATION_3_4)` dans `Room.databaseBuilder(...)`.
* La déclaration `@ColumnInfo(defaultValue = "")` doit correspondre exactement au `DEFAULT ''` de la migration, sinon Room lève une erreur de validation du schéma au démarrage.

------------------------------------------------------------
DAO : `TacheDao` (modifié, `Daos.kt`)
------------------------------------------------------------

Ajouter :

```kotlin
@Query("SELECT MAX(ordre) FROM taches WHERE categorie = :categorie")
suspend fun obtenirOrdreMaxParCategorie(categorie: String): Int?
```

------------------------------------------------------------
Format JSON de sauvegarde (version 2)
------------------------------------------------------------

```json
{
  "formatVersion": 2,
  "exportDate": "2026-11-09T18:30:00",
  "appVersion": "…",
  "titreListe": "Courses",
  "parametres": [ … ],
  "categories": [ … ],
  "taches": [
    { "titre": "Aubergine", "categorie": "Légumes", "faite": false, "ordre": 0, "quantite": "3" },
    { "titre": "Sel", "categorie": "Épicerie", "faite": true, "ordre": 1, "quantite": "" }
  ]
}
```

------------------------------------------------------------
UI State
------------------------------------------------------------

* `TodoUiState` : **inchangé** (les produits contiennent déjà l'entité complète, donc la quantité).
* État local du dialogue (dans `ProduitEditionDialog`, avec `rememberSaveable` pour survivre à la rotation) :
  * `titre: String`
  * `quantite: String`
  * `categorie: String`
  * `menuCategoriesOuvert: Boolean`

============================================================
ARCHITECTURE
============================================================

Principe : respecter l'architecture actuelle (un `AndroidViewModel` qui expose un `StateFlow<TodoUiState>` et appelle les DAO Room ; écrans Compose sans état métier, qui reçoivent des callbacks). **Ne pas introduire de Repository, d'injection de dépendances ni de nouvelle couche** dans ce module.

------------------------------------------------------------
Fichiers à créer
------------------------------------------------------------

`ui/components/ProduitEditionDialog.kt`

```kotlin
@Composable
fun ProduitEditionDialog(
    tache: TacheEntity,
    categories: List<CategorieEntity>,
    onCheckErreurTitre: (String) -> String?,   // retourne le message d'erreur ou null
    onValider: (titre: String, quantite: String, categorie: String) -> Unit,
    onSupprimer: () -> Unit,
    onFermer: () -> Unit
)
```

* Construit avec `Dialog` + `Surface` (forme et élévation Material 3 par défaut), pas avec `AlertDialog`, pour placer la croix de fermeture dans la barre de titre.
* `DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true)` ; `onDismissRequest = onFermer`.
* Contient la logique de nettoyage (`trim`), la détection « rien n'a changé » et l'activation du bouton Valider.

`Formatage.kt` (package `com.jb.shoplist`) : fonctions pures, testables sans Android.

```kotlin
/** "Aubergine : 3", ou "Aubergine" si la quantité est vide. */
fun libelleAvecQuantite(titre: String, quantite: String): String

/** Découpe une ligne CSV séparée par ';' en respectant les guillemets. */
fun parserLigneCsv(ligne: String): List<String>
```

------------------------------------------------------------
Fichiers à modifier
------------------------------------------------------------

`Entities.kt`
* Ajouter `quantite` à `TacheEntity`.

`TodoDatabase.kt`
* `version = 4`, `MIGRATION_3_4`, `.addMigrations(MIGRATION_3_4)`.

`Daos.kt`
* Ajouter `obtenirOrdreMaxParCategorie`.

`TodoViewModel.kt`
* Remplacer `modifierTitreTache(tache, nouveauTitre)` par :

```kotlin
fun modifierProduit(
    tache: TacheEntity,
    nouveauTitre: String,
    nouvelleQuantite: String,
    nouvelleCategorie: String
)
```

  * Si la catégorie change : calculer le nouvel `ordre` via `obtenirOrdreMaxParCategorie(nouvelleCategorie)`.
  * Un seul `tacheDao.mettreAJour(tache.copy(titre = …, quantite = …, categorie = …, ordre = …))`.
* `FORMAT_VERSION_BACKUP = 2` ; `construireJsonSauvegarde()` écrit `"quantite"`.
* `parserSauvegarde()` : accepter les versions 1 et 2 ; lire `quantite` avec `optString("quantite", "")`.
* `exporterCsv()` : 3e colonne quantité.
* `importerDepuisCsv()` : utiliser `parserLigneCsv`, lire la 3e colonne facultative, appliquer les règles de l'import (création et mise à jour de quantité).
* `basculerFait`, `toutCocher`, `annulerDernierCochage`, `annulerDerniereAction` : vérifier qu'ils utilisent `copy(...)` sans toucher `quantite` (c'est déjà le cas, ne pas les réécrire).

`ui/components/TacheItem.kt`
* Afficher la quantité selon la règle de disposition, avec un `Layout` personnalisé (voir COMPOSANTS UI).
* Rendre toute la zone nom et quantité cliquable (`onClickTache`).

`ui/screens/liste/TodoListScreen.kt`
* Remplacer le paramètre `onModifierTitreTache: (TacheEntity, String) -> Unit` par `onModifierProduit: (TacheEntity, String, String, String) -> Unit`.
* Remplacer le `SaisieDialog` « Modifier la tâche » (bloc `tacheEnEdition?.let { … }`) par `ProduitEditionDialog`, en conservant la même règle de doublon dans `onCheckErreurTitre`.
* Ne pas toucher aux autres `SaisieDialog` (nouvelle catégorie, renommer la liste, nouvelle tâche).

`ui/navigation/ShopListNavHost.kt`
* `onModifierTitreTache = viewModel::modifierTitreTache` devient `onModifierProduit = viewModel::modifierProduit`.

`ui/screens/partage/PartagerScreen.kt`
* Dans `texteCalcule`, remplacer `tache.titre` par `libelleAvecQuantite(tache.titre, tache.quantite)`.

------------------------------------------------------------
Fichiers à ne pas modifier
------------------------------------------------------------

* `SaisieDialog.kt` (toujours utilisé par les autres dialogues).
* `CategoriesScreen.kt`, thème, navigation (hors la ligne indiquée), `MainActivity.kt`.

============================================================
COMPOSANTS UI
============================================================

* `Dialog` + `Surface` : conteneur du dialogue d'édition.
* `Row` + `Text` + `IconButton(Icons.Default.Close)` : barre de titre du dialogue.
* `ExposedDropdownMenuBox` + `OutlinedTextField(readOnly = true)` + `ExposedDropdownMenu` + `DropdownMenuItem` : choix de la catégorie. Avec la version Material 3 du BOM 2024.12.01, l'ancre s'écrit `Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)`.
* `OutlinedTextField` (×2) avec `placeholder`, `trailingIcon = IconButton(Icons.Default.Clear)` conditionnel, `singleLine = true`, `keyboardOptions` / `keyboardActions`.
* `FocusRequester` : focus initial sur le nom, focus après effacement d'un champ, passage au champ quantité.
* `TextButton` « Supprimer » (couleur rouge, comme aujourd'hui) et `TextButton` « Valider », dans une `Row(horizontalArrangement = Arrangement.SpaceBetween)`.
* `Layout` personnalisé dans `TacheItem` pour la ligne produit :
  * mesurer la quantité avec `constraints.copy(minWidth = 0)` ;
  * comparer `nom.maxIntrinsicWidth(...) + 8.dp + largeurQuantite` à `constraints.maxWidth` ;
  * cas « même ligne » : placer le nom à `x = 0`, la quantité à `x = maxWidth - largeurQuantite`, en alignant les premières lignes de base (`FirstBaseline`) ;
  * cas « empilé » : nom mesuré sur toute la largeur en haut, quantité en dessous à `x = maxWidth - largeurQuantite`.
* Snackbar d'annulation de suppression : existante, inchangée.

============================================================
CAS LIMITES
============================================================

* **Base existante (mise à jour de l'app)** : migration 3 vers 4 ; aucun produit, aucune catégorie, aucun paramètre perdu ; quantités vides.
* **Base vide** : aucun impact.
* **Quantité vide** : ligne identique à l'affichage actuel ; partage sans ` : `.
* **Quantité composée d'espaces** : enregistrée comme vide.
* **Nom très long** : nom sur toute la largeur (multi-lignes), quantité en dessous à droite.
* **Quantité très longue** : passe à la ligne, alignée à droite, sans chevaucher le nom.
* **Nom effacé avec la croix** : placeholder « Nom du produit », Valider désactivé.
* **Nom en doublon** : message d'erreur, Valider désactivé.
* **Rien modifié puis Valider** : fermeture sans écriture.
* **Changement vers une catégorie vide** : `ordre = 0`.
* **Changement vers une catégorie repliée** : produit déplacé, catégorie non dépliée.
* **Rotation de l'écran dialogue ouvert** : la saisie en cours (nom, quantité, catégorie) est conservée (`rememberSaveable`).
* **Geste ou bouton Retour du système Android, dialogue ouvert** : fermeture sans enregistrer.
* **Toucher hors du dialogue** : aucun effet.
* **Suppression puis annulation** : produit restauré avec sa quantité, sa catégorie et son état coché.
* **Produit coché** : quantité grisée et barrée ; décocher ne la modifie pas.
* **Restauration d'une sauvegarde v1** : acceptée, quantités vides.
* **Restauration d'une sauvegarde v3 ou plus** : refusée avec le message existant.
* **CSV ancien format (2 colonnes)** : importé comme aujourd'hui.
* **CSV avec quantité contenant `;` ou `"`** : exportée entre guillemets, réimportée à l'identique.
* **Liste très longue** : le `Layout` personnalisé ne doit pas provoquer de recomposition en boucle ni de ralentissement visible au défilement.
* **Erreur Room à l'enregistrement** : l'exception est capturée dans le ViewModel et un message est émis via `_evenements` (mécanisme existant), pas de crash.

============================================================
TESTS
============================================================

------------------------------------------------------------
Tests unitaires (JVM, `app/src/test`)
------------------------------------------------------------

`FormatageTest`
* `libelleAvecQuantite("Aubergine", "3") == "Aubergine : 3"`
* `libelleAvecQuantite("Farine", "1kg") == "Farine : 1kg"`
* `libelleAvecQuantite("Sel", "") == "Sel"`
* `parserLigneCsv("Frais;Lait;1 L") == listOf("Frais", "Lait", "1 L")`
* `parserLigneCsv("Frais;Lait") == listOf("Frais", "Lait")`
* `parserLigneCsv("Frais;Sel;") == listOf("Frais", "Sel", "")`
* `parserLigneCsv("Épicerie;\"Riz; basmati\";\"2 \"\"gros\"\" sacs\"")` donne `listOf("Épicerie", "Riz; basmati", "2 \"gros\" sacs")`
* Aller-retour : `parserLigneCsv` de la sortie de `escapeCsv` redonne les valeurs d'origine (rendre `escapeCsv` `internal` si besoin pour le test, sans changer sa logique).

------------------------------------------------------------
Tests instrumentés (`app/src/androidTest`)
------------------------------------------------------------

* Migration 3 vers 4 avec `MigrationTestHelper`, **si** le schéma `app/schemas/com.jb.shoplist.TodoDatabase/3.json` existe. Sinon, test manuel décrit dans les critères d'acceptation.
* Test Compose de `ProduitEditionDialog` :
  * la croix d'un champ vide n'est pas affichée ; elle vide le champ quand on la touche ;
  * Valider est désactivé quand le nom est vide ;
  * sélectionner une autre catégorie puis Valider appelle `onValider` avec la nouvelle catégorie ;
  * la croix de fermeture appelle `onFermer` sans appeler `onValider`.

============================================================
CRITÈRES D'ACCEPTATION
============================================================

* Le projet compile.
* Aucun nouveau warning de compilation.
* Aucun crash.
* **Migration** : installer la version actuelle avec des produits, des catégories colorées et des produits cochés, puis installer la nouvelle version par-dessus : toutes les données sont présentes, quantités vides.
* Une quantité saisie s'affiche à droite sur la ligne du produit.
* Un nom long fait passer la quantité en dessous, alignée à droite, sans chevauchement.
* La quantité d'un produit coché est grisée et barrée ; elle est conservée après décochage.
* Le dialogue s'intitule « Modifier le produit » ; placeholder « Nom du produit » ; champ quantité facultatif ; croix d'effacement fonctionnelles.
* La liste déroulante affiche la catégorie actuelle et déplace le produit à la fin de la catégorie choisie après Valider.
* Boutons « Supprimer » puis « Valider » sur la même ligne ; plus de bouton « Annuler » ; la croix en haut à droite et le Retour système Android ferment sans enregistrer ; aucune flèche Retour dans le dialogue ; un toucher hors du dialogue ne le ferme pas.
* Suppression OK et annulation de la suppression OK (quantité restaurée).
* Partage : `Aubergine : 3`, `Farine : 1kg`, et `Sel` sans quantité.
* Export CSV avec 3 colonnes ; réimport d'un CSV à 2 colonnes et d'un CSV à 3 colonnes OK.
* Sauvegarde complète puis restauration : quantités identiques.
* Restauration d'une ancienne sauvegarde (formatVersion 1) OK.
* Recherche : inchangée (porte sur le nom).
* Tests unitaires `FormatageTest` au vert.

============================================================
TRAVAIL ATTENDU DE GEMINI
============================================================

Créer uniquement les classes nécessaires : `ProduitEditionDialog.kt` et `Formatage.kt` (et `FormatageTest.kt`).

Modifier uniquement les fichiers listés dans ARCHITECTURE.

Ne pas modifier d'autres modules ni le dialogue d'ajout d'un produit.

Ne pas refactoriser l'application (pas de Repository, pas de renommage `Tache` vers `Produit`, pas de déplacement de fichiers).

Procéder dans cet ordre et vérifier que le projet compile après chaque étape :

1. Modèle de données et migration (`Entities.kt`, `TodoDatabase.kt`, `Daos.kt`).
2. ViewModel (`modifierProduit`, sauvegarde/restauration, CSV) et `Formatage.kt`.
3. Dialogue `ProduitEditionDialog` et branchement dans `TodoListScreen` / `ShopListNavHost`.
4. Affichage de la quantité dans `TacheItem`.
5. Partage (`PartagerScreen`).
6. Tests.

À la fin, résumer :

• fichiers créés

• fichiers modifiés

• points restant à faire
