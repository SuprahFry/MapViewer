package net.supahfly.jagmapview.world.map;

import net.supahfly.jagmapview.BitMask;
import net.supahfly.jagmapview.Direction;

public class Collision extends BitMask
{
	public static final int OPEN = 0;
	public static final int CLOSED = 0xFFFFFF;
	public static final int UNINITIALIZED = 0x1000000;
	public static final int OCCUPIED = 0x100;
	public static final int SOLID = 0x20000;
	public static final int BLOCKED = 0x200000;
	
	public static final int NORTH = 0x2;
	public static final int EAST = 0x8;
	public static final int SOUTH = 0x20;
	public static final int WEST = 0x80;
	
	public static final int NORTHEAST = 0x4;
	public static final int SOUTHEAST = 0x10;
	public static final int SOUTHWEST = 0x40;
	public static final int NORTHWEST = 0x1;

	public static final int EAST_NORTH = EAST | NORTH;
	public static final int EAST_SOUTH = EAST | SOUTH;
	public static final int WEST_SOUTH = WEST | SOUTH;
	public static final int WEST_NORTH = WEST | NORTH;
	
	public static final int BLOCKED_NORTH = 0x400;
	public static final int BLOCKED_EAST = 0x1000;
	public static final int BLOCKED_SOUTH = 0x4000;
	public static final int BLOCKED_WEST = 0x10000;
	
	public static final int BLOCKED_NORTHEAST = 0x800;
	public static final int BLOCKED_SOUTHEAST = 0x2000;
	public static final int BLOCKED_NORTHWEST = 0x200;
	public static final int BLOCKED_SOUTHWEST = 0x8000;
	
	public static final int BLOCKED_EAST_NORTH = BLOCKED_EAST | BLOCKED_NORTH;
	public static final int BLOCKED_EAST_SOUTH = BLOCKED_EAST | BLOCKED_SOUTH;
	public static final int BLOCKED_WEST_SOUTH = BLOCKED_WEST | BLOCKED_SOUTH;
	public static final int BLOCKED_WEST_NORTH = BLOCKED_WEST | BLOCKED_NORTH;
	
	public Collision()
	{
		super(OPEN);
	}
	
	public static boolean canMove4Way(Collision start, Collision dest, Direction destApproach)
	{
		if (isBlocked(dest))
		{
			return false;
		}
		
		switch (destApproach)
		{
			case NORTH:
				return !(dest.has(SOUTH) || start.has(NORTH));
			case EAST:
				return !(dest.has(WEST) || start.has(EAST));
			case SOUTH:
				return !(dest.has(NORTH) || start.has(SOUTH));
			case WEST:
				return !(dest.has(EAST) || start.has(WEST));
			default:
				return false;
		}
	}
	
	public static boolean isBlocked(Collision collision)
	{
		return (collision.is(CLOSED) || collision.has(BLOCKED) || collision.has(OCCUPIED) || collision.has(SOLID));
	}
	
	public static boolean canMove8Way(Collision start, Collision dest, Collision north, Collision east, Collision south, Collision west, Direction destApproach)
	{
		if (isBlocked(dest))
		{
			return false;
		}
		
		switch (destApproach)
		{
			case NORTH_EAST:
				/* -- N NE
				   -- O  E
				   -- - -- */
				return !(dest.has(SOUTHWEST) || dest.has(SOUTH) || dest.has(WEST)
							|| start.has(NORTHEAST) || start.has(EAST) || start.has(NORTH)
							|| east.has(WEST) || east.has(NORTH) || isBlocked(east)
							|| north.has(EAST) || north.has(SOUTH) || isBlocked(north));
			case NORTH_WEST:
				/* NW N --
				   W  O --
				   -- - -- */
				return !(dest.has(SOUTHEAST) || dest.has(SOUTH) || dest.has(EAST)
							|| start.has(NORTHWEST) || start.has(WEST) || start.has(NORTH)
							|| west.has(EAST) || west.has(NORTH) || isBlocked(west)
							|| north.has(WEST) || north.has(SOUTH) || isBlocked(north));
			case SOUTH_EAST:
				/* -- - --
				   -- O  E
				   -- S SE */
				// origin to southeast
				// origin to east
				// origin to south
				// east to south
				// south to east
				return !(dest.has(NORTHWEST) || dest.has(NORTH) || dest.has(WEST)
							|| start.has(SOUTHEAST) || start.has(EAST) || start.has(SOUTH)
							|| east.has(WEST) || east.has(SOUTH) || isBlocked(east)
							|| south.has(EAST) || south.has(NORTH) || isBlocked(south));
			case SOUTH_WEST:
				/* -- - --
				   W  O --
				   SW S -- */
				return !(dest.has(NORTHEAST) || dest.has(NORTH) || dest.has(EAST)
							|| start.has(SOUTHWEST) || start.has(WEST) || start.has(SOUTH)
							|| west.has(EAST) || west.has(SOUTH) || isBlocked(west)
							|| south.has(WEST) || south.has(NORTH) || isBlocked(south));
			default:
				return canMove4Way(start, dest, destApproach);
		}
	}
}
