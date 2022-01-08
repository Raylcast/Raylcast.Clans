package raylcast.clans.services;

import net.luckperms.api.LuckPerms;
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
    private final Map<String, CommandBase> Commands;

    public CommandHandlerService(JavaPlugin plugin){
        Commands = new HashMap<>();

        Plugin = plugin;

        for (var command : GetCommands()){
            Commands.put(command.getName().toUpperCase(), command);
        }
    }

    private CommandBase[] GetCommands(){
        return new CommandBase[] {
          new ClanCommand(),
        };
    }

    public void onEnable(){
        for(var command : Commands.values()){
            var cmd = Plugin.getCommand(command.getName());

            if (cmd == null){
                throw new NullPointerException("cmd is null");
            }

            cmd.setExecutor(command);
            cmd.setTabCompleter(Plugin);
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
