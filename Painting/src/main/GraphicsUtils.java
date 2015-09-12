package main;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * The Class GraphicsUtils.
 */
public final class GraphicsUtils {

	/** The Constant GFX_CONFIG. */
	private static final GraphicsConfiguration GFX_CONFIG = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
			.getDefaultConfiguration();

	// Utility class, cannot instantiate
	private GraphicsUtils() {
	}

	/**
	 * Takes an image and makes a compatible version.
	 *
	 * @param image
	 *            the image
	 * @return the buffered image
	 */
	public static BufferedImage toCompatibleImage(final BufferedImage image) {
		/*
		 * if image is already compatible and optimized for current system settings, simply return it
		 */
		if (image.getColorModel().equals(GFX_CONFIG.getColorModel())) {
			return image;
		}

		// image is not optimized, so create a new image that is
		final BufferedImage new_image = GFX_CONFIG.createCompatibleImage(image.getWidth(), image.getHeight(), image.getTransparency());

		// get the graphics context of the new image to draw the old image on
		final Graphics2D g2d = (Graphics2D) new_image.getGraphics();

		// actually draw the image and dispose of context no longer needed
		g2d.drawImage(image, 0, 0, null);
		g2d.dispose();

		// return the new optimized image
		return new_image;
	}

	/**
	 * Creates a compatible image given the parameters.
	 *
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 * @param transparency
	 *            the transparency
	 * @return the buffered image
	 */
	public static BufferedImage createImage(final int width, final int height, final int transparency) {
		BufferedImage image = GFX_CONFIG.createCompatibleImage(width, height, transparency);
		if (image.getRaster().getDataBuffer().getDataType() != DataBuffer.TYPE_INT) {
			switch (transparency) {
			case Transparency.TRANSLUCENT:
				image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				break;
			case Transparency.OPAQUE:
				image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				break;
			case Transparency.BITMASK:
				image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
				break;
			default:
				break;
			}
		}
		return image;
	}

	public static BufferedImage loadImage(final String filename) throws IOException {
		final InputStream in = GraphicsUtils.class.getResourceAsStream(filename);
		return toCompatibleImage(ImageIO.read(in));
	}

}