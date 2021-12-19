package raylcast.clans.models;

public class TimedAbilityEntry extends AbilityEntry {
    public int TaskId;
    public int Duration;

    public TimedAbilityEntry(long startTime, int taskId, int duration) {
        super(startTime);
        TaskId = taskId;
        Duration = duration;
    }
}
