package raylcast.clans.services;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TimeCounterRunnable extends TimeCounter {
    private final Map<Player, Integer> RunnableIds;

    private final Plugin Plugin;
    private final BiConsumer<Player, Long> Runnable;
    private final int Delay;
    private final int Frequency;

    public TimeCounterRunnable(Plugin plugin, BiConsumer<Player, Long> runnable, int delay, int frequency){
        super();
        RunnableIds = new HashMap<>();

        Plugin = plugin;
        Runnable = runnable;
        Delay = delay;
        Frequency = frequency;
    }

    @Override
    public void TryStartCounting(Player player) {
        super.TryStartCounting(player);

        if (RunnableIds.containsKey(player)){
            return;
        }

        var task = Bukkit.getScheduler().runTaskTimer(Plugin, () -> {
            long timeSinceStart = GetTimeSinceStart(player);
            Runnable.accept(player, timeSinceStart);
        }, Delay, Frequency);
        RunnableIds.put(player, task.getTaskId());
    }

    @Override
    public long TryStopCounting(Player player) {
        var taskId = RunnableIds.get(player);

        if (taskId != null){
            Bukkit.getScheduler().cancelTask(taskId);
            RunnableIds.remove(player);
        }

        return super.TryStopCounting(player);
    }
}
