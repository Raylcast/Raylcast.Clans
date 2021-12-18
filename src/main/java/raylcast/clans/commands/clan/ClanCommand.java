package raylcast.clans.commands.clan;

import raylcast.clans.commands.GroupedCommand;
import raylcast.clans.commands.SubCommand;
import raylcast.clans.commands.clan.sub.InfoCommand;
import raylcast.clans.commands.clan.sub.JoinCommand;
import raylcast.clans.commands.clan.sub.ListCommand;
import raylcast.clans.services.ClanMemberService;

import java.util.Map;

public class ClanCommand extends GroupedCommand {
    public static final String CommandName = "Clan";

    private Map<String, SubCommand> SubCommands;

    private final ClanMemberService ClanMemberService;

    public ClanCommand(ClanMemberService clanMemberService){
        ClanMemberService = clanMemberService;
        init();
    }

    @Override
    public SubCommand[] loadSubcommands() {
        return new SubCommand[] {
            new InfoCommand(),
            new ListCommand(),
            new JoinCommand(ClanMemberService)
        };
    }

    @Override
    public String getName() {
        return CommandName;
    }
}
