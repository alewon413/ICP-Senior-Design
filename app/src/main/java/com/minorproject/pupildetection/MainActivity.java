package com.minorproject.pupildetection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple demonstration of how to use OpenCV to detect pupil
 */

public class MainActivity extends Activity implements CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";

    private Mat mRgba;
    private Mat mGray;

    private CascadeClassifier mJavaDetectorLeftEye;

    private CameraBridgeViewBase mOpenCvCameraView;

    private Mat mIntermediateMat;
    private Mat hierarchy;

    private int cam_i = 0;

    private Mat mZoomWindow;

    private final ArrayList<Double> resList = new ArrayList<Double>();

    private Camera camera;


    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // load cascade file from application resources
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);

                    mJavaDetectorLeftEye = loadClassifier(R.raw.haarcascade_lefteye_2splits, "haarcascade_eye_left.xml",
                            cascadeDir);

                    cascadeDir.delete();
                    mOpenCvCameraView.setCameraIndex(0);

                    // Reducing resolution to avoid lag in Camera
                    mOpenCvCameraView.setMaxFrameSize(1024, 768);

                    //mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        1);
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_eyes_detect);

        mOpenCvCameraView = findViewById(R.id.eyes_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        Button resultBtn = findViewById(R.id.resultBtn) ;
        resultBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent resultIntent = new Intent(MainActivity.this, ResultActivity.class);
                        resultIntent.putExtra("resList", resList);
                        startActivity(resultIntent);
                        resList.clear();
                    }
                }
        );
        ImageButton modeBtn = findViewById(R.id.changeMode) ;
        modeBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOpenCvCameraView.disableView();
                        if(cam_i == 0) {
                            cam_i = 1;
                            mOpenCvCameraView.setCameraIndex(1);
                        } else {
                            cam_i = 0;
                            mOpenCvCameraView.setCameraIndex(0);
                        }
                        mOpenCvCameraView.enableView();
                    }
                }
        );
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        hierarchy = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();

        mIntermediateMat.release();
        hierarchy.release();

        mZoomWindow.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mZoomWindow == null)
            createAuxiliaryMats();

        Rect area = new Rect(new Point(50,50), new Point(mGray.width() - 50, mGray.height() - 50));
        detectEye(mJavaDetectorLeftEye, area, 200);
        return mRgba;
    }

    private void createAuxiliaryMats() {
        if (mGray.empty())
            return;

        int rows = mGray.rows();
        int cols = mGray.cols();

        if (mZoomWindow == null) {
            mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2 + cols / 10, cols);
        }

    }

    private Mat detectEye(CascadeClassifier classificator, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = mGray.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();

        //isolate the eyes first
        classificator.detectMultiScale(mROI, eyes, 1.15, 2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30), new Size());

        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length;) {
            Rect e = eyesArray[i];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int) e.tl().x, (int) (e.tl().y + e.height * 0.4), e.width,
                    (int) (e.height * 0.6));

            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            Core.rectangle(mRgba, eye_only_rectangle.tl(), eye_only_rectangle.br(), new Scalar(255, 255, 0, 255), 2);

            //find the pupil inside the eye rect
            detectPupil(eye_only_rectangle);

            return template;
        }

        return template;
    }

    protected void detectPupil(Rect eyeRect) {
        hierarchy = new Mat();

        Mat img = mRgba.submat(eyeRect);
        Mat img_hue = new Mat();

        Mat circles = new Mat();

        // Convert it to hue, convert to range color, and blur to remove false
        Imgproc.cvtColor(img, img_hue, Imgproc.COLOR_RGB2HSV);

        Core.inRange(img_hue, new Scalar(0, 0, 0), new Scalar(255, 255, 32), img_hue);

        Imgproc.erode(img_hue, img_hue, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));

        Imgproc.dilate(img_hue, img_hue, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6)));

        Imgproc.Canny(img_hue, img_hue, 170, 220);
        Imgproc.GaussianBlur(img_hue, img_hue, new Size(9, 9), 2, 2);
        // Apply Hough Transform to find the circles
        Imgproc.HoughCircles(img_hue, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 20, 50, 30, 7, 21);

        if (circles.cols() > 0)
            for (int x = 0; x < circles.cols(); x++) {
                double[] vCircle = circles.get(0, x);

                if (vCircle == null)
                    break;

                Point pt = new Point(Math.round(vCircle[0]), Math.round(vCircle[1]));
                int radius = (int) Math.round(vCircle[2]);

                resList.add(vCircle[2]);

                // draw the found circle
                Core.circle(img, pt, radius, new Scalar(0, 255, 0), 2);
                //Core.circle(img, pt, 3, new Scalar(0, 0, 255), 2);
            }
    }

    private CascadeClassifier loadClassifier(int rawResId, String filename, File cascadeDir) {
        CascadeClassifier classifier = null;
        try {
            InputStream is = getResources().openRawResource(rawResId);
            File cascadeFile = new File(cascadeDir, filename);
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            classifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (classifier.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                classifier = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
        return classifier;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_13, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public class OpenCvCameraView extends JavaCameraView {

        public OpenCvCameraView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public List<String> getEffectList() {
            return mCamera.getParameters().getSupportedFlashModes();
        }

        public boolean isEffectSupported() {
            return (mCamera.getParameters().getFlashMode() != null);
        }

        public String getEffect() {
            return mCamera.getParameters().getFlashMode();
        }

        public void setEffect(String effect) {
            if(mCamera != null) {
                mCamera.getParameters();
                Camera.Parameters params = mCamera.getParameters();
                params.setFlashMode(effect);
                mCamera.setParameters(params);
            }
        }

        public void cameraRelease() {
            if(mCamera != null){
                mCamera.release();
            }
        }
    }
}