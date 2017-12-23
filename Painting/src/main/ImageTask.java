package main;

import java.awt.Color;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import kdtree.HPoint;
import kdtree.KDTree;
import kdtree.KeyMissingException;
import kdtree.KeySizeException;
import main.Program.ComparisonType;

public class ImageTask implements Runnable {

	private boolean stop;
	private final BufferedImage preImage;
	private final BufferedImage result;
	private final List<Point> points;
	private KDTree colors;
	private final JPanel panel;
	private final ControlPanel controls;
	private ComparisonType comparisonType;
	private Random rand = new Random();
	boolean[][] setPixel;

	public ImageTask(final BufferedImage preImage, final BufferedImage result, final List<Point> points,
			final KDTree colors, final JPanel panel, final ControlPanel controls, ComparisonType comparisonType) {
		this.preImage = preImage;
		this.result = result;
		this.points = points;
		this.colors = colors;
		this.panel = panel;
		this.controls = controls;
		this.comparisonType = comparisonType;
		setPixel = new boolean[preImage.getHeight()][preImage.getWidth()];
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

			final Color c = getAndRemoveClosestColor(p, colors);
			result.setRGB(p.x, p.y, c.getRGB());
			setPixel[p.y][p.x] = true;

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
	 * Finds the color most similar to a specified color in the tree, removes
	 * it, and returns it.
	 *
	 * @param color
	 * @param colors
	 * @return
	 */
	private Color getAndRemoveClosestColor(final Point p, final KDTree colors) {
		try {
			Color average = null;
			switch (comparisonType) {
			case SINGLE_PIXEL:
				average = new Color(preImage.getRGB(p.x, p.y));
				break;
			case AVERAGE_PRE_3x3: {
				int reds = 0;
				int greens = 0;
				int blues = 0;
				for (int y = -1; y <= 1; y++) {
					for (int x = -1; x <= 1; x++) {
						int posX = p.x + x;
						int posY = p.y + y;
						if (posX < 0 || posY < 0 || posX >= preImage.getWidth() || posY >= preImage.getHeight()) {
							continue;
						}

						Color color = new Color(preImage.getRGB(posX, posY));
						reds += color.getRed();
						greens += color.getGreen();
						blues += color.getBlue();

					}
				}
				average = new Color(reds / 9, greens / 9, blues / 9);
			}
				break;
			case AVERAGE_POST_5x5: {
				final float NEIGHBOR_WEIGHT = 0.5f;
				int reds = 0;
				int greens = 0;
				int blues = 0;
				int i = 0;
				for (int y = -2; y <= 2; y++) {
					for (int x = -2; x <= 2; x++) {
						int posX = p.x + x;
						int posY = p.y + y;
						if (posX < 0 || posY < 0 || posX >= preImage.getWidth() || posY >= preImage.getHeight()) {
							continue;
						}

						if (setPixel[posY][posX]) {
							i++;
							Color color = new Color(result.getRGB(posX, posY));
							reds += color.getRed();
							greens += color.getGreen();
							blues += color.getBlue();
						}
					}
				}
				if (i == 0) {
					// if first pixel, do single pixel method
					average = new Color(preImage.getRGB(p.x, p.y));
				} else {
					int neighborAverageRed = (int) (reds / i * NEIGHBOR_WEIGHT);
					int neighborAverageGreen = (int) (greens / i * NEIGHBOR_WEIGHT);
					int neighborAverageBlue = (int) (blues / i * NEIGHBOR_WEIGHT);

					Color center = new Color(preImage.getRGB(p.x, p.y));
					int centerRed = (int) (center.getRed() * (1f - NEIGHBOR_WEIGHT));
					int centerGreen = (int) (center.getGreen() * (1f - NEIGHBOR_WEIGHT));
					int centerBlue = (int) (center.getBlue() * (1f - NEIGHBOR_WEIGHT));

					average = new Color(neighborAverageRed + centerRed, neighborAverageGreen + centerGreen,
							neighborAverageBlue + centerBlue);

				}
			}
				break;
			case RANDOM_5x5: {
				int numToAverage = 5 * 5 / 2;
				int reds = 0;
				int greens = 0;
				int blues = 0;

				for (int i = 0; i < numToAverage; i++) {
					int y = rand.nextInt(5) - 2;
					int x = rand.nextInt(5) - 2;
					int posX = p.x + x;
					int posY = p.y + y;
					if (posX < 0 || posY < 0 || posX >= preImage.getWidth() || posY >= preImage.getHeight()) {
						continue;
					}

					Color color = new Color(preImage.getRGB(posX, posY));
					reds += color.getRed();
					greens += color.getGreen();
					blues += color.getBlue();
				}
				average = new Color(reds / numToAverage, greens / numToAverage, blues / numToAverage);
			}
				break;
			}

			Color nearest = (Color) colors
					.nearest(new int[] { average.getRed(), average.getGreen(), average.getBlue() });
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
