package fr.skytasul.guardianbeam;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * A whole class to create Guardian Lasers and Ender Crystal Beams using packets and reflection.<br>
 * Inspired by the API <a href="https://www.spigotmc.org/resources/guardianbeamapi.18329">GuardianBeamAPI</a><br>
 * <b>1.9 -> 1.18.1</b>
 *
 * @see <a href="https://github.com/SkytAsul/GuardianBeam">GitHub page</a>
 * @version 2.1.0
 * @author SkytAsul
 */
public abstract class Laser {

    protected final int distanceSquared;
    protected final int duration;
    protected boolean durationInTicks = false;
    protected Location start;
    protected Location end;

    protected Plugin plugin;
    protected BukkitRunnable main;
    protected BukkitTask startMove, endMove;
    protected Set<Player> show = ConcurrentHashMap.newKeySet();
    private Set<Player> seen = new HashSet<>();

    private List<Runnable> executeEnd = new ArrayList<>(1);

    protected Laser(Location start, Location end, int duration, int distance) {
        if (!Packets.enabled) throw new IllegalStateException("The Laser Beam API is disabled. An error has occured during initialization.");
        this.start = start;
        this.end = end;
        this.duration = duration;
        distanceSquared = distance < 0 ? -1 : distance * distance;
    }

    /**
     * Adds a runnable to execute when the laser reaches its final duration
     * @param runnable action to execute
     * @return this {@link Laser} instance
     */
    public Laser executeEnd(Runnable runnable) {
        executeEnd.add(runnable);
        return this;
    }

    /**
     * Makes the duration provided in the constructor passed as ticks and not seconds
     * @return this {@link Laser} instance
     */
    public Laser durationInTicks() {
        durationInTicks = true;
        return this;
    }

    /**
     * Starts this laser.
     * It will make the laser visible for nearby players and start the countdown to the final duration.
     * Once finished, it will destroy the laser and execute all runnables passed with {@link Laser#executeEnd}.
     * @param plugin plugin used to start the task
     */
    public void start(Plugin plugin) {
        Validate.isTrue(main == null, "Task already started");
        this.plugin = plugin;
        main = new BukkitRunnable() {
            int time = 0;

            @Override
            public void run() {
                try {
                    if (time == duration) {
                        cancel();
                        return;
                    }
                    if (!durationInTicks || time % 20 == 0) {
                        for (Player p : start.getWorld().getPlayers()) {
                            if (isCloseEnough(p.getLocation())) {
                                if (show.add(p)) {
                                    sendStartPackets(p, !seen.add(p));
                                }
                            }else if (show.remove(p)) {
                                sendDestroyPackets(p);
                            }
                        }
                    }
                    time++;
                }catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                super.cancel();
                main = null;
                try {
                    for (Player p : show) {
                        sendDestroyPackets(p);
                    }
                    show.clear();
                    executeEnd.forEach(Runnable::run);
                }catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                }
            }
        };
        main.runTaskTimerAsynchronously(plugin, 0L, durationInTicks ? 1L : 20L);
    }

    /**
     * Stops this laser.<br>
     *
     * This will destroy the laser for every player and start execute all runnables passed with {@link Laser#executeEnd}
     */
    public void stop() {
        Validate.isTrue(main != null, "Task not started");
        main.cancel();
    }

    public boolean isStarted() {
        return main != null;
    }

    public abstract LaserType getLaserType();

    /**
     * Instantly moves the start of the laser to the location provided.
     * @param location New start location
     * @throws ReflectiveOperationException if a reflection exception occurred during laser moving
     */
    public abstract void moveStart(Location location) throws ReflectiveOperationException;

    /**
     * Instantly moves the end of the laser to the location provided.
     * @param location New end location
     * @throws ReflectiveOperationException if a reflection exception occurred during laser moving
     */
    public abstract void moveEnd(Location location) throws ReflectiveOperationException;

    public Location getStart() {
        return start;
    }

    public Location getEnd() {
        return end;
    }

    /**
     * Moves the start of the laser smoothly to the new location, within a given time.
     * @param location New start location to go to
     * @param ticks Duration (in ticks) to make the move
     * @param callback {@link Runnable} to execute at the end of the move (nullable)
     */
    public void moveStart(Location location, int ticks, Runnable callback) {
        startMove = moveInternal(location, ticks, startMove, this::getStart, this::moveStart, callback);
    }

    /**
     * Moves the end of the laser smoothly to the new location, within a given time.
     * @param location New end location to go to
     * @param ticks Duration (in ticks) to make the move
     * @param callback {@link Runnable} to execute at the end of the move (nullable)
     */
    public void moveEnd(Location location, int ticks, Runnable callback) {
        endMove = moveInternal(location, ticks, endMove, this::getEnd, this::moveEnd, callback);
    }

    private BukkitTask moveInternal(Location location, int ticks, BukkitTask oldTask, Supplier<Location> locationSupplier, ReflectiveConsumer<Location> moveConsumer, Runnable callback) {
        Validate.isTrue(ticks > 0);
        Validate.isTrue(plugin != null, "Task didn't start once");
        if (oldTask != null && !oldTask.isCancelled()) oldTask.cancel();
        return new BukkitRunnable() {
            double xPerTick = (location.getX() - locationSupplier.get().getX()) / ticks;
            double yPerTick = (location.getY() - locationSupplier.get().getY()) / ticks;
            double zPerTick = (location.getZ() - locationSupplier.get().getZ()) / ticks;
            int elapsed = 0;

            @Override
            public void run() {
                try {
                    moveConsumer.accept(locationSupplier.get().add(xPerTick, yPerTick, zPerTick));
                }catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                    cancel();
                    return;
                }

                if (++elapsed == ticks) {
                    cancel();
                    if (callback != null) callback.run();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    protected void moveFakeEntity(Location location, int entityId, Object fakeEntity) throws ReflectiveOperationException {
        Object packet;
        if (fakeEntity == null) {
            packet = Packets.createPacketMoveEntity(location, entityId);
        }else {
            Packets.moveFakeEntity(fakeEntity, location);
            packet = Packets.createPacketMoveEntity(fakeEntity);
        }
        for (Player p : show) {
            Packets.sendPackets(p, packet);
        }
    }

    protected abstract void sendStartPackets(Player p, boolean hasSeen) throws ReflectiveOperationException;

    protected abstract void sendDestroyPackets(Player p) throws ReflectiveOperationException;

    private boolean isCloseEnough(Location location) {
        return distanceSquared == -1 ||
                start.distanceSquared(location) <= distanceSquared ||
                end.distanceSquared(location) <= distanceSquared;
    }

    public static class GuardianLaser extends Laser {
        private static int teamID = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);

        private final Object createGuardianPacket;
        private final Object createSquidPacket;
        private final Object teamCreatePacket;
        private final Object[] destroyPackets;
        private final Object metadataPacketGuardian;
        private final Object metadataPacketSquid;
        private final Object fakeGuardianDataWatcher;

        private final Object squid;
        private final int squidID;
        private final UUID squidUUID;
        private final Object guardian;
        private final int guardianID;
        private final UUID guardianUUID;

        /**
         * Creates a new Guardian Laser instance
         * @param start Location where laser will starts
         * @param end Location where laser will ends
         * @param duration Duration of laser in seconds (<i>-1 if infinite</i>)
         * @param distance Distance where laser will be visible (<i>-1 if infinite</i>)
         * @throws ReflectiveOperationException if a reflection exception occurred during Laser creation
         * @see #durationInTicks() to make the duration in ticks
         * @see #executeEnd(Runnable) to add Runnable-s to execute when the laser will stop
         */
        public GuardianLaser(Location start, Location end, int duration, int distance) throws ReflectiveOperationException {
            super(start, end, duration, distance);

            if (Packets.version < 17) {
                squid = null;
                createSquidPacket = Packets.createPacketEntitySpawnLiving(end, Packets.mappings.getSquidID());
            }else {
                squid = Packets.createSquid(end);
                createSquidPacket = Packets.createPacketEntitySpawnLiving(squid);
            }
            squidID = (int) Packets.getField(createSquidPacket, "a");
            squidUUID = (UUID) Packets.getField(createSquidPacket, "b");
            metadataPacketSquid = Packets.createPacketMetadata(squidID, Packets.fakeSquidWatcher);
            Packets.setDirtyWatcher(Packets.fakeSquidWatcher);

            fakeGuardianDataWatcher = Packets.createFakeDataWatcher();
            Packets.initGuardianWatcher(fakeGuardianDataWatcher, squidID);
            if (Packets.version < 17) {
                guardian = null;
                createGuardianPacket = Packets.createPacketEntitySpawnLiving(start, Packets.mappings.getGuardianID());
            }else {
                guardian = Packets.createGuardian(start);
                createGuardianPacket = Packets.createPacketEntitySpawnLiving(guardian);
            }
            guardianID = (int) Packets.getField(createGuardianPacket, "a");
            guardianUUID = (UUID) Packets.getField(createGuardianPacket, "b");
            metadataPacketGuardian = Packets.createPacketMetadata(guardianID, fakeGuardianDataWatcher);

            teamCreatePacket = Packets.createPacketTeamCreate("noclip" + teamID++, squidUUID, guardianUUID);
            destroyPackets = Packets.createPacketsRemoveEntities(squidID, guardianID);
        }

        @Override
        public LaserType getLaserType() {
            return LaserType.GUARDIAN;
        }

        @Override
        protected void sendStartPackets(Player p, boolean hasSeen) throws ReflectiveOperationException {
            Packets.sendPackets(p, createSquidPacket, createGuardianPacket);
            Packets.sendPackets(p, metadataPacketSquid, metadataPacketGuardian);
            if (!hasSeen) Packets.sendPackets(p, teamCreatePacket);
        }

        @Override
        protected void sendDestroyPackets(Player p) throws ReflectiveOperationException {
            Packets.sendPackets(p, destroyPackets);
        }

        @Override
        public void moveStart(Location location) throws ReflectiveOperationException {
            this.start = location;
            if (main != null) moveFakeEntity(start, guardianID, guardian);
        }

        @Override
        public void moveEnd(Location location) throws ReflectiveOperationException {
            this.end = location;
            if (main != null) moveFakeEntity(end, squidID, squid);
        }

        /**
         * Asks viewers' clients to change the color of this Laser
         * @throws ReflectiveOperationException
         */
        public void callColorChange() throws ReflectiveOperationException {
            for (Player p : show) {
                Packets.sendPackets(p, metadataPacketGuardian);
            }
        }

    }

    public static class CrystalLaser extends Laser {

        private Object createCrystalPacket;
        private Object metadataPacketCrystal;
        private Object[] destroyPackets;
        private Object fakeCrystalDataWatcher;

        private Object crystal;
        private int crystalID;

        /**
         * Creates a new Ender Crystal Laser instance
         * @param start Location where laser will starts. The Crystal laser do not handle decimal number, it will be rounded to blocks.
         * @param end Location where laser will ends. The Crystal laser do not handle decimal number, it will be rounded to blocks.
         * @param duration Duration of laser in seconds (<i>-1 if infinite</i>)
         * @param distance Distance where laser will be visible (<i>-1 if infinite</i>)
         * @throws ReflectiveOperationException if a reflection exception occurred during Laser creation
         * @see #durationInTicks() to make the duration in ticks
         * @see #executeEnd(Runnable) to add Runnable-s to execute when the laser will stop
         */
        public CrystalLaser(Location start, Location end, int duration, int distance) throws ReflectiveOperationException {
            super(start, end, duration, distance);

            fakeCrystalDataWatcher = Packets.createFakeDataWatcher();
            Packets.setCrystalWatcher(fakeCrystalDataWatcher, end);
            if (Packets.version < 17) {
                crystal = null;
                createCrystalPacket = Packets.createPacketEntitySpawnNormal(start, Packets.crystalID, Packets.crystalType);
            }else {
                crystal = Packets.createCrystal(start);
                createCrystalPacket = Packets.createPacketEntitySpawnNormal(crystal);
            }
            crystalID = (int) Packets.getField(createCrystalPacket, Packets.version < 17 ? "a" : "c");
            metadataPacketCrystal = Packets.createPacketMetadata(crystalID, fakeCrystalDataWatcher);

            destroyPackets = Packets.createPacketsRemoveEntities(crystalID);
        }

        @Override
        public LaserType getLaserType() {
            return LaserType.ENDER_CRYSTAL;
        }

        @Override
        protected void sendStartPackets(Player p, boolean hasSeen) throws ReflectiveOperationException {
            Packets.sendPackets(p, createCrystalPacket);
            Packets.sendPackets(p, metadataPacketCrystal);
        }

        @Override
        protected void sendDestroyPackets(Player p) throws ReflectiveOperationException {
            Packets.sendPackets(p, destroyPackets);
        }

        @Override
        public void moveStart(Location location) throws ReflectiveOperationException {
            this.start = location;
            if (main != null) moveFakeEntity(start, crystalID, crystal);
        }

        @Override
        public void moveEnd(Location location) throws ReflectiveOperationException {
            this.end = location;
            if (main != null) {
                Packets.setCrystalWatcher(fakeCrystalDataWatcher, location);
                metadataPacketCrystal = Packets.createPacketMetadata(crystalID, fakeCrystalDataWatcher);
                for (Player p : show) {
                    Packets.sendPackets(p, metadataPacketCrystal);
                }
            }
        }

    }

    public enum LaserType {
        GUARDIAN, ENDER_CRYSTAL;

        /**
         * Creates a new Laser instance, {@link GuardianLaser} or {@link CrystalLaser} depending on this enum value.
         * @param start Location where laser will starts
         * @param end Location where laser will ends
         * @param duration Duration of laser in seconds (<i>-1 if infinite</i>)
         * @param distance Distance where laser will be visible
         * @throws ReflectiveOperationException if a reflection exception occurred during Laser creation
         * @see #durationInTicks() to make the duration in ticks
         * @see #executeEnd(Runnable) to add Runnable-s to execute when the laser will stop
         */
        public Laser create(Location start, Location end, int duration, int distance) throws ReflectiveOperationException {
            switch (this) {
                case ENDER_CRYSTAL:
                    return new CrystalLaser(start, end, duration, distance);
                case GUARDIAN:
                    return new GuardianLaser(start, end, duration, distance);
            }
            throw new IllegalStateException();
        }
    }

    private static class Packets {
        private static int lastIssuedEID = 2000000000;

        static int generateEID() {
            return lastIssuedEID++;
        }

        private static Logger logger;
        private static int version;
        private static int versionMinor;
        private static String npack = "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        private static String cpack = Bukkit.getServer().getClass().getPackage().getName() + ".";
        private static ProtocolMappings mappings;

        private static int crystalID = 51; // pre-1.13

        private static Class<?> entityTypesClass;
        private static Object crystalType;
        private static Object squidType;
        private static Object guardianType;

        private static Object watcherObject1; // invisilibity
        private static Object watcherObject2; // spikes
        private static Object watcherObject3; // attack id
        private static Object watcherObject4; // crystal target

        private static Constructor<?> watcherConstructor;
        private static Method watcherSet;
        private static Method watcherRegister;
        private static Method watcherDirty;

        private static Constructor<?> blockPositionConstructor;

        private static Constructor<?> packetSpawnLiving;
        private static Constructor<?> packetSpawnNormal;
        private static Constructor<?> packetRemove;
        private static Constructor<?> packetTeleport;
        private static Constructor<?> packetMetadata;
        private static Class<?> packetTeam;

        private static Method createTeamPacket;
        private static Constructor<?> createTeam;
        private static Constructor<?> createScoreboard;
        private static Method setTeamPush;
        private static Object pushNever;
        private static Method getTeamPlayers;

        private static Method getHandle;
        private static Field playerConnection;
        private static Method sendPacket;
        private static Method setLocation;

        private static Object fakeSquid;
        private static Object fakeSquidWatcher;

        private static Object nmsWorld;

        public static boolean enabled = false;

        static {
            try {
                logger = new Logger("GuardianBeam", null) {
                    @Override
                    public void log(LogRecord record) {
                        record.setMessage("[GuardianBeam] " + record.getMessage());
                        super.log(record);
                    }
                };
                logger.setParent(Bukkit.getServer().getLogger());
                logger.setLevel(Level.ALL);

                // e.g. Bukkit.getServer().getClass().getPackage().getName() -> org.bukkit.craftbukkit.v1_17_R1
                String[] versions = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3].substring(1).split("_");
                version = Integer.parseInt(versions[1]); // 1.X
                if (version >= 17) {
                    // e.g. Bukkit.getBukkitVersion() -> 1.17.1-R0.1-SNAPSHOT
                    versions = Bukkit.getBukkitVersion().split("-R")[0].split("\\.");
                    versionMinor = versions.length <= 2 ? 0 : Integer.parseInt(versions[2]);
                }else versionMinor = Integer.parseInt(versions[2].substring(1)); // 1.X.Y

                mappings = ProtocolMappings.getMappings(version);
                if (mappings == null) {
                    mappings = ProtocolMappings.values()[ProtocolMappings.values().length - 1];
                    logger.warning("Loaded not matching version of the mappings for your server version (1." + version + "." + versionMinor + ")");
                }
                logger.info("Loaded mappings " + mappings.name());

                Class<?> entityClass = getNMSClass("world.entity", "Entity");
                entityTypesClass = getNMSClass("world.entity", "EntityTypes");
                watcherObject1 = getField(entityClass, mappings.getWatcherFlags(), null);
                watcherObject2 = getField(getNMSClass("world.entity.monster", "EntityGuardian"), mappings.getWatcherSpikes(), null);
                watcherObject3 = getField(getNMSClass("world.entity.monster", "EntityGuardian"), mappings.getWatcherTargetEntity(), null);
                watcherObject4 = getField(getNMSClass("world.entity.boss.enderdragon", "EntityEnderCrystal"), mappings.getWatcherTargetLocation(), null);

                if (version >= 13) {
                    crystalType = entityTypesClass.getDeclaredField(mappings.getCrystalTypeName()).get(null);
                    if (version >= 17) {
                        squidType = entityTypesClass.getDeclaredField("aJ").get(null);
                        guardianType = entityTypesClass.getDeclaredField("K").get(null);
                    }
                }

                Class<?> dataWatcherClass = getNMSClass("network.syncher", "DataWatcher");
                watcherConstructor = dataWatcherClass.getDeclaredConstructor(entityClass);
                if (version >= 18) {
                    watcherSet = dataWatcherClass.getDeclaredMethod("b", watcherObject1.getClass(), Object.class);
                    watcherRegister = dataWatcherClass.getDeclaredMethod("a", watcherObject1.getClass(), Object.class);
                }else {
                    watcherSet = getMethod(dataWatcherClass, "set");
                    watcherRegister = getMethod(dataWatcherClass, "register");
                }
                if (version >= 15) watcherDirty = getMethod(dataWatcherClass, "markDirty");
                packetSpawnLiving = getNMSClass("network.protocol.game", "PacketPlayOutSpawnEntityLiving").getDeclaredConstructor(version < 17 ? new Class<?>[0] : new Class<?>[] { getNMSClass("world.entity", "EntityLiving") });
                packetSpawnNormal = getNMSClass("network.protocol.game", "PacketPlayOutSpawnEntity").getDeclaredConstructor(version < 17 ? new Class<?>[0] : new Class<?>[] { getNMSClass("world.entity", "Entity") });
                packetRemove = getNMSClass("network.protocol.game", "PacketPlayOutEntityDestroy").getDeclaredConstructor(version == 17 && versionMinor == 0 ? int.class : int[].class);
                packetMetadata = getNMSClass("network.protocol.game", "PacketPlayOutEntityMetadata").getDeclaredConstructor(int.class, dataWatcherClass, boolean.class);
                packetTeleport = getNMSClass("network.protocol.game", "PacketPlayOutEntityTeleport").getDeclaredConstructor(version < 17 ? new Class<?>[0] : new Class<?>[] { entityClass });
                packetTeam = getNMSClass("network.protocol.game", "PacketPlayOutScoreboardTeam");

                blockPositionConstructor = getNMSClass("core", "BlockPosition").getConstructor(double.class, double.class, double.class);

                nmsWorld = Class.forName(cpack + "CraftWorld").getDeclaredMethod("getHandle").invoke(Bukkit.getWorlds().get(0));
                Object[] entityConstructorParams = version < 14 ? new Object[] { nmsWorld } : new Object[] { getNMSClass("world.entity", "EntityTypes").getDeclaredField(version < 17 ? "SQUID" : "aJ").get(null), nmsWorld };
                fakeSquid = getNMSClass("world.entity.animal", "EntitySquid").getDeclaredConstructors()[0].newInstance(entityConstructorParams);
                fakeSquidWatcher = createFakeDataWatcher();
                tryWatcherSet(fakeSquidWatcher, watcherObject1, (byte) 32);

                getHandle = Class.forName(cpack + "entity.CraftPlayer").getDeclaredMethod("getHandle");
                playerConnection = getNMSClass("server.level", "EntityPlayer").getDeclaredField(version < 17 ? "playerConnection" : "b");
                sendPacket = getNMSClass("server.network", "PlayerConnection").getMethod(version < 18 ? "sendPacket" : "a", getNMSClass("network.protocol", "Packet"));

                if (version >= 17) {
                    setLocation = entityClass.getDeclaredMethod(version < 18 ? "setLocation" : "a", double.class, double.class, double.class, float.class, float.class);

                    createTeamPacket = packetTeam.getMethod("a", getNMSClass("world.scores", "ScoreboardTeam"), boolean.class);

                    Class<?> scoreboardClass = getNMSClass("world.scores", "Scoreboard");
                    Class<?> teamClass = getNMSClass("world.scores", "ScoreboardTeam");
                    Class<?> pushClass = getNMSClass("world.scores", "ScoreboardTeamBase$EnumTeamPush");
                    createTeam = teamClass.getDeclaredConstructor(scoreboardClass, String.class);
                    createScoreboard = scoreboardClass.getDeclaredConstructor();
                    setTeamPush = teamClass.getDeclaredMethod(mappings.getTeamSetCollision(), pushClass);
                    pushNever = pushClass.getDeclaredField("b").get(null);
                    getTeamPlayers = teamClass.getDeclaredMethod(mappings.getTeamGetPlayers());
                }

                enabled = true;
            }catch (ReflectiveOperationException e) {
                e.printStackTrace();
                String errorMsg = "Laser Beam reflection failed to initialize. The util is disabled. Please ensure your version (" + Bukkit.getServer().getClass().getPackage().getName() + ") is supported.";
                if (logger == null)
                    System.err.println(errorMsg);
                else
                    logger.severe(errorMsg);
            }
        }

        public static void sendPackets(Player p, Object... packets) throws ReflectiveOperationException {
            Object connection = playerConnection.get(getHandle.invoke(p));
            for (Object packet : packets) {
                sendPacket.invoke(connection, packet);
            }
        }

        public static Object createFakeDataWatcher() throws ReflectiveOperationException {
            Object watcher = watcherConstructor.newInstance(fakeSquid);
            if (version > 13) setField(watcher, "registrationLocked", false);
            return watcher;
        }

        public static void setDirtyWatcher(Object watcher) throws ReflectiveOperationException {
            if (version >= 15) watcherDirty.invoke(watcher, watcherObject1);
        }

        public static Object createSquid(Location location) throws ReflectiveOperationException {
            Object entity = getNMSClass("world.entity.animal", "EntitySquid").getDeclaredConstructors()[0].newInstance(squidType, nmsWorld);
            moveFakeEntity(entity, location);
            return entity;
        }

        public static Object createGuardian(Location location) throws ReflectiveOperationException {
            Object entity = getNMSClass("world.entity.monster", "EntityGuardian").getDeclaredConstructors()[0].newInstance(guardianType, nmsWorld);
            moveFakeEntity(entity, location);
            return entity;
        }

        public static Object createCrystal(Location location) throws ReflectiveOperationException {
            return getNMSClass("world.entity.boss.enderdragon", "EntityEnderCrystal").getDeclaredConstructor(nmsWorld.getClass().getSuperclass(), double.class, double.class, double.class).newInstance(nmsWorld, location.getX(), location.getY(), location.getZ());
        }

        public static Object createPacketEntitySpawnLiving(Location location, int typeID) throws ReflectiveOperationException {
            Object packet = packetSpawnLiving.newInstance();
            setField(packet, "a", generateEID());
            setField(packet, "b", UUID.randomUUID());
            setField(packet, "c", typeID);
            setField(packet, "d", location.getX());
            setField(packet, "e", location.getY());
            setField(packet, "f", location.getZ());
            setField(packet, "j", (byte) (location.getYaw() * 256.0F / 360.0F));
            setField(packet, "k", (byte) (location.getPitch() * 256.0F / 360.0F));
            if (version <= 14) setField(packet, "m", fakeSquidWatcher);
            return packet;
        }

        public static Object createPacketEntitySpawnNormal(Location location, int typeID, Object type) throws ReflectiveOperationException {
            Object packet = packetSpawnNormal.newInstance();
            setField(packet, "a", generateEID());
            setField(packet, "b", UUID.randomUUID());
            setField(packet, "c", location.getX());
            setField(packet, "d", location.getY());
            setField(packet, "e", location.getZ());
            setField(packet, "i", (int) (location.getYaw() * 256.0F / 360.0F));
            setField(packet, "j", (int) (location.getPitch() * 256.0F / 360.0F));
            setField(packet, "k", version < 13 ? typeID : type);
            return packet;
        }

        public static Object createPacketEntitySpawnLiving(Object entity) throws ReflectiveOperationException {
            return packetSpawnLiving.newInstance(entity);
        }

        public static Object createPacketEntitySpawnNormal(Object entity) throws ReflectiveOperationException {
            return packetSpawnNormal.newInstance(entity);
        }

        public static void initGuardianWatcher(Object watcher, int squidId) throws ReflectiveOperationException {
            tryWatcherSet(watcher, watcherObject1, (byte) 32);
            tryWatcherSet(watcher, watcherObject2, false);
            tryWatcherSet(watcher, watcherObject3, squidId);
        }

        public static void setCrystalWatcher(Object watcher, Location target) throws ReflectiveOperationException {
            Object blockPosition = blockPositionConstructor.newInstance(target.getX(), target.getY(), target.getZ());
            tryWatcherSet(watcher, watcherObject4, version < 13 ? com.google.common.base.Optional.of(blockPosition) : Optional.of(blockPosition));
        }

        public static Object[] createPacketsRemoveEntities(int... entitiesId) throws ReflectiveOperationException {
            Object[] packets;
            if (version == 17 && versionMinor == 0) {
                packets = new Object[entitiesId.length];
                for (int i = 0; i < entitiesId.length; i++) {
                    packets[i] = packetRemove.newInstance(entitiesId[i]);
                }
            }else {
                packets = new Object[] { packetRemove.newInstance(entitiesId) };
            }
            return packets;
        }

        public static Object createPacketMoveEntity(Location location, int entityId) throws ReflectiveOperationException {
            Object packet = packetTeleport.newInstance();
            setField(packet, "a", entityId);
            setField(packet, "b", location.getX());
            setField(packet, "c", location.getY());
            setField(packet, "d", location.getZ());
            setField(packet, "e", (byte) (location.getYaw() * 256.0F / 360.0F));
            setField(packet, "f", (byte) (location.getPitch() * 256.0F / 360.0F));
            setField(packet, "g", true);
            return packet;
        }

        public static void moveFakeEntity(Object entity, Location location) throws ReflectiveOperationException {
            setLocation.invoke(entity, location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw());
        }

        public static Object createPacketMoveEntity(Object entity) throws ReflectiveOperationException {
            return packetTeleport.newInstance(entity);
        }

        public static Object createPacketTeamCreate(String teamName, UUID squidUUID, UUID guardianUUID) throws ReflectiveOperationException {
            Object packet;
            if (version < 17) {
                packet = packetTeam.newInstance();
                setField(packet, "a", teamName);
                setField(packet, "i", 0);
                setField(packet, "f", "never");
                Collection<String> players = (Collection<String>) getField(packetTeam, "h", packet);
                players.add(squidUUID.toString());
                players.add(guardianUUID.toString());
            }else {
                Object team = createTeam.newInstance(createScoreboard.newInstance(), teamName);
                setTeamPush.invoke(team, pushNever);
                Collection<String> players = (Collection<String>) getTeamPlayers.invoke(team);
                players.add(squidUUID.toString());
                players.add(guardianUUID.toString());
                packet = createTeamPacket.invoke(null, team, true);
            }
            return packet;
        }

        private static Object createPacketMetadata(int entityId, Object watcher) throws ReflectiveOperationException {
            return packetMetadata.newInstance(entityId, watcher, false);
        }

        private static void tryWatcherSet(Object watcher, Object watcherObject, Object watcherData) throws ReflectiveOperationException {
            try {
                watcherSet.invoke(watcher, watcherObject, watcherData);
            }catch (InvocationTargetException ex) {
                watcherRegister.invoke(watcher, watcherObject, watcherData);
                if (version >= 15) watcherDirty.invoke(watcher, watcherObject);
            }
        }

        /* Reflection utils */
        private static Method getMethod(Class<?> clazz, String name) throws NoSuchMethodException {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(name)) return m;
            }
            throw new NoSuchMethodException(name + " in " + clazz.getName());
        }

        private static void setField(Object instance, String name, Object value) throws ReflectiveOperationException {
            Validate.notNull(instance);
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(instance, value);
        }

        private static Object getField(Class<?> clazz, String name, Object instance) throws ReflectiveOperationException {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        }

        private static Object getField(Object instance, String name) throws ReflectiveOperationException {
            Field field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        }

        private static Class<?> getNMSClass(String package17, String className) throws ClassNotFoundException {
            return Class.forName((version < 17 ? npack : "net.minecraft." + package17) + "." + className);
        }
    }

    @FunctionalInterface
    public static interface ReflectiveConsumer<T> {
        abstract void accept(T t) throws ReflectiveOperationException;
    }

    enum ProtocolMappings {
        V1_9(9, "Z", "bA", "bB", "b", 94, 68),
        V1_10(10, V1_9),
        V1_11(11, V1_10),
        V1_12(12, V1_11),
        V1_13(13, "ac", "bF", "bG", "b", 70, 28),
        V1_14(14, "W", "b", "bD", "c", 73, 30),
        V1_15(15, "T", "b", "bA", "c", 74, 31),
        V1_16(16, "T", "b", "d", "c", 74, 31, "u", null, null){
            @Override
            public int getSquidID() {
                return Packets.versionMinor < 2 ? super.getSquidID() : 81;
            }

            @Override
            public String getWatcherFlags() {
                return Packets.versionMinor < 2 ? super.getWatcherFlags() : "S";
            }
        },
        V1_17(17, "Z", "b", "e", "c", 86, 35, "u", "setCollisionRule", "getPlayerNameSet"),
        V1_18(18, "aa", "b", "e", "c", 86, 35, "u", "a", "g"),
        ;

        private final int major;
        private final String watcherFlags;
        private final String watcherSpikes;
        private final String watcherTargetEntity;
        private final String watcherTargetLocation;
        private final int squidID;
        private final int guardianID;
        private final String crystalTypeName;
        private String teamSetCollision;
        private String teamGetPlayers;

        private ProtocolMappings(int major, ProtocolMappings parent) {
            this(major, parent.watcherFlags, parent.watcherSpikes, parent.watcherTargetEntity, parent.watcherTargetLocation, parent.squidID, parent.guardianID, parent.crystalTypeName, parent.teamSetCollision, parent.teamGetPlayers);
        }

        private ProtocolMappings(int major,
                                 String watcherFlags, String watcherSpikes, String watcherTargetEntity, String watcherTargetLocation,
                                 int squidID, int guardianID) {
            this(major, watcherFlags, watcherSpikes, watcherTargetEntity, watcherTargetLocation, squidID, guardianID, "END_CRYSTAL", null, null);
        }

        private ProtocolMappings(int major,
                                 String watcherFlags, String watcherSpikes, String watcherTargetEntity, String watcherTargetLocation,
                                 int squidID, int guardianID,
                                 String crystalTypeName, String teamSetCollision, String teamGetPlayers) {
            this.major = major;
            this.watcherFlags = watcherFlags;
            this.watcherSpikes = watcherSpikes;
            this.watcherTargetEntity = watcherTargetEntity;
            this.watcherTargetLocation = watcherTargetLocation;
            this.squidID = squidID;
            this.guardianID = guardianID;
            this.crystalTypeName = crystalTypeName;
            this.teamSetCollision = teamSetCollision;
            this.teamGetPlayers = teamGetPlayers;
        }

        public int getMajor() {
            return major;
        }

        public String getWatcherFlags() {
            return watcherFlags;
        }

        public String getWatcherSpikes() {
            return watcherSpikes;
        }

        public String getWatcherTargetEntity() {
            return watcherTargetEntity;
        }

        public String getWatcherTargetLocation() {
            return watcherTargetLocation;
        }

        public int getSquidID() {
            return squidID;
        }

        public int getGuardianID() {
            return guardianID;
        }

        public String getCrystalTypeName() {
            return crystalTypeName;
        }

        public String getTeamSetCollision() {
            return teamSetCollision;
        }

        public String getTeamGetPlayers() {
            return teamGetPlayers;
        }

        public static ProtocolMappings getMappings(int major) {
            for (ProtocolMappings map : values()) {
                if (major == map.getMajor()) return map;
            }
            return null;
        }

    }
}