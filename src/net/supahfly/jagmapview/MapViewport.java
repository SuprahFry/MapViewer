package net.supahfly.jagmapview;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import net.supahfly.jagmapview.world.GameTile;
import net.supahfly.jagmapview.world.GameWorld;
import net.supahfly.jagmapview.world.def.Floor;
import net.supahfly.jagmapview.world.map.TileFlags;

public class MapViewport extends Container implements KeyListener, MouseListener, MouseMotionListener
{
	private static final long serialVersionUID = -101301056032356846L;
	private Point center = new Point(3200, 3432);
	private boolean drawTextures = false;
	private Point view = null;
	private Point mouse = null;
	private int plane = 0;
	private int size = 4;
	private double angle = 0;
	private final Runtime runtime = Runtime.getRuntime();
	private boolean modifyBrightness = true;
	private boolean renderBridgeMode = true;
	private boolean drawInfo = true;
	private boolean drawDebug = true;
	
	public void centerView(Point center)
	{
		this.center = center;
		this.view = new Point(center.x() - (getWidth() / size / 2), center.y() + (getHeight() / size / 2));
	}
	
	@Override
	public void paint(Graphics g)
	{
		for (int x = 0; x < getWidth(); x += size) // 160 = width, height aka 10 tiles
		{
			for (int y = 0; y < getHeight(); y += size)
			{
				int lX = x / size;
				int lY = y / size;
				
				int absX = view.x() + lX;
				int absY = view.y() - lY;
				int drawX = (int)(x * Math.cos(angle) - y * Math.sin(angle));
				int drawY = (int)(x * Math.sin(angle) + y * Math.cos(angle));
				
				renderTile(g, drawX, drawY, lX, lY, absX, absY, plane);
				//g.setColor(Color.ORANGE);
				//g.drawString(lX + "," + lY, drawX, drawY);
				//g.setColor(new Color(0 + lY + (lX / 2), 0 + lX, 0 + lY));
			}
		}
		
		if (drawDebug)
		{
			g.setColor(Color.YELLOW);
			int y = 15;
			int usage = (int) ((runtime.totalMemory() - runtime.freeMemory()) / 1024L);
			g.drawString("Top left: " + view + ", plane: " + plane, 5, y);
			g.drawString("Center: " + center, 5, y += 15);
			g.drawString("Angle: " + angle + ", size: " + size + ", brightness: " + modifyBrightness + ", drawTextures: " + drawTextures + ", renderBridgeMode: " + renderBridgeMode, 5, y += 15);
			g.drawString("Memory: " + usage + "k (" + (usage / 1024L) + "MB)", 5, y += 15);
			g.drawString("Mouse: " + mouse, 5, y += 15);
		}
		
		if (mouse != null && drawInfo)
		{
			g.setColor(Color.WHITE);
			int y = getHeight() + 10;
			GameTile tile = GameWorld.tile(new Position(view.x() + mouse.x(), view.y() - mouse.y(), plane));

			if (tile.underlayID() > 0)
			{
				g.drawString("Underlay: " + Floor.cache[tile.underlayID() - 1], 5, y -= 15);
			}
			
			if (tile.overlayID() > 0)
			{
				g.drawString("Overlay: " + Floor.cache[tile.overlayID() - 1], 5, y -= 15);
			}
			
			g.drawString(tile + "", 5, y -= 15);
		}
	}
	
	public void renderTile(Graphics g, int x, int y, int lX, int lY, int absX, int absY, int plane)
	{
		try
		{
			GameWorld.loadRegionAbs(absX, absY);
			GameTile tile = GameWorld.tile(new Position(absX, absY, plane));
			float brightnessModifier = (float)tile.height() / 2048f;
			
			if (!modifyBrightness)
			{
				brightnessModifier = 0;
			}
			
			if (tile.flags().has(TileFlags.ROOF) && plane == 0)
			{
				// has roof above
				//g.setColor(Color.BLACK);
				//g.drawString("x", x, y);
				//renderTile(g, x, y, lX, lY, absX, absY, plane + 1);
				//return;
			}
			
			boolean hideUnderlay = false;
			
			if (plane > 0)
			{
				hideUnderlay = true;
				
				if (tile.overlayID() > 0 && !Floor.cache[tile.overlayID() - 1].occlude)
				{
					hideUnderlay = false;
				}
			}
			
			if (tile.underlayID() > 0 && !hideUnderlay)
			{
				Floor underlay = Floor.cache[tile.underlayID() - 1];
				
				if (underlay.textureID != -1)
				{
					if (drawTextures)
					{
						g.drawImage(MapViewer.textures[underlay.textureID], x, y, size, size, this);
					}
					else
					{
						g.setColor(new Color(MapViewer.textureAverageColors[underlay.textureID]));
						g.fillRect(x, y, size, size);
					}
				}
				else if (underlay.rgbColor != 16711935)
				{
					Color c = new Color(underlay.rgbColor);
					float[] f = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
					f[2] += brightnessModifier;
					//System.out.println((float)Math.abs(tile.height()) / 2048f);
					g.setColor(new Color(Color.HSBtoRGB(f[0], f[1], f[2])));
					//g.setColor(new Color(MapViewer.applyLightRGB(underlay.rgbColor, (float)Math.abs(tile.height()) / 2048f)));
					g.fillRect(x, y, size, size);
				}
			}
			
			if (tile.overlayID() > 0)
			{
				Floor overlay = Floor.cache[tile.overlayID() - 1];
				
				if (overlay.textureID != -1)
				{
					if (drawTextures)
					{
						g.drawImage(MapViewer.textures[overlay.textureID], x, y, size, size, this);
					}
					else
					{
						g.setColor(new Color(MapViewer.textureAverageColors[overlay.textureID]));
						g.fillRect(x, y, size, size);
					}
				}
				else if (overlay.rgbColor != 16711935)
				{
					Color c = new Color(overlay.rgbColor);
					float[] f = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
					f[2] += brightnessModifier;
					//System.out.println((float)Math.abs(tile.height()) / 2048f);
					g.setColor(new Color(Color.HSBtoRGB(f[0], f[1], f[2])));
					g.fillRect(x, y, size, size);
				}
			}
			
			if (tile.flags().has(TileFlags.UNWALKABLE))
			{
				//g.setColor(Color.WHITE);
				//g.drawString(".", x, y);
			}
			
			if (renderBridgeMode && (GameWorld.tile(new Position(absX, absY, plane + 1)).flags().has(TileFlags.BRIDGE)))
			{
				renderTile(g, x, y, lX, lY, absX, absY, plane + 1);
				//g.setColor(Color.GREEN);
				//g.drawString("-", x, y);
			}
			
			if (mouse != null && mouse.equals(new Point(lX, lY)))
			{
				g.setColor(new Color(0, 0, 0, 64));
				g.fillRect(x, y, size, size);
				g.setColor(new Color(0, 0, 0, 128));
				g.drawRect(x, y, size - 1, size - 1);
			}
		}
		catch (Exception e)
		{
			//e.printStackTrace();
			g.setColor(Color.BLACK);
			g.fillRect(x, y, size, size);
			//g.setColor(Color.DARK_GRAY);
			//g.drawString("-", x, y);
		}
	}

	@Override
	public void keyPressed(KeyEvent arg0)
	{
		switch (arg0.getKeyChar())
		{
			case '+':
				size++;
				centerView(center);
				break;
			case '-':
				size = size <= 0 ? 1 : size - 1;
				centerView(center);
				break;
			case 'q':
				angle -= 0.005;
				break;
			case 'e':
				angle += 0.005;
				break;
			case 'z':
				angle = 0;
				break;
			case 'r':
				plane = plane >= 3 ? 3 : plane + 1;
				break;
			case 'f':
				plane = plane <= 0 ? 0 : plane - 1;
				break;
			case 'b':
				modifyBrightness = !modifyBrightness;
				break;
			case 't':
				drawTextures = !drawTextures;
				break;
			case 'v':
				renderBridgeMode = !renderBridgeMode;
				break;
			case 'i':
				drawInfo = !drawInfo;
				break;
			case 'm':
				drawDebug = !drawDebug;
				break;
		}
		
		switch (arg0.getKeyCode())
		{
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_D:
				centerView(center.step(10 - size + 1, 0));
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_A:
				centerView(center.step(-(10 - size + 1), 0));
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_S:
				centerView(center.step(0, -(10 - size + 1)));
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				centerView(center.step(0, 10 - size + 1));
				break;
		}
		
		repaint();
	}

	@Override
	public void keyReleased(KeyEvent arg0)
	{
		//System.out.println(arg0);
	}

	@Override
	public void keyTyped(KeyEvent arg0)
	{
		//System.out.println(arg0);
	}

	@Override
	public void mouseClicked(MouseEvent arg0)
	{
		Point click = new Point(arg0.getX() / size, arg0.getY() / size);
		//Point click = new Point(arg0.getX() / size, (getHeight() - arg0.getY()) / size);
		GameTile tile = GameWorld.tile(new Position(view.x() + click.x(), view.y() - click.y(), plane));
		
		System.out.println("------");
		System.out.println("Click: " + click);
		System.out.println(tile);
		
		if (tile.underlayID() > 0)
		{
			System.out.println("Underlay: " + Floor.cache[tile.underlayID() - 1]);
		}
		
		if (tile.overlayID() > 0)
		{
			System.out.println("Overlay: " + Floor.cache[tile.overlayID() - 1]);
		}
	}

	@Override
	public void mouseEntered(MouseEvent arg0)
	{
		
	}

	@Override
	public void mouseExited(MouseEvent arg0)
	{
		
	}

	@Override
	public void mousePressed(MouseEvent arg0)
	{
		
	}

	@Override
	public void mouseReleased(MouseEvent arg0)
	{
		
	}

	@Override
	public void mouseDragged(MouseEvent arg0)
	{
		
	}

	@Override
	public void mouseMoved(MouseEvent arg0)
	{
		Point m = new Point(arg0.getX() / size, arg0.getY() / size);
		
		if (mouse != m)
		{
			mouse = m;
			repaint();
		}
	}
}
