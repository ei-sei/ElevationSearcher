package adp.elevation.ui;

import adp.elevation.Configuration;
import adp.elevation.jar.BasicSearcher;
import adp.elevation.jar.Searcher;
import adp.elevation.jar.Searcher.SearchListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a basic GUI interface for Searcher
 * implementations. This class implements SearchListener to
 * receive the Searcher's output information.
 */
public class SearchUI extends JFrame implements SearchListener {
    private static final long serialVersionUID = 1L;

    private final JButton openBigButton = new JButton("Open elevation data");
    private final JLabel mainFilenameLabel = new JLabel();

    private final ImagePanel mainImagePanel = new ImagePanel();

    private final JFileChooser chooser = new JFileChooser();

    private final JLabel outputLabel = new JLabel("information");
    private final JButton startButton = new JButton("Start");

    private Searcher searcher;

    private BufferedImage raster;

    /**
     * Construct an SearchUI and set it visible.
     */
    public SearchUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // kill the application on closing the window

        final JPanel mainFilePanel = new JPanel(new BorderLayout());
        mainFilePanel.add(this.openBigButton, BorderLayout.WEST);
        mainFilePanel.add(this.mainFilenameLabel, BorderLayout.CENTER);

        final JPanel topPanel = new JPanel(new GridLayout(0, 1));
        topPanel.add(mainFilePanel);

        final JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.add(this.mainImagePanel, BorderLayout.CENTER);

        final JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(this.outputLabel, BorderLayout.CENTER);
        bottomPanel.add(this.startButton, BorderLayout.SOUTH);

        final JPanel mainPanel = new JPanel(new BorderLayout());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(imagePanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        this.openBigButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                if (SearchUI.this.chooser.showOpenDialog(SearchUI.this) == JFileChooser.APPROVE_OPTION) {
                    final File file = SearchUI.this.chooser.getSelectedFile();
                    SearchUI.this.mainFilenameLabel.setText(file.getName());
                    try {
                        SearchUI.this.raster = ImageIO.read(file);
                    } catch (final IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    SearchUI.this.mainImagePanel.resetHighlights();
                    SearchUI.this.mainImagePanel.setImage(SearchUI.this.raster);
                    pack();
                    SearchUI.this.mainImagePanel.repaint();
                }
            }
        });

        this.startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                runSearch();
            }
        });

        this.chooser.setMultiSelectionEnabled(false);
        this.chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        this.chooser.setCurrentDirectory(new File("rgbelevation"));

        add(mainPanel);
        pack();
        setVisible(true);
    }

    /**
     * Clears output label and runs the search by calling {@link Searcher#runSearch(SearchListener)}.
     */
    private void runSearch() {
        this.searcher = new BasicSearcher(this.raster, Configuration.side, Configuration.deviationThreshold);
        this.outputLabel.setText("information");
        this.searcher.runSearch(this);
    }

    /**
     * Implements {@link SearchListener#information(String)} by displaying the
     * information in the UI output label.
     */
    @Override
    public void information(final String message) {
        this.outputLabel.setText(message + "\n");
    }

    /**
     * Implements {@link SearchListener#possibleMatch(int, long, long)} by
     * displaying the information in the UI output label.
     */
    @Override
    public void possibleMatch(final int position, final long elapsedTime, final long positionsTriedSoFar) {
        final int x = position % this.raster.getWidth();
        final int y = position / this.raster.getWidth();
        this.outputLabel.setText("Possible match at: [" + x + "," + y + "] at " + (elapsedTime / 1000.0) + "s (" + positionsTriedSoFar + " positions attempted)\n");

        final Rectangle r = new Rectangle(x, y, Configuration.side, Configuration.side);
        this.mainImagePanel.addHighlight(r);
    }

    @Override
    public void update(final int position, final long elapsedTime, final long positionsTriedSoFar) {
        final int x = position % this.raster.getWidth();
        final int y = position / this.raster.getWidth();
        this.outputLabel.setText("Update at: [" + x + "," + y + "] at " + (elapsedTime / 1000.0) + "s (" + positionsTriedSoFar + " positions attempted)\n");
    }


    private static void launch() {
        new SearchUI();
    }

    private static class ImagePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private BufferedImage image;

        private final List<Rectangle> highlights = new ArrayList<Rectangle>();

        public void setImage(final BufferedImage image) {
            this.image = image;

            double scale = 1;

            if (image.getWidth() >= image.getHeight()) {
                if (image.getWidth() > 800) {
                    scale = 800.0 / image.getWidth();
                }
            } else {
                if (image.getHeight() > 800) {
                    scale = 800.0 / image.getHeight();
                }
            }
            final Dimension d = new Dimension(
                    (int) Math.ceil(image.getWidth() * scale),
                    (int) Math.ceil(image.getHeight() * scale));
            //System.out.println( d);
            setPreferredSize(d);

            invalidate();
            repaint();
        }

        public void addHighlight(final Rectangle r) {
            synchronized (this.highlights) {
                this.highlights.add(r);
            }
            repaint();
        }

        public void resetHighlights() {
            synchronized (this.highlights) {
                this.highlights.clear();
            }
            repaint();
        }

        @Override
        public void paintComponent(Graphics g) {
            if (this.image != null) {
                g = g.create();
                final double scale = getWidth() / (double) this.image.getWidth();
                //System.out.println( scale + "!");
                g.drawImage(this.image, 0, 0, getWidth(), (int) (this.image.getHeight() * scale), this);
                //System.out.println( ">>>" + completed);
                g.setColor(Color.YELLOW);
                synchronized (this.highlights) {
                    for (final Rectangle r : this.highlights) {
                        final Rectangle s = new Rectangle(
                                (int) (r.x * scale),
                                (int) (r.y * scale),
                                (int) (r.width * scale),
                                (int) (r.height * scale));
                        ((Graphics2D) g).draw(s);
                        //System.out.println( r + " >> " + s);
                    }
                }
            }
        }

    }

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        launch();
                    }
                }
        );
    }

}
