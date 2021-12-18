package raylcast.clans.runnables;

import org.bukkit.scheduler.BukkitRunnable;

public class LimitedRunnable extends BukkitRunnable {
    private final Runnable Runnable;
    private final int MaxRuns;

    private int Runs;

    public LimitedRunnable(Runnable runnable, int maxRuns){
        Runnable = runnable;
        MaxRuns = maxRuns;

        if (MaxRuns < 1){
            throw new IllegalArgumentException("maxRuns must be greater than 0");
        }

        Runs = 0;
    }

    @Override
    public void run() {
        Runnable.run();

        if (++Runs >= MaxRuns){
            cancel();
        }
    }
}
