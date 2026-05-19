package ui;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.highgui.HighGui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class AppUI {
    public String chooseImageFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh đầu vào để phát hiện khuôn mặt");

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files (*.jpg, *.png)", "jpg", "png");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            return selectedFile.getAbsolutePath();
        }
        return null;
    }


    public void showResult(Mat original, Mat result) {
        if (original == null || result == null || original.empty() || result.empty()) {
            System.out.println("Dữ liệu ảnh không hợp lệ để hiển thị!");
            return;
        }
        HighGui.imshow("Anh Goc (Original Image)", original);
        HighGui.imshow("Ket Qua Phat Hien (Result Image)", result);
        HighGui.waitKey(0);
    }


    public void saveResult(String originalPath, Mat result) {
        if (originalPath == null || result == null || result.empty()) {
            System.out.println("Không thể lưu ảnh do dữ liệu không hợp lệ!");
            return;
        }
        try {
            File originalFile = new File(originalPath);
            String parentDirectory = originalFile.getParent();
            String outputPath = parentDirectory + File.separator + "output.jpg";

            boolean isSaved = Imgcodecs.imwrite(outputPath, result);
            if (isSaved) {
                System.out.println("Đã lưu ảnh kết quả thành công tại: " + outputPath);
            } else {
                System.out.println("Lưu ảnh thất bại!");
            }
        } catch (Exception e) {
            System.out.println("Đã xảy ra lỗi khi lưu file: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);

        AppUI ui = new AppUI();
        System.out.println("Đang mở hộp thoại chọn ảnh...");
        String path = ui.chooseImageFile();

        if (path != null) {
            System.out.println("\n--- KẾT QUẢ TEST ---");
            System.out.println("Đường dẫn ảnh bạn đã chọn thành công: " + path);
        } else {
            System.out.println("\nBạn đã hủy chọn hoặc không chọn file ảnh nào.");
        }
        System.exit(0);
    }
}