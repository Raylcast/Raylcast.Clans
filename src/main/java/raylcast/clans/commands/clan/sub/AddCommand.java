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

public class AddCommand extends SubCommand {
    private LuckPerms LuckPerms;

    @Override
    public String getName() {
        return "Add";
    }
    @Override
    public String getDescription() {
        return "Add a player to a clan";
    }

    @Override
    public String getUsage() {
        return getName() + " [Target] [Clan] : " + getDescription();
    }

    @Override
    public Permission getPermission() {
        return new Permission("raylcast.clans.command.clan.add");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, List<String> args) {
        if (args.size() != 2){
            return false;
        }
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("This command is only for players!");
            return true;
        }

        var target = Bukkit.getPlayerExact(args.get(0));

        if (target == null){
            commandSender.sendMessage("Target player not found");
            return true;
        }

        if (LuckPerms == null){
            LuckPerms = Objects.requireNonNull(Bukkit.getServicesManager().getRegistration(LuckPerms.class)).getProvider();
        }

        try{
            var clanType = ClanType.valueOf(args.get(1));
            LuckPerms.getUserManager().modifyUser(target.getUniqueId(), user -> {
                var userData = user.data();
                var node = userData.toCollection().stream().filter(n -> n.getKey().equals(clanType.getPermission().getName())).findFirst();
                node.ifPresent(userData::remove);
                user.data().add(Node.builder(clanType.getPermission().getName()).build());
            });

            player.sendMessage(ChatColor.GREEN + "Success, Clan has been added to " + target.getName());
        }
        catch (IllegalArgumentException e){
            commandSender.sendMessage(ChatColor.RED + "There is no clan with this name. Please use the exact spelling!");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(List<String> args) {
        if (args.size() >= 3){
            return new ArrayList<>();
        }
        if (args.size() == 2){
            return Arrays.stream(ClanType.values()).map(Enum::name).toList();
        }

        return null;
    }
}
