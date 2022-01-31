package raylcast.clans.handlers;

import org.bukkit.*;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
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
    private final int ExplosionPower = 5;
    private final int RocketJumpOverchargePower = 4;
    private final int ExpectedTPS = 20;

    private final float MaxRocketJumpPower = 3f;
    private final float MaxJumpHeightChargeTime = 4500;
    private final float MaxRocketJumpChargeTime = 5000;

    private final int RocketJumpCooldownTicks = 120;

    private final int WaterDamageTickInterval = 30;

    private ChargedAbility<EmptyState> WaterDamage;
    private TimedAbility FireSpeed;
    private ChargedAbility<ExplodedState> RocketJump;

    public FirebornHandler(Permission clanMemberPermission) {
        super(clanMemberPermission);
    }

    public void onEnable(){
        WaterDamage = new ChargedAbility<EmptyState>(Plugin,
        (player) -> new EmptyState(),
        (player, time, state) -> {
            if(player.getVehicle() instanceof Boat){
                return ChargeStateChange.Cancel;
            }

            if (time > 5000){
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, WaterDamageTickInterval + 5, (int)(time / 2000), true, false));
            }

            if (player.hasPotionEffect(PotionEffectType.CONDUIT_POWER)){
                return ChargeStateChange.None;
            }

            if (time > 10000){
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, WaterDamageTickInterval + 5, (int)(time / 10000), true, false));
            }

            player.damage(time / 6000d);
            return ChargeStateChange.None;
        },
        WaterDamageTickInterval,
        (player, time, state) -> 0,
        (player, time, state) -> 0);

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
                    state.setIsExploded(true);
                    world.createExplosion(player.getLocation(), RocketJumpOverchargePower, false, true);
                    player.setHealth(0);
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
        e.setTarget(null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        if (!e.isSneaking() || player.getLocation().getPitch() > -60) {
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

        if (!player.isSneaking() || player.getLocation().getPitch() > -60) {
            RocketJump.cancelCharge(player);
        }
        else {
            RocketJump.startCharge(player);
        }

        if (player.getVehicle() instanceof Boat){
            WaterDamage.cancelCharge(player);
            return;
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

        if (e.getAction() == Action.LEFT_CLICK_AIR){
            RocketJump.releaseCharge(player);
            return;
        }

        if (e.getAction() != Action.RIGHT_CLICK_AIR){
            return;
        }

        var mainHandType = player.getInventory().getItemInMainHand().getType();
        var offHandType = player.getInventory().getItemInOffHand().getType();

        if (mainHandType == Material.FLINT_AND_STEEL || offHandType == Material.FLINT_AND_STEEL){
            player.setFireTicks(20 * 10);
            player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamaged(EntityDamageEvent e){
        if (!(e.getEntity() instanceof Player player)){
            return;
        }
        if (!isMember(player)){
            return;
        }

        if (e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK){
            FireSpeed.startAbility(player, 1500);
            e.setCancelled(true);
            return;
        }

        if (e.getCause() == EntityDamageEvent.DamageCause.LAVA){
            e.setDamage(e.getDamage() * LavaResistance);
            return;
        }

        if (e.getCause() == EntityDamageEvent.DamageCause.FIRE ||
            e.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR ||
            e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK){
            e.setCancelled(true);
        }
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

        RocketJump.cancelCharge(player);
        WaterDamage.cancelCharge(player);
        FireSpeed.cancelAbility(player);

        if (RocketJump.isCurrentlyCharging(player)){
            var state = RocketJump.getState(player);
            if (state.getIsExploded()){
                e.setDeathMessage(player.displayName() + " blew up trying to reach the stars");
                return;
            }
            state.setIsExploded(true);
        }

        Logger.info("Fireworker died, scheduled explosion");

        var delayedDrops =  e.getDrops().stream().toList();
        var dropLocation = e.getPlayer().getLocation();

        e.getDrops().clear();

        var task = Bukkit.getScheduler().runTaskTimerAsynchronously(Plugin, () -> {
            world.spawnParticle(Particle.FIREWORKS_SPARK, deathLocation, 100);
            world.spawnParticle(Particle.FLAME, deathLocation, 100);
            world.playSound(deathLocation, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 1);
        } , 0, 5);

        Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () ->
        {
            Logger.info("Executing explosion");
            task.cancel();
            world.createExplosion(deathLocation, ExplosionPower, false, true);

        }, ExplosionDelayTicks);

       Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> {
            for(var drop : delayedDrops){
                world.dropItem(dropLocation, drop);
            }
        }, ExplosionDelayTicks + 5);
    }
}
