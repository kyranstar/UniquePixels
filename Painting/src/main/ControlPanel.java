package main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import main.Program.ComparisonType;
import main.Program.Direction;

@SuppressWarnings("serial")
public class ControlPanel extends JPanel {
	private final JLabel percent;

	public ControlPanel(final ActionListener runListener, final Consumer<BufferedImage> loadImageFunc) {
		final JButton pickImageButton = new JButton("Pick image");
		pickImageButton.addActionListener((e) -> {
			final BufferedImage image = pickImage();
			if (image != null) {
				loadImageFunc.accept(image);
			}
		});
		add(pickImageButton);

		final JComboBox<Direction> directions = new JComboBox<Direction>(Direction.values());
		directions.setSelectedIndex(Arrays.asList(Direction.values()).indexOf(Program.currentDirection));
		directions.addActionListener((e) -> handleChangeDirection(e));
		add(new JLabel("Direction:"));
		add(directions);

		final JComboBox<ComparisonType> comparisonMode = new JComboBox<ComparisonType>(ComparisonType.values());
		comparisonMode.setSelectedIndex(Arrays.asList(ComparisonType.values()).indexOf(Program.comparisonType));
		comparisonMode.addActionListener((e) -> handleChangeComparisonType(e));
		add(new JLabel("Comparison Type:"));
		add(comparisonMode);

		final JComboBox<Float> accuracies = new JComboBox<Float>(new Float[] { .25f, .5f, 1f, 2f, 3f, 4f, 5f });
		accuracies.setSelectedIndex(3);
		accuracies.addActionListener((e) -> handleChangeAccuracy(e));
		add(new JLabel("Accuracy (bigger is slower, but more accurate):"));
		add(accuracies);

		final JButton runButton = new JButton("Run");
		runButton.addActionListener(runListener);
		add(runButton);

		percent = new JLabel();
		add(percent);
	}

	@SuppressWarnings("unchecked")
	private void handleChangeComparisonType(ActionEvent e) {
		Program.comparisonType = (ComparisonType) ((JComboBox<ComparisonType>) e.getSource()).getSelectedItem();
	}

	@SuppressWarnings("unchecked")
	private void handleChangeAccuracy(final ActionEvent e) {
		Program.accuracy = (Float) ((JComboBox<Float>) e.getSource()).getSelectedItem();
	}

	@SuppressWarnings("unchecked")
	private void handleChangeDirection(final ActionEvent e) {
		Program.currentDirection = (Direction) ((JComboBox<Direction>) e.getSource()).getSelectedItem();
	}

	private BufferedImage pickImage() {
		final JFileChooser fc = new JFileChooser();
		final int returnVal = fc.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			final File file = fc.getSelectedFile();
			try {
				final BufferedImage image = ImageIO.read(file);
				if (image.getWidth() * image.getHeight() > 255 * 255 * 255) {
					JOptionPane.showMessageDialog(null, "Image cannot have an area of over 255 * 255 * 255!");
					return null;
				}
				return image;
			} catch (final IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}

	public void setCompletion(final float f) {
		percent.setText(String.format("Completion: %.3f%%", f));
	}
}
