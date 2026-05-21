package edu.montana.pika.migrations;

import edu.montana.pika.PikaORM;
import edu.montana.pika.logging.PikaLogger;
import edu.montana.pika.query.PikaList;

import java.io.Console;
import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")


//========================================================================================
// Migrations System
//========================================================================================

public abstract class Migrations {

    public static final String HELP_MSG = """
            Migrations Commands
              show      - show all migrations
              up        - apply one pending migration
              down      - back out the latest migration
              all       - apply all pending migrations
              exit/quit - exit this tool
              help/?    - show this help message
            """;
    private LinkedHashMap<String, PikaMigration> migrationsMap;
    private PikaORM orm;

    public void setORM(PikaORM orm) {
        this.orm = orm;
    }

    public PikaORM getORM() {
        if (orm != null) {
            return orm;
        }
        if (PikaORM.getDefault() != null) {
            return PikaORM.getDefault();
        }
        throw new IllegalStateException("ORM has not been set and there is no default ORM, don't know what database to migrate!");
    }

    protected void add(Supplier<PikaMigration> migrationCallable) {
        add(migrationCallable.get());
    }

    protected void add(PikaMigration migration) {
        String migrationName = migration.getName();
        if (migrationsMap.containsKey(migrationName)) {
            throw new IllegalArgumentException("Migration " + migrationName + " already exists!");
        }
        migrationsMap.put(migrationName, migration);
    }

    /**
     * @return the initial pre-migrations schema for the database
     */
    protected String initialSchema() {
        return "";
    }

    public abstract void migrations();

    public static PikaMigration makeMigration(String name) {
        return new PikaMigration(name);
    }

    public void console() {
        getORM();
        orm.exec(PikaMigration.DDL);
        Console console = System.console();
        System.out.println("Welcome to PikaORM Migrations, type ? for help");
        while (true) {
            String cmd = console.readLine("migrations > ").strip();
            //noinspection IfCanBeSwitch
            if (cmd.equals("show")) {
                console.printf(show());
            } else if (cmd.equals("raw")) {
                var mergedMigrations = loadMigrations(orm);
                console.printf(new PikaList<>(mergedMigrations.values()).toString("\n"));
            } else if (cmd.equals("up")) {
                up();
            } else if (cmd.equals("down")) {
                down();
            } else if (cmd.equals("all")) {
                applyAll();
                console.printf("All pending migrations have been applied");
            } else if (cmd.equals("help") || cmd.equals("?")) {
                console.printf(HELP_MSG);
            } else if (cmd.equals("exit") || cmd.equals("quit")) {
                break;
            } else {
                console.printf("Unknown command : " + cmd + "\n");
                console.printf(HELP_MSG);
            }
        }
    }

    private String show() {
        getORM();
        orm.exec(PikaMigration.DDL);
        var mergedMigrations = loadMigrations(orm);

        StringBuilder sb = new StringBuilder("All Migrations:\n");
        String formatString = "%-30.30s | %-15.15s | %-30.30s | %-30.30s | %-30.30s | %-30.30s\n";
        sb.append(String.format(formatString, "name", "status", "applied", "description", "up", "down"));
        sb.append("-------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        for (PikaMigration value : mergedMigrations.values()) {
            sb.append(String.format(formatString,
                    value.getName(), value.getStatus(), value.appliedAtForDisplay(), value.description, value.upForDisplay(), value.downForDisplay()));
        }
        return sb.toString();
    }

    public void up() {
        getORM();
        orm.exec(PikaMigration.DDL);
        var mergedMigrations = loadMigrations(orm);

        var values = new PikaList<>(mergedMigrations.values());
        var firstUnappliedMigration = values.firstWhere(PikaMigration::isPending);
        if (firstUnappliedMigration != null) {
            firstUnappliedMigration.runUp(orm);
        } else {
            orm.getLogger().log(PikaLogger.Level.WARN, "No pending migrations were found in migrations file to apply");
        }
    }

    public void down() {
        getORM();
        orm.exec(PikaMigration.DDL);
        var mergedMigrations = loadMigrations(orm);

        var values = new PikaList<>(mergedMigrations.values());
        var lastAppliedMigration = values.lastWhere(PikaMigration::isApplied);
        if (lastAppliedMigration != null) {
            lastAppliedMigration.runDown(orm);
        } else {
            orm.getLogger().log(PikaLogger.Level.WARN, "No applied migrations were found in migrations file to back out");
        }
    }

    /**
     * Applies all outstanding migrations in the order they are declared
     */
    public void applyAll() {
        orm.inTransaction(() -> {
            orm.getLogger().log(PikaLogger.Level.INFO, "Applying migrations");
            orm.exec(PikaMigration.DDL);
            var mergedMigrations = loadMigrations(orm);
            int migrationCount = 0;
            for (PikaMigration migration : mergedMigrations.values()) {
                if (!migration.isApplied()) {
                    orm.getLogger().log(PikaLogger.Level.INFO, "Applying migration " + migration.name);
                    migrationCount++;
                    migration.runUp(orm);
                }
            }
            if (migrationCount > 0) {
                orm.getLogger().log(PikaLogger.Level.INFO, "Done applying " + migrationCount + " migration" + (migrationCount == 1 ? "" : "s"));
            } else {
                orm.getLogger().log(PikaLogger.Level.INFO, "No pending migrations found");
            }
        });
    }

    private LinkedHashMap<String, PikaMigration> loadMigrations(PikaORM orm) {

        migrationsMap = new LinkedHashMap<>();
        migrations();
        // compute migrations with persisted migrations merged in
        PikaList<PikaMigration> persistedMigrations = orm.find(PikaMigration.class).all().toList();
        var mergedMigrations = new LinkedHashMap<>(migrationsMap);
        for (PikaMigration persistedMigration : persistedMigrations.copy()) {
            PikaMigration existingMigration = mergedMigrations.get(persistedMigration.getName());
            if (existingMigration != null) {
                if (!existingMigration.equals(persistedMigration)) {
                    orm.getLogger().log(PikaLogger.Level.WARN, MessageFormat.format("""
                                    Migration {0} has different content in the codebase and in the database:
                                    
                                    DB Content:
                                    {1}
                                    
                                    Code Content:
                                    {2}
                                    
                                    This may be due to ongoing development but should not be the case in production.
                                    """,
                            existingMigration.name,
                            persistedMigration.getDebugString(),
                            existingMigration.getDebugString()
                    ));
                }
                // update ID
                existingMigration.uuid = persistedMigration.uuid;
                existingMigration.status = persistedMigration.status;
                persistedMigrations.remove(persistedMigration);
            }
        }

        if (!persistedMigrations.isEmpty()) {
            orm.getLogger().log(PikaLogger.Level.WARN,
                    "The following migrations have been found in the database, but are not in the current migration file:\n" +
                            persistedMigrations.toString("\n"));
        }
        return mergedMigrations;
    }

    public static final class PikaMigration {

        public static final String DDL = """
                CREATE TABLE IF NOT EXISTS pika_migrations (
                    uuid TEXT PRIMARY KEY,
                    applied_at bigint,
                    name VARCHAR UNIQUE NOT NULL,
                    description VARCHAR,
                    up VARCHAR,
                    down VARCHAR,
                    status VARCHAR
                );
                """;

        private String uuid;
        private Long appliedAt;
        private String name;
        private String description;
        private String up;
        private String down;
        private MigrationStatus status = MigrationStatus.PENDING;

        private PikaMigration() {
        }

        public PikaMigration(String name) {
            this.name = name;
        }

        public PikaMigration description(String description) {
            this.description = description;
            return this;
        }

        public PikaMigration up(String up) {
            this.up = up;
            return this;
        }

        public PikaMigration down(String down) {
            this.down = down;
            return this;
        }

        public String getName() {
            return name;
        }

        public boolean isApplied() {
            return status == MigrationStatus.APPLIED;
        }

        public boolean isPending() {
            return status == MigrationStatus.PENDING;
        }

        public String[] getUpSqlSplitOnSemicolons() {
            return this.up.split(";");
        }

        public String[] getDownSqlSplitOnSemicolons() {
            return this.down.split(";");
        }

        void runUp(PikaORM orm) {
            orm.inTransaction(() -> {
                String[] upSqlSplitOnSemicolons = getUpSqlSplitOnSemicolons();
                for (String sql : upSqlSplitOnSemicolons) {
                    if (!sql.isBlank()) {
                        orm.exec(sql);
                    }
                }
                this.status = MigrationStatus.APPLIED;
                this.appliedAt = new Date().getTime();
                if (this.uuid == null) {
                    this.uuid = "" + UUID.randomUUID();
                    orm.insert(this);
                } else {
                    orm.update(this);
                }
            });
        }

        void runDown(PikaORM orm) {
            orm.inTransaction(() -> {
                String[] upSqlSplitOnSemicolons = getDownSqlSplitOnSemicolons();
                for (String sql : upSqlSplitOnSemicolons) {
                    if (!sql.isBlank()) {
                        orm.exec(sql);
                    }
                }
                orm.delete(this);
            });
        }


        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PikaMigration migration = (PikaMigration) o;
            return Objects.equals(name, migration.name) && Objects.equals(up, migration.up) && Objects.equals(down, migration.down);
        }


        public int hashCode() {
            return Objects.hash(name, up, down);
        }

        public MigrationStatus getStatus() {
            return status;
        }


        public String toString() {
            return "Migration{id=%d, appliedAt=%d, name='%s', description='%s', up='%s', down='%s', status=%s}".formatted(uuid, appliedAt, name, description, up, down, status);
        }

        public Object getDebugString() {
            return "{down='%s', up='%s'}".formatted(down, up);
        }

        public Object upForDisplay() {
            String[] lines = up.split("\n");
            for (int i = 0; i < lines.length; i++) {
                lines[i] = lines[i].strip();
            }
            return String.join(" ", lines);
        }

        public Object downForDisplay() {
            String[] lines = down.split("\n");
            for (int i = 0; i < lines.length; i++) {
                lines[i] = lines[i].strip();
            }
            return String.join(" ", lines);
        }

        public Object appliedAtForDisplay() {
            if (appliedAt == null) {
                return null;
            } else {
                return new Date(appliedAt);
            }
        }
    }

    public enum MigrationStatus {
        PENDING,
        APPLIED,
        SKIPPED
    }
}
