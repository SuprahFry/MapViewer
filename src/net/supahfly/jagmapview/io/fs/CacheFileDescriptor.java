package net.supahfly.jagmapview.io.fs;

public class CacheFileDescriptor
{
	private final CacheIndex index;
	private final int startBlock;
	private final int size;
	private final int id;
	
	public CacheFileDescriptor(CacheIndex index, int id, int size, int startBlock)
	{
		this.startBlock = startBlock;
		this.index = index;
		this.size = size;
		this.id = id;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof CacheFileDescriptor)
		{
			CacheFileDescriptor o = (CacheFileDescriptor)other;
			return o.id == id && o.size == size && o.startBlock == startBlock && o.index.id() == index.id();
		}
		
		return false;
	}
	
	@Override
	public String toString()
	{
		return "CacheFileDescriptor(" + index + ", " + id + ", " + size + ", " + startBlock + ")";
	}

	public int size()
	{
		return size;
	}
	
	public int startBlock()
	{
		return startBlock;
	}
	
	public CacheIndex index()
	{
		return index;
	}
	
	public int id()
	{
		return id;
	}
}
