package com.daemitus.deadbolt.commands;

import com.daemitus.deadbolt.Deadbolt;
import com.daemitus.deadbolt.Deadbolted;
import com.daemitus.deadbolt.Perm;
import com.daemitus.deadbolt.util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeadboltCommandExecutor implements CommandExecutor {

    private final Deadbolt plugin;

    public DeadboltCommandExecutor(final Deadbolt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player)
            return onPlayerCommand((Player) sender, command, label, args);
        else
            return onConsoleCommand(sender, command, label, args);
    }

    private boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        int arg = args.length;

        if (arg == 0) {
            player.sendMessage(ChatColor.RED + "Deadbolt v" + plugin.getDescription().getVersion());
            player.sendMessage(ChatColor.RED + plugin.config.cmd_help_editsign);
            player.sendMessage(ChatColor.RED + plugin.config.cmd_help_fix);
            player.sendMessage(ChatColor.RED + plugin.config.cmd_help_fixAll);
            if (plugin.config.hasPermission(player, Perm.command_reload))
                player.sendMessage(ChatColor.RED + plugin.config.cmd_help_reload);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
            return reload(player);
        if (args[0].equalsIgnoreCase("fix"))
            return fix(player);
        if (args[0].equalsIgnoreCase("fixall"))
            return fixAll(player);
        try {
            return lineChange(player, Integer.valueOf(args[0]), args);
        } catch (NumberFormatException ex) {
        }

        plugin.config.sendMessage(player, ChatColor.RED, plugin.config.cmd_command_not_found);
        return true;
    }

    private boolean reload(Player player) {
        plugin.config.sendMessage(player, ChatColor.RED, plugin.config.cmd_reload);
        plugin.config.load();
        return true;
    }

    private boolean lineChange(Player player, int lineNum, String[] args) {
        if (lineNum < 1 || lineNum > 4) {
            plugin.config.sendMessage(player, ChatColor.RED, plugin.config.cmd_line_num_out_of_range);
            return true;
        }
        Block block = plugin.config.selectedSign.get(player);
        if (block == null) {
            plugin.config.sendMessage(player, ChatColor.RED, plugin.config.cmd_sign_not_selected);
            return true;
        }
        if (!block.getType().equals(Material.WALL_SIGN)) {
            plugin.config.sendMessage(player, ChatColor.RED, plugin.config.cmd_sign_selected_error);
            plugin.config.selectedSign.remove(player);
            return true;
        }

        lineNum--;
        Sign sign = (Sign) block.getState();
        String lines[] = sign.getLines();

        String text = "";
        for (int i = 1; i < args.length; i++)
            text += args[i] + (i + 1 < args.length ? " " : "");
        if (plugin.config.hasPermission(player, Perm.user_color))
            text = Util.createColor(text);
        text = Util.formatForSign(text);
        if (lineNum == 0) {
            if (Util.removeColor(lines[0]).equalsIgnoreCase(Util.removeColor(text))) {
                lines[0] = text;
            } else {
                plugin.config.sendMessage(player, ChatColor.RED, plugin.config.cmd_identifier_not_changeable);
                return true;
            }
        } else if (lineNum == 1 && plugin.config.isPrivate(Util.removeColor(lines[0]))) {
            if (Util.removeColor(lines[1]).equalsIgnoreCase(Util.removeColor(text))) {
                lines[1] = text;
            } else {
                plugin.config.sendMessage(player, ChatColor.RED, plugin.config.cmd_owner_not_changeable);
                return true;
            }
        } else {
            lines[lineNum] = text;
        }
        if (plugin.config.deselectSign)
            plugin.config.selectedSign.remove(player);

        boolean isPrivate = plugin.config.isPrivate(Util.removeColor(lines[0]));
        boolean isMoreUsers = plugin.config.isMoreUsers(Util.removeColor(lines[0]));
        if (isPrivate)
            for (int i = 0; i < 4; i++)
                lines[i] = Util.formatForSign(plugin.config.default_colors_private[i] + lines[i]);
        else if (isMoreUsers)
            for (int i = 0; i < 4; i++)
                lines[i] = Util.formatForSign(plugin.config.default_colors_moreusers[i] + lines[i]);
        sign.update(true);
        plugin.config.sendMessage(player, ChatColor.GOLD, plugin.config.cmd_sign_updated);
        return true;
    }

    private boolean fix(Player player) {
        Block block = player.getTargetBlock(null, 100);
        Deadbolted db = Deadbolted.get(block);

        if (db.isProtected()) {
            if (db.isOwner(player)) {
                fixHelper(player, block, true);
            } else if (plugin.config.hasPermission(player, Perm.admin_commands)) {
                fixHelper(player, block, true);
                plugin.config.sendMessage(player, ChatColor.RED, plugin.config.msg_admin_block_fixed, db.getOwner());
            } else {
                plugin.config.sendMessage(player, ChatColor.RED, plugin.config.cmd_fix_notowned);
            }
        }
        return true;
    }

    private void fixHelper(Player player, Block block, boolean recurse) {
        switch (block.getType()) {
            case WOODEN_DOOR:
            case IRON_DOOR_BLOCK:
                block.setData((byte) (block.getData() ^ 0x4));
                break;
            case TRAP_DOOR:
            case FENCE_GATE:
                block.setData((byte) (block.getData() ^ 0x4));
                break;
            default:
                plugin.config.sendMessage(player, ChatColor.RED, plugin.config.cmd_fix_bad_type);
        }
    }

    private boolean fixAll(Player player) {
        Block block = player.getTargetBlock(null, 100);
        Deadbolted db = Deadbolted.get(block);

        if (db.isProtected()) {
            if (db.isOwner(player)) {
                fixAllHelper(player, block, db);
            } else if (plugin.config.hasPermission(player, Perm.admin_commands)) {
                plugin.config.sendMessage(player, ChatColor.RED, plugin.config.msg_admin_block_fixed, db.getOwner());
                fixAllHelper(player, block, db);
            } else {
                plugin.config.sendMessage(player, ChatColor.RED, plugin.config.cmd_fix_notowned);
            }
        }
        return true;
    }

    private void fixAllHelper(Player player, Block block, Deadbolted db) {
        switch (block.getType()) {
            case WOODEN_DOOR:
            case IRON_DOOR_BLOCK:
            case TRAP_DOOR:
            case FENCE_GATE:
                for (Block b : db.getBlocks())
                    if (b.getType().equals(block.getType()))
                        b.setData((byte) (b.getData() ^ 0x4));
                break;
            default:
                plugin.config.sendMessage(player, ChatColor.RED, plugin.config.cmd_fix_bad_type);
        }
    }

    private boolean onConsoleCommand(CommandSender sender, Command command, String label, String[] args) {
        int arg = args.length;
        if (arg == 0) {
            sender.sendMessage("[Deadbolt] " + plugin.getDescription().getVersion() + " options: reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
            return creload(sender);

        sender.sendMessage("[Deadbolt] " + plugin.config.cmd_console_command_not_found);
        return true;
    }

    private boolean creload(CommandSender sender) {
        sender.sendMessage("[Deadbolt] " + plugin.config.cmd_console_reload);
        plugin.config.load();
        return true;
    }
}
