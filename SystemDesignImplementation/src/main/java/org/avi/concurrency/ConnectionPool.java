package org.avi.concurrency;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ConnectionPool {
    private final BlockingQueue<Connection> connectionPool;
    private final int poolSize;

    private final String jdbcUrl;
    private final String user;
    private final String password;

    public ConnectionPool(String jdbcUrl, String user, String password, int poolSize)
            throws SQLException {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
        this.poolSize = poolSize;
        this.connectionPool = new ArrayBlockingQueue<>(poolSize);
        initializeConnections();
    }

    private void initializeConnections() throws SQLException {
        for (int i = 0; i < poolSize; i++) {
            Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
            connectionPool.offer(conn);
        }
    }

    public Connection getConnection() throws InterruptedException {
        return connectionPool.take(); // blocks if empty
    }

    public void releaseConnection(Connection connection) {
        if (connection != null) {
            connectionPool.offer(connection); // returns it to the pool
        }
    }

    public void shutdown() throws SQLException {
        for (Connection conn : connectionPool) {
            conn.close();
        }
    }

    public static void main(String[] args) throws SQLException, InterruptedException {
        ConnectionPool pool = new ConnectionPool(
                "jdbc:mysql://localhost:3306/testdb", "user", "password", 10
        );

        // Get connection from pool
        Connection conn = pool.getConnection();

        try {
            // Use the connection
        } finally {
            pool.releaseConnection(conn); // Always release it
        }

        // On shutdown
        pool.shutdown();
    }
}
