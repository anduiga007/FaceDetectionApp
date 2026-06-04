package detection;

import org.opencv.core.*;
import org.opencv.dnn.*;
import org.opencv.imgproc.Imgproc;

public class MaskClassifier {
    private Net net;
    private static final int INPUT_SIZE = 224;

    // Load model .onnx
    public MaskClassifier(String modelPath) throws Exception {
        net = Dnn.readNetFromONNX(modelPath);
        if (net.empty()) {
            throw new Exception("Không load được model: " + modelPath);
        }
        System.out.println("✅ MaskClassifier loaded: " + modelPath);
    }

    // Phân loại: "MASK" hoặc "NO_MASK"
    public String classify(Mat faceImg) {
        Mat blob = preprocess(faceImg);
        net.setInput(blob);
        Mat output = net.forward();
        float[] scores = new float[2];
        output.get(0, 0, scores);
        return scores[0] > scores[1] ? "MASK" : "NO_MASK";
    }

    // % confidence
    public float getConfidence(Mat faceImg) {
        Mat blob = preprocess(faceImg);
        net.setInput(blob);
        Mat output = net.forward();
        float[] scores = new float[2];
        output.get(0, 0, scores);
        float max = Math.max(scores[0], scores[1]);
        return max * 100;
    }

    private Mat preprocess(Mat img) {
        Mat resized = new Mat();
        Imgproc.resize(img, resized, new Size(INPUT_SIZE, INPUT_SIZE));
        return Dnn.blobFromImage(resized, 1.0 / 255.0,
                new Size(INPUT_SIZE, INPUT_SIZE),
                new Scalar(0, 0, 0), true, false);
    }
}