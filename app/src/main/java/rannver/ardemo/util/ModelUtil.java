package rannver.ardemo.util;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import cn.easyar.ARScene;
import cn.easyar.ARSceneTracker;
import cn.easyar.CameraCalibration;
import cn.easyar.CameraDevice;
import cn.easyar.CameraDeviceFocusMode;
import cn.easyar.CameraDeviceType;
import cn.easyar.CameraFrameStreamer;
import cn.easyar.Frame;
import cn.easyar.Renderer;
import cn.easyar.Target;
import cn.easyar.TargetInstance;
import cn.easyar.TargetStatus;
import cn.easyar.Vec2I;
import cn.easyar.Vec4I;
import rannver.ardemo.bean.RenderDataBean;

import static rannver.ardemo.util.ARRender.MODEL_BOUND_SIZE;
import static rannver.ardemo.util.ARRender.Z_FAR;
import static rannver.ardemo.util.ARRender.Z_NEAR;

/**
 * Created by Rannver on 2018/1/25.
 */

public class ModelUtil {

    private Context context;
    private PlyModel model;

    private CameraDevice camera;
    private CameraFrameStreamer streamer;
    private ARSceneTracker tracker;
    private Renderer videobg_renderer;
    private BoxRenderer box_renderer;
    private boolean viewport_changed = false;
    private Vec2I view_size = new Vec2I(0, 0);
    private int rotation = 0;
    private Vec4I viewport = new Vec4I(0, 0, 1280, 720);

    private RenderDataBean renderDataBean;

    public ModelUtil(Context context, PlyModel model)
    {
        this.context = context;
        this.model = model;
    }


    public boolean init()
    {
        camera = new CameraDevice();
        streamer = new CameraFrameStreamer();
        streamer.attachCamera(camera);

        boolean status = true;
        status &= camera.open(CameraDeviceType.Default);
        camera.setSize(new Vec2I(1280, 720));

        return status;
    }

    public void dispose()
    {
        if (tracker != null) {
            tracker.dispose();
            tracker = null;
        }
//        box_renderer = null;
        if (videobg_renderer != null) {
            videobg_renderer.dispose();
            videobg_renderer = null;
        }
        if (streamer != null) {
            streamer.dispose();
            streamer = null;
        }
        if (camera != null) {
            camera.dispose();
            camera = null;
        }
    }

    public boolean start()
    {
        boolean status = true;
        status &= (camera != null) && camera.start();
        status &= (streamer != null) && streamer.start();
        camera.setFocusMode(CameraDeviceFocusMode.Continousauto);
        return status;
    }

    public boolean stop()
    {
        boolean status = true;
        if (tracker != null) {
            status &= tracker.stop();
        }
        status &= (streamer != null) && streamer.stop();
        status &= (camera != null) && camera.stop();
        return status;
    }

    public boolean startTracker()
    {
        boolean status = true;
        if (tracker != null) {
            tracker.stop();
            tracker.dispose();
        }
        tracker = new ARSceneTracker();
        tracker.attachStreamer(streamer);
        if (tracker != null) {
            status &= tracker.start();
        }
        return status;
    }

    public boolean stopTracker()
    {
        boolean status = true;
        if (tracker != null) {
            status &= tracker.stop();
            tracker.dispose();
            tracker = null;
        }
        return status;
    }

    public void initGL()
    {
        if (videobg_renderer != null) {
            videobg_renderer.dispose();
        }
        videobg_renderer = new Renderer();
//        box_renderer = new BoxRenderer();
//        box_renderer.init();
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        if (model != null) {
            model.init(MODEL_BOUND_SIZE);
        }
    }

    public RenderDataBean resizeGL(int width, int height, float[] projectionMatrix, float rotateAngleX, float rotateAngleY, float translateX, float translateY, float translateZ, float[] viewMatrix)
    {
        view_size = new Vec2I(width, height);
        viewport_changed = true;

        RenderDataBean dataBean = new RenderDataBean();
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, Z_NEAR, Z_FAR);
        // initialize the view matrix
        rotateAngleX = 0;
        rotateAngleY = 0;
        translateX = 0f;
        translateY = 0f;
        translateZ = -MODEL_BOUND_SIZE * 1.5f;
        updateViewMatrix(viewMatrix,translateX,translateY,translateZ,rotateAngleX,rotateAngleY);

        // By default, rotate the model towards the user a bit
        rotateAngleX = -15.0f;
        rotateAngleY = 15.0f;
        updateViewMatrix(viewMatrix,translateX,translateY,translateZ,rotateAngleX,rotateAngleY);

        dataBean.setProjectionMatrix(projectionMatrix);
        dataBean.setRotateAngleX(rotateAngleX);
        dataBean.setTranslateY(rotateAngleY);
        dataBean.setTranslateX(translateX);
        dataBean.setTranslateY(translateY);
        dataBean.setTranslateZ(translateZ);
        dataBean.setViewMatrix(viewMatrix);

        renderDataBean = dataBean;

        return dataBean;
    }



    private void updateViewport()
    {
        CameraCalibration calib = camera != null ? camera.cameraCalibration() : null;
        int rotation = calib != null ? calib.rotation() : 0;
        if (rotation != this.rotation) {
            this.rotation = rotation;
            viewport_changed = true;
        }
        if (viewport_changed) {
            Vec2I size = new Vec2I(1, 1);
            if ((camera != null) && camera.isOpened()) {
                size = camera.size();
            }
            if (rotation == 90 || rotation == 270) {
                size = new Vec2I(size.data[1], size.data[0]);
            }
            float scaleRatio = Math.max((float) view_size.data[0] / (float) size.data[0], (float) view_size.data[1] / (float) size.data[1]);
            Vec2I viewport_size = new Vec2I(Math.round(size.data[0] * scaleRatio), Math.round(size.data[1] * scaleRatio));
            viewport = new Vec4I((view_size.data[0] - viewport_size.data[0]) / 2, (view_size.data[1] - viewport_size.data[1]) / 2, viewport_size.data[0], viewport_size.data[1]);

            if ((camera != null) && camera.isOpened())
                viewport_changed = false;
        }
    }

    public void render(float[] viewMatrix, float[] projectionMatrix)
    {
        GLES20.glClearColor(1.f, 1.f, 1.f, 1.f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (videobg_renderer != null) {
            Vec4I default_viewport = new Vec4I(0, 0, view_size.data[0], view_size.data[1]);
            GLES20.glViewport(default_viewport.data[0], default_viewport.data[1], default_viewport.data[2], default_viewport.data[3]);
            if (videobg_renderer.renderErrorMessage(default_viewport)) {
                return;
            }
        }

        if (streamer == null) { return; }
        Frame frame = streamer.peek();
        try {
            updateViewport();
            GLES20.glViewport(viewport.data[0], viewport.data[1], viewport.data[2], viewport.data[3]);

            if (videobg_renderer != null) {
                videobg_renderer.render(frame, viewport);
            }

            for (TargetInstance targetInstance : frame.targetInstances()) {
                int status = targetInstance.status();
                if (status == TargetStatus.Tracked) {
                    Target target = targetInstance.target();
                    ARScene scene = target instanceof ARScene ? (ARScene)(target) : null;
                    if (scene == null) {
                        continue;
                    }
//                    if (box_renderer != null) {
//                        box_renderer.render(camera.projectionGL(0.2f, 500.f), targetInstance.poseGL(), new Vec2F(1, 1));
//                    }
                    if (model != null) {
                        model.draw(viewMatrix, camera.projectionGL(0.2f, 500.f).data,null, targetInstance.poseGL());
                    }
                }
            }
        }
        finally {
            frame.dispose();
        }
    }

    private void updateViewMatrix(float[] viewMatrix, float translateX, float translateY, float translateZ, float rotateAngleX, float rotateAngleY) {
        //模型大小
        Matrix.setLookAtM(viewMatrix, 0, 0, 0,translateZ, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.translateM(viewMatrix, 0, -translateX, -translateY, 0f);
        /**
         * 旋转模型
         * 加减90为旋转方向
         * 转view不转模型或者只转模型
         */
        Matrix.rotateM(viewMatrix, 0, rotateAngleX, 1f, 0f, 0f);
        Matrix.rotateM(viewMatrix, 0, rotateAngleY, 0f, 1f, 0f);
    }

}
