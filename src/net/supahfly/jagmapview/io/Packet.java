package net.supahfly.jagmapview.io;


public class Packet
{
	private final DataBuffer buffer;
	private final int opcode;
	
	public Packet(int opcode, DataBuffer buffer)
	{
		this.opcode = opcode;
		this.buffer = buffer;
	}
	
	public DataBuffer buffer()
	{
		return buffer;
	}
	
	public int opcode()
	{
		return opcode;
	}
}
