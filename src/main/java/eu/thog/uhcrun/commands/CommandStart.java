package eu.thog.uhcrun.commands;

import eu.thog.uhcrun.UHCRun;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class CommandStart implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings)
    {
        if (commandSender.hasPermission("uhcrun.start"))
        {
            UHCRun.getInstance().getAdaptator().getGame().startGame();
        }
        return true;
    }
}
