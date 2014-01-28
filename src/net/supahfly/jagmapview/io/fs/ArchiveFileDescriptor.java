package net.supahfly.jagmapview.io.fs;

public class ArchiveFileDescriptor
{
	private final int uncompressedSize;
	private final ArchiveFile archive;
	private final int compressedSize;
	private final int offset;
	private final int hash;
	
	public ArchiveFileDescriptor(int hash, int uncompressedSize, int compressedSize, int offset, ArchiveFile archive)
	{
		this.hash = hash;
		this.uncompressedSize = uncompressedSize;
		this.compressedSize = compressedSize;
		this.offset = offset;
		this.archive = archive;
	}
	
	public ArchiveFile archive()
	{
		return archive;
	}
	
	public boolean compressed()
	{
		return compressedSize != uncompressedSize;
	}
	
	@Override
	public String toString()
	{
		return "ArchiveFileDescriptor(" + hash + ", " + uncompressedSize + ", " + compressedSize + ", " + offset + ", compressed = " + compressed() + ")";
	}
	
	public int hash()
	{
		return hash;
	}
	
	public int uncompressedSize()
	{
		return uncompressedSize;
	}
	
	public int compressedSize()
	{
		return compressedSize;
	}
	
	public int offset()
	{
		return offset;
	}
}
