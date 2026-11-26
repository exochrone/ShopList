# shopList : description fonctionnelle et technique

Document de référence décrivant l'application shopList telle qu'elle existe dans le dépôt `exochrone/shopList` (version `2026.04`, versionCode 2, branche `main`, dernier commit relu : `ea29cf5` « Correction typo dialogue restauration + test position barre »). Il sert de contexte pour concevoir et spécifier les évolutions.

**Dernière mise à jour : 26/11/2026 à 17:35 (heure de Paris).**

Modules implémentés depuis la première version de ce document : **MODULE 01** (quantités des produits), **MODULE 01b** (affichage de la quantité), **MODULE 02** (dialogue « Nouveau produit » harmonisé, catégories en couleur dans les dialogues produit), **MODULE 03** (tri alphabétique respectant les lettres accentuées), **MODULE 04** (unicité des produits par catégorie), **MODULE 05** (fermeture automatique de la recherche, bouton œil masqué en recherche), **MODULE 05b** (fermeture de la recherche en touchant sous la fin de la liste), **MODULE 06** (recherche sur le nom des produits seulement, dépliage automatique des catégories en recherche), **MODULE 07** (écran Options), **MODULE 07b** (import, sauvegarde, restauration et choix de position de la barre dans Options) et **MODULE 07c** (menu principal allégé, position haut / bas de la barre de l'écran principal, retour à la liste après import ou restauration). Leurs spécifications sont dans le dossier `/specs` du dépôt.

---

## 1. Présentation

shopList est une application Android personnelle de **liste de courses persistante**, organisée par catégories.

Son principe central : **le catalogue des produits reste en permanence dans l'application.** On ne recrée pas une liste à chaque course : on coche et décoche les produits existants selon les besoins. Cela évite de ressaisir les mêmes articles d'une semaine à l'autre.

* Un produit **non coché** est à acheter.
* Un produit **coché** est acheté ou pas nécessaire pour le moment. Il reste dans la liste, grisé et barré, et peut être masqué.
* Chaque produit peut avoir une **quantité** facultative en texte libre (`2`, `2 boîtes`, `200 g`…), conservée quand on coche ou décoche.

L'application gère une seule liste (titre par défaut « Courses », renommable). Elle est utilisée au quotidien par son auteur sur un Samsung Galaxy S23+.

Vocabulaire : l'application est née comme une todo list. Dans le code et dans une partie de l'interface, un produit s'appelle une **tâche** (`TacheEntity`, « Tâche supprimée »…). Les dialogues « Nouveau produit » et « Modifier le produit », eux, parlent de « produit ». Dans ce document, « produit » et « tâche » désignent la même chose.

---

## 2. Description fonctionnelle

### 2.1 Écran principal : la liste

**Barre de titre et d'options**, de gauche à droite :

* Titre de la liste.
* **Annuler le dernier cochage** (icône flèche retour) : actif seulement s'il existe des cochages à annuler.
* **Rechercher** (loupe).
* **Afficher / masquer les produits cochés** (icône œil). Le réglage est mémorisé. Ce bouton n'est pas affiché en mode recherche (voir 2.5).
* **Menu** (icône hamburger) : Partager la liste, Tout cocher, Options.

**Position de la barre** (réglage de l'écran Options, voir 2.11) :

* **Haut** (par défaut) : barre en haut de l'écran.
* **Bas** : barre en bas de l'écran, au-dessus de la barre de navigation Android ; la liste occupe l'espace au-dessus et commence juste sous la barre d'état. En recherche, la barre remonte au-dessus du clavier. Le menu hamburger s'ouvre vers le haut ; les snackbars de l'écran principal s'affichent au-dessus de la barre. Le contenu de la barre et ses comportements sont identiques dans les deux positions.

**Contenu** : les catégories dans l'ordre défini par l'utilisateur, chacune suivie de ses produits.

* **En-tête de catégorie** : chevron (replié / déplié, selon l'état affiché, qui peut différer de l'état en base pendant une recherche, voir 2.5), nom de la catégorie dans sa couleur, nombre de produits non cochés entre parenthèses, bouton **⋮** (options de la catégorie).
  * Toucher l'en-tête replie ou déplie la catégorie (état mémorisé en base ; cas de la recherche en 2.5).
  * Le bouton ⋮ ouvre un menu :
    * « N élément(s) » (information, non cliquable) ;
    * **Ajout d'élément** : ouvre le dialogue « Nouveau produit » avec cette catégorie présélectionnée (voir 2.2) ;
    * **Tri alphabétique** : trie les produits de la catégorie selon l'ordre alphabétique français (lettres accentuées et ligature « Œ » rangées avec leur lettre de base, casse ignorée : « Échalotes » et « Épinards » se placent à la lettre E) ;
    * **Modifier la catégorie** : renommer (même règle d'unicité qu'en 2.6) ;
    * **Choix de la couleur** : palette des 20 couleurs ;
    * **Supprimer** (icône rouge) : confirmation « N tâche(s) dans la catégorie X seront supprimées avec la catégorie. Voulez-vous continuer ? » (Oui / Non), demandée même si la catégorie est vide ; puis snackbar avec **Annuler**.
* **Ligne produit** : case à cocher (de la couleur de la catégorie), puis le texte du produit.
  * Produit non coché avec quantité : le nom (14 sp), un petit espace, puis la quantité en **12 sp, dans la couleur de la catégorie**. Nom et quantité forment un seul texte qui passe à la ligne naturellement ; la quantité suit le dernier mot du nom et n'est jamais coupée en deux (« 2 boîtes » reste d'un bloc).
  * Produit coché : case et nom grisés, nom barré, **quantité masquée** (elle reste enregistrée et réapparaît au décochage).
  * Toucher la case : coche ou décoche.
  * Toucher le texte (nom ou quantité) : ouvre le dialogue « Modifier le produit ».
  * Appui long puis glisser : déplace le produit, y compris vers une autre catégorie (glisser-déposer). Si le produit est lâché dans une autre catégorie qui contient déjà un produit du même nom, le déplacement est annulé : toute la liste revient à son état d'avant le glisser, et un Toast affiche « Ce produit existe déjà dans la catégorie X : déplacement annulé. ». Seule la catégorie au moment du lâcher compte (traverser une telle catégorie pendant le glisser est sans effet).
* Un séparateur horizontal termine la liste.

**Affichage des catégories** :

* Produits cochés affichés : toutes les catégories sont affichées, même vides.
* Produits cochés masqués : seules les catégories ayant au moins un produit non coché sont affichées.

### 2.2 Ajouter un produit

Menu ⋮ d'une catégorie, « Ajout d'élément », puis dialogue **« Nouveau produit »**, construit comme « Modifier le produit » (2.3) :

* **Barre de titre** : « Nouveau produit » et une **croix de fermeture** en haut à droite.
* **Catégorie** : liste déroulante, présélectionnée sur la catégorie du menu utilisé ; elle permet d'ajouter le produit dans une autre catégorie. Les noms de catégories (liste et champ) s'affichent dans leur couleur.
* **Nom du produit** : champ vide, avec une croix d'effacement (visible quand le champ n'est pas vide).
* **Quantité (facultatif)** : champ vide, avec une croix d'effacement.
* **Bouton** : **Ajouter**, seul, aligné à droite. Il n'y a ni bouton Annuler ni bouton Supprimer.

Comportement :

* Le focus est sur le nom à l'ouverture. Clavier : « Suivant » passe du nom à la quantité, « OK » ajoute.
* Le clavier propose une majuscule en début de saisie pour le nom et la quantité. Le texte saisi n'est pas modifié par l'application.
* **Ajouter** est désactivé si le nom est vide ou déjà utilisé : le nom doit être unique **dans la catégorie choisie** (comparaison sans tenir compte de la casse ni des espaces en début et fin, accents comptés), sinon message « Ce produit existe déjà dans cette catégorie. ». Un même nom peut exister dans plusieurs catégories différentes. Le contrôle est réévalué en direct quand le nom ou la catégorie choisie change.
* Le produit est créé avec le nom et la quantité nettoyés (espaces de début et fin supprimés ; quantité vide si non renseignée), non coché, **en fin de la catégorie choisie**. Le dialogue se ferme, sans Toast ni snackbar.
* **Fermer sans ajouter** : croix en haut à droite, ou geste / bouton Retour du système Android. Un toucher hors du dialogue ne le ferme pas.

### 2.3 Modifier ou supprimer un produit

Toucher le texte d'un produit ouvre le dialogue **« Modifier le produit »** :

* **Barre de titre** : « Modifier le produit » et une **croix de fermeture** en haut à droite.
* **Catégorie** : liste déroulante affichant la catégorie actuelle ; elle permet de déplacer le produit dans une autre catégorie. Les noms de catégories (liste et champ) s'affichent dans leur couleur.
* **Nom du produit** : champ prérempli, avec une croix d'effacement (visible quand le champ n'est pas vide).
* **Quantité (facultatif)** : champ prérempli (vide si aucune), avec une croix d'effacement.
* **Boutons** sur une même ligne : **Supprimer** (rouge) à gauche, **Valider** à droite. Il n'y a pas de bouton Annuler.

Comportement :

* Le focus est sur le nom à l'ouverture. Clavier : « Suivant » passe du nom à la quantité, « OK » valide.
* Le clavier propose une majuscule en début de saisie pour le nom et la quantité. Le texte saisi n'est pas modifié par l'application.
* **Valider** est désactivé si le nom est vide ou déjà utilisé par un autre produit de la catégorie choisie (même règle d'unicité que pour l'ajout, message « Ce produit existe déjà dans cette catégorie. »). Le produit modifié n'est pas comparé à lui-même (changer seulement la casse est accepté). Choisir une catégorie qui contient déjà ce nom désactive « Valider ».
* À la validation, le nom et la quantité sont nettoyés (espaces de début et fin supprimés). Si rien n'a changé, le dialogue se ferme sans écriture en base.
* **Changement de catégorie** : le produit est placé en fin de la catégorie choisie ; son état coché et sa quantité sont conservés.
* **Fermer sans enregistrer** : croix en haut à droite, ou geste / bouton Retour du système Android. Un toucher hors du dialogue ne le ferme pas.
* **Supprimer** : suppression immédiate, puis snackbar « Tâche supprimée » avec action **Annuler** qui restaure le produit (quantité comprise).
* La quantité d'un produit coché reste visible et modifiable dans ce dialogue.

### 2.4 Cocher, décocher, annuler

* Cocher ou décocher un produit ne modifie jamais sa quantité.
* Chaque cochage ou décochage est empilé dans une **pile d'annulation** (10 actions maximum). Le bouton « Annuler le dernier cochage » de la barre du haut dépile une action à la fois.
* La pile est vidée par les opérations qui modifient la structure (suppression de produit ou de catégorie, import CSV, tout cocher, restauration).
* **Tout cocher** (menu) : coche tous les produits, réaffiche les produits cochés, puis snackbar « N tâche(s) cochée(s) » avec action **Annuler** qui décoche les produits concernés.

### 2.5 Recherche

* La loupe remplace le titre par un champ de recherche (focus automatique) et affiche une **flèche retour** à gauche pour quitter la recherche.
* Filtre en direct, **sans tenir compte des accents ni de la casse**, sur le **nom du produit uniquement** (le nom de la catégorie n'est pas comparé, la quantité non plus). Une catégorie n'apparaît pendant une recherche que si elle contient au moins un produit correspondant, et elle n'affiche que ces produits.
* En recherche, les produits cochés sont toujours inclus, et le glisser-déposer est désactivé.
* Un produit présent dans plusieurs catégories apparaît sous chacune d'elles.
* **Dépliage automatique** : quand un terme est saisi, une catégorie qui contient au moins un résultat s'affiche **dépliée**, même si elle est repliée en base. Ce dépliage n'est jamais écrit en base : dès qu'elle n'a plus de résultat (le terme change) ou à la sortie de la recherche, elle reprend son état en base. Exemple : « Boulangerie » repliée contient « Pain » et « Pain de mie » ; « pain » la déplie et affiche les deux produits ; « pain au » la fait disparaître (aucun résultat) ; elle est repliée à la sortie de la recherche.
* **Toucher un en-tête quand un terme est saisi** : l'état affiché est inversé et la base est alignée dessus ; **la recherche reste ouverte**. Pour une catégorie dépliée automatiquement (repliée en base), le premier toucher la replie à l'écran sans écriture en base (la base dit déjà « repliée »), le suivant la déplie et l'enregistre. Après un toucher, le dépliage automatique ne s'applique plus à cette catégorie jusqu'à la sortie de la recherche.
* Une croix, visible seulement quand le champ contient du texte, efface la recherche. Le **bouton œil n'est pas affiché** en recherche (il n'aurait pas d'effet, puisque les cochés sont toujours inclus) ; il réapparaît à la sortie avec son état d'avant. Le bouton d'annulation du cochage et le menu hamburger ne sont pas affichés non plus.
* **Quitter la recherche** (le terme est effacé, le clavier se ferme) :
  * flèche retour de la barre, ou Retour système ;
  * **fermeture automatique** quand l'utilisateur :
    * touche le texte d'un produit : la recherche se ferme, puis le dialogue « Modifier le produit » s'ouvre ;
    * touche un en-tête de catégorie **alors que le champ de recherche est vide** : la recherche se ferme, puis la catégorie est repliée ou dépliée en base ;
    * choisit une action dans le menu ⋮ d'une catégorie (Ajout d'élément, Tri alphabétique, Modifier la catégorie, Choix de la couleur, Supprimer) : la recherche se ferme, puis l'action se déroule normalement ;
    * touche la zone vide **sous le séparateur qui termine la liste** (uniquement si ce séparateur est visible à l'écran).
* Dans les trois premiers cas automatiques, la liste complète défile si besoin pour garder visible le produit ou l'en-tête touché (vers l'en-tête de sa catégorie si le produit n'est plus affiché, par exemple un produit coché alors que les cochés sont masqués).
* Ne ferment pas la recherche : toucher un en-tête de catégorie quand un terme est saisi, cocher ou décocher un produit (pour cocher plusieurs résultats d'affilée), ouvrir puis refermer le menu ⋮ sans choisir d'action, toucher entre deux lignes ou dans les marges, faire défiler la liste.

### 2.6 Gérer les catégories

Écran « Gestion des catégories » (bouton « Gérer les catégories » de l'écran Options ; la flèche retour ramène à Options), avec flèche retour et bouton « + » dans la barre du haut. Il complète le menu ⋮ des en-têtes de l'écran principal.

Chaque catégorie affiche : poignée de déplacement, nom (dans sa couleur), nombre de produits (« Vide » ou « N tâche(s) »), puis trois boutons :

* **Trier A→Z** : trie alphabétiquement les produits de la catégorie (même règle que le menu ⋮, voir 2.1). Après usage, le bouton de cette catégorie reste grisé et inactif jusqu'à la sortie de l'écran (mémorisation locale `categoriesTriees`, non persistée).
* **Palette** : choix de la couleur dans une palette fixe de 20 couleurs.
* **Supprimer** : suppression directe si la catégorie est vide ; sinon, confirmation « N tâche(s) dans la catégorie X seront supprimées avec la catégorie ». Snackbar avec **Annuler** qui restaure la catégorie et ses produits.

Autres actions :

* Glisser-déposer pour réordonner les catégories.
* Toucher le nom : dialogue « Modifier la catégorie » (renommer, ou supprimer). Le nom de catégorie est unique (sans tenir compte de la casse). Le renommage met à jour tous les produits de la catégorie.
* « + » : nouvelle catégorie, ajoutée en fin de liste, blanche par défaut.

### 2.7 Partager la liste

Écran « Partager la liste » (menu) :

* Aperçu du texte à partager, modifiable à la main. Après une modification manuelle, un bouton permet de revenir au texte automatique.
* Options : **Masquer les catégories** et **Afficher les éléments cochés** (par défaut, seuls les produits non cochés sont partagés).
* Format : nom de la catégorie, puis ses produits indentés de 4 espaces (sans indentation si les catégories sont masquées). Les catégories sans produit partagé sont omises. Avec « Masquer les catégories », un produit présent dans plusieurs catégories apparaît plusieurs fois.
* Chaque produit est suivi de sa quantité, séparée par ` : ` (`Aubergine : 3`, `Farine : 1kg`) ; un produit sans quantité apparaît seul (`Sel`). Les produits cochés partagés gardent leur quantité.
* **Partager** : ouvre le sélecteur de partage Android (texte brut).
* **Export CSV** : voir 2.8.

### 2.8 Import et export CSV

**Export** (depuis l'écran Partager) : fichier `shopList-<titre>-<date>.csv`, UTF-8 avec BOM, séparateur `;`, sans en-tête.

* Une ligne `categorie;produit;quantite` par produit, cochés compris ; la 3e colonne est vide si le produit n'a pas de quantité (`Frais;Sel;`).
* Une ligne `categorie;` pour chaque catégorie vide.
* Les valeurs contenant `;`, `"` ou un retour à la ligne sont placées entre guillemets.

**Import** (bouton « Importer une liste » de l'écran Options) : après un message d'explication, choix d'un fichier CSV. Après un import réussi, l'application revient sur l'écran principal ; en cas d'erreur ou d'annulation, on reste sur Options.

* Format accepté : `categorie;produit` ou `categorie;produit;quantite`, UTF-8, sans en-tête ; catégorie et produit obligatoires, quantité facultative, colonnes suivantes ignorées.
* Les champs entre guillemets sont correctement lus (un `;` entre guillemets ne sépare pas, `""` donne `"`).
* Les catégories inconnues sont créées en fin de liste.
* Un produit n'est créé que s'il n'existe pas déjà un produit de **même nom dans la même catégorie** ; il est créé avec la quantité lue. Le nom du produit est comparé sans tenir compte de la casse ni des espaces en début et fin (règle `nomsProduitsIdentiques`), le nom de catégorie est comparé exactement. Les produits créés plus haut dans le même fichier sont pris en compte (`Frais;Lait;1` puis `Frais;lait;2` donne un seul produit « Lait » de quantité `2`).
* Produit déjà existant : sa quantité est remplacée seulement si le fichier en fournit une non vide ; son état coché n'est pas modifié.
* L'import est fait dans une transaction ; un message confirme la réussite ou l'erreur.
* Le texte d'explication du dialogue d'import mentionne encore deux colonnes seulement.

### 2.9 Sauvegarde complète et restauration

**Sauvegarde complète** (bouton de l'écran Options ; on reste sur Options) : fichier JSON `shopList-Backup-<aaMMjj>.json` (format version 2) contenant le titre de la liste, les paramètres, toutes les catégories (nom, ordre, couleur, repliée) et tous les produits (titre, catégorie, cochée, ordre, quantité). Les paramètres sauvegardés sont uniquement `titreListe`, `afficherFaites` et `positionBarre` (liste écrite en dur dans `construireJsonSauvegarde`).

**Restaurer une sauvegarde** (bouton de l'écran Options), en trois temps :

1. Avertissement : la restauration remplacera toutes les données, action irréversible.
2. Après choix du fichier, résumé : titre de la liste, nombre de catégories et de produits, date de la sauvegarde. Confirmation par « Restaurer ».
3. Remplacement complet des données dans une transaction. En cas de fichier invalide (JSON incorrect, version non supportée, champ manquant, catégorie en doublon, produit rattaché à une catégorie inconnue), un dialogue « Restauration impossible » affiche la raison.

Après une restauration réussie, l'application revient sur l'écran principal ; en cas d'erreur ou d'annulation, on reste sur Options.

Les sauvegardes en version 1 (antérieures aux quantités) restent restaurables : les produits ont alors une quantité vide. Une sauvegarde sans paramètre `positionBarre` (antérieure au MODULE 07c) remet la barre en position « Haut ».

### 2.10 Divers

* **Renommer la liste** (écran Options, zone « Nom de la liste ») : change le titre affiché.
* **Premier lancement** (base vide) : création d'une catégorie « Personnel » avec trois tâches d'exemple (héritage de la todo list d'origine).
* Les messages de résultat (import, export, sauvegarde, restauration, erreur d'ajout ou de modification d'un produit) s'affichent en Toast ; les actions annulables en snackbar.
* Application en **portrait uniquement**, **thème sombre unique**.
* Application entièrement **locale** : aucun accès réseau.

### 2.11 Options

Écran « Options » (entrée « Options » du menu principal, en dernier). Barre du haut : flèche retour (vers l'écran principal) et titre « Options ». Contenu, de haut en bas :

* **Nom de la liste** : libellé « Nom de la liste » puis le nom actuel (2 lignes maximum, points de suspension). Un toucher ouvre le dialogue « Renommer la liste » (champ prérempli, texte grisé « Nouveau titre », boutons « Valider » / « Annuler » ; valider un champ vide ne renomme pas).
* **Position de la barre de titre et d'options** : deux boutons segmentés « Haut » / « Bas » (« Haut » par défaut). Le choix est enregistré immédiatement (paramètre `positionBarre`) et appliqué à la barre de l'écran principal (voir 2.1). Il ne concerne pas les barres des autres écrans.
* Boutons pleine largeur, dans cet ordre :
  * **Gérer les catégories** : ouvre l'écran « Gestion des catégories » (2.6), dont le retour ramène à Options ;
  * **Importer une liste** (2.8) ;
  * **Sauvegarde complète** (2.9) ;
  * **Restaurer une sauvegarde** (2.9).
* En bas de l'écran, centré : « shopList v2026.04 » (nom de version de l'application).

---

## 3. Description technique

### 3.1 Environnement

| Élément | Valeur |
|---|---|
| Langage | Kotlin 2.0.21 |
| UI | Jetpack Compose, BOM 2024.12.01, Material 3, icônes étendues |
| Build | AGP 8.7.3, Gradle 8.10.2, KSP 2.0.21-1.0.28, catalogue `gradle/libs.versions.toml` |
| SDK | compileSdk 35, targetSdk 35, minSdk 31, Java 11 |
| Persistance | Room 2.6.1 (KSP) |
| Navigation | Navigation Compose 2.8.4 |
| Glisser-déposer | `sh.calvin.reorderable:reorderable` 2.4.0 |
| JSON | `org.json` (inclus dans Android) |
| Réseau | aucun : pas de permission `INTERNET` dans le manifeste, aucune bibliothèque HTTP |
| Package | `com.jb.shoplist` |
| Release | minification activée (R8, ProGuard par défaut) |

Points de configuration Gradle à ne pas casser (leçons des premières versions) :

* Le plugin `kotlin-compose` est utilisé ; le build racine déclare tous les plugins avec `apply false`.
* `android.useAndroidX=true` est obligatoire dans `gradle.properties`.
* Les BOM Compose 2026.x ne sont pas compatibles avec Kotlin 2.0.21 ; AGP 9.x n'est pas compatible avec la configuration actuelle.

### 3.2 Architecture

Architecture **MVVM simple, une seule activité**, sans couche Repository ni injection de dépendances :

```
MainActivity (ShopListTheme + TodoApp)
 └─ TodoApp : Scaffold + SnackbarHost, collecte des StateFlow et des événements
     └─ ShopListNavHost (4 routes)
         ├─ TodoListScreen      (route "liste", départ)
         ├─ OptionsScreen       (route "options", depuis le menu de la liste)
         ├─ CategoriesScreen    (route "categories", depuis Options)
         └─ PartagerScreen      (route "partager", depuis le menu de la liste)

TodoViewModel (AndroidViewModel, unique, partagé par les 4 écrans)
 └─ TacheDao / CategorieDao / ParametreDao
     └─ TodoDatabase (Room, singleton)
```

* Les écrans sont **sans état métier** : ils reçoivent l'état (`TodoUiState` et autres flux) et des callbacks vers le ViewModel. Seul l'état d'interface local (dialogues ouverts, mode recherche, saisies) vit dans les composables.
* Le ViewModel lit et écrit directement via les DAO, dans `viewModelScope`. Les opérations multi-tables utilisent `database.withTransaction { }`.

### 3.3 Arborescence des sources

```
app/src/main/java/com/jb/shoplist/
├── MainActivity.kt            Activité, composable racine TodoApp (Toast, snackbar)
├── Entities.kt                Entités Room
├── Daos.kt                    DAO Room
├── TodoDatabase.kt            Base Room (singleton) et migration 3 → 4
├── TodoViewModel.kt           ViewModel unique, états, sauvegarde, CSV, enum PositionBarre
├── Formatage.kt               Fonctions pures : libellé avec quantité, découpage CSV, comparateur alphabétique, état replié affiché
└── ui/
    ├── navigation/
    │   ├── Routes.kt          Constantes de routes
    │   └── ShopListNavHost.kt NavHost et câblage écrans ↔ ViewModel
    ├── screens/
    │   ├── liste/
    │   │   ├── TodoListScreen.kt   Écran principal, menu ⋮ des catégories, dialogues produit et catégorie, recherche, position de la barre
    │   │   ├── TodoListTopBar.kt   Barre de titre et d'options (haut ou bas), recherche, menu
    │   │   └── LigneAffichee.kt    Modèle des lignes affichées (en-tête / produit)
    │   ├── options/
    │   │   └── OptionsScreen.kt    Écran Options : nom de la liste, position de la barre, catégories, import, sauvegarde, restauration, version
    │   ├── categories/
    │   │   └── CategoriesScreen.kt Gestion des catégories
    │   └── partage/
    │       └── PartagerScreen.kt   Partage texte et export CSV
    ├── components/
    │   ├── TacheItem.kt             Ligne produit (nom + quantité)
    │   ├── ProduitEditionDialog.kt  Dialogues « Nouveau produit » et « Modifier le produit »
    │   ├── SaisieDialog.kt          Dialogue générique de saisie d'un texte
    │   └── ColorPickerDialog.kt     Choix de couleur
    └── theme/
        ├── Color.kt, Type.kt, Theme.kt   Thème sombre et couleurs personnalisées
        └── PaletteCategories.kt          Palette des 20 couleurs de catégorie

app/schemas/com.jb.shoplist.TodoDatabase/  Schémas Room exportés (3.json, 4.json)
app/src/test/java/com/jb/shoplist/FormatageTest.kt               Tests unitaires de Formatage.kt
app/src/androidTest/java/com/jb/shoplist/ProduitEditionDialogTest.kt  Tests Compose du dialogue (modes ajout et modification)
app/src/androidTest/java/com/jb/shoplist/OptionsScreenTest.kt        Tests Compose de l'écran Options
specs/                                                            Spécifications des modules (.md)
```

### 3.4 Modèle de données

Base Room `todo_database`, **version 4**, `exportSchema = true` (dossier `app/schemas`, versionné).

```kotlin
@Entity(tableName = "taches")
data class TacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titre: String,
    val categorie: String,   // nom de la catégorie (pas de clé étrangère)
    val faite: Boolean = false,
    val ordre: Int = 0,
    @ColumnInfo(defaultValue = "")
    val quantite: String = ""   // texte libre, "" si aucune
)

@Entity(tableName = "categories")
data class CategorieEntity(
    @PrimaryKey val nom: String,
    val ordre: Int = 0,
    val couleur: Long = 0xFFFFFFFF,  // ARGB
    val repliee: Boolean = false
)

@Entity(tableName = "parametres")
data class ParametreEntity(
    @PrimaryKey val cle: String,
    val valeur: String
)
```

**Migrations** déclarées dans `TodoDatabase.kt` et enregistrées par `.addMigrations(...)` :

* `MIGRATION_3_4` : `ALTER TABLE taches ADD COLUMN quantite TEXT NOT NULL DEFAULT ''`.

Remarques :

* Le lien produit → catégorie se fait **par le nom** de la catégorie, sans clé étrangère. Renommer une catégorie = insérer la nouvelle, mettre à jour les produits, supprimer l'ancienne (en transaction).
* `ordre` des produits : un ordre **global** (`ORDER BY ordre` sur toute la table) ; l'affichage regroupe ensuite par catégorie. Pour placer un produit en fin de catégorie (ajout, changement de catégorie), le code utilise `obtenirOrdreMaxParCategorie(categorie) + 1`.
* Paramètres utilisés : `titreListe`, `afficherFaites`, `positionBarre` (`"HAUT"` ou `"BAS"`, absent = `HAUT` ; ajouté au MODULE 07c sans migration, la table `parametres` étant clé / valeur).

**DAO principaux** :

* `TacheDao` : `observerToutes(): Flow`, `inserer` (REPLACE), `mettreAJour`, `mettreAJourToutes`, `supprimer`, `renommerCategorie`, `compterParCategorie`, `trouverParTitreEtCategorie`, `obtenirOrdreMax`, `obtenirOrdreMaxParCategorie`, `supprimerParCategorie`, `supprimerToutes`.
* `CategorieDao` : `observerToutes(): Flow`, `inserer` (IGNORE), `mettreAJour`, `mettreAJourToutes`, `supprimer`, `supprimerToutes`.
* `ParametreDao` : `observer(cle): Flow<String?>`, `enregistrer` (REPLACE), `supprimerTous`.

### 3.5 ViewModel et états

`TodoViewModel` expose :

* `uiState: StateFlow<TodoUiState>` : combinaison des flux Room (produits, catégories, `titreListe`, `afficherFaites`) et du terme de recherche, via `combine` + `stateIn(WhileSubscribed(5000))`.

```kotlin
data class TodoUiState(
    val taches: List<TacheEntity> = emptyList(),
    val categories: List<CategorieEntity> = emptyList(),
    val titreListe: String = "Courses",
    val afficherFaites: Boolean = true,
    val termeRecherche: String = ""
)
```

* `actionAnnulable: StateFlow<ActionAnnulable?>` : dernière action annulable par snackbar (`SuppressionTache`, `SuppressionCategorie`, `ToutCocher`), chacune avec son message.
* `pileCochages: StateFlow<List<ActionCochage>>` : pile d'annulation des cochages (`tacheId`, `etatPrecedent`), limitée à 10.
* `etatRestauration: StateFlow<EtatRestauration>` : `Inactif`, `FichierLu(ContenuSauvegarde)`, `Erreur(message)`.
* `evenements: SharedFlow<String>` : messages ponctuels affichés en Toast par `TodoApp`.
* `positionBarre: StateFlow<PositionBarre>` : lu depuis le paramètre `positionBarre` (`map` puis `stateIn(SharingStarted.Eagerly, PositionBarre.HAUT)`, valeur inconnue ou absente = `HAUT`). Hors de `TodoUiState`. Collecté dans `ShopListNavHost`.
* `operationReussie: SharedFlow<Unit>` : émis juste après le Toast « Importation réussie ! » (`importerDepuisCsv`) et après « Restauration réussie » (`confirmerRestauration`), jamais en cas d'erreur. Sert au retour vers la liste depuis Options.

`enum class PositionBarre { HAUT, BAS }` est déclarée dans `TodoViewModel.kt`, à côté de `EtatRestauration`.

Principales fonctions publiques : `ajouterTache`, `basculerFait`, `annulerDernierCochage`, `modifierProduit`, `supprimerTache`, `reordonnerTaches`, `trierTachesParAlpha`, `toutCocher`, `ajouterCategorie`, `supprimerCategorie`, `reordonnerCategories`, `renommerCategorie`, `modifierCouleurCategorie`, `toggleCategorieRepliee`, `modifierTitreListe`, `setAfficherFaites`, `setPositionBarre`, `setTermeRecherche`, `importerDepuisCsv`, `exporterCsv`, `sauvegardeComplete`, `lireFichierSauvegarde`, `confirmerRestauration`, `annulerRestauration`, `consommerErreurRestauration`, `annulerDerniereAction`, `consommerAction`.

* `ajouterTache(titre, categorie, quantite)` : nettoie titre et quantité, place le produit en fin de catégorie (`obtenirOrdreMaxParCategorie + 1`), l'insère non coché. Les erreurs sont capturées et signalées par `evenements` (« Erreur lors de l'ajout du produit : … »).
* `modifierProduit(tache, nouveauTitre, nouvelleQuantite, nouvelleCategorie)` : nettoie titre et quantité, calcule le nouvel `ordre` via `obtenirOrdreMaxParCategorie` si la catégorie change, puis enregistre en un seul `mettreAJour`. Les erreurs sont capturées et signalées par `evenements`.
* `construireJsonSauvegarde()` : parcourt une liste de clés écrite en dur (`listOf("titreListe", "afficherFaites", "positionBarre")`) pour remplir le tableau `parametres` de la sauvegarde.

**Fonctions utilitaires** (`Formatage.kt`, testées par `FormatageTest`) :

* `libelleAvecQuantite(titre, quantite)` : `"titre : quantite"`, ou `"titre"` si la quantité est vide (utilisée par le partage).
* `parserLigneCsv(ligne)` : découpe une ligne CSV séparée par `;` en respectant les guillemets (utilisée par l'import).
* `comparateurAlphabetique()` : `Comparator<String>` basé sur `java.text.Collator.getInstance(Locale.FRENCH)` en force `Collator.TERTIARY` (nouvelle instance à chaque appel). Utilisé par `trierTachesParAlpha` via `sortedWith(compareBy(comparateurAlphabetique()) { it.titre })`.
* `nomsProduitsIdentiques(a, b)` : vrai si `a.trim().lowercase() == b.trim().lowercase()` (accents comptés). Règle unique d'unicité des produits, utilisée par les dialogues produit, le glisser-déposer et l'import CSV.
* `categorieReplieeAffichee(replieeEnBase, rechercheActive, aDesResultats, toucheeEnRecherche)` : état replié à afficher ; vaut `false` (déplié) si recherche active, catégorie avec résultats et non touchée pendant la recherche, sinon `replieeEnBase`. Utilisée par le calcul des lignes de `TodoListScreen`.

### 3.6 Interface

* **Construction des lignes** : `TodoListScreen` calcule une `List<LigneAffichee>` (`Header` ou `ItemTache`, avec clé unique stable `"header_<nom>"` / `"tache_<id>"` ; `ItemTache` porte la couleur de la catégorie) à partir de l'état, en appliquant le filtre des cochés, la recherche et le repli des catégories. Une seule `LazyColumn` affiche ces lignes.
  * En recherche, `tachesFinales` ne retient que les produits dont `titre.sansAccents().lowercase()` contient le terme normalisé (le nom de catégorie n'est pas comparé).
  * `LigneAffichee.Header(categorie, nombreNonCoches, replieeAffichee: Boolean = categorie.repliee)` : `replieeAffichee` est calculé par `categorieReplieeAffichee(...)` ; les `ItemTache` ne sont ajoutés que si `!replieeAffichee`. Le chevron et sa description suivent `replieeAffichee`.
* **Recherche** : `modeRecherche` est un état local de `TodoListScreen` ; le terme passe par `TodoViewModel.setTermeRecherche`. La normalisation sans accents est faite par l'extension privée `String.sansAccents()` (`Normalizer`, forme NFD) en bas de `TodoListScreen.kt`.
  * `categoriesToucheesEnRecherche: Set<String>` (état local, `remember`) : noms des catégories dont l'en-tête a été touché pendant la recherche en cours ; clé du `remember` de `lignes`, vidé par `quitterRecherche`.
  * Toucher d'un en-tête : si recherche active (`modeRecherche` et terme non vide), appel de `onToggleCategorieRepliee` seulement si `ligne.replieeAffichee == ligne.categorie.repliee`, puis ajout du nom à `categoriesToucheesEnRecherche`, sans fermer la recherche ; sinon `quitterRecherche(CibleDefilement(...))` si `modeRecherche`, puis `onToggleCategorieRepliee`.
  * `quitterRecherche(cible: CibleDefilement? = null)` (fonction locale) retire le focus (`LocalFocusManager`), masque le clavier (`LocalSoftwareKeyboardController`), remet `modeRecherche = false`, efface le terme, vide `categoriesToucheesEnRecherche` et mémorise la cible de défilement. Elle est utilisée par le `BackHandler`, la flèche retour (`onToggleSearchMode`), et, uniquement si `modeRecherche`, par le toucher d'un en-tête avec un terme vide, les 5 actions du menu ⋮ et `onClickTache` (avant leur action habituelle). `onCheckedChange` de `TacheItem` n'est pas concerné.
  * `private data class CibleDefilement(cle: String, cleRepli: String?)` : clé de la ligne à garder visible, avec repli sur l'en-tête de la catégorie pour un produit. Un `LaunchedEffect(lignes, cibleDefilement)` attend que le terme soit vide, cherche l'index de la clé (puis de la clé de repli), appelle `lazyListState.scrollToItem(index)` seulement si l'élément n'est pas entièrement visible, puis remet la cible à `null`.
  * Toucher sous la liste : en recherche seulement, la `LazyColumn` reçoit `Modifier.pointerInput(Unit) { detectTapGestures(onTap = …) }` ; si le dernier item visible est le dernier de la liste (séparateur final) et que le toucher est sous son bas (`offset + size`), `quitterRecherche()` est appelée sans cible. Les touchers consommés par les lignes et les défilements ne déclenchent rien.
  * `TodoListTopBar` n'affiche en recherche que la flèche retour, le champ et la croix d'effacement (plus de bouton œil).
* **Position de la barre** : `TodoListScreen` reçoit `positionBarre: PositionBarre`. La barre est définie une fois (`val barre: @Composable () -> Unit = { TodoListTopBar(..., enBas = positionBarre == PositionBarre.BAS) }`) puis placée par `Scaffold(topBar = { if (HAUT) barre() }, bottomBar = { if (BAS) barre() })`. Sans `topBar`, le `innerPadding` contient la hauteur de la barre d'état.
  * `TodoListTopBar(…, enBas: Boolean = false)` : seul effet de `enBas`, `windowInsets` de la `TopAppBar` = `WindowInsets.navigationBars.union(WindowInsets.ime)` en bas, `TopAppBarDefaults.windowInsets` en haut.
  * Menu hamburger de `TodoListTopBar` : trois `DropdownMenuItem` sans icône (« Partager la liste », « Tout cocher », « Options »).
* **Écran Options** (`OptionsScreen`) : composable sans état métier.
  * Signature : `OptionsScreen(titreListe, versionApp, onModifierTitreListe, onOuvrirCategories, positionBarre, onChangerPositionBarre, onImporterCsv, onSauvegardeComplete, etatRestauration, onLireFichierSauvegarde, onConfirmerRestauration, onAnnulerRestauration, onConsommerErreurRestauration, onRetour)`.
  * `Scaffold` + `TopAppBar` (`surfaceVariant`), `Column` : zone cliquable du nom, `SingleChoiceSegmentedButtonRow` (deux `SegmentedButton`), quatre `OutlinedButton`, `Spacer(weight(1f))`, texte de version.
  * États locaux : `afficherDialogRenommer`, `afficherDialogImport`, `afficherDialogPreRestauration`. Contient les trois lanceurs SAF (import, sauvegarde, restauration) et les dialogues « Renommer la liste » (`SaisieDialog`), « Import CSV », « Restaurer une sauvegarde », « Confirmer la restauration », « Restauration impossible » (piloté par `etatRestauration`). L'import, la sauvegarde et la restauration n'existent plus dans `TodoListScreen`.
  * Navigation (`ShopListNavHost`) : `versionApp = BuildConfig.VERSION_NAME`, `onOuvrirCategories = navigate(Routes.CATEGORIES)`, `onRetour = popBackStack()` ; un `LaunchedEffect(Unit)` collecte `viewModel.operationReussie` et appelle `navController.popBackStack(Routes.LISTE, inclusive = false)`.
* **Ligne produit** (`TacheItem`) : un seul `Text` construit avec un `AnnotatedString` (nom, puis si non coché et quantité non vide : espace demi-cadratin ` ` et quantité en `SpanStyle(fontSize = 12.sp, color = couleur de la catégorie)`, espaces internes rendus insécables). La couleur de catégorie arrive par le paramètre `couleurCase`.
* **Dialogue produit** (`ProduitEditionDialog`) : un seul composant pour l'ajout et la modification.
  * Signature : `ProduitEditionDialog(tache: TacheEntity?, categories, categorieInitiale: String = tache?.categorie ?: "", onCheckErreurTitre(titre, categorie), onValider(titre, quantite, categorie), onSupprimer: () -> Unit = {}, onFermer)`.
  * `tache == null` : **mode ajout** (titre « Nouveau produit », champs vides, catégorie = `categorieInitiale`, bouton unique « Ajouter » aligné à droite, `onValider` toujours appelé). Sinon **mode modification** (titre « Modifier le produit », champs préremplis, boutons « Supprimer » et « Valider », pas d'écriture si rien n'a changé).
  * `Dialog` + `Surface` avec `DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true)`, `ExposedDropdownMenuBox` pour la catégorie, deux `OutlinedTextField` avec croix d'effacement, état local en `rememberSaveable`.
  * Couleur des catégories : éléments du menu en `Color(cat.couleur)`, champ Catégorie via `textStyle` dans la couleur de la catégorie sélectionnée (`Color.Unspecified` si introuvable).
  * Les deux `OutlinedTextField` utilisent `KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = …)` (`Next` pour le nom, `Done` pour la quantité). Aucune mise en majuscule n'est faite par le code.
* **Menu des catégories** : `DropdownMenu` ancré sur le bouton ⋮ de chaque en-tête dans `TodoListScreen`, qui réutilise les fonctions du ViewModel (`trierTachesParAlpha`, `renommerCategorie`, `modifierCouleurCategorie`, `supprimerCategorie`) ainsi que `SaisieDialog` et `ColorPickerDialog`. « Ajout d'élément » renseigne `categorieCibleAjout`, qui ouvre `ProduitEditionDialog(tache = null, categorieInitiale = …)`.
* **Glisser-déposer** : bibliothèque Reorderable (`rememberReorderableLazyListState`, `ReorderableItem`, `longPressDraggableHandle`). Déposer un produit sur un en-tête ou un produit d'une autre catégorie change sa catégorie ; la liste complète est réordonnée puis enregistrée à chaque pas du glisser.
  * Contrôle des doublons au lâcher : `longPressDraggableHandle(onDragStarted = …, onDragStopped = …)` ; au démarrage, `TodoListScreen` mémorise dans des états locaux `idTacheGlissee`, `categorieOrigineGlissement`, `categorieFinaleGlissement` (mise à jour à chaque pas) et `tachesAvantGlissement` (copie de `uiState.taches`). Au lâcher, `verifierDoublonApresGlissement()` restaure la copie via `onReordonnerTaches` si la catégorie finale contient un homonyme, affiche le Toast (directement depuis l'écran, via `LocalContext`, pas via `evenements`), puis remet les états à `null`.
  * `ReorderableItem` est appelé avec `animateItemModifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)` : l'animation de placement est conservée, les fondus sont supprimés car le fondu d'apparition pouvait rester bloqué en recherche (produit filtré affiché comme un espace vide).
* **Dialogues** : `SaisieDialog` est le dialogue générique (titre, champ unique, placeholder, validation avec message d'erreur, actions supplémentaires comme Supprimer), utilisé pour l'ajout et le renommage de catégorie et le renommage de la liste (depuis Options). Il ne sert plus pour les produits. Les confirmations utilisent `AlertDialog`.
* **Fichiers** : Storage Access Framework via `rememberLauncherForActivityResult` (`OpenDocument`, `CreateDocument`), aucune permission de stockage.
* **Thème** : `ShopListTheme`, thème sombre unique (fond `#2A3135`, texte blanc). Couleurs personnalisées exposées par `CompositionLocal` : `ShopListTheme.customColors.tacheNonFaite` et `.tacheFaite` (gris `#787D7F`).

### 3.7 Format de sauvegarde JSON (formatVersion 2)

```json
{
  "formatVersion": 2,
  "exportDate": "2026-11-09T18:30:00",
  "appVersion": "2026.04",
  "titreListe": "Courses",
  "parametres": [ { "cle": "afficherFaites", "valeur": "true" }, { "cle": "positionBarre", "valeur": "BAS" } ],
  "categories": [ { "nom": "Frais", "ordre": 0, "couleur": 4294967295, "repliee": false } ],
  "taches":     [ { "titre": "Lait", "categorie": "Frais", "faite": false, "ordre": 0, "quantite": "1 L" } ]
}
```

À la restauration, les versions 1 et 2 sont acceptées (version 1 ou champ absent : `quantite = ""`) ; toute autre version est refusée. Tous les paramètres présents dans le fichier sont restaurés (la table `parametres` est vidée avant) : un paramètre absent du fichier reprend sa valeur par défaut. Les `id` des produits sont régénérés par Room.

### 3.8 Limites connues et dette technique

À garder en tête lors des évolutions :

* **Migrations Room** : chaque évolution du modèle doit ajouter une migration explicite (`MIGRATION_x_y`) dans `TodoDatabase.kt`, et le nouveau schéma exporté dans `app/schemas` doit être commité. Aucun test automatisé de migration n'existe : les migrations se testent à la main (installer la nouvelle version par-dessus l'ancienne, après une sauvegarde complète).
* **Documentation des migrations** : `specs/MIGRATIONS.md` indique de placer les migrations dans un fichier `Migrations.kt`, qui n'existe pas ; l'usage réel (et la règle à suivre) est `TodoDatabase.kt`.
* **Nouveaux paramètres** : un nouveau paramètre stocké dans la table `parametres` n'est sauvegardé que s'il est ajouté à la liste de clés de `construireJsonSauvegarde`.
* **Sauvegarde automatique Android** : `android:allowBackup="true"` avec les fichiers `backup_rules.xml` et `data_extraction_rules.xml` laissés à l'état de modèle (aucune règle active). La sauvegarde cloud Android peut donc inclure la base Room et la restaurer lors d'une réinstallation.
* **Import CSV** : le nom de catégorie est comparé exactement (« frais » et « Frais » donnent deux catégories) ; le texte d'aide du dialogue d'import (dans `OptionsScreen`) ne mentionne pas la colonne quantité.
* **Snackbar en double** : la même `SnackbarHostState` est affichée par le `SnackbarHost` du `Scaffold` de `TodoApp` et par celui de `TodoListScreen` (et de `CategoriesScreen`). Invisible en position « Haut » (superposition exacte), visible en position « Bas ». Correction spécifiée dans le MODULE 07d (non implémenté à cette date).
* **Position « Bas »** : au lancement, la barre peut apparaître en haut une fraction de seconde avant la lecture du paramètre ; la liste n'est pas calée en bas quand elle est plus courte que l'écran (prévu au MODULE 07d).
* `TacheDao.trouverParTitreEtCategorie` n'est plus utilisée depuis le MODULE 04 (conservée).
* **Pas de clé étrangère** entre produits et catégories (cohérence assurée par le code).
* **ViewModel unique** volumineux (environ 780 lignes) : états, CSV et sauvegarde y sont regroupés. `TodoListScreen` fait environ 565 lignes (menu ⋮, dialogues produit et catégorie, recherche) depuis le déplacement de l'import et de la restauration vers `OptionsScreen` (environ 290 lignes).
* `modeRecherche` et `categoriesToucheesEnRecherche` sont des `remember` simples : la recherche ouverte n'est pas conservée si l'écran est recréé ou si l'on navigue vers un autre écran puis revient.
* **Recherche, champ vide** : toucher un en-tête ferme la recherche quand le champ est vide (comportement hérité du MODULE 05) ; correction prévue au MODULE 07d.
* **Doublon fonctionnel** : la gestion des catégories existe à la fois dans le menu ⋮ des en-têtes et dans l'écran « Gestion des catégories », avec une petite différence (confirmation de suppression toujours demandée depuis le menu ⋮, seulement pour une catégorie non vide depuis l'écran dédié).
* **Libellés** mêlant « tâche », « élément » et « produit » (« Ajout d'élément », « Tâche supprimée », « N tâche(s) »…).
* **Données d'exemple** du premier lancement héritées de la todo list.
* **Tests** :
  * `FormatageTest` (unitaires : libellé avec quantité, découpage CSV, comparateur alphabétique, `nomsProduitsIdentiques`, `categorieReplieeAffichee`) ;
  * `ProduitEditionDialogTest` (Compose, modes ajout et modification, contrôle d'erreur selon la catégorie sélectionnée) ;
  * `OptionsScreenTest` (Compose : affichage du nom, de la version, des boutons et de l'option de position, sélection Haut / Bas, renommage, ouverture des dialogues d'import et de restauration, dialogue d'erreur de restauration) ;
  * tests Compose à lancer avec `./gradlew connectedAndroidTest`, appareil branché. La recherche (fermeture automatique, dépliage automatique), la position de la barre et le retour à la liste après import ou restauration n'ont pas de test automatisé.
* `StatusBarColor` défini via une API dépréciée dans `Theme.kt`.

---

## 4. Méthode de développement

* Développement assisté par LLM : les évolutions sont décrites dans des **spécifications par module** au format Markdown (structure : Objectif, Fonctionnalités, Écrans, Comportement, Règles métier, Modèle de données, Architecture, Composants UI, Cas limites, Tests, Critères d'acceptation, Travail attendu), rangées dans `/specs`, puis implémentées par Gemini dans Android Studio.
* Consignes habituelles pour l'implémentation : ne créer que les classes nécessaires, ne pas modifier les autres modules, ne pas refactoriser l'application sans demande explicite, compiler après chaque étape, résumer à la fin les fichiers créés, les fichiers modifiés et les points restant à faire.
* Nommage du code en **français** (`ajouterTache`, `basculerFait`, `CategorieEntity`…), à conserver.
* La copie `specs/shopList_description.md` du dépôt doit être remplacée par ce document à chaque mise à jour.
