package net.supahfly.jagmapview;

public enum Direction
{
	NORTH(0, 0, 1),
	NORTH_EAST(1, 1, 1),
	EAST(2, 1, 0),
	SOUTH_EAST(3, 1, -1),
	SOUTH(4, 0, -1),
	SOUTH_WEST(5, -1, -1),
	WEST(6, -1, 0),
	NORTH_WEST(7, -1, 1),
	NONE(-1, 0, 0);
	
	public final int deltaX;
	public final int deltaY;
	public final int value;
	
	private Direction(int value, int deltaX, int deltaY)
	{
		this.value = value;
		this.deltaX = deltaX;
		this.deltaY = deltaY;
	}
	
	public static boolean isConnectable(int deltaX, int deltaY)
	{
		return Math.abs(deltaX) == Math.abs(deltaY) || deltaX == 0 || deltaY == 0;
	}
	
	public static Direction fromInteger(int value)
	{
		for (Direction direction : Direction.values())
		{
			if (direction.value == value)
			{
				return direction;
			}
		}
		
		return Direction.NONE;
	}
	
	public static Direction fromInteger2(int value)
	{
		if (value > -1)
		{
			value >>= 1;
		}
		
		switch (value)
		{
			case 0:
				return NORTH;
			case 1:
				return NORTH_EAST;
			case 2:
				return EAST;
			case 3:
				return SOUTH_EAST;
			case 4:
				return SOUTH;
			case 5:
				return SOUTH_WEST;
			case 6:
				return WEST;
			case 7:
				return NORTH_WEST;
		}
		
		return Direction.NONE;
	}
	
	public static double angle(Point src, Point dst)
	{
		// TODO: make this work
		Point delta = src.delta(dst);
		double angle = Math.atan((double)delta.y() / (double)delta.x());
		angle = Math.toDegrees(angle);
		
		if (Double.isNaN(angle))
		{
			return Double.NaN;
		}
		
		if (Math.signum(delta.x()) < 0)
		{
			angle += 180.0;
		}
		
		return angle;
		
		/*double dx = (double) x - srcX, dy = (double) y - srcY;
		double angle = Math.atan(dy / dx);
		angle = Math.toDegrees(angle);
		if (Double.isNaN(angle))
			return -1;
		if (Math.signum(dx) < 0)
			angle += 180.0;
		return (int) ((((90 - angle) / 22.5) + 16) % 16);*/
	}
	
	public static Direction fromPositions(Point src, Point dst)
	{
		double angle = angle(src, dst);
		
		if (Double.isNaN(angle))
		{
			return Direction.NONE;
		}
		
		int direction = (int)((((90 - angle) / 22.5) + 16) % 16);
		System.out.println("angle: " + angle + ", direction: " + direction);
		
		/*if (direction > -1)
		{
			direction >>= 1;
		}*/
		
		return Direction.fromInteger2(direction);
	}
	
	public static Direction fromDelta(Point delta)
	{
		return fromDelta(delta.x(), delta.y());
	}
	
	public static Direction fromDelta(int deltaX, int deltaY)
	{
		for (Direction direction : Direction.values())
		{
			if (direction.deltaX == deltaX && direction.deltaY == deltaY)
			{
				return direction;
			}
		}
		
		/*if (deltaY == 1)
		{
			if (deltaX == 1)
			{
				return Direction.NORTH_EAST;
			}
			else if (deltaX == 0)
			{
				return Direction.NORTH;
			}
			else
			{
				return Direction.NORTH_WEST;
			}
		}
		else if (deltaY == -1)
		{
			if (deltaX == 1)
			{
				return Direction.SOUTH_EAST;
			}
			else if (deltaX == 0)
			{
				return Direction.SOUTH;
			}
			else
			{
				return Direction.SOUTH_WEST;
			}
		}
		else
		{
			if (deltaX == 1)
			{
				return Direction.EAST;
			}
			else if (deltaX == -1)
			{
				return Direction.WEST;
			}
		}*/
		
		return Direction.NONE;
	}
}
