package raylcast.clans;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import raylcast.clans.handlers.*;
import raylcast.clans.services.ClanMemberService;
import raylcast.clans.services.CommandHandlerService;
import raylcast.clans.services.FileStorage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class RaylcastClans extends JavaPlugin {
    private final Logger Logger;
    private final Random Random;

    private final List<ClanHandler> ClanHandlers;

    private final FileStorage Storage;
    private final ClanMemberService ClanMemberService;
    private final CommandHandlerService CommandHandlerService;

    private FileConfiguration Config;

    public RaylcastClans(){
        Logger = getLogger();
        Random = new Random();
        ClanHandlers = new ArrayList<>();

        Storage = new FileStorage();
        ClanMemberService = new ClanMemberService(Storage);
        CommandHandlerService = new CommandHandlerService(this, ClanMemberService);

        saveDefaultConfig();
        Config = getConfig();

        InstantiateHandlers();
    }

    public void InstantiateHandlers(){
        ClanHandlers.add(new FirebornHandler());
        ClanHandlers.add(new EnderbornHandler());
        ClanHandlers.add(new EarthbornHandler());
        ClanHandlers.add(new ThunderbornHandler());

        for (var clanHandler : ClanHandlers){
            clanHandler.injectServices(Logger, Random, this, ClanMemberService, Config);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Config.addDefault("enableAllAbilities", true);
        Config.options().copyDefaults(true);
        saveConfig();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        for(var clanHandler : ClanHandlers){
            getServer().getPluginManager().registerEvents(clanHandler, this);
            clanHandler.onEnable();
        }

        ClanMemberService.onEnable();
        CommandHandlerService.onEnable();
        Config = getConfig();
    }

    @Override
    public void onDisable() {
        super.onDisable();

        for(var clanHandler : ClanHandlers){
            clanHandler.onDisable();
        }

        CommandHandlerService.onDisable();
        ClanMemberService.onDisable();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return CommandHandlerService.onTabComplete(command.getName(), Arrays.stream(args).toList());
    }


}
