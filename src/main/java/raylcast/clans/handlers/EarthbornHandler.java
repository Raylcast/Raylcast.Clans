package raylcast.clans.handlers;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.permissions.Permission;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import raylcast.clans.helper.PlayerHelper;
import raylcast.clans.models.ChargeStateChange;
import raylcast.clans.models.LaserState;
import raylcast.clans.services.ChargedAbility;
import raylcast.clans.services.TimedAbility;

public class EarthbornHandler extends ClanHandler {
    private final int ExpectedTPS = 20;
    private final int TreeDelayTicks = 150;
    private final int PunishmentPercentage = 75;
    private final double PunishmentThreshold = PunishmentPercentage / 100d;

    private final int WeatherChangeChargeTime = 10000;

    private final int BoneMealCost = 4;

    private TimedAbility RainSpeed;
    private ChargedAbility<LaserState> WeatherChange;

    public EarthbornHandler(Permission clanMemberPermission){
        super(clanMemberPermission);
    }

    @Override
    public void onEnable() {
        WeatherChange = new ChargedAbility<LaserState>(Plugin,
                (player) -> {
                    player.setWalkSpeed(0f);
                    return new LaserState(Plugin, WeatherChangeChargeTime / 45, player.getLocation(), player.getLocation().add(0, 1, 0));
                },
                (player, time, state) -> {
                    var world = player.getWorld();
                    for(int i = 0; i < time / 100; i++){
                        world.spawnParticle(Particle.WATER_SPLASH, player.getLocation(), 1, 0, i / 3f, 0);
                        world.spawnParticle(Particle.WATER_BUBBLE, player.getLocation(), 1, 0, i / 3f, 0);
                    }

                    if (time > WeatherChangeChargeTime){
                        return ChargeStateChange.Release;
                    }

                    state.setEnd(player.getLocation().add(0, time / 100f, 0));

                    return ChargeStateChange.None;
                },
                5,
                (player, time, state) -> {
                    player.setWalkSpeed(0.2f);
                    state.stop();
                    return 0;
                },
                (player, time, state) -> {
                    var world = player.getWorld();

                    world.setStorm(world.isClearWeather());

                    player.setWalkSpeed(0.2f);
                    state.stop();
                    return 120 * 20;
                });

        RainSpeed = new TimedAbility(Plugin,
                (player) -> {
                    player.setWalkSpeed(0.28f);
                },
                (player, time) -> {
                    return false;
                }, 60,
                (player, time) -> {
                    player.setWalkSpeed(0.2f);
                    return 0;
                });

        Bukkit.getScheduler().runTaskTimer(Plugin, () -> {
            var overworldOpt = Bukkit.getWorlds().stream().filter(w -> w.getEnvironment() == World.Environment.NORMAL).findFirst();

            if (overworldOpt.isEmpty()){
                return;
            }

            var overworld = overworldOpt.get();

            if (overworld.isClearWeather()){
                return;
            }

            for(var player : overworld.getPlayers()){
                if (!isMember(player)){
                    return;
                }
                if (!player.isInRain()){
                    return;
                }

                RainSpeed.startAbility(player, 3000);
            }
        }, 60, 60);
    }

    @Override
    public void onDisable() {
        WeatherChange.onDisable();
        RainSpeed.onDisable();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent e){
        var player = e.getPlayer();

        if (!isMember(player)){
            return;
        }

        var location = player.getLocation();
        var world = player.getWorld();

        if (!e.isSneaking() || player.getLocation().getDirection().normalize().getY() < 0.9) {
            WeatherChange.cancelCharge(player);
            return;
        }
        if (world.getHighestBlockAt(location.getBlockX(), location.getBlockZ()).getLocation().getBlockY() > location.getBlockY()){
            return;
        }

        WeatherChange.startCharge(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getTarget() instanceof Player player)) {
            return;
        }
        if (!isMember(player)){
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
        if (!isMember(e.getPlayer())){
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
                e.setUseItemInHand(Event.Result.DENY);
                clickedBlock.applyBoneMeal(BlockFace.DOWN);
            }
            return;
        }

        ageable.setAge(0);
        e.setUseItemInHand(Event.Result.DENY);
        e.setCancelled(true);

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

        if (entityType == EntityType.ZOMBIE || entityType == EntityType.CREEPER ||
                entityType == EntityType.SKELETON || entityType == EntityType.ENDER_DRAGON ||
                entityType == EntityType.SILVERFISH || entityType == EntityType.HUSK ||
                entityType == EntityType.EVOKER || entityType == EntityType.HOGLIN) {
            return;
        }
        if (killer == null){
            return;
        }
        if (!isMember(killer)){
            return;
        }
        for (int i = Random.nextInt(5); i < 5; i++){
            killer.addPotionEffect(getPunishmentEffect());;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e){
        var player = e.getPlayer();

        if(!isMember(player)){
            return;
        }

        RainSpeed.cancelAbility(e.getPlayer());
        WeatherChange.cancelCharge(e.getPlayer());
    }
    @EventHandler(ignoreCancelled = true)
    public void onPlayerKicked(PlayerKickEvent e){
        var player = e.getPlayer();

        if(!isMember(player)){
            return;
        }

        RainSpeed.cancelAbility(e.getPlayer());
        WeatherChange.cancelCharge(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e){
        var deathLocation = e.getEntity().getLocation();
        var player = e.getEntity();

        if (!isMember(player)){
            return;
        }

        WeatherChange.cancelCharge(player);
        RainSpeed.cancelAbility(player);

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
                placeDeathTemple(deathLocation);

            }
        }, TreeDelayTicks);
    }

    private final Material[][][] deathTemple =
    {
        {
            { Material.INFESTED_CRACKED_STONE_BRICKS, Material.MYCELIUM, Material.INFESTED_MOSSY_STONE_BRICKS },
            { Material.MYCELIUM, Material.MYCELIUM, Material.MYCELIUM },
            { Material.INFESTED_STONE_BRICKS, Material.MYCELIUM, Material.INFESTED_MOSSY_STONE_BRICKS },
        },
        {
            { Material.INFESTED_CRACKED_STONE_BRICKS, Material.AIR, Material.INFESTED_MOSSY_STONE_BRICKS },
            { Material.AIR, Material.RED_MUSHROOM, Material.AIR },
            { Material.INFESTED_STONE_BRICKS, Material.AIR, Material.INFESTED_MOSSY_STONE_BRICKS },
        },
        {
            { Material.INFESTED_CHISELED_STONE_BRICKS, Material.AIR, Material.INFESTED_CHISELED_STONE_BRICKS },
            { Material.AIR, Material.AIR, Material.AIR },
            { Material.INFESTED_CHISELED_STONE_BRICKS, Material.AIR, Material.INFESTED_CHISELED_STONE_BRICKS },
        },
    };


    private void placeDeathTemple(Location middleLocation){
        for(int x = -1; x < 2; x++){
            for(int y = 0; y < 3; y++){
                for(int z = -1; z < 2; z++){
                    var location = middleLocation.clone();
                    location.add(x, y, z);

                    var block = location.getBlock();
                    if (shouldIgnoreBlock(block)){
                        continue;
                    }

                    block.setType(deathTemple[y][x + 1][z + 1]);
                }
            }
        }
    }

    private boolean shouldIgnoreBlock(Block block){
        if (block.getState() instanceof TileState){
            return true;
        }
        if (block.getType() == Material.DIAMOND_BLOCK || block.getType() == Material.GOLD_BLOCK || block.getType() == Material.LAPIS_BLOCK ||
                block.getType() == Material.REDSTONE_BLOCK || block.getType() == Material.NETHERITE_BLOCK || block.getType() == Material.ANCIENT_DEBRIS){
            return true;
        }
        if (block.getType() == Material.END_PORTAL || block.getType() == Material.NETHER_PORTAL){
            return true;
        }
        if (block.getType() == Material.BEDROCK || block.getType() == Material.END_PORTAL_FRAME){
            return true;
        }

        return false;
    }

    private PotionEffect getPunishmentEffect(){
        return switch (Random.nextInt(7)) {
            case 0 -> new PotionEffect(PotionEffectType.BLINDNESS, getRandomTickCount(3, 12), 1);
            case 1 -> new PotionEffect(PotionEffectType.HUNGER, getRandomTickCount(5, 60), Random.nextInt(2, 6));
            case 2 -> new PotionEffect(PotionEffectType.SLOW, getRandomTickCount(5, 20), Random.nextInt(2, 6));
            case 3 -> new PotionEffect(PotionEffectType.SLOW_DIGGING, getRandomTickCount(8, 60), Random.nextInt(2, 5));
            case 4 -> new PotionEffect(PotionEffectType.CONFUSION, getRandomTickCount(6, 20), 1);
            case 5 -> new PotionEffect(PotionEffectType.POISON, getRandomTickCount(2, 8), Random.nextInt(1, 4));
            case 6 -> new PotionEffect(PotionEffectType.WEAKNESS, getRandomTickCount(8, 24), Random.nextInt(1, 5));
            default -> throw new UnsupportedOperationException("This should never be executed!");
        };
    }

    private TreeType getRandomTreeType(){
        return switch (Random.nextInt(5)) {
            case 0 -> TreeType.TREE;
            case 1 -> TreeType.BIG_TREE;
            case 2 -> TreeType.BIRCH;
            case 3 -> TreeType.TALL_BIRCH;
            case 4 -> TreeType.BROWN_MUSHROOM;
            default -> throw new UnsupportedOperationException("This should never be executed!");
        };
    }

    private int getRandomTickCount(int minSeconds, int maxSeconds){
        return ExpectedTPS * Random.nextInt(minSeconds, maxSeconds);
    }


}
