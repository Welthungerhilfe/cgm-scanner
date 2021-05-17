/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package de.welthungerhilfe.cgm.scanner.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class HandMotionView extends androidx.appcompat.widget.AppCompatImageView {

  private static final long ANIMATION_SPEED_MS = 2500;
  private HandMotionAnimation animation;

  public HandMotionView(Context context) {
    super(context);
  }

  public HandMotionView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    setScaleX(0.5f);
    setScaleY(0.5f);

    clearAnimation();

    animation = new HandMotionAnimation(this);
    animation.setRepeatCount(Animation.INFINITE);
    animation.setDuration(ANIMATION_SPEED_MS);
    animation.setStartOffset(1000);

    startAnimation(animation);
  }

  public HandMotionAnimation getAnimation() {
    return animation;
  }

  public static class HandMotionAnimation extends Animation {
    private final View handImageView;

    private static final float TWO_PI = (float) Math.PI * 2.0f;
    private static final float HALF_PI = (float) Math.PI / 2.0f;

    private int offsetX = 0;
    private int offsetY = 0;

    public HandMotionAnimation(View handImageView) {
      this.handImageView = handImageView;
    }

    public void setOffset(int x, int y) {
      offsetX = x;
      offsetY = y;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation) {
      float progressAngle = TWO_PI * interpolatedTime;
      float currentAngle = HALF_PI + progressAngle;

      float density = handImageView.getResources().getDisplayMetrics().density;
      float handWidth = handImageView.getWidth();
      float radius = density * 15.0f;

      float xPos = radius * 2.0f * (float) Math.cos(currentAngle);
      float yPos = radius * (float) Math.sin(currentAngle);

      xPos += offsetX;
      yPos += offsetY;

      xPos -= handWidth / 2.0f;
      yPos -= handImageView.getHeight() / 2.0f;

      // Position the hand.
      handImageView.setX(xPos);
      handImageView.setY(yPos);
      handImageView.invalidate();
    }
  }
}