package eu.thog.uhcrun.game;

import com.connorlinfoot.titleapi.TitleAPI;
import eu.thog.uhcrun.UHCRun;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;


/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class SoloGame extends AbstractGame
{

    public SoloGame(UHCRun plugin)
    {
        super(plugin, plugin.getProperties());
    }

    @Override
    public void checkStump(Player player)
    {
        UHCPlayer playerData = getPlayer(player.getUniqueId());
        if (getOnlineInGamePlayers().size() == 2)
        {
            // HACK
            this.gamePlayers.remove(playerData.getUUID());
            UUID winnerId = getOnlineInGamePlayers().keySet().iterator().next();
            this.gamePlayers.put(playerData.getUUID(), playerData);

            Player winner = server.getPlayer(winnerId);
            if (winner == null)
            {
                this.handleGameEnd();
            } else
            {
                this.win(winner);
            }
        } else if (getOnlineInGamePlayers().size() == 1)
        {
            this.handleGameEnd();
        } else
        {
            server.broadcastMessage(ChatColor.YELLOW + "Il reste encore " + ChatColor.AQUA + (getOnlineInGamePlayers().size() - 1) + ChatColor.YELLOW + " joueur(s) en vie.");
        }

    }

    public void win(final Player player)
    {
        server.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Victoire de " + player.getDisplayName() + ChatColor.GOLD + "" + ChatColor.BOLD + " !");

        for (Player user : server.getOnlinePlayers())
        {
            TitleAPI.sendTitle(user, 5, 70, 5, ChatColor.GOLD + "Victoire de " + player.getDisplayName(), "");
        }

        this.effectsOnWinner(player);
        this.handleGameEnd();
    }

    @Override
    protected void teleport()
    {
        Iterator<Location> locationIterator = this.spawnPoints.iterator();

        for (UUID uuid : this.getOnlineInGamePlayers().keySet())
        {
            Player player = server.getPlayer(uuid);
            if (player == null)
            {
                gamePlayers.remove(uuid);
                continue;
            }

            if (!locationIterator.hasNext())
            {
                player.kickPlayer(ChatColor.RED + "Plus de place dans la partie.");
                gamePlayers.remove(uuid);
                continue;
            }

            Location location = locationIterator.next();
            player.teleport(location);
        }

    }

    @Override
    public void teleportDeathMatch()
    {
        Collections.shuffle(this.spawnPoints);
        Iterator<Location> locationIterator = this.spawnPoints.iterator();

        for (UUID uuid : this.getOnlineInGamePlayers().keySet())
        {
            Player player = server.getPlayer(uuid);
            if (player == null)
            {
                gamePlayers.remove(uuid);
                continue;
            }

            if (!locationIterator.hasNext())
            {
                player.kickPlayer(ChatColor.RED + "Plus de place dans la partie.");
                gamePlayers.remove(uuid);
                continue;
            }

            Location location = locationIterator.next();
            player.teleport(new Location(location.getWorld(), location.getX() * 4 / 10, 150.0, location.getZ() * 4 / 10));
        }
    }
}
