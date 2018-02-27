#ifdef GL_ES
precision highp float;
#endif
varying vec4 vcolor;

void main(void)
{
gl_FragColor = vcolor;
}
