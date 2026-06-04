package webcam;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * WebcamTest — Mở webcam và hiển thị live feed trong cửa sổ Swing.
 * Nhấn phím ESC hoặc đóng cửa sổ để thoát.
 */
public class WebcamTest {

    public static void main(String[] args) {

        // 1. Load OpenCV native library
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("✅ OpenCV loaded: " + Core.VERSION);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("❌ Không load được OpenCV. Kiểm tra VM option -Djava.library.path!");
            return;
        }

        // 2. Tạo cửa sổ Swing
        JFrame frame = new JFrame("📷 Webcam Live — 640x480");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        // Label để hiển thị frame ảnh
        JLabel videoLabel = new JLabel();
        videoLabel.setHorizontalAlignment(JLabel.CENTER);
        videoLabel.setPreferredSize(new Dimension(640, 480));

        // Panel thông tin phía dưới
        JLabel infoLabel = new JLabel("Đang khởi động webcam...", JLabel.CENTER);
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        infoLabel.setForeground(new Color(60, 60, 60));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        frame.setLayout(new BorderLayout());
        frame.add(videoLabel, BorderLayout.CENTER);
        frame.add(infoLabel, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null); // Giữa màn hình
        frame.setVisible(true);

        // 3. Mở webcam
        WebcamCapture webcam = new WebcamCapture();
        try {
            webcam.openCamera();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,
                    "❌ " + e.getMessage(),
                    "Lỗi Webcam", JOptionPane.ERROR_MESSAGE);
            frame.dispose();
            return;
        }

        System.out.println("✅ Bắt đầu hiển thị webcam. Đóng cửa sổ để thoát.");

        // 4. Vòng lặp đọc và hiển thị frame
        long frameCount = 0;
        long startTime = System.currentTimeMillis();

        while (frame.isVisible()) {
            try {
                Mat mat = webcam.readFrame();
                frameCount++;

                // Tính FPS mỗi 30 frame
                if (frameCount % 30 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double fps = frameCount * 1000.0 / elapsed;
                    String info = String.format("Frame: %dx%d  |  FPS: %.1f",
                            mat.cols(), mat.rows(), fps);
                    infoLabel.setText(info);
                    System.out.println("📊 " + info);
                }

                // Chuyển Mat → BufferedImage và hiển thị
                BufferedImage img = matToBufferedImage(mat);
                if (img != null) {
                    videoLabel.setIcon(new ImageIcon(img));
                }

                mat.release();

            } catch (Exception e) {
                System.err.println("⚠️ Lỗi đọc frame: " + e.getMessage());
                break;
            }
        }

        // 5. Dọn dẹp
        webcam.closeCamera();
        System.out.println("✅ Đã thoát.");
    }

    /**
     * Chuyển đổi OpenCV Mat → Java BufferedImage để hiển thị trên Swing.
     */
    private static BufferedImage matToBufferedImage(Mat mat) {
        try {
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".jpg", mat, mob);
            byte[] byteArray = mob.toArray();
            return ImageIO.read(new ByteArrayInputStream(byteArray));
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi chuyển đổi frame: " + e.getMessage());
            return null;
        }
    }
}
