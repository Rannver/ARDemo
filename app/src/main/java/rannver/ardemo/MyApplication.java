package rannver.ardemo;

import android.app.Application;
import android.content.Context;

import rannver.ardemo.model.ModelGLView;

/**
 * Created by Mr.chen on 2018/1/24.
 */

public class MyApplication extends Application {


    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static Context getContext(){
        return context;
    }
}
