package raylcast.clans.handlers;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.*;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import raylcast.clans.models.ChargeStateChange;
import raylcast.clans.models.ClanType;
import raylcast.clans.runnables.LimitedRunnable;
import raylcast.clans.services.AbilityChargeTimer;
import raylcast.clans.services.TimeCounter;
import raylcast.clans.services.TimeCounterRunnable;

public class FireworkerHandler extends ClanHandler {
    private final double LavaResistance = 0.5;
    private final int ExplosionDelayTicks = 50;
    private final int ExplosionPower = 6;
    private final int RocketJumpOverchargePower = 5;
    private final int ExpectedTPS = 20;

    private final float MaxRocketJumpPower = 3f;
    private final float MaxJumpHeightChargeTime = 4500;
    private final float MaxRocketJumpChargeTime = 5000;

    private final int RocketJumpCooldownTicks = 120;

    private TimeCounter WaterCounter;

    private AbilityChargeTimer RocketJumpChargeTimer;

    public void onEnable(){
        WaterCounter = new TimeCounter();

        RocketJumpChargeTimer = new AbilityChargeTimer(Plugin,
            (player) -> {
                player.setWalkSpeed(0);
            },
            (player, time) -> {
                var world = player.getWorld();

                if (time > MaxRocketJumpChargeTime){
                    world.createExplosion(player.getLocation(), RocketJumpOverchargePower);
                    player.setHealth(0);
                    return ChargeStateChange.Cancel;
                }
                if (time > MaxJumpHeightChargeTime){
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2, 2);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 100);
                }

                world.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1,0), (int)(time / 33), 0.33, 1.8, 0.33);
                return ChargeStateChange.None;
            }, 3,
            (player, time) -> {
                player.setWalkSpeed(0.2f);

                var world = player.getWorld();
                world.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 4, 4);
                return 0;
            },
            (player, time) -> {
                player.setWalkSpeed(0.2f);

                var world = player.getWorld();
                var velocity = player.getVelocity();

                if (velocity.getY() < -0.1){
                    return 0;
                }
                if (time > 3000){
                    var entities = player.getNearbyEntities(4, 4, 4);

                    for(var entity : entities){
                        entity.setFireTicks((int)(ExpectedTPS * time / 750));
                    }
                }

                var newVelocity = MaxRocketJumpPower * (time / MaxJumpHeightChargeTime);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.getVelocity().getY() <= 0){
                            cancel();
                        }

                        world.spawnParticle(Particle.GLOW, player.getLocation(), 10, 0,0, 0);
                    }
                }.runTaskTimer(Plugin, 1, 1);

                velocity.setY(newVelocity);
                player.setVelocity(velocity);
                world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 10, 10);
                return RocketJumpCooldownTicks;
            }
        );

        Bukkit.getScheduler().runTaskTimer(Plugin, () -> {
            long now = System.currentTimeMillis();

            WaterCounter.StartTimes.forEach((player, startTime) -> {
                long timeInWater = now - startTime;

                if (timeInWater > 5000){
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 65, (int)timeInWater / 2000, true, false));
                }
                if (timeInWater > 10000){
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 65, (int)timeInWater / 10000, true, false));
                }

                player.damage(timeInWater / 6000d);
            });
        }, 60, 60);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent e){
        var player = e.getPlayer();

        if (!isMember(player, ClanType.Fireborn)){
            return;
        }

        if (!e.isSneaking() || player.getLocation().getDirection().normalize().getY() < 0.9) {
            RocketJumpChargeTimer.cancelCharge(player);
            return;
        }

        RocketJumpChargeTimer.startCharge(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e){
        var player = e.getPlayer();

        if (!isMember(player, ClanType.Fireborn)){
            return;
        }

        if (!player.isSneaking() || player.getLocation().getDirection().normalize().getY() < 0.9) {
            RocketJumpChargeTimer.cancelCharge(player);
        }
        else {
            RocketJumpChargeTimer.startCharge(player);
        }

        var block = player.getLocation().getBlock();
        var material = block.getType();
        if (material == Material.WATER || material == Material.SEA_PICKLE ||
            material == Material.SEAGRASS  || material == Material.TALL_SEAGRASS ||
            block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged())
        {
            WaterCounter.TryStartCounting(player);
            return;
        }

        WaterCounter.TryStopCounting(player);
    }


    @EventHandler(ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent e){
        var player = e.getPlayer();

        if (!isMember(player, ClanType.Fireborn)){
            return;
        }

        if (e.getAction() != Action.LEFT_CLICK_AIR){
            return;
        }

        RocketJumpChargeTimer.releaseCharge(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamaged(EntityDamageEvent e){
        if (!(e.getEntity() instanceof Player player)){
            return;
        }
        if (!isMember(player, ClanType.Fireborn)){
            return;
        }
        if (e.getCause() == EntityDamageEvent.DamageCause.LAVA){
            e.setDamage(e.getDamage() * LavaResistance);
        }
        if (e.getCause() != EntityDamageEvent.DamageCause.FIRE &&
            e.getCause() != EntityDamageEvent.DamageCause.HOT_FLOOR &&
            e.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK){
            return;
        }

        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false)
    public void onPlayerDeath(PlayerDeathEvent e){
        var deathLocation = e.getEntity().getLocation();
        var player = e.getEntity();
        var world = player.getWorld();

        if (!isMember(player, ClanType.Fireborn)){
            return;
        }

        WaterCounter.TryStopCounting(player);

        Logger.info("Fireworker died, scheduled explosion");

        var task = Bukkit.getScheduler().runTaskTimerAsynchronously(Plugin, () -> {
            world.spawnParticle(Particle.FIREWORKS_SPARK, deathLocation, 100);
            world.spawnParticle(Particle.FLAME, deathLocation, 100);
            world.playSound(deathLocation, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 1);
        } , 0, 5);

        Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () ->
        {
            Logger.info("Executing explosion");
            task.cancel();
            world.createExplosion(deathLocation, ExplosionPower);

        }, ExplosionDelayTicks);
    }

    private PotionEffect getPunishmentEffect(){
        switch ( Random.nextInt(6)){
            case 0:
                return new PotionEffect(PotionEffectType.WITHER, getRandomTickCount(3,9), Random.nextInt(1, 4));
            case 1:
                return new PotionEffect(PotionEffectType.HUNGER, getRandomTickCount(5, 60), Random.nextInt(2, 6));
            case 2:
                return new PotionEffect(PotionEffectType.SLOW, getRandomTickCount(5, 20), Random.nextInt(2, 6));
            case 3:
                return new PotionEffect(PotionEffectType.SLOW_DIGGING, getRandomTickCount(8, 60), Random.nextInt(2, 5));
            case 4:
                return new PotionEffect(PotionEffectType.CONFUSION, getRandomTickCount(6, 20), 1);
            case 5:
                return new PotionEffect(PotionEffectType.POISON, getRandomTickCount(2, 8), Random.nextInt(1, 4));
            default:
                throw new UnsupportedOperationException("This should never be executed!");
        }
    }

    private int getRandomTickCount(int minSeconds, int maxSeconds){
        return ExpectedTPS * Random.nextInt(minSeconds, maxSeconds);
    }


}
