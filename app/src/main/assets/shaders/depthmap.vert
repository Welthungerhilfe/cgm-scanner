uniform float u_PointSize;
uniform float u_Rotation;

attribute vec4 a_Position;
attribute vec4 a_Edge;
attribute vec4 a_Color;

varying vec4 v_Color;

void main() {
   v_Color = vec4(1.0 / a_Position.z, a_Color.r, a_Edge.x, 1.0);
   if (abs(u_Rotation - 180.0) < 1.0) gl_Position = vec4(-a_Position.y, a_Position.x, a_Position.z * 0.01, 1.0);
   if (abs(u_Rotation - 90.0) < 1.0) gl_Position = vec4(-a_Position.x, -a_Position.y, a_Position.z * 0.01, 1.0);
   if (abs(u_Rotation - 0.0) < 1.0) gl_Position = vec4(a_Position.y, -a_Position.x, a_Position.z * 0.01, 1.0);
   if (abs(u_Rotation + 90.0) < 1.0) gl_Position = vec4(a_Position.x, a_Position.y, a_Position.z * 0.01, 1.0);
   gl_PointSize = u_PointSize;
}
