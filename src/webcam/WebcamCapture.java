package webcam;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * WebcamCapture — wrapper gọn cho VideoCapture của OpenCV.
 */
public class WebcamCapture {

    private VideoCapture camera;
    private final int width;
    private final int height;
    private int cameraIndex = 0;

    public WebcamCapture() {
        this(640, 480);
    }

    public WebcamCapture(int width, int height) {
        this.width  = width;
        this.height = height;
    }

    /** Mở webcam mặc định (index 0). */
    public void openCamera() throws Exception {
        openCamera(0);
    }

    /** Mở webcam theo index. */
    public void openCamera(int index) throws Exception {
        this.cameraIndex = index;
        camera = new VideoCapture(index);
        camera.set(Videoio.CAP_PROP_FRAME_WIDTH,  width);
        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, height);

        if (!camera.isOpened()) {
            throw new Exception("Khong the mo webcam " + index
                    + " (kiem tra ket noi camera)");
        }

        int actualW = (int) camera.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int actualH = (int) camera.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        System.out.println("Webcam da mo. Do phan giai: " + actualW + "x" + actualH);
    }

    /**
     * Đọc 1 frame từ webcam.
     * Trả về Mat rỗng nếu lỗi.
     */
    public Mat readFrame() {
        Mat frame = new Mat();
        if (camera != null && camera.isOpened()) {
            camera.read(frame);
        }
        return frame;
    }

    public boolean isOpened() {
        return camera != null && camera.isOpened();
    }

    public void closeCamera() {
        if (camera != null && camera.isOpened()) {
            camera.release();
            System.out.println("Webcam da dong.");
        }
    }

    public int getWidth()  { return width;  }
    public int getHeight() { return height; }
    public int getCameraIndex() { return cameraIndex; }
}