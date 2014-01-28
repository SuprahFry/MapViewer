package net.supahfly.jagmapview.io.fs;

import java.util.HashMap;

import net.supahfly.jagmapview.io.DataBuffer;

public class CacheIndex
{
	public static final int DATA_SIZE = 6;
	
	private final HashMap<Integer, CacheFileDescriptor> index = new HashMap<Integer, CacheFileDescriptor>();
	private final int id;
	
	public CacheIndex(int id, DataBuffer buffer)
	{
		this.id = id;
		
		while (buffer.hasRemaining())
		{
			int file = (int)buffer.position() / DATA_SIZE;
			index.put(file, new CacheFileDescriptor(this, file, buffer.getTribyte(), buffer.getTribyte()));
		}
	}
	
	@Override
	public String toString()
	{
		return "CacheIndex(id = " + id + ", size = " + size() + ")";
	}
	
	public CacheFileDescriptor get(int id)
	{
		return index.get(id);
	}
	
	public int size()
	{
		return index.size();
	}
	
	public int id()
	{
		return id;
	}
}
