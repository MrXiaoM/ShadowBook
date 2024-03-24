package top.mrxiaom.fix.book.db;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.Converters;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NBTType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.fix.book.ShadowBookPlugin;
import top.mrxiaom.fix.book.func.AbstractPluginHolder;
import top.mrxiaom.fix.book.func.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class BookReplace extends AbstractPluginHolder {
    private static final String TAG_ID_KEY = "shadowbook:id";
    public BookReplace(ShadowBookPlugin plugin) {
        super(plugin);
        plugin.getProtocolManager().addPacketListener(new PacketSendHook());
        plugin.getProtocolManager().addPacketListener(new PacketReceiveHook());
    }

    public class PacketSendHook extends PacketAdapter {
        public PacketSendHook() {
            super(new AdapterParameteters()
                    .plugin(BookReplace.this.plugin)
                    .serverSide()
                    .types(
                            PacketType.Play.Server.SET_SLOT,
                            PacketType.Play.Server.WINDOW_ITEMS
                    )
                    .optionAsync());
        }

        @Override
        public void onPacketSending(PacketEvent event) {
            PacketContainer packet = event.getPacket();
            if (packet.getType().equals(PacketType.Play.Server.SET_SLOT)) {
                onSendingSetSlot(packet);
            }
            if (packet.getType().equals(PacketType.Play.Server.WINDOW_ITEMS)) {
                onSendingWindowItems(packet);
            }
        }

        @Nullable
        private ItemStack handleItem(ItemStack item) {
            NBTContainer nbtItem = NBTItem.convertItemtoNBT(item);
            NBTCompound tags = nbtItem.getCompound("tags");
            if (tags != null && tags.hasTag(TAG_ID_KEY, NBTType.NBTTagString)) {
                String id = nbtItem.getString(TAG_ID_KEY);
                nbtItem.removeKey(TAG_ID_KEY);
                ItemStack newItem = NBTItem.convertNBTtoItem(nbtItem);
                if (newItem != null) {
                    ItemMeta meta = newItem.getItemMeta();
                    if (meta instanceof BookMeta) {
                        BookMeta book = (BookMeta) meta;
                        // 获取书本内容，写入虚拟物品
                        List<String> pages = DatabaseManager.inst().example.getPagesById(id);
                        if (!pages.isEmpty()) {
                            book.setPages(pages);
                            newItem.setItemMeta(book);
                        }
                        return newItem;
                    }
                }
            }
            return null;
        }

        private void onSendingSetSlot(PacketContainer packet) {
            ItemStack item = packet.getItemModifier().read(0);
            ItemStack newItem = handleItem(item);
            if (newItem != null) {
                packet.getItemModifier().write(0, newItem);
            }
        }

        private void onSendingWindowItems(PacketContainer packet) {
            AtomicBoolean flag = new AtomicBoolean(false);
            List<ItemStack> items = packet.getItemListModifier().read(0);
            items = items.stream().map(it -> {
                ItemStack newItem = handleItem(it);
                if (newItem != null) {
                    flag.set(true);
                    return newItem;
                }
                return it;
            }).collect(Collectors.toList());
            if (flag.get()) {
                packet.getItemListModifier().write(0, items);
            }
        }
    }

    public class PacketReceiveHook extends PacketAdapter {
        public PacketReceiveHook() {
            super(new AdapterParameteters()
                    .plugin(BookReplace.this.plugin)
                    .clientSide()
                    .types(
                            PacketType.Play.Client.B_EDIT
                    )
                    .optionAsync());
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            PacketContainer packet = event.getPacket();
            if (packet.getItemModifier().size() > 0) { // old version
                oldBookEdit(packet, event);
                return;
            }
            event.setCancelled(true);
            newBookEdit(packet, event);
        }

        private void oldBookEdit(PacketContainer packet, PacketEvent event) {
            Player player = event.getPlayer();
            ItemStack item = packet.getItemModifier().read(0);
            if (!item.getType().equals(Material.WRITABLE_BOOK)) return;
            NBTContainer nbtItem = NBTItem.convertItemtoNBT(item);
            NBTCompound tags = nbtItem.getOrCreateCompound("tags");
            String id;
            if (tags.hasTag(TAG_ID_KEY, NBTType.NBTTagString)) {
                id = tags.getString(TAG_ID_KEY);
            } else {
                tags.setString(TAG_ID_KEY, id = UUID.randomUUID().toString().replace("-", "") + "-" + player.getName());
            }
            item = NBTItem.convertNBTtoItem(nbtItem);
            if (item == null) return;
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof BookMeta) {
                BookMeta book = (BookMeta) item.getItemMeta();
                List<String> pages = book.getPages();
                // TODO: 写入文本到数据库
                DatabaseManager.inst().example.putOrUpdatePages(id, pages);
                book.setPages(new ArrayList<>());
                item.setItemMeta(book);
            }
            packet.getItemModifier().write(0, item);
        }

        private void newBookEdit(PacketContainer packet, PacketEvent event) {
            Player player = event.getPlayer();

            // 快捷栏 或 副手
            int slot = packet.getIntegers().read(0);

            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || !item.getType().equals(Material.WRITABLE_BOOK)) return;
            NBTContainer nbtItem = NBTItem.convertItemtoNBT(item);
            NBTCompound tags = nbtItem.getOrCreateCompound("tags");
            String id;
            if (tags.hasTag(TAG_ID_KEY, NBTType.NBTTagString)) {
                id = tags.getString(TAG_ID_KEY);
            } else {
                tags.setString(TAG_ID_KEY, id = UUID.randomUUID().toString().replace("-", "") + "-" + event.getPlayer().getName());
            }
            item = NBTItem.convertNBTtoItem(nbtItem);
            if (item == null) return;

            List<String> pages = packet.getLists(Converters.passthrough(String.class)).read(0);
            DatabaseManager.inst().example.putOrUpdatePages(id, pages);

            // 当标题不为 null 时，代表给书签名
            String title = packet.getOptionals(Converters.passthrough(String.class)).read(0).orElse(null);
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof BookMeta) {
                BookMeta book = (BookMeta) meta;
                book.setPages(new ArrayList<>());
                if (title != null) {
                    item.setType(Material.WRITTEN_BOOK);
                    book.setTitle(title);
                    book.setAuthor(player.getName());
                }
                item.setItemMeta(book);
            }
            player.getInventory().setItem(slot, item);
        }
    }
}
