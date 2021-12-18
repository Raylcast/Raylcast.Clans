package raylcast.clans.commands.clan;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public abstract class CommandBase implements CommandExecutor {
    public abstract String getName();

    public List<String> onTabComplete(List<String> args){
        return null;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        throw new RuntimeException("Not implemented!");
    }
}
