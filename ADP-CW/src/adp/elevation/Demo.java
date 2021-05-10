package adp.elevation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import adp.elevation.jar.BasicSearcher;
import adp.elevation.jar.Searcher;
import adp.elevation.jar.Searcher.SearchListener;

/**
 * This class implements {@link Searcher.SearchListener} and emits all messages
 * on the command line output.
 */
public class Demo implements SearchListener {

	public Demo(final File file) throws IOException {
		final BufferedImage raster = ImageIO.read(file);
		final Searcher searcher = new BasicSearcher(raster, Configuration.side, Configuration.deviationThreshold);// ,
																													// image2);
		searcher.runSearch(this);

	}

	@Override
	public void information(final String message) {
		System.out.println(message);
	}

	@Override
	public void possibleMatch(final int position, final long elapsedTime, final long numberOfPositionsTriedSoFar) {
		System.out.println("Possible match at: " + position + " at " + (elapsedTime / 1000.0) + "s ("
				+ numberOfPositionsTriedSoFar + " positions attempted)");
	}

	@Override
	public void update(final int position, final long elapsedTime, final long numberOfPositionsTriedSoFar) {
		System.out.println("Searching at: " + position + " at " + (elapsedTime / 1000.0) + "s ("
				+ numberOfPositionsTriedSoFar + " positions attempted)");
	}

	/**
	 * Set the file names to search rasters of different sizes.
	 *
	 * @param args
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {
		final File file = new File("rgbelevation/smallelevation.png");
		//final File file = new File( "rgbelevation/bigelevation.png");
		new Demo(file);
	}

}
