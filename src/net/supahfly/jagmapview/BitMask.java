package net.supahfly.jagmapview;

public class BitMask implements Cloneable
{	
	private int mask;
	
	public BitMask(int mask)
	{
		this.mask = mask;
	}
	
	@Override
	public String toString()
	{
		return "BitMask[" + mask + "]";
	}
	
	@Override
	public BitMask clone()
	{
		return new BitMask(mask);
	}
	
	public BitMask set(int value)
	{
		mask = value;
		return this;
	}
	
	public BitMask clear()
	{
		return set(0);
	}
	
	public BitMask add(int flag)
	{
		mask |= flag;
		return this;
	}
	
	public BitMask add(int flag, boolean condition)
	{
		if (condition)
		{
			add(flag);
		}
		
		return this;
	}
	
	public BitMask clear(int flag)
	{
		mask &= ~flag;
		return this;
	}
	
	public boolean has(int flag)
	{
		return (mask & flag) != 0;
	}
	
	public boolean is(int flag)
	{
		return mask == flag;
	}
	
	public boolean isEmpty()
	{
		return is(0);
	}
	
	@Override
	public boolean equals(Object flag)
	{
		if (flag instanceof Integer)
		{
			return mask == (int)flag;
		}
		else if (flag instanceof BitMask)
		{
			return mask == ((BitMask)flag).get();
		}
		
		return super.equals(flag);
	}
	
	public int get()
	{
		return mask;
	}
}
