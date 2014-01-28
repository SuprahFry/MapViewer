package net.supahfly.jagmapview.io.fs;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.zip.CRC32;

import net.supahfly.jagmapview.io.DataBuffer;

public class IndexedFileSystem implements Closeable
{
	public static final int DATA_BLOCK_SIZE = 512;
	public static final int DATA_HEADER_SIZE = 8;
	public static final int DATA_SIZE = DATA_HEADER_SIZE + DATA_BLOCK_SIZE;

	private final RandomAccessFile dataFile;
	private final CacheIndex[] indices;
	private final byte[] crcTable;
	private final int[] checksums;
	
	public IndexedFileSystem(File directory) throws IOException
	{
		System.out.println("Loading cache from " + directory.getPath());
		this.dataFile = new RandomAccessFile(new File(directory.getPath() + "/main_file_cache.dat"), "r");
		ArrayList<CacheIndex> temp = new ArrayList<CacheIndex>();
		int totalEntries = 0;
		
		for (int i = 0; i < 255; i++)
		{
			File indexFile = new File(directory.getPath() + "/main_file_cache.idx" + i);
			
			if (indexFile.exists())
			{
				FileInputStream fis = new FileInputStream(indexFile);
				byte[] data = new byte[fis.available()];
				fis.read(data);
				fis.close();
				CacheIndex idx = new CacheIndex(i, new DataBuffer(data));
				temp.add(idx);
				System.out.println("\tIndex " + idx.id() + " found and built with " + idx.size() + " entries");
				totalEntries += idx.size();
			}
		}
		
		this.indices = temp.toArray(new CacheIndex[0]);
		System.out.println("Total files indexed in cache: " + totalEntries);
		
		CacheIndex idx = index(0);
		System.out.println("Building CRC32 table (" + idx.size() + " entries)");
		
		checksums = new int[idx.size()];
		DataBuffer crcBuffer = new DataBuffer(new byte[checksums.length * 4 + 4]);
		int hash = 1234;
		CRC32 crc32 = new CRC32();
		
		for (int i = 1; i < checksums.length; i++)
		{
			crc32.reset();
			byte[] bytes = get(0, i).buffer().array();
			crc32.update(bytes, 0, bytes.length);
			checksums[i] = (int)crc32.getValue();
			System.out.println("\tFile " + i + " CRC: " + checksums[i]);
		}
		
		for (int i = 0; i < checksums.length; i++)
		{
			hash = (hash << 1) + checksums[i];
			crcBuffer.putInt(checksums[i]);
		}
		
		System.out.println("CRC32 hash: " + hash);
		
		this.crcTable = crcBuffer.putInt(hash).flip().array();
	}
	
	public CacheIndex index(int index)
	{
		return indices[index];
	}
	
	public DataBuffer getBuffer(CacheFileDescriptor descriptor) throws IOException
	{
		//System.out.println("Requesting file " + descriptor);
		
		int expectedIndexID = descriptor.index().id() + 1;
		DataBuffer fileBuffer = new DataBuffer();
		int currentBlockID = descriptor.startBlock();
		int remaining = descriptor.size();
		int nextPartID = 0;
		
		while (remaining > 0)
		{
			dataFile.seek(currentBlockID * DATA_SIZE);
			byte[] tempData = new byte[DATA_HEADER_SIZE];
			dataFile.read(tempData);
			DataBuffer tempBuffer = new DataBuffer(tempData);
			int currentFileID = tempBuffer.getShort();
			int currentPartID = tempBuffer.getShort();
			int nextBlockID = tempBuffer.getTribyte();
			int nextIndexID = tempBuffer.get();
			
			if (currentFileID != descriptor.id())
			{
				throw new IOException("Different file ID, index and data appear to be corrupt.");
			}
			else if (currentPartID != nextPartID)
			{
				throw new IOException("Block ID out of order or wrong file being accessed.");
			}
			else if (nextIndexID != expectedIndexID)
			{
				throw new IOException("Wrong index ID, must be a different type of file.");
			}
			
			byte[] block = new byte[remaining > DATA_BLOCK_SIZE ? DATA_BLOCK_SIZE : remaining];
			dataFile.read(block);
			fileBuffer.put(block);
			remaining -= block.length;
			currentBlockID = nextBlockID;
			nextPartID++;
		}
		
		fileBuffer.flip();
		return fileBuffer;
	}
	
	public CacheFile get(int index, int identifier) throws IOException
	{
		return get(index(index).get(identifier));
	}
	
	public CacheFile get(CacheFileDescriptor descriptor) throws IOException
	{
		return new CacheFile(descriptor, getBuffer(descriptor));
	}
	
	public ArchiveFile getArchive(int index, int identifier) throws IOException
	{
		return getArchive(index(index).get(identifier));
	}
	
	public ArchiveFile getArchive(CacheFileDescriptor descriptor) throws IOException
	{
		return new ArchiveFile(descriptor, getBuffer(descriptor));
	}
	
	public long dataLength() throws IOException
	{
		return dataFile.length();
	}
	
	public int indexLength()
	{
		return indices.length;
	}
	
	public int[] checksums()
	{
		return checksums;
	}
	
	public byte[] crcTable()
	{
		return crcTable;
	}
	
	@Override
	public void close() throws IOException
	{
		dataFile.close();
	}
}
