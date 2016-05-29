package eu.thog.uhcrun.utils;

/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */

import org.bukkit.Color;

public enum Colors
{

    WHITE(Color.WHITE),
    AQUA(Color.AQUA),
    BLACK(Color.BLACK),
    BLUE(Color.BLUE),
    FUCHSIA(Color.FUCHSIA),
    GRAY(Color.GRAY),
    GREEN(Color.GREEN),
    LIME(Color.LIME),
    MAROON(Color.MAROON),
    NAVY(Color.NAVY),
    OLIVE(Color.OLIVE),
    ORANGE(Color.ORANGE),
    PURPLE(Color.PURPLE),
    RED(Color.RED),
    SILVER(Color.SILVER),
    TEAL(Color.TEAL),
    YELLOW(Color.YELLOW);


    private final Color color;

    Colors(Color color)
    {
        this.color = color;
    }

    /**
     * Get a color Object with a given number
     *
     * @param i: color number
     * @return the Color object (if it's out of range, the color will be Color.WHITE)
     */
    public static Color getColor(int i)
    {
        return i > values().length ? Color.WHITE : values()[i + 1].color;
    }
}
