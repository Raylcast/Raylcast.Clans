package raylcast.clans.commands.clan.sub;

import org.bukkit.command.CommandSender;
import raylcast.clans.commands.SubCommand;
import raylcast.clans.models.ClanType;

import java.util.List;

public class ListCommand extends SubCommand {
    @Override
    public String getName() {
        return "List";
    }

    @Override
    public String getDescription() {
        return "Show all clans";
    }

    @Override
    public boolean onCommand(CommandSender commandSender, List<String> args) {
        var clanTypes = ClanType.values();

        var sb = new StringBuilder();

        for(var clanType : clanTypes){
            sb.append(clanType.name());
            sb.append('\n');
        }

        commandSender.sendMessage(sb.toString());
        return true;
    }
}
