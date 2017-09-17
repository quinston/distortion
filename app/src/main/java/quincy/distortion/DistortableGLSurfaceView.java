package quincy.distortion;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.glGenBuffers;
import static javax.microedition.khronos.opengles.GL10.GL_COLOR_BUFFER_BIT;
import static javax.microedition.khronos.opengles.GL10.GL_FALSE;
import static javax.microedition.khronos.opengles.GL10.GL_FLOAT;

/**
 * Created by quincy on 06/09/17.
 */

public class DistortableGLSurfaceView extends GLSurfaceView {
    private class MyRenderer implements GLSurfaceView.Renderer {

        private int program;
        private final Resources res;
        private final int cameraTextureUnit = 0;


        public MyRenderer(Resources _res) {
            res = _res;
        }

        private int loadShader(int type, String shaderCode) {
            int shader = GLES30.glCreateShader(type);
            GLES30.glShaderSource(shader, shaderCode);
            GLES30.glCompileShader(shader);
            return shader;
        }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            String vsCode = Utility.rawResourceToString(res, R.raw.vs);
            int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vsCode);

            Log.d("MyRenderer vs", GLES30.glGetShaderInfoLog(vertexShader));
            int[] status = {0};
            GLES30.glGetShaderiv(vertexShader, GLES30.GL_COMPILE_STATUS, status, 0);
            Log.d("MyRenderer vs", "compile succeeded? " + status[0]);

            String fsCode = Utility.rawResourceToString(res, R.raw.fs);
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


            GLES30.glActiveTexture(cameraTextureUnit);
            int[] texture = {0};
            GLES30.glGenTextures(1, texture, 0);
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);

            CameraDisplay.st.detachFromGLContext();
            CameraDisplay.st.attachToGLContext(texture[0]);
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int i, int i1) {

        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            if (CameraDisplay.st != null) {
                GLES30.glClearColor(0.5f, 0.5f, 0, 1.0f);
                GLES30.glClear(GL_COLOR_BUFFER_BIT);

                float[] transformMatrix = new float[16];
                CameraDisplay.st.getTransformMatrix(transformMatrix);
                int transformMatrixHandle = GLES30.glGetAttribLocation(program, "transformMatrix");
                GLES30.glUniformMatrix4fv(transformMatrixHandle, 1, false, transformMatrix, 0);

                // x,y,r,g,b, tx, ty
                float vertexCoords[] = {
                        0f, 0f, 1.0f, 0f, 0f,    1, 1,
                        0.5f, 0.5f, 0f, 1f, 0f,  1, 0,
                        0f, 0.5f, 0f, 0f, 1f,    0, 1,
                        -0.5f, 0f, 0f, 1f, 1f,   0, 0,
                };
                final int entriesPerVertexCoord = 7;



                // load vertices into vbo
                ByteBuffer bb = ByteBuffer.allocateDirect(vertexCoords.length * Float.BYTES);
                bb.order(ByteOrder.nativeOrder());

                FloatBuffer fb = bb.asFloatBuffer();
                fb.put(vertexCoords);
                fb.position(0);


                int vbo[] = {0};
                GLES30.glGenBuffers(1, vbo, 0);
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexCoords.length * Float.BYTES, fb, GL_STATIC_DRAW);


                // draw a parallelogram of sorts
                short drawOrder[] = {
                        0,1,2,
                        2,0,3,
                };

                ByteBuffer drawlistb = ByteBuffer.allocateDirect(drawOrder.length * Short.BYTES);
                drawlistb.order(ByteOrder.nativeOrder());

                ShortBuffer drawlistBuffer = drawlistb.asShortBuffer();
                drawlistBuffer.put(drawOrder);
                drawlistBuffer.position(0);

                int ebo[] = {0};
                GLES30.glGenBuffers(1, ebo, 0);
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo[0]);
                GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, drawOrder.length * Short.BYTES, drawlistBuffer, GL_STATIC_DRAW);


                int positionHandle = GLES30.glGetAttribLocation(program, "position");
                GLES30.glVertexAttribPointer(positionHandle, 2, GL_FLOAT, false, entriesPerVertexCoord * Float.BYTES, 0);
                GLES30.glEnableVertexAttribArray(positionHandle);

                int colourHandle = GLES30.glGetAttribLocation(program, "colour");
                GLES30.glVertexAttribPointer(colourHandle, 3, GL_FLOAT, false, entriesPerVertexCoord * Float.BYTES, 2 * Float.BYTES);
                GLES30.glEnableVertexAttribArray(colourHandle);


                float rotationArgument = ((float)(System.currentTimeMillis() % 1000)) / 1000 * 360;
                Log.d("degree", "" + rotationArgument);
                float[] rotationMatrix = new float[] {
                        1,0,0,0,
                        0,1,0,0,
                        0,0,1,0,
                        0,0,0,1
                };
                Matrix.rotateM(rotationMatrix, 0, rotationArgument, -1, 0.5f, 0.3f);

                int transformHandle = GLES30.glGetUniformLocation(program, "trans");
                GLES30.glUniformMatrix4fv(transformHandle, 1, false, rotationMatrix, 0);

                // Put camera image onto the parallelogram
                int textureCoordsHandle = GLES30.glGetAttribLocation(program, "textureCoords");
                GLES30.glVertexAttribPointer(textureCoordsHandle, 2, GL_FLOAT, false, entriesPerVertexCoord * Float.BYTES, 5 * Float.BYTES);
                GLES30.glEnableVertexAttribArray(textureCoordsHandle);

                int samplerHandle = GLES30.glGetUniformLocation(program, "cameraTex");
                GLES30.glUniform1i(samplerHandle, cameraTextureUnit); //for GL_TEXTURE0

//                int[] texture = {0};
//                GLES30.glGenTextures(1, texture, 0);
//                GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
//                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture[0]);


//                CameraDisplay.textureView.getSurfaceTexture().detachFromGLContext();
//                CameraDisplay.textureView.getSurfaceTexture().attachToGLContext(0);
//                Log.d("shit", "it works");
//                GLES30.glActiveTexture(GLES30.GL_TEXTURE0);



                GLES30.glDrawElements(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_SHORT, 0);

                GLES30.glDisableVertexAttribArray(positionHandle);
                GLES30.glDisableVertexAttribArray(colourHandle);
                GLES30.glDisableVertexAttribArray(textureCoordsHandle);


            }

        }
    }

    private MyRenderer renderer;

    public DistortableGLSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(3);

        renderer = new MyRenderer(getResources());
        setRenderer(renderer);
    }
}
