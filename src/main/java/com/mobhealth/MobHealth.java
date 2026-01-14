package com.mobhealth;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MobHealth extends JavaPlugin {

    private final Set<UUID> disabledPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        getLogger().info("MobHealth has been enabled!");
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getCommand("health").setExecutor(new HealthCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("MobHealth has been disabled!");
    }

    public boolean toggleMobHealth(UUID playerId) {
        if (disabledPlayers.contains(playerId)) {
            disabledPlayers.remove(playerId);
            return false; // Enabled
        } else {
            disabledPlayers.add(playerId);
            return true; // Disabled
        }
    }

    public boolean isMobHealthVisible(UUID playerId) {
        return !disabledPlayers.contains(playerId);
    }
}
