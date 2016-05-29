package eu.thog.uhcrun.compatibility;

import eu.thog.uhcrun.game.AbstractGame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public final class MessageManager
{
    private final AbstractGame game;

    public MessageManager(AbstractGame game)
    {
        this.game = game;
    }

    public void writeGameStartTime(int time)
    {
        Bukkit.broadcastMessage(game.getTag() + ChatColor.YELLOW + " Début de la partie dans " + ChatColor.RED + time + " secondes" + ChatColor.YELLOW + ".");
    }

    public void writeNotEnougthPlayersToStart()
    {
        Bukkit.broadcastMessage(game.getTag() + ChatColor.RED + " Il n'y a plus assez de joueurs pour commencer.");
    }

    public String getPreStartSentence()
    {
        return "Préparez-vous à jouer !";
    }

    public void writeReconnectRemainTimeout(Player player)
    {
        Bukkit.broadcastMessage(game.getTag() + ChatColor.RED + player.getName() + " à été exclu de la partie (Temps de reconnection écoulé)");
    }

    public void writePlayerJoinToAll(Player player)
    {
        Bukkit.broadcastMessage(game.getTag() + " " + ChatColor.YELLOW + player.getName() + " a rejoint la partie ! " + ChatColor.DARK_GRAY + "[" + ChatColor.RED + game.getOnlineInGamePlayers().size() + ChatColor.DARK_GRAY + "/" + ChatColor.RED + game.getGameProperties().getMaxSlots() + ChatColor.DARK_GRAY + "]");
    }
}
