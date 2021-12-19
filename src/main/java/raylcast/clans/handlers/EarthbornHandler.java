package raylcast.clans.handlers;

import org.bukkit.*;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import raylcast.clans.helper.PlayerHelper;
import raylcast.clans.models.ClanType;

public class EarthbornHandler extends ClanHandler {
    private final int ExpectedTPS = 20;
    private final int TreeDelayTicks = 150;
    private final int PunishmentPercentage = 75;
    private final double PunishmentThreshold = PunishmentPercentage / 100d;

    private final int BoneMealCost = 4;

    public EarthbornHandler(){
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    public void onItemUsed(PlayerItemBreakEvent e){
        Bukkit.broadcastMessage("TEST");
    }

    public void onItemConsume(PlayerItemConsumeEvent e){
        Bukkit.broadcastMessage("TES 2T");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getTarget() instanceof Player player)) {
            return;
        }
        if (!isMember(player, ClanType.Earthborn)){
            return;
        }
        if (e.getEntityType() != EntityType.BEE &&
            e.getEntityType() != EntityType.SPIDER &&
            e.getEntityType() != EntityType.CAVE_SPIDER &&
            e.getEntityType() != EntityType.LLAMA){
            return;
        }

        e.setCancelled(true);
        e.setTarget(null);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent e){
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK){
            return;
        }
        var mainItem = e.getPlayer().getInventory().getItemInMainHand();
        var mainItemType = mainItem.getType();

        if(mainItemType != Material.AIR && mainItemType != Material.BONE_MEAL){
            return;
        }
        if (!isMember(e.getPlayer(), ClanType.Earthborn)){
            return;
        }

        var clickedBlock = e.getClickedBlock();

        if (clickedBlock == null){
            Logger.warning("ClickedBlock unexpectedly was null!");
            return;
        }

        if (!(clickedBlock.getBlockData() instanceof Ageable ageable)){
            return;
        }

        if (clickedBlock.getType() == Material.MELON_STEM){
            return;
        }
        if (clickedBlock.getType() == Material.PUMPKIN_STEM){
            return;
        }
        if (ageable.getMaximumAge() != 7 && ageable.getMaximumAge() != 3){
            return;
        }
        if (ageable.getAge() != ageable.getMaximumAge()){
            if (mainItemType == Material.BONE_MEAL && mainItem.getAmount() == 1 &&
                    PlayerHelper.takeXP(e.getPlayer(), BoneMealCost)){
                mainItem.setAmount(2);
            }
            return;
        }

        ageable.setAge(0);

        var location = clickedBlock.getLocation();
        var drops = clickedBlock.getDrops();

        clickedBlock.setBlockData(ageable);

        for (var drop : drops){
            if (drop.getType() == Material.CARROT ||
                drop.getType() == Material.BEETROOT_SEEDS ||
                drop.getType() == Material.POTATO ||
                drop.getType() == Material.WHEAT_SEEDS){

                drop.setAmount(drop.getAmount() - 1);
                break;
            }
        }

        for(var drop : drops){
            if (drop.getType() == Material.AIR){
                continue;
            }
            if (drop.getAmount() == 0){
                continue;
            }

            clickedBlock.getWorld().dropItem(location, drop);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e){
        var killer = e.getEntity().getKiller();
        var entityType = e.getEntityType();

        if (entityType == EntityType.ZOMBIE || entityType == EntityType.CREEPER || entityType == EntityType.SKELETON){
            return;
        }
        if (killer == null){
            return;
        }
        if (!isMember(killer, ClanType.Earthborn)){
            return;
        }

        while (Random.nextDouble() <= PunishmentThreshold){

            killer.addPotionEffect(getPunishmentEffect());;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e){
        var deathLocation = e.getEntity().getLocation();
        var player = e.getEntity();

        if (!isMember(player, ClanType.Earthborn)){
            return;
        }

        var world = player.getWorld();

        Logger.info("Farmer died, scheduled tree");

        var task = Bukkit.getScheduler().runTaskTimerAsynchronously(Plugin, () -> {
            world.spawnParticle(Particle.FIREWORKS_SPARK, deathLocation, 100);
            world.spawnParticle(Particle.GLOW_SQUID_INK, deathLocation, 100);
            world.playSound(deathLocation, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, 1, 1);
        } , 0, 5);

        Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () ->
        {
            Logger.info("Executing tree");
            task.cancel();
            if(!world.generateTree(deathLocation, Random, getRandomTreeType())){
                deathLocation.getBlock().setType(Material.MYCELIUM);
                deathLocation.add(0, 1 ,0).getBlock().setType(Material.POTTED_RED_MUSHROOM);
            }
        }, TreeDelayTicks);
    }

    private PotionEffect getPunishmentEffect(){
        switch ( Random.nextInt(7)){
            case 0:
                return new PotionEffect(PotionEffectType.BLINDNESS, getRandomTickCount(3,12), 1);
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
            case 6:
                return new PotionEffect(PotionEffectType.WEAKNESS, getRandomTickCount(8, 24), Random.nextInt(1, 5));
            default:
                throw new UnsupportedOperationException("This should never be executed!");
        }
    }

    private TreeType getRandomTreeType(){
        var treeTypes = TreeType.values();
        return treeTypes[Random.nextInt(treeTypes.length)];
    }

    private int getRandomTickCount(int minSeconds, int maxSeconds){
        return ExpectedTPS * Random.nextInt(minSeconds, maxSeconds);
    }


}
