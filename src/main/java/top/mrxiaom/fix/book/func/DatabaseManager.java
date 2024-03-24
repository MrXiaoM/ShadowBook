package top.mrxiaom.fix.book.func;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.fix.book.ShadowBookPlugin;
import top.mrxiaom.fix.book.db.BookDatabase;
import top.mrxiaom.fix.book.db.IDatabase;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager extends AbstractPluginHolder {

    HikariDataSource dataSource = null;
    private final List<IDatabase> databases = new ArrayList<>();
    @Nullable
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (Throwable t) {
            warn("从连接池获取数据库连接时出现异常", t);
            return null;
        }
    }
    public final BookDatabase example;
    public DatabaseManager(ShadowBookPlugin plugin) {
        super(plugin);
        databases.add(example = new BookDatabase(this));
        register();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setAutoCommit(true);
        hikariConfig.setMaxLifetime(120000L);
        hikariConfig.setIdleTimeout(5000L);
        hikariConfig.setConnectionTimeout(5000L);
        hikariConfig.setMinimumIdle(10);
        hikariConfig.setMaximumPoolSize(100);

        if (dataSource != null) dataSource.close();

        String type = config.getString("database.type", "mysql");
        if (type.equalsIgnoreCase("mysql")) {
            String host = config.getString("database.host", "localhost");
            int port = config.getInt("database.port", 3306);
            String user = config.getString("database.user", "root");
            String pass = config.getString("database.pass", "root");
            String database = config.getString("database.database", "db");
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&verifyServerCertificate=false&serverTimezone=UTC");
            hikariConfig.setUsername(user);
            hikariConfig.setPassword(pass);
        } else if (type.equalsIgnoreCase("sqlite")) {
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + new File(plugin.getDataFolder(), "books.db").getAbsolutePath());
        } else {
            warn("不支持的数据库类型 " + type);
            return;
        }
        dataSource = new HikariDataSource(hikariConfig);

        info("正在连接数据库...");
        Connection conn = getConnection();
        if (conn != null) {
            for (IDatabase db : databases) db.reload(conn);
            info("数据库连接成功");
            try {
                conn.close();
            } catch (Throwable t) {
                warn("关闭数据库连接时出现异常", t);
            }
        }
    }

    @Override
    public void onDisable() {
        if (dataSource != null) dataSource.close();
    }

    @NotNull
    public static DatabaseManager inst() {
        return AbstractPluginHolder.get(DatabaseManager.class).orElseThrow(IllegalStateException::new);
    }
}
