package net.supahfly.jagmapview.io;

/**
 * A class that makes it easy to write a stream of bits.
 * @author Supah Fly
 */
public class BitBuffer implements Cloneable
{
	public static final int[] BITMASKS = {
		0, 0x1, 0x3, 0x7,
		0xf, 0x1f, 0x3f, 0x7f,
		0xff, 0x1ff, 0x3ff, 0x7ff,
		0xfff, 0x1fff, 0x3fff, 0x7fff,
		0xffff, 0x1ffff, 0x3ffff, 0x7ffff,
		0xfffff, 0x1fffff, 0x3fffff, 0x7fffff,
		0xffffff, 0x1ffffff, 0x3ffffff, 0x7ffffff,
		0xfffffff, 0x1fffffff, 0x3fffffff, 0x7fffffff
	};

	private int bitPosition;
	private int dataLength;
	private int bitTotal;
	private byte[] data;
	private int mark;
	
	/**
	 * Initializes the buffer using the default size for the backing byte array.
	 */
	public BitBuffer()
	{
		clear();
	}
	
	/**
	 * Initializes the buffer using the supplied backing byte array.
	 * @param data A byte array containing bit data.
	 */
	public BitBuffer(byte[] data)
	{
		this();
		this.data = data;
		skip(data.length * 8);
	}
	
	/**
	 * Clones bits from the current buffer to another {@link BitBuffer}.
	 * @param to The {@link BitBuffer} to clone bits to.
	 * @return A reference to the current {@link BitBuffer}.
	 */
	public BitBuffer copy(BitBuffer to)
	{
		int position = 0;
		
		for (byte b : data)
		{
			for (int i = 7; i >= 0; i--)
			{
				if (bitPosition > position)
				{
					to.put((b & (1 << i)) > 0);
					position++;
					continue;
				}
				
				return this;
			}
		}
		
		return this;
	}
	
	/**
	 * Clones bits from the input {@link BitBuffer} to the current {@link BitBuffer}.
	 * @param buffer The {@link BitBuffer} to clone bits from.
	 * @return A reference to the current {@link BitBuffer}.
	 */
	public BitBuffer put(BitBuffer buffer)
	{
		buffer.copy(this);
		return this;
	}
	
	/**
	 * Writes a single bit (bits have only two possible values - true or false) to the backing byte array.
	 * @param value The value of the bit to write.
	 * @return A reference to the current {@link BitBuffer}.
	 */
	public BitBuffer put(boolean value)
	{
		return put(1, value ? 1 : 0);
	}
	
	/**
	 * Calculates and writes an integer to the backing byte array based on how wide it is.
	 * @deprecated AS I CAN NOT FORCE YOU TO NOT USE IT, I STRONGLY ADVISE AGAINST IT.
	 * @param value The value of the integer to write.
	 * @return A reference to the current {@link BitBuffer}.
	 */
	public BitBuffer put(int value)
	{
		int tempValue = value;
		int count = 0;
		
		while (tempValue > 0)
		{
			count++;
			tempValue >>= 1;
		}
		
		return put(count, value);
	}
	
	/**
	 * Writes an integer using a set width to the backing byte array.
	 * @param width The width of the value.
	 * @param value The value of the integer to write.
	 * @return A reference to the current {@link BitBuffer}.
	 */
	public BitBuffer put(int width, int value)
	{
		int bytePos = bitPosition >> 3;
		int bitOffset = 8 - (bitPosition & 7);
		skip(width);

		for (; width > bitOffset; bitOffset = 8)
		{
			data[bytePos] &= (byte)~BITMASKS[bitOffset];
			data[bytePos++] |= (byte)((value >> (width - bitOffset)) & BITMASKS[bitOffset]);
			width -= bitOffset;
		}

		if (width == bitOffset)
		{
			data[bytePos] &= (byte)~BITMASKS[bitOffset];
			data[bytePos] |= (byte)(value & BITMASKS[bitOffset]);
		}
		else
		{
			data[bytePos] &= (byte)~(BITMASKS[width] << (bitOffset - width));
			data[bytePos] |= (byte)((value & BITMASKS[width]) << (bitOffset - width));
		}
		
		return this;
	}
	
	/**
	 * Reads a boolean (1 bit) from the backing byte array.
	 * @return A boolean representing the value of a single bit.
	 */
	public boolean get()
	{
		return get(1) > 0;
	}
	
	/**
	 * Reads an integer using a set width from the backing byte array.
	 * @param width The width of the value.
	 * @return An integer representing the value of a set of bits.
	 */
	public int get(int width)
	{
		int bytePos = bitPosition >> 3;
		int bitOffset = 8 - (bitPosition & 7);
		int value = 0;
		bitPosition += width;
		
		for (; width > bitOffset; bitOffset = 8)
		{
			value += (data[bytePos++] & BITMASKS[bitOffset]) << width - bitOffset;
			width -= bitOffset;
		}
		
		if (width == bitOffset)
		{
			value += data[bytePos] & BITMASKS[bitOffset];
		}
		else
		{
			value += data[bytePos] >> bitOffset - width & BITMASKS[width];
		}
		
		return value;
	}
	
	/**
	 * Rewinds this buffer. The position is set to zero and the mark is discarded.
	 * @returns A reference to the current {@link BitBuffer}.
	 */
	public BitBuffer rewind()
	{
		mark = 0;
		bitPosition = 0;
		return this;
	}
	
	@Override
	public BitBuffer clone()
	{
		BitBuffer b = new BitBuffer();
		copy(b);
		return b;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		int position = 0;
		
		for (int d = 0; d < dataLength; d++)
		{
			for (int i = 7; i >= 0; i--)
			{
				if (bitPosition > position)
				{
					sb.append((data[d] & (1 << i)) > 0 ? 1 : 0);
					position++;
					continue;
				}
				
				break;
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * @return The amount of bits remaining in the buffer.
	 */
	public int remaining()
	{
		return bitTotal - bitPosition;
	}
	
	/**
	 * Calculates the correct amount of bytes that encapsulates the bits written.
	 * @return A byte array containing bit data.
	 */
	public byte[] bytes()
	{
		dataLength = (bitTotal + 7) / 8;
		byte[] newData = new byte[dataLength];
		System.arraycopy(data, 0, newData, 0, dataLength);
		return newData;
	}
	
	/**
	 * This value may not be up to date. This value is cached. 
	 * @return The amount of bytes that encapsulates the bits that have been written.
	 */
	public int dataLength()
	{
		return dataLength;
	}
	
	/**
	 * @return The total number of bits in this buffer.
	 */
	public int length()
	{
		return bitTotal;
	}
	
	/**
	 * Resets the backing byte array and pointers to the default values.
	 * @return A reference to the current {@link BitBuffer}.
	 */
	public BitBuffer clear()
	{
		data = new byte[1];
		bitPosition = 0;
		bitTotal = 0;
		dataLength = 0;
		mark = 0;
		return this;
	}
	
	/**
	 * Jumps ahead in the buffer's position by the specified width.
	 * @param width The amount of bits to skip.
	 * @return A reference to the current {@link BitBuffer}.
	 */
	public BitBuffer skip(int width)
	{
		bitPosition += width;
		dataLength = (bitPosition + 7) / 8;
		bitTotal = bitPosition;
		ensure(dataLength);
		return this;
	}
	
	/**
	 * Sets this buffer's mark at its position.
	 * @return A reference to the current {@link BitBuffer}.
	 */
	public BitBuffer mark()
	{
		mark = bitPosition;
		return this;
	}
	
	/**
	 * Resets this buffer's position to the previously-marked position.
	 * @return A reference to the current {@link BitBuffer}.
	 */
	public BitBuffer reset()
	{
		bitPosition = mark;
		return this;
	}
	
	/**
	 * Flips this buffer. The limit is set to the current position and then the position is set to zero.
	 * @return A reference to the current {@link BitBuffer}.
	 */
	public BitBuffer flip()
	{
		bitTotal = bitPosition;
		bitPosition = 0;
		return this;
	}
	
	private void ensure(int minimum)
	{
		if (minimum >= data.length)
		{
			expand(minimum);
		}
	}
	
	private void expand(int amount)
	{
		int capacity = (data.length + 1) * 2;
		
		if (amount > capacity)
		{
			capacity = amount;
		}
		
		byte[] load = new byte[capacity];
		dataLength = data.length;
		System.arraycopy(data, 0, load, 0, dataLength);
		data = load;
	}
}
