package raylcast.clans.commands.clan;

import raylcast.clans.commands.GroupedCommand;
import raylcast.clans.commands.SubCommand;
import raylcast.clans.commands.clan.sub.*;

import java.util.Map;

public class ClanCommand extends GroupedCommand {
    public static final String CommandName = "Clan";
    private Map<String, SubCommand> SubCommands;

    public ClanCommand(){
        init();
    }

    @Override
    public SubCommand[] loadSubcommands() {
        return new SubCommand[] {
            new InfoCommand(),
            new ListCommand(),
            new JoinCommand(),
            new LeaveCommand(),
            new AddCommand(),
        };
    }

    @Override
    public String getName() {
        return CommandName;
    }
}
