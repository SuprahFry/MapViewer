package net.supahfly.jagmapview;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JPanel;

public class Loader extends JPanel
{
	private static final long serialVersionUID = -9017388733640845424L;
	
	private String status = "Loading, please wait";
	private int progress = 0;
	
	public void updateProgress(String status, int progress)
	{
		this.status = status;
		this.progress = progress > 100 ? 100 : progress;
		repaint();
	}
	
	public void updateProgress(String status, int amount, int completed)
	{
		int percent = Math.round(((float)completed / (float)amount) * 100F);
		updateProgress(status + " (" + percent + "%)", percent);
	}
	
	@Override
	public void paint(Graphics g)
	{
		super.paint(g);
		Font font = new Font("Helvetica", 1, 13);
		g.setFont(font);
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(new Color(140, 17, 17));
		int midX = getWidth() / 2;
		int midY = getHeight() / 2;
		int width = 350;
		int height = 40;
		int rectOffX = midX - width / 2;
		int rectOffY = midY - height / 2;
		g.drawRect(rectOffX, rectOffY, width, height);
		width -= 3;
		height -= 3;
		rectOffX = midX - width / 2;
		rectOffY = midY - height / 2;
		int progressWidth = (int)Math.ceil(((double)width / 100D) * (double)progress);
		
		if(progressWidth > width)
		{
			progressWidth = width;
		}
		
		g.fillRect(rectOffX, rectOffY, progressWidth, height);
		g.setColor(Color.WHITE);
		FontMetrics fontmetrics = g.getFontMetrics(g.getFont());
		int l1 = fontmetrics.stringWidth(status);
		g.drawString(status, midX - l1 / 2, midY + 5);
	}
}
