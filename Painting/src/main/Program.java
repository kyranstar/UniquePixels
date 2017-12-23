package main;

import java.awt.Color;
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
import javax.swing.RowFilter.ComparisonType;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import kdtree.KDTree;
import kdtree.KeyDuplicateException;
import kdtree.KeySizeException;

public class Program {
	static Direction currentDirection = Direction.RIGHT;
	// This adds more colors to choose from, more = slower
	static float accuracy = 2f;
	private static BufferedImage image;
	static ImageIcon rightIcon;
	static ImageIcon leftIcon;
	private static JPanel panel;
	static ControlPanel controls;
	private static BufferedImage newImage;
	static JFrame frame;
	private static List<ImageTask> tasks = new ArrayList<>();
	public static ComparisonType comparisonType = ComparisonType.SINGLE_PIXEL;

	public static void main(final String[] args) throws Exception {
		// Set cross-platform Java L&F (also called "Metal")
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		frame = new JFrame();

		image = GraphicsUtils.createImage(50, 50, Transparency.OPAQUE);
		newImage = GraphicsUtils.createImage(image.getWidth(), image.getHeight(), Transparency.OPAQUE);

		panel = new JPanel();
		final JPanel images = new JPanel();
		leftIcon = new ImageIcon(newImage);
		images.add(new JLabel(leftIcon));
		rightIcon = new ImageIcon(image);
		images.add(new JLabel(rightIcon));
		controls = new ControlPanel((e) -> createNewImage(image, newImage, panel), (b) -> setImage(b));
		final JSplitPane horizontalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, controls, images);
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
		newImage = GraphicsUtils.createImage(image.getWidth(), image.getHeight(), Transparency.OPAQUE);
		leftIcon.setImage(newImage);
		rightIcon.setImage(image);
		frame.pack();
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

		final ImageTask task = new ImageTask(image, result, points, colors, panel, controls, comparisonType);
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

	enum Direction {
		UP,
		DOWN,
		LEFT,
		RIGHT,
		SPIRAL_OUT,
		SPIRAL_IN;
	}
	static enum ComparisonType {
		SINGLE_PIXEL,
		AVERAGE_PRE_3x3, // the average of all pixels in 3x3 area in the input
		AVERAGE_POST_5x5, // the average of all already set pixels in a 5x5 area of the output
		RANDOM_5x5,
		
	}
}
