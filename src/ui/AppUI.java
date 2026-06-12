package ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;

public class AppUI extends JFrame {

    private int hasMaskCount = 0;
    private int noMaskCount = 0;
    private double complianceRate = 0.0;

    public AppUI() {
        setTitle("HỆ THỐNG PHÁT HIỆN KHẨU TRANG - NHÓM 3");
        setSize(500, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(0x1E, 0x1E, 0x1E));
        JLabel titleLabel = new JLabel("HỆ THỐNG KIỂM TRA KHẨU TRANG");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        JPanel menuPanel = new JPanel();
        menuPanel.setBackground(new Color(0x1E, 0x1E, 0x1E));
        menuPanel.setLayout(new GridLayout(3, 1, 15, 15));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        JButton btnUploadImage = createStyledButton("Kiểm tra ảnh — upload ảnh tĩnh");
        JButton btnWebcam = createStyledButton("Webcam real-time — bật webcam");
        JButton btnExportReport = createStyledButton("Xuất báo cáo (Report)");

        menuPanel.add(btnUploadImage);
        menuPanel.add(btnWebcam);
        menuPanel.add(btnExportReport);
        add(menuPanel, BorderLayout.CENTER);

        // ── Nút ảnh tĩnh ──────────────────────────────────────────────────
        btnUploadImage.addActionListener(e -> {
            String path = chooseImageFile();
            if (path != null) {
                new Thread(() -> processImage(path), "ImageThread").start();
            }
        });

        // ── Nút webcam ────────────────────────────────────────────────────
        btnWebcam.addActionListener(e -> {
            new Thread(() -> {
                JFrame webcamFrame = new JFrame("Webcam - Mask Detection");
                webcamFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                JLabel videoLabel = new JLabel();
                videoLabel.setPreferredSize(new Dimension(640, 480));
                videoLabel.setHorizontalAlignment(JLabel.CENTER);

                JLabel infoLabel = new JLabel("Đang khởi động...", JLabel.CENTER);
                infoLabel.setFont(new Font("Arial", Font.PLAIN, 13));
                infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

                webcamFrame.setLayout(new BorderLayout());
                webcamFrame.add(videoLabel, BorderLayout.CENTER);
                webcamFrame.add(infoLabel, BorderLayout.SOUTH);
                webcamFrame.pack();
                webcamFrame.setLocationRelativeTo(null);
                SwingUtilities.invokeLater(() -> webcamFrame.setVisible(true));

                webcam.WebcamCapture cap = new webcam.WebcamCapture();
                try {
                    cap.openCamera();
                    detection.DNNFaceDetector fd = new detection.DNNFaceDetector(
                            "resources/face_detection_yunet_2023mar.onnx", 640, 480);
                    detection.MaskClassifier mc = new detection.MaskClassifier(
                            "resources/mask_classifier.onnx");

                    int frameCount = 0;
                    int frameHas = 0, frameNo = 0;

                    while (webcamFrame.isVisible()) {
                        org.opencv.core.Mat frame = cap.readFrame();
                        org.opencv.core.Mat faces = fd.detect(frame);
                        int faceCount = fd.getFaceCount(faces);
                        frameHas = 0;
                        frameNo = 0;

                        for (int i = 0; i < faceCount; i++) {
                            org.opencv.core.Mat faceImg = fd.cropFace(frame, faces, i);
                            if (faceImg.empty()) continue;
                            String label = mc.classify(faceImg);
                            float conf = mc.getConfidence(faceImg);
                            int x = (int) faces.get(i, 0)[0];
                            int y = (int) faces.get(i, 1)[0];
                            int w = (int) faces.get(i, 2)[0];
                            int h = (int) faces.get(i, 3)[0];
                            org.opencv.core.Rect r = new org.opencv.core.Rect(
                                    Math.max(0, x), Math.max(0, y),
                                    Math.min(w, frame.cols() - Math.max(0, x)),
                                    Math.min(h, frame.rows() - Math.max(0, y)));
                            fd.drawMaskBox(frame, r, label, conf);
                            if (label.equals("MASK")) frameHas++;
                            else frameNo++;
                        }

                        frameCount++;
                        if (frameCount % 30 == 0) {
                            final String info = "Co khau trang: " + frameHas
                                    + "  |  Khong: " + frameNo
                                    + "  |  Tong: " + faceCount;
                            SwingUtilities.invokeLater(() -> infoLabel.setText(info));
                        }

                        org.opencv.core.MatOfByte mob = new org.opencv.core.MatOfByte();
                        org.opencv.imgcodecs.Imgcodecs.imencode(".jpg", frame, mob);
                        try {
                            BufferedImage img = ImageIO.read(new ByteArrayInputStream(mob.toArray()));
                            if (img != null) videoLabel.setIcon(new ImageIcon(img));
                        } catch (Exception ex) { ex.printStackTrace(); }
                        frame.release();
                    }

                    cap.closeCamera();
                    final int fHas = frameHas, fNo = frameNo;
                    SwingUtilities.invokeLater(() -> showStats(fHas, fNo, null));

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(AppUI.this,
                                    "Loi webcam: " + ex.getMessage(), "Loi", JOptionPane.ERROR_MESSAGE));
                }
            }, "WebcamThread").start();
        });

        btnExportReport.addActionListener(e -> exportToTxt());
    }

    // ── Xử lý ảnh tĩnh ───────────────────────────────────────────────────
    private void processImage(String imagePath) {
        try {
            org.opencv.core.Mat image = org.opencv.imgcodecs.Imgcodecs.imread(imagePath);
            if (image.empty()) {
                JOptionPane.showMessageDialog(this, "Khong doc duoc anh!", "Loi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            detection.DNNFaceDetector fd = new detection.DNNFaceDetector(
                    "resources/face_detection_yunet_2023mar.onnx", image.cols(), image.rows());
            detection.MaskClassifier mc = new detection.MaskClassifier(
                    "resources/mask_classifier.onnx");

            org.opencv.core.Mat faces = fd.detect(image);
            int faceCount = fd.getFaceCount(faces);

            if (faceCount == 0) {
                JOptionPane.showMessageDialog(this, "Khong phat hien khuon mat!", "Thong bao", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int hasMask = 0, noMask = 0;
            org.opencv.core.Mat result = image.clone();

            for (int i = 0; i < faceCount; i++) {
                org.opencv.core.Mat faceImg = fd.cropFace(image, faces, i);
                if (faceImg.empty()) continue;
                String label = mc.classify(faceImg);
                float confidence = mc.getConfidence(faceImg);
                int x = (int) faces.get(i, 0)[0];
                int y = (int) faces.get(i, 1)[0];
                int w = (int) faces.get(i, 2)[0];
                int h = (int) faces.get(i, 3)[0];
                org.opencv.core.Rect faceRect = new org.opencv.core.Rect(
                        Math.max(0, x), Math.max(0, y),
                        Math.min(w, image.cols() - Math.max(0, x)),
                        Math.min(h, image.rows() - Math.max(0, y)));
                fd.drawMaskBox(result, faceRect, label, confidence);
                if (label.equals("MASK")) hasMask++;
                else noMask++;
            }

            // Chuyển Mat → BufferedImage
            org.opencv.core.MatOfByte mob = new org.opencv.core.MatOfByte();
            org.opencv.imgcodecs.Imgcodecs.imencode(".jpg", result, mob);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(mob.toArray()));

            final int finalHas = hasMask, finalNo = noMask;
            SwingUtilities.invokeLater(() -> {
                // 1. Hiện ảnh
                JFrame resultFrame = new JFrame("Ket qua phat hien khau trang");
                resultFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                resultFrame.add(new JLabel(new ImageIcon(img)));
                resultFrame.pack();
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                resultFrame.setLocation(screen.width - resultFrame.getWidth(), 0);
                resultFrame.setVisible(true);

                // 2. Hiện thống kê (1 lần duy nhất, dùng resultFrame làm parent)
                showStats(finalHas, finalNo, resultFrame);
            });

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Loi: " + ex.getMessage(), "Loi", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // ── Hiện thống kê — CHỈ GỌI Ở ĐÂY, không gọi ở chỗ khác ─────────────
    private void showStats(int hasMask, int noMask, JFrame parent) {
        this.hasMaskCount = hasMask;
        this.noMaskCount = noMask;
        int total = hasMask + noMask;
        this.complianceRate = total > 0 ? (double) hasMask / total * 100 : 0.0;

        String msg = String.format(
                "KET QUA PHAN TICH:\n\n" +
                        " Co khau trang: %d nguoi\n" +
                        " Khong khau trang: %d nguoi\n" +
                        " Ti le tuan thu: %.2f%%",
                hasMask, noMask, complianceRate);

        JOptionPane.showMessageDialog(parent, msg, "THONG KE", JOptionPane.INFORMATION_MESSAGE);

        if (noMask > 0) {
            JOptionPane.showMessageDialog(parent,
                    "Phat hien " + noMask + " nguoi khong deo khau trang!",
                    "CANH BAO", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ── updateStatisticsData giữ lại để webcam gọi ───────────────────────
    public void updateStatisticsData(int hasMask, int noMask) {
        showStats(hasMask, noMask, null);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(0x2D, 0x2D, 0x2D));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(0x44, 0x44, 0x44), 1));
        return button;
    }

    private void exportToTxt() {
        try (FileWriter writer = new FileWriter("report.txt")) {
            writer.write("=========================================\n");
            writer.write("   BAO CAO KET QUA KIEM TRA KHAU TRANG\n");
            writer.write("=========================================\n");
            writer.write("So nguoi CO khau trang: " + hasMaskCount + " nguoi\n");
            writer.write("So nguoi KHONG khau trang: " + noMaskCount + " nguoi\n");
            writer.write(String.format("Ti le tuan thu: %.2f%%\n", complianceRate));
            writer.write("Trang thai: " + (noMaskCount > 0 ? "CAN NHAC NHO!" : "AN TOAN") + "\n");
            writer.write("=========================================\n");
            JOptionPane.showMessageDialog(this, "Da xuat bao cao: report.txt",
                    "Xuat File", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Loi xuat file!", "Loi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String chooseImageFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chon anh de kiem tra khau trang");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (*.jpg, *.png, *.jpeg)", "jpg", "png", "jpeg"));
        return fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
                ? fc.getSelectedFile().getAbsolutePath() : null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppUI().setVisible(true));
    }
}