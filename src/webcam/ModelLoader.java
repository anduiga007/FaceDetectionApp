package webcam;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

/**
 * ModelLoader — Test load 2 model ONNX để đảm bảo không bị crash:
 *   1. face_detection_yunet_2023mar.onnx  (YuNet — FaceDetectorYN)
 *   2. yolov8n-face.onnx                 (YOLOv8 — cv2.dnn.Net)
 *
 * Chạy class này standalone để kiểm tra model trước khi tích hợp.
 */
public class ModelLoader {

    private static final String YUNET_PATH  = "resources/face_detection_yunet_2023mar.onnx";
    private static final String YOLO_PATH   = "resources/yolov8n-face.onnx";

    public static void main(String[] args) {
        // 1. Load OpenCV native library
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("✅ OpenCV loaded: " + Core.VERSION);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("❌ Không load được OpenCV. Kiểm tra VM option -Djava.library.path!");
            return;
        }

        // 2. Test load YuNet (FaceDetectorYN)
        System.out.println("\n── Test 1: YuNet (face_detection_yunet_2023mar.onnx) ──");
        testLoadYuNet();

        // 3. Test load YOLOv8 via DNN
        System.out.println("\n── Test 2: YOLOv8n-face (yolov8n-face.onnx) ──");
        testLoadYoloV8();

        System.out.println("\n✅ Tất cả model load thành công — không crash!");
    }

    // ── Load YuNet ──────────────────────────────────────────────────────────
    public static FaceDetectorYN loadYuNet(int width, int height) throws Exception {
        FaceDetectorYN detector = FaceDetectorYN.create(
                YUNET_PATH, "", new Size(width, height)
        );
        if (detector == null) {
            throw new Exception("FaceDetectorYN.create() trả về null!");
        }
        detector.setScoreThreshold(0.6f);
        detector.setNMSThreshold(0.3f);
        return detector;
    }

    // ── Load YOLOv8 qua OpenCV DNN ──────────────────────────────────────────
    public static Net loadYoloV8() throws Exception {
        Net net = Dnn.readNetFromONNX(YOLO_PATH);
        if (net.empty()) {
            throw new Exception("Dnn.readNetFromONNX() trả về net rỗng!");
        }
        // Ưu tiên dùng CUDA nếu có, fallback sang CPU
        net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);
        net.setPreferableTarget(Dnn.DNN_TARGET_CPU);
        return net;
    }

    // ── Test helpers (dùng trong main) ──────────────────────────────────────
    private static void testLoadYuNet() {
        try {
            FaceDetectorYN det = loadYuNet(640, 480);
            System.out.println("✅ YuNet load OK — detector: " + det);
        } catch (Exception e) {
            System.err.println("❌ YuNet FAILED: " + e.getMessage());
        }
    }

    private static void testLoadYoloV8() {
        try {
            Net net = loadYoloV8();
            System.out.println("✅ YOLOv8n-face load OK — net empty? " + net.empty());
        } catch (Exception e) {
            System.err.println("❌ YOLOv8 FAILED: " + e.getMessage());
        }
    }
}
