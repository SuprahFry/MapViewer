package net.supahfly.jagmapview.io.fs;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import net.supahfly.jagmapview.io.DataBuffer;

public class ArchiveFileEntry
{
	private final ArchiveFileDescriptor descriptor;
	private final DataBuffer buffer;
	
	public ArchiveFileEntry(ArchiveFileDescriptor descriptor, byte[] data) throws IOException
	{
		this.descriptor = descriptor;
		
		if (descriptor.compressed())
		{
			DataBuffer bz = new DataBuffer();
			bz.put((byte)'B', (byte)'Z', (byte)'h', (byte)'1').put(data).flip();
			
			BZip2CompressorInputStream decompressor = new BZip2CompressorInputStream(new ByteArrayInputStream(bz.array()));
			buffer = new DataBuffer();
			int b = -1;
			
			while ((b = decompressor.read()) != -1)
			{
				buffer.put(b);
			}
			
			decompressor.close();
			buffer.rewind();
		}
		else
		{
			buffer = new DataBuffer(data);
		}
	}
	
	public DataBuffer buffer()
	{
		return buffer;
	}
	
	public ArchiveFileDescriptor descriptor()
	{
		return descriptor;
	}
}
