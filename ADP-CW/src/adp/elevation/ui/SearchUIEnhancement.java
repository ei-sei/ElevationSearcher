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
import java.util.concurrent.atomic.AtomicBoolean;

public class SearchUIEnhancement extends JFrame implements SearchListener {
    private static final long serialVersionUID = 1L;

    private final JButton openBigButton = new JButton("Open elevation data");
    private final JLabel mainFilenameLabel = new JLabel();

    public static ImagePanel mainImagePanel = new ImagePanel();
    private final JFileChooser chooser = new JFileChooser();

    public static JLabel outputLabel = new JLabel("information");
    private final JButton startButton = new JButton("Start");

    private Searcher searcher;
    private BufferedImage raster;

    //cancel button objects
    private volatile AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;
    private final JButton cancelButton = new JButton("Cancel");

    //progress bar
    private static JProgressBar progressBar = new JProgressBar();
    private Thread progressThread;

    public SearchUIEnhancement() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // kill the application on closing the window

        //Creates panel for open elevation button
        final JPanel mainFilePanel = new JPanel(new BorderLayout());
        mainFilePanel.add(this.openBigButton, BorderLayout.WEST);
        mainFilePanel.add(this.mainFilenameLabel, BorderLayout.CENTER);

        final JPanel topPanel = new JPanel(new GridLayout(0, 1));
        topPanel.add(mainFilePanel);

        //Creates the central panel to put the image
        final JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.add(mainImagePanel, BorderLayout.CENTER);

        //Creates the bottom panel to place the start and information button
        final JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(this.outputLabel, BorderLayout.WEST);
        bottomPanel.add(this.startButton, BorderLayout.NORTH);
        bottomPanel.add(this.cancelButton, BorderLayout.EAST);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);

        //layout of the panels
        final JPanel mainPanel = new JPanel(new BorderLayout());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(imagePanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        // mainPanel.add(progressPanel, BorderLayout.AFTER_LAST_LINE);

        this.openBigButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                if (SearchUIEnhancement.this.chooser.showOpenDialog(SearchUIEnhancement.this) ==
                        JFileChooser.APPROVE_OPTION) {
                    final File file = SearchUIEnhancement.this.chooser.getSelectedFile();
                    SearchUIEnhancement.this.mainFilenameLabel.setText(file.getName());
                    try {
                        SearchUIEnhancement.this.raster = ImageIO.read(file);
                    } catch (final IOException e) {

                        e.printStackTrace();
                    }
                    SearchUIEnhancement.this.mainImagePanel.resetHighlights();
                    SearchUIEnhancement.this.mainImagePanel.setImage(SearchUIEnhancement.this.raster);
                    pack();
                    SearchUIEnhancement.this.mainImagePanel.repaint();
                }
            }
        });

        this.startButton.addActionListener(new ActionListener() {
            @Override
            public final void actionPerformed(final ActionEvent ev) {
                synchronized (this) {
                    thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            runSearch();
                        }
                    });
                    thread.start();
                }
            }
        });

        this.cancelButton.addActionListener(new ActionListener() {
            @Override

            public void actionPerformed(ActionEvent e) {
                System.out.println(Thread.currentThread().getName());

                cancel();
            }
        });

        this.chooser.setMultiSelectionEnabled(false);
        this.chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        this.chooser.setCurrentDirectory(new File("rgbelevation"));

        add(mainPanel);
        pack();
        setVisible(true);
    }

    private void runSearch() {

        running.set(true);
        while (running.get()) {

            synchronized (this) {
                this.searcher = new BasicSearcher(this.raster, Configuration.side, Configuration.deviationThreshold);
                this.outputLabel.setText("information");
                this.searcher.runSearch(this);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Thread was interrupted");
                }
            }
        }
    }

    private void cancel() {
        this.running.set(false);
        runSearch();
    }

    @Override
    public void information(final String message) {
        this.outputLabel.setText(message + "\n");
    }

    @Override
    public void possibleMatch(final int position, final long elapsedTime, final long positionsTriedSoFar) {
        final int x = position % this.raster.getWidth();
        final int y = position / this.raster.getWidth();
        this.outputLabel.setText("Possible match at: [" + x + "," + y + "] at " + (elapsedTime / 1000.0) + "s (" +
                positionsTriedSoFar + " positions attempted)\n");

        final Rectangle r = new Rectangle(x, y, Configuration.side, Configuration.side);
        this.mainImagePanel.addHighlight(r);
    }

    @Override
    public void update(final int position, final long elapsedTime, final long positionsTriedSoFar) {
        final int x = position % this.raster.getWidth();
        final int y = position / this.raster.getWidth();
        this.outputLabel.setText("Update at: [" + x + "," + y + "] at " + (elapsedTime / 1000.0) + "s (" +
                positionsTriedSoFar + " positions attempted)\n");

        progressBar.setMaximum(searcher.numberOfPositionsToTry());
        progressThread = new Thread(new Runnable() {

            @Override
            public void run() {
                int progress = 0;

                synchronized (this) {
                    try {
                        progressThread.sleep(1000);
                        while (progress <= searcher.numberOfPositionsTriedSoFar()) {
                            progressBar.setValue(progress);
                            progress += searcher.numberOfPositionsTriedSoFar();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        progressThread.start();
    }

    private static void launch() {
        new SearchUIEnhancement();
    }

    private static class ImagePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        public BufferedImage image;

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

