package raylcast.clans.models;

public class ChargedAbilityEntry extends AbilityEntry {
    public int TaskId;

    public ChargedAbilityEntry(long startTime, int taskId){
        super(startTime);
        TaskId = taskId;
    }
}
