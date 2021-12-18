package raylcast.clans.models;

import javax.naming.OperationNotSupportedException;

public enum ClanType {
    None(0),
    Fireborn(10),
    Magician(20),
    Farmer(30),
    Cyborg(40);

    private final int Id;

    private ClanType(int id){
        Id = id;
    }

    public int getId() {
        return Id;
    }

    public static ClanType fromId(int id){
        switch (id){
            case 0:
                return None;
            case 10:
                return Fireborn;
            case 20:
                return Magician;
            case 30:
                return Farmer;
            case 40:
                return Cyborg;
            default:
                throw new RuntimeException("Invalid ClanType Id!");
        }
    }
}
