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

Pour les changements complexes, SQLite ne permet pas l' `ALTER TABLE` direct. Procédure en 4 étapes :

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
