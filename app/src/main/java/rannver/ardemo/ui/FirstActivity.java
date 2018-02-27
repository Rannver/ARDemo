package rannver.ardemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rannver.ardemo.R;

/**
 * Created by Rannver on 2018/2/10.
 */

public class FirstActivity extends AppCompatActivity {

    @BindView(R.id.btu_ar)
    Button btuAr;
    @BindView(R.id.btu_model)
    Button btuModel;
    @BindView(R.id.btu_model2)
    Button btuModel2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_activity);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.btu_ar, R.id.btu_model,R.id.btu_model2})
    public void onViewClicked(View view) {
        Intent intent;
        intent = new Intent(FirstActivity.this, MainActivity.class);
        switch (view.getId()) {
            case R.id.btu_ar:
                intent.putExtra("flag", "ar");
                break;
            case R.id.btu_model:
                intent.putExtra("flag", "model");
                break;
            case R.id.btu_model2:
                intent.putExtra("flag", "model2");
                break;
        }
        startActivity(intent);
    }
}
