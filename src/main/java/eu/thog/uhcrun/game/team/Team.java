package eu.thog.uhcrun.game.team;

import mkremins.fanciful.FancyMessage;
import eu.thog.uhcrun.game.TeamGame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class Team
{

    private final CopyOnWriteArrayList<UUID> players;
    private final TeamGame game;
    private final ItemStack icon;
    private final DyeColor color;
    private final ChatColor chatColor;
    private int maxSize = -1;
    private String teamName;
    private List<UUID> invited = new ArrayList<>();
    private boolean locked;


    public Team(TeamGame game, String name, DyeColor color, ChatColor chatColor)
    {
        this.game = game;
        this.teamName = name;
        this.players = new CopyOnWriteArrayList<>();
        this.color = color;
        this.chatColor = chatColor;
        this.icon = new ItemStack(Material.WOOL, 1, color.getData());

        if (maxSize == -1)
        {
            maxSize = game.getPersonsPerTeam();
        }
    }

    public void join(UUID player)
    {
        Player newJoiner = Bukkit.getPlayer(player);

        if (newJoiner != null)
        {
            for (UUID member : players)
            {
                Player user = Bukkit.getPlayer(member);
                if (user != null)
                {
                    user.sendMessage(game.getTag() + " " + ChatColor.AQUA + newJoiner.getName() + ChatColor.YELLOW + " a rejoint l'équipe.");
                }

            }
        }

        this.players.add(player);
    }

    public boolean isLocked()
    {
        return locked;
    }

    public void setLocked(boolean locked)
    {
        this.locked = locked;
    }

    public void remove(UUID player)
    {
        players.remove(player);

        this.lockCheck();
    }

    public ItemStack getIcon()
    {
        return icon;
    }

    public void invite(String inviter, UUID invited)
    {
        this.invited.add(invited);

        new FancyMessage("Vous avez été invité dans l'équipe " + this.teamName + " par " + inviter + " ")
                .color(ChatColor.GOLD)
                .style(ChatColor.BOLD)
                .then("[Rejoindre]")
                .color(ChatColor.GREEN)
                .style(ChatColor.BOLD)
                .command("/uhc join " + this.chatColor.getChar())
                .send(Bukkit.getPlayer(invited));
    }

    public boolean hasPlayer(UUID player)
    {
        return players.contains(player);
    }

    public CopyOnWriteArrayList<UUID> getPlayersUUID()
    {
        return players;
    }

    public String getTeamName()
    {
        return teamName;
    }

    public void setTeamName(String teamName)
    {
        this.teamName = teamName;
    }

    public ChatColor getChatColor()
    {
        return chatColor;
    }

    public boolean isEmpty()
    {
        return players.isEmpty();
    }

    public int removePlayer(UUID uniqueId)
    {
        players.remove(uniqueId);
        this.lockCheck();
        return players.size();
    }

    private void lockCheck()
    {
        int size = players.size();
        if (size == 0 && isLocked())
        {
            this.setLocked(false);
        }

    }

    public boolean canJoin()
    {
        return !this.isLocked() && players.size() < maxSize;
    }

    public boolean isFull()
    {
        return players.size() == game.getPersonsPerTeam();
    }
}
