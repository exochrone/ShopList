# MODULE 02
Dialog « Nouveau produit » harmonisé avec « Modifier le produit »

==========================
OBJECTIF
==========================

Remplacer le dialog d'ajout de produit, ouvert par le menu ⋮ d'une catégorie (« Ajout d'élément »), par un dialog identique dans sa forme au dialog « Modifier le produit » existant (`ProduitEditionDialog`).

Le nouveau dialog permet de choisir la catégorie, de saisir le nom et, si on le souhaite, la quantité dès la création du produit.

Pour cela, `ProduitEditionDialog` est étendu avec un **mode ajout**. Il n'y a pas de nouveau composant de dialog : un seul composant sert aux deux usages.

Dans les deux modes (ajout et modification), les catégories de la liste déroulante s'affichent dans leur couleur.

Ce que ce module NE fait PAS :

* Il ne modifie pas l'écran « Gestion des catégories » (`CategoriesScreen`).
* Il ne crée pas d'autre point d'entrée pour ajouter un produit (pas de bouton flottant, pas d'ajout depuis la recherche).
* Il ne remplace pas `SaisieDialog` ailleurs : ce dialog générique reste utilisé pour l'ajout et le renommage de catégorie et le renommage de la liste. `SaisieDialog.kt` n'est pas modifié.
* Il ne change pas la règle d'unicité du nom de produit.
* Il ne modifie pas le modèle de données Room, la sauvegarde JSON, l'import/export CSV ni le partage.
* Il ne renomme pas les autres libellés « tâche » de l'application (snackbar « Tâche supprimée », etc.).

Modules dont il dépend : MODULE 01 (quantités des produits) et MODULE 01b (affichage de la quantité), déjà implémentés.

==========================
FONCTIONNALITÉS
==========================

L'utilisateur peut, depuis le menu ⋮ d'une catégorie > « Ajout d'élément » :

* choisir la catégorie du nouveau produit (préremplie avec la catégorie du menu utilisé) ;
* saisir le nom du produit (obligatoire) ;
* saisir une quantité (facultative) ;
* vider chaque champ texte d'un toucher sur sa croix ;
* ajouter le produit avec le bouton « Ajouter » ;
* fermer le dialog sans rien ajouter avec la croix en haut à droite ou le Retour Android.

Dans les dialogs « Nouveau produit » et « Modifier le produit », l'utilisateur voit chaque catégorie de la liste déroulante, ainsi que la catégorie sélectionnée dans le champ, écrite dans la couleur de la catégorie.

==========================
ÉCRANS
==========================

Aucun nouvel écran. Un dialog est modifié.

--------------------------
Dialog « Nouveau produit » (mode ajout de ProduitEditionDialog)
--------------------------

Nom : Nouveau produit

Objectif : créer un produit avec sa catégorie, son nom et sa quantité éventuelle.

Contenu, de haut en bas (même disposition, mêmes marges et mêmes composants que « Modifier le produit ») :

1. Barre de titre : texte « Nouveau produit » (`titleLarge`, gras) à gauche, bouton icône croix (`Icons.Default.Close`, contentDescription « Fermer ») à droite.
2. Champ « Catégorie » : liste déroulante en lecture seule (`ExposedDropdownMenuBox`), label « Catégorie ». Valeur initiale : la catégorie dont le menu ⋮ a été utilisé. Le texte du champ est dans la couleur de la catégorie sélectionnée. Le menu liste toutes les catégories triées par `ordre`, chaque nom dans sa couleur.
3. Champ « Nom du produit » : `OutlinedTextField` une ligne, label et placeholder « Nom du produit », vide à l'ouverture, croix d'effacement à droite (`Icons.Default.Clear`, contentDescription « Effacer ») visible seulement si le champ n'est pas vide. Message d'erreur sous le champ si le nom existe déjà.
4. Champ « Quantité (facultatif) » : `OutlinedTextField` une ligne, label et placeholder « Quantité (facultatif) », vide à l'ouverture, croix d'effacement identique.
5. Ligne de boutons : un seul bouton, « Ajouter » (`TextButton`), **aligné à droite**. Pas de bouton Supprimer, pas de bouton Annuler.

Actions possibles : choisir une catégorie, saisir, effacer un champ, Ajouter, fermer.

Navigation : aucune. Le dialog s'affiche au-dessus de `TodoListScreen` et revient à la liste à sa fermeture.

--------------------------
Dialog « Modifier le produit » (mode modification, existant)
--------------------------

Inchangé, à une exception près : affichage des catégories en couleur (champ et menu), exactement comme en mode ajout.

==========================
COMPORTEMENT
==========================

Ouverture :

* Menu ⋮ d'une catégorie > « Ajout d'élément » : le menu se ferme et le dialog « Nouveau produit » s'ouvre, catégorie préremplie avec celle du menu.
* Le focus est sur « Nom du produit » et le clavier s'ouvre.

Saisie :

* Clavier du nom : action « Suivant » qui passe le focus à la quantité.
* Clavier de la quantité : action « OK » qui déclenche l'ajout (même effet que le bouton « Ajouter », sans effet si le bouton est désactivé).
* Croix d'un champ : vide le champ et remet le focus dessus.
* Choisir une catégorie dans le menu : met à jour le champ (texte et couleur) et ferme le menu. Le message d'erreur du nom ne dépend pas de la catégorie (unicité sur toute la liste).

Bouton « Ajouter » :

* Désactivé si le nom nettoyé (`trim()`) est vide ou s'il existe déjà un produit de même nom dans la liste.
* Au toucher : le produit est créé avec le nom nettoyé, la quantité nettoyée (`""` si vide) et la catégorie sélectionnée, non coché, en fin de la catégorie sélectionnée. Le dialog se ferme.
* Pas de Toast ni de snackbar après l'ajout : le produit apparaît dans la liste.

Fermeture sans ajout :

* Croix en haut à droite, ou geste / bouton Retour Android : le dialog se ferme, rien n'est créé.
* Un toucher hors du dialog ne le ferme pas (`dismissOnClickOutside = false`, comme aujourd'hui pour « Modifier le produit »).

Rotation : sans objet (application en portrait uniquement). Les saisies sont malgré tout conservées par `rememberSaveable`, comme dans le mode modification.

==========================
RÈGLES MÉTIER
==========================

* La catégorie est obligatoire. Elle est toujours renseignée puisqu'elle est préremplie et que le champ est en lecture seule (on ne peut que choisir une autre catégorie existante).
* Le nom est obligatoire et unique dans **toute** la liste : comparaison sans tenir compte de la casse ni des espaces de début et fin, comme aujourd'hui. Message inchangé : « Cet élément existe déjà dans la catégorie X » (X = catégorie du produit existant).
* La quantité est facultative, en texte libre, nettoyée par `trim()`.
* Le nouveau produit est non coché (`faite = false`).
* Le nouveau produit est placé en fin de la catégorie choisie : `ordre = (obtenirOrdreMaxParCategorie(categorie) ?: -1) + 1`, même calcul que `modifierProduit` lors d'un changement de catégorie.
* L'ajout ne touche pas à la pile d'annulation des cochages (`pileCochages`), comme aujourd'hui.
* Couleur des catégories : `Color(categorie.couleur)`, conversion déjà utilisée dans `TodoListScreen` et `CategoriesScreen`. Aucune adaptation de contraste (une catégorie blanche s'affiche en blanc, comme dans les en-têtes).

==========================
MODÈLE DE DONNÉES
==========================

Aucune modification du modèle Room. Pas de migration. Version de la base inchangée (4).

Entité utilisée : `TacheEntity` (champs `titre`, `categorie`, `faite`, `ordre`, `quantite`), `CategorieEntity` (champs `nom`, `ordre`, `couleur`).

Sauvegarde JSON, import/export CSV et partage : aucun impact (la quantité saisie à la création est une quantité ordinaire, déjà gérée partout).

==========================
ARCHITECTURE
==========================

Aucune classe à créer. Trois fichiers à modifier, plus le test.

--------------------------
1. ui/components/ProduitEditionDialog.kt
--------------------------

Nouvelle signature :

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduitEditionDialog(
    tache: TacheEntity?,                         // null = mode ajout
    categories: List<CategorieEntity>,
    categorieInitiale: String = tache?.categorie ?: "",
    onCheckErreurTitre: (String) -> String?,
    onValider: (titre: String, quantite: String, categorie: String) -> Unit,
    onSupprimer: () -> Unit = {},               // ignoré en mode ajout
    onFermer: () -> Unit
)
```

Logique :

```kotlin
val modeAjout = tache == null

var titre by rememberSaveable { mutableStateOf(tache?.titre ?: "") }
var quantite by rememberSaveable { mutableStateOf(tache?.quantite ?: "") }
var categorie by rememberSaveable { mutableStateOf(tache?.categorie ?: categorieInitiale) }

val aModifie = modeAjout ||
        titreClean != tache!!.titre.trim() ||
        quantiteClean != tache.quantite.trim() ||
        categorie != tache.categorie
```

(Gemini peut écrire l'équivalent sans `!!`, par exemple avec `tache == null || ...` et un smart cast ; le comportement doit être celui-ci.)

* `executerValidation()` reste identique : en mode ajout, `aModifie` vaut toujours `true`, donc `onValider` est toujours appelé quand le bouton est actif.
* Titre : `if (modeAjout) "Nouveau produit" else "Modifier le produit"`.
* Ligne de boutons :
  * mode ajout : un seul `TextButton` « Ajouter », aligné à droite (`horizontalArrangement = Arrangement.End`), `enabled = validerActif` ;
  * mode modification : inchangé (« Supprimer » rouge à gauche, « Valider » à droite).
* Couleur des catégories (les deux modes) :

```kotlin
val couleurCategorie = categories.find { it.nom == categorie }
    ?.let { Color(it.couleur) }
    ?: Color.Unspecified

// Champ Catégorie
OutlinedTextField(
    value = categorie,
    ...
    textStyle = LocalTextStyle.current.copy(color = couleurCategorie),
    ...
)

// Éléments du menu
DropdownMenuItem(
    text = { Text(cat.nom, color = Color(cat.couleur)) },
    ...
)
```

`Color.Unspecified` en repli garde la couleur de texte par défaut si la catégorie n'est pas trouvée.

Tout le reste du composant (Dialog, DialogProperties, focus, actions clavier, croix d'effacement, labels) est conservé tel quel.

--------------------------
2. TodoViewModel.kt
--------------------------

Remplacer `ajouterTache(titre: String, categorie: String)` par :

```kotlin
fun ajouterTache(titre: String, categorie: String, quantite: String) {
    viewModelScope.launch {
        try {
            val ordre = (tacheDao.obtenirOrdreMaxParCategorie(categorie) ?: -1) + 1
            tacheDao.inserer(
                TacheEntity(
                    titre = titre.trim(),
                    categorie = categorie,
                    quantite = quantite.trim(),
                    ordre = ordre
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _evenements.emit("Erreur lors de l'ajout du produit : ${e.message}")
        }
    }
}
```

`ajouterTache` n'a pas d'autre appelant que `TodoListScreen` (via `ShopListNavHost`). Ne modifier aucune autre fonction du ViewModel.

--------------------------
3. ui/screens/liste/TodoListScreen.kt
--------------------------

* Paramètre : `onAjouterTache: (String, String) -> Unit` devient `onAjouterTache: (titre: String, categorie: String, quantite: String) -> Unit`.
* Remplacer le bloc `categorieCibleAjout?.let { categorie -> SaisieDialog(...) }` par :

```kotlin
categorieCibleAjout?.let { categorie ->
    ProduitEditionDialog(
        tache = null,
        categories = uiState.categories,
        categorieInitiale = categorie,
        onCheckErreurTitre = { titre ->
            val tacheExistante = uiState.taches.find {
                it.titre.lowercase().trim() == titre.lowercase().trim()
            }
            if (tacheExistante != null) "Cet élément existe déjà dans la catégorie ${tacheExistante.categorie}" else null
        },
        onValider = { titre, quantite, cat ->
            onAjouterTache(titre, cat, quantite)
            categorieCibleAjout = null
        },
        onFermer = { categorieCibleAjout = null }
    )
}
```

* L'état `categorieCibleAjout` et l'entrée « Ajout d'élément » du menu ⋮ restent inchangés.
* L'appel existant `ProduitEditionDialog(tache = tache, ...)` du mode modification ne change pas.
* Garder l'import de `SaisieDialog` (toujours utilisé dans ce fichier).

--------------------------
4. ui/navigation/ShopListNavHost.kt
--------------------------

`onAjouterTache = viewModel::ajouterTache` doit compiler tel quel avec la nouvelle signature. Ne le modifier que si la compilation l'exige.

==========================
COMPOSANTS UI
==========================

Tous déjà utilisés dans `ProduitEditionDialog` :

* `Dialog` + `Surface` (`DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true)`)
* `IconButton` + `Icon(Icons.Default.Close)`
* `ExposedDropdownMenuBox`, `ExposedDropdownMenu`, `DropdownMenuItem`
* `OutlinedTextField` (avec `textStyle` pour la couleur du champ Catégorie)
* `IconButton` + `Icon(Icons.Default.Clear)`
* `TextButton`

==========================
CAS LIMITES
==========================

* Nom vide ou uniquement des espaces : « Ajouter » désactivé, aucun message d'erreur affiché (comme en modification).
* Nom déjà existant, y compris dans une autre catégorie ou avec une casse différente : « Ajouter » désactivé, message « Cet élément existe déjà dans la catégorie X ».
* Nom existant d'un produit coché : même règle (bloqué).
* Changement de catégorie dans le menu puis ajout : le produit est créé dans la catégorie choisie, pas dans celle du menu ⋮ d'origine.
* Catégorie cible repliée : le produit est créé ; il n'apparaît que lorsqu'on déplie la catégorie (comportement actuel).
* Produits cochés masqués et catégorie cible sans produit non coché : la catégorie réapparaît avec le nouveau produit (comportement actuel de l'affichage).
* Catégorie vide : `obtenirOrdreMaxParCategorie` renvoie `null`, l'ordre vaut 0.
* Quantité saisie avec espaces seulement : enregistrée `""`.
* Retour Android pendant que le menu des catégories est ouvert : ferme d'abord le menu (comportement standard), puis le dialog au Retour suivant.
* Retour Android pendant que le clavier est ouvert : ferme d'abord le clavier (comportement standard).
* Erreur Room à l'insertion : Toast « Erreur lors de l'ajout du produit : … », pas de crash.

==========================
TESTS
==========================

Fichier `app/src/androidTest/java/com/jb/shoplist/ProduitEditionDialogTest.kt` :

* Les tests existants doivent continuer à passer sans modification (ils passent `tache` non nul et `onSupprimer`, toujours acceptés).
* Ajouter les tests du mode ajout :
  1. `testModeAjoutAfficheTitreEtBoutons` : `tache = null`, `categorieInitiale = "Frais"` ; vérifier la présence de « Nouveau produit », de « Ajouter », du texte « Frais », et l'absence de « Supprimer » et de « Valider ».
  2. `testModeAjoutAjouterDesactiveQuandNomVide` : « Ajouter » désactivé à l'ouverture.
  3. `testModeAjoutAppelleOnValiderAvecValeursNettoyees` : saisir «  Lait  » et « 1 L  », toucher « Ajouter » ; vérifier `onValider("Lait", "1 L", "Frais")`.
  4. `testModeAjoutChangementCategorie` : choisir une autre catégorie dans le menu, saisir un nom, toucher « Ajouter » ; vérifier que `onValider` reçoit la nouvelle catégorie.
  5. `testModeAjoutCroixFermetureAppelleOnFermer` : la croix appelle `onFermer` et pas `onValider`.

Pas de test unitaire supplémentaire (aucune fonction pure ajoutée).

==========================
CRITÈRES D'ACCEPTATION
==========================

* Le projet compile, sans nouveau warning.
* Aucun crash.
* Menu ⋮ > « Ajout d'élément » ouvre « Nouveau produit » avec la catégorie du menu préremplie, en couleur.
* Les catégories du menu déroulant s'affichent dans leur couleur, dans « Nouveau produit » et dans « Modifier le produit ».
* Ajout avec nom seul : produit créé sans quantité, en fin de catégorie, non coché.
* Ajout avec nom et quantité : la quantité s'affiche immédiatement dans la ligne produit.
* Ajout dans une autre catégorie que celle du menu : produit créé dans la catégorie choisie, en fin de catégorie.
* Nom en double : bouton désactivé et message affiché.
* Croix et Retour Android ferment sans rien créer ; un toucher hors du dialog ne le ferme pas.
* « Modifier le produit » fonctionne exactement comme avant (hors couleur des catégories).
* Tests de `ProduitEditionDialogTest` (anciens et nouveaux) au vert avec `./gradlew connectedAndroidTest`.

==========================
TRAVAIL ATTENDU DE GEMINI
==========================

Étapes, dans cet ordre, en compilant après chacune :

1. `ProduitEditionDialog.kt` : ajouter le mode ajout (signature, état initial, titre, boutons) et la couleur des catégories.
2. `TodoViewModel.kt` : nouvelle signature et nouveau calcul d'ordre de `ajouterTache`.
3. `TodoListScreen.kt` : type de `onAjouterTache` et remplacement du `SaisieDialog` d'ajout par `ProduitEditionDialog`. Vérifier `ShopListNavHost.kt`.
4. `ProduitEditionDialogTest.kt` : ajouter les tests du mode ajout.

Consignes :

* Créer uniquement les classes nécessaires (ici : aucune).
* Ne pas modifier d'autres modules ni d'autres fichiers que ceux listés (en particulier `SaisieDialog.kt`, `CategoriesScreen.kt`, `Entities.kt`, `Daos.kt`, `TodoDatabase.kt`).
* Ne pas refactoriser l'application, ne rien renommer, conserver le nommage en français.
* Compiler après chaque étape.

À la fin, résumer :

• fichiers créés

• fichiers modifiés

• points restant à faire
