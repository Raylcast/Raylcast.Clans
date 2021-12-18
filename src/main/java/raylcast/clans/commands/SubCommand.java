package raylcast.clans.commands;

import org.bukkit.command.CommandSender;
import raylcast.clans.commands.clan.CommandBase;

import java.util.List;

public abstract class SubCommand extends CommandBase {
    public abstract String getName();
    public abstract String getDescription();
    public String getUsage(){
        return getName() + " : " + getDescription();
    }

    public abstract boolean onCommand(CommandSender commandSender, List<String> args);
}
