import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Mandelbrot extends JPanel {
    private int screenWidth;  // Screen width
    private int screenHeight; // Screen height
    private int fractalSize;  // Size of the fractal (square)
    private static final int MAX_ITER = 300;

    private double zoom = 1.0;
    private double offsetX = -0.5;
    private double offsetY = 0.0;
    private static final double ZOOM_FACTOR = 1.5;
    private BufferedImage image;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private int lastMouseX, lastMouseY;
    private boolean dragging = false;

    public Mandelbrot() {
        // Get the screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenWidth = screenSize.width;
        screenHeight = screenSize.height;

        // Set the fractal size to the smaller dimension (to maintain aspect ratio)
        fractalSize = Math.min(screenWidth, screenHeight);

        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(Color.BLACK); // Black bars to fill in space
        image = new BufferedImage(fractalSize, fractalSize, BufferedImage.TYPE_INT_RGB);
        renderMandelbrot();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                dragging = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    double dx = (e.getX() - lastMouseX) / (0.5 * zoom * fractalSize);
                    double dy = (e.getY() - lastMouseY) / (0.5 * zoom * fractalSize);
                    offsetX -= dx;
                    offsetY -= dy;
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    renderMandelbrot();
                }
            }
        });

        addMouseWheelListener(e -> {
            double scaleFactor = e.getWheelRotation() < 0 ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;
            double mouseX = (e.getX() - screenWidth / 2.0) / (0.5 * zoom * fractalSize) + offsetX;
            double mouseY = (e.getY() - screenHeight / 2.0) / (0.5 * zoom * fractalSize) + offsetY;

            offsetX = mouseX + (offsetX - mouseX) / scaleFactor;
            offsetY = mouseY + (offsetY - mouseY) / scaleFactor;
            zoom *= scaleFactor;
            renderMandelbrot();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the fractal centered on the screen
        int x = (screenWidth - fractalSize) / 2;
        int y = (screenHeight - fractalSize) / 2;
        g.drawImage(image, x, y, null);
    }

    private void renderMandelbrot() {
        int tileSize = 50; // Divide image into 50x50 pixel tiles
        for (int tx = 0; tx < fractalSize; tx += tileSize) {
            for (int ty = 0; ty < fractalSize; ty += tileSize) {
                final int startX = tx;
                final int startY = ty;
                executor.submit(() -> renderTile(startX, startY, tileSize));
            }
        }
    }

    private void renderTile(int startX, int startY, int tileSize) {
        BufferedImage tile = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < tileSize; x++) {
            for (int y = 0; y < tileSize; y++) {
                int globalX = startX + x;
                int globalY = startY + y;
                if (globalX >= fractalSize || globalY >= fractalSize) continue;

                double zx = 0, zy = 0;
                double cX = (globalX - fractalSize / 2.0) / (0.5 * zoom * fractalSize) + offsetX;
                double cY = (globalY - fractalSize / 2.0) / (0.5 * zoom * fractalSize) + offsetY;
                int iter = MAX_ITER;
                while (zx * zx + zy * zy < 4 && iter > 0) {
                    double tmp = zx * zx - zy * zy + cX;
                    zy = 2.0 * zx * zy + cY;
                    zx = tmp;
                    iter--;
                }
                int colorValue = iter == 0 ? 255 : Math.max(0, Math.min(255, 255 - (iter * 255 / MAX_ITER))); 
                tile.setRGB(x, y, new Color(colorValue, colorValue, colorValue).getRGB());
            }
        }
        synchronized (image) {
            image.getGraphics().drawImage(tile, startX, startY, null);
        }
        repaint();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Mandelbrot Set");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
        frame.setResizable(false);
        frame.setUndecorated(true);
        frame.add(new Mandelbrot());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}