import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FractalAnimator extends JPanel {

    private BufferedImage currentImage; // Current fractal image being displayed
    private BufferedImage nextImage;    // Next fractal image being computed
    private double cRe, cIm;           // Constant c = 0.7885 * e^(i*a)
    private int maxIteration = 50;    // Set to 50 for best performance

    // Variables for animation
    private double angle = 0;          // Angle a in radians
    private final double radius = 0.7885; // Radius of the circle for c

    // Thread pool for parallel fractal generation
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private int fractalWidth = 1000;   // Default width
    private int fractalHeight = 1000;  // Default height

    public FractalAnimator() {
        currentImage = new BufferedImage(fractalWidth, fractalHeight, BufferedImage.TYPE_INT_ARGB);
        nextImage = new BufferedImage(fractalWidth, fractalHeight, BufferedImage.TYPE_INT_ARGB);

        // Timer to animate the fractal continuously 
        Timer timer = new Timer(30, e -> {  
            updateFractal();
            repaint();
        });
        timer.start();
    }

    private void updateFractal() {
        angle += 0.01; // Increment the angle
        if (angle > 2 * Math.PI) {
            angle -= 2 * Math.PI; // Switch around after 2π
        }

        cRe = radius * Math.cos(angle); // Real part of c
        cIm = radius * Math.sin(angle); // Imaginary part of c

        // Generate the next fractal
        generateFractalParallel();
    }

    // Generate the fractal in parallel using multiple threads
    private void generateFractalParallel() {
        // Split the work into multiple threads
        int numThreads = Runtime.getRuntime().availableProcessors();
        int segmentHeight = fractalHeight / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int startY = i * segmentHeight;
            int endY = (i == numThreads - 1) ? fractalHeight : startY + segmentHeight;

            executor.submit(() -> generateFractalSegment(nextImage, fractalWidth, startY, endY));
        }

        // Swap currentImage and nextImage once the fractal is done
        SwingUtilities.invokeLater(() -> {
            BufferedImage temp = currentImage;
            currentImage = nextImage;
            nextImage = temp;
        });
    }

    // Generate a segment of the fractal
    private void generateFractalSegment(BufferedImage image, int width, int startY, int endY) {
        for (int y = startY; y < endY; y++) {
            for (int x = 0; x < width; x++) {
                double zx = 1.5 * (x - width / 2) / (0.5 * width);
                double zy = (y - width / 2) / (0.5 * width);
                int iteration = 0;

                while (zx * zx + zy * zy < 4 && iteration < maxIteration) {
                    double temp = zx * zx - zy * zy + cRe; // Real part of z^2 + c
                    zy = 2.0 * zx * zy + cIm;             // Imaginary part of z^2 + c
                    zx = temp;                             // Update zx for the next iteration
                    iteration++;
                }

                int color = iteration | (iteration << 8); // Simple coloring
                if (iteration < maxIteration) {
                    image.setRGB(x, y, color);
                } else {
                    image.setRGB(x, y, Color.BLACK.getRGB()); 
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Debugging
        if (currentImage != null) {
            // Scale the fractal to fit the panel
            g.drawImage(currentImage, 0, 0, getWidth(), getHeight(), null);
        } else {
            System.out.println("Image is null, unable to draw fractal.");
        }
    }

    public static void main(String[] args) {
        // Set up the JFrame 
        JFrame frame = new JFrame("Julia Set Animator");
        FractalAnimator panel = new FractalAnimator();

        // Set up the main layout
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set the frame size to match the screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(screenSize.width, screenSize.height);
        frame.setUndecorated(true);
        frame.add(panel, BorderLayout.CENTER); 
        frame.setVisible(true);
    }
}