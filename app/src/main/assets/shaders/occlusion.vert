/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

uniform float u_PointSize;

attribute vec4 a_Position;

varying vec4 v_Color;

void main() {
   v_Color = vec4(a_Position.z * 4.0, a_Position.z * 2.0, a_Position.z, 1.0);
   gl_Position = vec4(a_Position.xyz, 1.0);
   gl_PointSize = u_PointSize;
}
