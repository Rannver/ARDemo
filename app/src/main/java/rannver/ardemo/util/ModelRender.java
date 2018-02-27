package rannver.ardemo.util;

import android.content.Context;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import rannver.ardemo.bean.RenderDataBean;

/**
 * Created by Mr.chen on 2018/1/24.
 */

public class ModelRender implements GLSurfaceView.Renderer {
    public static final float MODEL_BOUND_SIZE = 50f;
    public static final float Z_NEAR = 2f;
    public static final float Z_FAR = MODEL_BOUND_SIZE * 10;

    private PlyModel model;


    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];

    private float rotateAngleX;
    private float rotateAngleY;
    private float translateX;
    private float translateY;
    private float translateZ;

    private Context context;
    private ModelUtil modelUtil;

    public ModelRender(Context context, PlyModel model, ModelUtil modelUtil){
        this.model=model;
        this.context = context;
        this.modelUtil = modelUtil;
    }
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

        synchronized (modelUtil){
            modelUtil.initGL();
        }

    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        synchronized (modelUtil){
            RenderDataBean dataBean = modelUtil.resizeGL(width,height,projectionMatrix,rotateAngleX,rotateAngleY,translateX,translateY,translateZ,viewMatrix);
            setData(dataBean);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        synchronized (modelUtil){
            modelUtil.render(viewMatrix,projectionMatrix);
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
}
