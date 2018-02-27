package rannver.ardemo.util;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import cn.easyar.Matrix44F;
import cn.easyar.Vec2F;
import rannver.ardemo.R;
import rannver.ardemo.util.InitialUtil;

/**
 * Created by Mr.chen on 2018/1/24.
 */

public class PlyModel {

    private static final String TAG = "plyModel";

    private float centerMassX;
    private float centerMassY;
    private float centerMassZ;
    protected float floorOffset;

    protected int glProgram = -1;
    private float[] modelMatrix = new float[16];
    protected float[] mvMatrix = new float[16];
    protected float[] mvpMatrix = new float[16];

    private float maxX;
    private float maxY;
    private float maxZ;
    private float minX;
    private float minY;
    private float minZ;

    private static final int BYTES_PER_FLOAT = 4;
    private static final int COORDS_PER_VERTEX = 3;
    protected static final int VERTEX_STRIDE = COORDS_PER_VERTEX * BYTES_PER_FLOAT;
    private static final int INPUT_BUFFER_SIZE = 0x10000;

    private int vertexCount;
    private FloatBuffer colorBuffer;
    private FloatBuffer vertexBuffer;
    private Context context;

//    int mvpMatrixHandle;
//    int positionHandle;
//    int mColorHandle;
//    int transHandle;

    private int pos_coord_box;
    private int pos_color_box;
    private int pos_trans_box;
    private int pos_proj_box;


    public PlyModel(InputStream inputStream, Context context) throws IOException {
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;
        maxZ = Float.MIN_VALUE;
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        minZ = Float.MAX_VALUE;

        this.context = context;

        BufferedInputStream stream = new BufferedInputStream(inputStream, INPUT_BUFFER_SIZE);
        readText(stream);
        if (vertexCount <= 0 || vertexBuffer == null) {
            throw new IOException("Invalid model.");
        }

    }

    public void init(float boundSize) {
        if (GLES20.glIsProgram(glProgram)) {
            Log.d(TAG, "init");
            GLES20.glDeleteProgram(glProgram);
            glProgram = -1;
        }
        InitialUtil initialUtil = new InitialUtil(context);
        glProgram = initialUtil.compileProgram(R.raw.box_vert, R.raw.box_frag,
                null);
        initModelMatrix(boundSize);
//        mvpMatrixHandle = GLES20.glGetUniformLocation(glProgram, "u_MVP");
//        positionHandle = GLES20.glGetAttribLocation(glProgram, "a_Position");
//        mColorHandle = GLES20.glGetAttribLocation(glProgram, "a_Color");
//        transHandle = GLES20.glGetUniformLocation(glProgram, "trans");
//        Log.d(TAG, "draw: "+mvpMatrixHandle);
//        Log.d(TAG, "draw: "+positionHandle);
//        Log.d(TAG, "draw: "+mColorHandle);
//        Log.d(TAG, "draw: "+transHandle);

        pos_coord_box = GLES20.glGetAttribLocation(glProgram, "coord");
        pos_color_box = GLES20.glGetAttribLocation(glProgram, "color");
        pos_trans_box = GLES20.glGetUniformLocation(glProgram, "trans");
        pos_proj_box = GLES20.glGetUniformLocation(glProgram, "proj");

        Log.d("1111", "init: "+pos_coord_box);
        Log.d("1111", "init: "+pos_color_box);
        Log.d("1111", "init: "+pos_trans_box);
        Log.d("1111", "init: "+pos_proj_box);
    }

    public void initModelMatrix(float boundSize) {
        final float yRotation = 180f;
        initModelMatrix(boundSize, 0.0f, yRotation, 0.0f);
        float scale = getBoundScale(boundSize);
        if (scale == 0.0f) {
            scale = 1.0f;
        }
        floorOffset = (minY - centerMassY) / scale;
    }

    public void initModelMatrix(float boundSize, float rotateX, float rotateY, float rotateZ) {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, rotateX, 1.0f, 0.0f, 0.0f);
        Matrix.rotateM(modelMatrix, 0, rotateY, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(modelMatrix, 0, rotateZ, 0.0f, 0.0f, 1.0f);
        scaleModelMatrixToBounds(boundSize);
        Matrix.translateM(modelMatrix, 0, -centerMassX, -centerMassY, -centerMassZ);
    }

    private void scaleModelMatrixToBounds(float boundSize) {
        float scale = getBoundScale(boundSize);
        if (scale != 0f) {
            scale = 1f / scale;
            Matrix.scaleM(modelMatrix, 0, scale, scale, scale);
        }
    }

    public float getBoundScale(float boundSize) {
        float scaleX = (maxX - minX) / boundSize;
        float scaleY = (maxY - minY) / boundSize;
        float scaleZ = (maxZ - minZ) / boundSize;
        float scale = scaleX;
        if (scaleY > scale) {
            scale = scaleY;
        }
        if (scaleZ > scale) {
            scale = scaleZ;
        }
        return scale;
    }

    public void draw(float[] viewMatrix, float[] projectionMatrix, Vec2F size , Matrix44F cameraview) {

        if (vertexBuffer == null) {
            return;
        }
        GLES20.glUseProgram(glProgram);
        GLES20.glEnableVertexAttribArray(pos_coord_box);
        /**
         * param：
         index
         指定要修改的顶点属性的索引值
         size
         指定每个顶点属性的组件数量。必须为1、2、3或者4。初始值为4。（如position是由3个（x,y,z）组成，而颜色是4个（r,g,b,a））
         type
         指定数组中每个组件的数据类型。可用的符号常量有GL_BYTE, GL_UNSIGNED_BYTE, GL_SHORT,GL_UNSIGNED_SHORT, GL_FIXED, 和 GL_FLOAT，初始值为GL_FLOAT。
         normalized
         指定当被访问时，固定点数据值是否应该被归一化（GL_TRUE）或者直接转换为固定点值（GL_FALSE）。
         stride
         指定连续顶点属性之间的偏移量。如果为0，那么顶点属性会被理解为：它们是紧密排列在一起的。初始值为0。
         pointer
         指定第一个组件在数组的第一个顶点属性中的偏移量。该数组与GL_ARRAY_BUFFER绑定，储存于缓冲区中。初始值为0
         */
        GLES20.glVertexAttribPointer(pos_coord_box, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
                VERTEX_STRIDE, vertexBuffer);
        GLES20.glEnableVertexAttribArray(pos_color_box);
        GLES20.glVertexAttribPointer(pos_color_box, 3, GLES20.GL_FLOAT, false,
                VERTEX_STRIDE, colorBuffer);
        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, mvMatrix, 0);
        GLES20.glUniformMatrix4fv(pos_trans_box, 1, false, cameraview.data, 0);
        GLES20.glUniformMatrix4fv(pos_proj_box, 1, false, projectionMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(pos_coord_box);
    }

    private void readText(BufferedInputStream stream) throws IOException {
        List<Float> vertices = new ArrayList<>();
        List<Float> n_vertices = new ArrayList<>();
        List<Float> colors = new ArrayList<>();


        BufferedReader reader = new BufferedReader(new InputStreamReader(stream), INPUT_BUFFER_SIZE);
        String line;
        String[] lineArr;

        stream.mark(0x100000);
        boolean isBinary = false;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("format ")) {
                if (line.contains("binary")) {
                    isBinary = true;
                }
            } else if (line.startsWith("element vertex")) {
                lineArr = line.split(" ");
                vertexCount = Integer.parseInt(lineArr[2]);
            } else if (line.startsWith("end_header")) {
                break;
            }
        }

        if (vertexCount <= 0) {
            return;
        }

        if (isBinary) {
            Log.d("model analyse", "readText: error data");
        } else {
            readVerticesText(vertices, n_vertices, colors, reader);
        }

        float[] floatArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            floatArray[i] = vertices.get(i);
        }
        ByteBuffer vbb = ByteBuffer.allocateDirect(floatArray.length * BYTES_PER_FLOAT);
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(floatArray);
        vertexBuffer.position(0);

        float[] colorArray = new float[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            colorArray[i] = colors.get(i);
        }
        ByteBuffer vcc = ByteBuffer.allocateDirect(colorArray.length * BYTES_PER_FLOAT);
        vcc.order(ByteOrder.nativeOrder());
        colorBuffer = vcc.asFloatBuffer();
        colorBuffer.put(colorArray);
        colorBuffer.position(0);

    }

    private void readVerticesText(List<Float> vertices, List<Float> n_vertices, List<Float> colors, BufferedReader reader) throws IOException {
        String[] lineArr;
        float x, y, z, nx, ny, nz, r, g, b;

        double centerMassX = 0.0;
        double centerMassY = 0.0;
        double centerMassZ = 0.0;

        for (int i = 0; i < vertexCount; i++) {
            lineArr = reader.readLine().trim().split(" ");
            x = Float.parseFloat(lineArr[0]);
            y = Float.parseFloat(lineArr[1]);
            z = Float.parseFloat(lineArr[2]);
            nx = Float.parseFloat(lineArr[3]);
            ny = Float.parseFloat(lineArr[4]);
            nz = Float.parseFloat(lineArr[5]);
            r = Float.parseFloat(lineArr[6]);
            g = Float.parseFloat(lineArr[7]);
            b = Float.parseFloat(lineArr[8]);

            vertices.add(x);
            vertices.add(y);
            vertices.add(z);
            adjustMaxMin(x, y, z);
            centerMassX += x;
            centerMassY += y;
            centerMassZ += z;

            n_vertices.add(nx);
            n_vertices.add(ny);
            n_vertices.add(nz);

            colors.add(r / 255.0f);
            colors.add(g / 255.0f);
            colors.add(b / 255.0f);
        }
        this.centerMassX = (float) (centerMassX / vertexCount);
        this.centerMassY = (float) (centerMassY / vertexCount);
        this.centerMassZ = (float) (centerMassZ / vertexCount);
    }

    private void adjustMaxMin(float x, float y, float z) {
        if (x > maxX) {
            maxX = x;
        }
        if (y > maxY) {
            maxY = y;
        }
        if (z > maxZ) {
            maxZ = z;
        }
        if (x < minX) {
            minX = x;
        }
        if (y < minY) {
            minY = y;
        }
        if (z < minZ) {
            minZ = z;
        }
    }
}
