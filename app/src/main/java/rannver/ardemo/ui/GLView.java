package rannver.ardemo.ui;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

import android.content.Context;
import android.opengl.GLSurfaceView;

import java.io.IOException;
import java.io.InputStream;

import cn.easyar.Engine;
import rannver.ardemo.model2.ModelRender2;
import rannver.ardemo.model2.ModelUtil2;
import rannver.ardemo.model2.PlyModel2;
import rannver.ardemo.util.ARRender;
import rannver.ardemo.util.ARUtil;
import rannver.ardemo.util.ModelRender;

import rannver.ardemo.util.ModelUtil;
import rannver.ardemo.util.PlyModel;

import static cn.easyar.engine.EasyAR.getApplicationContext;

public class GLView extends GLSurfaceView
{

    private ARUtil arUtil;
    private ModelUtil modelUtil;
    private ModelUtil2 modelUtil2;

    private Context context;
    private String flag;

    private static final String FLAG_GLVIEW_AR = "ar";
    private static final String FLAG_GLVIEW_MODEL = "model";
    private static final String FLAG_GLVIEW_MODEL2 = "model2";

    public GLView(Context context,String plyName,String flag)
    {
        super(context);

        this.context = context;
        this.flag = flag;
        PlyModel model = loadSampleModel(plyName);
        PlyModel2 model2 = loadSampleModel2(plyName);

        setEGLContextFactory(new ContextFactory());
        setEGLConfigChooser(new ConfigChooser());

        switch (flag){
            case FLAG_GLVIEW_AR:
                arUtil = new ARUtil(context,model);
                this.setRenderer(new ARRender(context,model,arUtil));
                break;
            case FLAG_GLVIEW_MODEL:
                //只加载模型(HelloARSLAM方式)
                modelUtil = new ModelUtil(context,model);
                this.setRenderer(new ModelRender(context,model,modelUtil));
                break;
            case FLAG_GLVIEW_MODEL2:
                //只加载模型（opengles加载附加监听事件）
//                modelUtil2 = new ModelUtil2(context,model2);
//                this.setRenderer(new ModelRender2(context,model2,modelUtil2));
                modelUtil = new ModelUtil(context,model);
                this.setRenderer(new ModelRender(context,model,modelUtil));
                break;
        }

        this.setZOrderMediaOverlay(true);

    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        switch (flag){
            case FLAG_GLVIEW_AR:
                synchronized (arUtil){
                    if(arUtil.init()){
                        arUtil.start();
                    }
                }
                break;
            case FLAG_GLVIEW_MODEL:
                synchronized (modelUtil){
                    if(modelUtil.init()){
                        modelUtil.start();
                    }
                }
                break;
        }

    }

    @Override
    protected void onDetachedFromWindow()
    {
        switch (flag){
            case FLAG_GLVIEW_AR:
                synchronized (arUtil){
                    arUtil.stop();
                    arUtil.dispose();
                }
                break;
            case FLAG_GLVIEW_MODEL:
                synchronized (modelUtil){
                    modelUtil.stop();
                    modelUtil.dispose();
                }
                break;
        }
        super.onDetachedFromWindow();
    }

    public void start() {
        synchronized (modelUtil) {
            modelUtil.startTracker();
        }
    }

    public void stop() {
        synchronized (modelUtil) {
            modelUtil.stopTracker();
        }
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

    private PlyModel loadSampleModel(String plyName) {
        PlyModel plyModel;
        try {
            InputStream stream = getApplicationContext().getAssets()
                    .open(plyName);
            plyModel = new PlyModel(stream,context);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
            plyModel = null;
        }
        return plyModel;
    }
    private PlyModel2 loadSampleModel2(String plyName) {
        PlyModel2 plyModel;
        try {
            InputStream stream = getApplicationContext().getAssets()
                    .open(plyName);
            plyModel = new PlyModel2(stream,context);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
            plyModel = null;
        }
        return plyModel;
    }

}
