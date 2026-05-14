package preprocessing;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class ImagePreprocessor {

    public Mat loadImage(String path) throws Exception {
        Mat img = Imgcodecs.imread(path);
        if (img.empty()) {
            throw new Exception("Không đọc được ảnh: " + path);
        }
        return img;
    }

    public Mat toGrayscale(Mat src) {
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        return gray;
    }

    public Mat equalize(Mat gray) {
        Mat equalized = new Mat();
        Imgproc.equalizeHist(gray, equalized);
        return equalized;
    }
}