package ui;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class AppUI {

    // Chọn file ảnh
    public String chooseImageFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh đầu vào để phát hiện khuôn mặt");
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image Files (*.jpg, *.png)", "jpg", "png");
        fileChooser.setFileFilter(filter);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    // Chuyển Mat → BufferedImage để hiện trong Swing
    private BufferedImage matToBufferedImage(Mat mat) {
        Mat rgb = new Mat();
        Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGR2RGB);
        int width = rgb.cols();
        int height = rgb.rows();
        byte[] data = new byte[width * height * 3];
        rgb.get(0, 0, data);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        image.getRaster().setDataElements(0, 0, width, height, data);
        return image;
    }

    // Scale ảnh vừa với panel
    private ImageIcon scaleImage(BufferedImage img, int maxW, int maxH) {
        double scale = Math.min((double) maxW / img.getWidth(), (double) maxH / img.getHeight());
        int w = (int) (img.getWidth() * scale);
        int h = (int) (img.getHeight() * scale);
        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    // Hiển thị kết quả với giao diện đẹp
    public void showResult(Mat original, Mat result, int faceCount) {
        // Tạo cửa sổ chính
        JFrame frame = new JFrame("🔍 Face Detection App — OpenCV + YuNet");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(30, 30, 30));

        // Thanh tiêu đề thông tin
        JLabel titleLabel = new JLabel(
                "  ✅ Phát hiện được " + faceCount + " khuôn mặt",
                SwingConstants.LEFT
        );
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(0, 220, 100));
        titleLabel.setBackground(new Color(20, 20, 20));
        titleLabel.setOpaque(true);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        frame.add(titleLabel, BorderLayout.NORTH);

        // Panel chứa 2 ảnh
        JPanel imagePanel = new JPanel(new GridLayout(1, 2, 10, 10));
        imagePanel.setBackground(new Color(30, 30, 30));
        imagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Ảnh gốc
        BufferedImage origBI = matToBufferedImage(original);
        JLabel origLabel = new JLabel(scaleImage(origBI, 500, 500));
        origLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel origPanel = createImagePanel(origLabel, "Ảnh gốc (Original)");
        imagePanel.add(origPanel);

        // Ảnh kết quả
        BufferedImage resultBI = matToBufferedImage(result);
        JLabel resultLabel = new JLabel(scaleImage(resultBI, 500, 500));
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel resultPanel = createImagePanel(resultLabel, "Kết quả (Detected: " + faceCount + " mặt)");
        imagePanel.add(resultPanel);

        frame.add(imagePanel, BorderLayout.CENTER);

        // Thanh nút bấm phía dưới
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(new Color(20, 20, 20));

        JButton closeButton = new JButton("❌ Đóng");
        closeButton.setFont(new Font("Arial", Font.BOLD, 13));
        closeButton.setBackground(new Color(180, 50, 50));
        closeButton.setForeground(Color.WHITE);
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> frame.dispose());

        JButton newButton = new JButton("📂 Chọn ảnh khác");
        newButton.setFont(new Font("Arial", Font.BOLD, 13));
        newButton.setBackground(new Color(50, 120, 200));
        newButton.setForeground(Color.WHITE);
        newButton.setFocusPainted(false);
        newButton.setBorderPainted(false);
        newButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        newButton.addActionListener(e -> frame.dispose());

        buttonPanel.add(newButton);
        buttonPanel.add(closeButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Hiện cửa sổ
        frame.setSize(1100, 620);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Tạo panel ảnh có tiêu đề
    private JPanel createImagePanel(JLabel imageLabel, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(45, 45, 45));
        panel.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 13));
        titleLabel.setForeground(new Color(200, 200, 200));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        titleLabel.setBackground(new Color(35, 35, 35));
        titleLabel.setOpaque(true);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(imageLabel, BorderLayout.CENTER);
        return panel;
    }

    // Lưu ảnh kết quả
    public void saveResult(String originalPath, Mat result) {
        try {
            File originalFile = new File(originalPath);
            String outputPath = originalFile.getParent() + File.separator + "output.jpg";
            boolean isSaved = Imgcodecs.imwrite(outputPath, result);
            if (isSaved) {
                System.out.println("✅ Đã lưu ảnh tại: " + outputPath);
            } else {
                System.out.println("❌ Lưu ảnh thất bại!");
            }
        } catch (Exception e) {
            System.out.println("❌ Lỗi lưu file: " + e.getMessage());
        }
    }
}