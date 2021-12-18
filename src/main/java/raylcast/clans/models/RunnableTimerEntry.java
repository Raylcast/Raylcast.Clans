package raylcast.clans.models;

public class RunnableTimerEntry {
    public long StartTime;
    public int TaskId;

    public RunnableTimerEntry(long startTime, int taskId){
        StartTime = startTime;
        TaskId = taskId;
    }
}
