package net.edwardcode.fishbot;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class Database {
    private final HikariDataSource ds;
    public Database() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(
                "jdbc:mysql://"
                        + Main.config.mysql.host
                        + "/"
                        + Main.config.mysql.name
        );
        config.setUsername(Main.config.mysql.user);
        config.setPassword(Main.config.mysql.pass);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        config.setMinimumIdle(50);
        config.setIdleTimeout(15000);
        config.setMaxLifetime(60000);
        config.setMaximumPoolSize(1000);

        ds = new HikariDataSource(config);
    }

    public Connection getConn() throws SQLException {
        return ds.getConnection();
    }
}
