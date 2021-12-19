package raylcast.clans.handlers;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityDismountEvent;
import raylcast.clans.models.ChargeStateChange;
import raylcast.clans.models.ClanType;
import raylcast.clans.services.ChargeAbility;
import raylcast.clans.services.TimedAbility;

public class EnderbornHandler extends ClanHandler {
    private final double ExperienceMultiplier = 100;
    private final double FallDamageMultiplier = 0.2;
    private final double PearlDisappearChance = 0.1;

    private final double SphereRadius = 3;
    private final int HoverCooldownTicks = 200;

    private TimedAbility HoverAbility;

    public EnderbornHandler(){
    }

    @Override
    public void onEnable() {
        HoverAbility = new TimedAbility(Plugin,
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

                if (time > 10000){
                    return true;
                }

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
    }

    @Override
    public void onDisable() {
        HoverAbility.onDisable();
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
        if (e.getAction() != Action.RIGHT_CLICK_AIR){
            return;
        }

        var player = e.getPlayer();

        if (!isMember(player, ClanType.Enderborn)){
            return;
        }
        if (player.getInventory().getItemInMainHand().getType() != Material.FEATHER){
            return;
        }
        if (player.getVelocity().getY() > -1){
            return;
        }

        HoverAbility.startCharge(player);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onExperienceChange(PlayerExpChangeEvent e){
        if (!isMember(e.getPlayer(), ClanType.Enderborn)){
            return;
        }

        e.setAmount((int)Math.ceil(e.getAmount() * ExperienceMultiplier));
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e){
        if (!isMember(e.getPlayer(), ClanType.Enderborn)){
            return;
        }
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL){
            return;
        }

        var target = e.getTo();

        e.getPlayer().teleport(target);
        e.setCancelled(true);

        if (Random.nextDouble() >= PearlDisappearChance){
            e.getPlayer().getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleSneakEvent(PlayerToggleSneakEvent e){
        if (!e.isSneaking()){
            return;
        }

        var player = e.getPlayer();

        if (!isMember(player, ClanType.Enderborn)){
            return;
        }
        if (player.getLocation().getDirection().normalize().getY() < 0.9){
            return;
        }

        player.setVelocity(player.getVelocity().setY(1));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerDamage(EntityDamageEvent e){
        if (!(e.getEntity() instanceof Player player)){
            return;
        }
        if (!isMember(player, ClanType.Enderborn)){
            return;
        }
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL){
            return;
        }

        e.setDamage(e.getDamage() * FallDamageMultiplier);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e){
        var player = e.getPlayer();

        if (!isMember(player, ClanType.Enderborn)){
            return;
        }

        HoverAbility.cancelAbility(player);

        e.setDroppedExp(0);
        e.setNewExp(0);
        e.setNewLevel(0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e){
        var player = e.getPlayer();

        if (!isMember(player, ClanType.Enderborn)){
            return;
        }

        HoverAbility.cancelAbility(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent e){
        var player = e.getPlayer();

        if (!isMember(player, ClanType.Enderborn)){
            return;
        }

        HoverAbility.cancelAbility(player);
    }
}
