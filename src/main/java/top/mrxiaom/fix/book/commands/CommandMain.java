package top.mrxiaom.fix.book.commands;

import com.google.common.collect.Lists;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.fix.book.ShadowBookPlugin;
import top.mrxiaom.fix.book.func.AbstractPluginHolder;

import java.util.List;

public class CommandMain extends AbstractPluginHolder implements CommandExecutor, TabCompleter {
    public CommandMain(ShadowBookPlugin plugin) {
        super(plugin);
        registerCommand("shadowbook", this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.isOp()) {
            plugin.reloadConfig();
            return t(sender, "&a配置文件已重载");
        }
        return true;
    }

    private static final List<String> emptyList = Lists.newArrayList();
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return emptyList;
    }
}
