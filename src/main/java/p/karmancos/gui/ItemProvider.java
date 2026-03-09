package p.karmancos.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ItemProvider {
    ItemStack getItem(Player player);
}

