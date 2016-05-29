package eu.thog.uhcrun.generator;

import net.minecraft.server.v1_8_R3.ChunkSnapshot;
import net.minecraft.server.v1_8_R3.WorldGenCanyon;
import net.minecraft.server.v1_8_R3.WorldGenCaves;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.generator.BlockPopulator;

import java.util.List;
import java.util.Random;


/**
 * This file is a part of the UHCRun Project CodeBase
 * Created by Thog
 * (C) Copyright Thomas Guillemard 2016
 * All rights reserved.
 */
public class OrePopulator extends BlockPopulator
{

    private List<Rule> rules = new java.util.ArrayList<>();

    private int randInt(Random rand, int min, int max)
    {
        return rand.nextInt((max - min) + 1) + min;
    }

    public void addRule(Rule rule)
    {
        if (!rules.contains(rule))
        {
            rules.add(rule);
        }
    }

    @Override
    public void populate(World world, Random random, Chunk chunk)
    {
        if (chunk == null)
        {
            return;
        }

        // Gen more caves
        CraftWorld handle = (CraftWorld) world;
        int xr = randInt(random, -100, 100);
        if (xr >= 50)
        {
            new WorldGenCaves().a(handle.getHandle().chunkProviderServer, handle.getHandle(), chunk.getX(), chunk.getZ(), new ChunkSnapshot());
        } else if (xr <= -50)
        {
            new WorldGenCanyon().a(handle.getHandle().chunkProviderServer, handle.getHandle(), chunk.getX(), chunk.getZ(), new ChunkSnapshot());
        }


        // Apply rules
        for (Rule bloc : rules)
        {
            for (int i = 0; i < bloc.round; i++)
            {
                int x = chunk.getX() * 16 + random.nextInt(16);
                int y = bloc.minY + random.nextInt(bloc.maxY - bloc.minY);
                int z = chunk.getZ() * 16 + random.nextInt(16);
                generate(world, random, x, y, z, bloc.size, bloc);
            }
        }

    }

    private void generate(World world, Random rand, int x, int y, int z, int size, Rule material)
    {
        double rpi = rand.nextDouble() * Math.PI;
        double x1 = x + 8 + Math.sin(rpi) * size / 8.0F;
        double x2 = x + 8 - Math.sin(rpi) * size / 8.0F;
        double z1 = z + 8 + Math.cos(rpi) * size / 8.0F;
        double z2 = z + 8 - Math.cos(rpi) * size / 8.0F;
        double y1 = y + rand.nextInt(3) + 2;
        double y2 = y + rand.nextInt(3) + 2;
        for (int i = 0; i <= size; i++)
        {
            double xPos = x1 + (x2 - x1) * i / size;
            double yPos = y1 + (y2 - y1) * i / size;
            double zPos = z1 + (z2 - z1) * i / size;
            double fuzz = rand.nextDouble() * size / 16.0D;
            double fuzzXZ = (Math.sin((float) (i * Math.PI / size)) + 1.0F) * fuzz + 1.0D;
            double fuzzY = (Math.sin((float) (i * Math.PI / size)) + 1.0F) * fuzz + 1.0D;
            int xStart = (int) Math.floor(xPos - fuzzXZ / 2.0D);
            int yStart = (int) Math.floor(yPos - fuzzY / 2.0D);
            int zStart = (int) Math.floor(zPos - fuzzXZ / 2.0D);
            int xEnd = (int) Math.floor(xPos + fuzzXZ / 2.0D);
            int yEnd = (int) Math.floor(yPos + fuzzY / 2.0D);
            int zEnd = (int) Math.floor(zPos + fuzzXZ / 2.0D);
            for (int ix = xStart; ix <= xEnd; ix++)
            {
                double xThresh = (ix + 0.5D - xPos) / (fuzzXZ / 2.0D);
                if (xThresh * xThresh < 1.0D)
                {
                    for (int iy = yStart; iy <= yEnd; iy++)
                    {
                        double yThresh = (iy + 0.5D - yPos) / (fuzzY / 2.0D);
                        if (xThresh * xThresh + yThresh * yThresh < 1.0D)
                        {
                            for (int iz = zStart; iz <= zEnd; iz++)
                            {
                                double zThresh = (iz + 0.5D - zPos) / (fuzzXZ / 2.0D);
                                if (xThresh * xThresh + yThresh * yThresh + zThresh * zThresh < 1.0D)
                                {
                                    Block block = getBlock(world, ix, iy, iz);
                                    if (block != null && block.getType() == Material.STONE)
                                    {
                                        block.setType(Material.getMaterial(material.id));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Block getBlock(World world, int x, int y, int z)
    {
        int cx = x >> 4;
        int cz = z >> 4;

        if ((!world.isChunkLoaded(cx, cz)) &&
                (!world.loadChunk(cx, cz, false)))
        {
            return null;
        }

        Chunk chunk = world.getChunkAt(cx, cz);
        if (chunk == null)
        {
            return null;
        }
        return chunk.getBlock(x & 0xF, y, z & 0xF);
    }


    public static class Rule
    {

        public String id;
        public int round;
        public int minY;
        public int maxY;
        public int size;

        public Rule(String type, int round, int minY, int maxY, int size)
        {
            this.id = type;
            this.round = round;
            this.minY = minY;
            this.maxY = maxY;
            this.size = size;
        }
    }
}
