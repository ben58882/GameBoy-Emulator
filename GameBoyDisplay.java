import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public class GameBoyDisplay {
    private final int width = 160;
    private final int height = 144;
    private final int scale = 4;

    private final Canvas canvas;
    private final BufferStrategy bs;
    private final BufferedImage image;
    public JFrame frame;
    
    // This is the array your PPU will write to directly
    public final int[] pixelBuffer; 

    public GameBoyDisplay() {
        // 1. Setup the raw OS window (Canvas)
        frame = new JFrame("Game Boy");
        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(width * scale, height * scale));
        canvas.setIgnoreRepaint(true); // Stop Java from auto-refreshing

        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // 2. Initialize hardware double-buffering
        canvas.createBufferStrategy(2);
        bs = canvas.getBufferStrategy();

        // 3. Create the image and extract the jailbroken memory array
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        pixelBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    // 4. Call this method when the PPU finishes a frame
    public void renderFrame() {
        Graphics g = bs.getDrawGraphics();
        
        // Draw the backed image to the hidden buffer
        g.drawImage(image, 0, 0, width * scale, height * scale, null);
        g.dispose();
        
        // Instantly swap the buffer to the monitor
        bs.show();

    }
}