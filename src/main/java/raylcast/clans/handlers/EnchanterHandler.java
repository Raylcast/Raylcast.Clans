package raylcast.clans.handlers;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import raylcast.clans.models.ClanType;

public class EnchanterHandler extends ClanHandler {
    private final double ExperienceMultiplier = 100;
    private final double FallDamageMultiplier = 0.2;
    private final double PearlDisappearChance = 0.1;

    public EnchanterHandler(){
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onExperienceChange(PlayerExpChangeEvent e){
        if (!isMember(e.getPlayer(), ClanType.Magician)){
            return;
        }

        e.setAmount((int)Math.ceil(e.getAmount() * ExperienceMultiplier));
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e){
        if (!isMember(e.getPlayer(), ClanType.Magician)){
            return;
        }
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL){
            return;
        }

        var target = e.getTo();

        if (target != null){
            e.getPlayer().teleport(target);
            e.setCancelled(true);

            if (Random.nextDouble() >= PearlDisappearChance){
                e.getPlayer().getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleSneakEvent(PlayerToggleSneakEvent e){
        if (!e.isSneaking()){
            return;
        }

        var player = e.getPlayer();

        if (!isMember(player, ClanType.Magician)){
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
        if (!isMember(player, ClanType.Magician)){
            return;
        }
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL){
            return;
        }

        e.setDamage(e.getDamage() * FallDamageMultiplier);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e){
        if (!isMember(e.getEntity(), ClanType.Magician)){
            return;
        }

        e.setDroppedExp(0);
        e.setNewExp(0);
        e.setNewLevel(0);
    }
}
