package net.supahfly.jagmapview.world;

import net.supahfly.jagmapview.Position;
import net.supahfly.jagmapview.world.def.GameObjectDefinition;

public class GameObject
{
	private final Position position;
	private final int id;
	private final int face;
	private final int type;
	
	public GameObject(int id, int x, int y, int plane, int face, int type)
	{
		position = new Position(x, y, plane);
		this.id = id;
		this.face = face;
		this.type = type;
	}
	
	public int id()
	{
		return id;
	}
	
	public int face()
	{
		return face;
	}
	
	public int type()
	{
		return type;
	}
	
	public int x()
	{
		return position.x();
	}
	
	public int y()
	{
		return position.y();
	}
	
	public int z()
	{
		return position.z();
	}
	
	@Override
	public int hashCode()
	{
		return super.hashCode();
	}
	
	@Override
	public String toString()
	{
		return "GameObject(" + id + ", " + super.toString() + ", " + face + ", " + type + ")";
	}
	
	public GameObjectDefinition definition()
	{
		return GameObjectDefinition.getDefinition(id);
	}

	public Position position()
	{
		return position;
	}
}
