package net.supahfly.jagmapview.world.def;

import java.io.IOException;

import net.supahfly.jagmapview.io.DataBuffer;
import net.supahfly.jagmapview.io.fs.ArchiveFile;

public class MapIndex
{
	private static int[] regionHash;
	private static int[] regionObjectMapIndex;
	private static int[] regionLandscapeIndex;
	private static int[] regionPreload;
	
	public static void load(ArchiveFile config) throws IOException
	{
		DataBuffer b = config.entry("map_index").buffer();
		int count = b.length() / 7;
		
		regionHash = new int[count];
		regionObjectMapIndex = new int[count];
		regionLandscapeIndex = new int[count];
		regionPreload = new int[count];
		
		for (int reg = 0; reg < count; reg++)
		{
			regionHash[reg] = b.getShortUnsigned();
			regionLandscapeIndex[reg] = b.getShortUnsigned();
			regionObjectMapIndex[reg] = b.getShortUnsigned();
			regionPreload[reg] = b.getUnsigned();
		}
		
		System.out.println("Built map index with " + count + " region indices");
	}
	
	public static int[] regionHashes()
	{
		return regionHash;
	}
	
	public static int idx(int area)
	{
		int idx = -1;
		
		for (int i = 0; i < regionHash.length; i++)
		{
			if (regionHash[i] == area)
			{
				idx = i;
				break;
			}
		}
		
		return idx;
	}
	
	public static int preload(int area)
	{
		return regionPreload[idx(area)];
	}
	
	public static int landscape(int area)
	{
		return regionLandscapeIndex[idx(area)];
	}
	
	public static int objectscape(int area)
	{
		return regionObjectMapIndex[idx(area)];
	}
}
