package eu.thog.uhcrun.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

import java.util.List;

/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class StackListener implements Listener
{
    private int radius;


    public StackListener(int radius)
    {
        this.radius = radius;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemSpawn(ItemSpawnEvent event)
    {
        if (event.getEntityType() != EntityType.DROPPED_ITEM)
        {
            return;
        }

        Item newEntity = event.getEntity();
        int maxSize = newEntity.getItemStack().getMaxStackSize();

        List<Entity> entityList = newEntity.getNearbyEntities(radius, 1, radius);

        for (Entity anEntityList : entityList)
        {
            if (anEntityList instanceof Item)
            {
                Item curEntity = (Item) anEntityList;
                if (!curEntity.isDead())
                {
                    if (curEntity.getItemStack().getType().equals(newEntity.getItemStack().getType()))
                    {
                        if (curEntity.getItemStack().getData().getData() == newEntity.getItemStack().getData().getData())
                        {
                            if (curEntity.getItemStack().getDurability() == newEntity.getItemStack().getDurability())
                            {
                                if (Math.abs(curEntity.getLocation().getX() - newEntity.getLocation().getX()) <= radius
                                        && Math.abs(curEntity.getLocation().getY() - newEntity.getLocation().getY()) <= radius
                                        && Math.abs(curEntity.getLocation().getZ() - newEntity.getLocation().getZ()) <= radius)
                                {

                                    int newAmount = newEntity.getItemStack().getAmount();
                                    int curAmount = curEntity.getItemStack().getAmount();


                                    int more = Math.min(newAmount, maxSize - curAmount);
                                    curAmount += more;
                                    newAmount -= more;
                                    curEntity.getItemStack().setAmount(curAmount);
                                    newEntity.getItemStack().setAmount(newAmount);
                                    if (newAmount <= 0)
                                    {
                                        event.setCancelled(true);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
