package eu.thog.uhcrun.utils;

import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public final class Metadatas
{

    private Metadatas()
    {

    }

    public static Object getMetadata(JavaPlugin plugin, Metadatable object, String key)
    {
        List<MetadataValue> values = object.getMetadata(key);
        for (MetadataValue value : values)
        {
            if (value.getOwningPlugin().equals(plugin))
            {
                return value.value();
            }
        }

        return null;
    }

    public static void setMetadata(JavaPlugin plugin, Metadatable object, String key, Object value)
    {
        object.setMetadata(key, new FixedMetadataValue(plugin, value));
    }
}

