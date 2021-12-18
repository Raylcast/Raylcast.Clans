package raylcast.clans.services;

import org.bukkit.entity.Player;
import raylcast.clans.models.ClanType;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class ClanMemberService {
    private final FileStorage Storage;

    private Map<UUID, ClanType> PlayerClans;

    public ClanMemberService(FileStorage storage){
        Storage = storage;
    }

    public void onEnable() {
        PlayerClans = Storage.LoadPlayerClans();
    }
    public void onDisable() {
        try {
            Storage.WritePlayerClans(PlayerClans);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Saving PlayerClans failed!");
        }
    }

    public ClanType GetPlayerClanType(Player player){
        return PlayerClans.getOrDefault(player.getUniqueId(), ClanType.None);
    }

    public void SetPlayerClanType(Player player, ClanType clanType){
        PlayerClans.put(player.getUniqueId(), clanType);
    }

    public void DeletePlayerClanType(Player player){
        PlayerClans.remove(player.getUniqueId());
    }
}
