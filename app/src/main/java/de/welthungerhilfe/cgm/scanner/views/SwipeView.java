package de.welthungerhilfe.cgm.scanner.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.view.View;

import de.welthungerhilfe.cgm.scanner.R;

public abstract class SwipeView extends Callback {

    private Drawable background;
    private Drawable deleteIcon;
    private Drawable editIcon;

    private int xMarkMargin;

    private boolean initiated;
    private Context context;

    private int direction;

    public SwipeView(int direction, Context context) {
        super();
        this.context = context;
        this.direction = direction;
    }

    private void init() {
        background = new ColorDrawable();
        xMarkMargin = (int) context.getResources().getDimension(R.dimen.activity_horizontal_margin);
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.trash);
        deleteIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        editIcon = ContextCompat.getDrawable(context, R.drawable.edit);
        editIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        initiated = true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, direction);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public abstract void onSwiped(RecyclerView.ViewHolder viewHolder, int direction);

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        if (!initiated) {
            init();
        }

        int itemHeight = itemView.getBottom() - itemView.getTop();

        //Setting Swipe Background
        if (dX > 0) {
            ((ColorDrawable) background).setColor(ContextCompat.getColor(context, R.color.colorPrimary));
            background.setBounds(0, itemView.getTop(), (int) dX, itemView.getBottom());
            background.draw(c);

            int intrinsicWidth = editIcon.getIntrinsicWidth();
            int intrinsicHeight = editIcon.getIntrinsicWidth();

            int xMarkLeft = xMarkMargin;
            int xMarkRight = xMarkMargin + intrinsicWidth;
            int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int xMarkBottom = xMarkTop + intrinsicHeight;

            //Setting Swipe Icon
            editIcon.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
            editIcon.draw(c);
        } else {
            ((ColorDrawable) background).setColor(ContextCompat.getColor(context, R.color.colorBlue));
            background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            background.draw(c);

            int intrinsicWidth = deleteIcon.getIntrinsicWidth();
            int intrinsicHeight = deleteIcon.getIntrinsicWidth();

            int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
            int xMarkRight = itemView.getRight() - xMarkMargin;
            int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int xMarkBottom = xMarkTop + intrinsicHeight;

            //Setting Swipe Icon
            deleteIcon.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
            deleteIcon.draw(c);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
