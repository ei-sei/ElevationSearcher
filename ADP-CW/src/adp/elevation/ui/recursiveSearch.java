package adp.elevation.ui;
//this class is wehre im trying to implement forkjoin (basically a copy of abstract searcher)


import adp.elevation.jar.Searcher;

import java.awt.image.BufferedImage;
import java.util.concurrent.RecursiveAction;

public class recursiveSearch extends RecursiveAction {

    private final BufferedImage raster;
    private final int side;
    private final double deviationThreshold;

    private final int firstPosition;
    private final int endPosition;

    private volatile int counter = 0;
    private volatile int currentPosition;

    public recursiveSearch( BufferedImage raster,  int side, final double deviationThreshold,
                           final int firstPosition, final int endPosition, int numberOfPositionsToTry) {

        this.raster = raster;
        this.side = side;
        this.deviationThreshold = deviationThreshold;
        this.firstPosition = 0;
        this.endPosition = (raster.getWidth() * raster.getHeight()) - 1;
        this.currentPosition = 0;
    }

    public final int numberOfPositionsToTry() {
        return this.endPosition - this.firstPosition;
    }


    public final int numberOfPositionsTriedSoFar() {
        return this.counter;
    }


    public void reset() {
        this.counter = 0;
        this.currentPosition = this.firstPosition;
    }


    public void runSearch(final Searcher.SearchListener listener) {
        this.reset();
        listener.information("SEARCHING...");
        final long startTime = System.currentTimeMillis();
        while (true) {
            final int foundMatch = this.findMatch(listener, startTime);
            if (foundMatch >= 0) {
                listener.possibleMatch(foundMatch, System.currentTimeMillis() - startTime,
                        numberOfPositionsTriedSoFar());
            } else {
                break;
            }
        }
        listener.information("Finished at " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s\n");
        // listener.information(this.counter + " positions attempted.");
    }

    private int findMatch(final Searcher.SearchListener listener, final long startTime) {
        while (this.counter < numberOfPositionsToTry()) {
            final boolean hit = tryPosition();
            this.currentPosition++;
            this.counter++;
            if (hit) {
                return this.currentPosition - 1;
            } else if (this.counter % 1000 == 0) {
                listener.update(this.currentPosition - 1, System.currentTimeMillis() - startTime,
                        numberOfPositionsTriedSoFar());
            }
        }
        return -1;
    }

    protected boolean tryPosition() {

        final int x1 = this.currentPosition % this.raster.getWidth();
        final int y1 = this.currentPosition / this.raster.getWidth();

//		System.out.println( "Position: " + this.currentPosition);
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;
        final double[] heights = new double[this.side * this.side];
        int count = 0;
        for (int x2 = 0; x2 < this.side; x2++) {
            if (x1 + x2 >= this.raster.getWidth()) {
                break;
            }
            for (int y2 = 0; y2 < this.side; y2++) {
                if (y1 + y2 >= this.raster.getHeight()) {
                    break;
                }

                // This code extracts the elevation data from the RGB data
                // according to the following formula, as specified by the
                // source of the example elevation data we are using.
                // height = -10000 + ((R * 256 * 256 + G * 256 + B) * 0.1)

                long elevation = this.raster.getRGB(x1 + x2, y1 + y2);
                elevation = elevation & 0xFFFFFFFFL; // mask off signed upper 32 bits
                double trueElevation = (long) ((elevation * 0.1) - 10000);
                heights[count++] = trueElevation;

                if (trueElevation >= max) {
                    max = trueElevation;
                }
                if (trueElevation <= min) {
                    min = trueElevation;
                }
//				if ( this.currentPosition == 10) {
//					System.out.print( (x1 + x2) + "," + (y1 + y2) + " == " + x2 + "," + y2 + " ----> ");
//				}
            }
        }

        final double stdev = standardDevPop(heights, count);
        // System.out.println( stdev + " (" + max + ", " + min + ")");
        return stdev < this.deviationThreshold;
    }

    /**
     * Calculates the standard deviation by population of the first {@code size}
     * elements of the given array.
     */
    private double standardDevPop(final double[] array, final int size) {
        double sum = 0;
        for (int i = 0; i < size; i++) {
            sum += array[i];
        }
        final double mean = sum / size;
        double variance = 0;
        for (int i = 0; i < size; i++) {
            final double dev = array[i] - mean;
            variance += dev * dev;
        }
        variance /= size;
        return Math.sqrt(variance);
    }


protected static int sThreshold = 1000;

    @Override
    protected void compute() {

//        if (numberOfPositionsToTry() < sThreshold) {
//            runSearch((final Searcher.SearchListener listener)
//
//            return;
//        }
//
//        int split = numberOfPositionsToTry() / 2;

//        invokeAll(new recursiveSearch(raster, firstPosition, split, endPosition),
//                new recursiveSearch(mSource, mStart + split, mLength - split,
//                        mDestination));
    }
}
