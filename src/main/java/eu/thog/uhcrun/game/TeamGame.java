package eu.thog.uhcrun.game;

import com.connorlinfoot.titleapi.TitleAPI;
import eu.thog.uhcrun.UHCRun;
import eu.thog.uhcrun.compatibility.Status;
import eu.thog.uhcrun.game.team.Team;
import eu.thog.uhcrun.game.team.TeamList;
import eu.thog.uhcrun.game.team.TeamSelector;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class TeamGame extends AbstractGame
{

    private final int personsPerTeam;
    private TeamList teams = new TeamList();
    private TeamSelector teamSelector;


    public TeamGame(UHCRun plugin, int nbByTeam)
    {
        super(plugin, plugin.getProperties());
        this.personsPerTeam = nbByTeam;
        try
        {
            this.teamSelector = new TeamSelector(this);
        } catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }

        plugin.getServer().getPluginManager().registerEvents(teamSelector, plugin);

        // Register team
        registerTeam("Blanc", ChatColor.WHITE, DyeColor.WHITE);
        registerTeam("Orange", ChatColor.GOLD, DyeColor.ORANGE);
        registerTeam("Bleu Clair", ChatColor.BLUE, DyeColor.LIGHT_BLUE);
        registerTeam("Bleu Foncé", ChatColor.DARK_BLUE, DyeColor.BLUE);
        registerTeam("Cyan", ChatColor.AQUA, DyeColor.CYAN);
        registerTeam("Jaune", ChatColor.YELLOW, DyeColor.YELLOW);
        registerTeam("Rose", ChatColor.LIGHT_PURPLE, DyeColor.PINK);
        registerTeam("Vert Foncé", ChatColor.DARK_GREEN, DyeColor.GREEN);
        registerTeam("Rouge", ChatColor.RED, DyeColor.RED);
        registerTeam("Violet", ChatColor.DARK_PURPLE, DyeColor.PURPLE);
        registerTeam("Gris", ChatColor.GRAY, DyeColor.GRAY);
        registerTeam("Noir", ChatColor.BLACK, DyeColor.BLACK);
    }

    protected void registerTeam(String name, ChatColor chatColor, DyeColor color)
    {
        teams.add(new Team(this, name, color, chatColor));
    }

    @Override
    public void startGame()
    {
        if (this.getOnlineInGamePlayers().size() <= personsPerTeam)
        {
            return;
        }
        for (UUID id : this.getOnlineInGamePlayers().keySet())
        {
            Player player = Bukkit.getPlayer(id);
            if (player == null)
            {
                continue;
            }

            if (getPlayerTeam(id) == null)
            {
                for (Team team : teams)
                {
                    if (!team.isFull() && !team.isLocked())
                    {
                        team.join(id);
                        break;
                    }
                }

                if (getPlayerTeam(id) == null)
                {
                    player.kickPlayer(ChatColor.RED + "Aucune team était apte à vous reçevoir, vous avez été réenvoyé dans le lobby.");
                }
            }
        }
        super.startGame();
    }

    @Override
    protected void teleport()
    {
        Iterator<Location> locationIterator = spawnPoints.iterator();
        List<Team> toRemove = new ArrayList<>();

        for (Team team : teams)
        {
            if (!locationIterator.hasNext() || team.isEmpty())
            {
                toRemove.add(team);
                for (UUID player : team.getPlayersUUID())
                {
                    Player p = server.getPlayer(player);
                    if (p != null)
                    {
                        p.kickPlayer(ChatColor.RED + "Plus de place dans la partie.");
                    }
                    gamePlayers.remove(player);
                }
                continue;
            }

            Location location = locationIterator.next();

            for (UUID player : team.getPlayersUUID())
            {
                Player p = server.getPlayer(player);
                if (p == null)
                {
                    gamePlayers.remove(player);
                } else
                {
                    p.teleport(location);
                }
            }
        }
        teams.removeAll(toRemove);
        toRemove.clear();
    }

    @Override
    public void teleportDeathMatch()
    {
        Iterator<Location> locationIterator = spawnPoints.iterator();
        List<Team> toRemove = new ArrayList<>();

        for (Team team : teams)
        {
            if (!locationIterator.hasNext())
            {
                toRemove.add(team);
                for (UUID player : team.getPlayersUUID())
                {
                    Player p = server.getPlayer(player);
                    if (p != null)
                    {
                        p.kickPlayer(ChatColor.RED + "Plus de place dans la partie.");
                    }
                    gamePlayers.remove(player);
                }
                continue;
            }

            Location location = locationIterator.next();

            for (UUID player : team.getPlayersUUID())
            {
                Player p = server.getPlayer(player);
                if (p == null)
                {
                    gamePlayers.remove(player);
                } else
                {
                    p.teleport(new Location(location.getWorld(), location.getX() * 4 / 10, 150.0, location.getZ() * 4 / 10));
                }
            }
        }
        teams.removeAll(toRemove);
        toRemove.clear();
    }

    @Override
    public void checkStump(final Player player)
    {
        server.getScheduler().runTaskLater(plugin, () -> {
            List<Team> toRemove = new ArrayList<>();
            Team team = teams.getTeam(player.getUniqueId());
            if (team == null)
            {
                return;
            }

            int left = team.removePlayer(player.getUniqueId());
            if (left == 0)
            {
                server.broadcastMessage(ChatColor.GOLD + "L'équipe " + team.getChatColor() + team.getTeamName() + ChatColor.GOLD + " a été éliminée !");
                teams.remove(team);

                left = teams.size();
                if (left == 1)
                {
                    win(teams.get(0));
                    return;
                } else if (left < 1)
                {
                    handleGameEnd();
                    return;
                } else
                {
                    server.broadcastMessage(ChatColor.YELLOW + "Il reste encore " + ChatColor.AQUA + teams.size() + ChatColor.YELLOW + " équipes en jeu.");
                }
            }

            // Security check
            for (Team t : teams)
            {
                int players1 = 0;
                if (!t.isEmpty())
                {
                    for (UUID id : t.getPlayersUUID())
                    {
                        if (server.getPlayer(id) != null)
                        {
                            players1++;
                        }
                    }
                }

                if (players1 == 0)
                {
                    server.broadcastMessage(ChatColor.GOLD + "L'équipe " + t.getChatColor() + t.getTeamName() + ChatColor.GOLD + " a été éliminée !");
                    toRemove.add(t);

                    left = teams.size();
                    if (left == 2)
                    {
                        win(teams.get(0));
                    } else if (left < 2)
                    {
                        handleGameEnd();
                    } else
                    {
                        server.broadcastMessage(ChatColor.YELLOW + "Il reste encore " + ChatColor.AQUA + teams.size() + ChatColor.YELLOW + " équipes en jeu.");
                    }
                }
            }
            teams.removeAll(toRemove);
        }, 2L);
    }


    private void win(final Team team)
    {
        try
        {
            server.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Victoire de l'équipe " + team.getChatColor() + team.getTeamName() + ChatColor.GOLD + "" + ChatColor.BOLD + " !");
            for (Player user : server.getOnlinePlayers())
            {
                TitleAPI.sendTitle(user, 5, 70, 5, ChatColor.GOLD + "Victoire !", "Bravo à l'équipe " + team.getChatColor() + team.getTeamName());
            }
        } catch (Exception ignored)
        {

        }


        for (final UUID playerID : team.getPlayersUUID())
        {
            final Player player = server.getPlayer(playerID);
            if (player == null)
            {
                continue;
            }

            this.effectsOnWinner(player);
        }

        handleGameEnd();
    }

    @Override
    public void stumpPlayer(Player player, boolean logout)
    {
        if (logout && !this.getStatus().equals(Status.IN_GAME))
        {
            Team team = teams.getTeam(player.getUniqueId());
            if (team != null)
            {
                team.remove(player.getUniqueId());
            }

        }
        super.stumpPlayer(player, logout);
    }

    public Team getPlayerTeam(UUID uniqueId)
    {
        return teams.getTeam(uniqueId);
    }

    public TeamList getTeams()
    {
        return teams;
    }

    public int getPersonsPerTeam()
    {
        return personsPerTeam;
    }
}
