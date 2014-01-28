package net.supahfly.jagmapview.io;

import java.nio.ByteBuffer;

// TODO: make better
public final class DataBuffer
{
	private int lengthPosition = 0;
	private int position = 0;
	private int length = 0;
	private byte[] buffer;
	private int mark = 0;
	
	public DataBuffer(byte[] buffer)
	{
		this.buffer = buffer;
		this.length = buffer.length;
	}
	
	public DataBuffer()
	{
		this(new byte[0]);
	}
	
	private long getNumber(int amountOfBytes)
	{
		if (amountOfBytes <= 0)
		{
			throw new IndexOutOfBoundsException("amountOfBytes must be greater than 0");
		}
		
		int offset = amountOfBytes * 8;
		long output = 0;
		
		if (offset > 8)
		{
			offset -= 8;
		}
		
		for (int i = 0; i < amountOfBytes; i++, offset -= 8)
		{
			output += (get() & 0xFF) << offset;
		}
		
		return output;
	}
	
	private DataBuffer putNumber(long value, int amountOfBytes)
	{
		if (amountOfBytes <= 0)
		{
			throw new IndexOutOfBoundsException("amountOfBytes must be greater than 0");
		}
		
		int offset = (amountOfBytes * 8) - 8;
		
		for (int i = 0; i < amountOfBytes; i++, offset -= 8)
		{
			put((byte)(value >> offset));
		}
		
		return this;
	}
	
	public byte get()
	{
		return buffer[position++];
	}
	
	public byte[] get(int amount)
	{
		byte[] buff = new byte[amount];
		System.arraycopy(buffer, position, buff, 0, amount);
		position += amount;
		return buff;
	}
	
	public int getUnsigned()
	{
		return get() & 0xFF;
	}
	
	public boolean getBoolean()
	{
		return get() != 0;
	}

	public short getShort()
	{
		return (short)getNumber(2);
	}
	
	public int getShortUnsigned()
	{
		return getShort() & 0xFFFF;
	}
	
	public int getTribyte()
	{
		return (int)getNumber(3);
	}
	
	public int getInt()
	{
		return (int)getNumber(4);
	}
	
	public long getLong()
	{
		return getNumber(8);
	}
	
	public int getSmart()
	{
		int b = peek() & 0xFF;
		
		if (b < 128)
		{
			return getUnsigned();
		}
		else
		{
			return getShortUnsigned() - 32768;
		}
	}
	
	public DataBuffer putReverse(byte[] data)
	{
		for (int i = data.length - 1; i >= 0; i--)
		{
			put(data[i]);
		}
		
		return this;
	}
	
	public byte[] getReverse(int amount)
	{
		byte[] data1 = get(amount);
		byte[] data2 = new byte[data1.length];

		for (int i = data1.length - 1, n = 0; i >= 0; i--, n++)
		{
			data2[n] = data1[i];
		}

		return data2;
	}
	
	public int peek()
	{
		mark();
		int b = get();
		reset();
		return b;
	}
	
	public String getString()
	{
		return getString(10);
	}
	
	public String getString(int terminator)
	{
		StringBuilder builder = new StringBuilder();
		int b = 0;
		
		while ((b = get()) != terminator)
		{
			builder.append((char)b);
		}
		
		return builder.toString();
	}
	
	public DataBuffer put(Enum<?> e)
	{
		return put(e.ordinal());
	}
	
	public DataBuffer put(int b)
	{
		return put((byte)b);
	}
	
	public DataBuffer put(byte b)
	{
		if (position >= buffer.length)
		{
			expand();
		}
		
		buffer[position++] = b;
		
		if (position >= length)
		{
			length = position;
		}
		
		return this;
	}
	
	public DataBuffer put(byte... b)
	{
		for (int i = 0; i < b.length; i++)
		{
			put(b[i]);
		}
		
		return this;
	}
	
	public DataBuffer put(DataBuffer src)
	{
		while (src.hasRemaining())
		{
			put(src.get());
		}
		
		return this;
	}
	
	public DataBuffer put(ByteBuffer src)
	{
		while (src.hasRemaining())
		{
			 put(src.get());
		}
		
		return this;
	}
	
	public DataBuffer putLengthByte()
	{
		put((byte)0);
		lengthPosition = position;
		return this;
	}
	
	public DataBuffer putLengthShort()
	{
		putShort((short)0);
		lengthPosition = position;
		return this;
	}
	
	public DataBuffer finishByteHeader()
	{
		mark();
		int length = position - lengthPosition;
		position(lengthPosition - 1);
		put(length);
		reset();
		return this;
	}
	
	public DataBuffer finishShortHeader()
	{
		mark();
		int length = position - lengthPosition;
		position(lengthPosition - 2);
		putShort(length);
		reset();
		return this;
	}
	
	public DataBuffer putBoolean(boolean value)
	{
		return put((byte)(value ? 1 : 0));
	}
	
	public DataBuffer putShort(int value)
	{
		return putNumber(value, 2);
	}
	
	public DataBuffer putTribyte(int value)
	{
		return putNumber(value, 3);
	}
	
	public DataBuffer putInt(int value)
	{
		return putNumber(value, 4);
	}
	
	public DataBuffer putLong(long value)
	{
		return putNumber(value, 8);
	}
	
	public DataBuffer putString(String value)
	{
		return putString(value, 10);
	}
	
	public DataBuffer putString(String value, int terminator)
	{
		put(value.getBytes());
		put((byte)terminator);
		return this;
	}

	public DataBuffer expand()
	{
		byte[] oldBuffer = buffer;
		buffer = new byte[(oldBuffer.length + 1) * 2];
		System.arraycopy(oldBuffer, 0, buffer, 0, oldBuffer.length);
		return this;
	}
	
	public DataBuffer mark()
	{
		mark = position;
		return this;
	}
	
	public DataBuffer reset()
	{
		position = mark;
		return this;
	}
	
	public DataBuffer skip()
	{
		return skip(1);
	}
	
	public DataBuffer skip(int amount)
	{
		position += amount;
		return this;
	}
	
	public DataBuffer rewind()
	{
		position = 0;
		mark = 0;
		return this;
	}
	
	public int position()
	{
		return position;
	}
	
	public DataBuffer position(int to)
	{
		position = to;
		return this;
	}
	
	public DataBuffer flip()
	{
		length = position;
		position = 0;
		mark = 0;
		return this;
	}
	
	public DataBuffer compact()
	{
		int newLength = length - position;
		byte[] temp = new byte[newLength];
		System.arraycopy(buffer, position, temp, 0, newLength);
		length = newLength;
		position = length;
		buffer = temp;
		return this;
	}
	
	public int capacity()
	{
		return buffer.length;
	}
	
	public int length()
	{
		return length;
	}
	
	public boolean hasRemaining()
	{
		return position < length;
	}
	
	public int remaining()
	{
		return length - position;
	}
	
	public ByteBuffer buffer()
	{
		return ByteBuffer.wrap(array());
	}
	
	public byte[] array()
	{
		byte[] out = new byte[length];
		System.arraycopy(buffer, 0, out, 0, out.length);
		return out;
	}
	
	public DataBuffer clear()
	{
		length = buffer.length;
		position = 0;
		mark = 0;
		return this;
	}
}
