package detection;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;

/**
 * DNNFaceDetector — bọc YuNet (FaceDetectorYN) của OpenCV.
 *
 * YuNet output format per row:
 *   [x, y, w, h,  x_re, y_re, x_le, y_le,  x_nt, y_nt,
 *    x_rcm, y_rcm, x_lcm, y_lcm,  score]   (15 cột)
 */
public class DNNFaceDetector {

    private final FaceDetectorYN detector;

    public DNNFaceDetector(String modelPath, int width, int height) throws Exception {
        detector = FaceDetectorYN.create(modelPath, "", new Size(width, height));
        detector.setScoreThreshold(0.60f);
        detector.setNMSThreshold(0.30f);
        System.out.println("FaceDetector loaded: " + modelPath);
    }

    // ── Detection ─────────────────────────────────────────────────────────

    /** Phát hiện khuôn mặt trong frame, trả về Mat kết quả (rows = số mặt). */
    public Mat detect(Mat frame) {
        detector.setInputSize(new Size(frame.cols(), frame.rows()));
        Mat faces = new Mat();
        detector.detect(frame, faces);
        return faces;
    }

    public int getFaceCount(Mat faces) {
        return faces.rows();
    }

    // ── Face crop ─────────────────────────────────────────────────────────

    /**
     * Lấy Rect khuôn mặt thứ index (đã clip vào biên ảnh).
     * Trả về Rect rỗng nếu lỗi.
     */
    public Rect getFaceRect(Mat frame, Mat faces, int index) {
        try {
            int x = (int) faces.get(index, 0)[0];
            int y = (int) faces.get(index, 1)[0];
            int w = (int) faces.get(index, 2)[0];
            int h = (int) faces.get(index, 3)[0];

            int x1 = Math.max(0, x);
            int y1 = Math.max(0, y);
            int x2 = Math.min(frame.cols(), x1 + w);
            int y2 = Math.min(frame.rows(), y1 + h);

            if (x2 <= x1 || y2 <= y1) return new Rect();
            return new Rect(x1, y1, x2 - x1, y2 - y1);
        } catch (Exception e) {
            return new Rect();
        }
    }

    /**
     * Crop vùng khuôn mặt thứ index ra thành Mat mới (bộ nhớ liên tục).
     * Trả về Mat rỗng nếu lỗi.
     */
    public Mat cropFace(Mat frame, Mat faces, int index) {
        Rect rect = getFaceRect(frame, faces, index);
        if (rect.width <= 0 || rect.height <= 0) return new Mat();
        Mat crop = new Mat();
        new Mat(frame, rect).copyTo(crop);   // copyTo → bộ nhớ liên tục
        return crop;
    }

    // ── Drawing ───────────────────────────────────────────────────────────

    /**
     * Vẽ bounding box + nhãn lên frame.
     *
     * @param hasMask  true = xanh lá, false = đỏ
     * @param label    chuỗi hiện lên ("CO KHAU TRANG" / "KHONG KHAU TRANG")
     * @param conf     % độ tin cậy
     */
    public void drawDetection(Mat frame, Rect rect,
                              boolean hasMask, String label, float conf) {
        if (rect.width <= 0) return;

        Scalar boxColor  = hasMask ? new Scalar(40, 200, 40) : new Scalar(30, 30, 230);
        Scalar textColor = new Scalar(255, 255, 255);

        // Bounding box
        Imgproc.rectangle(frame, rect, boxColor, 2);

        // Thanh nền chữ
        String text = (hasMask ? "[OK] " : "[!] ") + label
                + "  " + String.format("%.0f", conf) + "%";
        int[] bl = {0};
        Size ts = Imgproc.getTextSize(text, Imgproc.FONT_HERSHEY_SIMPLEX, 0.55, 2, bl);

        int ty = (rect.y > 30) ? rect.y - 8 : rect.y + rect.height + (int) ts.height + 8;
        Point tl = new Point(rect.x,           ty);
        Point br = new Point(rect.x + ts.width + 8, ty - ts.height - 6);

        Imgproc.rectangle(frame, tl, br, boxColor, Imgproc.FILLED);
        Imgproc.putText(frame, text,
                new Point(rect.x + 4, ty - 3),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.55, textColor, 2);
    }
}