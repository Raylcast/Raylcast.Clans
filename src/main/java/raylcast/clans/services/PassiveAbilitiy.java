package raylcast.clans.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import raylcast.clans.models.AbilityEntry;
import raylcast.clans.models.TimedAbilityEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class PassiveAbilitiy {
    private final Map<Player, AbilityEntry> AbilityStartTimes;

    private final Plugin Plugin;

    private final Function<Player, Boolean> OnUpdateHandler;
    private final Consumer<Player> OnStartHandler;

    private final BiFunction<Player, Long, Boolean> OnTickHandler;
    private final int TickInterval;

    private final BiConsumer<Player, Long> OnStopHandler;

    public PassiveAbilitiy(Plugin plugin,
                        Function<Player, Boolean> onUpdateHandler, Consumer<Player> onStartHandler,
                        BiFunction<Player, Long, Boolean> onTickHandler, int tickInterval,
                        BiConsumer<Player, Long> onStopHandler){
        AbilityStartTimes = new HashMap<>();

        Plugin = plugin;

        OnUpdateHandler = onUpdateHandler;
        OnStartHandler = onStartHandler;

        OnTickHandler = onTickHandler;
        TickInterval = tickInterval;

        OnStopHandler = onStopHandler;
    }


    public void onDisable(){
        long now = System.currentTimeMillis();
        AbilityStartTimes.forEach((player, entry) -> {
            OnStopHandler.accept(player, now - entry.StartTime);
            Bukkit.getScheduler().cancelTask(entry.TaskId);
        });
    }

    public void update(Player player){
        boolean shouldCancel = OnUpdateHandler.apply(player);
        if (shouldCancel){
            cancelAbility(player);
            return;
        }

        if (isCurrentlyActive(player)){
            return;
        }

        int taskId = Bukkit.getScheduler().runTaskTimer(Plugin, () -> {
            long now = System.currentTimeMillis();
            var entry = AbilityStartTimes.get(player);

            boolean cancelAbility = OnTickHandler.apply(player, now - entry.StartTime);

            if (cancelAbility){
                cancelAbility(player);
            }
        }, 1, TickInterval).getTaskId();

        var entry = new AbilityEntry(System.currentTimeMillis(), taskId);
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
        OnStopHandler.accept(player, now - entry.StartTime);
    }

    public boolean isCurrentlyActive(Player player){
        return AbilityStartTimes.containsKey(player);
    }
}
