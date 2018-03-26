package rannver.ardemo.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.easyar.Engine;
import rannver.ardemo.R;
import rannver.ardemo.model.ModelGLView;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.preview)
    FrameLayout preview;
    @BindView(R.id.btu_camera)
    ImageButton btuCamera;
    @BindView(R.id.btu_start)
    ImageButton btuStart;
    @BindView(R.id.btu_end)
    ImageButton btuEnd;

    private String key = "gjWuewAzkutlvqSyu23DCl5tzd5QeUSb29gQTBEHicQtDXhV0cq0hJidkQEyxHeWKpQmcDUHZG0Wx73gKiQuxZ7g7dEV7GOxEW2mtprAHx0Df3WxGAWUL0jeOYrgs68AjTm8hfpmXEiMcYufRYsIgEnmoj28OZzusqaJicjt1NAE30xF3Zp6H7CWgjnyyWiIMiUF9xyg";
    private GLView glView;
    private ModelGLView modelGLView;
    private String plyName = "test.ply";
    private String plyPath = "/storage/emulated/0/plymodel/test.ply";
    private String flag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!Engine.initialize(this, key)) {
            Log.e("HelloAR", "Initialization Failed.");
        }

        initView();

        requestCameraPermission(new PermissionCallback() {
            @Override
            public void onSuccess() {
                if (flag.equals("model2")) {
                    ((ViewGroup) findViewById(R.id.preview)).addView(modelGLView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                } else {
                    ((ViewGroup) findViewById(R.id.preview)).addView(glView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
            }

            @Override
            public void onFailure() {
            }
        });
    }

    private void initView() {

        Intent intent = getIntent();
        if (intent != null) {
            flag = intent.getStringExtra("flag");
        }

        if (flag != null && (!flag.equals("model2"))) {
            glView = new GLView(this, plyPath, flag);
        }
        if (flag.equals("model2")) {
            modelGLView = new ModelGLView(this, plyPath);
        }

        if (flag.equals("ar") || flag.equals("model2")) {
            btuStart.setVisibility(View.GONE);
        }
    }

    @OnClick({R.id.btu_camera, R.id.btu_start, R.id.btu_end})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btu_camera:
                //截图操作
                break;
            case R.id.btu_start:
                //开始加载模型
                btuStart.setVisibility(View.GONE);
                btuEnd.setVisibility(View.VISIBLE);
                glView.start();
                break;
            case R.id.btu_end:
                //模型加载结束
                btuEnd.setVisibility(View.GONE);
                btuStart.setVisibility(View.VISIBLE);
                glView.stop();
                break;
        }
    }

    private interface PermissionCallback {
        void onSuccess();

        void onFailure();
    }

    private HashMap<Integer, PermissionCallback> permissionCallbacks = new HashMap<Integer, PermissionCallback>();
    private int permissionRequestCodeSerial = 0;

    @TargetApi(23)
    private void requestCameraPermission(PermissionCallback callback) {

        String[] permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};
        List<String> permissionList = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 23) {
//            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                int requestCode = permissionRequestCodeSerial;
//                permissionRequestCodeSerial += 1;
//                permissionCallbacks.put(requestCode, callback);
//                requestPermissions(permissions, requestCode);
//            } else {
//                callback.onSuccess();
//            }
            for (int i = 0;i< permissions.length;i++){
                if (checkSelfPermission(permissions[i])!= PackageManager.PERMISSION_GRANTED){
                    permissionList.add(permissions[i]);
                }
            }
            if (!permissionList.isEmpty()){
                String[] needPermission = permissionList.toArray(new String[permissionList.size()]);
                permissionCallbacks.put(1,callback);
                requestPermissions(needPermission,1);
            }else {
                callback.onSuccess();
            }
        } else {
            callback.onSuccess();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissionCallbacks.containsKey(requestCode)) {
            PermissionCallback callback = permissionCallbacks.get(requestCode);
            permissionCallbacks.remove(requestCode);
            boolean executed = false;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    executed = true;
                    callback.onFailure();
                }
            }
            if (!executed) {
                callback.onSuccess();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glView != null) {
            glView.onResume();
        }
        if (modelGLView != null) {
            modelGLView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (glView != null) {
            glView.onPause();
        }
        if (modelGLView != null) {
            modelGLView.onPause();
        }
        super.onPause();
    }
}
