package raylcast.clans.models;

import org.bukkit.permissions.Permission;
import raylcast.clans.handlers.EarthbornHandler;

public enum ClanType {
    None(0, null),
    Fireborn(10, new Permission("raylcast.clans.member.fireborn")),
    Enderborn(20, new Permission("raylcast.clans.member.enderborn")),
    Earthborn(30, new Permission("raylcast.clans.member.earthborn"));
    //Thunderborn(40, new Permission("raylcast.clans.member.thunderborn"));

    private final int Id;
    private final Permission Permission;

    private ClanType(int id, Permission permission){
        Id = id;
        Permission = permission;
    }

    public int getId() {
        return Id;
    }

    public Permission getPermission(){
        return Permission;
    }

    public static ClanType fromId(int id){
        return switch (id) {
            case 0 -> None;
            case 10 -> Fireborn;
            case 20 -> Enderborn;
            case 30 -> Earthborn;
            //case 40:
            //return Thunderborn;
            default -> throw new RuntimeException("Invalid ClanType Id!");
        };
    }
}
