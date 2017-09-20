package quincy.distortion;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

/**
 * Created by quincy on 20/09/17.
 */



@RunWith(MockitoJUnitRunner.class)
public class DistortagleGLSurfaceViewTest {
    @Test
    public void computeVertexNewLocation_doNotPerturbVerticesWithCoordinatesPlusMinusOne() throws Exception {
        float[] xy = {1,0};
        float x0 = -0.5f;
        float y0 = -0.3f;
        float touchX = -0.3f;
        float touchY = 0.2f;
        float dx = 10;
        float dy = -3;
        float[] ret = Arrays.copyOf(xy, 2);

        DistortableGLSurfaceView.computeVertexNewLocation(x0, y0, touchX, touchY, dx, dy, ret);
        assert(Arrays.equals(xy, ret));

        xy = new float[]{-1,3333330};
        ret = Arrays.copyOf(xy, 2);
        DistortableGLSurfaceView.computeVertexNewLocation(x0, y0, touchX, touchY, dx, dy, ret);
        assert(Arrays.equals(xy, ret));

        xy = new float[]{-3,1};
        ret = Arrays.copyOf(xy, 2);
        DistortableGLSurfaceView.computeVertexNewLocation(x0, y0, touchX, touchY, dx, dy, ret);
        assert(Arrays.equals(xy, ret));

        xy = new float[]{3939,-1};
        ret = Arrays.copyOf(xy, 2);
        DistortableGLSurfaceView.computeVertexNewLocation(x0, y0, touchX, touchY, dx, dy, ret);
        assert(Arrays.equals(xy, ret));
    }
}
