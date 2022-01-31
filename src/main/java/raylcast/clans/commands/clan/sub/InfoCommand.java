package raylcast.clans.commands.clan.sub;

import net.luckperms.api.node.Node;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import raylcast.clans.InfoTexts;
import raylcast.clans.commands.SubCommand;
import raylcast.clans.models.ClanType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InfoCommand extends SubCommand {
    @Override
    public String getName() {
        return "Info";
    }

    @Override
    public String getUsage() {
        return getName() + " [Clan] : " + getDescription();
    }

    @Override
    public String getDescription() {
        return "Show information about a clan";
    }

    @Override
    public Permission getPermission() {
        return new Permission("raylcast.clans.command.clan.info");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, List<String> args) {
        if (args.size() != 1){
            return false;
        }

        try{
            var clanType = ClanType.valueOf(args.get(0));
            String text = InfoTexts.getText(clanType);
            commandSender.sendMessage(ChatColor.GOLD + text);
        }
        catch (IllegalArgumentException e){
            commandSender.sendMessage(ChatColor.RED + "There is no clan with this name. Please use the exact spelling!");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, List<String> args) {
        if (args.size() > 1){
            return new ArrayList<>();
        }

        return Arrays.stream(ClanType.values()).map(Enum::name).toList();
    }
}
