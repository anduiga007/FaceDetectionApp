package ui;

import detection.AIZOODetector;
import detection.DetectionResult;
import detection.FaceTracker;
import detection.ResourcePaths;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import webcam.WebcamCapture;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AppUI — Giao diện chính của hệ thống phát hiện khẩu trang y tế.
 *
 * Chức năng:
 *  1. Webcam thời gian thực
 *  2. Phân tích ảnh tĩnh
 *  3. Xuất báo cáo
 */
public class AppUI extends JFrame {

    // ── Palette màu y tế ─────────────────────────────────────────────────
    static final Color C_NAVY    = new Color(0x0D, 0x47, 0xA1);
    static final Color C_BLUE    = new Color(0x19, 0x76, 0xD2);
    static final Color C_LBLUE   = new Color(0xBB, 0xDE, 0xFB);
    static final Color C_BG      = new Color(0xEE, 0xF2, 0xF7);
    static final Color C_WHITE   = Color.WHITE;
    static final Color C_GREEN   = new Color(0x2E, 0x7D, 0x32);
    static final Color C_LGREEN  = new Color(0xC8, 0xE6, 0xC9);
    static final Color C_RED     = new Color(0xC6, 0x28, 0x28);
    static final Color C_LRED    = new Color(0xFF, 0xCC, 0xBC);
    static final Color C_AMBER   = new Color(0xE6, 0x50, 0x00);
    static final Color C_SIDEBAR = new Color(0x1A, 0x23, 0x7E);
    static final Color C_TEXT    = new Color(0x21, 0x21, 0x21);
    static final Color C_MUTED   = new Color(0x75, 0x75, 0x75);
    static final Font  F_TITLE   = new Font("Arial", Font.BOLD,  22);
    static final Font  F_HEAD    = new Font("Arial", Font.BOLD,  15);
    static final Font  F_BODY    = new Font("Arial", Font.PLAIN, 13);
    static final Font  F_SMALL   = new Font("Arial", Font.PLAIN, 11);
    static final Font  F_MONO    = new Font("Monospaced", Font.BOLD, 30);

    // ── Session stats (đếm theo người duy nhất, không cộng theo frame) ───
    private int totalMask   = 0;
    private int totalNoMask = 0;

    // ── Webcam control ────────────────────────────────────────────────────
    private volatile boolean webcamRunning = false;

    // ─────────────────────────────────────────────────────────────────────

    public AppUI() {
        setTitle("He Thong Phat Hien Khau Trang | Medical Vision System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(860, 560));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);
        root.add(buildHeader(),      BorderLayout.NORTH);
        root.add(buildFeatureArea(), BorderLayout.CENTER);
        root.add(buildFooter(),      BorderLayout.SOUTH);
        setContentPane(root);

        pack();
        setSize(900, 620);
        setLocationRelativeTo(null);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  MAIN WINDOW
    // ═════════════════════════════════════════════════════════════════════

    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0,0, C_NAVY, getWidth(), getHeight(), C_BLUE));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        hdr.setPreferredSize(new Dimension(0, 115));
        hdr.setBorder(new EmptyBorder(18, 32, 18, 32));

        // Left — logo + title
        JPanel left = new JPanel(new GridLayout(3, 1, 0, 3));
        left.setOpaque(false);

        JLabel badge = makeLabel("+ MEDICAL VISION  |  KHOA CONG NGHE THONG TIN", F_SMALL, C_LBLUE);
        JLabel title = makeLabel("HE THONG PHAT HIEN KHAU TRANG", F_TITLE, C_WHITE);
        JLabel sub   = makeLabel("Face Mask Detection  |  OpenCV 4.12  |  MobileNetV2", F_SMALL, C_LBLUE);

        left.add(badge);
        left.add(title);
        left.add(sub);

        // Right — live clock
        JLabel clock = makeLabel(nowTime(), new Font("Arial", Font.BOLD, 20), C_WHITE);
        clock.setHorizontalAlignment(JLabel.RIGHT);
        new Timer(1000, e -> clock.setText(nowTime())).start();

        hdr.add(left,  BorderLayout.CENTER);
        hdr.add(clock, BorderLayout.EAST);
        return hdr;
    }

    private JPanel buildFeatureArea() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(C_BG);
        wrap.setBorder(new EmptyBorder(30, 40, 20, 40));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill    = GridBagConstraints.BOTH;
        gc.insets  = new Insets(12, 12, 12, 12);
        gc.weighty = 1.0;

        // Card 1 — Webcam
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 1.0;
        wrap.add(makeFeatureCard(
            "WEBCAM REAL-TIME",
            "Phat hien truc tiep qua camera",
            "Bat dau",
            C_BLUE,
            "CAM",
            this::openWebcamWindow
        ), gc);

        // Card 2 — Static image
        gc.gridx = 1;
        wrap.add(makeFeatureCard(
            "KIEM TRA ANH TINH",
            "Upload anh va phan tich khau trang",
            "Upload anh",
            C_GREEN,
            "IMG",
            this::openImageAnalysis
        ), gc);

        // Card 3 — Report (bottom, full width)
        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 2; gc.weighty = 0.5;
        wrap.add(makeFeatureCard(
            "XUAT BAO CAO",
            "Xem lich su phien lam viec va xuat bao cao (report.txt)",
            "Xuat bao cao",
            C_AMBER,
            "DOC",
            this::exportReport
        ), gc);

        return wrap;
    }

    private JPanel buildFooter() {
        JPanel ft = new JPanel(new BorderLayout());
        ft.setBackground(C_NAVY);
        ft.setPreferredSize(new Dimension(0, 28));
        ft.setBorder(new EmptyBorder(0, 20, 0, 20));

        JLabel left  = makeLabel("OpenCV 4.12.0  |  MobileNetV2  |  YuNet Face Detector", F_SMALL, C_LBLUE);
        JLabel right = makeLabel("Nhom 3  —  2025", F_SMALL, C_LBLUE);
        right.setHorizontalAlignment(JLabel.RIGHT);

        ft.add(left,  BorderLayout.WEST);
        ft.add(right, BorderLayout.EAST);
        return ft;
    }

    /** Tạo feature card có gradient accent strip phía trên. */
    private JPanel makeFeatureCard(String title, String desc, String btnText,
                                   Color accent, String iconText, Runnable action) {
        // Outer panel — simulates shadow
        JPanel shadow = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Shadow
                g2.setColor(new Color(0, 0, 0, 28));
                g2.fillRoundRect(3, 3, getWidth()-3, getHeight()-3, 14, 14);
                // Card background
                g2.setColor(C_WHITE);
                g2.fillRoundRect(0, 0, getWidth()-4, getHeight()-4, 14, 14);
                // Accent strip
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth()-4, 7, 7, 7);
                g2.fillRect(0, 3, getWidth()-4, 5);
                g2.dispose();
            }
        };
        shadow.setOpaque(false);
        shadow.setBorder(new EmptyBorder(18, 22, 18, 22));

        // Content
        JPanel content = new JPanel(new BorderLayout(12, 8));
        content.setOpaque(false);

        // Left: icon badge
        JLabel icon = makeLabel(iconText, new Font("Arial", Font.BOLD, 13), C_WHITE);
        icon.setHorizontalAlignment(JLabel.CENTER);
        icon.setOpaque(true);
        icon.setBackground(accent);
        icon.setPreferredSize(new Dimension(52, 52));
        icon.setBorder(BorderFactory.createLineBorder(accent.darker(), 2, true));

        // Center: title + description
        JPanel info = new JPanel(new GridLayout(2, 1, 0, 4));
        info.setOpaque(false);
        info.add(makeLabel(title, F_HEAD, C_NAVY));
        info.add(makeLabel(desc,  F_BODY, C_MUTED));

        // Right: button
        JButton btn = new JButton(btnText + "  →") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? accent.darker() : accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(C_WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(130, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());

        content.add(icon, BorderLayout.WEST);
        content.add(info, BorderLayout.CENTER);
        content.add(btn,  BorderLayout.EAST);

        shadow.add(content, BorderLayout.CENTER);
        return shadow;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  FEATURE 1 — WEBCAM REAL-TIME
    // ═════════════════════════════════════════════════════════════════════

    private void openWebcamWindow() {
        if (webcamRunning) {
            JOptionPane.showMessageDialog(this,
                "Webcam dang chay. Vui long dong cua so webcam truoc.",
                "Thong bao", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFrame wf = new JFrame("Phat Hien Khau Trang — Webcam Real-Time");
        wf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        wf.setResizable(false);

        // ── Video panel ───────────────────────────────────────────────
        JLabel videoLabel = new JLabel();
        videoLabel.setPreferredSize(new Dimension(640, 480));
        videoLabel.setBackground(Color.BLACK);
        videoLabel.setOpaque(true);
        videoLabel.setHorizontalAlignment(JLabel.CENTER);
        videoLabel.setText("Dang khoi dong camera...");
        videoLabel.setForeground(Color.GRAY);
        videoLabel.setFont(F_BODY);

        JPanel videoWrap = new JPanel(new BorderLayout());
        videoWrap.setBackground(Color.BLACK);
        videoWrap.add(videoLabel);

        // ── Stats sidebar ─────────────────────────────────────────────
        StatsPanel statsPanel = new StatsPanel();

        // ── Bottom controls ───────────────────────────────────────────
        JButton btnStop   = makeSolidButton("Dung camera", C_RED);
        JButton btnSnap   = makeSolidButton("Chup man hinh", C_BLUE);
        JButton btnExport = makeSolidButton("Xuat bao cao", C_GREEN);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        controls.setBackground(new Color(0x26, 0x32, 0x38));
        controls.add(btnStop);
        controls.add(btnSnap);
        controls.add(btnExport);

        // Layout
        JPanel main = new JPanel(new BorderLayout());
        main.add(videoWrap,  BorderLayout.CENTER);
        main.add(statsPanel, BorderLayout.EAST);
        main.add(controls,   BorderLayout.SOUTH);

        wf.setContentPane(main);
        wf.pack();
        wf.setLocationRelativeTo(this);
        wf.setVisible(true);

        // ── Start capture thread ───────────────────────────────────────
        webcamRunning = true;
        final Mat[] lastFrame = {null};
        final int[] liveSession = {0, 0};
        final AtomicBoolean sessionSaved = new AtomicBoolean(false);

        Thread captureThread = new Thread(() -> {
            WebcamCapture cam      = new WebcamCapture(640, 480);
            FaceTracker   tracker  = new FaceTracker();
            try {
                cam.openCamera();
                String proto = ResourcePaths.resolve("resources/face_mask_detection.prototxt");
                String model = ResourcePaths.resolve("resources/face_mask_detection.caffemodel");
                AIZOODetector detector = new AIZOODetector(proto, model);

                while (webcamRunning && wf.isVisible()) {
                    Mat frame = cam.readFrame();
                    if (frame == null || frame.empty()) { Thread.sleep(30); continue; }

                    // 1. Phát hiện khuôn mặt + phân loại
                    java.util.List<DetectionResult> results = detector.detect(frame);

                    // 2. Tracker trả về track đã gộp (1 khung / mặt)
                    java.util.List<FaceTracker.Track> tracks = tracker.update(results);

                    // 3. Vẽ kết quả với ID người
                    drawTracked(frame, tracks);

                    // 4. Lấy stats từ tracker (số người DUY NHẤT)
                    int curFace    = tracker.currentFaceCount();
                    int curMask    = tracker.currentMask();
                    int curNoMask  = tracker.currentNoMask();
                    int totMask    = tracker.totalUniqueMask();
                    int totNoMask  = tracker.totalUniqueNoMask();
                    liveSession[0] = totMask;
                    liveSession[1] = totNoMask;

                    lastFrame[0] = frame.clone();

                    // 5. HUD timestamp
                    org.opencv.imgproc.Imgproc.putText(frame,
                            new java.text.SimpleDateFormat("HH:mm:ss  dd/MM/yyyy").format(new java.util.Date()),
                            new org.opencv.core.Point(8, 20),
                            org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX,
                            0.5, new Scalar(200, 200, 200), 1);

                    BufferedImage img = matToBufferedImage(frame);

                    SwingUtilities.invokeLater(() -> {
                        if (img != null) videoLabel.setIcon(new ImageIcon(img));
                        videoLabel.setText("");
                        statsPanel.update(curFace, curMask, curNoMask, totMask, totNoMask);
                    });

                    frame.release();
                    Thread.sleep(10);
                }

                cam.closeCamera();

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(wf,
                            "Loi: " + ex.getMessage(), "Loi", JOptionPane.ERROR_MESSAGE);
                    wf.dispose();
                });
            }
        }, "WebcamCapture");
        captureThread.setDaemon(true);
        captureThread.start();

        Runnable finishWebcamSession = () -> {
            webcamRunning = false;
            if (sessionSaved.compareAndSet(false, true)) {
                totalMask   += liveSession[0];
                totalNoMask += liveSession[1];
            }
        };

        // Stop khi đóng cửa sổ
        wf.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                finishWebcamSession.run();
            }
        });

        // Nút Dừng
        btnStop.addActionListener(e -> {
            finishWebcamSession.run();
            wf.dispose();
        });

        // Nút Chụp
        btnSnap.addActionListener(e -> {
            if (lastFrame[0] != null && !lastFrame[0].empty()) {
                String path = "snapshot_"
                        + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
                        + ".jpg";
                Imgcodecs.imwrite(path, lastFrame[0]);
                JOptionPane.showMessageDialog(wf,
                        "Da luu: " + path, "Chup man hinh", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Nút Xuất báo cáo (gồm cả phiên webcam đang chạy)
        btnExport.addActionListener(e -> exportReport(
                totalMask + liveSession[0],
                totalNoMask + liveSession[1]));
    }

    // ═════════════════════════════════════════════════════════════════════
    //  FEATURE 2 — STATIC IMAGE ANALYSIS
    // ═════════════════════════════════════════════════════════════════════

    private void openImageAnalysis() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chon anh can kiem tra");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Anh (*.jpg, *.png, *.jpeg, *.bmp)", "jpg","jpeg","png","bmp"));

        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String path = fc.getSelectedFile().getAbsolutePath();

        // Chạy trong thread riêng để không block EDT
        new Thread(() -> {
            try {
                Mat image = Imgcodecs.imread(path);
                if (image.empty()) {
                    showError("Khong doc duoc anh: " + path);
                    return;
                }

                AIZOODetector detector = new AIZOODetector(
                        ResourcePaths.resolve("resources/face_mask_detection.prototxt"),
                        ResourcePaths.resolve("resources/face_mask_detection.caffemodel"));

                java.util.List<DetectionResult> results = detector.detect(image);

                if (results.isEmpty()) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Khong phat hien khuon mat trong anh.",
                            "Ket qua", JOptionPane.INFORMATION_MESSAGE));
                    return;
                }

                int maskCnt = 0, noMaskCnt = 0;
                Mat result = image.clone();
                detector.drawResults(result, results);

                for (DetectionResult r : results) {
                    if (r.hasMask()) maskCnt++;
                    else            noMaskCnt++;
                }

                totalMask   += maskCnt;
                totalNoMask += noMaskCnt;

                BufferedImage bimg = matToBufferedImage(result);
                final int total = results.size(), mc2 = maskCnt, nc2 = noMaskCnt;
                double rate = total > 0 ? maskCnt * 100.0 / total : 0;

                SwingUtilities.invokeLater(() ->
                        showImageResult(bimg, total, mc2, nc2, rate));

            } catch (Exception ex) {
                showError("Loi phan tich anh: " + ex.getMessage());
            }
        }, "ImageAnalysis").start();
    }

    private void showImageResult(BufferedImage img, int total, int mask, int noMask, double rate) {
        JFrame rf = new JFrame("Ket Qua Phan Tich — " + mask + "/" + total + " co khau trang");
        rf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Scale image to max 800×600
        int w = img.getWidth(), h = img.getHeight();
        if (w > 800 || h > 600) {
            double s = Math.min(800.0/w, 600.0/h);
            w = (int)(w*s); h = (int)(h*s);
        }
        ImageIcon icon = new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        JLabel imgLbl = new JLabel(icon);

        // Stats bar below image
        JPanel statBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        statBar.setBackground(C_NAVY);
        statBar.add(statChip("Tong mat", total + "", C_BLUE));
        statBar.add(statChip("Co khau trang", mask + "", C_GREEN));
        statBar.add(statChip("Khong khau trang", noMask + "", C_RED));
        statBar.add(statChip("Ti le tuan thu", String.format("%.0f%%", rate),
                rate >= 80 ? C_GREEN : rate >= 50 ? C_AMBER : C_RED));

        rf.setLayout(new BorderLayout());
        rf.add(new JScrollPane(imgLbl), BorderLayout.CENTER);
        rf.add(statBar, BorderLayout.SOUTH);
        rf.pack();
        rf.setLocationRelativeTo(this);
        rf.setVisible(true);

        // Alert
        if (noMask > 0) {
            JOptionPane.showMessageDialog(rf,
                    "Phat hien " + noMask + " nguoi KHONG deo khau trang!",
                    "CANH BAO", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  FEATURE 3 — EXPORT REPORT
    // ═════════════════════════════════════════════════════════════════════

    private void exportReport() {
        exportReport(totalMask, totalNoMask);
    }

    private void exportReport(int maskCount, int noMaskCount) {
        String filename = "report_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
                + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            int total = maskCount + noMaskCount;
            double rate = total > 0 ? maskCount * 100.0 / total : 0;

            pw.println("=========================================");
            pw.println("   BAO CAO KET QUA KIEM TRA KHAU TRANG");
            pw.println("=========================================");
            pw.println("Ngay gio     : " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
            pw.println("He thong     : Medical Vision System v1.0");
            pw.println("-----------------------------------------");
            pw.println("Tong nguoi (duy nhat): " + total + " nguoi");
            pw.println("  Co khau trang    : " + maskCount   + " nguoi");
            pw.println("  Khong khau trang : " + noMaskCount + " nguoi");
            pw.println(String.format("Ti le tuan thu   : %.2f%%", rate));
            pw.println("-----------------------------------------");
            pw.println("Trang thai: " + (noMaskCount == 0 ? "AN TOAN" : "CAN CANH BAO"));
            pw.println("=========================================");

            JOptionPane.showMessageDialog(this,
                    "Da xuat bao cao: " + filename,
                    "Thanh cong", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Loi xuat bao cao: " + ex.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  INNER CLASS — STATS SIDEBAR
    // ═════════════════════════════════════════════════════════════════════

    private static class StatsPanel extends JPanel {

        private int faces, mask, noMask, totalMask, totalNoMask;
        private boolean alertBlink = false;

        // Live labels
        private final JLabel lFaces, lMask, lNoMask, lRate;
        private final JProgressBar pbRate;
        private final JPanel alertBanner;
        private final JLabel lAlert;

        // Session labels
        private final JLabel lSessionMask, lSessionNoMask, lSessionRate;

        StatsPanel() {
            setPreferredSize(new Dimension(260, 480));
            setBackground(C_SIDEBAR);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(18, 16, 18, 16));

            // ── Header ────────────────────────────────────────────────
            add(sideLabel("THONG KE THEO DOI", F_HEAD, C_LBLUE));
            add(Box.createVerticalStrut(6));
            add(makeSep());
            add(Box.createVerticalStrut(14));

            // ── Live section ──────────────────────────────────────────
            add(sideLabel("FRAME HIEN TAI", F_SMALL, new Color(0xBB, 0xDE, 0xFB)));
            add(Box.createVerticalStrut(8));

            lFaces  = bigStatLabel("0", C_WHITE);
            lMask   = bigStatLabel("0", C_LGREEN);
            lNoMask = bigStatLabel("0", C_LRED);
            lRate   = sideLabel("0%", F_HEAD, C_WHITE);

            add(statRow("Khuon mat:",   lFaces));
            add(Box.createVerticalStrut(6));
            add(statRow("Co khau trang:", lMask));
            add(Box.createVerticalStrut(6));
            add(statRow("Khong deo:",   lNoMask));
            add(Box.createVerticalStrut(10));

            // Progress bar
            add(sideLabel("Ti le tuan thu:", F_SMALL, C_LBLUE));
            add(Box.createVerticalStrut(4));
            pbRate = new JProgressBar(0, 100);
            pbRate.setStringPainted(true);
            pbRate.setForeground(C_GREEN);
            pbRate.setBackground(new Color(0x37, 0x47, 0x4F));
            pbRate.setBorderPainted(false);
            pbRate.setPreferredSize(new Dimension(220, 22));
            pbRate.setMaximumSize(new Dimension(220, 22));
            add(pbRate);
            add(Box.createVerticalStrut(16));
            add(makeSep());
            add(Box.createVerticalStrut(14));

            // ── Session section ───────────────────────────────────────
            add(sideLabel("PHIEN (MOI NGUOI 1 LAN)", F_SMALL, new Color(0xBB, 0xDE, 0xFB)));
            add(Box.createVerticalStrut(8));

            lSessionMask    = sideLabel("0", F_BODY, C_LGREEN);
            lSessionNoMask  = sideLabel("0", F_BODY, C_LRED);
            lSessionRate    = sideLabel("N/A", F_BODY, C_WHITE);

            add(statRow("Co khau trang:", lSessionMask));
            add(Box.createVerticalStrut(5));
            add(statRow("Khong deo:",     lSessionNoMask));
            add(Box.createVerticalStrut(5));
            add(statRow("Ti le chung:",   lSessionRate));
            add(Box.createVerticalStrut(16));
            add(makeSep());
            add(Box.createVerticalStrut(14));

            // ── Alert banner ──────────────────────────────────────────
            alertBanner = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(alertBlink ? C_RED : new Color(0x37, 0x47, 0x4F));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
            };
            alertBanner.setOpaque(false);
            alertBanner.setBorder(new EmptyBorder(10, 10, 10, 10));
            alertBanner.setMaximumSize(new Dimension(228, 70));
            alertBanner.setPreferredSize(new Dimension(228, 70));

            lAlert = new JLabel("HE THONG BINH THUONG", JLabel.CENTER);
            lAlert.setFont(new Font("Arial", Font.BOLD, 11));
            lAlert.setForeground(C_WHITE);
            alertBanner.add(lAlert, BorderLayout.CENTER);
            add(alertBanner);

            // Blink timer
            new Timer(600, e -> {
                alertBlink = !alertBlink;
                if (noMask > 0) alertBanner.repaint();
            }).start();
        }

        void update(int f, int m, int nm, int totalM, int totalNM) {
            this.faces       = f;
            this.mask        = m;
            this.noMask      = nm;
            this.totalMask   = totalM;
            this.totalNoMask = totalNM;

            lFaces.setText(String.valueOf(f));
            lMask.setText(String.valueOf(m));
            lNoMask.setText(String.valueOf(nm));

            int rate = (f > 0) ? m * 100 / f : 0;
            pbRate.setValue(rate);
            pbRate.setString(rate + "%");
            pbRate.setForeground(rate >= 80 ? C_GREEN : rate >= 50 ? C_AMBER : C_RED);

            int totalAll = totalM + totalNM;
            lSessionMask.setText(String.valueOf(totalM));
            lSessionNoMask.setText(String.valueOf(totalNM));
            lSessionRate.setText(totalAll > 0
                    ? String.format("%.1f%%", totalM * 100.0 / totalAll) : "N/A");

            if (nm > 0) {
                lAlert.setText("CANH BAO: " + nm + " NGUOI KHONG DEO KT!");
                lAlert.setForeground(C_WHITE);
            } else if (f > 0) {
                lAlert.setText("HE THONG BINH THUONG");
                lAlert.setForeground(new Color(0xA5, 0xD6, 0xA7));
                alertBlink = false;
                alertBanner.repaint();
            }
        }

        // ── Helpers ───────────────────────────────────────────────────
        private JLabel sideLabel(String t, Font f, Color c) {
            JLabel l = new JLabel(t);
            l.setFont(f); l.setForeground(c);
            l.setAlignmentX(LEFT_ALIGNMENT);
            return l;
        }

        private JLabel bigStatLabel(String t, Color c) {
            JLabel l = new JLabel(t);
            l.setFont(new Font("Arial", Font.BOLD, 26));
            l.setForeground(c);
            return l;
        }

        private JPanel statRow(String labelText, JLabel value) {
            JPanel row = new JPanel(new BorderLayout(4, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(228, 32));
            JLabel k = new JLabel(labelText);
            k.setFont(F_SMALL);
            k.setForeground(C_LBLUE);
            row.add(k,     BorderLayout.WEST);
            row.add(value, BorderLayout.EAST);
            return row;
        }

        private JSeparator makeSep() {
            JSeparator sep = new JSeparator();
            sep.setForeground(new Color(0x37, 0x47, 0x4F));
            sep.setMaximumSize(new Dimension(228, 1));
            return sep;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ═════════════════════════════════════════════════════════════════════

    private static BufferedImage matToBufferedImage(Mat mat) {
        try {
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".jpg", mat, mob);
            return ImageIO.read(new ByteArrayInputStream(mob.toArray()));
        } catch (Exception e) {
            return null;
        }
    }

    private static JLabel makeLabel(String text, Font font, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(fg);
        return l;
    }

    private static JButton makeSolidButton(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(C_WHITE);
        b.setFont(new Font("Arial", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setPreferredSize(new Dimension(160, 36));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel statChip(String key, String val, Color color) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setBackground(color.darker().darker());
        p.setBorder(new EmptyBorder(8, 14, 8, 14));
        JLabel k = makeLabel(key.toUpperCase(), F_SMALL, new Color(0xEE, 0xEE, 0xEE));
        JLabel v = makeLabel(val, new Font("Arial", Font.BOLD, 20), C_WHITE);
        k.setHorizontalAlignment(JLabel.CENTER);
        v.setHorizontalAlignment(JLabel.CENTER);
        p.add(k); p.add(v);
        return p;
    }

    private void showError(String msg) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "Loi", JOptionPane.ERROR_MESSAGE));
    }

    private static String nowTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    /** Vẽ bbox + ID — chỉ track đã gộp (1 mặt = 1 khung). */
    private static void drawTracked(Mat frame, java.util.List<FaceTracker.Track> tracks) {
        for (FaceTracker.Track t : tracks) {
            boolean masked  = DetectionResult.MASK.equals(t.label);
            Scalar boxColor = masked
                    ? new Scalar(40, 200, 40)
                    : new Scalar(30, 30, 220);

            Imgproc.rectangle(frame, t.rect, boxColor, 2);

            String text = String.format("#%d  %s  %.0f%%",
                    t.id,
                    masked ? "[OK] CO KHAU TRANG" : "[!] KHONG KHAU TRANG",
                    t.confidence);

            int[] baseline = {0};
            Size ts = Imgproc.getTextSize(text, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, 1, baseline);
            int ty = (t.rect.y > 22) ? t.rect.y - 5 : t.rect.y + t.rect.height + (int) ts.height + 6;

            Imgproc.rectangle(frame,
                    new org.opencv.core.Point(t.rect.x, ty - (int) ts.height - 4),
                    new org.opencv.core.Point(t.rect.x + (int) ts.width + 6, ty + 2),
                    boxColor, Imgproc.FILLED);
            Imgproc.putText(frame, text,
                    new org.opencv.core.Point(t.rect.x + 3, ty - 1),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.5,
                    new Scalar(255, 255, 255), 1);
        }
    }
}