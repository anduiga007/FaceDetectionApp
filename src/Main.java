import org.opencv.core.Core;
import org.opencv.core.Mat;
import preprocessing.ImagePreprocessor;

public class Main {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        ImagePreprocessor preprocessor = new ImagePreprocessor();

        try {
            Mat original = preprocessor.loadImage("test.jpg");
            System.out.println("Load OK — " + original.cols() + "x" + original.rows());

            Mat gray = preprocessor.toGrayscale(original);
            System.out.println("Grayscale OK — channels: " + gray.channels());

            Mat equalized = preprocessor.equalize(gray);
            System.out.println("Equalize OK — channels: " + equalized.channels());

            System.out.println("Pipeline chạy OK!");

        } catch (Exception e) {
            System.out.println("Lỗi: " + e.getMessage());
        }
    }
}