import detection.DNNFaceDetector;
import detection.MaskClassifier;
import ImagePreprocessor.ImagePreprocessor;
import ui.AppUI;

import org.opencv.core.*;
import javax.swing.SwingUtilities;

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
            detector = new DNNFaceDetector(modelPath, original.cols(), original.rows());
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

        // -------------------------------------------------------
        // 6.5 [NGƯỜI 2] Phân loại khẩu trang từng khuôn mặt
        // -------------------------------------------------------
        String maskModelPath = "resources/mask_classifier.onnx";
        Mat result = original.clone();
        try {
            MaskClassifier classifier = new MaskClassifier(maskModelPath);
            int faceCount = detector.getFaceCount(faces);

            for (int i = 0; i < faceCount; i++) {
                // Crop từng khuôn mặt ra
                Mat faceImg = detector.cropFace(original, faces, i);
                if (faceImg.empty()) continue;

                // Phân loại: MASK hoặc NO_MASK
                String label = classifier.classify(faceImg);
                float confidence = classifier.getConfidence(faceImg);

                // Lấy tọa độ để vẽ box màu
                int x = (int) faces.get(i, 0)[0];
                int y = (int) faces.get(i, 1)[0];
                int w = (int) faces.get(i, 2)[0];
                int h = (int) faces.get(i, 3)[0];
                Rect faceRect = new Rect(x, y, w, h);

                // Vẽ box màu theo kết quả (xanh lá / đỏ)
                detector.drawMaskBox(result, faceRect, label, confidence);

                System.out.println("  Mặt " + (i+1) + ": " + label
                        + " (" + String.format("%.1f", confidence) + "%)");
            }
            System.out.println("✅ Phân loại khẩu trang hoàn tất.");

        } catch (Exception e) {
            // Nếu chưa có model mask thì fallback về drawBoxes bình thường
            System.out.println("⚠️ Chưa có MaskClassifier, dùng detect thường: " + e.getMessage());
            result = detector.drawBoxes(original.clone(), faces);
        }
        // -------------------------------------------------------

        // 7. Hiển thị + lưu kết quả
        try {
            int faceCount = detector.getFaceCount(faces);
            ui.showResult(original, result, faceCount);
            ui.saveResult(imagePath, result);
            System.out.println("✅ Hoàn tất! Đã lưu output.jpg.");
        } catch (Exception e) {
            System.err.println("❌ Lỗi hiển thị/lưu: " + e.getMessage());
        }
    }

    public static void restart() {
        SwingUtilities.invokeLater(() -> main(new String[]{}));
    }
}