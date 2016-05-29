package eu.thog.uhcrun.game;

import com.connorlinfoot.titleapi.TitleAPI;
import eu.thog.uhcrun.UHCRun;
import eu.thog.uhcrun.compatibility.*;
import eu.thog.uhcrun.listener.ChunkListener;
import eu.thog.uhcrun.task.GameLoop;
import eu.thog.uhcrun.utils.Colors;
import eu.thog.uhcrun.utils.Metadatas;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.SpawnerCreature;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public abstract class AbstractGame
{
    protected final UHCRun plugin;
    protected final GameAdaptator adaptator;
    protected final Server server;
    protected final List<Location> spawnPoints;
    protected final HashMap<UUID, UHCPlayer> gamePlayers;
    private final int preparingTime;
    private final int deathMatchSize;
    private final int reductionTime;
    private final GameProperties properties;
    private GameLoop gameLoop;
    private Scoreboard scoreboard;
    private boolean pvpEnabled;
    private BukkitTask mainTask;
    private boolean damages;
    private Status status;
    private BukkitTask beginTimer;
    private MessageManager messageManager;

    public AbstractGame(UHCRun plugin, GameProperties properties)
    {
        this.status = Status.STARTING;
        this.plugin = plugin;
        this.adaptator = plugin.getAdaptator();
        this.server = plugin.getServer();
        this.spawnPoints = new ArrayList<>();
        this.gamePlayers = new HashMap<>();
        this.beginTimer = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new eu.thog.uhcrun.compatibility.TimerTask(this), 20L, 20L);
        this.messageManager = new MessageManager(this);
        UHCPlayer.setGame(this);

        this.properties = properties;
        this.preparingTime = ((Double) plugin.getProperties().getOption("preparingTime", 20D)).intValue();
        this.deathMatchSize = ((Double) plugin.getProperties().getOption("deathMatchSize", 400D)).intValue();
        this.reductionTime = ((Double) plugin.getProperties().getOption("reductionTime", 10D)).intValue();
    }

    public void postInit(World world)
    {
        this.scoreboard = server.getScoreboardManager().getMainScoreboard();
        this.gameLoop = new GameLoop(this, plugin, server);
        this.disableDamages();
        this.computeLocations(world);
    }

    private void computeLocations(World world)
    {
        spawnPoints.add(new Location(world, 0, 150, 200));
        spawnPoints.add(new Location(world, 0, 150, 400));
        spawnPoints.add(new Location(world, 200, 150, 0));
        spawnPoints.add(new Location(world, 400, 150, 0));
        spawnPoints.add(new Location(world, 400, 150, 200));
        spawnPoints.add(new Location(world, 200, 150, 400));
        spawnPoints.add(new Location(world, 400, 150, 400));
        spawnPoints.add(new Location(world, 200, 150, 200));
        spawnPoints.add(new Location(world, 0, 150, -200));
        spawnPoints.add(new Location(world, 0, 150, -400));
        spawnPoints.add(new Location(world, -200, 150, 0));
        spawnPoints.add(new Location(world, -400, 150, 0));
        spawnPoints.add(new Location(world, -400, 150, -200));
        spawnPoints.add(new Location(world, -200, 150, -400));
        spawnPoints.add(new Location(world, -400, 150, -400));
        spawnPoints.add(new Location(world, -200, 150, -200));
        spawnPoints.add(new Location(world, 400, 150, -200));
        spawnPoints.add(new Location(world, -400, 150, 200));
        spawnPoints.add(new Location(world, 200, 150, -400));
        spawnPoints.add(new Location(world, -200, 150, 400));
        spawnPoints.add(new Location(world, -400, 150, 400));
        spawnPoints.add(new Location(world, 400, 150, -400));
        spawnPoints.add(new Location(world, 200, 150, -200));
        Collections.shuffle(spawnPoints);
    }

    protected void removeFromGame(UUID uuid)
    {
        UHCPlayer player = this.gamePlayers.get(uuid);
        if (player != null)
        {
            player.setSpectator();
        }
    }

    public UHCRun getPlugin()
    {
        return plugin;
    }

    public void startGame()
    {
        setStatus(Status.IN_GAME);
        this.beginTimer.cancel();
        adaptator.removeSpawn();

        Objective displayNameLife = scoreboard.getObjective("vie") == null ? scoreboard.registerNewObjective("vie", "health") : scoreboard.getObjective("vie");
        Objective playerListLife = scoreboard.getObjective("vieb") == null ? scoreboard.registerNewObjective("vieb", "health") : scoreboard.getObjective("vieb");

        playerListLife.setDisplayName(ChatColor.RED + "♥");
        displayNameLife.setDisplayName("HP");
        displayNameLife.setDisplaySlot(DisplaySlot.BELOW_NAME);
        playerListLife.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        this.mainTask = server.getScheduler().runTaskTimer(plugin, gameLoop, 20, 20);
        teleport();

        for (UUID uuid : getOnlineInGamePlayers().keySet())
        {
            Player player = server.getPlayer(uuid);
            if (player == null)
            {
                gamePlayers.remove(uuid);
                continue;
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.setFoodLevel(20);
            player.setScoreboard(scoreboard);
            displayNameLife.getScore(player.getName()).setScore(20);
            playerListLife.getScore(player.getName()).setScore(20);
            player.setLevel(0);
            player.getInventory().clear();
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 24000, 0));
            ObjectiveSign sign = new ObjectiveSign("gameside", ChatColor.GOLD + "" + ChatColor.ITALIC + ChatColor.BOLD + "= UHCRun =", player);
            gameLoop.addPlayer(player.getUniqueId(), sign);
        }

        server.getPluginManager().registerEvents(new ChunkListener(plugin), plugin);
        SpawnerCreature spawner = new SpawnerCreature();
        for (int i = 0; i < 3; i++)
        {
            spawner.a(MinecraftServer.getServer().getWorldServer(0), false, true, true); // Force spawning of passive annimals
        }
    }

    public void rejoin(Player thePlayer)
    {
        if (thePlayer != null)
        {
            server.getScheduler().runTaskLater(plugin, () -> {

                thePlayer.setScoreboard(this.scoreboard);
                ObjectiveSign sign = new ObjectiveSign("gameside", ChatColor.GOLD + "" + ChatColor.ITALIC + ChatColor.BOLD + "= UHCRun =", thePlayer);
                this.gameLoop.addPlayer(thePlayer.getUniqueId(), sign);
            }, 10L);

        }
    }

    public void stumpPlayer(Player player, boolean logout)
    {
        if (this.status == Status.IN_GAME)
        {
            Object lastDamager = Metadatas.getMetadata(plugin, player, "lastDamager");
            Player killer = null;
            if (lastDamager != null && lastDamager instanceof Player)
            {
                killer = (Player) lastDamager;
                if (killer.isOnline() && this.isInGame(killer.getUniqueId()))
                {
                    this.getPlayer(killer.getUniqueId()).addKill();
                    killer.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 400, 1));
                } else
                {
                    killer = null;
                }
            }

            if (logout)
            {
                messageManager.writeReconnectRemainTimeout(player);
            } else if (killer != null)
            {
                server.broadcastMessage(this.getTag() + " " + player.getDisplayName() + ChatColor.GOLD + " a été tué par " + killer.getDisplayName());
            } else
            {
                server.broadcastMessage(this.getTag() + " " + player.getDisplayName() + ChatColor.GOLD + " est mort.");
            }

            this.checkStump(player);
            removeFromGame(player.getUniqueId());
            if (!logout)
            {
                TitleAPI.sendTitle(player, 5, 70, 5, ChatColor.RED + "Vous êtes mort !", ChatColor.GOLD + "Vous êtes maintenant spectateur.");
                player.setGameMode(GameMode.SPECTATOR);
                player.setHealth(20.0D);
            }
        }
    }

    public boolean isInGame(UUID uniqueId)
    {
        return this.gamePlayers.containsKey(uniqueId) && !this.gamePlayers.get(uniqueId).isSpectator();
    }

    public abstract void checkStump(Player player);

    protected abstract void teleport();

    public abstract void teleportDeathMatch();

    public int getPreparingTime()
    {
        return preparingTime;
    }

    public int getDeathMatchSize()
    {
        return deathMatchSize;
    }

    public int getReductionTime()
    {
        return reductionTime;
    }


    public void enablePVP()
    {
        this.pvpEnabled = true;
    }

    public boolean isPvpEnabled()
    {
        return pvpEnabled;
    }

    public void enableDamages()
    {
        this.damages = true;
    }

    public void disableDamages()
    {
        this.damages = false;
    }


    public boolean isDamagesEnabled()
    {
        return damages;
    }

    public GameLoop getGameLoop()
    {
        return gameLoop;
    }

    public void effectsOnWinner(Player player)
    {
        server.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
        {
            int timer = 0;

            @Override
            public void run()
            {
                if (this.timer < 20)
                {
                    Firework fw = (Firework) player.getWorld().spawnEntity(player.getPlayer().getLocation(), EntityType.FIREWORK);
                    FireworkMeta fwm = fw.getFireworkMeta();
                    Random r = new Random();
                    int rt = r.nextInt(4) + 1;
                    FireworkEffect.Type type = FireworkEffect.Type.BALL;
                    if (rt == 1)
                    {
                        type = FireworkEffect.Type.BALL;
                    }

                    if (rt == 2)
                    {
                        type = FireworkEffect.Type.BALL_LARGE;
                    }

                    if (rt == 3)
                    {
                        type = FireworkEffect.Type.BURST;
                    }

                    if (rt == 4)
                    {
                        type = FireworkEffect.Type.CREEPER;
                    }

                    if (rt == 5)
                    {
                        type = FireworkEffect.Type.STAR;
                    }

                    int r1i = r.nextInt(15) + 1;
                    int r2i = r.nextInt(15) + 1;
                    Color c1 = Colors.getColor(r1i);
                    Color c2 = Colors.getColor(r2i);
                    FireworkEffect effect = FireworkEffect.builder().flicker(r.nextBoolean()).withColor(c1).withFade(c2).with(type).trail(r.nextBoolean()).build();
                    fwm.addEffect(effect);
                    int rp = r.nextInt(2) + 1;
                    fwm.setPower(rp);
                    fw.setFireworkMeta(fwm);
                    this.timer++;
                }
            }
        }, 5L, 5L);
    }

    public void handleGameEnd()
    {
        this.interrupt();
    }

    public void interrupt()
    {
        this.mainTask.cancel();
        server.getScheduler().runTaskLater(plugin, server::shutdown, 200L);
    }

    public GameProperties getGameProperties()
    {
        return properties;
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public UHCPlayer getPlayer(UUID player)
    {
        return this.gamePlayers.get(player);
    }

    public Map<UUID, UHCPlayer> getOnlineInGamePlayers()
    {
        Map<UUID, UHCPlayer> result = new HashMap<>();

        for (UUID uuid : this.gamePlayers.keySet())
        {
            final UHCPlayer uhcPlayer = this.gamePlayers.get(uuid);
            if (!uhcPlayer.isSpectator())
            {
                result.put(uuid, uhcPlayer);
            }
        }
        return result;
    }

    public String getTag()
    {
        return ChatColor.DARK_AQUA + "[" + ChatColor.AQUA + "UHCRun" + ChatColor.DARK_AQUA + "]" + ChatColor.RESET;
    }

    public boolean isGameStarted()
    {
        return this.status == Status.IN_GAME;
    }

    public BukkitTask getBeginTimer()
    {
        return beginTimer;
    }

    public MessageManager getMessageManager()
    {
        return messageManager;
    }

    public void handleJoin(PlayerJoinEvent event)
    {
        if (status == Status.WAITING_FOR_PLAYERS)
        {
            UHCPlayer player = new UHCPlayer(event.getPlayer());
            this.gamePlayers.put(player.getUUID(), player);
            player.handleLogin(event.getPlayer(), false);
        } else if (this.isInGame(event.getPlayer().getUniqueId()))
        {
            this.getPlayer(event.getPlayer().getUniqueId()).handleLogin(event.getPlayer(), true);
        }
    }

    public void handleLogin(PlayerLoginEvent event)
    {
        if (status == Status.WAITING_FOR_PLAYERS || this.isInGame(event.getPlayer().getUniqueId()))
        {
            event.allow();
        } else
        {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "La partie est en cours!");
        }
    }

    public void handleLogout(PlayerQuitEvent event)
    {
        UHCPlayer uhcPlayer = getPlayer(event.getPlayer().getUniqueId());
        if (uhcPlayer != null)
        {
            uhcPlayer.handleLogout();
        }

        if (status != Status.IN_GAME)
        {
            this.gamePlayers.remove(event.getPlayer().getUniqueId());
        }
    }
}