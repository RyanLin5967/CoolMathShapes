import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Mandelbrot extends JPanel {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;
    private static final int MAX_ITER = 1000;

    private double zoom = 1.0;
    private double offsetX = -0.5;
    private double offsetY = 0.0;
    private static final double ZOOM_FACTOR = 1.2; 
    private BufferedImage image;
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private int lastMouseX, lastMouseY;
    private boolean dragging = false;

    public Mandelbrot() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.WHITE);
        image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
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
                    double dx = (e.getX() - lastMouseX) / (0.5 * zoom * WIDTH);
                    double dy = (e.getY() - lastMouseY) / (0.5 * zoom * HEIGHT);
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
            double mouseX = (e.getX() - WIDTH / 2.0) / (0.5 * zoom * WIDTH) + offsetX;
            double mouseY = (e.getY() - HEIGHT / 2.0) / (0.5 * zoom * HEIGHT) + offsetY;
            
            offsetX = mouseX + (offsetX - mouseX) / scaleFactor;
            offsetY = mouseY + (offsetY - mouseY) / scaleFactor;
            zoom *= scaleFactor;
            renderMandelbrot();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, null);
    }

    private void renderMandelbrot() {
        int tileSize = 50; // Divide image into 50x50 pixel tiles
        for (int tx = 0; tx < WIDTH; tx += tileSize) {
            for (int ty = 0; ty < HEIGHT; ty += tileSize) {
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
                if (globalX >= WIDTH || globalY >= HEIGHT) continue;
                
                double zx = 0, zy = 0;
                double cX = (globalX - WIDTH / 2.0) / (0.5 * zoom * WIDTH) + offsetX;
                double cY = (globalY - HEIGHT / 2.0) / (0.5 * zoom * HEIGHT) + offsetY;
                int iter = MAX_ITER;
                while (zx * zx + zy * zy < 4 && iter > 0) {
                    double tmp = zx * zx - zy * zy + cX;
                    zy = 2.0 * zx * zy + cY;
                    zx = tmp;
                    iter--;
                }
                int colorValue = iter == 0 ? 255 : Math.max(0, Math.min(255, 255 - (iter * 255 / MAX_ITER))); // Clamp values to 0-255
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
        frame.setResizable(false);
        frame.add(new Mandelbrot());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}