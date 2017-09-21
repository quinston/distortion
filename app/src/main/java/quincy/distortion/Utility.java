package quincy.distortion;

import android.content.res.Resources;
import android.graphics.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

final class Utility {
    public static String findRearFacingCameraId(CameraManager cm) throws CameraAccessException, RuntimeException {
        for (String cameraId : cm.getCameraIdList()) {
            if (cm.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK) {
                return cameraId;
            }
        }
        throw new RuntimeException("No rear-facing camera found.");
    }

    public static String rawResourceToString(Resources res, int id) {
        InputStream is = res.openRawResource(id);
        return new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
    }

    public static float cheapExp(float x) {
        float t = (1 + x/16);
        t *= t;
        t *= t;
        t *= t;
        return t*t;
    }

    public static float cheapLogOneMinusX(float x) {
        return -x-x*x/2-x*x*x/3;
    }

    public static float cheapLog(float x) {
        return cheapLogOneMinusX(-1-x);
    }

    public static float cheapPow(float base, float expt) {
        return cheapExp(expt * cheapLog(base));
    }
}

