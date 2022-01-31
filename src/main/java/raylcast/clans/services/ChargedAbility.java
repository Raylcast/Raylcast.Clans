package raylcast.clans.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import raylcast.clans.models.ChargeStateChange;
import raylcast.clans.models.ChargedAbilityEntry;
import raylcast.clans.models.TriFunction;

import javax.swing.plaf.nimbus.State;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class ChargedAbility<T> {
    private final Map<Player, ChargedAbilityEntry> ChargeStartTimes;
    private final Map<Player, T> StateMap;

    private final Set<Player> PlayersOnCooldown;

    private final Plugin Plugin;

    private final Function<Player, T> OnStartHandler;

    private final TriFunction<Player, Long, T, ChargeStateChange> OnChargeHandler;
    private final int TickInterval;

    private final TriFunction<Player, Long, T, Integer> OnCancelHandler;
    private final TriFunction<Player, Long, T, Integer> OnReleaseHandler;

    public ChargedAbility(Plugin plugin,
                          Function<Player, T> onStartHandler,
                          TriFunction<Player, Long, T, ChargeStateChange> onChargeHandler, int tickInterval,
                          TriFunction<Player, Long, T, Integer> onCancelHandler,
                          TriFunction<Player, Long, T, Integer> onReleaseHandler){
        ChargeStartTimes = new HashMap<>();
        StateMap = new HashMap<>();

        PlayersOnCooldown = new HashSet<>();

        Plugin = plugin;

        OnStartHandler = onStartHandler;

        OnChargeHandler = onChargeHandler;
        TickInterval = tickInterval;

        OnCancelHandler = onCancelHandler;
        OnReleaseHandler = onReleaseHandler;
    }

    public T getState(Player player){
        return StateMap.get(player);
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
            var state = StateMap.get(player);

            var stateChange = OnChargeHandler.apply(player, now - entry.StartTime, state);

            if (stateChange == ChargeStateChange.Cancel){
                cancelCharge(player);}
            else if (stateChange == ChargeStateChange.Release){
                releaseCharge(player);
            }
        }, 1, TickInterval).getTaskId();

        var entry = new ChargedAbilityEntry(System.currentTimeMillis(), taskId);
        ChargeStartTimes.put(player, entry);

        var state = OnStartHandler.apply(player);
        StateMap.put(player, state);
    }

    public void cancelCharge(Player player){
        var entry = ChargeStartTimes.remove(player);
        var state = StateMap.remove(player);

        if (entry == null){
            return;
        }

        long now = System.currentTimeMillis();
        Bukkit.getScheduler().cancelTask(entry.TaskId);
        long cooldown = OnCancelHandler.apply(player, now - entry.StartTime, state);

        if (cooldown > 0){
            PlayersOnCooldown.add(player);
            Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> PlayersOnCooldown.remove(player), cooldown);
        }
    }

    public void releaseCharge(Player player){
        var entry = ChargeStartTimes.remove(player);
        var state = StateMap.remove(player);

        if (entry == null){
            return;
        }

        long now = System.currentTimeMillis();
        Bukkit.getScheduler().cancelTask(entry.TaskId);
        long cooldown = OnReleaseHandler.apply(player, now - entry.StartTime, state);

        if (cooldown > 0){
            PlayersOnCooldown.add(player);
            Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin, () -> PlayersOnCooldown.remove(player), cooldown);
        }
    }

    public void onDisable(){
        long now = System.currentTimeMillis();
        ChargeStartTimes.forEach((player, entry) -> {
            var state = StateMap.get(player);
            OnCancelHandler.apply(player, now - entry.StartTime, state);
            Bukkit.getScheduler().cancelTask(entry.TaskId);
        });
    }

    public boolean isCurrentlyCharging(Player player){
        return ChargeStartTimes.containsKey(player);
    }
}
