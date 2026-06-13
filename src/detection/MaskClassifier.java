package detection;

import org.opencv.core.*;
import org.opencv.dnn.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * MaskClassifier — MobileNetV2 đã chuyển đổi sang NCHW.
 *
 * Model: mask_classifier_nchw.onnx
 *   (được tạo từ mask_classifier.onnx bằng convert_model.py)
 *   - Input : NCHW [1, 3, 224, 224]  ← OpenCV blobFromImage tiêu chuẩn
 *   - Bên trong model có Transpose NCHW→NHWC
 *   - Output: [1, 3]  (3 classes)
 *
 * Label mapping:
 *   0 → incorect_mask   (đeo sai cách)  → MASK
 *   1 → with_mask       (đeo đúng)      → MASK
 *   2 → without_mask    (không đeo)     → NO_MASK
 */
public class MaskClassifier {

    private static final String[] RAW_LABELS = {"incorect_mask", "with_mask", "without_mask"};

    private final Net net;
    private static final int INPUT_SIZE = 224;

    // ── Temporal smoothing ────────────────────────────────────────────────
    private static final int SMOOTH_FRAMES = 5;
    private final Deque<float[]> scoreBuffer = new ArrayDeque<>();

    // ── Hysteresis — chỉ đổi nhãn khi đủ tự tin ─────────────────────────
    private static final float HYSTERESIS_THRESHOLD = 55.0f;
    private String lastLabel = ClassificationResult.NO_MASK;

    // ─────────────────────────────────────────────────────────────────────

    public MaskClassifier(String modelPath) throws Exception {
        // Ưu tiên dùng file NCHW nếu tồn tại (được tạo bởi convert_model.py)
        String actualPath = tryNchwVariant(modelPath);
        net = Dnn.readNetFromONNX(actualPath);
        if (net.empty()) throw new Exception("Khong load duoc model: " + actualPath);
        System.out.println("MaskClassifier loaded: " + actualPath);
    }

    /** Nếu có file _nchw.onnx thì dùng, không thì fallback về file gốc. */
    private static String tryNchwVariant(String path) {
        String nchw = path.replace(".onnx", "_nchw.onnx");
        if (new java.io.File(nchw).exists()) {
            System.out.println("[OK] Dung model NCHW: " + nchw);
            return nchw;
        }
        System.out.println("[WARN] Khong tim thay " + nchw + ", dung file goc: " + path);
        return path;
    }

    /**
     * Phân loại + trả về confidence trong 1 lần gọi (tránh infer 2 lần / frame).
     */
    public ClassificationResult classifyWithConfidence(Mat faceImg) {
        float[] s = getSmoothedScores(faceImg);

        if (s.length < 3) {
            return new ClassificationResult(lastLabel, "unknown", 0f);
        }

        // Softmax
        double[] exp = new double[s.length];
        double   sum = 0;
        for (int i = 0; i < s.length; i++) {
            exp[i] = Math.exp(clamp(s[i]));
            sum   += exp[i];
        }

        // Winner
        int    winnerIdx  = 0;
        double winnerProb = 0;
        for (int i = 0; i < s.length; i++) {
            double prob = exp[i] / sum;
            if (prob > winnerProb) { winnerProb = prob; winnerIdx = i; }
        }

        float  confPct   = (float)(winnerProb * 100);
        String rawLabel  = RAW_LABELS[winnerIdx];
        String newLabel  = (winnerIdx == 2) ? ClassificationResult.NO_MASK
                                            : ClassificationResult.MASK;

        if (confPct > HYSTERESIS_THRESHOLD) {
            lastLabel = newLabel;
        }

        return new ClassificationResult(lastLabel, rawLabel, confPct);
    }

    public void resetBuffer() {
        scoreBuffer.clear();
        lastLabel = ClassificationResult.NO_MASK;
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private float[] getSmoothedScores(Mat faceImg) {
        float[] raw = getRawScores(faceImg);
        scoreBuffer.addLast(raw);
        if (scoreBuffer.size() > SMOOTH_FRAMES) scoreBuffer.pollFirst();

        float[] avg = new float[raw.length];
        for (float[] s : scoreBuffer)
            for (int i = 0; i < s.length; i++) avg[i] += s[i];
        int n = scoreBuffer.size();
        for (int i = 0; i < avg.length; i++) avg[i] /= n;
        return avg;
    }

    private float[] getRawScores(Mat faceImg) {
        Mat blob = preprocess(faceImg);
        net.setInput(blob);
        Mat out  = net.forward();
        Mat flat = out.reshape(1, 1);
        float[] s = new float[(int) flat.total()];
        flat.get(0, 0, s);
        return s;
    }

    /**
     * Preprocess ảnh → NCHW blob [1, 3, 224, 224] bằng blobFromImage tiêu chuẩn.
     *
     * Model mask_classifier_nchw.onnx đã được convert để nhận NCHW:
     *   - Bên trong model có sẵn Transpose NCHW→NHWC
     *   - Dùng blobFromImage (cách OpenCV khuyến nghị) → không còn lỗi Permute
     *
     * Normalize: pixel/127.5 - 1  tương đương mean=127.5, scalefactor=1/127.5
     */
    private Mat preprocess(Mat img) {
        // Resize về 224×224
        Mat resized = new Mat();
        Imgproc.resize(img, resized, new Size(INPUT_SIZE, INPUT_SIZE));

        // blobFromImage: resize, BGR→RGB (swapRB=true), normalize [-1,1]
        // Tạo NCHW blob [1, 3, 224, 224] — đúng format OpenCV DNN
        return Dnn.blobFromImage(
                resized,
                1.0 / 127.5,                   // scalefactor
                new Size(INPUT_SIZE, INPUT_SIZE),
                new Scalar(127.5, 127.5, 127.5), // mean (trừ đi) → kết quả = pixel/127.5 - 1
                true,                            // swapRB: BGR→RGB
                false                            // crop
        );
    }

    private static double clamp(float v) {
        return Math.max(-50.0, Math.min(50.0, v));
    }
}