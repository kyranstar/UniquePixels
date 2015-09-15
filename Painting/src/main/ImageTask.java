package main;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import kdtree.HPoint;
import kdtree.KDTree;
import kdtree.KeyMissingException;
import kdtree.KeySizeException;

public class ImageTask implements Runnable {

	private boolean stop;
	private final BufferedImage preImage;
	private final BufferedImage result;
	private final List<Point> points;
	private KDTree colors;
	private final JPanel panel;
	private final ControlPanel controls;

	public ImageTask(final BufferedImage preImage, final BufferedImage result, final List<Point> points, final KDTree colors, final JPanel panel,
			final ControlPanel controls) {
		this.preImage = preImage;
		this.result = result;
		this.points = points;
		this.colors = colors;
		this.panel = panel;
		this.controls = controls;
	}

	@Override
	public void run() {
		System.out.println("Creating image");
		int i = 1;
		// prints debug information and rebalances tree every so many iterations
		final int iterationsPerPrint = 100;
		final int iterationsPerPrune = 2500;
		for (final Point p : points) {
			if (isStopped()) {
				return;
			}

			final Color c = getAndRemoveClosestColor(new Color(preImage.getRGB(p.x, p.y)), colors);
			result.setRGB(p.x, p.y, c.getRGB());

			if (i % iterationsPerPrint == 0) {
				synchronized (controls) {
					controls.setCompletion((float) i / points.size() * 100f);
				}
				panel.repaint();
			}
			if (i % iterationsPerPrune == 0) {
				colors = colors.pruneAndRebalance(new HPoint(new int[] { c.getRed(), c.getGreen(), c.getBlue() }));
			}
			i++;
		}
		System.out.println("Finished!");
		saveImage(result);
	}

	/**
	 * Finds the color most similar to a specified color in the tree, removes it, and returns it.
	 *
	 * @param color
	 * @param colors
	 * @return
	 */
	private static Color getAndRemoveClosestColor(final Color color, final KDTree colors) {
		try {
			final Color nearest = (Color) colors.nearest(new int[] { color.getRed(), color.getGreen(), color.getBlue() });
			colors.delete(new int[] { nearest.getRed(), nearest.getGreen(), nearest.getBlue() });
			return nearest;
		} catch (final KeySizeException | KeyMissingException e) {
			throw new RuntimeException(e);
		}
	}

	private static void saveImage(final BufferedImage image) {
		final File outputfile = new File("image.png");
		try {
			ImageIO.write(image, "png", outputfile);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized boolean isStopped() {
		synchronized (this) {
			return stop;
		}
	}

	public void setStop(final boolean stop) {
		synchronized (this) {
			this.stop = stop;
		}
	}

}
