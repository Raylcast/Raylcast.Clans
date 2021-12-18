package raylcast.clans.handlers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import raylcast.clans.models.ClanType;
import raylcast.clans.services.ClanMemberService;

import java.util.Random;
import java.util.logging.Logger;

public abstract class ClanHandler implements Listener {
    protected Logger Logger;
    protected Random Random;
    protected Plugin Plugin;
    private ClanMemberService ClanMemberService;
    private FileConfiguration Config;

    public void injectServices(Logger logger, Random random, Plugin plugin,
                               ClanMemberService clanMemberService, FileConfiguration config){
        Logger = logger;
        Random = random;
        Plugin = plugin;
        ClanMemberService = clanMemberService;
        Config = config;
    }

    public abstract void onEnable();
    public abstract void onDisable();

    protected boolean isMember(Player player, ClanType type){
        if (Config.getBoolean("enableAllAbilities")){
            return true;
        }

        return ClanMemberService.GetPlayerClanType(player) == type;
    }
}
