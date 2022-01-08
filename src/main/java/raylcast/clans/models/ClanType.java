package raylcast.clans.models;

import org.bukkit.permissions.Permission;
import raylcast.clans.handlers.EarthbornHandler;

public enum ClanType {
    None(0, null),
    Fireborn(10, new Permission("raylcast.clans.member.fireborn")),
    Enderborn(20, new Permission("raylcast.clans.member.enderborn")),
    Earthborn(30, new Permission("raylcast.clans.member.earthborn")),
    Thunderborn(40, new Permission("raylcast.clans.member.thunderborn"));

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
        switch (id){
            case 0:
                return None;
            case 10:
                return Fireborn;
            case 20:
                return Enderborn;
            case 30:
                return Earthborn;
            case 40:
                return Thunderborn;
            default:
                throw new RuntimeException("Invalid ClanType Id!");
        }
    }
}
