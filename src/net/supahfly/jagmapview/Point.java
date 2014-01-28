package net.supahfly.jagmapview;

public class Point implements Cloneable
{
	public static final int REGION_PADDING = 6;
	public static final int REGION_BASE_THRESHOLD = REGION_PADDING - 2;
	public static final int REGION_SIZE = 8;
	public static final int TOTAL_REGIONS = 13;
	public static final int LOADED_AREA = REGION_SIZE * TOTAL_REGIONS;
	public static final int REGION_LOCAL_THRESHOLD = REGION_BASE_THRESHOLD * REGION_SIZE;
	public static final int REGION_LOCAL_THRESHOLD_HIGH = LOADED_AREA - REGION_BASE_THRESHOLD * REGION_SIZE;

	protected int x = 0;
	protected int y = 0;
	protected int regionCenterX;
	protected int regionCenterY;
	
	public Point(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public void copy(Point other)
	{
		this.x = other.x;
		this.y = other.y;
		rebaseRegion();
	}
	
	public int distance(Point other)
	{
		int lX = (other.x - x);
		int lY = (other.y - y);
		lX *= lX;
		lY *= lY;
		return (int)Math.abs(Math.sqrt(lX + lY));
	}
	
	public Point delta(Point other)
	{
		return new Point(other.x - x, other.y - y);
	}
	
	public boolean withinDistance(Point other)
	{
		//Position delta = delta(other);
		//return delta.x <= 15 && delta.x >= -16 && delta.y <= 15 && delta.y >= -16;
		return distance(other) < 16; // this seems to be a better way of doing it.
	}
	
	public boolean reachedRegionBaseThreshold()
	{
		return (localX() <= REGION_LOCAL_THRESHOLD
				|| localX() >= REGION_LOCAL_THRESHOLD_HIGH
				|| localY() <= REGION_LOCAL_THRESHOLD
				|| localY() >= REGION_LOCAL_THRESHOLD_HIGH);
		/*return (currentCenterRegionX() <= loadedCenterRegionX() - REGION_BASE_THRESHOLD
				|| currentCenterRegionX() >= loadedCenterRegionX() + REGION_BASE_THRESHOLD
				|| currentCenterRegionY() <= loadedCenterRegionY() - REGION_BASE_THRESHOLD
				|| currentCenterRegionY() >= loadedCenterRegionY() + REGION_BASE_THRESHOLD);*/
	}
	
	public void rebaseRegion()
	{
		regionCenterX = currentCenterRegionX(); // top left
		regionCenterY = currentCenterRegionY();
	}
	
	public Point move(int x, int y)
	{
		this.x = x;
		this.y = y;
		return this;
	}
	
	@Override
	public int hashCode()
	{
		return (x << 8) + y;
	}
	
	@Override
	public String toString()
	{
		return "Point[x: " + x + ", y: " + y + "]";
	}
	
	@Override
	public Point clone()
	{
		return new Point(x, y);
	}
	
	public boolean equals(Object obj)
	{
		if (obj == this)
		{
			return true;
		}
		
		if (obj != null && obj instanceof Point)
		{
			Point other = (Point)obj;
			
			if (other.x == x && other.y == y)
			{
				return true;
			}
		}
		
		return false;
	}
	
	public Point step(int x, int y)
	{
		return move(this.x + x, this.y + y);
	}
	
	public Point step(Direction direction)
	{
		return step(direction.deltaX, direction.deltaY);
	}
	
	public Point in(Direction direction)
	{
		return clone().step(direction);
	}
	
	public int x()
	{
		return x;
	}
	
	public int y()
	{
		return y;
	}
	
	public int currentCenterRegionX()
	{
		return x >> 3;
	}
	
	public int currentCenterRegionY()
	{
		return y >> 3;
	}
	
	public int loadedCenterRegionX()
	{
		return regionCenterX;
	}
	
	public int loadedCenterRegionY()
	{
		return regionCenterY;
	}
	
	public int loadedRegionBaseX()
	{
		return regionCenterX - REGION_PADDING;
	}
	
	public int loadedRegionBaseY()
	{
		return regionCenterY - REGION_PADDING;
	}
	
	public int loadedLowRegionX()
	{
		return regionCenterX + REGION_PADDING;
	}
	
	public int loadedLowRegionY()
	{
		return regionCenterY + REGION_PADDING;
	}
	
	public int localX()
	{
		return x - 8 * loadedRegionBaseX(); // center of loaded region in client
	}
	
	public int localY()
	{
		return y - 8 * loadedRegionBaseY();
	}
	
	public Point base()
	{
		return new Point(x & 65472, y & 65472);
	}
	
	public int regionArea()
	{
		return (regionAreaX() << 8) + regionAreaY();
	}
	
	public int regionAreaX()
	{
		return x >> 6;
	}
	
	public int regionAreaY()
	{
		return y >> 6;
	}
}
