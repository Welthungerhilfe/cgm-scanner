package com.github.mikephil.charting.interfaces.datasets;

import android.graphics.DashPathEffect;
import android.graphics.Path;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;

/**
 * Created by Philpp Jahoda on 21/10/15.
 */
public interface ILineDataSet extends ILineRadarDataSet<Entry> {

    /**
     * Returns the drawing mode for this line dataset
     *
     * @return
     */
    LineDataSet.Mode getMode();

    /**
     * Returns the intensity of the cubic lines (the effect intensity).
     * Max = 1f = very cubic, Min = 0.05f = low cubic effect, Default: 0.2f
     *
     * @return
     */
    float getCubicIntensity();

    @Deprecated
    boolean isDrawCubicEnabled();

    @Deprecated
    boolean isDrawSteppedEnabled();

    /**
     * Returns the geometry of the drawn shapes.
     */
    Path getShapePath();

    /**
     * Returns the color at the given index of the DataSet's shape-color array.
     * Performs a IndexOutOfBounds check by modulus.
     *
     * @param index
     * @return
     */
    int getShapeColor(int index);

    /**
     * Returns true if drawing shapes for this DataSet is enabled, false if not
     *
     * @return
     */
    boolean isDrawShapesEnabled();

    /**
     * Returns true if drawing shapes shadow for this DataSet is enabled, false if not
     *
     * @return
     */
    boolean isDrawShapesShadowEnabled();

    /**
     * Returns the DashPathEffect that is used for drawing the lines.
     *
     * @return
     */
    DashPathEffect getDashPathEffect();

    /**
     * Returns true if the dashed-line effect is enabled, false if not.
     * If the DashPathEffect object is null, also return false here.
     *
     * @return
     */
    boolean isDashedLineEnabled();

    /**
     * Returns the IFillFormatter that is set for this DataSet.
     *
     * @return
     */
    IFillFormatter getFillFormatter();
}