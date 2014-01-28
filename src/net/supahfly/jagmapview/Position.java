package net.supahfly.jagmapview;

public class Position extends Point
{
	private int z = 0;
	
	public Position(int x, int y, int z)
	{
		super(x, y);
		this.z = z;
	}
	
	public Position(int x, int y)
	{
		this(x, y, 0);
	}
	
	public void copy(Position position)
	{
		this.z = position.z;
		super.copy(position);
	}
	
	public boolean withinDistance(Position other)
	{
		if (other.z != z)
		{
			return false;
		}
		
		return super.withinDistance(other);
	}
	
	public Position move(int x, int y, int z)
	{
		super.move(x, y);
		this.z = z;
		return this;
	}
	
	@Override
	public Position in(Direction direction)
	{
		return (Position)clone().step(direction);
	}
	
	@Override
	public int hashCode()
	{
		return ((z << 30) & 0xC0000000) | ((y << 15) & 0x3FFF8000) | (x & 0x7FFF); // stolen from apollo :)
	}
	
	@Override
	public String toString()
	{
		return "Position[x: " + x + ", y: " + y + ", z: " + z + "]";
	}
	
	@Override
	public Position clone()
	{
		return new Position(x, y, z);
	}
	
	public boolean equals(Object obj)
	{
		if (obj == this)
		{
			return true;
		}
		
		if (obj != null && obj instanceof Position)
		{
			Position other = (Position)obj;
			
			if (super.equals(obj) && other.z == z)
			{
				return true;
			}
		}
		
		return false;
	}
	
	public int z()
	{
		return z;
	}
}

