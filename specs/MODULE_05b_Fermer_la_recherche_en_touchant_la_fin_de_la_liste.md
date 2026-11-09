Évolution MODULE 05b : fermer la recherche en touchant sous la fin de la liste

Objectif
En mode recherche, un toucher dans la zone vide située sous le séparateur horizontal qui termine la liste (dernier item de la LazyColumn dans TodoListScreen.kt) doit fermer la recherche, exactement comme la flèche retour.

Comportement attendu
- Uniquement si modeRecherche == true. Hors recherche, aucun changement.
- Appeler quitterRecherche() sans cible : pas de défilement.
- Seul un toucher simple compte. Un défilement (glisser), un appui sur un produit, une case, un en-tête ou le menu ⋮ garde son comportement actuel.
- Un toucher dans les espaces entre les lignes ou dans les marges horizontales ne ferme pas la recherche. Seule la zone sous le dernier item compte.
- Si le séparateur final n'est pas visible (liste plus longue que l'écran et pas défilée jusqu'en bas), il n'y a pas de zone vide, donc rien ne se passe.
- Si la recherche ne donne aucun résultat, la liste ne contient que le séparateur : toucher en dessous ferme la recherche.

Implémentation (fichier unique : ui/screens/liste/TodoListScreen.kt)
Sur le Modifier de la LazyColumn, ajouter à la fin de la chaîne actuelle (après les deux .padding) :

.then(
    if (modeRecherche) Modifier.pointerInput(Unit) {
        detectTapGestures(onTap = { position ->
            val info = lazyListState.layoutInfo
            val dernier = info.visibleItemsInfo.lastOrNull()
            if (dernier != null &&
                dernier.index == info.totalItemsCount - 1 &&
                position.y > dernier.offset + dernier.size
            ) {
                quitterRecherche()
            }
        })
    } else Modifier
)

Imports si absents : androidx.compose.foundation.gestures.detectTapGestures, androidx.compose.ui.input.pointer.pointerInput.

Ne pas toucher
- Ne pas modifier l'item HorizontalDivider final ni ajouter d'item ou de Spacer à la liste (cela changerait la hauteur défilable).
- Ne pas modifier quitterRecherche(), le LaunchedEffect de défilement, les déclencheurs du MODULE 05, TacheItem, TodoListTopBar ni le ViewModel.
- Ne pas refactoriser.

Tests manuels
1. Recherche avec quelques résultats : toucher sous le séparateur ferme la recherche et le clavier.
2. Toucher entre deux lignes ou dans la marge gauche : la recherche reste ouverte.
3. Recherche sans résultat : toucher sous le séparateur ferme la recherche.
4. Faire défiler la liste en recherche en partant de la zone vide : pas de fermeture.
5. Hors recherche : toucher sous le séparateur ne fait rien, le glisser-déposer fonctionne toujours.

Travail attendu
Créer uniquement ce qui est nécessaire, ne pas modifier d'autres modules, ne pas refactoriser, compiler après la modification, puis résumer : fichiers créés, fichiers modifiés, points restant à faire.
