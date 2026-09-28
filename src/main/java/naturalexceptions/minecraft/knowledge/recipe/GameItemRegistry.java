package naturalexceptions.minecraft.knowledge.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Universal item index discovering all registered items and blocks in the game,
 * with search and mod-domain filtering (@minecraft, @naturalexceptions).
 */
public final class GameItemRegistry {

    private static final GameItemRegistry INSTANCE = new GameItemRegistry();

    public static final class IndexedItem {

        private final ItemStack stack;
        private final String displayName;
        private final String modId;
        private final String unlocalizedName;

        public IndexedItem(ItemStack stack, String displayName, String modId, String unlocalizedName) {
            this.stack = stack.copy();
            this.displayName = displayName != null ? displayName : "";
            this.modId = modId != null ? modId.toLowerCase(Locale.ROOT) : "minecraft";
            this.unlocalizedName = unlocalizedName != null ? unlocalizedName.toLowerCase(Locale.ROOT) : "";
        }

        public ItemStack stack() {
            return stack.copy();
        }

        public String displayName() {
            return displayName;
        }

        public String modId() {
            return modId;
        }

        public String unlocalizedName() {
            return unlocalizedName;
        }
    }

    private final List<IndexedItem> indexedItems = new ArrayList<>();
    private final Set<String> indexedKeys = new HashSet<>();
    private boolean initialized = false;

    private GameItemRegistry() {}

    public static GameItemRegistry instance() {
        return INSTANCE;
    }

    public synchronized void reset() {
        indexedItems.clear();
        indexedKeys.clear();
        initialized = false;
    }

    public synchronized void init() {
        if (initialized) return;
        initialized = true;

        indexedItems.clear();
        indexedKeys.clear();

        try {
            Iterator iterator = Item.itemRegistry.iterator();
            while (iterator.hasNext()) {
                Object itemObj = iterator.next();
                if (!(itemObj instanceof Item)) continue;

                Item item = (Item) itemObj;
                if (item == Item.getItemFromBlock(Blocks.air)) continue;

                String modId = "unknown";
                try {
                    GameRegistry.UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(item);
                    if (uid != null && uid.modId != null) {
                        modId = uid.modId;
                    }
                } catch (Throwable ignored) {}

                List<ItemStack> subItems = new ArrayList<>();
                try {
                    item.getSubItems(item, (CreativeTabs) null, subItems);
                } catch (Throwable ignored) {}

                if (subItems.isEmpty()) {
                    try {
                        ItemStack defaultStack = new ItemStack(item);
                        if (defaultStack.getItem() != null) {
                            subItems.add(defaultStack);
                        }
                    } catch (Throwable ignored) {}
                }

                for (ItemStack stack : subItems) {
                    if (stack == null || stack.getItem() == null) continue;

                    String name = "";
                    try {
                        name = stack.getDisplayName();
                    } catch (Throwable t) {
                        continue; // Skip items that throw in getDisplayName
                    }

                    if (name == null || name.trim()
                        .isEmpty()) continue;
                    if (name.trim()
                        .equalsIgnoreCase("tile.null.name")) continue;

                    String unloc = "";
                    try {
                        unloc = stack.getUnlocalizedName();
                    } catch (Throwable ignored) {}

                    String itemKey = modId.toLowerCase(Locale.ROOT) + ":"
                        + unloc.toLowerCase(Locale.ROOT)
                        + ":"
                        + stack.getItemDamage()
                        + ":"
                        + name.toLowerCase(Locale.ROOT);
                    if (indexedKeys.add(itemKey)) indexedItems.add(new IndexedItem(stack, name, modId, unloc));
                }
            }
        } catch (Throwable ignored) {}
    }

    public List<IndexedItem> getAllIndexedItems() {
        init();
        return Collections.unmodifiableList(indexedItems);
    }

    /**
     * Search indexed items by keyword or @modid prefix.
     */
    public List<IndexedItem> search(String query) {
        init();
        if (query == null || query.trim()
            .isEmpty()) {
            return getAllIndexedItems();
        }

        String lowerQuery = query.toLowerCase(Locale.ROOT)
            .trim();
        String targetModId = null;

        if (lowerQuery.startsWith("@")) {
            int spaceIdx = lowerQuery.indexOf(' ');
            if (spaceIdx > 1) {
                targetModId = lowerQuery.substring(1, spaceIdx)
                    .trim();
                lowerQuery = lowerQuery.substring(spaceIdx + 1)
                    .trim();
            } else {
                targetModId = lowerQuery.substring(1)
                    .trim();
                lowerQuery = "";
            }
        }

        String[] tokens = lowerQuery.isEmpty() ? new String[0] : lowerQuery.split("\\s+");
        List<IndexedItem> results = new ArrayList<>();

        for (IndexedItem item : indexedItems) {
            if (targetModId != null && !targetModId.isEmpty()) {
                if (!item.modId()
                    .contains(targetModId)) {
                    continue;
                }
            }

            if (tokens.length == 0) {
                results.add(item);
                continue;
            }

            String name = item.displayName()
                .toLowerCase(Locale.ROOT);
            String unloc = item.unlocalizedName();

            boolean match = true;
            for (String token : tokens) {
                if (!name.contains(token) && !unloc.contains(token)) {
                    match = false;
                    break;
                }
            }

            if (match) {
                results.add(item);
            }
        }

        return results;
    }
}
