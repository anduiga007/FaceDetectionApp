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

    public Mat detect(Mat colorImg) {
        // YuNet cần resize input đúng kích thước
        detector.setInputSize(new Size(colorImg.cols(), colorImg.rows()));
        Mat faces = new Mat();
        detector.detect(colorImg, faces);
        return faces;
    }

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

    public int getFaceCount(Mat faces) {
        return faces.rows();
    }
}