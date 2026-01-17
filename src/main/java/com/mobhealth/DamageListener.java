package com.mobhealth;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Vector3f;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class DamageListener implements Listener {

    private final DecimalFormat df = new DecimalFormat("#.##");
    private final MobHealth plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, Map<UUID, HpBarSession>> activeHpBars = new ConcurrentHashMap<>();

    // Cache the serializer to avoid lookups every tick
    private static WrappedDataWatcher.Serializer componentSerializer;
    private static WrappedDataWatcher.Serializer vector3fSerializer;
    private static boolean vector3fSerializerChecked = false;

    private static class HpBarSession {
        final int entityId;
        BukkitTask task;

        HpBarSession(int entityId, BukkitTask task) {
            this.entityId = entityId;
            this.task = task;
        }
    }

    public DamageListener(MobHealth plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * Helper to get the correct Component serializer for 1.21.4
     */
    private WrappedDataWatcher.Serializer getComponentSerializer() {
        if (componentSerializer == null) {
            // For 1.21.4, TextDisplay text (index 22) requires a Component serializer.
            // Registry.getChatComponentSerializer(false) correctly returns the non-optional serializer.
            componentSerializer = Registry.getChatComponentSerializer(false);
        }
        return componentSerializer;
    }

    private WrappedDataWatcher.Serializer getVector3fSerializer() {
        if (!vector3fSerializerChecked) {
            try {
                vector3fSerializer = Registry.get(Vector3f.class);
            } catch (Throwable e) {
                plugin.getLogger().warning("Could not find Vector3f serializer. Translation metadata will be skipped. Ensure ProtocolLib is up to date.");
            }
            vector3fSerializerChecked = true;
        }
        return vector3fSerializer;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile projectile = (org.bukkit.entity.Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }

        if (attacker == null) {
            return;
        }

        LivingEntity victim = (LivingEntity) event.getEntity();

        // Check Blacklist
        if (isBlacklisted(victim.getType())) {
            return;
        }

        if (!attacker.hasPermission("mobhealth.see")) {
            return;
        }

        if (!plugin.isMobHealthVisible(attacker.getUniqueId())) {
            return;
        }

        // 距離チェック
        double viewDistance = plugin.getConfig().getDouble("display.view-distance", 16.0);
        if (attacker.getLocation().distanceSquared(victim.getLocation()) > viewDistance * viewDistance) {
            return;
        }

        double damage = event.getFinalDamage();
        double currentHealth = Math.max(0, victim.getHealth() - damage);
        
        updateOrSpawnHpBar(attacker, victim, currentHealth);
        spawnDamageIndicator(attacker, victim, damage);
    }

    private boolean isBlacklisted(EntityType type) {
        List<String> blacklist = plugin.getConfig().getStringList("blacklist.entities");
        return blacklist.contains(type.name());
    }

    // HP表示用のエンティティをスポーン・更新させる (Singleton per Mob)
    private void updateOrSpawnHpBar(Player player, LivingEntity victim, double currentHealth) {
        Location loc = victim.getEyeLocation().add(0, 0.25, 0); // 初期位置 (マウントするのであまり関係ないが)

        // Configからフォーマット取得
        String format = plugin.getConfig().getString("display.hp-format", "&aHP: &a{hp}");
        String text = format.replace("{hp}", df.format(currentHealth));
        Component textComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(text);

        Map<UUID, HpBarSession> playerBars = activeHpBars.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        HpBarSession session = playerBars.get(victim.getUniqueId());

        if (session != null) {
            // 既存のバーがある場合は更新
            updateHpBarMetadata(player, session.entityId, textComponent);
            
            // タスクをキャンセルして再スケジュール (寿命を延ばす)
            if (session.task != null && !session.task.isCancelled()) {
                session.task.cancel();
            }
            session.task = scheduleDestroyTask(player, session.entityId, victim.getUniqueId());
        } else {
            // 新規作成
            int entityId = ThreadLocalRandom.current().nextInt(1000000, Integer.MAX_VALUE);
            spawnHpBarEntity(player, loc, entityId, textComponent, victim);
            
            BukkitTask task = scheduleDestroyTask(player, entityId, victim.getUniqueId());
            playerBars.put(victim.getUniqueId(), new HpBarSession(entityId, task));
        }
    }

    private void updateHpBarMetadata(Player player, int entityId, Component textComponent) {
        PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, entityId);

        String jsonText = GsonComponentSerializer.gson().serialize(textComponent);
        WrappedChatComponent wrappedText = WrappedChatComponent.fromJson(jsonText);

        List<WrappedDataValue> dataValues = new ArrayList<>();
        // Index 23: Text (Component) - Correct for 1.21.1
        dataValues.add(new WrappedDataValue(23, getComponentSerializer(), wrappedText.getHandle()));

        metadataPacket.getDataValueCollectionModifier().write(0, dataValues);

        try {
            protocolManager.sendServerPacket(player, metadataPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spawnHpBarEntity(Player player, Location startLoc, int entityId, Component textComponent, LivingEntity followTarget) {
        UUID entityUuid = UUID.randomUUID();

        // 1. Spawn Packet
        PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawnPacket.getIntegers().write(0, entityId);
        spawnPacket.getUUIDs().write(0, entityUuid);
        spawnPacket.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY);
        
        spawnPacket.getDoubles()
                .write(0, startLoc.getX())
                .write(1, startLoc.getY())
                .write(2, startLoc.getZ());
        
        spawnPacket.getBytes()
                .write(0, (byte) (startLoc.getPitch() * 256.0F / 360.0F))
                .write(1, (byte) (startLoc.getYaw() * 256.0F / 360.0F));

        // 2. Metadata Packet
        PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, entityId);

        String jsonText = GsonComponentSerializer.gson().serialize(textComponent);
        WrappedChatComponent wrappedText = WrappedChatComponent.fromJson(jsonText);

        List<WrappedDataValue> dataValues = new ArrayList<>();

        // Index 23: Text (Component) - Correct for 1.21.1
        dataValues.add(new WrappedDataValue(23, getComponentSerializer(), wrappedText.getHandle()));

        // Index 5: No Gravity (Boolean)
        WrappedDataWatcher.Serializer boolSerializer = Registry.get(Boolean.class);
        if (boolSerializer != null) {
             dataValues.add(new WrappedDataValue(5, boolSerializer, true));
        }

        // Translation (Index 11 for 1.21.1)
        WrappedDataWatcher.Serializer vecSerializer = getVector3fSerializer();
        if (vecSerializer != null) {
            dataValues.add(new WrappedDataValue(11, vecSerializer, new Vector3f(0f, 0.2f, 0f)));
        }

        // Index 15: Billboard (Byte) - 3 = CENTER (Correct for 1.21.1)
        WrappedDataWatcher.Serializer byteSerializer = Registry.get(Byte.class);
        if (byteSerializer != null) {
            dataValues.add(new WrappedDataValue(15, byteSerializer, (byte) 3));
        }

        // Index 25: Background Color (Integer) - 0 = Transparent (Correct for 1.21.1)
        WrappedDataWatcher.Serializer intSerializer = Registry.get(Integer.class);
        if (intSerializer != null) {
            dataValues.add(new WrappedDataValue(25, intSerializer, 0));
        }

        metadataPacket.getDataValueCollectionModifier().write(0, dataValues);

        try {
            protocolManager.sendServerPacket(player, spawnPacket);
            protocolManager.sendServerPacket(player, metadataPacket);
            
            // Mount Packet
            PacketContainer mountPacket = protocolManager.createPacket(PacketType.Play.Server.MOUNT);
            mountPacket.getIntegers().write(0, followTarget.getEntityId());
            mountPacket.getIntegerArrays().write(0, new int[]{ entityId });
            protocolManager.sendServerPacket(player, mountPacket);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BukkitTask scheduleDestroyTask(Player player, int entityId, UUID mobId) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                // Remove from map
                Map<UUID, HpBarSession> playerBars = activeHpBars.get(player.getUniqueId());
                if (playerBars != null) {
                    playerBars.remove(mobId);
                }

                PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                destroyPacket.getIntLists().write(0, Collections.singletonList(entityId));
                try {
                    protocolManager.sendServerPacket(player, destroyPacket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.runTaskLater(plugin, 40L); // 2秒後に消える
    }

    // ダメージ表示用のエンティティをスポーンさせる (Fire and Forget)
    private void spawnDamageIndicator(Player player, LivingEntity victim, double damage) {
        Location loc = victim.getEyeLocation().add(0, -0.2, 0); 
        
        double offsetX = (Math.random() - 0.5) * 0.5;
        double offsetZ = (Math.random() - 0.5) * 0.5;
        loc.add(offsetX, 0, offsetZ);

        String format = plugin.getConfig().getString("display.damage-format", "&c- {damage}");
        
        String text = format.replace("{damage}", df.format(damage));
        Component textComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(text);

        spawnFloatingText(player, loc, textComponent); 
    }

    private void spawnFloatingText(Player player, Location startLoc, Component textComponent) {
        int entityId = ThreadLocalRandom.current().nextInt(1000000, Integer.MAX_VALUE);
        UUID entityUuid = UUID.randomUUID();

        PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        spawnPacket.getIntegers().write(0, entityId);
        spawnPacket.getUUIDs().write(0, entityUuid);
        spawnPacket.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY);
        
        spawnPacket.getDoubles().write(0, startLoc.getX()).write(1, startLoc.getY()).write(2, startLoc.getZ());
        spawnPacket.getBytes().write(0, (byte) 0).write(1, (byte) 0);

        PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        metadataPacket.getIntegers().write(0, entityId);

        String jsonText = GsonComponentSerializer.gson().serialize(textComponent);
        WrappedChatComponent wrappedText = WrappedChatComponent.fromJson(jsonText);
        List<WrappedDataValue> dataValues = new ArrayList<>();
        
        // Use Index 23 for Text (1.21.1)
        dataValues.add(new WrappedDataValue(23, getComponentSerializer(), wrappedText.getHandle()));
        
        WrappedDataWatcher.Serializer byteSerializer = Registry.get(Byte.class);
        if (byteSerializer != null) {
            dataValues.add(new WrappedDataValue(15, byteSerializer, (byte) 3)); // Billboard (1.21.1)
        }
        
        WrappedDataWatcher.Serializer intSerializer = Registry.get(Integer.class);
        if (intSerializer != null) {
            dataValues.add(new WrappedDataValue(25, intSerializer, 0)); // Background (1.21.1)
        }

        metadataPacket.getDataValueCollectionModifier().write(0, dataValues);

        try {
            protocolManager.sendServerPacket(player, spawnPacket);
            protocolManager.sendServerPacket(player, metadataPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new BukkitRunnable() {
            int tick = 0;
            Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                if (tick >= 30 || !player.isOnline()) { 
                    PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                    destroyPacket.getIntLists().write(0, Collections.singletonList(entityId));
                    try {
                        protocolManager.sendServerPacket(player, destroyPacket);
                    } catch (Exception e) {}
                    this.cancel();
                    return;
                }
                currentLoc.add(0, 0.03, 0); // Speed reduced from 0.05 to 0.03
                try {
                    PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
                    teleportPacket.getIntegers().write(0, entityId);
                    teleportPacket.getDoubles().write(0, currentLoc.getX()).write(1, currentLoc.getY()).write(2, currentLoc.getZ());
                    teleportPacket.getBytes().write(0, (byte) 0).write(1, (byte) 0);
                    teleportPacket.getBooleans().write(0, false); 
                    protocolManager.sendServerPacket(player, teleportPacket);
                } catch (Exception e) {}
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
