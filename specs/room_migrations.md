# Epic : Activation des migrations Room

| | |
|---|---|
| **Complexité** | Faible (préparation) à Moyenne (chaque migration future) |
| **Type** | Refactoring de sécurité, sans changement fonctionnel visible |
| **Priorité** | Haute |
| **Estimation** | 1 à 2 heures pour la mise en place initiale |

---

## Contexte

L'application ShopList utilise actuellement `fallbackToDestructiveMigration()` dans la configuration de la base de données Room :

```kotlin
// TodoDatabase.kt - état actuel
Room.databaseBuilder(
    context.applicationContext,
    TodoDatabase::class.java,
    "todo_database"
)
.fallbackToDestructiveMigration()
.build()
```

Ce flag indique à Room d'**effacer toutes les données utilisateur** dès que la structure de la base évolue (ajout d'une colonne, nouvelle entité, modification d'un type, etc.).

En production, c'est inacceptable : à chaque mise à jour de l'app introduisant une modification d'entité, les utilisateurs perdraient toutes leurs tâches, catégories, et paramètres.

L'objectif est de mettre en place l'infrastructure de migrations Room afin que :
1. Les données utilisateur soient préservées entre les versions
2. Le développeur soit forcé d'écrire une migration explicite à chaque évolution de schéma
3. Les erreurs soient détectées à la compilation plutôt qu'à l'exécution chez l'utilisateur

## Objectif

Mettre en place un système de migrations Room sûr et maintenable, permettant de faire évoluer le schéma de la base de données sans perte de données utilisateur.

## Critères d'acceptation

- [ ] Le flag `fallbackToDestructiveMigration()` est retiré du code de production
- [ ] L'export du schéma Room est activé via `room.schemaLocation`
- [ ] Le schéma de la version actuelle (v3) est généré dans `app/schemas/` et commité dans le dépôt Git
- [ ] Le dossier `app/schemas/` est versionné (et **non** ajouté au `.gitignore`)
- [ ] L'application compile et fonctionne à l'identique pour les utilisateurs existants (aucune perte de données)
- [ ] Une migration de test est documentée pour servir de référence aux futures évolutions

---

## Spécifications techniques

### Étape 1 : Activer l'export du schéma

Modifier `app/build.gradle.kts` pour indiquer à KSP où générer les fichiers de schéma JSON.

**Avant :**
```kotlin
android {
    namespace = "com.jb.shoplist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jb.shoplist"
        minSdk = 31
        targetSdk = 35
        versionCode = 2
        versionName = "2026.04"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // ...
}
```

**Après :**
```kotlin
android {
    namespace = "com.jb.shoplist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jb.shoplist"
        minSdk = 31
        targetSdk = 35
        versionCode = 2
        versionName = "2026.04"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // ...
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

> Note : le bloc `ksp { }` se place **en dehors** du bloc `android { }`, au niveau racine du fichier `build.gradle.kts`, après le bloc `dependencies { }` ou avant, peu importe.

### Étape 2 : Activer `exportSchema` dans la base

Modifier `TodoDatabase.kt`.

**Avant :**
```kotlin
@Database(
    entities = [TacheEntity::class, CategorieEntity::class, ParametreEntity::class],
    version = 3,
    exportSchema = false
)
abstract class TodoDatabase : RoomDatabase() {
    // ...
    Room.databaseBuilder(
        context.applicationContext,
        TodoDatabase::class.java,
        "todo_database"
    )
    .fallbackToDestructiveMigration()
    .build()
}
```

**Après :**
```kotlin
@Database(
    entities = [TacheEntity::class, CategorieEntity::class, ParametreEntity::class],
    version = 3,
    exportSchema = true
)
abstract class TodoDatabase : RoomDatabase() {
    // ...
    Room.databaseBuilder(
        context.applicationContext,
        TodoDatabase::class.java,
        "todo_database"
    )
    .build()
}
```

Deux changements :
- `exportSchema = true` (était `false`)
- Suppression de `.fallbackToDestructiveMigration()`

### Étape 3 : Générer le schéma initial

Lancer un build complet du projet :

```
Build > Rebuild Project
```

ou en ligne de commande :

```bash
./gradlew clean build
```

Après build, vérifier la création du fichier :

```
app/schemas/com.jb.shoplist.TodoDatabase/3.json
```

Ce fichier décrit la structure exacte de la base v3 (toutes les tables, colonnes, types, contraintes, index). Il sert de référence pour les migrations futures.

### Étape 4 : Versionner le dossier `schemas`

Vérifier que le dossier n'est pas exclu par `.gitignore`. Si nécessaire, ajouter explicitement :

```
# .gitignore
# (pas de règle excluant /app/schemas)
```

Puis committer :

```bash
git add app/schemas/
git commit -m "feat(db): activate Room schema export and migrations"
```

### Étape 5 : Documenter la procédure pour les évolutions futures

Créer un fichier `docs/MIGRATIONS.md` à la racine du projet avec le contenu suivant :

```markdown
# Migrations Room

## Procédure pour modifier le schéma de la base

À chaque modification d'une entité (`TacheEntity`, `CategorieEntity`, `ParametreEntity` ou nouvelle entité), suivre cette procédure :

1. **Modifier l'entité** dans `Entities.kt`
2. **Incrémenter la version** dans `TodoDatabase.kt` :
   ```kotlin
   @Database(version = N+1, ...)
   ```
3. **Écrire la migration** correspondante dans un fichier `Migrations.kt`
4. **Enregistrer la migration** dans le `Room.databaseBuilder` :
   ```kotlin
   .addMigrations(MIGRATION_N_TO_NPLUS1)
   ```
5. **Compiler** : Room génère automatiquement le nouveau fichier `app/schemas/.../N+1.json`
6. **Tester** la migration sur une base existante avant de pousser
7. **Committer** le code ET le fichier JSON généré

## Exemples de migrations

### Ajout d'une colonne

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE taches ADD COLUMN datePeremption INTEGER")
    }
}
```

### Ajout d'une colonne non-null avec valeur par défaut

```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE taches ADD COLUMN priorite INTEGER NOT NULL DEFAULT 0")
    }
}
```

### Création d'une nouvelle table

```kotlin
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE listes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nom TEXT NOT NULL,
                ordre INTEGER NOT NULL DEFAULT 0
            )
        """)
    }
}
```

### Modification de structure complexe (changement de type, ajout de FK)

Pour les changements complexes, SQLite ne permet pas l'`ALTER TABLE` direct. Procédure en 4 étapes :

```kotlin
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Créer la nouvelle table avec la structure cible
        db.execSQL("""
            CREATE TABLE taches_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                titre TEXT NOT NULL,
                categorie TEXT NOT NULL,
                listeId INTEGER NOT NULL DEFAULT 1,
                faite INTEGER NOT NULL DEFAULT 0,
                ordre INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (listeId) REFERENCES listes(id) ON DELETE CASCADE
            )
        """)
        
        // 2. Copier les données depuis l'ancienne table
        db.execSQL("""
            INSERT INTO taches_new (id, titre, categorie, faite, ordre)
            SELECT id, titre, categorie, faite, ordre FROM taches
        """)
        
        // 3. Supprimer l'ancienne table
        db.execSQL("DROP TABLE taches")
        
        // 4. Renommer la nouvelle
        db.execSQL("ALTER TABLE taches_new RENAME TO taches")
        
        // Recréer les index si nécessaire
        db.execSQL("CREATE INDEX idx_taches_listeId ON taches(listeId)")
    }
}
```

## Tester une migration

Avant de déployer une migration en production, la tester sur une base réelle :

1. Sur l'émulateur ou un appareil de test, installer la version actuelle de l'app
2. Créer des données de test (catégories, tâches, paramètres)
3. Installer la nouvelle version par-dessus (sans désinstaller)
4. Vérifier que toutes les données sont conservées
5. Vérifier que les nouvelles fonctionnalités liées à l'évolution fonctionnent

## En cas d'urgence

Si une migration ne fonctionne pas et que tu n'as pas le temps de la corriger pour une release urgente, tu peux ajouter `.fallbackToDestructiveMigrationFrom(N)` pour autoriser la destruction depuis une version spécifique uniquement. **À utiliser en dernier recours**, car les utilisateurs concernés perdront leurs données.

```kotlin
.fallbackToDestructiveMigrationFrom(3)  // efface uniquement si ancienne version = 3
```
```

### Étape 6 : Vérification finale

Après toutes ces étapes, vérifier :

- [ ] Le projet compile sans erreur
- [ ] Le fichier `app/schemas/com.jb.shoplist.TodoDatabase/3.json` existe
- [ ] L'app démarre normalement avec les données existantes intactes
- [ ] Le fichier `docs/MIGRATIONS.md` est présent
- [ ] Tous les nouveaux fichiers sont commités

---

## Tests à effectuer

### Test 1 : Non-régression sur installation existante

**Objectif** : vérifier que les données existantes sont préservées après le passage à `exportSchema = true` et la suppression de `fallbackToDestructiveMigration()`.

**Procédure** :
1. Installer l'app version actuelle (avec `fallbackToDestructiveMigration`) sur un appareil
2. Créer plusieurs catégories et tâches, en cocher quelques-unes
3. Compiler et installer la nouvelle version (sans désinstaller)
4. Vérifier que toutes les données sont présentes

**Résultat attendu** : aucune donnée perdue, l'app fonctionne identiquement.

### Test 2 : Compilation sans migration manquante

**Objectif** : vérifier que Room refuse de compiler si une évolution d'entité n'est pas accompagnée d'une migration.

**Procédure** :
1. Modifier une entité (ex : ajouter `val test: String = ""` à `TacheEntity`)
2. Incrémenter `version = 3` à `version = 4`
3. **Ne pas** ajouter de migration
4. Compiler

**Résultat attendu** : erreur de compilation Room indiquant qu'une migration de 3 vers 4 est requise.

### Test 3 : Migration réussie

**Objectif** : valider qu'une migration simple fonctionne de bout en bout.

**Procédure** :
1. Sur l'app installée (version 3), créer des données de test
2. Modifier `TacheEntity` pour ajouter un champ optionnel : `val notes: String? = null`
3. Passer la version à 4
4. Ajouter la migration :
   ```kotlin
   val MIGRATION_3_4 = object : Migration(3, 4) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE taches ADD COLUMN notes TEXT")
       }
   }
   ```
5. L'enregistrer dans le `databaseBuilder`
6. Installer la nouvelle version sur l'appareil de test

**Résultat attendu** : 
- Les données existantes sont préservées
- Le champ `notes` est `null` pour toutes les tâches existantes
- L'app fonctionne normalement
- Le fichier `4.json` est généré automatiquement

> Ce test est facultatif mais fortement recommandé pour valider que toute la chaîne fonctionne.

---

## Points d'attention

### Le dossier `schemas` doit être versionné

Beaucoup de templates `.gitignore` Android excluent ce dossier par défaut. **Vérifier qu'il n'est pas ignoré**, car les fichiers JSON sont la référence des schémas pour les migrations futures.

### Ne jamais modifier un fichier de schéma déjà commité

Une fois le fichier `3.json` commité et l'app v3 distribuée, **ne jamais le modifier**. Pour faire évoluer le schéma, créer une v4 avec migration. Modifier rétrospectivement un schéma existant briserait les bases de données des utilisateurs ayant cette version.

### Préférer des migrations atomiques et testées

Une migration qui plante chez l'utilisateur en production est très difficile à récupérer. Toujours :
- Utiliser `withTransaction` implicite (Room le fait pour vous, mais valider)
- Tester sur des données réalistes avant déploiement
- Privilégier les évolutions additives (nouvelle colonne nullable) plutôt que destructives (suppression, changement de type)

### Migrations multiples

Si un utilisateur passe de la version 3 directement à la version 6 (ayant raté les versions 4 et 5), Room enchaîne automatiquement `MIGRATION_3_4`, `MIGRATION_4_5` et `MIGRATION_5_6`. Toutes les migrations intermédiaires doivent rester définies dans le code, même celles concernant des versions anciennes.

### Pour les évolutions majeures, prévoir un backup

Avant une migration complexe (changement de structure majeur, refonte du modèle), envisager d'ajouter une étape de backup automatique de la base avant migration. Pas couvert par cette Epic, mais à garder en tête.

---

## Bénéfices

### Court terme

- Les données utilisateur sont protégées dès maintenant
- L'infrastructure est en place pour toute évolution future

### Long terme

- Chaque évolution du schéma force une réflexion explicite sur la migration
- Les erreurs sont détectées à la compilation, pas chez l'utilisateur
- Les schémas versionnés permettent de retracer l'historique du modèle de données
- Possibilité de tester les migrations en isolation (avec `MigrationTestHelper` de Room)

---

## Hors-scope de cette Epic

Les éléments suivants pourraient être traités dans des Epics ultérieures :

- **Tests automatisés des migrations** via `androidx.room:room-testing` et `MigrationTestHelper`
- **Sauvegarde automatique** de la base avant migration (cf. Epic 7 "Sauvegarde et restauration automatique")
- **Mécanisme de fallback gracieux** : en cas d'échec de migration, restaurer un backup au lieu de planter

---

## Références

- [Documentation officielle Room - Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Codelab Google - Room Migration](https://developer.android.com/codelabs/android-room-with-a-view-kotlin)
- [Article : Understanding Room Migrations](https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929)
