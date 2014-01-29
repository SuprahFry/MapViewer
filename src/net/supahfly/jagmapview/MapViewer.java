package net.supahfly.jagmapview;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.PixelGrabber;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JApplet;
import javax.swing.JFrame;

import net.supahfly.jagmapview.io.fs.IndexedFileSystem;
import net.supahfly.jagmapview.world.def.Floor;
import net.supahfly.jagmapview.world.def.GameObjectDefinition;
import net.supahfly.jagmapview.world.def.MapIndex;

public class MapViewer extends JApplet implements Runnable
{
	private static final long serialVersionUID = 101284373361096694L;
	private static String[] testFolders = { "../cache/", "./cache/" };
	private static IndexedFileSystem cache;
	public static int[] textureAverageColors;
	public static Image[] textures;
	private String folder = "../cache/";
	private Loader loaderPanel;
	private MapViewport viewport;
	
	private void gui()
	{
		setSize(765, 503);
		setBackground(Color.BLACK);
		loaderPanel = new Loader();
		viewport = new MapViewport();
		loaderPanel.setSize(getWidth(), getHeight());
		viewport.setSize(getWidth(), getHeight());
		setContentPane(loaderPanel);
		loaderPanel.repaint();
	}
	
	public boolean propogateCacheDirectory()
	{
		for (String f : testFolders)
		{
			File f2 = new File(f);
			
			if (f2.exists() && f2.isDirectory())
			{
				folder = f;
				return true;
			}
		}
		
		return false;
	}
	
	public static void main(String[] args)
	{
		JFrame frame = new JFrame("JMV - Supah Fly");
		MapViewer viewer = new MapViewer();
		viewer.init();
		frame.setContentPane(viewer);
		frame.setSize(viewer.getWidth(), viewer.getHeight());
		frame.setMinimumSize(viewer.getSize());
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	@Override
	public void init()
	{
		try
		{
			propogateCacheDirectory();
			gui();
			new Thread(this).start();
	    }
		catch (Exception ex)
		{
	        ex.printStackTrace();
	    }
	}
	
	public void loadTextures() throws Exception
	{	
		textures = new Image[255];
		textureAverageColors = new int[255];
		
		for (int i = 0; i < 255; i++)
		{
			File f2 = new File(folder + "textures/" + i + ".png");
			
			if (!f2.exists())
			{
				continue;
			}
			
			Image img = ImageIO.read(f2);
			textures[i] = img;
			int width = img.getWidth(this);
			int height = img.getHeight(this);
			int[] pixels = new int[width * height];
			PixelGrabber pixelgrabber = new PixelGrabber(img, 0, 0, width, height, pixels, 0, width);
			pixelgrabber.grabPixels();
			textureAverageColors[i] = averageColorForPixels(pixels);
		}
	}
	
	public int averageColorForPixels(int[] pixels)
	{
		int redTotal = 0;
		int greenTotal = 0;
		int blueTotal = 0;
		int totalPixels = pixels.length;
		
		for (int i = 0; i < totalPixels; i++)
		{
			if (pixels[i] == 0xff00ff)
			{
				totalPixels--;
				continue;
			}
			
			redTotal += pixels[i] >> 16 & 0xff;
			greenTotal += pixels[i] >> 8 & 0xff;
			blueTotal += pixels[i] & 0xff;
		}
		
		int averageRGB = (redTotal / totalPixels << 16) + (greenTotal / totalPixels << 8) + blueTotal / totalPixels;
		averageRGB = applyLightRGB(averageRGB, 1.4);
		
		if (averageRGB == 0)
		{
			averageRGB = 1;
		}
		
		return averageRGB;
	}
	
	public static int applyLightRGB(int rgb, double intensity) {
		double red = (rgb >> 16) / 256.0;
		double green = (rgb >> 8 & 0xff) / 256.0;
		double blue = (rgb & 0xff) / 256.0;
		red = Math.pow(red, intensity);
		green = Math.pow(green, intensity);
		blue = Math.pow(blue, intensity);
		int oRed = (int)(red * 256.0);
		int oGreen = (int)(green * 256.0);
		int oBlue = (int)(blue * 256.0);
		return (oRed << 16) + (oGreen << 8) + oBlue;
	}
    
	@Override
	public void run()
	{
		try
		{
			loaderPanel.updateProgress("Loading filesystem", 0);
			cache = new IndexedFileSystem(new File(folder));
			Thread.sleep(1500);
			loaderPanel.updateProgress("Loading game objects", 20);
			GameObjectDefinition.load(cache.getArchive(0, 2), loaderPanel);
			loaderPanel.updateProgress("Indexing maps", 40);
			MapIndex.load(cache.getArchive(0, 5));
			loaderPanel.updateProgress("Loading floor date", 60);
			Floor.load(cache.getArchive(0, 2));
			loaderPanel.updateProgress("Loading textures", 80);
			loadTextures();
			loaderPanel.updateProgress("Initializing map viewport", 100);
			//GameWorld.loadRegions(loaderPanel);
			viewport.centerView(new Point(3222, 3222));
			setContentPane(viewport);
			addKeyListener(viewport);
			addMouseListener(viewport);
			addMouseMotionListener(viewport);
			viewport.addKeyListener(viewport);
			viewport.addMouseMotionListener(viewport);
			viewport.addMouseListener(viewport);
			viewport.setFocusable(true);
			viewport.requestFocusInWindow();
			viewport.repaint();
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static IndexedFileSystem cache()
	{
		return cache;
	}
}
