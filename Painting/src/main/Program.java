package main;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import kdtree.KDTree;
import kdtree.KeyDuplicateException;
import kdtree.KeySizeException;

public class Program {
	static Direction currentDirection = Direction.RIGHT;
	private static final String DEFAULT_IMAGE_FILE = "/Untitled.png";
	// This adds more colors to choose from, more = slower
	static float accuracy = 2f;
	private static BufferedImage image;
	static ImageIcon rightIcon;
	private static JPanel panel;
	static ControlPanel controls;
	private static BufferedImage newImage;
	private static List<ImageTask> tasks = new ArrayList<>();

	public static void main(final String[] args) throws IOException {
		try {
			// Set cross-platform Java L&F (also called "Metal")
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final UnsupportedLookAndFeelException e) {
			// handle exception
		} catch (final ClassNotFoundException e) {
			// handle exception
		} catch (final InstantiationException e) {
			// handle exception
		} catch (final IllegalAccessException e) {
			// handle exception
		}

		final JFrame frame = new JFrame();

		image = GraphicsUtils.loadImage(DEFAULT_IMAGE_FILE);
		newImage = GraphicsUtils.createImage(image.getWidth(), image.getHeight(), Transparency.OPAQUE);

		panel = new JPanel();
		final JPanel images = new JPanel();
		images.add(new JLabel(new ImageIcon(newImage)));
		rightIcon = new ImageIcon(image);
		images.add(new JLabel(rightIcon));
		controls = new ControlPanel((e) -> createNewImage(image, newImage, panel), (b) -> setImage(b));
		final JSplitPane horizontalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, images, controls);
		horizontalSplit.setDividerSize(0);
		panel.add(horizontalSplit);

		frame.add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	private static void setImage(final BufferedImage b) {
		image = b;
		clearAndStop();
	}

	private static void clearAndStop() {
		rightIcon.setImage(image);
		final Graphics g = newImage.getGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());
		panel.repaint();

		// stop other tasks
		for (final ImageTask task : tasks) {
			task.setStop(true);
		}
	}

	/**
	 * Recreates the image using only unique pixels, starting from the left of the image.
	 *
	 * @param image
	 * @return
	 */
	private static void createNewImage(final BufferedImage image, final BufferedImage result, final JPanel panel) {
		System.out.println("Generating points");
		final List<Point> points = generateAllPoints(image.getWidth(), image.getHeight(), currentDirection);

		System.out.println("Generating colors");
		final KDTree colors = generateAllColors((int) (image.getWidth() * image.getHeight() * accuracy), image.getWidth() * image.getHeight());

		System.out.println("Number of points: " + points.size() + ", Number of colors: " + colors.size());

		clearAndStop();

		final ImageTask task = new ImageTask(image, result, points, colors, panel, controls);
		final Thread thread = new Thread(task);
		tasks.add(task);
		thread.start();
	}

	/**
	 * Generates a list of all points in a rectangle
	 *
	 * @param width
	 * @param height
	 * @param dir
	 * @return
	 */
	private static List<Point> generateAllPoints(final int width, final int height, final Direction dir) {
		final List<Point> points = new ArrayList<>(width * height);

		switch (dir) {
		case RIGHT:
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					points.add(new Point(x, y));
				}
			}
			break;
		case LEFT:
			for (int x = width - 1; x >= 0; x--) {
				for (int y = 0; y < height; y++) {
					points.add(new Point(x, y));
				}
			}
			break;
		case DOWN:
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					points.add(new Point(x, y));
				}
			}
			break;
		case UP:
			for (int y = height - 1; y >= 0; y--) {
				for (int x = 0; x < width; x++) {
					points.add(new Point(x, y));
				}
			}
			break;

		case SPIRAL_OUT:
			spiralIn(width, height, points);
			Collections.reverse(points);
			break;
		case SPIRAL_IN:
			spiralIn(width, height, points);
			break;
		}
		return points;
	}

	private static void spiralIn(final int width, final int height, final List<Point> points) {
		int x = width - 1;
		int y = 0;
		int left = width;
		int right = width - 1;
		int up = height - 2;
		int down = height - 1;
		int counter = width * height;

		while (counter > 0) {
			for (int i = left; i > 0; i--) {
				points.add(new Point(x, y));
				counter--;
				x--;
			}
			left -= 2;
			y++;
			x++;
			for (int i = down; i > 0; i--) {
				points.add(new Point(x, y));
				counter--;
				y++;
			}
			down -= 2;
			y--;
			x++;
			for (int i = right; i > 0; i--) {
				points.add(new Point(x, y));
				counter--;
				x++;
			}
			right -= 2;
			y--;
			x--;
			for (int i = up; i > 0; i--) {
				points.add(new Point(x, y));
				counter--;
				y--;
			}
			up -= 2;
			y++;
			x--;
		}
	}

	private static KDTree generateAllColors(int i, final int pixels) {
		if (i > 255 * 255 * 255) {
			i = 255 * 255 * 255;
		} else if (i < pixels) {
			i = pixels;
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

	static enum Direction {
		UP,
		DOWN,
		LEFT,
		RIGHT,
		SPIRAL_OUT,
		SPIRAL_IN;
	}
}
