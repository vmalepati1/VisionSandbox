package org.waltonrobotics.visionsandbox;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.bilateralFilter;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.line;
import static org.opencv.videoio.Videoio.CV_CAP_ANDROID;

public class TestingActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener {

    private static final String  TAG = "TestingActivity";

    private CameraBridgeViewBase mOpenCvCameraView;

    private boolean showEdgeArray = true;
    private List<Point> edgeArray = new ArrayList<>();

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    /* Now enable camera view to start receiving frames */
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    TAG.hashCode());
        }

        Log.d(TAG, "Creating and setting view.");
        mOpenCvCameraView = (CameraBridgeViewBase) new JavaCameraView(this, -1);
        setContentView(mOpenCvCameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setMaxFrameSize(640, 480);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d("TestingActivity", Integer.toString(width));
        Log.d("TestingActivity", Integer.toString(height));
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        int stepSize = 8;

        edgeArray = new ArrayList<>();

        Mat imgGray = new Mat();

        cvtColor(inputFrame, imgGray, COLOR_BGR2GRAY);

        Mat imgGrayWithBilateral = new Mat();

        bilateralFilter(imgGray, imgGrayWithBilateral, 9,30,30);

        Mat imgEdge = new Mat();

        Canny(imgGrayWithBilateral, imgEdge, 50, 100);

        int imageWidth = imgEdge.width() - 1;
        int imageHeight = imgEdge.height() - 1;

        for (int j = 0; j < imageWidth; j += stepSize) {
            for (int i = imageHeight - 5; i > 0; i--) {
                if (imgEdge.get(i, j)[0] == 255) {
                    edgeArray.add(new Point(j, i));
                    break;
                }

                if (i == 1) {
                    edgeArray.add(new Point(j, 0));
                }
            }
        }

        if (showEdgeArray) {
            for (int x = 0; x < edgeArray.size() - 1; x++) {
                line(inputFrame, edgeArray.get(x), edgeArray.get(x + 1), new Scalar(0, 255, 0), 1);
            }

            for (int x = 0; x < edgeArray.size(); x++) {
                line(inputFrame, new Point(x * stepSize, imageHeight), edgeArray.get(x), new Scalar(0, 255, 0), 1);
            }
        }

        return imgEdge;
    }

}
