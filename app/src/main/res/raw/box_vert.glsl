uniform mat4 trans;
uniform mat4 proj;   //u_mvp
attribute vec4 coord;  //a_position
attribute vec4 color;
varying vec4 vcolor;

void main(void)
{
vcolor = color;
gl_Position = proj * trans * coord;
//gl_Position = proj * coord;
gl_PointSize = 5.0;
}
