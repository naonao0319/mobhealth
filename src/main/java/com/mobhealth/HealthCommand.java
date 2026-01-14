package com.mobhealth;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HealthCommand implements CommandExecutor, TabCompleter {

    private final MobHealth plugin;

    public HealthCommand(MobHealth plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players."));
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("toggle")) {
                if (!player.hasPermission("mobhealth.see")) {
                     String msg = plugin.getConfig().getString("messages.no-permission", "&cYou do not have permission to use this command.");
                     player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
                     return true;
                }
                boolean isHidden = plugin.toggleMobHealth(player.getUniqueId());
                String msgKey = isHidden ? "messages.toggle-off" : "messages.toggle-on";
                String defMsg = isHidden ? "&cMobHealth display disabled." : "&aMobHealth display enabled.";
                String msg = plugin.getConfig().getString(msgKey, defMsg);
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
                return true;
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("mobhealth.reload")) {
                    String msg = plugin.getConfig().getString("messages.no-permission", "&cYou do not have permission to use this command.");
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
                    return true;
                }
                plugin.reloadConfig();
                String msg = plugin.getConfig().getString("messages.reload", "&aMobHealth configuration reloaded.");
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
                return true;
            }
        }

        player.sendMessage(Component.text("Usage: /health <toggle|reload>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("mobhealth.see")) completions.add("toggle");
            if (sender.hasPermission("mobhealth.reload")) completions.add("reload");
            return completions;
        }
        return new ArrayList<>();
    }
}
