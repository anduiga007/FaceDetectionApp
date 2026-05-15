package detection;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class TestFaceDetector {

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        String cascadePath = "resources/haarcascade_frontalface_default.xml";
        String imagePath   = "test.jpg"; // file có sẵn trong project

        Mat original = Imgcodecs.imread(imagePath);
        if (original.empty()) {
            System.out.println("Không đọc được ảnh!");
            return;
        }

        Mat grayImg = new Mat();
        Imgproc.cvtColor(original, grayImg, Imgproc.COLOR_BGR2GRAY);

        FaceDetector detector = new FaceDetector(cascadePath);
        MatOfRect faces = detector.detect(grayImg);

        System.out.println("Số mặt detect được: " + faces.toArray().length);
    }
}