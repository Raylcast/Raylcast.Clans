package raylcast.clans.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import raylcast.clans.commands.clan.CommandBase;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GroupedCommand extends CommandBase {
    private final Map<String, SubCommand> SubCommands;

    public GroupedCommand(){
        SubCommands = new HashMap<>();
    }

    protected void init(){
        for(var subCommand : loadSubcommands()){
            SubCommands.put(subCommand.getName().toUpperCase(), subCommand);
        }
    }

    public abstract SubCommand[] loadSubcommands();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (args.length == 0){
            commandSender.sendMessage(getGroupedUsage());
            return true;
        }

        var subCommand = SubCommands.get(args[0].toUpperCase());

        if (subCommand == null){
            commandSender.sendMessage(getGroupedUsage());
            return true;
        }

        boolean success = subCommand.onCommand(commandSender, Arrays.stream(args).skip(1).toList());

        if (!success){
            commandSender.sendMessage("Use /" + getName() + " " + subCommand.getUsage());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(List<String> args) {
        if (args.isEmpty()) {
            return SubCommands.values().stream().map(SubCommand::getName).toList();
        }
        if (args.size() == 1){
            return SubCommands.values().stream().map(CommandBase::getName).filter(name -> name.toUpperCase().startsWith(args.get(0).toUpperCase())).toList();
        }

        var command = SubCommands.get(args.get(0).toUpperCase());

        if(command == null){
            return null;
        }

        return command.onTabComplete(args.stream().skip(1).toList());
    }

    private String getGroupedUsage(){
        var sb = new StringBuilder();

        sb.append("Use /");
        sb.append(getName());
        sb.append(" [Sub Command]");
        sb.append('\n');

        for(var subCommand : SubCommands.values()){
            sb.append(ChatColor.GOLD);
            sb.append(subCommand.getName());
            sb.append(" : ");
            sb.append(ChatColor.WHITE);
            sb.append(subCommand.getDescription());
            sb.append('\n');
        }

        return sb.toString();
    }


}
