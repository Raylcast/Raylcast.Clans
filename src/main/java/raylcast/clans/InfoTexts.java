package raylcast.clans;

import raylcast.clans.models.ClanType;

public class InfoTexts {
    public static String Fireborn = "Members of the Fireborn are hot. And I don't mean how they look, they're literally on fire! AAAH! They are passively immune against things as hot as they are  and do not experience harm from their old pig buddies. They move faster while on FIIREEEEEEEA! The tool called Flint and Steel lasts for longer. The best part about their kit is a legendary rocket jump allowing them to fly high and show everyone how hot they are DAMN! But if they charge it for too long, they die. When they die, no matter what the cause is, their brain explodes, taking their opponent with them. Their weakness is water: They take damage in it and are cursed with horrible effects.";
    public static String Earthborn = "Risen from an empire far below the dirty ground, the Earthborn have come to the surface to defend mother nature. Thanks to their friendship, several creatures do not attack them ever. They can automatically harvest crops and save bone meal doing so. While in god's tears, their movement speed is greatly increased. They can hand him a handkerchief or make him cry by looking up and charging a weather-changing beam. However, killing god's creations will result in divine punishment. If Earthborn leave earth and go to heaven, they leave behind a wildlife gift.";
    public static String Enderborn = "Time travel isn't real. But teleporting is. The Enderborn prove this. Their magic presence allows them to gain experience faster and not take damage from throwing their magic pearls. After doing so, they move faster for short time. They have a good chance of not losing those precious pearls. Teleporting can lead one into a falling situation, in which they can rub a feather to create a magic sphere holding onto the air trying to not let go and keep them up. This magic has a drawback, after eating they might teleport uncontrollably. If they happen to put their wizard hat aside and join the dead, a fat Dragon Fireball crashes into their grave and lacerates the criminal scum into a thousand pieces.";

    public static String getText(ClanType clanType){
        return switch (clanType) {
            case Fireborn -> Fireborn;
            case Earthborn -> Earthborn;
            case Enderborn -> Enderborn;

            default -> "Lol not done yet neb!";
        };
    }
}
