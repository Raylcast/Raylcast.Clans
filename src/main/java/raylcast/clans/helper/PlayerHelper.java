package raylcast.clans.helper;

import org.bukkit.entity.Player;

public class PlayerHelper {
    public static boolean takeXP(Player player, int amount){
            int currentXP = (int) (player.getExpToLevel() * player.getExp());

            if (currentXP < amount){
                if (player.getLevel() == 0){
                    return false;
                }

                player.setLevel(player.getLevel() - 1);
                player.setExp((player.getExpToLevel() + currentXP - amount) / (float)player.getExpToLevel());
            }
            else {
                player.setExp((currentXP - amount) / (float)player.getExpToLevel());
            }

            return true;
    }
}
