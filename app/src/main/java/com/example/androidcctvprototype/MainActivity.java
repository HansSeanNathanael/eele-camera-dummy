package com.example.androidcctvprototype;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewViewCamera;

    private CameraServer cameraServer;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
                MainActivity.this.startCamera();
                MainActivity.this.startServer();
            } else {
                Toast.makeText(this, "The app need camera permission to work", Toast.LENGTH_SHORT).show();

            }
        });

        this.requestPermissionLauncher.launch(Manifest.permission.CAMERA);

        this.previewViewCamera = this.findViewById(R.id.previewViewCamera);


        this.cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startServer() {
        new Thread(() -> {
            this.cameraServer = new CameraServer(new InetSocketAddress("0.0.0.0", 7711));
            this.cameraServer.run();
        }).start();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {

            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(this.previewViewCamera.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                imageAnalysis.setAnalyzer(this.cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        try {
                            Bitmap bitmap = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
                            bitmap.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());

                            int rotation = imageProxy.getImageInfo().getRotationDegrees();
                            imageProxy.close();

                            Matrix matrix = new Matrix();
                            matrix.postRotate(rotation);

                            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            bitmap.recycle();

                            MainActivity.this.cameraServer.setImage(rotatedBitmap);
                        } catch (Exception ignored) {}
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        MainActivity.this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        imageAnalysis,
                        preview
                );

            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Failed to start camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
}