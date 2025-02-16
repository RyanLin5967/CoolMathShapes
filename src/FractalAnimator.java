import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FractalAnimator extends JPanel {

    private BufferedImage currentImage; // Current fractal image being displayed
    private BufferedImage nextImage;    // Next fractal image being computed
    private double cRe = -0.599, cIm = 0.99; // These values are cool
    private int maxIteration = 100; // Reduced max iterations for performance
    //private final JTextField iterationInputField; // Input field for iterations
    //private final JButton setIterationsButton; // Button to set iterations

    // Variables for smooth oscillation
    private double cReSpeed = 0.005; // Speed of change for cRe
    private double cImSpeed = 0.003; // Speed of change for cIm
    private double cReMin = -1.0, cReMax = 1.0; // Bounds for cRe
    private double cImMin = -1.0, cImMax = 1.0; // Bounds for cIm

    // Thread pool for parallel fractal generation
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // Fractal dimensions (fixed to screen size)
    private int fractalWidth = 1000; // Default width
    private int fractalHeight = 1000; // Default height

    public FractalAnimator() {
        // Set up the input field for the number of iterations
        //iterationInputField = new JTextField(String.valueOf(maxIteration), 10);

        // Set up the button to update the number of iterations
        /* 
        setIterationsButton = new JButton("Set Iterations");
        setIterationsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    maxIteration = Integer.parseInt(iterationInputField.getText());
                    if (maxIteration <= 0) {
                        maxIteration = 1;
                    }
                } catch (NumberFormatException ex) {
                    // If the input is invalid, reset to default value
                    maxIteration = 100;
                }
                repaint(); // Repaint the fractal when the iteration count changes
            }
        });
        */
        // Initialize the fractal images
        currentImage = new BufferedImage(fractalWidth, fractalHeight, BufferedImage.TYPE_INT_ARGB);
        nextImage = new BufferedImage(fractalWidth, fractalHeight, BufferedImage.TYPE_INT_ARGB);

        // Timer to animate the fractal continuously (30 FPS)
        Timer timer = new Timer(30, e -> {  // ~30 FPS
            updateFractal();
            repaint();
        });
        timer.start(); 
    }

    // Update the fractal by changing the parameter c
    private void updateFractal() {
        // Update cRe and cIm with smooth oscillation
        cRe += cReSpeed;
        cIm += cImSpeed;

        // Reverse direction if cRe or cIm exceeds bounds
        if (cRe > cReMax || cRe < cReMin) {
            cReSpeed *= -1; // Reverse direction
        }
        if (cIm > cImMax || cIm < cImMin) {
            cImSpeed *= -1; // Reverse direction
        }

        // Generate the next fractal in parallel
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
                    double temp = zx * zx - zy * zy + cRe;
                    zy = 2.0 * zx * zy + cIm;
                    zx = temp;
                    iteration++;
                }

                // Apply color based on the number of iterations
                int color = iteration | (iteration << 8); // Simple coloring
                if (iteration < maxIteration) {
                    image.setRGB(x, y, color);
                } else {
                    image.setRGB(x, y, Color.BLACK.getRGB()); // Points that don't escape
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Check if currentImage is initialized and not null
        if (currentImage != null) {
            // Scale the fractal to fit the panel
            g.drawImage(currentImage, 0, 0, getWidth(), getHeight(), null);
        } else {
            System.out.println("Image is null, unable to draw fractal.");
        }
    }

    public static void main(String[] args) {
        // Set up the JFrame to cover the entire screen
        JFrame frame = new JFrame("Fractal Animator");
        FractalAnimator panel = new FractalAnimator();

        // Create a JPanel for the sidebar with the iteration input field and button
        JPanel sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));

        // Label for iteration input field
        //JLabel iterationLabel = new JLabel("Max Iterations: ");
        //sidebarPanel.add(iterationLabel);
        //sidebarPanel.add(panel.iterationInputField);
        //sidebarPanel.add(panel.setIterationsButton); // Add the button to set iterations

        // Set up the main layout: a border layout with the sidebar on the left
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set the frame size to match the screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(screenSize.width, screenSize.height);

        frame.add(sidebarPanel, BorderLayout.WEST); // Add sidebar to the left
        frame.add(panel, BorderLayout.CENTER); // Add the fractal panel in the center

        frame.setVisible(true);
    }
}