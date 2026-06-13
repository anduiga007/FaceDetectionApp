import org.opencv.core.Core;
import ui.AppUI;

import javax.swing.*;

/**
 * Main — Khởi động hệ thống phát hiện khẩu trang y tế.
 *
 * Yêu cầu:
 *   -Djava.library.path=C:\opencv\build\java\x64
 *   classpath: opencv-4120.jar
 */
public class Main {
    public static void main(String[] args) {
        // 1. Load OpenCV native library
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("OpenCV loaded: " + Core.VERSION);
        } catch (UnsatisfiedLinkError e) {
            JOptionPane.showMessageDialog(null,
                    "Khong load duoc OpenCV!\n\n"
                  + "Kiem tra:\n"
                  + "  - VM option: -Djava.library.path=C:\\opencv\\build\\java\\x64\n"
                  + "  - Classpath: opencv-4120.jar\n\n"
                  + "Chi tiet loi:\n" + e.getMessage(),
                    "Loi nghiem trong", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // 2. Khởi động UI trên Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            // Look & Feel: dùng Nimbus nếu có, fallback về System
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {
                // dùng default L&F
            }

            new AppUI().setVisible(true);
        });
    }
}