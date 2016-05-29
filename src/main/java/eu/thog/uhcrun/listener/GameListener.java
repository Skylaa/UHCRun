package eu.thog.uhcrun.listener;

import com.connorlinfoot.titleapi.TitleAPI;
import net.minecraft.server.v1_8_R3.EntityExperienceOrb;
import net.minecraft.server.v1_8_R3.MathHelper;
import net.minecraft.server.v1_8_R3.World;
import eu.thog.uhcrun.compatibility.Status;
import eu.thog.uhcrun.game.AbstractGame;
import eu.thog.uhcrun.game.SoloGame;
import eu.thog.uhcrun.game.TeamGame;
import eu.thog.uhcrun.generator.WorldLoader;
import eu.thog.uhcrun.utils.Metadatas;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Tree;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class GameListener implements Listener
{

    private AbstractGame game;
    private Random random;

    public GameListener(AbstractGame game)
    {
        this.game = game;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            Player damaged = (Player) event.getEntity();
            Entity damager = event.getDamager();

            if (damager instanceof Player)
            {
                if (!game.isPvpEnabled() || (game instanceof TeamGame && ((TeamGame) game).getPlayerTeam(damager.getUniqueId()).hasPlayer(damaged.getUniqueId())))
                {
                    event.setCancelled(true);
                    return;
                }
                Metadatas.setMetadata(game.getPlugin(), damaged, "lastDamager", damager);

                if (((Player) damager).hasPotionEffect(PotionEffectType.INCREASE_DAMAGE))
                {
                    event.setDamage(EntityDamageEvent.DamageModifier.MAGIC, event.getDamage(EntityDamageEvent.DamageModifier.MAGIC) / 2);
                }
            } else if (damager instanceof Projectile)
            {
                Projectile arrow = (Projectile) damager;
                if (arrow.getShooter() instanceof Player)
                {
                    Player shooter = (Player) arrow.getShooter();
                    if (!game.isPvpEnabled() || (game instanceof TeamGame && ((TeamGame) game).getPlayerTeam(shooter.getUniqueId()).hasPlayer(damaged.getUniqueId())))
                    {
                        event.setCancelled(true);
                        return;
                    }
                    Metadatas.setMetadata(game.getPlugin(), damaged, "lastDamager", shooter);

                    if (shooter.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE))
                    {
                        event.setDamage(EntityDamageEvent.DamageModifier.MAGIC, event.getDamage(EntityDamageEvent.DamageModifier.MAGIC) / 2);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBrewUse(BrewEvent event)
    {
        if (event.getContents().getIngredient().getType() == Material.GLOWSTONE_DUST)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event)
    {
        Metadatas.setMetadata(game.getPlugin(), event.getItemDrop(), "playerDrop", true);
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event)
    {
        if (event.getItem().getType() == Material.GOLDEN_APPLE)
        {
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1));
        }
    }

    @EventHandler
    public void itemSpawn(ItemSpawnEvent event)
    {
        if (Metadatas.getMetadata(game.getPlugin(), event.getEntity(), "playerDrop") != null)
        {
            return;
        }

        Material mat = event.getEntity().getItemStack().getType();

        switch (mat)
        {
            case IRON_ORE:
                event.getEntity().setItemStack(new ItemStack(Material.IRON_INGOT, 2));
                break;
            case SAND:
                event.getEntity().setItemStack(new ItemStack(Material.GLASS, 1));
                break;
            case SAPLING:
                double percent = ((Tree) event.getEntity().getItemStack().getData()).getSpecies().equals(TreeSpecies.GENERIC) ? 0.1 : 0.3;
                if (random.nextDouble() <= percent)
                {
                    event.getEntity().setItemStack(new ItemStack(Material.APPLE));
                } else
                {
                    event.setCancelled(true);
                }
                break;
            case GRAVEL:
            case FLINT:
                if (random.nextDouble() < 0.75)
                {
                    ItemStack loot = new ItemStack(Material.ARROW, 3);
                    event.getEntity().setItemStack(loot);
                }
                break;
            case GOLD_ORE:
                event.getEntity().setItemStack(new ItemStack(Material.GOLD_INGOT, 2));
                break;
            case COAL:
                event.getEntity().setItemStack(new ItemStack(Material.TORCH, 3));
                break;
            case DIAMOND:
                event.getEntity().getItemStack().setAmount(event.getEntity().getItemStack().getAmount() * 2);
                break;
            case CACTUS:
                event.getEntity().setItemStack(new ItemStack(Material.LOG, 2));
                break;
            default:
                break;
        }
        spawnXPFromItemStack(event.getEntity(), event.getEntity().getItemStack().getType(), event.getEntity().getItemStack().getAmount());
    }

    private void spawnXPFromItemStack(Entity entity, Material ore, int amount)
    {
        World world = ((CraftEntity) entity).getHandle().getWorld();

        int i = 0;
        switch (ore)
        {
            case QUARTZ:
            case INK_SACK:
                i = MathHelper.nextInt(world.random, 2, 5);
                break;
            case EMERALD:
            case DIAMOND:
                i = MathHelper.nextInt(world.random, 3, 7);
                break;
            case COAL:
            case GOLD_INGOT:
            case IRON_INGOT:
                i = MathHelper.nextInt(world.random, 0, 2);
                break;
            default:
                break;

        }
        if (i == 0)
        {
            return;
        }
        int orbSize = 0;
        while (i > 0)
        {
            orbSize = EntityExperienceOrb.getOrbValue(i);
            i -= orbSize;
            world.addEntity(new EntityExperienceOrb(world, entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ(), orbSize));
        }
    }


    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event)
    {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock().getType().equals(Material.CHEST))
        {
            Chest chest = (Chest) event.getClickedBlock().getState();
            int slot = 0;
            while (slot < chest.getInventory().getSize())
            {
                ItemStack stack = chest.getInventory().getItem(slot);
                if (stack == null)
                {
                    slot++;
                    continue;
                }
                slot++;
            }
        } else if (event.getPlayer().getItemInHand() != null && event.getPlayer().getItemInHand().getType() == (Material.MINECART))
        {
            event.getPlayer().sendMessage(ChatColor.RED + "L'utilisation de Minecart est strictement interdit.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event)
    {
        if (game.isInGame(event.getEntity().getUniqueId()))
        {
            game.stumpPlayer(event.getEntity(), false);
            event.getDrops().add(new ItemStack(Material.GOLDEN_APPLE));
            if (event.getEntity().getKiller() instanceof Player)
            {
                event.getEntity().getKiller().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 20, 1));
                event.setDeathMessage("");
            } else
            {
                event.setDeathMessage(game.getTag() + " " + event.getDeathMessage());
            }

            for (Player player : Bukkit.getOnlinePlayers())
            {
                player.playSound(player.getPlayer().getPlayer().getLocation(), Sound.WITHER_DEATH, 1, 1);
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event)
    {
        if (event.getEntityType() == EntityType.WITCH || event.getEntityType() == EntityType.GUARDIAN)
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event)
    {
        if (event.getEntity() instanceof Player && !game.isDamagesEnabled())
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event)
    {
        LivingEntity entity = event.getEntity();
        List<ItemStack> newDrops = null;
        if (entity instanceof Cow || entity instanceof Horse)
        {
            newDrops = new ArrayList<>();
            for (ItemStack stack : event.getDrops())
            {
                if (stack.getType() == Material.RAW_BEEF)
                {
                    newDrops.add(new ItemStack(Material.COOKED_BEEF, stack.getAmount() * 2));
                } else if (stack.getType() == Material.LEATHER)
                {
                    newDrops.add(new ItemStack(Material.LEATHER, stack.getAmount() * 2));
                }
            }
        } else if (entity instanceof Sheep)
        {
            newDrops = event.getDrops().stream().filter(stack -> stack.getType() == Material.MUTTON).map(stack -> new ItemStack(Material.COOKED_MUTTON, stack.getAmount() * 2)).collect(Collectors.toList());
            if (random.nextInt(32) >= 16)
            {
                newDrops.add(new ItemStack(Material.LEATHER, random.nextInt(5) + 1));
            }
            if (random.nextInt(32) >= 16)
            {
                newDrops.add(new ItemStack(Material.STRING, random.nextInt(2) + 1));
            }
        } else if (entity instanceof Pig)
        {
            newDrops = event.getDrops().stream().filter(stack -> stack.getType() == Material.PORK).map(stack -> new ItemStack(Material.GRILLED_PORK, stack.getAmount() * 2)).collect(Collectors.toList());
            if (random.nextInt(32) >= 16)
            {
                newDrops.add(new ItemStack(Material.LEATHER, random.nextInt(5) + 1));
            }
        } else if (entity instanceof Rabbit)
        {
            newDrops = event.getDrops().stream().filter(stack -> stack.getType() == Material.RABBIT).map(stack -> new ItemStack(Material.COOKED_RABBIT, stack.getAmount() * 2)).collect(Collectors.toList());
        } else if (entity instanceof Chicken)
        {
            newDrops = new ArrayList<>();
            for (ItemStack stack : event.getDrops())
            {
                if (stack.getType() == Material.RAW_CHICKEN)
                {
                    newDrops.add(new ItemStack(Material.COOKED_CHICKEN, stack.getAmount() * 2));
                } else if (stack.getType() == Material.FEATHER)
                {
                    newDrops.add(new ItemStack(Material.ARROW, stack.getAmount()));
                }
            }
        } else if (entity instanceof Squid)
        {
            newDrops = new ArrayList<>();
            newDrops.add(new ItemStack(Material.COOKED_FISH, random.nextInt(5) + 1));
        } else if (entity instanceof Skeleton)
        {
            newDrops = new ArrayList<>();
            for (ItemStack stack : event.getDrops())
            {
                if (stack.getType() == Material.ARROW)
                {
                    newDrops.add(new ItemStack(Material.ARROW, stack.getAmount() * 2));
                }
                if (stack.getType() == Material.BOW)
                {
                    stack.setDurability((short) 0);
                    newDrops.add(stack);
                }
            }

        }
        if (newDrops != null)
        {
            event.getDrops().clear();
            event.getDrops().addAll(newDrops);
        }
        event.setDroppedExp(event.getDroppedExp() * 2);
    }

    @EventHandler
    public void onLoseFood(FoodLevelChangeEvent event)
    {
        event.setCancelled(this.game.getStatus() != Status.IN_GAME || !this.game.isInGame(event.getEntity().getUniqueId()));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if (game.isPvpEnabled() && event.getBlockPlaced().getY() > WorldLoader.getHighestNaturalBlockAt(event.getBlockPlaced().getX(), event.getBlockPlaced().getZ()) + 15)
        {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "[" + ChatColor.RED + "Towers" + ChatColor.DARK_RED + "] " + ChatColor.RED + "Les Towers sont interdites en UHCRun.");
        }
    }


    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event)
    {
        if (event.getBucket().equals(Material.LAVA_BUCKET) && !game.isPvpEnabled())
        {
            event.getPlayer().sendMessage(ChatColor.RED + "Le PVP est désactivé, l'utilisation de sources de lave est interdite.");
            // Force client update to avoid lava render when cancelled
            event.getPlayer().getWorld().getBlockAt(event.getBlockClicked().getLocation().add(event.getBlockFace().getModX(), event.getBlockFace().getModY(), event.getBlockFace().getModZ())).setType(Material.AIR);

            // Force client update to avoid sync problems when cancelled
            event.getPlayer().getItemInHand().setType(Material.LAVA_BUCKET);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent e)
    {
        for (int i = 0; i < 4; i++)
        {
            if (e.getLine(i).matches("^[a-zA-Z0-9ÀÁÂÄÇÈÉÊËÌÍÎÏÒÓÔÖÙÚÛÜàáâäçèéêëîïôöûü &]*$"))
            {
                if (e.getLine(i).length() > 20)
                {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLogin(PlayerLoginEvent event)
    {
        this.game.handleLogin(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLogout(PlayerQuitEvent event)
    {
        this.game.handleLogout(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPostLogin(PlayerJoinEvent event)
    {
        if (game.getStatus() == Status.WAITING_FOR_PLAYERS)
        {
            TitleAPI.sendTitle(event.getPlayer(), 20, 20 * 3, 20, ChatColor.GOLD + "" + ChatColor.BOLD + "UHCRun", ChatColor.AQUA + (game instanceof SoloGame ? "Solo" : "Team de " + ((TeamGame) game).getPersonsPerTeam()));
            game.getMessageManager().writePlayerJoinToAll(event.getPlayer());
            event.setJoinMessage("");
            game.handleJoin(event);
        }
    }
}
