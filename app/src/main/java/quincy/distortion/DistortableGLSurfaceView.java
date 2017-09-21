package quincy.distortion;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.glGenBuffers;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static javax.microedition.khronos.opengles.GL10.GL_COLOR_BUFFER_BIT;
import static javax.microedition.khronos.opengles.GL10.GL_FLOAT;

/**
 * Created by quincy on 06/09/17.
 */

public class DistortableGLSurfaceView extends GLSurfaceView {
    private class MyRenderer implements GLSurfaceView.Renderer {

        private int program;
        private final int cameraTextureUnit = 0;
        private SurfaceTexture st;
        private boolean isOverlayEnabled = false;

        private float[] vertices;
        private short[] drawOrder;

        public void setOverlayEnabled(boolean b) {
            isOverlayEnabled = b;
        }


        public MyRenderer(float[] _vertices,
                          short[] _drawOrder) {
            vertices = _vertices;
            drawOrder = _drawOrder;
        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES30.glCreateShader(type);
            GLES30.glShaderSource(shader, shaderCode);
            GLES30.glCompileShader(shader);
            return shader;
        }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            String vsCode = Utility.rawResourceToString(DistortableGLSurfaceView.this.getResources(), R.raw.vs);
            int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vsCode);

            Log.d("MyRenderer vs", GLES30.glGetShaderInfoLog(vertexShader));
            int[] status = {0};
            GLES30.glGetShaderiv(vertexShader, GLES30.GL_COMPILE_STATUS, status, 0);
            Log.d("MyRenderer vs", "compile succeeded? " + status[0]);

            String fsCode = Utility.rawResourceToString(DistortableGLSurfaceView.this.getResources(), R.raw.fs);
            int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fsCode);

            GLES30.glGetShaderiv(fragmentShader, GLES30.GL_COMPILE_STATUS, status, 0);
            Log.d("MyRenderer fs", "compile succeeded? " + status[0]);
            Log.d("MyRenderer fs", GLES30.glGetShaderInfoLog(fragmentShader));

            program = GLES30.glCreateProgram();
            GLES30.glAttachShader(program, vertexShader);
            GLES30.glAttachShader(program, fragmentShader);

            GLES30.glLinkProgram(program);

            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0);
            Log.d("MyRenderer program", "link succeeded? " + status[0]);
            Log.d("MyRenderer program", GLES30.glGetProgramInfoLog(program));


            GLES30.glUseProgram(program);

            GLES30.glDeleteShader(vertexShader);
            GLES30.glDeleteShader(fragmentShader);

            /******* set up access to camera texture through opengl */
            GLES30.glActiveTexture(cameraTextureUnit);
            int[] texture = {0};
            GLES30.glGenTextures(1, texture, 0);
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);

            st = new SurfaceTexture(texture[0], false);

            CameraManager cm = getContext().getSystemService(CameraManager.class);
            try {
                String rearFacingCameraId = Utility.findRearFacingCameraId(cm);

                List<Surface> outputSurfaces = new ArrayList<>();


                post(() -> {
                    try {
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
                });
            }
            catch(CameraAccessException e){
                Log.e("CameraDisplay onCreate", "Failed to get a rear-facing camera ID.");
            }
            /******************************************/
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int i, int i1) {

        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            st.updateTexImage();

            GLES30.glClearColor(0f, 0f, 0, 1.0f);
            GLES30.glClear(GL_COLOR_BUFFER_BIT);

            float[] transformMatrix = new float[16];
            st.getTransformMatrix(transformMatrix);

            int transformMatrixHandle = GLES30.glGetAttribLocation(program, "textureTransformMatrix");
            GLES30.glUniformMatrix4fv(transformMatrixHandle, 1, false, transformMatrix, 0);

            // x,y, tx, ty
            float vertexCoords[] = vertices;
            final int entriesPerVertexCoord = 4;

            /************************* load vertices into vbo */
            ByteBuffer bb = ByteBuffer.allocateDirect(vertexCoords.length * Float.BYTES);
            bb.order(ByteOrder.nativeOrder());

            FloatBuffer fb = bb.asFloatBuffer();
            fb.put(vertexCoords);
            fb.position(0);


            int vbo[] = {0};
            GLES30.glGenBuffers(1, vbo, 0);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexCoords.length * Float.BYTES, fb, GL_STATIC_DRAW);
            /**************************************************************/

            /********************** draw a grid of squares */

            ByteBuffer drawlistb = ByteBuffer.allocateDirect(drawOrder.length * Short.BYTES);
            drawlistb.order(ByteOrder.nativeOrder());

            ShortBuffer drawlistBuffer = drawlistb.asShortBuffer();
            drawlistBuffer.put(drawOrder);
            drawlistBuffer.position(0);

            int ebo[] = {0};
            GLES30.glGenBuffers(1, ebo, 0);
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo[0]);
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, drawOrder.length * Short.BYTES, drawlistBuffer, GL_STATIC_DRAW);
            /**************************************************/

            /*********************************** set some attributes */
            int positionHandle = GLES30.glGetAttribLocation(program, "position");
            GLES30.glVertexAttribPointer(positionHandle, 2, GL_FLOAT, false, entriesPerVertexCoord * Float.BYTES, 0);
            GLES30.glEnableVertexAttribArray(positionHandle);

            int textureCoordsHandle = GLES30.glGetAttribLocation(program, "textureCoords");
            GLES30.glVertexAttribPointer(textureCoordsHandle, 2, GL_FLOAT, false, entriesPerVertexCoord * Float.BYTES, 2 * Float.BYTES);
            GLES30.glEnableVertexAttribArray(textureCoordsHandle);
            /***********************************************************/

            /************************* set some uniforms */
            int samplerHandle = GLES30.glGetUniformLocation(program, "cameraTex");
            GLES30.glUniform1i(samplerHandle, cameraTextureUnit);

            int opacityHandle = GLES30.glGetUniformLocation(program, "opacity");
            if (isOverlayEnabled) {
                GLES30.glUniform1f(opacityHandle, 0.3f);
            }
            else {
                GLES30.glUniform1f(opacityHandle, 1f);
            }
            /******************************************/

            GLES30.glDrawElements(GLES30.GL_TRIANGLES, drawOrder.length, GLES30.GL_UNSIGNED_SHORT, 0);

            GLES30.glDisableVertexAttribArray(positionHandle);
            GLES30.glDisableVertexAttribArray(textureCoordsHandle);

        }
    }

    private MyRenderer renderer;
    private boolean showGrid = false;

    private final long gridUpdatePeriodMillis = 40;
    private final short noVerticesPerRow = 18;
    private final short noVerticesPerCol = 22;
    // x,y,tx,ty
    private final int noEntriesPerVertex = 4;

    public DistortableGLSurfaceView(Context context) {
        super(context);

        glVertices = new float[noEntriesPerVertex * noVerticesPerCol * noVerticesPerRow];
        final float inColumnStepSize = 2f / (noVerticesPerCol - 1);
        final float inRowStepSize = 2f / (noVerticesPerRow - 1);

        for (int i = 0; i < noVerticesPerCol; ++i) {
            for (int j = 0; j < noVerticesPerRow; ++j) {
                // x
                glVertices[(i * noVerticesPerRow + j) * noEntriesPerVertex + 0] = (-1) + j * inRowStepSize;
                // y
                glVertices[(i * noVerticesPerRow + j) * noEntriesPerVertex + 1] = (-1) + i * inColumnStepSize;
                // tx
                glVertices[(i * noVerticesPerRow + j) * noEntriesPerVertex + 2] = 1-i * inColumnStepSize / 2;
                // ty
                glVertices[(i * noVerticesPerRow + j) * noEntriesPerVertex + 3] = 1-j * inRowStepSize /2;
            }
        }

//        Log.d("vertices", Arrays.toString(glVertices));
//        Log.d("draw order", Arrays.toString(Utility.generateDrawOrder(noVerticesPerRow, noVerticesPerCol)));


        Runnable r = new Runnable() {
            @Override
            public void run() {
                // Do not move the points at all if there is no motion
                if (dragX != touchDownX || dragY != touchDownY) {
                    final float dx = dragX - touchDownX;
                    final float dy = dragY - touchDownY;

                    for (int i = 0; i < glVertices.length/noEntriesPerVertex; ++i) {
                        // Do not move at all if this is an edge vertex
                        if (Math.abs(glVertices[noEntriesPerVertex * i]) == 1
                                || Math.abs(glVertices[noEntriesPerVertex * i + 1]) == 1) {
                            continue;
                        }

                    /* Compute the new location of the vertex */
                        // Tuning parameters
                        final float distanceDecay = 1;

                        final float x0 =  glVertices[noEntriesPerVertex*i];
                        final float y0 =  glVertices[noEntriesPerVertex*i+1];

                        final float xDistanceToTouch = (touchDownX/getWidth() * 2 -1) - x0;
                        final float yDistanceToTouch = (1 - touchDownY/getHeight() * 2) - y0;
                        float distanceFromSourceTerm =
                                1f/ (1 + distanceDecay * (xDistanceToTouch * xDistanceToTouch + yDistanceToTouch * yDistanceToTouch));

                        float x1 = (float)
                                (x0 + dx / getWidth()
                                        * (1 - Math.abs(x0))
                                        * distanceFromSourceTerm
                        );
                        // have to negate dy since Y is in the opposite direction for GL
                        // coordinates versus screen coordinates
                        float y1 = (float)
                                (y0 - dy / getHeight()
                                        * (1 - Math.abs(y0))
                                        * distanceFromSourceTerm
                                );

                        /*******************************************************************/


                        // Translate back to GL coordinates
                        glVertices[noEntriesPerVertex*i] = x1;
                        glVertices[noEntriesPerVertex*i+1] = y1;
                    }

                    // Having processed the motion event, reset the touchdown point
                    // (we don't want momentum)
                    touchDownX = dragX;
                    touchDownY = dragY;

                    invalidate();
                }
                postDelayed(this, gridUpdatePeriodMillis);
            }
        };
        r.run();


        setEGLContextClientVersion(3);

        renderer = new MyRenderer(glVertices, Utility.generateDrawOrder(noVerticesPerRow, noVerticesPerCol));
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // possible race condition?
        setWillNotDraw(false);
    }

    // Update these in onTouchEvent, then asynchronously update the point locations
    // in a periodic function
    private float touchDownX = 0;
    private float touchDownY = 0;
    private float dragX = 0;
    private float dragY = 0;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case ACTION_DOWN:
                renderer.setOverlayEnabled(true);
                showGrid = true;

                touchDownX = dragX = e.getRawX();
                touchDownY = dragY = e.getRawY();

                invalidate();
                break;
            case ACTION_UP:
                renderer.setOverlayEnabled(false);
                showGrid = false;

                invalidate();
                break;
            case ACTION_MOVE:
                dragX = e.getRawX();
                dragY = e.getRawY();
                break;
        }
        return true;
    }

    private float[] glVertices;


    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (showGrid) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor((100 << 24) | (255 << 16) | (255 << 8) | 255);

            //configure me
            final int radius = 10;


            for (int i = 0; i < glVertices.length / noEntriesPerVertex; ++i) {
                // Move from GL coordinates to screen coordinates
                float circleCentreX = getWidth() / 2 * (1 + glVertices[noEntriesPerVertex * i]);
                // have to negate since GL y-direction and screen y-direciton
                // are opposite
                float circleCentreY = getHeight() / 2 * (1 - glVertices[noEntriesPerVertex * i + 1]);

                canvas.drawOval(circleCentreX - radius, circleCentreY - radius,
                        circleCentreX + radius, circleCentreY + radius,
                        paint);
            }
        }
    }
}
