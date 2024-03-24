package top.mrxiaom.fix.book.db;

import java.sql.Connection;

public interface IDatabase {
    void reload(Connection conn);
}
