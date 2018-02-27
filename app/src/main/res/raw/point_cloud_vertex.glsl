precision mediump float;

attribute vec4 a_Position;
uniform mat4 u_MVP;
//uniform float u_PointThickness;
attribute vec4 a_Color;
varying vec4 v_Color;

void main() {
    gl_Position = u_MVP * a_Position;
    gl_PointSize = 5.0;
    v_Color = a_Color;
}
