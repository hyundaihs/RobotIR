#version 100

precision mediump float;
varying vec2 v_texCoord;
uniform sampler2D u_samplerTexture;
void main() {
    gl_FragColor = texture2D(u_samplerTexture, v_texCoord);
}
