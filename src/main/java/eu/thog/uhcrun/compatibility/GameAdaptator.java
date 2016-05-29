package eu.thog.uhcrun.compatibility;

import com.connorlinfoot.actionbarapi.ActionBarAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import eu.thog.uhcrun.listener.*;
import eu.thog.uhcrun.utils.Reflection;
import eu.thog.uhcrun.UHCRun;
import eu.thog.uhcrun.commands.CommandNextEvent;
import eu.thog.uhcrun.commands.CommandStart;
import eu.thog.uhcrun.game.AbstractGame;
import eu.thog.uhcrun.game.SoloGame;
import eu.thog.uhcrun.game.TeamGame;
import eu.thog.uhcrun.generator.LobbyPopulator;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;

/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class GameAdaptator implements Listener
{
    private final UHCRun plugin;
    private LobbyPopulator loobyPopulator;
    private AbstractGame game;
    private PluginManager pluginManager;

    public GameAdaptator(UHCRun plugin)
    {
        this.plugin = plugin;
        this.pluginManager = Bukkit.getPluginManager();
    }

    public void onEnable()
    {
        int nb = ((Double) plugin.getProperties().getOption("playersPerTeam", 1D)).intValue();

        if (nb > 1)
        {
            this.game = new TeamGame(plugin, nb);
        } else
        {
            this.game = new SoloGame(plugin);
        }

        pluginManager.registerEvents(new SpectatorListener(game), plugin);
        pluginManager.registerEvents(new GameListener(game), plugin);
        pluginManager.registerEvents(new CompassTargeter(this), plugin);
        pluginManager.registerEvents(new StackListener(2), plugin);
        pluginManager.registerEvents(this, plugin);

        plugin.getCommand("startgame").setExecutor(new CommandStart());
        plugin.getCommand("nextevent").setExecutor(new CommandNextEvent(game));
        ActionBarAPI.nmsver = Reflection.getVersion(); // Don't initialize ActionBarAPI so we setup the minimal requirement
    }

    public void postInit(World world)
    {
        JsonArray defaults = new JsonArray();
        defaults.add(new JsonPrimitive(6D));
        defaults.add(new JsonPrimitive(199D));
        defaults.add(new JsonPrimitive(7));
        JsonArray spawnPos = (JsonArray) plugin.getProperties().getOption("spawnPos", defaults);

        Location spawnLocation = new Location(world, spawnPos.get(0).getAsDouble(), spawnPos.get(1).getAsDouble(), spawnPos.get(2).getAsDouble());
        plugin.setSpawnLocation(spawnLocation);
        world.setSpawnLocation(spawnLocation.getBlockX(), spawnLocation.getBlockY(), spawnLocation.getBlockZ());

        game.setStatus(Status.STARTING);
        // Add the lobby
        loobyPopulator = new LobbyPopulator(plugin.getLogger(), plugin.getDataFolder());
        loobyPopulator.generate();
        pluginManager.registerEvents(new CraftListener(), plugin);
        pluginManager.registerEvents(new BlockListener(game), plugin);

        game.postInit(world);
    }

    @EventHandler
    public void onPreJoin(PlayerJoinEvent event)
    {
        if (game == null || game.getStatus() == Status.WAITING_FOR_PLAYERS)
        {
            event.getPlayer().setGameMode(GameMode.ADVENTURE);
            event.getPlayer().teleport(plugin.getSpawnLocation());
        }
    }

    public AbstractGame getGame()
    {
        return game;
    }

    public void loadEnd()
    {
        game.setStatus(Status.WAITING_FOR_PLAYERS);
    }

    public UHCRun getPlugin()
    {
        return plugin;
    }

    public void removeSpawn()
    {
        loobyPopulator.remove();
    }

}
