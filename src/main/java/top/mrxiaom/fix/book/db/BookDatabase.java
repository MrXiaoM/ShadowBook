package top.mrxiaom.fix.book.db;

import com.google.common.collect.Lists;
import top.mrxiaom.fix.book.func.AbstractPluginHolder;
import top.mrxiaom.fix.book.func.DatabaseManager;
import top.mrxiaom.sqlhelper.*;
import top.mrxiaom.sqlhelper.conditions.Condition;
import top.mrxiaom.sqlhelper.conditions.EnumOperators;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BookDatabase extends AbstractPluginHolder implements IDatabase {
    private String tableName = "SHADOW";
    public final DatabaseManager manager;
    public BookDatabase(DatabaseManager manager) {
        super(manager.plugin);
        this.manager = manager;
    }

    @Override
    public void reload(Connection conn) {
        String prefix = plugin.getConfig().getString("database.table_prefix");
        String suffix = plugin.getConfig().getString("database.table_suffix");
        tableName = (prefix + "SHADOW" + suffix).toUpperCase();
        SQLang.createTable(conn, tableName, true,
                TableColumn.create(SQLValueType.ValueString.of(255), "id", EnumConstraints.PRIMARY_KEY),
                TableColumn.create(SQLValueType.LongText, "pages")
        );
    }

    public List<String> getPagesById(String id) {
        try (Connection conn = manager.getConnection()) {
            try (PreparedStatement ps = SQLang.select(tableName)
                    .column("id", "pages")
                    .where(
                            Condition.of("id", EnumOperators.EQUALS, id)
                    )
                    .limit(1)
                    .build(conn)
            ) {
                try (ResultSet result = ps.executeQuery()) {

                    if (result.next()) {
                        String example = result.getString("pages");
                        return Lists.partition(Arrays.asList(example.split("\n")), 14)
                                .stream().map(it -> {
                                    String s = String.join("\n", it);
                                    while (s.endsWith("\n")) {
                                        s = s.substring(0, s.length() - 1);
                                    }
                                    return s;
                                }).collect(Collectors.toList());
                    }
                }
            }
        } catch (Throwable e) {
            warn("通过ID获取书内容时出现错误", e);
        }
        return new ArrayList<>();
    }

    public void putOrUpdatePages(String id, List<String> pages) {
        List<String> list = new ArrayList<>();
        for (String page : pages) {
            String[] split = page.split("\n");
            list.addAll(Arrays.asList(split));

            int lines = split.length;
            for (int i = lines; i <= 14; i++) {
                list.add("\n");
            }
        }
        String pagesString = String.join("\n", list);
        try (Connection conn = manager.getConnection()) {
            try (PreparedStatement ps = SQLang.insertInto(tableName)
                    .addValues(
                            Pair.of("id", id),
                            Pair.of("pages", pagesString)
                    ).onDuplicateKeyUpdate(
                            "pages", pagesString
                    ).build(conn)
            ) {
                ps.execute();
            }
        } catch (SQLException e) {
            warn("更新书内容时出现错误", e);
        }
    }
}
