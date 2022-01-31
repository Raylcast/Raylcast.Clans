package raylcast.clans.commands.clan.sub;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import raylcast.clans.commands.SubCommand;
import raylcast.clans.models.ClanType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class JoinCommand extends SubCommand {
    private LuckPerms LuckPerms;

    @Override
    public String getName() {
        return "Join";
    }
    @Override
    public String getDescription() {
        return "Join a clan";
    }

    @Override
    public String getUsage() {
        return getName() + " [Clan] : " + getDescription();
    }

    @Override
    public Permission getPermission() {
        return new Permission("raylcast.clans.command.clan.join");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, List<String> args) {
        if (args.size() != 1){
            return false;
        }
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("This command is only for players!");
            return true;
        }

        if (LuckPerms == null){
            LuckPerms = Objects.requireNonNull(Bukkit.getServicesManager().getRegistration(LuckPerms.class)).getProvider();
        }

        try{
            var clanType = ClanType.valueOf(args.get(0));
            LuckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> {
                var userData = user.data();
                var node = userData.toCollection().stream().filter(n -> n.getKey().equals(clanType.getPermission().getName())).findFirst();
                node.ifPresent(userData::remove);
                user.data().add(Node.builder(clanType.getPermission().getName()).build());
            });

            player.sendMessage(ChatColor.GREEN + "Success, You've joined " + clanType.name());
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
