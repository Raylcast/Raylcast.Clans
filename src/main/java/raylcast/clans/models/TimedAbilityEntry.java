package raylcast.clans.models;

public class TimedAbilityEntry extends AbilityEntry {
    public int Duration;

    public TimedAbilityEntry(long startTime, int taskId, int duration) {
        super(startTime, taskId);
        Duration = duration;
    }
}
