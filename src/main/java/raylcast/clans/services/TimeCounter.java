package raylcast.clans.services;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;

public class TimeCounter {
    public final Map<Player, Long> StartTimes;

    public TimeCounter(){
        StartTimes = new HashMap<>();
    }

    public void TryStartCounting(Player player){
        StartTimes.putIfAbsent(player, System.currentTimeMillis());
    }

    public long GetTimeSinceStart(Player player){
        long now = System.currentTimeMillis();
        long startTime = StartTimes.getOrDefault(player, now);
        return now - startTime;
    }

    public long TryStopCounting(Player player){
        var startTime = StartTimes.remove(player);

        if (startTime == null){
            return 0;
        }

        return System.currentTimeMillis() - startTime;
    }
}
