package top.mrxiaom.fix.book;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import top.mrxiaom.fix.book.commands.CommandMain;
import top.mrxiaom.fix.book.db.BookReplace;
import top.mrxiaom.fix.book.func.AbstractPluginHolder;
import top.mrxiaom.fix.book.func.DatabaseManager;

import static top.mrxiaom.fix.book.utils.Util.stackTraceToString;

@SuppressWarnings({"unused"})
public class ShadowBookPlugin extends JavaPlugin implements Listener, TabCompleter {
    private static ShadowBookPlugin instance;
    public static ShadowBookPlugin getInstance() {
        return instance;
    }
    ProtocolManager protocolManager;

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    @Override
    public void onEnable() {
        instance = this;

        loadHooks();

        loadFunctions();
        reloadConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("插件加载完毕");
    }

    public void loadFunctions() {
        try {
            for (Class<?> clazz : Lists.newArrayList(
                    CommandMain.class, DatabaseManager.class,
                    BookReplace.class
            )) {
                clazz.getDeclaredConstructor(getClass()).newInstance(this);
            }
        } catch (Throwable t) {
            getLogger().warning(stackTraceToString(t));
        }
    }

    public void loadHooks() {
        protocolManager = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public void onDisable() {
        AbstractPluginHolder.disableAllModule();
        getLogger().info("插件已卸载");
    }

    @Override
    public void reloadConfig() {
        this.saveDefaultConfig();
        super.reloadConfig();

        FileConfiguration config = getConfig();
        AbstractPluginHolder.reloadAllConfig(config);
    }
}
