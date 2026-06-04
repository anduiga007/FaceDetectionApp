package webcam;

import org.opencv.core.Core;
import org.opencv.core.Mat;

/**
 * WebcamTest — Test mở webcam và in ra kích thước frame.
 *
 * Kết quả mong đợi:
 *   ✅ Webcam đã mở.
 *   Frame: 640x480
 *   ✅ Đã đóng webcam.
 */
public class WebcamTest {

    public static void main(String[] args) {

        // 1. Load OpenCV
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("✅ OpenCV loaded: " + Core.VERSION);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("❌ Không load được OpenCV. Kiểm tra VM option -Djava.library.path!");
            return;
        }

        WebcamCapture webcam = new WebcamCapture();

        try {
            // 2. Mở webcam (640x480)
            webcam.openCamera();

            // 3. Đọc 1 frame và in kích thước
            Mat frame = webcam.readFrame();
            System.out.println("Frame: " + frame.cols() + "x" + frame.rows());

            // 4. Xác nhận kích thước đúng
            if (frame.cols() == 640 && frame.rows() == 480) {
                System.out.println("✅ Kích thước frame đúng: 640x480");
            } else {
                System.out.println("⚠️ Kích thước frame thực tế: "
                        + frame.cols() + "x" + frame.rows()
                        + " (webcam có thể không hỗ trợ 640x480 chính xác)");
            }

            // 5. Giải phóng bộ nhớ frame
            frame.release();

        } catch (Exception e) {
            System.err.println("❌ Lỗi webcam: " + e.getMessage());
        } finally {
            // 6. Luôn đóng webcam dù có lỗi
            webcam.closeCamera();
        }
    }
}
