import detection.DNNFaceDetector;
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

        // 2. Khởi tạo UI và Preprocessor
        AppUI ui = new AppUI();
        ImagePreprocessor preprocessor = new ImagePreprocessor();

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

        // 4. Load ảnh gốc
        Mat original;
        try {
            original = preprocessor.loadImage(imagePath);
            System.out.println("✅ Load ảnh OK: " + original.cols() + "x" + original.rows());
        } catch (Exception e) {
            System.err.println("❌ Lỗi load ảnh: " + e.getMessage());
            return;
        }

        // 5. Khởi tạo YuNet detector
        String modelPath = "resources/face_detection_yunet_2023mar.onnx";
        DNNFaceDetector detector;
        try {
            detector = new DNNFaceDetector(
                    modelPath,
                    original.cols(),
                    original.rows()
            );
            System.out.println("✅ Load YuNet model thành công.");
        } catch (Exception e) {
            System.err.println("❌ Không load được model: " + e.getMessage());
            return;
        }

        // 6. Detect khuôn mặt
        Mat faces;
        try {
            faces = detector.detect(original);
            System.out.println("✅ Phát hiện được " + detector.getFaceCount(faces) + " khuôn mặt.");
        } catch (Exception e) {
            System.err.println("❌ Lỗi detect: " + e.getMessage());
            return;
        }

        // 7. Vẽ box + hiển thị + lưu
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