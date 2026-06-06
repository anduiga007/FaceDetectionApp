
package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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




        btnUploadImage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                JOptionPane.showMessageDialog(AppUI.this,
                        "Vui lòng chọn ảnh từ hộp thoại hệ thống vừa xuất hiện!", "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });


        btnWebcam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(AppUI.this,
                        "Đang khởi động Webcam real-time...", "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE);


                setVisible(false);

                try {
                    webcam.WebcamTest.main(new String[]{});
                } catch (Exception ex) {
                    new webcam.WebcamTest();
                }
            }
        });

        btnExportReport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportToTxt();
            }
        });
    }
    public void updateStatisticsData(int hasMask, int noMask) {
        this.hasMaskCount = hasMask;
        this.noMaskCount = noMask;
        calculateStatistics();
        showResultDialog();

        if (this.noMaskCount > 0) {
            JOptionPane.showMessageDialog(null,
                    "Phát hiện " + this.noMaskCount + " người không đeo khẩu trang!",
                    "CẢNH BÁO", JOptionPane.WARNING_MESSAGE);
        }
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

    private void calculateStatistics() {
        int total = hasMaskCount + noMaskCount;
        if (total > 0) {
            complianceRate = ((double) hasMaskCount / total) * 100;
        } else {
            complianceRate = 0.0;
        }
    }
    private void showResultDialog() {
        String resultMessage = String.format(
                "KẾT QUẢ PHÂN TÍCH:\n\n" +
                        " Có khẩu trang: %d người\n" +
                        " Không khẩu trang: %d người\n" +
                        " Tỉ lệ tuân thủ: %.2f%%",
                hasMaskCount, noMaskCount, complianceRate);

        JOptionPane.showMessageDialog(this, resultMessage, "THỐNG KÊ", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportToTxt() {
        String filePath = "report.txt";
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("=========================================\n");
            writer.write("       BÁO CÁO KẾT QUẢ KIỂM TRA KHẨU TRANG\n");
            writer.write("=========================================\n");
            writer.write("Số người CÓ khẩu trang: " + hasMaskCount + " người\n");
            writer.write("Số người KHÔNG khẩu trang: " + noMaskCount + " người\n");
            writer.write(String.format("Tỉ lệ tuân thủ quy định: %.2f%%\n", complianceRate));
            writer.write("-----------------------------------------\n");
            writer.write("Trạng thái đánh giá: " + (noMaskCount > 0 ? "CẦN NHẮC NHỞ QUY ĐỊNH!" : "AN TOÀN") + "\n");
            writer.write("=========================================\n");

            JOptionPane.showMessageDialog(this, "Đã xuất báo cáo thành công vào file: " + filePath,
                    "Xuất File", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi xuất file báo cáo!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public String chooseImageFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh tĩnh để kiểm tra khẩu trang");

        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image Files (*.jpg, *.png, *.jpeg)", "jpg", "png", "jpeg"
        ));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AppUI().setVisible(true);
            }
        });
    }
}