package eu.thog.uhcrun.compatibility;

import eu.thog.uhcrun.utils.Reflection;
import net.minecraft.server.v1_8_R3.IScoreboardCriteria;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardScore;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class ObjectiveSign
{
    private static Method getEntityHandle;
    private static Field getPlayerConnection;
    private static Method sendPacket;
    private static Class<?> packetScoreBoardObjective, packetDisplayObjective;

    static
    {
        try
        {

            getEntityHandle = Reflection.getOBCClass("entity.CraftPlayer").getMethod("getHandle");
            getPlayerConnection = Reflection.getNMSClass("EntityPlayer").getDeclaredField("playerConnection");
            sendPacket = Reflection.getNMSClass("PlayerConnection").getMethod("sendPacket", Reflection.getNMSClass("Packet"));
            packetScoreBoardObjective = Reflection.getNMSClass("PacketPlayOutScoreboardObjective");
            packetDisplayObjective = Reflection.getNMSClass("PacketPlayOutScoreboardDisplayObjective");

        } catch (ReflectiveOperationException e)
        {
            e.printStackTrace();
        }
    }

    protected String oldName;
    protected String name;
    protected String displayName;
    protected Player player;
    protected boolean toggled;
    protected List<Score> scores = new ArrayList<>(15);

    public ObjectiveSign(String name, String displayName, Player player)
    {
        this.name = name;
        this.oldName = name;
        this.displayName = displayName;
        this.player = player;
        sendObjective(player, name, displayName, 0, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER); // Create the tmp Objective
        showSideBar(player);
        updateScoreObjective(player);
    }

    public void updateLines()
    {
        if (player.isOnline())
        {
            this.toggled = !toggled;
            this.name = oldName + toggled;
            sendObjective(player, name, displayName, 0, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER); // Create the tmp Objective
            updateScoreObjective(player);
            showSideBar(player);
            sendObjective(player, oldName + !toggled, "", 1, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER); // Force clean up
        }
    }

    private void updateScoreObjective(Player player)
    {
        for (Score score : scores)
        {
            sendScore(player, score.name, scores.size() - score.score - 1, PacketPlayOutScoreboardScore.EnumScoreboardAction.CHANGE);
        }
    }

    private void sendScore(Player player, String name, int score, PacketPlayOutScoreboardScore.EnumScoreboardAction en)
    {
        try
        {
            Object packet = new PacketPlayOutScoreboardScore();
            Reflection.setValue(packet, "a", name);
            Reflection.setValue(packet, "b", this.name);
            Reflection.setValue(packet, "c", score);
            Reflection.setValue(packet, "d", en); // TODO: Remove NMS dependency
            this.sendPacket(packet, player);
        } catch (ReflectiveOperationException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Send Objective creation packet
     *
     * @param player
     * @param displayName
     */
    private void sendObjective(Player player, String name, String displayName, int id, IScoreboardCriteria.EnumScoreboardHealthDisplay en)
    {
        try
        {
            Object packet = packetScoreBoardObjective.newInstance();
            Reflection.setValue(packet, "a", name);
            Reflection.setValue(packet, "b", displayName);
            Reflection.setValue(packet, "c", en); // TODO: Remove NMS dependency
            Reflection.setValue(packet, "d", id);
            this.sendPacket(packet, player);
        } catch (ReflectiveOperationException e)
        {
            e.printStackTrace();
        }
    }

    private void showSideBar(Player player)
    {
        try
        {
            Object packet = packetDisplayObjective.newInstance();
            Reflection.setValue(packet, "a", 1);
            Reflection.setValue(packet, "b", name);
            this.sendPacket(packet, player);
        } catch (ReflectiveOperationException e)
        {
            e.printStackTrace();
        }
    }

    private void sendPacket(Object packet, Player player)
    {
        try
        {
            Object thePlayer = getEntityHandle.invoke(player);
            Object playerConnection = getPlayerConnection.get(thePlayer);
            sendPacket.invoke(playerConnection, packet);
        } catch (ReflectiveOperationException e)
        {
            e.printStackTrace();
        }
    }

    public void setLine(int index, String value)
    {
        if (index > 15 || index < 0)
        {
            return;
        }
        if (scores.size() <= index)
        {
            scores.add(index, new Score(value, index));
        } else
        {
            scores.set(index, new Score(value, index));
        }
    }

    public static class Score
    {
        private int score;
        private String name;

        public Score(String name, int score)
        {
            this.name = name;
            this.score = score;
        }

        public void setScore(int score)
        {
            this.score = score;
        }

        public String getName()
        {
            return name;
        }
    }
}
