package quincy.distortion;

import android.Manifest;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW;


public class CameraDisplay extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private final int CAMERA_PERMISSIONS_REQUEST_CODE = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_display);


        requestPermissions(new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull  String[] permissions, @NonNull int[] grantResults) {
        if ((requestCode == CAMERA_PERMISSIONS_REQUEST_CODE) &&
                Arrays.equals(permissions, new String[]{Manifest.permission.CAMERA}) &&
                Arrays.equals(grantResults, new int[]{PERMISSION_GRANTED})) {

            Log.d("CameraActivity", "Got camera permissions.");

            CameraManager cm = getSystemService(CameraManager.class);

            try {
                String rearFacingCameraId = Utility.findRearFacingCameraId(cm);

                List<Surface> outputSurfaces = new ArrayList<>();

                TextureView.SurfaceTextureListener l = new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                        Log.d("TextureView.SurfaceTextureListener", "Surface texture available.");
                        try {
                            SurfaceTexture st = ((TextureView) findViewById(R.id.textureView)).getSurfaceTexture();
                            StreamConfigurationMap map = cm.getCameraCharacteristics(rearFacingCameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);


                            Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);
                            Log.d("TextureView.SurfaceTextureListener", "Available camera sizes: " + Arrays.toString(outputSizes));
                            Log.d("TextureView.SurfaceTextureListener", " Arbitrarily picking size 0: " + outputSizes[0]);
                            st.setDefaultBufferSize(outputSizes[0].getWidth(),
                                    outputSizes[0].getHeight());

                            Surface textureViewSurface = new Surface(st);
                            outputSurfaces.add(textureViewSurface);

                            cm.openCamera(rearFacingCameraId,
                                    new CameraDevice.StateCallback() {
                                        @Override
                                        public void onOpened(@NonNull CameraDevice camera) {
                                            Log.d("CameraDevice.Statecallback", "Called onOpened");

                                            try {
                                                camera.createCaptureSession(outputSurfaces,
                                                        new CameraCaptureSession.StateCallback() {
                                                            @Override
                                                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                                                Log.d("CameraCaptureSession.StateCallback", "Called onConfigured");

                                                                try {
                                                                    CaptureRequest.Builder cb = camera.createCaptureRequest(TEMPLATE_PREVIEW);
                                                                    cb.addTarget(outputSurfaces.get(0));

                                                                    cameraCaptureSession.setRepeatingRequest(cb.build(),
                                                                            new CameraCaptureSession.CaptureCallback() {
                                                                                @Override
                                                                                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                                                                    super.onCaptureCompleted(session, request, result);
                                                                                    Log.d("", "Got a frame!");
                                                                                }
                                                                            }, null);
                                                                }
                                                                catch (CameraAccessException e) {
                                                                    Log.e("createCaptureSession", "Failed to make capture request.");
                                                                }
                                                            }

                                                            @Override
                                                            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                                                                Log.e("createCaptureSession", "Failed to configure camera.");
                                                            }
                                                        },
                                                        null);
                                            }
                                            catch (CameraAccessException e) {
                                                Log.e("onSurfaceTextureAvailable", "Problem with camera connection: " + e);
                                            }
                                        }

                                        @Override
                                        public void onDisconnected(@NonNull CameraDevice camera) {
                                            Log.d("CameraDevice.Statecallback", "Called onDisconnected");
                                        }

                                        @Override
                                        public void onError(@NonNull CameraDevice camera, int error) {
                                            Log.d("CameraDevice.Statecallback", "Called onError: error=" + error);
                                        }
                                    },
                                    null);

                        }
                        catch (CameraAccessException e) {
                            Log.e("TextureView.SurfaceTextureListener onSurfaceTextureAvailable", "Couldn't get configuration map for camera.");
                        }
                        catch (SecurityException e) {
                            Log.e("TextureView.SurfaceTextureListener onSurfaceTextureAvailable", "SecurityException: was not able to access camera.");
                        }

                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

                    }
                };
                TextureView view = (TextureView)findViewById(R.id.textureView);
                if (view.isAvailable()) {
                    l.onSurfaceTextureAvailable(view.getSurfaceTexture(), view.getWidth(), view.getHeight());
                }
                else {
                    view.setSurfaceTextureListener(l);
                }
            }
            catch(CameraAccessException e){
                Log.e("CameraDisplay onCreate", "Failed to get a rear-facing camera ID.");
            }
        }
    }
}
