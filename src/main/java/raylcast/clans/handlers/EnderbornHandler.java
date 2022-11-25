package raylcast.clans.handlers;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;
import raylcast.clans.services.PassiveAbilitiy;
import raylcast.clans.services.TimedAbility;

public class EnderbornHandler extends ClanHandler {
    private final double ExperienceMultiplier = 1.2;
    private final double PearlDisappearChance = 0.09;

    private final double SphereRadius = 3;
    private final int HoverCooldownTicks = 200;

    private TimedAbility FeatherHover;
    private PassiveAbilitiy NetherWeakness;

    public EnderbornHandler(Permission clanMemberPermission) {
        super(clanMemberPermission);
    }

    @Override
    public void onEnable() {
        FeatherHover = new TimedAbility(Plugin,
            player -> {
                var world = player.getWorld();
                world.spawnEntity(player.getLocation(), EntityType.ARMOR_STAND, CreatureSpawnEvent.SpawnReason.CUSTOM, entity -> {
                    var armorStand = (ArmorStand) entity;

                    armorStand.setInvulnerable(true);
                    armorStand.setGravity(false);
                    armorStand.setVisible(false);
                    armorStand.setPersistent(false);
                    armorStand.setCustomName("BUBBLE_" + player.getName());
                    armorStand.addPassenger(player);
                });
            },
            (player, time) -> {
                var world = player.getWorld();
                var location = player.getLocation();

                for(double phi=0; phi<=Math.PI; phi+=Math.PI/25) {
                    for(double theta=0; theta<=2*Math.PI; theta+=Math.PI/25) {
                        double x = SphereRadius*Math.cos(theta)*Math.sin(phi);
                        double y = SphereRadius*Math.cos(phi) + 1.5;
                        double z = SphereRadius*Math.sin(theta)*Math.sin(phi);

                        location.add(x,y,z);
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 1, 0F, 0F, 0F, 0.001);
                        location.subtract(x, y, z);
                    }
                }

                world.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 1, 0.5, 0.5, 0.5);
                return false;
            }, 8,
            (player, time) -> {
                var world = player.getWorld();
                for(var entity : world.getEntities()){
                    if (!entity.getName().equals("BUBBLE_" + player.getName())){
                        continue;
                    }

                    entity.setCustomName("None");
                    entity.remove();
                }

                return HoverCooldownTicks;
            });

        NetherWeakness = new PassiveAbilitiy(Plugin,
            (player) -> {
                return player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) ||
                        player.getWorld().getEnvironment() != World.Environment.NETHER;
            },
            (player) -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 160, 0));
            },
            (player, time) -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 160, 0));
                return player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE);
            }, 100,
            (player, time) -> {
                player.removePotionEffect(PotionEffectType.WEAKNESS);
            });
    }

    @Override
    public void onDisable() {
        FeatherHover.onDisable();
        NetherWeakness.onDisable();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEntityDismount(EntityDismountEvent e){
        if (e.getEntityType() != EntityType.PLAYER){
            return;
        }
        if (!e.getDismounted().getName().equals("BUBBLE_" + e.getEntity().getName())){
            return;
        }

        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        NetherWeakness.update(player);

        if (e.getAction() != Action.RIGHT_CLICK_AIR){
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.FEATHER){
            return;
        }
        if (player.getVelocity().getY() > -1){
            return;
        }

        FeatherHover.startAbility(player, 7500);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onExperienceChange(PlayerExpChangeEvent e){
        if (!isMember(e.getPlayer())){
            return;
        }

        e.setAmount((int)Math.ceil(e.getAmount() * ExperienceMultiplier));
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL){
            return;
        }

        FeatherHover.cancelAbility(player);

        var speed = new PotionEffect(PotionEffectType.SPEED, 10 * 20, 1, false, false);
        player.addPotionEffect(speed);

        var target = e.getTo();
        var world = player.getWorld();

        player.teleport(target);
        e.setCancelled(true);

        if (Random.nextDouble() >= PearlDisappearChance){
            player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> NetherWeakness.update(player), 2);

        if(e.getItem().getType() == Material.POTION ||
           e.getItem().getType() == Material.MILK_BUCKET ||
           e.getItem().getType() == Material.CHORUS_FRUIT){
            return;
        }

        var world = player.getWorld();

        if (world.getEnvironment() == World.Environment.THE_END){
            return;
        }
        else if (world.getEnvironment() == World.Environment.NETHER) {
            if (Random.nextDouble() > 0.33){
                return;
            }
        }
        else if (world.getEnvironment() == World.Environment.NORMAL){
            if (Random.nextDouble() > 0.10) {
                return;
            }
        }

        int maxNegativeOffsetY = -30;
        if (world.getEnvironment() == World.Environment.NETHER){
            maxNegativeOffsetY = -75;
        }

        int minimumY = 5;
        if (world.getEnvironment() == World.Environment.NORMAL){
            minimumY = -58;
        }

        for(int i = 0; i < 12; i++){
            int offsetX = Random.nextInt(50) - 25;
            int offsetY = Random.nextInt(50) - 15;
            int offsetZ = Random.nextInt(50) - 25;

            int airCount = 0;

            if (player.getLocation().getY() + offsetY > world.getMaxHeight() - 3){
                offsetY = world.getMaxHeight() - player.getLocation().getBlockY() - 3;
            }

            for(; player.getLocation().getBlockY() + offsetY > minimumY && offsetY > maxNegativeOffsetY; offsetY--) {
                var target = player.getLocation().add(offsetX, offsetY, offsetZ);

                if (target.getBlock().getType() == Material.AIR){
                    airCount++;
                    continue;
                }
                if (airCount < 2){
                    airCount = 0;
                    continue;
                }
                if (player.teleport(target.add(0, 1, 0))){
                    world.spawnParticle(Particle.EXPLOSION_LARGE, target, 2);
                    world.playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1, 1);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        FeatherHover.cancelAbility(player);
        NetherWeakness.cancelAbility(player);

        e.setDroppedExp(0);
        e.setNewExp(0);
        e.setNewLevel(0);

        var deathLoc = e.getPlayer().getLocation();
        var location = new Location(player.getWorld(), deathLoc.getBlockX(), deathLoc.getBlockY() + 100, deathLoc.getBlockZ(), 90, 0);
        var fireball = e.getPlayer().getWorld().spawn(location, DragonFireball.class);
        fireball.setDirection(new Vector().setY(-3));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        FeatherHover.cancelAbility(player);
        NetherWeakness.cancelAbility(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        NetherWeakness.update(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        FeatherHover.cancelAbility(player);
        NetherWeakness.cancelAbility(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        NetherWeakness.update(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        NetherWeakness.update(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent e){
        if (e.getEntity().getWorld().getEnvironment() != World.Environment.NETHER){
            return;
        }
        if (e.getPotion().getEffects().stream().noneMatch(effect -> effect.getType() == PotionEffectType.FIRE_RESISTANCE)){
            return;
        }

        for(var affected : e.getAffectedEntities()){
            if (!(affected instanceof Player player)){
                continue;
            }

            if (!isMember(player)){
                continue;
            }

            Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> NetherWeakness.update(player), 2);
        }
    }
}
