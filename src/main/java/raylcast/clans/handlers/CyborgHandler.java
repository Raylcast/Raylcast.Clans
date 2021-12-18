package raylcast.clans.handlers;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import raylcast.clans.models.ClanType;
import raylcast.clans.services.TimeCounterRunnable;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;

import java.util.HashSet;

public class CyborgHandler extends ClanHandler {
    private final double GuardianDamageMultiplier = 0.66;

    private final int LightningDelayTicks = 50;
    private final int OverchargeDuration = 20000;

    private TimeCounterRunnable OverchargeTimer;

    public CyborgHandler(){
    }

    @Override
    public void onEnable() {
        OverchargeTimer = new TimeCounterRunnable(Plugin, (player, time) -> {
            if (time > OverchargeDuration){
                player.sendMessage("Overcharge stop");
                OverchargeTimer.TryStopCounting(player);
                return;
            }

            player.sendMessage("Set light source");
            var world = player.getWorld();
            var location = player.getLocation().getBlock();
        }, 20, 20);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent e){
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e){
        if (!(e.getEntity() instanceof Player player)){
            return;
        }
        if (!isMember(player, ClanType.Cyborg)){
            return;
        }
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK){
            return;
        }
        if (e.getDamager().getType() != EntityType.GUARDIAN){
            return;
        }

        player.sendMessage("Overcharged!");
        OverchargeTimer.TryStartCounting(player);
        e.setDamage(e.getDamage() * GuardianDamageMultiplier);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e){
        var deathLocation = e.getEntity().getLocation();
        var player = e.getEntity();

        if (!isMember(player, ClanType.Cyborg)){
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
