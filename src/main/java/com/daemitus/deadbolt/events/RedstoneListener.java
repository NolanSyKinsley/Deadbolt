package com.daemitus.deadbolt.events;

import com.daemitus.deadbolt.Deadbolt;
import com.daemitus.deadbolt.Deadbolted;
import com.daemitus.deadbolt.listener.ListenerManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

public final class RedstoneListener implements Listener {

    private final Deadbolt plugin = Deadbolt.instance;

    public RedstoneListener() {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockRedstoneChange(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        if (block == null) {
            return;
        }
        if (!plugin.config.redstone_protected_blockids.contains(block.getTypeId())) {
            return;
        }
        Deadbolted db = Deadbolted.get(block);
        if (db.isProtected() && !ListenerManager.canRedstoneChange(db, event) && !db.isEveryone()) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }
}
