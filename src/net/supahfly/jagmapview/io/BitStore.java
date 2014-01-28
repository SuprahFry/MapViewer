package net.supahfly.jagmapview.io;

/**
 * High speed, low memory impact collection of single bit flags. The collection does not resize.
 * @author Supah Fly
 */
public class BitStore
{
	private int count = 0;
	private byte[] store;
	private int max = 0;
	
	/**
	 * Initializes the collection with a fixed size.
	 * @param size The amount of bits to store.
	 */
	public BitStore(int size)
	{
		store = new byte[(size + 7) >> 3];
		max = store.length * 8;
	}
	
	/**
	 * Re-initializes the store to all false.
	 */
	public void clear()
	{
		store = new byte[(store.length + 7) >> 3];
		count = 0;
	}
	
	/**
	 * @param index The index of the bit flag.
	 * @return Value of the bit flag for the specified index.
	 */
	public boolean get(int index)
	{
		return (store[index >> 3] & (1 << (index & 7))) != 0;
	}
	
	/**
	 * Sets the value of the bit flag at the specified index to the opposite of its current value.
	 * @param index The index of the bit flag.
	 */
	public void toggle(int index)
	{
		set(index, !get(index));
	}
	
	/**
	 * Sets the value of the bit flag at the specified index to the specified value.
	 * @param index The index of the bit flag.
	 * @param value Value of the bit flag.
	 */
	public void set(int index, boolean value)
	{
		boolean got = get(index);
		
		if (value)
		{
			if (!got)
			{
				count++;
			}
			
			store[index >> 3] |= (byte)(1 << (index & 7));
		}
		else
		{
			if (got)
			{
				count--;
			}
			
			store[index >> 3] &= (byte)~(1 << (index & 7));
		}
	}
	
	/**
	 * @return The size of the store's backing array of bytes.
	 */
	public int size()
	{
		return store.length;
	}
	
	/**
	 * @return The maximum amount of bit flags this instance supports.
	 */
	public int max()
	{
		return max;
	}
	
	/**
	 * @return The total amount of bit flags set to true.
	 */
	public int count()
	{
		return count;
	}
}
