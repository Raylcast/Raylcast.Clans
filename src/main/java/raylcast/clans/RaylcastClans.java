package raylcast.clans;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;
import raylcast.clans.handlers.*;
import raylcast.clans.models.ClanType;
import raylcast.clans.services.CommandHandlerService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class RaylcastClans extends JavaPlugin {
    private final Logger Logger;
    private final Random Random;

    private final List<ClanHandler> ClanHandlers;
    private final CommandHandlerService CommandHandlerService;

    private LuckPerms LuckPerms;
    private FileConfiguration Config;

    public RaylcastClans(){
        Logger = getLogger();
        Random = new Random();
        ClanHandlers = new ArrayList<>();

        CommandHandlerService = new CommandHandlerService(this);

        saveDefaultConfig();
        Config = getConfig();

        InstantiateHandlers();
    }

    public void InstantiateHandlers(){
        ClanHandlers.add(new FirebornHandler(ClanType.Fireborn.getPermission()));
        ClanHandlers.add(new EnderbornHandler(ClanType.Enderborn.getPermission()));
        ClanHandlers.add(new EarthbornHandler(ClanType.Earthborn.getPermission()));
        //ClanHandlers.add(new ThunderbornHandler(ClanType.Thunderborn.getPermission()));

        for (var clanHandler : ClanHandlers){
            clanHandler.injectServices(Logger, Random, this, Config);
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

        var customItems = new CustomItems(this);

        for(var clanHandler : ClanHandlers){
            getServer().getPluginManager().registerEvents(clanHandler, this);
            clanHandler.onEnable();
        }

        for(var recipe : customItems.getRecipes()){
            Bukkit.addRecipe(recipe);
        }

        getServer().getPluginManager().registerEvents(new DragonFightHandler(this), this);

        var registration = Bukkit.getServicesManager().getRegistration(LuckPerms.class);

        if (registration == null){
            throw new IllegalStateException("Can't enable the plugin without Luckperms installed!");
        }

        LuckPerms = registration.getProvider();
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
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return CommandHandlerService.onTabComplete(sender, command.getName(), Arrays.stream(args).toList());
    }


}
