package raylcast.clans.handlers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;

import java.util.Random;
import java.util.logging.Logger;

public abstract class ClanHandler implements Listener {
    protected Logger Logger;
    protected Random Random;
    protected Plugin Plugin;

    private final Permission ClanMemberPermission;
    private FileConfiguration Config;

    public ClanHandler(Permission clanMemberPermission){
        ClanMemberPermission = clanMemberPermission;
    }

    public void injectServices(Logger logger, Random random, Plugin plugin,
                               FileConfiguration config){
        Logger = logger;
        Random = random;
        Plugin = plugin;
        Config = config;
    }

    public abstract void onEnable();
    public abstract void onDisable();

    protected boolean isMember(Player player){
        if (Config.getBoolean("enableAllAbilities")){
            return true;
        }

        return player.hasPermission(ClanMemberPermission);
    }
}
