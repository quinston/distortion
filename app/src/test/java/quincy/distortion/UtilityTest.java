package quincy.distortion;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class UtilityTest {


    @Test
    public void findRearFacingCameraId_findsItIfItIsFirst() throws Exception {
        CameraManager cm = mock(CameraManager.class);
        when(cm.getCameraIdList()).thenReturn(new String[]{"rearcamera", "id1", "id2"});

        CameraCharacteristics rearFacingCamera = mock(CameraCharacteristics.class);
        when(rearFacingCamera.get(CameraCharacteristics.LENS_FACING)).thenReturn(CameraMetadata.LENS_FACING_BACK);
        when(cm.getCameraCharacteristics("rearcamera")).thenReturn(rearFacingCamera);

        assertEquals("rearcamera", Utility.findRearFacingCameraId(cm));
    }

    @Test
    public void findRearFacingCameraId_findsItIfItIsNotFirst() throws Exception {
        CameraManager cm = mock(CameraManager.class);
        final String rearFacingCameraActualId = "id2";
        when(cm.getCameraIdList()).thenReturn(new String[]{"id0", "id1", rearFacingCameraActualId});

        CameraCharacteristics rearFacingCamera = mock(CameraCharacteristics.class);
        when(rearFacingCamera.get(CameraCharacteristics.LENS_FACING)).thenReturn(CameraMetadata.LENS_FACING_BACK);
        when(cm.getCameraCharacteristics(rearFacingCameraActualId)).thenReturn(rearFacingCamera);

        CameraCharacteristics otherCamera = mock(CameraCharacteristics.class);
        when(otherCamera.get(CameraCharacteristics.LENS_FACING)).thenReturn(CameraMetadata.LENS_FACING_FRONT);
        when(cm.getCameraCharacteristics("id0")).thenReturn(otherCamera);
        when(cm.getCameraCharacteristics("id1")).thenReturn(otherCamera);

        assertEquals(rearFacingCameraActualId, Utility.findRearFacingCameraId(cm));
    }

    @Test
    public void findRearFacingCameraId_findsFirstOneIfThereAreMultiple() throws Exception {
        CameraManager cm = mock(CameraManager.class);
        final String rearFacingCameraActualId = "id2";
        final String rearFacingCameraActualId2 = "id3";
        when(cm.getCameraIdList()).thenReturn(new String[]{"id0", "id1", rearFacingCameraActualId, "iddd", rearFacingCameraActualId2});

        CameraCharacteristics otherCamera = mock(CameraCharacteristics.class);
        when(cm.getCameraCharacteristics("id0")).thenReturn(otherCamera);
        when(cm.getCameraCharacteristics("id1")).thenReturn(otherCamera);
        when(cm.getCameraCharacteristics("iddd")).thenReturn(otherCamera);

        CameraCharacteristics rearFacingCamera = mock(CameraCharacteristics.class);
        when(rearFacingCamera.get(CameraCharacteristics.LENS_FACING)).thenReturn(CameraMetadata.LENS_FACING_BACK);

        when(cm.getCameraCharacteristics(rearFacingCameraActualId)).thenReturn(rearFacingCamera);

        assertEquals(rearFacingCameraActualId, Utility.findRearFacingCameraId(cm));
    }
}
