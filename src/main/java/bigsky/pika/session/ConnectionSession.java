package bigsky.pika.session;

import bigsky.pika.PikaORM;
import bigsky.pika.logging.PikaLogger;
import bigsky.pika.util.SafeAutoCloseable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import static bigsky.pika.PikaORM.safely;

public class ConnectionSession implements SafeAutoCloseable {

    private final PikaORM orm;
    ConnectionSession previous;
    Connection conn;
    int openCount;
    int transactionCount;
    private UUID uuid;
    ArrayList<PreparedStatement> preparedStatements = new ArrayList<>();
    ArrayList<ResultSet> resultSets = new ArrayList<>();

    public ConnectionSession(PikaORM orm, ConnectionSession previousConnection) {
        this.orm = orm;
        this.conn = orm.getNewRawConnection();
        this.previous = previousConnection;
        this.setUuid(UUID.randomUUID());
        this.openCount = 0;
        this.transactionCount = 0;
    }

    public void incrementOpenCount() {
        openCount++;
        orm.getLogger().log(PikaLogger.Level.DEBUG, "Incremented open count on connection {}: {}", getUUID(), "*".repeat(openCount));
    }

    public void close() {
        openCount--;
        orm.getLogger().log(PikaLogger.Level.DEBUG, "Decremented open count on connection {}: {}", getUUID(), "*".repeat(openCount));
        if (openCount == 0) {
            orm.getLogger().log(PikaLogger.Level.DEBUG, "Closing connection {} on Thread {}", getUUID(), Thread.currentThread().getName());
            try {
                for (var rs : resultSets) {
                    try {
                        if (!rs.isClosed()) {
                            rs.close();
                        }
                    } catch (SQLException e) { /* swallow */ }
                }
                for (var ps : preparedStatements) {
                    try {
                        if (!ps.isClosed()) {
                            ps.close();
                        }
                    } catch (SQLException e) { /* swallow */ }
                }
            } finally {
                try {
                    safely(() -> conn.close());
                } finally {
                    orm.setCurrentSession(this.previous);
                }
            }
        }
    }

    public boolean isInTransaction() {
        return transactionCount > 0;
    }

    public void startTransaction() {
        if (transactionCount == 0) {
            safely(() -> conn.setAutoCommit(false));
            orm.getLogger().log(PikaLogger.Level.DEBUG, "Starting new transaction for connection {}", getUUID());
        } else {
            orm.getLogger().log(PikaLogger.Level.DEBUG, "Existing transaction for connection {}, joining it", getUUID());
        }
        transactionCount++;
    }

    public void finishTransaction() {
        if (isInTransaction()) {
            transactionCount--;
            if (transactionCount == 0) {
                orm.getLogger().log(PikaLogger.Level.DEBUG, "Transaction for connection {} completed, committing", getUUID());
                safely(() -> conn.commit());
            } else {
                orm.getLogger().log(PikaLogger.Level.INFO, "Nested transaction detected for connection {}, deferring commit", getUUID());
            }
            close();
        } else {
            orm.getLogger().log(PikaLogger.Level.ERROR, "No current transaction for connection {}", getUUID());
        }
    }

    public void rollBackTransaction() {
        if (isInTransaction()) {
            orm.getLogger().log(PikaLogger.Level.INFO, "Rolling back transaction for connection {}", getUUID());
            safely(() -> conn.rollback());
            transactionCount--;
            if (transactionCount == 0) {
                orm.getLogger().log(PikaLogger.Level.INFO, "Restoring autoCommit for connection {}", getUUID());
                safely(() -> conn.setAutoCommit(true));
            }
            close();
        } else {
            orm.getLogger().log(PikaLogger.Level.ERROR, "No current transaction for connection {}", getUUID());
        }
    }

    public ResultSet execute(PreparedStatement ps) {
        ResultSet resultSet = orm.time(ps::executeQuery);
        resultSets.add(resultSet);
        return resultSet;
    }

    public PreparedStatement prepareStatement(String updatedSql, Collection<Object> vals) throws SQLException {
        return prepareStatement(updatedSql, vals, null);
    }

    public PreparedStatement prepareStatement(String updatedSql, Collection<Object> vals, String[] keyCols) throws SQLException {
        orm.incrementThreadQueryCount();
        PreparedStatement ps;
        if (keyCols != null) {
            ps = conn.prepareStatement(updatedSql, keyCols);
        } else {
            ps = conn.prepareStatement(updatedSql);
        }
        preparedStatements.add(ps);
        int offset = 1;
        for (Object val : vals) {
            orm.setValueForQuery(ps, offset, val);
            offset++;
        }
        return ps;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
