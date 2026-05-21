import detection.FaceDetector;
import ImagePreprocessor.ImagePreprocessor;
import ui.AppUI;

import org.opencv.core.*;

public class Main {
    public static void main(String[] args) {

        // 1. Load OpenCV
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("✅ Load OpenCV thành công.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("❌ Không load được OpenCV. Kiểm tra VM option!");
            return;
        }

        // 2. Khởi tạo các class
        AppUI ui = new AppUI();
        ImagePreprocessor preprocessor = new ImagePreprocessor();
        String cascadePath = "resources/haarcascade_frontalface_default.xml";
        FaceDetector detector;

        try {
            detector = new FaceDetector(cascadePath);
            System.out.println("✅ Load cascade XML thành công.");
        } catch (Exception e) {
            System.err.println("❌ Không load được file XML: " + cascadePath);
            return;
        }

        // 3. Chọn ảnh
        String imagePath;
        try {
            imagePath = ui.chooseImageFile();
            if (imagePath == null) {
                System.out.println("⚠️ Không chọn ảnh. Thoát.");
                return;
            }
            System.out.println("✅ Chọn ảnh: " + imagePath);
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi chọn ảnh: " + e.getMessage());
            return;
        }

        // 4. Load + tiền xử lý
        Mat original, gray, equalized;
        try {
            original  = preprocessor.loadImage(imagePath);
            gray      = preprocessor.toGrayscale(original);
            equalized = preprocessor.equalize(gray);
            System.out.println("✅ Tiền xử lý xong: " + original.cols() + "x" + original.rows());
        } catch (Exception e) {
            System.err.println("❌ Lỗi tiền xử lý: " + e.getMessage());
            return;
        }

        // 5. Detect khuôn mặt
        MatOfRect faces;
        try {
            faces = detector.detect(equalized);
            System.out.println("✅ Phát hiện được " + faces.total() + " khuôn mặt.");
        } catch (Exception e) {
            System.err.println("❌ Lỗi detect: " + e.getMessage());
            return;
        }

        // 6. Vẽ khung + hiển thị + lưu
        try {
            Mat result = detector.drawBoxes(original.clone(), faces);
            ui.showResult(original, result);
            ui.saveResult(imagePath, result);
            System.out.println("✅ Hoàn tất! Đã lưu output.jpg.");
        } catch (Exception e) {
            System.err.println("❌ Lỗi hiển thị/lưu: " + e.getMessage());
        }
    }
}