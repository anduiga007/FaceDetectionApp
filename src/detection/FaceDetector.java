package detection;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class FaceDetector {

    private CascadeClassifier classifier;

    public FaceDetector(String cascadePath) {
        classifier = new CascadeClassifier(cascadePath);
        if (classifier.empty()) {
            throw new RuntimeException("Không load được cascade file: " + cascadePath);
        }
        System.out.println("CascadeClassifier loaded thành công.");
    }

    public MatOfRect detect(Mat grayImg) {
        MatOfRect faces = new MatOfRect();
        classifier.detectMultiScale(
                grayImg,
                faces,
                1.1,
                5,
                0,
                new Size(30, 30),
                new Size()
        );
        return faces;
    }

    public Mat drawBoxes(Mat original, MatOfRect faces) {
        Mat result = original.clone();
        for (Rect rect : faces.toArray()) {
            Imgproc.rectangle(
                    result,
                    new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(0, 255, 0),
                    2
            );
        }
        return result;
    }
}