package net.supahfly.jagmapview.world;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.supahfly.jagmapview.Loader;
import net.supahfly.jagmapview.MapViewer;
import net.supahfly.jagmapview.Position;
import net.supahfly.jagmapview.Direction;
import net.supahfly.jagmapview.io.DataBuffer;
import net.supahfly.jagmapview.world.def.GameObjectDefinition;
import net.supahfly.jagmapview.world.def.MapIndex;
import net.supahfly.jagmapview.world.map.Collision;
import net.supahfly.jagmapview.world.map.TileFlags;

public class GameWorld
{
	public static final int REGION_SIZE = 64;
	
	private static final HashMap<Position, GameTile> loadedTiles = new HashMap<Position, GameTile>();
	private static final ArrayList<Integer> loadedRegionIDs = new ArrayList<Integer>();
	public static int[] SINE = new int[2048];
	public static int[] COSINE = new int[2048];
	
	static
	{
		for (int i = 0; i < SINE.length; i++)
		{
			SINE[i] = (int) (65536.0 * Math.sin(i * 0.0030679615));
			COSINE[i] = (int) (65536.0 * Math.cos(i * 0.0030679615));
		}
	}
	
	public static boolean canTraverse(Position start, Position finish)
	{
		Direction destApproach = Direction.fromDelta(start.delta(finish));
		
		return Collision.canMove8Way(collision(start), collision(finish),
					collision(start.in(Direction.NORTH)),
					collision(start.in(Direction.EAST)),
					collision(start.in(Direction.SOUTH)),
					collision(start.in(Direction.WEST)), destApproach);
	}
	
	public static ArrayList<GameObject> objectsAtPosition(Position position)
	{
		return loadedTiles.get(position).objects();
	}
	
	public static GameObject objectAtPositionID(Position position, int id)
	{
		ArrayList<GameObject> objects = objectsAtPosition(position);
		
		for (GameObject obj : objects)
		{
			if (obj.id() == id)
			{
				return obj;
			}
		}
		
		return null;
	}
	
	public static boolean isObjectAtPosition(Position position, int id)
	{
		return objectAtPositionID(position, id) != null;
	}
	
	public static GameTile putTile(GameTile tile)
	{
		loadedTiles.put((Position)tile, tile);
		return tile;
	}
	
	public static GameTile tile(Position position)
	{
		if (!loadedTiles.containsKey(position))
		{
			return putTile(new GameTile(position.x(), position.y(), position.z()));
		}
		
		return loadedTiles.get(position);
	}
	
	public static void removeObject(GameObject object)
	{
		GameTile tile = tile(object.position());
		
		if (tile.objects().contains(object))
		{
			tile.objects().remove(object);
		}
	}
	
	public static void addObject(GameObject object)
	{
		GameTile tile = tile(object.position());
		
		if (!tile.objects().contains(object))
		{
			int renderPlane = object.z();

			GameTile above = tile(new Position(object.x(), object.y(), 1));
			
			if (above.flags().has(TileFlags.BRIDGE))
			{
				renderPlane--;
			}
			
			renderObject(object, renderPlane, false);
			tile.objects().add(object);
		}
	}
	
	/*public static int key(int x, int y, int plane)
	{
		return ((plane << 30) & 0xC0000000) | ((y << 15) & 0x3FFF8000) | (x & 0x7FFF);
	}*/
	
	public static int regionCoord(int input)
	{
		return input >> 6;
	}
	
	public static int baseCoord(int input)
	{
		if (input >= 1000)
		{
			return input & 65472; // 1023 * 64
		}
		
		return input * REGION_SIZE;
	}
	
	public static int regionKey(int x, int y)
	{
		return (x << 8) + y;
	}
	
	/*public static Region getRegion(int x, int y)
	{
		int area = (x << 8) + y;
		
		if (!loadedRegions.containsKey(area))
		{
			loadedRegions.put(area, new Region(x, y, REGION_SIZE, REGION_SIZE));
		}

		return loadedRegions.get(area);
	}*/
	
	public static void loadRegions(Loader loader) throws IOException
	{
		int len = MapIndex.regionHashes().length;
		loader.updateProgress("Loading map regions", len, 0);
		
		for (int i = 0; i < len; i++)
		{
			int area = MapIndex.regionHashes()[i];
			int x = area >> 8 & 0xFF;
			int y = area & 0xFF;
			loadRegion(x, y);
			loader.updateProgress("Loading map regions", len, i);
		}
	}
	
	public static void loadRegionAbs(int x, int y) throws IOException
	{
		loadRegion(regionCoord(x), regionCoord(y));
	}
	
	public static void loadRegion(int x, int y) throws IOException
	{
		int k = regionKey(x, y);
		
		if (!loadedRegionIDs.contains(k))
		{
			DataBuffer map = MapViewer.cache().get(4, MapIndex.objectscape(k)).decompress();
			DataBuffer ls = MapViewer.cache().get(4, MapIndex.landscape(k)).decompress();
			int baseX = baseCoord(x);
			int baseY = baseCoord(y);
			
			readTileMap(ls, baseX, baseY);
			readObjectMap(map, baseX, baseY);
			
			//System.out.println("Loaded region " + k + " (" + x + "->" + baseX + ", " + y + "->" + baseY + ") into memory");
			loadedRegionIDs.add(k);
		}
	}
	
	private static final int calculateVertexHeight(int x, int y)
	{
		// 3 octaves, 0.3 amplitude
		int mapHeight = interpolatedSmoothNoise(x + 45365, y + 91923, 4) - 128
							+ (interpolatedSmoothNoise(x + 10294, y + 37821, 2) - 128 >> 1)
							+ (interpolatedSmoothNoise(x, y, 1) - 128 >> 2);
		mapHeight = (int)(mapHeight * 0.3) + 35; // original = 0.29999999999999999D instead of 0.3
		if (mapHeight < 10) {
			mapHeight = 10;
		} else if (mapHeight > 60) {
			mapHeight = 60;
		}
		// could be shortened to: return k < 10 ? 10 : (k > 60 ? 60 : k);
		return mapHeight;
	}
	
	private static final int interpolatedSmoothNoise(int x, int y, int divisor)
	{
		// since x and y aren't fractional, divisor creates the fraction? 4, 2, 1 = grid sizes? 
		int iX = x / divisor; // integer x
		int fX = x & divisor - 1; // fractional x
		int iY = y / divisor; // integer y
		int fY = y & divisor - 1; // fractional y
		int center = smoothNoise(iX, iY);
		int right = smoothNoise(iX + 1, iY);
		int top = smoothNoise(iX, iY + 1);
		int topRight = smoothNoise(iX + 1, iY + 1);
		int interpolatedMiddle = interpolateCosine(center, right, fX, divisor);
		int interpolatedTop = interpolateCosine(top, topRight, fX, divisor);
		return interpolateCosine(interpolatedMiddle, interpolatedTop, fY, divisor);
	}
	
	private static final int smoothNoise(int x, int y)
	{
		int corners = calculateNoise(x - 1, y - 1) + calculateNoise(x + 1, y - 1) + calculateNoise(x - 1, y + 1) + calculateNoise(x + 1, y + 1);
		int sides   = calculateNoise(x - 1, y)     + calculateNoise(x + 1, y)     + calculateNoise(x,     y - 1) + calculateNoise(x,     y + 1);
		int center  = calculateNoise(x,     y);
		return (corners / 16) + (sides / 8) + (center / 4); // parenthesis added for visualization purposes.
	}

	private static final int calculateNoise(int x, int y)
	{
		int n = x + y * 57;
		n = n << 13 ^ n;
		int noise = n * (n * n * 15731 + 789221) + 1376312589 & 0x7fffffff;
		return noise >> 19 & 0xff;
	}
	
	private static final int interpolateCosine(int a, int b, int fraction, int divisor) {
		int res = 65536 - COSINE[fraction * 1024 / divisor] >> 1;
		return (a * (65536 - res) >> 16) + (b * res >> 16);
	}
	
	private static void readTileMap(DataBuffer buffer, int x, int y)
	{
		/*for (int pL = 0; pL < 4; pL++)
		{
			for (int pX = 0; pX < 64; pX++)
			{
				for (int pY = 0; pY < 64; pY++)
				{
					collisionMaps[pL].adjacency[pX][pY] &= ~0x1000000;
				}
			}
		}*/
		
		for (int pL = 0; pL < 4; pL++)
		{
			for (int pX = 0; pX < REGION_SIZE; pX++)
			{
				for (int pY = 0; pY < REGION_SIZE; pY++)
				{
					readTile(buffer, pX + x, pY + y, pL, 0);
				}
			}
		}
		
		for (int pL = 0; pL < 4; pL++)
		{
			for (int pX = 0; pX < REGION_SIZE; pX++)
			{
				for (int pY = 0; pY < REGION_SIZE; pY++)
				{
					GameTile tile = tile(new Position(pX + x, pY + y, pL));
					GameTile above = tile(new Position(pX + x, pY + y, 1));
					
					if (tile.flags().has(TileFlags.UNWALKABLE))
					{
						int renderPlane = tile.z();
						
						if (above.flags().has(TileFlags.BRIDGE))
						{
							renderPlane--;
							//System.out.println("has bridge: " + tile.x() + ", " + tile.y());
						}
						
						if (renderPlane >= 0)
						{
							//System.out.println("unwalkable: " + tile.x() + ", " + tile.y());
							tile(new Position(tile.x(), tile.y(), renderPlane)).clip().add(Collision.BLOCKED);
						}
					}
				}
			}
		}
	}
	
	private static void readTile(DataBuffer buffer, int x, int y, int plane, int shapeOffset)
	{
		GameTile tile = tile(new Position(x, y, plane));
		GameTile under = null;
		
		if (plane > 0)
		{
			under = tile(new Position(x, y, plane - 1));
		}
		
		tile.clip().set(Collision.OPEN);
		
		while (buffer.hasRemaining())
		{
			int setting = buffer.getUnsigned();
			
			if (setting == 0)
			{
				if (plane == 0)
				{
					tile.setHeight(-calculateVertexHeight(932731 + x, 556238 + y) * 8);
					//vertexHeights[0][x][y] = -Region.calculateVertexHeight(932731 + x + xOffset, 556238 + y + yOffset) * 8;
				}
				else
				{
					tile.setHeight(under.height() - 240);
					//vertexHeights[plane][x][y] = vertexHeights[plane - 1][x][y] - 240;
					break;
				}
				break;
			}
			
			if (setting == 1)
			{
				int height = buffer.getUnsigned();
				
				if (height == 1)
				{
					height = 0;
				}
				
				if (plane == 0)
				{
					tile.setHeight(-height * 8);
					//vertexHeights[0][x][y] = -height * 8;
				}
				else
				{
					tile.setHeight(under.height() - height * 8);
					//vertexHeights[plane][x][y] = vertexHeights[plane - 1][x][y] - height * 8;
					break;
				}
				break;
			}
			
			if (setting <= 49)
			{
				tile.setOverlayID(buffer.get());
				tile.setOverlayClip((setting - 2) / 4);
				tile.setOverlayRotation(setting - 2 + shapeOffset & 0x3);
			}
			else if (setting <= 81)
			{
				tile.flags().set(setting - 49);
			}
			else
			{
				tile.setUnderlayID(setting - 81);
			}
		}
		
		putTile(tile);
	}
	
	private static final void readObjectMap(DataBuffer buffer, int baseX, int baseY)
	{
		int id = -1;
		
		while (true)
		{
			int idOffset = buffer.getSmart();
			
			if (idOffset == 0)
			{
				break;
			}
			
			id += idOffset;
			int data = 0;
			
			while (true)
			{
				int dataOffset = buffer.getSmart();
				
				if (dataOffset == 0)
				{
					break;
				}
				
				data += dataOffset - 1;
				int offsetY = data & 0x3f;
				int offsetX = data >> 6 & 0x3f;
				int plane = data >> 12;
				int meta = buffer.getUnsigned();
				int type = meta >> 2;
				int rotation = meta & 0x3;
				int lX = offsetX + baseX; // absx now
				int lY = offsetY + baseY; // absy
				
				GameObject obj = new GameObject(id, lX, lY, plane, rotation, type);
				addObject(obj);
			}
		}
	}
	
	public static void renderObject(GameObject obj, int renderPlane, boolean bool)
	{
		int actualX = obj.x();
		int actualY = obj.y();
		int actualH = obj.z(); // or renderPlane, idk
		
		if (((actualX >= 2515 && actualX <= 2530 && actualY >= 3097 && actualY <= 3134 && actualH >= 0 && actualH <= 0) 
			|| (actualX >= 2531 && actualX <= 2542 && actualY >= 3113 && actualY <= 3135 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 3509 && actualX <= 3509 && actualY >= 9497 && actualY <= 9497 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 3230 && actualX <= 3248 && actualY >= 3600 && actualY <= 3619 && actualH >= 0 && actualH <= 0) //train
			|| (actualX >= 3230 && actualX <= 3248 && actualY >= 3600 && actualY <= 3619 && actualH >= 1 && actualH <= 1) //train
			|| (actualX >= 2898 && actualX <= 2898 && actualY >= 3428 && actualY <= 3428 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2956 && actualX <= 2956 && actualY >= 3212 && actualY <= 3212 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2987 && actualX <= 2987 && actualY >= 3240 && actualY <= 3240 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2986 && actualX <= 2986 && actualY >= 3239 && actualY <= 3239 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2984 && actualX <= 2984 && actualY >= 3237 && actualY <= 3237 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2982 && actualX <= 2982 && actualY >= 3234 && actualY <= 3234 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2612 && actualX <= 2612 && actualY >= 3098 && actualY <= 3098 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2611 && actualX <= 2611 && actualY >= 3098 && actualY <= 3098 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2611 && actualX <= 2611 && actualY >= 3099 && actualY <= 3099 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2608 && actualX <= 2608 && actualY >= 3107 && actualY <= 3107 && actualH >= 0 && actualH <= 0)			 
			// || (actualX >= 2606 && actualX <= 2606 && actualY >= 3108 && actualY <= 3108 && actualH >= 0 && actualH <= 0) //wall fix
			// || (actualX >= 2605 && actualX <= 2605 && actualY >= 3108 && actualY <= 3108 && actualH >= 0 && actualH <= 0) //wall fix 
			// || (actualX >= 2602 && actualX <= 2602 && actualY >= 3108 && actualY <= 3108 && actualH >= 0 && actualH <= 0) //wall fix 
			|| (actualX >= 2551 && actualX <= 2551 && actualY >= 3083 && actualY <= 3083 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2551 && actualX <= 2551 && actualY >= 3082 && actualY <= 3082 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2571 && actualX <= 2571 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2569 && actualX <= 2569 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2569 && actualX <= 2569 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)	 
			|| (actualX >= 2569 && actualX <= 2569 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2568 && actualX <= 2568 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2568 && actualX <= 2568 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2566 && actualX <= 2566 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2564 && actualX <= 2564 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2564 && actualX <= 2564 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2563 && actualX <= 2563 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)			 
			|| (actualX >= 2563 && actualX <= 2563 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)	 
			|| (actualX >= 2562 && actualX <= 2562 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3100 && actualY <= 3100 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3100 && actualY <= 3100 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3099 && actualY <= 3099 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2566 && actualX <= 2566 && actualY >= 3099 && actualY <= 3099 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3099 && actualY <= 3099 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3098 && actualY <= 3098 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3097 && actualY <= 3097 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3096 && actualY <= 3096 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2566 && actualX <= 2566 && actualY >= 3096 && actualY <= 3096 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3096 && actualY <= 3096 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2566 && actualX <= 2566 && actualY >= 3095 && actualY <= 3095 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2545 && actualX <= 2545 && actualY >= 3116 && actualY <= 3116 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2545 && actualX <= 2545 && actualY >= 3117 && actualY <= 3117 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2544 && actualX <= 2544 && actualY >= 3116 && actualY <= 3116 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2544 && actualX <= 2544 && actualY >= 3117 && actualY <= 3117 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2556 && actualX <= 2556 && actualY >= 3113 && actualY <= 3113 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2556 && actualX <= 2556 && actualY >= 3114 && actualY <= 3114 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2557 && actualX <= 2557 && actualY >= 3113 && actualY <= 3113 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2557 && actualX <= 2557 && actualY >= 3114 && actualY <= 3114 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2555 && actualX <= 2555 && actualY >= 3111 && actualY <= 3111 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2557 && actualX <= 2557 && actualY >= 3111 && actualY <= 3111 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2554 && actualX <= 2554 && actualY >= 3111 && actualY <= 3111 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2553 && actualX <= 2553 && actualY >= 3111 && actualY <= 3111 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2551 && actualX <= 2551 && actualY >= 3111 && actualY <= 3111 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3095 && actualY <= 3095 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2563 && actualX <= 2563 && actualY >= 3095 && actualY <= 3095 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2564 && actualX <= 2564 && actualY >= 3094 && actualY <= 3094 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2566 && actualX <= 2566 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2569 && actualX <= 2569 && actualY >= 3094 && actualY <= 3094 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2569 && actualX <= 2569 && actualY >= 3094 && actualY <= 3094 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2570 && actualX <= 2570 && actualY >= 3095 && actualY <= 3095 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2570 && actualX <= 2570 && actualY >= 3096 && actualY <= 3096 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2571 && actualX <= 2571 && actualY >= 3096 && actualY <= 3096 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2571 && actualX <= 2571 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2569 && actualX <= 2569 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2569 && actualX <= 2569 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2569 && actualX <= 2569 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2568 && actualX <= 2568 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2568 && actualX <= 2568 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2566 && actualX <= 2566 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2564 && actualX <= 2564 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2564 && actualX <= 2564 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2563 && actualX <= 2563 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2563 && actualX <= 2563 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2562 && actualX <= 2562 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3100 && actualY <= 3100 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3100 && actualY <= 3100 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3099 && actualY <= 3099 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2566 && actualX <= 2566 && actualY >= 3099 && actualY <= 3099 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3099 && actualY <= 3099 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3098 && actualY <= 3098 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3097 && actualY <= 3097 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3096 && actualY <= 3096 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2566 && actualX <= 2566 && actualY >= 3096 && actualY <= 3096 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3096 && actualY <= 3096 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2566 && actualX <= 2566 && actualY >= 3095 && actualY <= 3095 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3095 && actualY <= 3095 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2563 && actualX <= 2563 && actualY >= 3095 && actualY <= 3095 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2564 && actualX <= 2564 && actualY >= 3094 && actualY <= 3094 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2566 && actualX <= 2566 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2567 && actualX <= 2567 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2569 && actualX <= 2569 && actualY >= 3094 && actualY <= 3094 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2569 && actualX <= 2569 && actualY >= 3094 && actualY <= 3094 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2570 && actualX <= 2570 && actualY >= 3095 && actualY <= 3095 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2570 && actualX <= 2570 && actualY >= 3096 && actualY <= 3096 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2571 && actualX <= 2571 && actualY >= 3096 && actualY <= 3096 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2569 && actualX <= 2569 && actualY >= 3095 && actualY <= 3095 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2568 && actualX <= 2568 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2581 && actualX <= 2581 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2581 && actualX <= 2581 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2580 && actualX <= 2580 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2580 && actualX <= 2580 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2577 && actualX <= 2577 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2577 && actualX <= 2577 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2577 && actualX <= 2577 && actualY >= 3105 && actualY <= 3105 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2577 && actualX <= 2577 && actualY >= 3106 && actualY <= 3106 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2576 && actualX <= 2576 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2575 && actualX <= 2575 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2574 && actualX <= 2574 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2576 && actualX <= 2576 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2576 && actualX <= 2576 && actualY >= 3105 && actualY <= 3105 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2576 && actualX <= 2576 && actualY >= 3106 && actualY <= 3106 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2575 && actualX <= 2575 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2574 && actualX <= 2574 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2575 && actualX <= 2575 && actualY >= 3105 && actualY <= 3105 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2575 && actualX <= 2575 && actualY >= 3106 && actualY <= 3106 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2574 && actualX <= 2574 && actualY >= 3105 && actualY <= 3105 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2574 && actualX <= 2574 && actualY >= 3106 && actualY <= 3106 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2604 && actualX <= 2604 && actualY >= 3100 && actualY <= 3100 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2595 && actualX <= 2595 && actualY >= 3106 && actualY <= 3106 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2594 && actualX <= 2594 && actualY >= 3106 && actualY <= 3106 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2595 && actualX <= 2595 && actualY >= 3107 && actualY <= 3107 && actualH >= 0 && actualH <= 0)
			// || (actualX >= 2595 && actualX <= 2595 && actualY >= 3108 && actualY <= 3108 && actualH >= 0 && actualH <= 0) //wall fix
			|| (actualX >= 2594 && actualX <= 2594 && actualY >= 3107 && actualY <= 3107 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2572 && actualX <= 2572 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2574 && actualX <= 2574 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2577 && actualX <= 2577 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2576 && actualX <= 2576 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2577 && actualX <= 2577 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2576 && actualX <= 2576 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)
			//|| (actualX >= 2594 && actualX <= 2594 && actualY >= 3108 && actualY <= 3108 && actualH >= 0 && actualH <= 0) //wall fix
			|| (actualX >= 2597 && actualX <= 2597 && actualY >= 3107 && actualY <= 3107 && actualH >= 0 && actualH <= 0)
			// || (actualX >= 2598 && actualX <= 2598 && actualY >= 3105 && actualY <= 3105 && actualH >= 0 && actualH <= 0) //walls, save this
			// || (actualX >= 2598 && actualX <= 2598 && actualY >= 3104 && actualY <= 3104 && actualH >= 0 && actualH <= 0) //walls, save this
			// || (actualX >= 2598 && actualX <= 2598 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0) //walls, save this
			|| (actualX >= 2592 && actualX <= 2592 && actualY >= 3089 && actualY <= 3089 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2591 && actualX <= 2591 && actualY >= 3089 && actualY <= 3089 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2590 && actualX <= 2590 && actualY >= 3089 && actualY <= 3089 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2589 && actualX <= 2589 && actualY >= 3089 && actualY <= 3089 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2591 && actualX <= 2591 && actualY >= 3090 && actualY <= 3090 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2590 && actualX <= 2590 && actualY >= 3090 && actualY <= 3090 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2590 && actualX <= 2590 && actualY >= 3091 && actualY <= 3091 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2591 && actualX <= 2591 && actualY >= 3091 && actualY <= 3091 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2592 && actualX <= 2592 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2591 && actualX <= 2591 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2590 && actualX <= 2590 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2589 && actualX <= 2589 && actualY >= 3093 && actualY <= 3093 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2593 && actualX <= 2593 && actualY >= 3092 && actualY <= 3092 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2593 && actualX <= 2593 && actualY >= 3091 && actualY <= 3091 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2594 && actualX <= 2594 && actualY >= 3091 && actualY <= 3091 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2596 && actualX <= 2596 && actualY >= 3089 && actualY <= 3089 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2596 && actualX <= 2596 && actualY >= 3086 && actualY <= 3086 && actualH >= 0 && actualH <= 0)
			//|| (actualX >= 2594 && actualX <= 2594 && actualY >= 3085 && actualY <= 3085 && actualH >= 0 && actualH <= 0) //Ladder in mage guild
			|| (actualX >= 2593 && actualX <= 2593 && actualY >= 3083 && actualY <= 3083 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2588 && actualX <= 2588 && actualY >= 3083 && actualY <= 3083 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2587 && actualX <= 2587 && actualY >= 3084 && actualY <= 3084 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2586 && actualX <= 2586 && actualY >= 3085 && actualY <= 3085 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2585 && actualX <= 2585 && actualY >= 3086 && actualY <= 3086 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2585 && actualX <= 2585 && actualY >= 3089 && actualY <= 3089 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2587 && actualX <= 2587 && actualY >= 3091 && actualY <= 3091 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2593 && actualX <= 2593 && actualY >= 3106 && actualY <= 3106 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2593 && actualX <= 2593 && actualY >= 3105 && actualY <= 3105 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2592 && actualX <= 2592 && actualY >= 3106 && actualY <= 3106 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2592 && actualX <= 2592 && actualY >= 3105 && actualY <= 3105 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2591 && actualX <= 2591 && actualY >= 3106 && actualY <= 3106 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2591 && actualX <= 2591 && actualY >= 3105 && actualY <= 3105 && actualH >= 0 && actualH <= 0)
			//|| (actualX >= 2592 && actualX <= 2592 && actualY >= 3108 && actualY <= 3108 && actualH >= 0 && actualH <= 0) //wall fix
			//|| (actualX >= 2590 && actualX <= 2590 && actualY >= 3108 && actualY <= 3108 && actualH >= 0 && actualH <= 0) //wall fix
			//|| (actualX >= 2591 && actualX <= 2591 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0) //wall fix
			//|| (actualX >= 2590 && actualX <= 2590 && actualY >= 3103 && actualY <= 3103 && actualH >= 0 && actualH <= 0) //wall fix
			//|| (actualX >= 2601 && actualX <= 2601 && actualY >= 3108 && actualY <= 3108 && actualH >= 0 && actualH <= 0) //wall fix
			|| (actualX >= 2601 && actualX <= 2601 && actualY >= 3106 && actualY <= 3106 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2600 && actualX <= 2600 && actualY >= 3105 && actualY <= 3105 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2601 && actualX <= 2601 && actualY >= 3105 && actualY <= 3105 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2600 && actualX <= 2600 && actualY >= 3106 && actualY <= 3106 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2980 && actualX <= 2980 && actualY >= 3233 && actualY <= 3233 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2977 && actualX <= 2977 && actualY >= 3233 && actualY <= 3233 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2975 && actualX <= 2975 && actualY >= 3234 && actualY <= 3234 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2971 && actualX <= 2971 && actualY >= 3237 && actualY <= 3237 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2968 && actualX <= 2968 && actualY >= 3239 && actualY <= 3239 && actualH >= 0 && actualH <= 0)
		/*	|| (actualX >= 2603 && actualX <= 2603 && actualY >= 3079 && actualY <= 3079 && actualH >= 0 && actualH <= 0) //lol boat
			|| (actualX >= 2603 && actualX <= 2603 && actualY >= 3078 && actualY <= 3078 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2604 && actualX <= 2604 && actualY >= 3079 && actualY <= 3079 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2604 && actualX <= 2604 && actualY >= 3078 && actualY <= 3078 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2605 && actualX <= 2605 && actualY >= 3079 && actualY <= 3079 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2605 && actualX <= 2605 && actualY >= 3078 && actualY <= 3078 && actualH >= 0 && actualH <= 0) */
			|| (actualX >= 2969 && actualX <= 2969 && actualY >= 3240 && actualY <= 3240 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2969 && actualX <= 2969 && actualY >= 3242 && actualY <= 3242 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2812 && actualX <= 2812 && actualY >= 3343 && actualY <= 3343 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2605 && actualX <= 2605 && actualY >= 3081 && actualY <= 3081 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2606 && actualX <= 2606 && actualY >= 3081 && actualY <= 3081 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2607 && actualX <= 2607 && actualY >= 3081 && actualY <= 3081 && actualH >= 0 && actualH <= 0)
			//|| (actualX >= 2607 && actualX <= 2607 && actualY >= 3077 && actualY <= 3077 && actualH >= 0 && actualH <= 0) //wall
			|| (actualX >= 2812 && actualX <= 2812 && actualY >= 3343 && actualY <= 3343 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2810 && actualX <= 2810 && actualY >= 3342 && actualY <= 3342 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2585 && actualX <= 2585 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2585 && actualX <= 2585 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2586 && actualX <= 2586 && actualY >= 3101 && actualY <= 3101 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2586 && actualX <= 2586 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2586 && actualX <= 2586 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2812 && actualX <= 2812 && actualY >= 3341 && actualY <= 3341 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2844 && actualX <= 2844 && actualY >= 3337 && actualY <= 3337 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2845 && actualX <= 2845 && actualY >= 3337 && actualY <= 3337 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2851 && actualX <= 2851 && actualY >= 3332 && actualY <= 3332 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2857 && actualX <= 2857 && actualY >= 3338 && actualY <= 3338 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2859 && actualX <= 2859 && actualY >= 3338 && actualY <= 3338 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2606 && actualX <= 2606 && actualY >= 3082 && actualY <= 3082 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2860 && actualX <= 2860 && actualY >= 3338 && actualY <= 3338 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2609 && actualX <= 2609 && actualY >= 3089 && actualY <= 3089 && actualH >= 0 && actualH <= 0)
			// || (actualX >= 2611 && actualX <= 2611 && actualY >= 3088 && actualY <= 3088 && actualH >= 0 && actualH <= 0) //Remember this
			|| (actualX >= 2862 && actualX <= 2862 && actualY >= 3338 && actualY <= 3338 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2859 && actualX <= 2859 && actualY >= 3335 && actualY <= 3335 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2547 && actualX <= 2547 && actualY >= 3114 && actualY <= 3114 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2546 && actualX <= 2546 && actualY >= 3114 && actualY <= 3114 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2546 && actualX <= 2546 && actualY >= 3115 && actualY <= 3115 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2547 && actualX <= 2547 && actualY >= 3115 && actualY <= 3115 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2547 && actualX <= 2547 && actualY >= 3116 && actualY <= 3116 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2594 && actualX <= 2594 && actualY >= 3102 && actualY <= 3102 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2596 && actualX <= 2597 && actualY >= 3087 && actualY <= 3088 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 3603 && actualX <= 3603 && actualY >= 3082 && actualY <= 3082 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2563 && actualX <= 2563 && actualY >= 3082 && actualY <= 3082 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2551 && actualX <= 2551 && actualY >= 3098 && actualY <= 3098 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2538 && actualX <= 2539 && actualY >= 3091 && actualY <= 3092 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2532 && actualX <= 2533 && actualY >= 3091 && actualY <= 3092 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 3079 && actualX <= 3080 && actualY >= 3500 && actualY <= 3501 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 3079 && actualX <= 3079 && actualY >= 3497 && actualY <= 3497 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 3100 && actualX <= 3101 && actualY >= 3509 && actualY <= 3510 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 3070 && actualX <= 3070 && actualY >= 3515 && actualY <= 3515 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2569 && actualX <= 2570 && actualY >= 3118 && actualY <= 3119 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2574 && actualX <= 2574 && actualY >= 3124 && actualY <= 3124 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2565 && actualX <= 2565 && actualY >= 3124 && actualY <= 3124 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2585 && actualX <= 2585 && actualY >= 3141 && actualY <= 3141 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2609 && actualX <= 2609 && actualY >= 3143 && actualY <= 3143 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2606 && actualX <= 2606 && actualY >= 3152 && actualY <= 3152 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2613 && actualX <= 2613 && actualY >= 3150 && actualY <= 3150 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2614 && actualX <= 2618 && actualY >= 3148 && actualY <= 3150 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2616 && actualX <= 2616 && actualY >= 3147 && actualY <= 3147 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2611 && actualX <= 2618 && actualY >= 3142 && actualY <= 3144 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2617 && actualX <= 2617 && actualY >= 3171 && actualY <= 3171 && actualH >= 0 && actualH <= 0)		
			|| (actualX >= 2603 && actualX <= 2603 && actualY >= 3082 && actualY <= 3082 && actualH >= 0 && actualH <= 0)	
			|| (actualX >= 2581 && actualX <= 2582 && actualY >= 9875 && actualY <= 9876 && actualH >= 0 && actualH <= 0)					
			|| (actualX >= 2547 && actualX <= 2547 && actualY >= 3117 && actualY <= 3117 && actualH >= 0 && actualH <= 0)
			|| (actualX >= 2598 && actualX <= 2598 && actualY >= 3085 && actualY <= 3085 && actualH >= 0 && actualH <= 0) // this is a freaking BELL! we need to remove it to move it around
			|| (actualX >= 2740 && actualX <= 2743 && actualY >= 9504 && actualY <= 9507 && actualH >= 0 && actualH <= 0)))
		{
			//System.out.println("ActualX: " + actualX + ", ActualY: " + actualY + ", ActualH: " + actualH);
			return;
		}
		
		GameObjectDefinition def = obj.definition();
		
		if (!def.solid)
		{
			return;
		}
		
		if (obj.type() == 22) // ground decoration
		{
			if (def.actionsBoolean || def.unknown)
			{
				if (def.actionsBoolean)
				{
					// trapdoor in lumbridge kitchen triggers this!! :)
					//System.out.println("gdec block " + obj.x() + ", " + obj.y() + ", " + renderPlane);
					collision(obj.x(), obj.y(), renderPlane).add(Collision.BLOCKED);
				}
			}
		}
		else if (obj.type() == 10 || obj.type() == 11)
		{	
			markSolidOccupant(obj.x(), obj.y(), renderPlane, def.sizeX, def.sizeY, obj.face(), def.walkable);
		}
		else if (obj.type() >= 12)
		{
			markSolidOccupant(obj.x(), obj.y(), renderPlane, def.sizeX, def.sizeY, obj.face(), def.walkable);
		}
		else if (obj.type() == 0)
		{
			markWall(obj.x(), obj.y(), renderPlane, obj.type(), obj.face(), def.walkable);
		}
		else if (obj.type() == 1)
		{
			markWall(obj.x(), obj.y(), renderPlane, obj.type(), obj.face(), def.walkable);
		}
		else if (obj.type() == 2)
		{
			markWall(obj.x(), obj.y(), renderPlane, obj.type(), obj.face(), def.walkable);
		}
		else if (obj.type() == 3)
		{
			markWall(obj.x(), obj.y(), renderPlane, obj.type(), obj.face(), def.walkable);
		}
		else if (obj.type() == 9)
		{
			markSolidOccupant(obj.x(), obj.y(), renderPlane, def.sizeX, def.sizeY, obj.face(), def.walkable);
		}
		else
		{
			// wall decorations +/- adjustable to terrain?
		}
	}
	
	public static void markSolidOccupant(int x, int y, int plane, int width, int height, int orientation, boolean blocked)
	{
		if (orientation == 1 || orientation == 3)
		{
			int tmp = width;
			width = height;
			height = tmp;
		}
		
		for (int tX = x; tX < x + width; tX++)
		{
			for (int tY = y; tY < y + height; tY++)
			{
				collision(tX, tY, plane)
					.add(Collision.OCCUPIED)
					.add(Collision.SOLID, blocked);
			}
		}
	}
	
	public static void markWall(int x, int y, int plane, int position, int orientation, boolean blocked)
	{
		/*orientation += 1;
		
		if (orientation == 4)
		{
			orientation = 0;
		}*/
		
		if (position == 0)
		{
			if (orientation == 0)
			{
				collision(x, y, plane)
					.add(Collision.WEST) /* wall west */
					.add(Collision.BLOCKED_WEST, blocked); /* blocked wall west */
				collision(x - 1, y, plane)
					.add(Collision.EAST) /* wall east */
					.add(Collision.BLOCKED_EAST, blocked); /* blocked wall east */
			}
			else if (orientation == 1)
			{
				collision(x, y, plane)
					.add(Collision.NORTH) /* wall north */
					.add(Collision.BLOCKED_NORTH, blocked); /* blocked wall north */
				collision(x, y + 1, plane)
					.add(Collision.SOUTH) /* wall south */
					.add(Collision.BLOCKED_SOUTH, blocked); /* blocked wall south */
			}
			else if (orientation == 2)
			{
				collision(x, y, plane)
					.add(Collision.EAST) /* wall east */
					.add(Collision.BLOCKED_EAST, blocked); /* blocked wall east */
				collision(x + 1, y, plane)
					.add(Collision.WEST) /* wall west */
					.add(Collision.BLOCKED_WEST, blocked); /* blocked wall west */
			}
			else if (orientation == 3)
			{
				collision(x, y, plane)
					.add(Collision.SOUTH) /* wall south */
					.add(Collision.BLOCKED_SOUTH, blocked); /* blocked wall south */
				collision(x, y - 1, plane)
					.add(Collision.NORTH) /* wall north */
					.add(Collision.BLOCKED_NORTH, blocked); /* blocked wall north */
			}
		}
		else if (position == 1 || position == 3)
		{
			if (orientation == 0)
			{
				collision(x, y, plane)
					.add(Collision.NORTHWEST) /* wall northwest */
					.add(Collision.BLOCKED_NORTHWEST, blocked); /* blocked wall northwest */
				collision(x - 1, y + 1, plane)
					.add(Collision.SOUTHEAST) /* wall southeast */
					.add(Collision.BLOCKED_SOUTHEAST, blocked); /* blocked wall southeast */
			}
			else if (orientation == 1)
			{
				collision(x, y, plane)
					.add(Collision.NORTHEAST) /* wall northeast */
					.add(Collision.BLOCKED_NORTHEAST, blocked); /* blocked wall northeast */
				collision(x + 1, y + 1, plane)
					.add(Collision.SOUTHWEST) /* wall southwest */
					.add(Collision.BLOCKED_SOUTHWEST, blocked); /* blocked wall southwest */
			}
			else if (orientation == 2)
			{
				collision(x, y, plane)
					.add(Collision.SOUTHEAST) /* wall southeast */
					.add(Collision.BLOCKED_SOUTHEAST, blocked); /* blocked wall southeast */
				collision(x + 1, y - 1, plane)
					.add(Collision.NORTHWEST) /* wall northwest */
					.add(Collision.BLOCKED_NORTHWEST, blocked); /* blocked wall northwest */
			}
			else if (orientation == 3)
			{
				collision(x, y, plane)
					.add(Collision.SOUTHWEST) /* wall southwest */
					.add(Collision.BLOCKED_SOUTHWEST, blocked); /* blocked wall southwest */
				collision(x - 1, y - 1, plane)
					.add(Collision.NORTHEAST) /* wall northeast */
					.add(Collision.BLOCKED_NORTHEAST, blocked); /* blocked wall northeast */
			}
		}
		else if (position == 2)
		{
			if (orientation == 0)
			{
				collision(x, y, plane)
					.add(Collision.WEST_NORTH) /* wall west | north */
					.add(Collision.BLOCKED_WEST_NORTH, blocked); /* blocked wall west | north */
				collision(x - 1, y, plane)
					.add(Collision.EAST) /* wall east */
					.add(Collision.BLOCKED_EAST, blocked); /* blocked wall east */
				collision(x, y + 1, plane)
					.add(Collision.SOUTH) /* wall south */
					.add(Collision.BLOCKED_SOUTH, blocked); /* blocked wall south */
			}
			else if (orientation == 1)
			{
				collision(x, y, plane)
					.add(Collision.EAST_NORTH) /* wall east | north */
					.add(Collision.BLOCKED_EAST_NORTH, blocked); /* blocked wall east | north */
				collision(x, y + 1, plane)
					.add(Collision.SOUTH) /* wall south */
					.add(Collision.BLOCKED_SOUTH, blocked); /* blocked wall south */
				collision(x + 1, y, plane)
					.add(Collision.WEST) /* wall west */
					.add(Collision.BLOCKED_WEST, blocked); /* blocked wall west */
			}
			else if (orientation == 2)
			{
				collision(x, y, plane)
					.add(Collision.EAST_SOUTH) /* wall east | south */
					.add(Collision.BLOCKED_EAST_SOUTH, blocked); /* blocked wall east | south */
				collision(x + 1, y, plane)
					.add(Collision.WEST) /* wall west */
					.add(Collision.BLOCKED_WEST, blocked); /* blocked wall west */
				collision(x, y - 1, plane)
					.add(Collision.NORTH) /* wall north */
					.add(Collision.BLOCKED_NORTH, blocked); /* blocked wall north */
			}
			else if (orientation == 3)
			{
				collision(x, y, plane)
					.add(Collision.WEST_SOUTH) /* wall east | south */
					.add(Collision.BLOCKED_WEST_SOUTH, blocked); /* blocked wall west | south */
				collision(x, y - 1, plane)
					.add(Collision.NORTH) /* wall north */
					.add(Collision.BLOCKED_NORTH, blocked); /* blocked wall north */
				collision(x - 1, y, plane)
					.add(Collision.EAST) /* wall east */
					.add(Collision.BLOCKED_EAST, blocked); /* blocked wall east */
			}
		}
	}
	
	public static Collision collision(Position p)
	{
		return tile(p).clip();
	}
	
	public static Collision collision(int x, int y, int plane)
	{
		return collision(new Position(x, y, plane));
	}
}
