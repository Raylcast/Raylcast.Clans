package raylcast.clans.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import raylcast.clans.commands.clan.CommandBase;

import java.util.*;

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
    public abstract Permission getPermission();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!commandSender.hasPermission(getPermission())){
            return true;
        }

        if (args.length == 0){
            commandSender.sendMessage(getGroupedUsage(commandSender));
            return true;
        }

        var subCommand = SubCommands.get(args[0].toUpperCase());

        if (subCommand == null){
            commandSender.sendMessage(getGroupedUsage(commandSender));
            return true;
        }
        if (!commandSender.hasPermission(subCommand.getPermission())){
            commandSender.sendMessage("You do not have permissions to run this command!");
            return true;
        }

        boolean success = subCommand.onCommand(commandSender, Arrays.stream(args).skip(1).toList());

        if (!success){
            commandSender.sendMessage("Use /" + getName() + " " + subCommand.getUsage());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, List<String> args) {
        if (args.isEmpty()) {
            return SubCommands.values().stream()
                    .filter(command -> sender.hasPermission(command.getPermission()))
                    .map(SubCommand::getName).toList();
        }
        if (args.size() == 1){
            return SubCommands.values().stream()
                    .filter(command -> sender.hasPermission(command.getPermission()))
                    .map(CommandBase::getName)
                    .filter(name -> name.toUpperCase().startsWith(args.get(0).toUpperCase()))
                    .toList();
        }

        var command = SubCommands.get(args.get(0).toUpperCase());

        if(command == null){
            return new ArrayList<>();
        }
        if (!sender.hasPermission(command.getPermission())){
            return new ArrayList<>();
        }

        return command.onTabComplete(sender, args.stream().skip(1).toList());
    }

    private String getGroupedUsage(CommandSender sender){
        var sb = new StringBuilder();

        sb.append("Use /");
        sb.append(getName());
        sb.append(" [Sub Command]");
        sb.append('\n');

        for(var subCommand : SubCommands.values()){
            if (!sender.hasPermission(subCommand.getPermission())){
                continue;
            }
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
