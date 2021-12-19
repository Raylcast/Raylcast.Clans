package raylcast.clans.models;

public enum ClanType {
    None(0),
    Fireborn(10),
    Enderborn(20),
    Earthborn(30),
    Thunderborn(40);

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
