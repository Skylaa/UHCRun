package eu.thog.uhcrun.compatibility;

import com.connorlinfoot.titleapi.TitleAPI;
import eu.thog.uhcrun.game.AbstractGame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class TimerTask implements Runnable
{
    private final AbstractGame game;
    private int time;
    private boolean ready;

    public TimerTask(AbstractGame game)
    {
        this.game = game;
        this.time = 60;
        this.ready = false;
    }

    @Override
    public void run()
    {
        int nPlayers = this.game.getOnlineInGamePlayers().size();

        if (nPlayers >= game.getGameProperties().getMinSlots() && !this.ready)
        {
            this.ready = true;
            this.time = 60;
        }

        if (nPlayers < game.getGameProperties().getMinSlots() && this.ready)
        {
            this.ready = false;
            this.game.setStatus(Status.WAITING_FOR_PLAYERS);

            game.getMessageManager().writeNotEnougthPlayersToStart();

            for (Player p : Bukkit.getOnlinePlayers())
            {
                p.setLevel(60);
            }
        }

        if (this.ready)
        {
            this.time--;
            double percentPlayer = nPlayers / game.getGameProperties().getMaxSlots();

            if (time > 5 && percentPlayer >= 0.98)
            {
                time = 5;
            }

            if ((time < 5 && time > 0) || (time > 5 && time % 10 == 0))
            {
                game.getMessageManager().writeGameStartTime(this.time);
            }

            if (time <= 5 && time > 0)
            {
                for (Player player : Bukkit.getOnlinePlayers())
                {
                    TitleAPI.sendTitle(player, 0, 22, 0, ChatColor.RED + "" + ChatColor.BOLD + time, ChatColor.YELLOW + game.getMessageManager().getPreStartSentence());
                }
            }

            this.sendSound(this.time);

            if (this.time <= 0)
            {
                Bukkit.getScheduler().runTask(game.getPlugin(), () ->
                {
                    try
                    {
                        game.startGame();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                });

                game.getBeginTimer().cancel();
            }
        }
    }

    private void sendSound(int seconds)
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            player.setLevel(seconds);
            if (seconds <= 5 || seconds == 0)
            {
                player.playSound(player.getLocation(), Sound.NOTE_PIANO, 1, 1);
            }
        }
    }
}
