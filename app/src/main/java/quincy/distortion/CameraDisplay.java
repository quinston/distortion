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
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

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


//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

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

            ConstraintLayout cl = (ConstraintLayout) findViewById(R.id.rootLayout);
            DistortableGLSurfaceView glView = new DistortableGLSurfaceView(getApplicationContext());
            cl.addView(glView);
        }
    }


}
