package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import kdtree.HPoint;
import kdtree.KDTree;
import kdtree.KeyDuplicateException;
import kdtree.KeyMissingException;
import kdtree.KeySizeException;

public class Program {
	private static final String IMAGE_FILE = "/Untitled.png";
	// This adds more colors to choose from, more = slower
	private static final float ACCURACY_COEF = 2f;

	public static void main(final String[] args) throws IOException {
		final JFrame frame = new JFrame();

		final BufferedImage image = GraphicsUtils.loadImage(IMAGE_FILE);
		final BufferedImage newImage = createNewImage(image);
		saveImage(newImage);

		@SuppressWarnings("serial")
		final JPanel panel = new JPanel() {
			@Override
			public void paintComponent(final Graphics g) {
				g.drawImage(newImage, 0, 0, null);
				g.drawImage(image, newImage.getWidth(), 0, null);
			}
		};
		panel.setPreferredSize(new Dimension(newImage.getWidth() * 2, newImage.getHeight()));

		frame.add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private static void saveImage(final BufferedImage image) {
		final File outputfile = new File("image.png");
		try {
			ImageIO.write(image, "png", outputfile);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Recreates the image using only unique pixels, starting from the left of the image.
	 *
	 * @param image
	 * @return
	 */
	private static BufferedImage createNewImage(final BufferedImage image) {
		System.out.println("Generating points");
		final List<Point> points = generateAllPoints(image.getWidth(), image.getHeight());

		System.out.println("Generating colors");
		final KDTree colors = generateAllColors((int) (image.getWidth() * image.getHeight() * ACCURACY_COEF));

		System.out.println("Number of points: " + points.size() + ", Number of colors: " + colors.size());

		return createNewImage(image, points, colors);
	}

	/**
	 * Creates the new image
	 *
	 * @param preImage
	 *            the original image
	 * @param points
	 * @param colors
	 * @return
	 */
	private static BufferedImage createNewImage(final BufferedImage preImage, final List<Point> points, KDTree colors) {
		final long start = System.currentTimeMillis();
		final BufferedImage newImage = GraphicsUtils.createImage(preImage.getWidth(), preImage.getHeight(), Transparency.OPAQUE);

		System.out.println("Creating image");
		int i = 1;
		long time = System.currentTimeMillis();
		// prints debug information and rebalances tree every so many iterations
		final int iterationsPerPrint = 5000;
		for (final Point p : points) {

			final Color c = getAndRemoveClosestColor(new Color(preImage.getRGB(p.x, p.y)), colors);
			newImage.setRGB(p.x, p.y, c.getRGB());

			if (i++ % iterationsPerPrint == 0) {
				final long timeTaken = System.currentTimeMillis() - time;
				final int remaining = points.size() - i;
				final long remainingMillis = (long) (remaining * (timeTaken / (double) iterationsPerPrint));
				System.out.println(remaining + " points remaining. Time taken for " + iterationsPerPrint + " iterations: " + timeTaken + " ms, or "
						+ (float) timeTaken / iterationsPerPrint + " ms per iteration. Projected remaining time: " + extractTime(remainingMillis)
						+ "\nTime elapsed: " + extractTime(System.currentTimeMillis() - start) + ", percent finished: " + (float) i / points.size()
						* 100f);
				time = System.currentTimeMillis();

				colors = colors.pruneAndRebalance(new HPoint(new int[] { c.getRed(), c.getGreen(), c.getBlue() }));
			}
		}
		System.out.println("Finished!");
		return newImage;
	}

	private static String extractTime(final long millis) {
		return String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(millis),
				TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
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

	/**
	 * Generates a list of all points in a rectangle
	 *
	 * @param width
	 * @param height
	 * @return
	 */
	private static List<Point> generateAllPoints(final int width, final int height) {
		final List<Point> points = new ArrayList<>(width * height);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				points.add(new Point(x, y));
			}
		}
		return points;
	}

	private static KDTree generateAllColors(final int i) {
		if (i > 255 * 255 * 255 && i >= 0) {
			throw new IllegalArgumentException();
		}
		final float perColor = (float) Math.cbrt(i);
		assert perColor >= 0 && perColor <= 255;
		System.out.println("Generating with this many per channel: " + perColor);

		final KDTree tree = new KDTree(3);

		final float step = 255f / perColor;

		for (float r = 0; r < 255; r += step) {
			for (float g = 0; g < 255; g += step) {
				for (float b = 0; b < 255; b += step) {
					try {
						tree.insert(new int[] { (int) r, (int) g, (int) b }, new Color((int) r, (int) g, (int) b));
					} catch (KeySizeException | KeyDuplicateException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return tree;
	}
}
