import org.opencv.core.Core;
import ui.AppUI;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Load OpenCV
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("✅ OpenCV loaded: " + Core.VERSION);
        } catch (UnsatisfiedLinkError e) {
            JOptionPane.showMessageDialog(null,
                    "Không load được OpenCV!\n" + e.getMessage(),
                    "Lỗi nghiêm trọng", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Hiện UI
        SwingUtilities.invokeLater(() -> new AppUI().setVisible(true));
    }
}