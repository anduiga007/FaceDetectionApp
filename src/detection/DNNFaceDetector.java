package detection;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;

public class DNNFaceDetector {

    private FaceDetectorYN detector;

    public DNNFaceDetector(String modelPath, int width, int height) throws Exception {
        detector = FaceDetectorYN.create(modelPath, "", new Size(width, height));
        detector.setScoreThreshold(0.6f);   // độ tin cậy tối thiểu
        detector.setNMSThreshold(0.3f);     // lọc box trùng nhau
        if (detector == null) {
            throw new Exception("Không load được model: " + modelPath);
        }
    }

    // Phát hiện khuôn mặt trong ảnh, trả về Ma trận chứa tọa độ các mặt
    public Mat detect(Mat colorImg) {
        // YuNet cần resize input đúng kích thước
        detector.setInputSize(new Size(colorImg.cols(), colorImg.rows()));
        Mat faces = new Mat();
        detector.detect(colorImg, faces);
        return faces;
    }

    // Vẽ bounding box mặc định (xanh lá) lên tất cả khuôn mặt phát hiện được
    public Mat drawBoxes(Mat original, Mat faces) {
        Mat result = original.clone();
        for (int i = 0; i < faces.rows(); i++) {
            // Mỗi row: [x, y, w, h, ...landmarks..., score]
            int x = (int) faces.get(i, 0)[0];
            int y = (int) faces.get(i, 1)[0];
            int w = (int) faces.get(i, 2)[0];
            int h = (int) faces.get(i, 3)[0];

            // Vẽ bounding box xanh lá
            Imgproc.rectangle(
                    result,
                    new Point(x, y),
                    new Point(x + w, y + h),
                    new Scalar(0, 255, 0), 2
            );

            // Hiện % confidence góc trên box
            float score = (float) faces.get(i, 14)[0];
            String label = String.format("%.0f%%", score * 100);
            Imgproc.putText(
                    result, label,
                    new Point(x, y - 5),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.5, new Scalar(0, 255, 0), 1
            );
        }
        return result;
    }

    // Đếm số khuôn mặt phát hiện được
    public int getFaceCount(Mat faces) {
        return faces.rows();
    }

    /** cải tiến
     * Crop vùng khuôn mặt thứ "index" từ ảnh gốc
     * @param original  Ảnh gốc đầy đủ
     * @param faces     Ma trận kết quả từ detect()
     * @param index     Chỉ số khuôn mặt muốn crop (bắt đầu từ 0)
     * @return          Mat chứa vùng ảnh khuôn mặt, hoặc Mat rỗng nếu lỗi
     */
    public Mat cropFace(Mat original, Mat faces, int index) {
        try {
            // Lấy tọa độ khuôn mặt từ hàng index trong ma trận faces
            int x = (int) faces.get(index, 0)[0];
            int y = (int) faces.get(index, 1)[0];
            int w = (int) faces.get(index, 2)[0];
            int h = (int) faces.get(index, 3)[0];

            // Giới hạn tọa độ trong phạm vi ảnh, tránh crop ra ngoài
            int x1 = Math.max(0, x);
            int y1 = Math.max(0, y);
            int x2 = Math.min(original.cols(), x1 + w);
            int y2 = Math.min(original.rows(), y1 + h);

            // Tạo vùng crop và trả về
            Rect roi = new Rect(x1, y1, x2 - x1, y2 - y1);
            return new Mat(original, roi);

        } catch (Exception e) {
            System.out.println("❌ cropFace lỗi tại index " + index + ": " + e.getMessage());
            return new Mat(); // Trả về Mat rỗng nếu có lỗi
        }
    }

    /**
     * Vẽ bounding box màu theo kết quả phân loại khẩu trang
     * @param img         Ảnh cần vẽ lên
     * @param face        Tọa độ vùng khuôn mặt (Rect)
     * @param label       "MASK" hoặc "NO_MASK"
     * @param confidence  Độ tin cậy (0-100%)
     * @return            Ảnh đã vẽ box và nhãn
     */
    public Mat drawMaskBox(Mat img, Rect face, String label, float confidence) {
        // Xanh lá nếu có khẩu trang, đỏ nếu không
        Scalar color = label.equals("MASK")
                ? new Scalar(0, 255, 0)   // Xanh lá = có khẩu trang
                : new Scalar(0, 0, 255);  // Đỏ = không có khẩu trang

        // Vẽ hình chữ nhật quanh khuôn mặt
        Imgproc.rectangle(img, face, color, 2);

        // Hiển thị nhãn + % confidence phía trên box
        String text = label + " " + String.format("%.1f", confidence) + "%";
        Imgproc.putText(
                img, text,
                new Point(face.x, face.y - 10),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.6, color, 2
        );

        return img;
    }
}