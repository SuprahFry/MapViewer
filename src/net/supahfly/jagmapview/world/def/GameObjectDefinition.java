package net.supahfly.jagmapview.world.def;

import java.io.IOException;

import net.supahfly.jagmapview.Loader;
import net.supahfly.jagmapview.io.DataBuffer;
import net.supahfly.jagmapview.io.fs.ArchiveFile;

public class GameObjectDefinition {

	public boolean unknown;
	private byte lightAmbient;
	private int translateX;
	public String name;
	private int modelSizeZ;
	private byte lightDiffuse;
	public int sizeX;
	private int translateY;
	public int icon;
	private int[] originalModelColors;
	private int modelSizeX;
	public int configId;
	private boolean rotated;
	public static boolean lowMemory;
	private static DataBuffer buffer;
	public int id = -1;
	private static int[] bufferOffsets;
	private static int definitionCount;
	public boolean walkable;
	public int mapScene;
	public int[] childrenIds;
	public int solidInt;
	public int sizeY;
	public boolean adjustToTerrain;
	public boolean aBoolean269;
	public boolean unwalkableSolid;
	public boolean solid;
	public int face;
	private boolean delayShading;
	private static int cacheIndex;
	private int modelSizeY;
	private int[] modelIds;
	public int varbitId;
	public int unknown4;
	private int[] modelTypes;
	public String description;
	public boolean actionsBoolean;
	public boolean castsShadow;
	public int animationId;
	private static GameObjectDefinition[] cache;
	private int translateZ;
	private int[] modifiedModelColors;
	public String[] actions;

	public static final GameObjectDefinition getDefinition(int id) {
		for (int index = 0; index < 20; index++) {
			if (GameObjectDefinition.cache[index].id == id) {
				return GameObjectDefinition.cache[index];
			}
		}
		GameObjectDefinition.cacheIndex = (GameObjectDefinition.cacheIndex + 1) % 20;
		GameObjectDefinition definition = GameObjectDefinition.cache[GameObjectDefinition.cacheIndex];
		buffer.position(bufferOffsets[id]);
		//GameObjectDefinition.buffer.offset = GameObjectDefinition.bufferOffsets[id];
		definition.id = id;
		definition.setDefaultValues();
		definition.load(GameObjectDefinition.buffer);
		return definition;
	}

	private final void setDefaultValues() {
		modelIds = null;
		modelTypes = null;
		name = null;
		description = null;
		modifiedModelColors = null;
		originalModelColors = null;
		sizeX = 1;
		sizeY = 1;
		solid = true;
		walkable = true;
		actionsBoolean = false;
		adjustToTerrain = false;
		delayShading = false;
		aBoolean269 = false;
		animationId = -1;
		unknown4 = 16;
		lightAmbient = (byte) 0;
		lightDiffuse = (byte) 0;
		actions = null;
		icon = -1;
		mapScene = -1;
		rotated = false;
		castsShadow = true;
		modelSizeX = 128;
		modelSizeY = 128;
		modelSizeZ = 128;
		face = 0;
		translateX = 0;
		translateY = 0;
		translateZ = 0;
		unknown = false;
		unwalkableSolid = false;
		solidInt = -1;
		varbitId = -1;
		configId = -1;
		childrenIds = null;
	}


	public static final void reset() {
		GameObjectDefinition.bufferOffsets = null;
		GameObjectDefinition.cache = null;
		GameObjectDefinition.buffer = null;
	}

	public static final void load(ArchiveFile archive, Loader loader) throws IOException {
		//GameObjectDefinition.buffer = new Buffer(archive.getFile("loc.dat"));
		GameObjectDefinition.buffer = archive.entry("loc.dat").buffer();
		DataBuffer buffer = archive.entry("loc.idx").buffer();
		GameObjectDefinition.definitionCount = buffer.getShortUnsigned();
		GameObjectDefinition.bufferOffsets = new int[GameObjectDefinition.definitionCount];
		int offset = 2;
		for (int index = 0; index < GameObjectDefinition.definitionCount; index++) {
			loader.updateProgress("Loading game objects", definitionCount, index);
			GameObjectDefinition.bufferOffsets[index] = offset;
			offset += buffer.getShortUnsigned();
		}
		System.out.println("Indexed " + definitionCount + " Game Object definition offsets");
		GameObjectDefinition.cache = new GameObjectDefinition[20];
		for (int definition = 0; definition < 20; definition++) {
			GameObjectDefinition.cache[definition] = new GameObjectDefinition();
		}
	}

	/*public final GameObjectDefinition getChildDefinition() {
		int child = -1;
		if (varbitId != -1) {
			VarBit varbit = VarBit.cache[varbitId];
			int configId = varbit.configId;
			int leastSignificantBit = varbit.leastSignificantBit;
			int mostSignificantBit = varbit.mostSignificantBit;
			int bit = Game.BITFIELD_MAX_VALUE[mostSignificantBit - leastSignificantBit];
			child = GameObjectDefinition.client.widgetSettings[configId] >> leastSignificantBit & bit;
		} else if (configId != -1) {
			child = GameObjectDefinition.client.widgetSettings[configId];
		}
		if (child < 0 || child >= childrenIds.length || childrenIds[child] == -1) {
			return null;
		}
		return GameObjectDefinition.getDefinition(childrenIds[child]);
	}*/

	private final void load(DataBuffer buffer) {
		int hasActionsInt = -1;
		while (true) {
			int attributeId = buffer.getUnsigned();
			if (attributeId == 0) {
				break;
			}
			if (attributeId == 1) {
				int modelCount = buffer.getUnsigned();
				if (modelCount > 0) {
					if (modelIds == null || GameObjectDefinition.lowMemory) {
						modelTypes = new int[modelCount];
						modelIds = new int[modelCount];
						for (int model = 0; model < modelCount; model++) {
							modelIds[model] = buffer.getShortUnsigned();
							modelTypes[model] = buffer.getUnsigned();
						}
					} else {
						buffer.skip(modelCount * 3);
						//buffer.offset += modelCount * 3;
					}
				}
			} else if (attributeId == 2) {
				name = buffer.getString();
			} else if (attributeId == 3) {
				description = buffer.getString();
			} else if (attributeId == 5) {
				int modelCount = buffer.getUnsigned();
				if (modelCount > 0) {
					if (modelIds == null || GameObjectDefinition.lowMemory) {
						modelTypes = null;
						modelIds = new int[modelCount];
						for (int model = 0; model < modelCount; model++) {
							modelIds[model] = buffer.getShortUnsigned();
						}
					} else {
						buffer.skip(modelCount * 2);
						//buffer.offset += modelCount * 2;
					}
				}
			} else if (attributeId == 14) {
				sizeX = buffer.getUnsigned();
			} else if (attributeId == 15) {
				sizeY = buffer.getUnsigned();
			} else if (attributeId == 17) {
				solid = false;
			} else if (attributeId == 18) {
				walkable = false;
			} else if (attributeId == 19) {
				hasActionsInt = buffer.getUnsigned();
				if (hasActionsInt == 1) {
					actionsBoolean = true;
				}
			} else if (attributeId == 21) {
				adjustToTerrain = true;
			} else if (attributeId == 22) {
				delayShading = true;
			} else if (attributeId == 23) {
				aBoolean269 = true;
			} else if (attributeId == 24) {
				animationId = buffer.getShortUnsigned();
				if (animationId == 65535) {
					animationId = -1;
				}
			} else if (attributeId == 28) {
				unknown4 = buffer.getUnsigned();
			} else if (attributeId == 29) {
				lightAmbient = buffer.get();
			} else if (attributeId == 39) {
				lightDiffuse = buffer.get();
			} else if (attributeId >= 30 && attributeId < 39) {
				if (actions == null) {
					actions = new String[5];
				}
				actions[attributeId - 30] = buffer.getString();
				if (actions[attributeId - 30].equalsIgnoreCase("hidden")) {
					actions[attributeId - 30] = null;
				}
			} else if (attributeId == 40) {
				int modelColorCount = buffer.getUnsigned();
				modifiedModelColors = new int[modelColorCount];
				originalModelColors = new int[modelColorCount];
				for (int modelColor = 0; modelColor < modelColorCount; modelColor++) {
					modifiedModelColors[modelColor] = buffer.getShortUnsigned();
					originalModelColors[modelColor] = buffer.getShortUnsigned();
				}
			} else if (attributeId == 60) {
				icon = buffer.getShortUnsigned();
			} else if (attributeId == 62) {
				rotated = true;
			} else if (attributeId == 64) {
				castsShadow = false;
			} else if (attributeId == 65) {
				modelSizeX = buffer.getShortUnsigned();
			} else if (attributeId == 66) {
				modelSizeY = buffer.getShortUnsigned();
			} else if (attributeId == 67) {
				modelSizeZ = buffer.getShortUnsigned();
			} else if (attributeId == 68) {
				mapScene = buffer.getShortUnsigned();
			} else if (attributeId == 69) {
				face = buffer.getUnsigned();
			} else if (attributeId == 70) {
				translateX = buffer.getShort();
			} else if (attributeId == 71) {
				translateY = buffer.getShort();
			} else if (attributeId == 72) {
				translateZ = buffer.getShort();
			} else if (attributeId == 73) {
				unknown = true;
			} else if (attributeId == 74) {
				unwalkableSolid = true;
			} else if (attributeId == 75) {
				solidInt = buffer.getUnsigned();
			} else if (attributeId == 77) {
				varbitId = buffer.getShortUnsigned();
				if (varbitId == 65535) {
					varbitId = -1;
				}
				configId = buffer.getShortUnsigned();
				if (configId == 65535) {
					configId = -1;
				}
				int childrenCount = buffer.getUnsigned();
				childrenIds = new int[childrenCount + 1];
				for (int child = 0; child <= childrenCount; child++) {
					childrenIds[child] = buffer.getShortUnsigned();
					if (childrenIds[child] == 65535) {
						childrenIds[child] = -1;
					}
				}
			}
		}
		if (hasActionsInt == -1) {
			actionsBoolean = false;
			if (modelIds != null && (modelTypes == null || modelTypes[0] == 10)) {
				actionsBoolean = true;
			}
			if (actions != null) {
				actionsBoolean = true;
			}
		}
		if (unwalkableSolid) {
			solid = false;
			walkable = false;
		}
		if (solidInt != -1) {
			return;
		}
		solidInt = solid ? 1 : 0;
	}
}
