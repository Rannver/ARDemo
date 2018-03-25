package rannver.ardemo.model;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import rannver.ardemo.bean.RenderDataBean;
import rannver.ardemo.util.ARUtil;
import rannver.ardemo.util.PlyModel;

/**
 * Created by Mr.chen on 2018/1/24.
 */

public class CameraRender implements GLSurfaceView.Renderer {
    public static final float MODEL_BOUND_SIZE = 50f;
    public static final float Z_NEAR = 2f;
    public static final float Z_FAR = MODEL_BOUND_SIZE * 10;

    private PlyModel2 model;


    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];

    private float rotateAngleX;
    private float rotateAngleY;
    private float translateX;
    private float translateY;
    private float translateZ;

    private Context context;
    private CameraUtil cameraUtil;

    public CameraRender(Context context, PlyModel2 model, CameraUtil cameraUtil){
        this.model=model;
        this.context = context;
        this.cameraUtil = cameraUtil;
    }
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

        synchronized (cameraUtil){
            cameraUtil.initGL();
        }

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        synchronized (cameraUtil){
            RenderDataBean dataBean = cameraUtil.resizeGL(width,height,projectionMatrix,rotateAngleX,rotateAngleY,translateX,translateY,translateZ,viewMatrix);
            setData(dataBean);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        synchronized (cameraUtil){
            cameraUtil.render(viewMatrix,projectionMatrix);
        }
    }

    private void setData(RenderDataBean dataBean) {
        projectionMatrix =dataBean.getProjectionMatrix();
        rotateAngleX = dataBean.getRotateAngleX();
        rotateAngleY = dataBean.getRotateAngleY();
        translateX = dataBean.getTranslateX();
        translateY = dataBean.getTranslateY();
        translateZ = dataBean.getTranslateZ();
        viewMatrix = dataBean.getViewMatrix();
    }

    public void rotate(float aX, float aY) {
        final float rotateScaleFactor = 0.5f;
        rotateAngleX -= aX * rotateScaleFactor;
        rotateAngleY += aY * rotateScaleFactor;
        updateViewMatrix();
    }

    public void translate(float dx, float dy, float dz) {
        final float translateScaleFactor = MODEL_BOUND_SIZE / 200f;
        translateX += dx * translateScaleFactor;
        translateY += dy * translateScaleFactor;
        if (dz != 0f) {
            translateZ /= dz;
        }
        updateViewMatrix();
    }

    private void updateViewMatrix() {
        Log.d("111", "updateViewMatrix: "+rotateAngleX+","+rotateAngleY+","+translateZ);
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, translateZ, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.translateM(viewMatrix, 0, -translateX, -translateY, 0f);
        Matrix.rotateM(viewMatrix, 0, rotateAngleX, 1f, 0f, 0f);
        Matrix.rotateM(viewMatrix, 0, rotateAngleY, 0f, 1f, 0f);
    }


}
