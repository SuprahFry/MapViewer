package net.supahfly.jagmapview.io.fs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import net.supahfly.jagmapview.io.DataBuffer;

public class CacheFile
{
	protected final CacheFileDescriptor descriptor;
	protected DataBuffer buffer;
	
	public CacheFile(CacheFileDescriptor descriptor, DataBuffer buffer)
	{
		this.descriptor = descriptor;
		this.buffer = buffer;
	}
	
	public DataBuffer decompress() throws IOException
	{
		GZIPInputStream gzs = new GZIPInputStream(new ByteArrayInputStream(buffer.array()));
		int i = 0;
		byte[] gzipInputBuffer = new byte[65000];
		
		do
		{
			if(i == gzipInputBuffer.length)
				throw new RuntimeException("buffer overflow!");
			int k = gzs.read(gzipInputBuffer, i, gzipInputBuffer.length - i);
			if(k == -1)
				break;
			i += k;
		} while(true);
		
		byte[] data = new byte[i];
		System.arraycopy(gzipInputBuffer, 0, data, 0, i);
		return new DataBuffer(data);
	}
	
	public CacheFileDescriptor descriptor()
	{
		return descriptor;
	}
	
	public DataBuffer buffer()
	{
		return buffer;
	}
}
