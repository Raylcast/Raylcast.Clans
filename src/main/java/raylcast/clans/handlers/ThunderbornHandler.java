package raylcast.clans.handlers;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.permissions.Permission;
import raylcast.clans.services.TimedAbility;

public class ThunderbornHandler extends ClanHandler {
    private final double GuardianDamageMultiplier = 0.66;

    private final int LightningDelayTicks = 50;
    private final int OverchargeDuration = 20000;

    private final int KnockbackIntensity = 3;

    private TimedAbility OverchargeAbility;

    public ThunderbornHandler(Permission clanMemberPermission) {
        super(clanMemberPermission);
    }

    @Override
    public void onEnable() {
        OverchargeAbility = new TimedAbility(Plugin,
            (player) -> {
                player.setWalkSpeed(0.28f);
            },
            (player, time) -> {
                return false;
            }, 100,
            (player, time) -> {
                player.setWalkSpeed(0.2f);
                return 0;
            });
    }

    @Override
    public void onDisable() {
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e){
        if (!(e.getDamager() instanceof Player player)){
            return;
        }
        if (!isMember(player)){
            return;
        }
        if(!OverchargeAbility.isCurrentlyActive(player)){
            return;
        }

        var damagedEntity = e.getEntity();
        damagedEntity.setVelocity(player.getLocation().getDirection().setY(0).normalize().multiply(KnockbackIntensity));

        var world = player.getWorld();

        world.spawnParticle(Particle.GLOW, damagedEntity.getLocation(), 10);
        world.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 2, 2);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e){
        if (!(e.getEntity() instanceof Player player)){
            return;
        }
        if (!isMember(player)){
            return;
        }
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK){
            return;
        }
        if (e.getDamager().getType() != EntityType.GUARDIAN){
            return;
        }

        player.sendMessage("Overcharged!");
        OverchargeAbility.startAbility(player, 20000);
        e.setDamage(e.getDamage() * GuardianDamageMultiplier);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e){
        var deathLocation = e.getEntity().getLocation();
        var player = e.getEntity();

        if (!isMember(player)){
            return;
        }

        var world = player.getWorld();

        Logger.info("Cyborg died, scheduled lightning strike");

        var task = Bukkit.getScheduler().runTaskTimerAsynchronously(Plugin, () -> {
            world.spawnParticle(Particle.FIREWORKS_SPARK, deathLocation, 100);
            world.spawnParticle(Particle.GLOW_SQUID_INK, deathLocation, 100);
            world.playSound(deathLocation, Sound.ENTITY_CAT_HISS, 1, 1);
        } , 0, 5);

        Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () ->
        {
            Logger.info("Executing lightning strike");
            task.cancel();

            world.strikeLightningEffect(deathLocation);

            var nearbyEntities = player.getNearbyEntities(3, 3, 3);

            for(var entity : nearbyEntities){
                world.strikeLightning(entity.getLocation());
            }

            if (nearbyEntities.isEmpty()){
                world.strikeLightning(deathLocation.add(2, 0, -2));
                world.strikeLightning(deathLocation.add(-3, 0, 0));
            }

        }, LightningDelayTicks);
    }
}
