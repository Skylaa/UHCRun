package eu.thog.uhcrun.listener;

import net.md_5.bungee.api.ChatColor;
import eu.thog.uhcrun.compatibility.Status;
import eu.thog.uhcrun.game.AbstractGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class SpectatorListener implements Listener
{
    private AbstractGame game;

    public SpectatorListener(AbstractGame parent)
    {
        this.game = parent;
    }

    private boolean cancel(Player p)
    {
        return game.getStatus() != Status.IN_GAME || !game.isInGame(p.getUniqueId());
    }

    @EventHandler
    public void onLoseFood(FoodLevelChangeEvent event)
    {
        event.setCancelled(cancel((Player) event.getEntity()));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e)
    {
        e.setCancelled(cancel(e.getPlayer()));
    }

    @EventHandler
    public void onRain(WeatherChangeEvent event)
    {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent e)
    {
        if (e.getEntity() instanceof Player)
        {
            e.setCancelled(cancel((Player) e.getEntity()));
        }
    }

    @EventHandler
    public void pickup(PlayerPickupItemEvent e)
    {
        e.setCancelled(cancel(e.getPlayer()));
    }

    @EventHandler
    public void pickup(PlayerDropItemEvent e)
    {
        e.setCancelled(cancel(e.getPlayer()));
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e)
    {
        e.setCancelled(cancel(e.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event)
    {
        if ((game.getStatus() == Status.STARTING || game.getStatus() == Status.WAITING_FOR_PLAYERS) && event.getTo().getY() < 125)
        {
            event.setCancelled(true);
            event.getPlayer().teleport(game.getPlugin().getSpawnLocation());
            event.getPlayer().sendMessage(ChatColor.RED + "" + ChatColor.ITALIC + "Mais où allez vous comme ça ?!");
        }
    }

    @EventHandler
    public void onBucket(PlayerBucketFillEvent e)
    {
        e.setCancelled(cancel(e.getPlayer()));
    }

    @EventHandler
    public void onBucket(PlayerBucketEmptyEvent e)
    {
        e.setCancelled(cancel(e.getPlayer()));
    }

    @EventHandler
    public void onHanging(HangingBreakByEntityEvent e)
    {
        if (e.getEntity() instanceof Player)
        {
            e.setCancelled(cancel((Player) e.getEntity()));
        }
    }
}
