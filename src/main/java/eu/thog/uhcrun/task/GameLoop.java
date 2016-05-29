package eu.thog.uhcrun.task;

import com.connorlinfoot.actionbarapi.ActionBarAPI;
import eu.thog.uhcrun.UHCRun;
import eu.thog.uhcrun.compatibility.ObjectiveSign;
import eu.thog.uhcrun.game.UHCPlayer;
import eu.thog.uhcrun.game.AbstractGame;
import eu.thog.uhcrun.game.TeamGame;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class GameLoop implements Runnable
{
    private final World world;
    private final AbstractGame game;
    private final ConcurrentHashMap<UUID, ObjectiveSign> objectives = new ConcurrentHashMap<>();
    private final UHCRun plugin;
    private final Server server;
    private int minutes, seconds;
    private GameLoop.TimedEvent nextEvent;

    public GameLoop(AbstractGame game, UHCRun plugin, Server server)
    {
        this.game = game;
        this.plugin = plugin;
        this.server = server;
        this.world = server.getWorld("world");
        this.nextEvent = new GameLoop.TimedEvent(1, 0, "Dégats actifs", ChatColor.GOLD)
        {
            @Override
            public void run()
            {
                server.broadcastMessage(ChatColor.GOLD + "Les dégats sont désormais actifs.");
                server.broadcastMessage(ChatColor.GOLD + "La map sera réduite dans " + (game.getPreparingTime() - 1) + " minutes. Le PVP sera activé à ce moment là.");
                game.enableDamages();
                createReductionEvent();
            }
        };
    }

    public int getTime()
    {
        return this.minutes * 60 + this.seconds;
    }

    public void forceNextEvent()
    {
        if (this.nextEvent != null)
        {
            this.nextEvent.run();
        }

    }

    public void addPlayer(UUID uuid, ObjectiveSign sign)
    {
        this.objectives.put(uuid, sign);
    }

    public void removePlayer(UUID uuid)
    {
        this.objectives.remove(uuid);
    }

    private void createReductionEvent()
    {
        this.nextEvent = new GameLoop.TimedEvent(this.game.getPreparingTime() - 1, 0, "Téléportation", ChatColor.RED)
        {
            @Override
            public void run()
            {
                game.disableDamages();
                game.teleportDeathMatch();
                int size = game.getDeathMatchSize();
                int border = size / 2;

                try
                {
                    world.getWorldBorder().setSize((double) size, 0L);
                } catch (Exception var5)
                {
                    var5.printStackTrace();
                }

                try
                {
                    world.getWorldBorder().setSize(10.0D, (long) (game.getReductionTime() * 60));
                } catch (Exception var4)
                {
                    var4.printStackTrace();
                }

                server.broadcastMessage(ChatColor.GOLD + "La map est désormais réduite en " + size + " * " + size);
                server.broadcastMessage(ChatColor.GOLD + "Les bordures sont en coordonnées " + ChatColor.RED + "-" + border + " +" + border);
                server.broadcastMessage(ChatColor.GOLD + "Les dégats et le PVP seront activés dans 30 secondes !");
                nextEvent = new GameLoop.TimedEvent(0, 30, "PVP Activé", ChatColor.RED)
                {
                    @Override
                    public void run()
                    {
                        game.enablePVP();
                        game.enableDamages();
                        server.broadcastMessage(ChatColor.GOLD + "Les dégats et le PVP sont maintenant activés. Bonne chance !");
                        server.broadcastMessage(ChatColor.GOLD + "La map est maintenant en réduction constante pendant les 10 prochaines minutes.");
                        nextEvent = new GameLoop.TimedEvent(game.getReductionTime() - 1, 30, "Fin de réduction", ChatColor.WHITE)
                        {
                            @Override
                            public void run()
                            {
                                server.broadcastMessage(ChatColor.GOLD + "La map est désormais réduite. Fin de partie forcée dans 2 minutes.");
                                nextEvent = new GameLoop.TimedEvent(2, 0, "Fin de partie", ChatColor.RED)
                                {
                                    @Override
                                    public void run()
                                    {
                                        server.broadcastMessage(ChatColor.GOLD + "La partie se termine.");
                                        server.shutdown();
                                    }
                                };
                            }
                        };
                    }
                };
            }
        };
    }

    private String toString(int minutes, int seconds)
    {
        return (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    @Override
    public void run()
    {
        ++this.seconds;
        if (this.seconds >= 60)
        {
            ++this.minutes;
            this.seconds = 0;
        }

        for (UUID player : this.objectives.keySet())
        {
            final ObjectiveSign objective = this.objectives.get(player);
            Player player1 = server.getPlayer(player);
            if (player1 == null)
            {
                server.getLogger().info("Player null :  " + player);
                this.objectives.remove(player);
            } else
            {
                objective.setLine(0, ChatColor.BLUE + " ");
                objective.setLine(1, ChatColor.GRAY + "Joueurs : " + ChatColor.WHITE + game.getOnlineInGamePlayers().size());
                objective.setLine(2, ChatColor.GRAY + "  ");
                int lastLine = 1;
                if (game instanceof TeamGame)
                {
                    objective.setLine(2, ChatColor.GRAY + "Équipes : " + ChatColor.WHITE + ((TeamGame) game).getTeams().size());
                    objective.setLine(3, ChatColor.RED + "   ");
                    lastLine = 3;
                }
                if (this.nextEvent != null)
                {
                    ActionBarAPI.sendActionBar(player1, this.nextEvent.color.toString() + this.nextEvent.string + " dans " + this.toString(this.nextEvent.seconds == 0 ? this.nextEvent.minutes - 1 : this.nextEvent.minutes, this.nextEvent.seconds == 0 ? 59 : this.nextEvent.seconds - 1));
                }

                UHCPlayer uhcPlayer = this.game.getPlayer(player);
                final int kills = uhcPlayer == null ? 0 : uhcPlayer.getKills();
                if (kills > 0)
                {
                    objective.setLine(lastLine + 1, ChatColor.GRAY + "Joueurs tués : " + ChatColor.WHITE + "" + kills);
                    objective.setLine(lastLine + 2, ChatColor.AQUA + "      ");
                    lastLine += 2;
                }

                objective.setLine(lastLine + 1, ChatColor.GRAY + "Bordure :");
                objective.setLine(lastLine + 2, ChatColor.WHITE + "-" + (int) world.getWorldBorder().getSize() / 2 + " +" + (int) world.getWorldBorder().getSize() / 2);
                objective.setLine(lastLine + 3, ChatColor.RED + " ");
                objective.setLine(lastLine + 4, ChatColor.GRAY + "Temps : " + ChatColor.WHITE + this.toString(this.minutes, this.seconds));
                objective.updateLines();
            }
        }

        if (this.nextEvent.seconds == 0 && this.nextEvent.minutes <= 3 && this.nextEvent.minutes > 0 || this.nextEvent.minutes == 0 && (this.nextEvent.seconds < 6 || this.nextEvent.seconds == 10 || this.nextEvent.seconds == 30))
        {
            server.broadcastMessage(this.game.getTag() + " " + ChatColor.GOLD + this.nextEvent.string + ChatColor.GOLD + " dans " + (this.nextEvent.minutes != 0 ? this.nextEvent.minutes + "min" : this.nextEvent.seconds + " sec"));
        }

        if (this.nextEvent.seconds == 0 && 0 == this.nextEvent.minutes)
        {
            server.broadcastMessage(this.game.getTag() + " " + ChatColor.GOLD + this.nextEvent.string + ChatColor.GOLD + " maintenant !");
        }

        this.nextEvent.decrement();
    }

    public abstract class TimedEvent
    {
        public int minutes;
        public int seconds;
        public String string;
        public ChatColor color;
        public boolean wasRun = false;

        public TimedEvent(int minutes, int seconds, String string, ChatColor color)
        {
            this.minutes = minutes;
            this.seconds = seconds;
            this.string = string;
            this.color = color;
        }

        public abstract void run();

        public void decrement()
        {
            --this.seconds;
            if (this.seconds < 0)
            {
                --this.minutes;
                this.seconds = 59;
            }

            if ((this.minutes < 0 || this.seconds == 0 && this.minutes == 0) && !this.wasRun)
            {
                this.wasRun = true;
                this.run();
            }

        }
    }
}
