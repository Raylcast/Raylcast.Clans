package raylcast.clans.models;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import fr.skytasul.guardianbeam.Laser;

public class LaserState {
    private Laser Laser;

    public LaserState(Plugin plugin, int lifetime, Location startLocation, Location endLocation) {
        try {
            Laser = new Laser.GuardianLaser(startLocation, endLocation, lifetime, 100);
            Laser.start(plugin);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public void setEnd(Location endLocation){
        if (Laser == null){
            return;
        }

        try {
            Laser.moveEnd(endLocation);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        if (Laser == null){
            return;
        }

        Laser.stop();
    }
}
