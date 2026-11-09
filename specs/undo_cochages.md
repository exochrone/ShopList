# Epic : Annulation multi-niveaux des cochages / décochages


---

## Objectif

Permettre à l'utilisateur d'annuler les 10 dernières actions de cochage ou décochage de tâches via une icône dédiée dans la barre de titre. Chaque clic sur cette icône annule l'action la plus récente, jusqu'à épuisement de la pile.

Cette fonctionnalité s'ajoute au système d'undo existant basé sur les Snackbar (qui couvre suppressions de tâches, suppressions de catégories, et "Tout cocher"), sans interférer avec lui.

## Contexte

L'application dispose déjà d'un mécanisme d'undo via `ActionAnnulable` et `Snackbar`, mais ce mécanisme est :
- **Limité à une seule action** (la plus récente, pendant 4 secondes)
- **Visible seulement temporairement** (le Snackbar disparaît après 4 secondes)
- **Pas adapté** aux cochages/décochages individuels (qui se font en rafale lors d'une session de courses, par exemple)

L'utilisateur peut cocher 8 tâches lors de ses courses, puis se rendre compte qu'il a coché par erreur la mauvaise tâche en numéro 5. Sans undo multi-niveaux, il doit retrouver manuellement la tâche concernée. Avec cette fonctionnalité, il clique simplement 4 fois sur l'icône undo pour revenir à l'état souhaité.

## Parcours utilisateur

1. L'utilisateur ouvre l'app et voit la nouvelle icône undo (🔄) dans la TopAppBar, à gauche de l'icône loupe
2. Au lancement, l'icône est grisée (rien à annuler)
3. L'utilisateur coche une tâche. L'icône devient active (couleur normale)
4. L'utilisateur coche 5 autres tâches. La pile d'undo contient 6 actions
5. L'utilisateur clique sur l'icône undo. La dernière tâche cochée se décoche automatiquement, la pile contient maintenant 5 actions
6. L'utilisateur peut continuer à cliquer pour remonter chronologiquement dans son historique de cochages
7. Lorsque la pile est vide, l'icône redevient grisée

## Critères d'acceptation

- [ ] Une icône undo est présente dans la `TodoListTopBar`, positionnée **à gauche** de l'icône loupe
- [ ] L'icône utilise `Icons.AutoMirrored.Filled.Undo` (pour respecter les langues RTL)
- [ ] L'icône est grisée (`alpha = 0.38f`) quand la pile est vide, normale sinon
- [ ] La pile mémorise au maximum **10 actions** ; au-delà, les plus anciennes sont supprimées (FIFO sur la queue)
- [ ] Un clic sur l'icône annule la dernière action enregistrée (LIFO sur la pile)
- [ ] Seules les actions de **cochage / décochage** alimentent cette pile (pas les suppressions, créations, modifications de titre)
- [ ] La pile est **vidée** lorsque les actions deviennent obsolètes (voir section "Invalidation de la pile")
- [ ] La pile n'est pas persistée : elle est réinitialisée à chaque lancement de l'app
- [ ] L'icône loupe (mode recherche) **ne masque pas** l'icône undo : les deux peuvent coexister visuellement (sauf en mode recherche actif où la TopAppBar est remplacée par le champ de saisie)

---

## Spécifications techniques

### Architecture

L'état de la pile d'undo est géré dans le `TodoViewModel`. Les écrans observent cet état via un `StateFlow`.

```kotlin
// Capacité maximum de la pile
private const val UNDO_STACK_MAX_SIZE = 10
```

### Modélisation

Ajouter une nouvelle data class dans `TodoViewModel.kt`, à côté de `ActionAnnulable` :

```kotlin
/**
 * Représente une action de cochage / décochage potentiellement annulable.
 *
 * @property tacheId identifiant de la tâche concernée
 * @property etatPrecedent valeur de [TacheEntity.faite] avant l'action
 *                        (donc l'état à restaurer en cas d'annulation)
 */
data class ActionCochage(
    val tacheId: Int,
    val etatPrecedent: Boolean
)
```

> Note : on ne mémorise que l'identifiant et l'état précédent, pas l'entité complète. Ça suffit pour restaurer, et ça évite de garder des références potentiellement obsolètes vers des tâches qui auraient pu être modifiées entre-temps.

### Modifications du ViewModel

#### Ajout de l'état

```kotlin
class TodoViewModel(application: Application) : AndroidViewModel(application) {
    // ... existant ...

    private val _pileCochages = MutableStateFlow<List<ActionCochage>>(emptyList())
    val pileCochages: StateFlow<List<ActionCochage>> = _pileCochages.asStateFlow()

    // Existant, à conserver
    val uiState: StateFlow<TodoUiState> = ...
}
```

#### Modification de `basculerFait`

```kotlin
fun basculerFait(tache: TacheEntity) {
    viewModelScope.launch {
        // 1. Empiler l'état précédent AVANT de modifier
        empilerActionCochage(ActionCochage(tache.id, tache.faite))

        // 2. Modifier la tâche (logique existante)
        tacheDao.mettreAJour(tache.copy(faite = !tache.faite))
    }
}

private fun empilerActionCochage(action: ActionCochage) {
    _pileCochages.update { pile ->
        // On limite à UNDO_STACK_MAX_SIZE en supprimant les plus anciennes (en tête)
        val nouvelle = (pile + action)
        if (nouvelle.size > UNDO_STACK_MAX_SIZE) {
            nouvelle.takeLast(UNDO_STACK_MAX_SIZE)
        } else {
            nouvelle
        }
    }
}
```

> Note : la pile est modélisée comme une `List` simple. Le **dernier élément** est l'action la plus récente. L'annulation prend le dernier élément (LIFO) et la rotation supprime le premier élément (FIFO sur dépassement).

#### Nouvelle méthode `annulerDernierCochage`

```kotlin
fun annulerDernierCochage() {
    viewModelScope.launch {
        val pile = _pileCochages.value
        if (pile.isEmpty()) return@launch

        val derniereAction = pile.last()

        // Récupérer la tâche actuelle (peut avoir été modifiée entre-temps)
        val tacheActuelle = uiState.value.taches.firstOrNull { it.id == derniereAction.tacheId }
        if (tacheActuelle != null) {
            tacheDao.mettreAJour(tacheActuelle.copy(faite = derniereAction.etatPrecedent))
        }

        // Retirer cette action de la pile, qu'elle ait été appliquée ou non
        _pileCochages.update { it.dropLast(1) }
    }
}
```

#### Helper pour l'invalidation

```kotlin
private fun viderPileCochages() {
    _pileCochages.value = emptyList()
}
```

### Invalidation de la pile

La pile doit être **vidée** dans les cas suivants pour éviter les incohérences :

| Action | Raison |
|---|---|
| Suppression d'une tâche présente dans la pile | L'undo ne pourrait plus s'appliquer à cette tâche |
| Suppression d'une catégorie contenant des tâches dans la pile | Idem |
| Action "Tout cocher" | Modifie en masse l'état des cases ; les undo individuels n'ont plus de sens |
| Annulation de "Tout cocher" via Snackbar | Idem |
| Import CSV | L'état des tâches peut être réinitialisé |

Pour simplifier l'implémentation, **vider la pile à chaque action destructive ou massive** est une stratégie acceptable :

```kotlin
fun supprimerTache(tache: TacheEntity) {
    viewModelScope.launch {
        viderPileCochages()  // ← ajouté
        tacheDao.supprimer(tache)
        _actionAnnulable.value = ActionAnnulable.SuppressionTache(tache)
    }
}

fun supprimerCategorie(categorie: CategorieEntity) {
    viewModelScope.launch {
        viderPileCochages()  // ← ajouté
        // ... existant
    }
}

fun toutCocher() {
    viewModelScope.launch {
        viderPileCochages()  // ← ajouté
        // ... existant
    }
}

fun importerDepuisCsv(uri: android.net.Uri) {
    viewModelScope.launch {
        viderPileCochages()  // ← ajouté
        // ... existant
    }
}
```

> Note : ne **pas** vider la pile dans `modifierTitreTache` et `reordonnerTaches`, car ces actions ne changent pas l'état `faite` des tâches.

### Modifications de la navigation

#### `MainActivity.kt` (`TodoApp`)

Collecter le `pileCochages` et le passer à `ShopListNavHost` (similaire à `actionAnnulable`).

```kotlin
@Composable
fun TodoApp() {
    val viewModel: TodoViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionAnnulable by viewModel.actionAnnulable.collectAsStateWithLifecycle()
    val pileCochages by viewModel.pileCochages.collectAsStateWithLifecycle()  // ← nouveau
    // ... existant

    ShopListNavHost(
        navController = navController,
        uiState = uiState,
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        pileCochages = pileCochages  // ← nouveau
    )
}
```

#### `ShopListNavHost.kt`

Ajouter le paramètre et le propager à `TodoListScreen` :

```kotlin
@Composable
fun ShopListNavHost(
    navController: NavHostController,
    uiState: TodoUiState,
    viewModel: TodoViewModel,
    snackbarHostState: SnackbarHostState,
    pileCochages: List<ActionCochage>  // ← nouveau
) {
    NavHost(...) {
        composable(Routes.LISTE) {
            TodoListScreen(
                uiState = uiState,
                snackbarHostState = snackbarHostState,
                actionAnnulable = actionAnnulable,
                pileCochagesNonVide = pileCochages.isNotEmpty(),  // ← nouveau
                onAnnulerDernierCochage = viewModel::annulerDernierCochage,  // ← nouveau
                // ... existant
            )
        }
        // ... CategoriesScreen et PartagerScreen inchangés
    }
}
```

> Note : on ne passe pas la liste complète à l'écran, juste un `Boolean` indiquant si elle est non vide. C'est suffisant pour l'UI (activation/grisage de l'icône) et ça évite des recompositions inutiles à chaque modification de la pile.

### Modifications de l'écran Liste

#### `TodoListScreen.kt`

Ajouter les paramètres et les propager au `TodoListTopBar` :

```kotlin
@Composable
fun TodoListScreen(
    uiState: TodoUiState,
    snackbarHostState: SnackbarHostState,
    actionAnnulable: ActionAnnulable?,
    pileCochagesNonVide: Boolean,  // ← nouveau
    onAnnulerDernierCochage: () -> Unit,  // ← nouveau
    // ... existant
) {
    // ...
    Scaffold(
        topBar = {
            TodoListTopBar(
                titre = uiState.titreListe,
                afficherFaites = uiState.afficherFaites,
                onToggleAfficherFaites = { onBasculerAfficherFaites(!uiState.afficherFaites) },
                undoCochageActif = pileCochagesNonVide,  // ← nouveau
                onAnnulerCochage = onAnnulerDernierCochage,  // ← nouveau
                // ... existant
            )
        },
        // ...
    )
}
```

#### `TodoListTopBar.kt`

Ajouter les paramètres et l'IconButton :

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListTopBar(
    titre: String,
    afficherFaites: Boolean,
    onToggleAfficherFaites: () -> Unit,
    undoCochageActif: Boolean,                  // ← nouveau
    onAnnulerCochage: () -> Unit,               // ← nouveau
    onOuvrirCategories: () -> Unit,
    // ... existant
) {
    // ...
    TopAppBar(
        title = { ... },
        actions = {
            if (isSearchMode) {
                // ... mode recherche inchangé
            } else {
                // 1. Icône Undo cochage (nouvelle, en premier)
                IconButton(
                    onClick = onAnnulerCochage,
                    enabled = undoCochageActif
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = if (undoCochageActif) {
                            "Annuler le dernier cochage"
                        } else {
                            "Aucun cochage à annuler"
                        }
                    )
                }

                // 2. Icône Recherche (existante)
                IconButton(onClick = onToggleSearchMode) {
                    Icon(Icons.Default.Search, contentDescription = "Rechercher")
                }

                // 3. Icône Visibility (existante)
                IconButton(onClick = onToggleAfficherFaites) {
                    Icon(
                        imageVector = if (afficherFaites) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (afficherFaites) "Masquer les tâches faites" else "Afficher toutes les tâches"
                    )
                }

                // 4. Menu burger (existant)
                Box { ... }
            }
        }
    )
}
```

> Note : `IconButton` gère automatiquement le grisage visuel quand `enabled = false` (alpha appliqué à l'icône). Pas besoin de logique custom de couleur.

### Imports à ajouter

Dans `TodoListTopBar.kt` :

```kotlin
import androidx.compose.material.icons.automirrored.filled.Undo
```

---

## Tests à effectuer

### Test 1 : Comportement de base

**Procédure** :
1. Démarrer l'app
2. Vérifier que l'icône undo est grisée
3. Cocher une tâche
4. Vérifier que l'icône devient active
5. Cliquer sur l'icône undo
6. Vérifier que la tâche est décochée
7. Vérifier que l'icône redevient grisée

**Résultat attendu** : conforme aux étapes ci-dessus.

### Test 2 : Pile multi-actions

**Procédure** :
1. Cocher 5 tâches successivement (T1, T2, T3, T4, T5)
2. Cliquer 3 fois sur l'icône undo
3. Vérifier que T5, T4 puis T3 ont été décochées (dans cet ordre)
4. Vérifier que T1 et T2 sont toujours cochées
5. Vérifier que l'icône est toujours active (il reste 2 actions)

**Résultat attendu** : ordre LIFO respecté, pile diminue correctement.

### Test 3 : Limite de 10

**Procédure** :
1. Cocher 12 tâches successivement (T1 à T12)
2. Cliquer 12 fois sur l'icône undo
3. Vérifier que seules les 10 dernières (T3 à T12) ont été décochées
4. Vérifier que T1 et T2 restent cochées
5. Vérifier que l'icône est grisée après les 10 clics

**Résultat attendu** : T1 et T2 sont définitivement perdues de la pile (les plus anciennes ont été évincées).

### Test 4 : Cochage et décochage mélangés

**Procédure** :
1. Cocher T1
2. Cocher T2
3. Décocher T1
4. Cocher T3
5. Cliquer 4 fois sur l'icône undo

**Résultat attendu** :
- 1er undo : T3 redevient non-cochée
- 2e undo : T1 redevient cochée
- 3e undo : T2 redevient non-cochée
- 4e undo : T1 redevient non-cochée
- Toutes les tâches dans leur état initial

### Test 5 : Invalidation par suppression

**Procédure** :
1. Cocher 3 tâches
2. Vérifier que la pile contient 3 actions (icône active)
3. Supprimer une tâche (n'importe laquelle, même non-cochée)
4. Vérifier que l'icône undo est grisée

**Résultat attendu** : la pile est vidée par la suppression.

### Test 6 : Invalidation par "Tout cocher"

**Procédure** :
1. Cocher 2 tâches
2. Cliquer "Tout cocher" depuis le menu
3. Vérifier que l'icône undo est grisée
4. Cocher / décocher d'autres tâches
5. Vérifier que la pile fonctionne à nouveau normalement

**Résultat attendu** : "Tout cocher" reset la pile, mais les actions suivantes l'alimentent à nouveau.

### Test 7 : Persistance entre écrans

**Procédure** :
1. Cocher 2 tâches sur l'écran Liste
2. Naviguer vers "Gérer les catégories"
3. Revenir à l'écran Liste
4. Cliquer sur l'icône undo
5. Vérifier que la dernière tâche cochée est décochée

**Résultat attendu** : la pile survit aux navigations entre écrans (puisqu'elle vit dans le ViewModel).

### Test 8 : Réinitialisation au lancement

**Procédure** :
1. Cocher 5 tâches
2. Fermer complètement l'app (swipe out)
3. Relancer l'app
4. Vérifier que l'icône undo est grisée

**Résultat attendu** : la pile n'est pas persistée, elle redémarre vide à chaque lancement.

### Test 9 : Mode recherche

**Procédure** :
1. Cocher 3 tâches (la pile contient 3 actions)
2. Activer le mode recherche (icône loupe)
3. Vérifier que l'icône undo n'est plus visible (la TopAppBar bascule en mode champ de recherche)
4. Quitter le mode recherche
5. Vérifier que l'icône undo est de nouveau visible et active

**Résultat attendu** : l'icône disparaît visuellement pendant la recherche mais l'état de la pile est conservé.

---

## Points d'attention

### Pas de Snackbar

Cette fonctionnalité **ne déclenche pas de Snackbar** lors d'une annulation. Le retour visuel se fait directement par la mise à jour de la case à cocher (qui change d'état sous les yeux de l'utilisateur). Un Snackbar serait redondant et créerait du bruit visuel lors d'annulations multiples rapides.

### Pas de notification "rien à annuler"

Lorsque l'utilisateur clique sur l'icône grisée, **rien ne se passe** (le clic est désactivé via `enabled = false`). Pas de Toast "Aucune action à annuler" : l'état désactivé est lui-même le retour visuel.

### Cohabitation avec le système Snackbar existant

Les deux systèmes d'undo coexistent sans interaction :
- **Pile cochages** (cette Epic) : pour les cochages/décochages, accessible via icône
- **ActionAnnulable** (existant) : pour les suppressions et "Tout cocher", accessible via Snackbar

Une suppression de tâche déclenche un Snackbar **et** vide la pile cochages. C'est cohérent : la suppression invalide les undo de cochage potentiels sur cette tâche.

### Tâche introuvable au moment de l'annulation

Si entre l'empilement et l'annulation la tâche a disparu (ce qui ne devrait pas arriver vu les invalidations, mais par sécurité), `annulerDernierCochage` gère le cas avec un `firstOrNull` :

```kotlin
val tacheActuelle = uiState.value.taches.firstOrNull { it.id == derniereAction.tacheId }
if (tacheActuelle != null) {
    tacheDao.mettreAJour(...)
}
// Action retirée de la pile dans tous les cas
```

L'action est retirée de la pile même si la tâche est introuvable, pour garantir que l'utilisateur progresse dans l'historique à chaque clic.

### Pas de "redo"

Cette Epic ne couvre **pas** le redo (re-faire après avoir annulé). Si l'utilisateur clique 3 fois sur undo puis recoche manuellement, son cochage manuel alimente normalement la pile. Mais il n'y a pas de pile "future" pour refaire les annulations.

Implémenter un redo demanderait une seconde pile et complexifierait l'UI (ajout d'une icône redo). Hors-scope.

### Pas de persistance

Volontairement, la pile n'est pas sauvegardée en base. Raisons :
- Le besoin est ponctuel (corriger une erreur de cochage immédiate, pas annuler 3 jours plus tard)
- La persistance ouvrirait des cas de bord complexes (que faire si la tâche a été modifiée entre deux sessions ?)
- Cohérent avec le comportement habituel des piles d'undo dans les apps mobiles

---

