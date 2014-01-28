package net.supahfly.jagmapview.world.map;

import net.supahfly.jagmapview.BitMask;

public class TileFlags extends BitMask
{
	public static final int NONE = 0;
	public static final int UNWALKABLE = 0x1;
	public static final int BRIDGE = 0x2;
	public static final int ROOF = 0x4;
	public static final int zx8 = 0x8;
	
	public TileFlags()
	{
		super(NONE);
	}
}
