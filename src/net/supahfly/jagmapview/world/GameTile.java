package net.supahfly.jagmapview.world;

import java.util.ArrayList;

import net.supahfly.jagmapview.Position;
import net.supahfly.jagmapview.world.map.Collision;
import net.supahfly.jagmapview.world.map.TileFlags;

public class GameTile extends Position
{
	private final ArrayList<GameObject> objects = new ArrayList<GameObject>();
	private final TileFlags flags = new TileFlags();
	private final Collision clip = new Collision();
	
	private int underlayID;
	private int overlayID;
	private int overlayClip;
	private int overlayRotation;
	private int height;
	
	public GameTile(int x, int y, int plane)
	{
		super(x, y, plane);
	}
	
	@Override
	public String toString()
	{
		return super.toString() + " GameTile(height: " + height + ", ol: " + overlayID + ", ul: " + underlayID + " (or: " + overlayRotation + ", oc: " + overlayClip + ") clip: " + clip + ", flag: " + flags + ")";
	}
	
	public void setUnderlayID(int id)
	{
		this.underlayID = id;
	}
	
	public void setOverlayID(int id)
	{
		this.overlayID = id;
	}
	
	public void setOverlayClip(int id)
	{
		this.overlayClip = id;
	}
	
	public void setOverlayRotation(int id)
	{
		this.overlayRotation = id;
	}
	
	public void setHeight(int height)
	{
		this.height = height;
	}
	
	public int height()
	{
		return height;
	}
	
	public int underlayID()
	{
		return underlayID;
	}
	
	public int overlayID()
	{
		return overlayID;
	}
	
	public int overlayClip()
	{
		return overlayClip;
	}
	
	public int overlayRotation()
	{
		return overlayRotation;
	}
	
	public Collision clip()
	{
		return clip;
	}
	
	public TileFlags flags()
	{
		return flags;
	}

	public ArrayList<GameObject> objects()
	{
		return objects;
	}
}
