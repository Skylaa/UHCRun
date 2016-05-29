package eu.thog.uhcrun.game;

import eu.thog.uhcrun.compatibility.Status;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class UHCPlayer
{
    private static AbstractGame game;
    private final UUID uuid;
    private int kills;
    private boolean spectator;

    public UHCPlayer(Player player)
    {
        this.uuid = player.getUniqueId();
    }

    public static void setGame(AbstractGame game)
    {
        UHCPlayer.game = game;
    }

    public void handleLogin(Player player, boolean reconnect)
    {
        if (player == null)
        {
            return;
        }
        if (!reconnect)
        {
            player.setGameMode(GameMode.ADVENTURE);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.teleport(game.getPlugin().getSpawnLocation());
            player.getInventory().clear();
            player.getInventory().setHeldItemSlot(0);
            player.updateInventory();
            if (game instanceof TeamGame)
            {
                ItemStack star = new ItemStack(Material.NETHER_STAR);
                ItemMeta starMeta = star.getItemMeta();
                starMeta.setDisplayName(ChatColor.GOLD + "Sélectionner une équipe");
                star.setItemMeta(starMeta);
                player.getInventory().setItem(4, star);
                player.getInventory().setHeldItemSlot(4);
            }
        } else
        {
            game.rejoin(player);
        }

    }

    public void handleLogout()
    {
        if (!spectator)
        {
            Player player = getPlayerIfOnline();
            if (game.getStatus() == Status.IN_GAME)
            {
                game.getGameLoop().removePlayer(player.getUniqueId());
                if (game.isPvpEnabled())
                {
                    game.stumpPlayer(player, true);
                    Location time = player.getLocation();
                    World w = time.getWorld();
                    ItemStack[] var4 = player.getInventory().getContents();

                    for (ItemStack stack : var4)
                    {
                        if (stack != null)
                        {
                            w.dropItemNaturally(time, stack);
                        }
                    }
                }
            }
        }

    }

    public void addKill()
    {
        kills++;
    }

    public Player getPlayerIfOnline()
    {
        return Bukkit.getPlayer(this.uuid);
    }

    public int getKills()
    {
        return kills;
    }

    public boolean isSpectator()
    {
        return spectator;
    }

    public void setSpectator()
    {
        this.spectator = true;
        Player p = getPlayerIfOnline();
        if (p != null)
        {
            p.setGameMode(GameMode.SPECTATOR);
            for (Player player : Bukkit.getOnlinePlayers())
            {
                player.hidePlayer(p);
            }
        }
    }

    public UUID getUUID()
    {
        return uuid;
    }
}
