package webcam;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * WebcamCapture — Quản lý việc mở/đọc/đóng webcam bằng OpenCV.
 * Mặc định đặt độ phân giải 640x480.
 */
public class WebcamCapture {

    private VideoCapture cap;

    // ── Mở webcam (index 0 = webcam mặc định) ──────────────────────────────
    public void openCamera() throws Exception {
        cap = new VideoCapture(0);

        if (!cap.isOpened()) {
            throw new Exception("Không tìm thấy webcam!");
        }

        // Đặt độ phân giải 640x480
        cap.set(Videoio.CAP_PROP_FRAME_WIDTH,  640);
        cap.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);

        System.out.println("✅ Webcam đã mở.");
        System.out.println("   Độ phân giải: "
                + (int) cap.get(Videoio.CAP_PROP_FRAME_WIDTH)
                + "x"
                + (int) cap.get(Videoio.CAP_PROP_FRAME_HEIGHT));
    }

    // ── Đọc 1 frame từ webcam ───────────────────────────────────────────────
    public Mat readFrame() throws Exception {
        if (!isOpened()) {
            throw new Exception("Webcam chưa được mở!");
        }
        Mat frame = new Mat();
        boolean success = cap.read(frame);
        if (!success || frame.empty()) {
            throw new Exception("Không đọc được frame từ webcam!");
        }
        return frame;
    }

    // ── Đóng webcam (giải phóng tài nguyên) ────────────────────────────────
    public void closeCamera() {
        if (cap != null && cap.isOpened()) {
            cap.release();
            System.out.println("✅ Đã đóng webcam.");
        }
    }

    // ── Kiểm tra webcam có đang mở không ───────────────────────────────────
    public boolean isOpened() {
        return cap != null && cap.isOpened();
    }

    // ── Lấy chiều rộng frame hiện tại ──────────────────────────────────────
    public int getFrameWidth() {
        return isOpened() ? (int) cap.get(Videoio.CAP_PROP_FRAME_WIDTH) : 0;
    }

    // ── Lấy chiều cao frame hiện tại ───────────────────────────────────────
    public int getFrameHeight() {
        return isOpened() ? (int) cap.get(Videoio.CAP_PROP_FRAME_HEIGHT) : 0;
    }
}