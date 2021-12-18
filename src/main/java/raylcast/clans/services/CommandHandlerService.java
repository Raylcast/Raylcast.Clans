package raylcast.clans.services;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import raylcast.clans.commands.clan.ClanCommand;
import raylcast.clans.commands.clan.CommandBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandHandlerService {
    private final JavaPlugin Plugin;
    private final ClanMemberService ClanMemberService;

    private final Map<String, CommandBase> Commands;

    public CommandHandlerService(JavaPlugin plugin, ClanMemberService clanMemberService){
        Commands = new HashMap<>();

        Plugin = plugin;
        ClanMemberService = clanMemberService;

        for (var command : GetCommands()){
            Commands.put(command.getName().toUpperCase(), command);
        }
    }

    private CommandBase[] GetCommands(){
        return new CommandBase[] {
          new ClanCommand(ClanMemberService),
        };
    }

    public void onEnable(){
        for(var command : Commands.values()){
            Plugin.getCommand(command.getName()).setExecutor(command);
            Plugin.getCommand(command.getName()).setTabCompleter(Plugin);
        }
    }

    public void onDisable(){
    }

    public List<String> onTabComplete(String label, List<String> args){
        if (args.isEmpty()){
            return Commands.values().stream().map(CommandBase::getName).filter(name -> name.toUpperCase().startsWith(label.toUpperCase())).toList();
        }

        var command = Commands.get(label.toUpperCase());

        if (command == null){
            return null;
        }

        return command.onTabComplete(args.stream().toList());
    }
}
