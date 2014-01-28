package net.supahfly.jagmapview.io.fs;

import java.io.IOException;
import java.util.HashMap;

import net.supahfly.jagmapview.io.bzip2.BZip2Decompressor;

import net.supahfly.jagmapview.io.DataBuffer;

public class ArchiveFile extends CacheFile
{
	public static final int FILE_HEADER_SIZE = 10;
	public static final int DATA_HEADER_SIZE = 6;
	
	private final HashMap<Integer, ArchiveFileDescriptor> files = new HashMap<Integer, ArchiveFileDescriptor>();
	private final boolean compressed;
	
	public ArchiveFile(CacheFileDescriptor descriptor, DataBuffer buffer) throws IOException
	{
		super(descriptor, buffer);
		int uncompressedSize = buffer.getTribyte();
		int compressedSize = buffer.getTribyte();
		
		if (compressedSize != uncompressedSize)
		{
			byte[] data = new byte[uncompressedSize];
			BZip2Decompressor.decompress(data, uncompressedSize, buffer.get(compressedSize), compressedSize, 0);
			//BZip2CompressorInputStream decompressor = new BZip2CompressorInputStream(new ByteArrayInputStream(buffer.get(compressedSize)));
			this.buffer = new DataBuffer();
			this.buffer.putTribyte(uncompressedSize);
			this.buffer.putTribyte(compressedSize);
			
			/*int b = -1;
			
			while ((b = decompressor.read()) != -1)
			{
				this.buffer.put(b);
			}
			
			decompressor.close();*/
			this.buffer.put(data);
			this.buffer.rewind();
			this.compressed = true;
		}
		else
		{
			this.compressed = false;
		}
		
		buildIndex();
	}
	
	public ArchiveFileDescriptor descriptor(int nameHash)
	{
		return files.get(nameHash);
	}
	
	public ArchiveFileEntry entry(String name) throws IOException
	{
		return entry(hash(name));
	}
	
	public ArchiveFileEntry entry(int nameHash) throws IOException
	{
		return entry(descriptor(nameHash));
	}
	
	public ArchiveFileEntry entry(ArchiveFileDescriptor descriptor) throws IOException
	{
		buffer.position(descriptor.offset());
		return new ArchiveFileEntry(descriptor, buffer.get(descriptor.compressedSize()));
	}
	
	public boolean exists(int nameHash)
	{
		return files.containsKey(nameHash);
	}
	
	public void buildIndex()
	{
		buffer.position(DATA_HEADER_SIZE);
		int count = buffer.getShort();
		int offset = buffer.position() + (count * FILE_HEADER_SIZE);
		
		for (int i = 0; i < count; i++)
		{
			ArchiveFileDescriptor desc = new ArchiveFileDescriptor(buffer.getInt(), buffer.getTribyte(), buffer.getTribyte(), offset, this);
			//System.out.println("Indexing: " + desc);
			files.put(desc.hash(), desc);
			offset += desc.compressedSize();
		}
	}
	
	public boolean compressed()
	{
		return compressed;
	}
	
	public static int hash(String name)
	{
		int hash = 0;
		name = name.toUpperCase();
		
		for (int i = 0; i < name.length(); i++)
		{
			hash = (hash * 61 + name.charAt(i)) - 32;
		}
		
		return hash;
	}
}
