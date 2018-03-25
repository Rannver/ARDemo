package rannver.ardemo.util;

import android.content.Context;
import android.opengl.GLES20;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.util.Log;


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import rannver.ardemo.MyApplication;

/**
 *
 * @author Mr.chen
 * @date 2018/1/24
 */

public class InitialUtil {

    private static Context context;

    public  InitialUtil (Context context){
        this.context = context;
    }

    public int compileProgram(@RawRes int vertexShader, @RawRes int fragmentShader,
                                     @NonNull String[] attributes) {
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, loadShader(GLES20.GL_VERTEX_SHADER,
                readTextFileFromRawRes(vertexShader)));
        GLES20.glAttachShader(program, loadShader(GLES20.GL_FRAGMENT_SHADER,
                readTextFileFromRawRes(fragmentShader)));
//        for (int i = 0; i < attributes.length; i++) {
//            GLES20.glBindAttribLocation(program, i, attributes[i]);
//        }
        GLES20.glLinkProgram(program);
        return program;
    }
    private int loadShader(int type, @NonNull String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        // If the compilation fails, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e("loadShader", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed.");
        }
        return shader;
    }
    private String readTextFileFromRawRes(@RawRes int resourceId) {
        InputStream inputStream = context.getResources()
                .openRawResource(resourceId);
        try {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            return new String(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeSilently(inputStream);
        }
        throw new RuntimeException("Failed to read raw resource id " + resourceId);
    }

    public void closeSilently(@Nullable Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static float pxToDp(float px) {
        return px / getDensityScalar();
    }

    private static float getDensityScalar() {
        return context.getResources().getDisplayMetrics().density;
    }

}
