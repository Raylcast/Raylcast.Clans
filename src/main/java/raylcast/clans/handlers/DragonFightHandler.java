package raylcast.clans.handlers;

import net.kyori.adventure.sound.SoundStop;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;
import raylcast.clans.InfoTexts;
import raylcast.clans.models.ClanType;

import java.util.ArrayList;
import java.util.Objects;

public class DragonFightHandler implements Listener {
    private final Plugin Plugin;

    private final int LevitationDuration = 670;
    private final int KillDelay = 770;

    private final int MusicToLevitationDelay = 595;

    private boolean CurrentlyPlaying = false;

    public DragonFightHandler(Plugin plugin) {
        Plugin = plugin;
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent e){
        if (!CurrentlyPlaying){
            return;
        }
        if (e.getDismounted().getType() == EntityType.ARMOR_STAND){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityKillEntity(EntityDeathEvent e) throws ReflectiveOperationException {
        if (e.getEntityType() != EntityType.ENDER_DRAGON){
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(Plugin, () -> runDragonKillAnimation(e.getEntity().getWorld()));
    }

    private void runDragonKillAnimation(World world){
        if (CurrentlyPlaying){
            return;
        }

        var battle = world.getEnderDragonBattle();

        if (battle == null){
            return;
        }

        var portalLocation = battle.getEndPortalLocation();

        if (portalLocation == null){
            return;
        }

        var players = world.getPlayers()
                .stream().filter(this::hasDisabledClan).toList();

        if (players.isEmpty()){
            return;
        }

        CurrentlyPlaying = true;

        Bukkit.getScheduler().runTask(Plugin, () -> {
            int delay = 0;
            int delayAddition = 150;

            for(var player : players){
                player.addPotionEffect(getResistance(MusicToLevitationDelay + LevitationDuration + KillDelay + delay));

                if (delay > 0){
                    player.playSound(player.getLocation(), "minecraft:raylcast.clans.wait", SoundCategory.MASTER, 1, 1);
                }

                Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> {
                    try {
                        player.stopSound("minecraft:raylcast.clans.wait");
                        runClanEnableAnimation(player, portalLocation);
                    } catch (ReflectiveOperationException ex) {
                        ex.printStackTrace();
                    }
                }, delay);

                delay += delayAddition;
                delayAddition -= 20;

                if (delayAddition <= 20){
                    delayAddition = 20;
                }
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> {
                CurrentlyPlaying = false;
            }, delay + MusicToLevitationDelay + LevitationDuration + KillDelay);

            Bukkit.getScheduler().runTaskLaterAsynchronously(Plugin, () -> {
                var updatedPlayers = world.getPlayers();
                updatedPlayers.removeAll(players);
                updatedPlayers.removeIf(player -> !hasDisabledClan(player));

                Bukkit.getScheduler().runTask(Plugin, () -> {
                    int updatedDelay = 0;

                    for (var player : updatedPlayers){
                        Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> {
                            try {
                                runClanEnableAnimation(player, portalLocation);
                            } catch (ReflectiveOperationException ex) {
                                ex.printStackTrace();
                            }
                        }, updatedDelay);

                        updatedDelay += 20;
                    }
                });
            }, delay + MusicToLevitationDelay);

        });
    }

    private void runClanEnableAnimation(LivingEntity entity, Location portalLocation) throws ReflectiveOperationException {
        if (entity instanceof Player player){
            player.playSound(entity.getLocation(), "minecraft:raylcast.clans.ascend", SoundCategory.MASTER, 1, 1);
        }

        var world = entity.getWorld();
        var laser = new fr.skytasul.guardianbeam.Laser.CrystalLaser(portalLocation, entity.getLocation(), -1, 400);

        Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> {
            entity.addPotionEffect(getLevitation());
            laser.start(Plugin);
        }, MusicToLevitationDelay);

        var levitationSpecialsTask = Bukkit.getScheduler().runTaskTimer(Plugin, () -> {
            world.spawnParticle(Particle.FIREWORKS_SPARK, entity.getLocation(), 4);
            world.spawnParticle(Particle.FLAME, entity.getLocation(), 8);

            try {
                laser.moveEnd(entity.getLocation());
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }, MusicToLevitationDelay, 1);

        Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> {
            levitationSpecialsTask.cancel();

            entity.playEffect(EntityEffect.TOTEM_RESURRECT);

            var armorStand = world.spawn(entity.getLocation(), ArmorStand.class);
            armorStand.setGravity(false);
            armorStand.setInvulnerable(true);
            armorStand.setVisible(false);

            armorStand.addPassenger(entity);

            Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> {
                if (entity instanceof Player player){
                    enableClans(player);
                }

                entity.setHealth(0);
                laser.stop();
                armorStand.remove();

                if (entity instanceof Player player){
                    player.stopSound("minecraft:raylcast.clans.ascend");
                }

                if (world != entity.getWorld()){
                    return;
                }

                world.createExplosion(entity.getLocation(), 5);
            }, KillDelay);
        }, MusicToLevitationDelay + LevitationDuration);
    }

    private void enableClans(Player player){
        var luckPerms = Objects.requireNonNull(Bukkit.getServicesManager().getRegistration(LuckPerms.class)).getProvider();
        luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> {

            var userData = user.data();

            var clanTypes = ClanType.values();

            for (var clanType : clanTypes){
                if (clanType == ClanType.None){
                    continue;
                }

                var permission = clanType.getPermission();
                String disabledPermission = permission.getName() + ".disabled";

                var node = userData.toCollection().stream().filter(n -> n.getKey().equals(disabledPermission)).findFirst();

                if (node.isEmpty()){
                    continue;
                }

                userData.remove(node.get());
                userData.add(Node.builder(permission.getName()).build());

                player.sendMessage(
                ChatColor.GREEN + "You have been chosen to be a member of the " + clanType.name() + " Clan:",
                        ChatColor.GOLD + InfoTexts.getText(clanType));
            }
        });

    }

    private boolean hasDisabledClan(Player player){
        var luckPerms = Objects.requireNonNull(Bukkit.getServicesManager().getRegistration(LuckPerms.class)).getProvider();

        var user = luckPerms.getUserManager().getUser(player.getUniqueId());

        if (user == null){
            return false;
        }

        var userData = user.data();

        var clanTypes = ClanType.values();

        for (var clanType : clanTypes){
            if (clanType == ClanType.None){
                continue;
            }

            var permission = clanType.getPermission();
            String disabledPermission = permission.getName() + ".disabled";

            var node = userData.toCollection().stream().filter(n -> n.getKey().equals(disabledPermission)).findFirst();

            if (node.isEmpty()){
                continue;
            }

            return true;
        }

        return false;
    }

    private PotionEffect getLevitation()
    {
        return new PotionEffect(PotionEffectType.LEVITATION, LevitationDuration, 1);
    }
    private PotionEffect getResistance(int duration){
        return new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, 4);
    }
}
