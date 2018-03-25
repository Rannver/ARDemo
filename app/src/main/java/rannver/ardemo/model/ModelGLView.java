package rannver.ardemo.model;

import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import cn.easyar.Engine;
import rannver.ardemo.util.InitialUtil;
import rannver.ardemo.util.PlyModel;

import static cn.easyar.engine.EasyAR.getApplicationContext;

/**
 * Created by Rannver on 2018/3/24.
 */

public class ModelGLView extends GLSurfaceView{

    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_ROTATE = 1;
    private static final int TOUCH_ZOOM = 2;

    private CameraUtil cameraUtil;
    private CameraRender render;

    private Context context;
    private float previousX;
    private float previousY;
    private PointF pinchStartPoint = new PointF();
    private float pinchStartDistance = 0.0f;
    private int touchMode = TOUCH_NONE;

    public ModelGLView(Context context,String plyName)
    {
        super(context);

        this.context = context;
        PlyModel2 model = loadSampleModel(plyName);

        setEGLContextFactory(new ContextFactory());
        setEGLConfigChooser(new ConfigChooser());

        cameraUtil = new CameraUtil(context,model);
        this.render = new CameraRender(context,model,cameraUtil);
        this.setRenderer(render);

        this.setZOrderMediaOverlay(true);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                previousX = event.getX();
                previousY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1) {
                    if (touchMode != TOUCH_ROTATE) {
                        previousX = event.getX();
                        previousY = event.getY();
                    }
                    touchMode = TOUCH_ROTATE;
                    float x = event.getX();
                    float y = event.getY();
                    float dx = x - previousX;
                    float dy = y - previousY;
                    previousX = x;
                    previousY = y;
                    render.rotate(InitialUtil.pxToDp(dy), InitialUtil.pxToDp(dx));
                } else if (event.getPointerCount() == 2) {
                    if (touchMode != TOUCH_ZOOM) {
                        pinchStartDistance = getPinchDistance(event);
                        getPinchCenterPoint(event, pinchStartPoint);
                        previousX = pinchStartPoint.x;
                        previousY = pinchStartPoint.y;
                        touchMode = TOUCH_ZOOM;
                    } else {
                        PointF pt = new PointF();
                        getPinchCenterPoint(event, pt);
                        float dx = pt.x - previousX;
                        float dy = pt.y - previousY;
                        previousX = pt.x;
                        previousY = pt.y;
                        float pinchScale = getPinchDistance(event) / pinchStartDistance;
                        pinchStartDistance = getPinchDistance(event);
                        render.translate(InitialUtil.pxToDp(dx), InitialUtil.pxToDp(dy), pinchScale);
                    }

                }
                requestRender();
                break;

            case MotionEvent.ACTION_UP:
                pinchStartPoint.x = 0.0f;
                pinchStartPoint.y = 0.0f;
                touchMode = TOUCH_NONE;
                break;
        }
        return true;
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        synchronized (cameraUtil){
            if(cameraUtil.init()){
                cameraUtil.start();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow()
    {
        synchronized (cameraUtil){
            cameraUtil.stop();
            cameraUtil.dispose();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Engine.onResume();
    }

    @Override
    public void onPause()
    {
        Engine.onPause();
        super.onPause();
    }


    private static class ContextFactory implements GLSurfaceView.EGLContextFactory
    {
        private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig)
        {
            EGLContext context;
            int[] attrib = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
            context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib );
            return context;
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context)
        {
            egl.eglDestroyContext(display, context);
        }
    }

    private static class ConfigChooser implements GLSurfaceView.EGLConfigChooser
    {
        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display)
        {
            final int EGL_OPENGL_ES2_BIT = 0x0004;
            final int[] attrib = { EGL10.EGL_RED_SIZE, 4, EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
                    EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE };

            int[] num_config = new int[1];
            egl.eglChooseConfig(display, attrib, null, 0, num_config);

            int numConfigs = num_config[0];
            if (numConfigs <= 0)
                throw new IllegalArgumentException("fail to choose EGL configs");

            EGLConfig[] configs = new EGLConfig[numConfigs];
            egl.eglChooseConfig(display, attrib, configs, numConfigs,
                    num_config);

            for (EGLConfig config : configs)
            {
                int[] val = new int[1];
                int r = 0, g = 0, b = 0, a = 0, d = 0;
                if (egl.eglGetConfigAttrib(display, config, EGL10.EGL_DEPTH_SIZE, val))
                    d = val[0];
                if (d < 16)
                    continue;

                if (egl.eglGetConfigAttrib(display, config, EGL10.EGL_RED_SIZE, val))
                    r = val[0];
                if (egl.eglGetConfigAttrib(display, config, EGL10.EGL_GREEN_SIZE, val))
                    g = val[0];
                if (egl.eglGetConfigAttrib(display, config, EGL10.EGL_BLUE_SIZE, val))
                    b = val[0];
                if (egl.eglGetConfigAttrib(display, config, EGL10.EGL_ALPHA_SIZE, val))
                    a = val[0];
                if (r == 8 && g == 8 && b == 8 && a == 0)
                    return config;
            }

            return configs[0];
        }
    }

    private float getPinchDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void getPinchCenterPoint(MotionEvent event, PointF pt) {
        pt.x = (event.getX(0) + event.getX(1)) * 0.5f;
        pt.y = (event.getY(0) + event.getY(1)) * 0.5f;
    }

    private PlyModel2 loadSampleModel(String plyName) {
        PlyModel2 plyModel;
        try {
//            InputStream stream = getApplicationContext().getAssets()
//                    .open(plyName);
            InputStream inputStream = new FileInputStream(new File(plyName));
            plyModel = new PlyModel2(inputStream,context);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            plyModel = null;
        }
        return plyModel;
    }

}
