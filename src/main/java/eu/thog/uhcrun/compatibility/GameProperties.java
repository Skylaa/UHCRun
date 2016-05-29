package eu.thog.uhcrun.compatibility;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class GameProperties
{
    private Map<String, Object> options = new HashMap<>();
    @SerializedName(value = "min-slots")
    private int minSlots = 4;
    @SerializedName(value = "max-slots")
    private int maxSlots = 12;

    public Map<String, Object> getOptions()
    {
        return options;
    }

    public Object getOption(String key, Object defValue)
    {
        return this.options.getOrDefault(key, defValue);
    }

    public int getMinSlots()
    {
        return minSlots;
    }

    public int getMaxSlots()
    {
        return maxSlots;
    }
}
