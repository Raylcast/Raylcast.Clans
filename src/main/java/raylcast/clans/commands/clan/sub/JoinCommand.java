package raylcast.clans.commands.clan.sub;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import raylcast.clans.commands.SubCommand;
import raylcast.clans.models.ClanType;
import raylcast.clans.services.ClanMemberService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JoinCommand extends SubCommand {
    private final ClanMemberService ClanMemberService;

    public JoinCommand(ClanMemberService clanMemberService){
        ClanMemberService = clanMemberService;
    }

    @Override
    public String getName() {
        return "Join";
    }
    @Override
    public String getDescription() {
        return "Show information about a clan";
    }

    @Override
    public String getUsage() {
        return getName() + " [Clan] : " + getDescription();
    }

    @Override
    public boolean onCommand(CommandSender commandSender, List<String> args) {
        if (args.isEmpty()){
            return false;
        }
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("This command is only for players!");
            return true;
        }

        try{
            var clanType = ClanType.valueOf(args.get(0));
            ClanMemberService.SetPlayerClanType(player, clanType);

            player.sendMessage(ChatColor.GREEN + "Success, Clan set!");
        }
        catch (IllegalArgumentException e){
            commandSender.sendMessage(ChatColor.RED + "There is no clan with this name. Please use the exact spelling!");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(List<String> args) {
        if (args.size() > 1){
           return new ArrayList<>();
        }

        return Arrays.stream(ClanType.values()).map(Enum::name).toList();
    }
}
