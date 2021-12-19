package raylcast.clans.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import raylcast.clans.models.ChargeStateChange;
import raylcast.clans.models.RunnableTimerEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ChargeAbility {
    private final Map<Player, RunnableTimerEntry> ChargeStartTimes;
    private final Set<Player> PlayersOnCooldown;

    private final Plugin Plugin;

    private final Consumer<Player> OnStartHandler;

    private final BiFunction<Player, Long, ChargeStateChange> OnChargeHandler;
    private final int TickInterval;

    private final BiFunction<Player, Long, Integer> OnCancelHandler;
    private final BiFunction<Player, Long, Integer> OnReleaseHandler;

    public ChargeAbility(Plugin plugin,
                         Consumer<Player> onStartHandler,
                         BiFunction<Player, Long, ChargeStateChange> onChargeHandler, int tickInterval,
                         BiFunction<Player, Long, Integer> onCancelHandler,
                         BiFunction<Player, Long, Integer> onReleaseHandler){
        ChargeStartTimes = new HashMap<>();
        PlayersOnCooldown = new HashSet<>();

        Plugin = plugin;

        OnStartHandler = onStartHandler;

        OnChargeHandler = onChargeHandler;
        TickInterval = tickInterval;

        OnCancelHandler = onCancelHandler;
        OnReleaseHandler = onReleaseHandler;
    }

    public void startCharge(Player player){
        if (ChargeStartTimes.containsKey(player)){
            return;
        }
        if (PlayersOnCooldown.contains(player)){
            return;
        }

        int taskId = Bukkit.getScheduler().runTaskTimer(Plugin, () -> {
            long now = System.currentTimeMillis();
            var entry = ChargeStartTimes.get(player);

            var stateChange = OnChargeHandler.apply(player, now - entry.StartTime);

            if (stateChange == ChargeStateChange.Cancel){
                cancelCharge(player);}
            else if (stateChange == ChargeStateChange.Release){
                releaseCharge(player);
            }
        }, 1, TickInterval).getTaskId();

        var entry = new RunnableTimerEntry(System.currentTimeMillis(), taskId);
        ChargeStartTimes.put(player, entry);

        OnStartHandler.accept(player);
    }

    public void cancelCharge(Player player){
        var entry = ChargeStartTimes.remove(player);

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

    public void releaseCharge(Player player){
        var entry = ChargeStartTimes.remove(player);

        if (entry == null){
            return;
        }

        long now = System.currentTimeMillis();
        Bukkit.getScheduler().cancelTask(entry.TaskId);
        long cooldown = OnReleaseHandler.apply(player, now - entry.StartTime);

        if (cooldown > 0){
            PlayersOnCooldown.add(player);
            Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> PlayersOnCooldown.remove(player), cooldown);
        }
    }

    public void onDisable(){
        long now = System.currentTimeMillis();
        ChargeStartTimes.forEach((player, entry) -> {
            OnCancelHandler.apply(player, now - entry.StartTime);
            Bukkit.getScheduler().cancelTask(entry.TaskId);
        });
    }

    public boolean isCurrentlyCharging(Player player){
        return ChargeStartTimes.containsKey(player);
    }
}
