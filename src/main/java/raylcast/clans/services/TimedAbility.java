package raylcast.clans.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import raylcast.clans.models.TimedAbilityEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class TimedAbility {
    private final Map<Player, TimedAbilityEntry> AbilityStartTimes;
    private final Set<Player> PlayersOnCooldown;

    private final Plugin Plugin;

    private final Consumer<Player> OnStartHandler;

    private final BiFunction<Player, Long, Boolean> OnTickHandler;
    private final int TickInterval;

    private final BiFunction<Player, Long, Integer> OnCancelHandler;

    public TimedAbility(Plugin plugin,
                         Consumer<Player> onStartHandler,
                         BiFunction<Player, Long, Boolean> onTickHandler, int tickInterval,
                         BiFunction<Player, Long, Integer> onCancelHandler){
        AbilityStartTimes = new HashMap<>();
        PlayersOnCooldown = new HashSet<>();

        Plugin = plugin;

        OnStartHandler = onStartHandler;

        OnTickHandler = onTickHandler;
        TickInterval = tickInterval;

        OnCancelHandler = onCancelHandler;
    }

    public void onDisable(){
        long now = System.currentTimeMillis();
        AbilityStartTimes.forEach((player, entry) -> {
            OnCancelHandler.apply(player, now - entry.StartTime);
            Bukkit.getScheduler().cancelTask(entry.TaskId);
        });
    }

    public void startAbility(Player player, int duration){
        var current = AbilityStartTimes.get(player);

        if (current != null){
            long now = System.currentTimeMillis();

            if (current.StartTime + current.Duration >= now + duration){
                return;
            }

            current.StartTime = now;
            current.Duration = duration;
            AbilityStartTimes.put(player, current);
            return;
        }
        if (PlayersOnCooldown.contains(player)){
            return;
        }

        int taskId = Bukkit.getScheduler().runTaskTimer(Plugin, () -> {
            long now = System.currentTimeMillis();
            var entry = AbilityStartTimes.get(player);

            if (now - entry.StartTime > entry.Duration){
                cancelAbility(player);
                return;
            }

            boolean cancelAbility = OnTickHandler.apply(player, now - entry.StartTime);

            if (cancelAbility){
                cancelAbility(player);
            }
        }, 1, TickInterval).getTaskId();

        var entry = new TimedAbilityEntry(System.currentTimeMillis(), taskId, duration);
        AbilityStartTimes.put(player, entry);

        OnStartHandler.accept(player);
    }

    public void cancelAbility(Player player){
        var entry = AbilityStartTimes.remove(player);

        if (entry == null){
            return;
        }

        long now = System.currentTimeMillis();
        Bukkit.getScheduler().cancelTask(entry.TaskId);
        long cooldown = OnCancelHandler.apply(player, now - entry.StartTime);

        if (cooldown > 0){
            PlayersOnCooldown.add(player);
            Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> PlayersOnCooldown.remove(player), cooldown);
        }
    }

    public boolean isCurrentlyActive(Player player){
        return AbilityStartTimes.containsKey(player);
    }
}
