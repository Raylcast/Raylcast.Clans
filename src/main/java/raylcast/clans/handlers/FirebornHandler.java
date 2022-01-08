package raylcast.clans.handlers;

import org.bukkit.*;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.permissions.Permission;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import raylcast.clans.models.ChargeStateChange;
import raylcast.clans.models.EmptyState;
import raylcast.clans.models.ExplodedState;
import raylcast.clans.services.ChargedAbility;
import raylcast.clans.services.TimedAbility;

public class FirebornHandler extends ClanHandler {
    private final double LavaResistance = 0.5;
    private final int ExplosionDelayTicks = 50;
    private final int ExplosionPower = 6;
    private final int RocketJumpOverchargePower = 5;
    private final int ExpectedTPS = 20;

    private final float MaxRocketJumpPower = 3f;
    private final float MaxJumpHeightChargeTime = 4500;
    private final float MaxRocketJumpChargeTime = 5000;

    private final int RocketJumpCooldownTicks = 120;

    private ChargedAbility<EmptyState> WaterDamage;
    private TimedAbility FireSpeed;
    private ChargedAbility<ExplodedState> RocketJump;

    public FirebornHandler(Permission clanMemberPermission) {
        super(clanMemberPermission);
    }

    public void onEnable(){
        WaterDamage = new ChargedAbility<EmptyState>(Plugin,
        (player) -> {
            return new EmptyState();
        },
        (player, time, state) -> {
            if (time > 5000){
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 65, (int)(time / 2000), true, false));
            }
            if (time > 10000){
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 65, (int)(time / 10000), true, false));
            }

            player.damage(time / 6000d);
            return ChargeStateChange.None;
        }, 30,
        (player, time, state) -> {
            return 0;
        },
        (player, time, state) -> {
            player.setHealth(0);
            return 0;
        });

        RocketJump = new ChargedAbility<ExplodedState>(Plugin,
            (player) -> {
                player.setWalkSpeed(0);
                return new ExplodedState();
            },
            (player, time, state) -> {
                var world = player.getWorld();

                if (state.getIsExploded()){
                    return ChargeStateChange.Cancel;
                }
                if (time > MaxRocketJumpChargeTime){
                    world.createExplosion(player.getLocation(), RocketJumpOverchargePower);
                    player.setHealth(0);
                    state.setIsExploded(true);
                    return ChargeStateChange.None;
                }
                if (time > MaxJumpHeightChargeTime){
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2, 2);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 100);
                }

                world.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1,0), (int)(time / 33), 0.33, 1.8, 0.33);
                return ChargeStateChange.None;
            }, 3,
            (player, time, state) -> {
                player.setWalkSpeed(0.2f);

                var world = player.getWorld();
                world.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 4, 4);
                return 0;
            },
            (player, time, state) -> {
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

        FireSpeed = new TimedAbility(Plugin,
            player -> {
                player.setWalkSpeed(0.28f);
            },
            ((player, time) -> false), 20,
            (player, time) -> {
                player.setWalkSpeed(0.2f);
                return 0;
            });
    }

    @Override
    public void onDisable() {
        RocketJump.onDisable();
        WaterDamage.onDisable();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetLivingEntityEvent e){
        if (!(e.getTarget() instanceof Player player)){
            return;
        }
        if (!isMember(player)){
            return;
        }
        if (e.getEntityType() != EntityType.PIGLIN){
            return;
        }
        if (e.getReason() != EntityTargetEvent.TargetReason.CLOSEST_PLAYER){
            return;
        }

        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        if (!e.isSneaking() || player.getLocation().getDirection().normalize().getY() < 0.9) {
            RocketJump.cancelCharge(player);
            return;
        }

        RocketJump.startCharge(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        if (!player.isSneaking() || player.getLocation().getDirection().normalize().getY() < 0.9) {
            RocketJump.cancelCharge(player);
        }
        else {
            RocketJump.startCharge(player);
        }

        var block = player.getLocation().getBlock();
        var material = block.getType();
        if (material == Material.WATER || material == Material.SEA_PICKLE ||
            material == Material.SEAGRASS  || material == Material.TALL_SEAGRASS ||
            block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged())
        {
            WaterDamage.startCharge(player);
            return;
        }

        WaterDamage.cancelCharge(player);
    }


    @EventHandler(ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        if (e.getAction() != Action.LEFT_CLICK_AIR){
            return;
        }

        RocketJump.releaseCharge(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamaged(EntityDamageEvent e){
        if (!(e.getEntity() instanceof Player player)){
            return;
        }
        if (!isMember(player)){
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

        if (e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK){
            FireSpeed.startAbility(player, 1500);
        }

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        WaterDamage.cancelCharge(player);
        FireSpeed.cancelAbility(player);
        RocketJump.cancelCharge(player);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        WaterDamage.cancelCharge(player);
        FireSpeed.cancelAbility(player);
        RocketJump.cancelCharge(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemDamage(PlayerItemDamageEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }
        if (e.getItem().getType() != Material.FLINT_AND_STEEL){
            return;
        }

        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false)
    public void onPlayerDeath(PlayerDeathEvent e){
        var deathLocation = e.getEntity().getLocation();
        var player = e.getEntity();
        var world = player.getWorld();

        if (!isMember(player)){
            return;
        }
        if (RocketJump.isCurrentlyCharging(player)){
            RocketJump.cancelCharge(player);
            return;
        }

        WaterDamage.cancelCharge(player);
        FireSpeed.cancelAbility(player);

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
