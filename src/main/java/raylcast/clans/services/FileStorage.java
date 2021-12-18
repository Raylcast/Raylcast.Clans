package raylcast.clans.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import raylcast.clans.models.ClanType;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FileStorage {
    private final Path PlayerClansFilePath = Path.of("PlayerClans.json");

    public Map<UUID, ClanType> LoadPlayerClans() {
        if (!PlayerClansFilePath.toFile().exists()){
            return new HashMap<>();
        }

        try{
            var json = Files.readString(PlayerClansFilePath);

            var map = new Gson().fromJson(json, Map.class);

            if (map == null){
                return new HashMap<>();
            }

            var finalMap = new HashMap<UUID, ClanType>();

            map.forEach((pId, clanType) -> {
                finalMap.put(UUID.fromString((String)pId), ClanType.fromId((int)clanType));
            });

            return finalMap;
        }
        catch (IOException e) {
            return new HashMap<>();
        }
    }

    public void WritePlayerClans(Map<UUID, ClanType> playerClans) throws IOException {
        var json = new JsonObject();

        playerClans.forEach((pId, clanType) -> {
            json.addProperty(pId.toString(), clanType.getId());
        });

        var writer = new FileWriter("PlayerClans.json", false);
        writer.write(json.toString());
    }
}
