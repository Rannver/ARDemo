package rannver.ardemo.bean;

/**
 * Created by Rannver on 2018/1/29.
 */

public class RenderDataBean {
    private float rotateAngleX;
    private float rotateAngleY;
    private float translateX;
    private float translateY;
    private float translateZ;
    private  float[] projectionMatrix = new float[16];
    private  float[] viewMatrix = new float[16];

    public float getRotateAngleX() {
        return rotateAngleX;
    }

    public void setRotateAngleX(float rotateAngleX) {
        this.rotateAngleX = rotateAngleX;
    }

    public float getRotateAngleY() {
        return rotateAngleY;
    }

    public void setRotateAngleY(float rotateAngleY) {
        this.rotateAngleY = rotateAngleY;
    }

    public float getTranslateX() {
        return translateX;
    }

    public void setTranslateX(float translateX) {
        this.translateX = translateX;
    }

    public float getTranslateY() {
        return translateY;
    }

    public void setTranslateY(float translateY) {
        this.translateY = translateY;
    }

    public float getTranslateZ() {
        return translateZ;
    }

    public void setTranslateZ(float translateZ) {
        this.translateZ = translateZ;
    }

    public float[] getProjectionMatrix() {
        return projectionMatrix;
    }

    public float[] getViewMatrix() {
        return viewMatrix;
    }

    public void setProjectionMatrix(float[] projectionMatrix) {
        this.projectionMatrix = projectionMatrix;
    }

    public void setViewMatrix(float[] viewMatrix) {
        this.viewMatrix = viewMatrix;
    }
}
