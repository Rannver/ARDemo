package rannver.ardemo.util;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.util.ArrayList;

import cn.easyar.CameraCalibration;
import cn.easyar.CameraDevice;
import cn.easyar.CameraDeviceFocusMode;
import cn.easyar.CameraDeviceType;
import cn.easyar.CameraFrameStreamer;
import cn.easyar.Frame;
import cn.easyar.FunctorOfVoidFromPointerOfTargetAndBool;
import cn.easyar.ImageTarget;
import cn.easyar.ImageTracker;
import cn.easyar.Renderer;
import cn.easyar.StorageType;
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

public class ARUtil {

    private Context context;
    private PlyModel model;
    private CameraDevice camera;
    private CameraFrameStreamer streamer;
    private ArrayList<ImageTracker> trackers;
    private Renderer videobg_renderer;
    private BoxRenderer box_renderer;
    private boolean viewport_changed = false;
    private Vec2I view_size = new Vec2I(0, 0);
    private int rotation = 0;
    private Vec4I viewport = new Vec4I(0, 0, 1280, 720);
    private RenderDataBean renderDataBean;
    private String flag;

    public ARUtil(Context context,PlyModel model,String flag)
    {
        trackers = new ArrayList<ImageTracker>();
        this.context = context;
        this.model = model;
        this.flag = flag;
    }

    private void loadFromImage(ImageTracker tracker, String path)
    {
        ImageTarget target = new ImageTarget();
        String jstr = "{\n"
                + "  \"images\" :\n"
                + "  [\n"
                + "    {\n"
                + "      \"image\" : \"" + path + "\",\n"
                + "      \"name\" : \"" + "111" + "\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        target.setup(jstr, StorageType.Assets | StorageType.Json, "");
        Log.d("jstr", "loadFromImage: "+jstr);
        tracker.loadTarget(target, new FunctorOfVoidFromPointerOfTargetAndBool() {
            @Override
            public void invoke(Target target, boolean status) {
                Log.i("HelloAR", String.format("load target (%b): %s (%d)", status, target.name(), target.runtimeID()));
            }
        });
    }


    /**
     * 读取SD卡图片（敲定这个方法！）
     * 2018/1/26
     * @param tracker
     * @param path
     */
    private void loadFromImage2(ImageTracker tracker, String path)
    {
        ImageTarget target = new ImageTarget();
        String jstr = "{\n"
                + "  \"images\" :\n"
                + "  [\n"
                + "    {\n"
                + "      \"image\" : \"" + path + "\",\n"
                + "      \"name\" : \"" + "7428ec50873d135" + "\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        target.setup(jstr, StorageType.Absolute | StorageType.Json, "");
        Log.d("jstr", "loadFromImage2: "+jstr);
        tracker.loadTarget(target, new FunctorOfVoidFromPointerOfTargetAndBool() {
            @Override
            public void invoke(Target target, boolean status) {
                Log.i("HelloAR", String.format("load target (%b): %s (%d)", status, target.name(), target.runtimeID()));
            }
        });
    }


    public boolean init()
    {
        camera = new CameraDevice();
        streamer = new CameraFrameStreamer();
        streamer.attachCamera(camera);

        boolean status = true;
        status &= camera.open(CameraDeviceType.Default);
        camera.setSize(new Vec2I(1280, 720));

        if (!status) { return status; }
        ImageTracker tracker = new ImageTracker();
        tracker.attachStreamer(streamer);
        loadFromImage(tracker, "namecard.jpg");
        loadFromImage(tracker,"test2.jpg");
        loadFromImage(tracker,"test3.jpg");

        loadFromImage2(tracker,"/storage/emulated/0/Tencent/QQ_Images/7428ec50873d135.jpg");
        trackers.add(tracker);


        return status;
    }

    public void dispose()
    {
        for (ImageTracker tracker : trackers) {
            tracker.dispose();
        }
        trackers.clear();
        box_renderer = null;
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
        for (ImageTracker tracker : trackers) {
            status &= tracker.start();
        }
        return status;
    }

    public boolean stop()
    {
        boolean status = true;
        for (ImageTracker tracker : trackers) {
            status &= tracker.stop();
        }
        status &= (streamer != null) && streamer.stop();
        status &= (camera != null) && camera.stop();
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
        GLES20.glClearColor(0, 0, 0, 0);
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
                    ImageTarget imagetarget = target instanceof ImageTarget ? (ImageTarget) (target) : null;
                    if (imagetarget == null) {
                        continue;
                    }
//                    if (box_renderer != null) {
//                        box_renderer.render(camera.projectionGL(0.2f, 500.f), targetInstance.poseGL(), imagetarget.size());
//                    }
                    if (model != null) {
                        model.draw(viewMatrix, camera.projectionGL(0.2f, 500.f).data,imagetarget.size(), targetInstance.poseGL());
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
