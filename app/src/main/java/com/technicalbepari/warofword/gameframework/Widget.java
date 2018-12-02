package com.technicalbepari.warofword.gameframework;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.MotionEvent;

/**
 * A simple rectangular widget that can be drawn by a RenderSurface.
 *
 * @author Andrew Smith
 */
public class Widget {

	protected Paint mPaint;

	protected int mX;
	protected int mY;
	protected int mWidth;
	protected int mHeight;

	protected int mXOffset = 0;
	protected int mYOffset = 0;

	protected float mRotation = 0f;

	protected List<WidgetAnimation> mAnimations = new LinkedList<WidgetAnimation>();

	private static final int DRAG_THRESHOLD = 10; // pixels
	private boolean mDragInProgress = false;

	private int mDragClickOffset_x = 0;
	private int mDragClickOffset_y = 0;
	private int mDragStart_x = 0;
	private int mDragStart_y = 0;

	private WidgetClickListener mClickListener;
	private WidgetDragListener mDragListener;

	public Widget(int color) {
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setColor(color);
		mPaint.setStyle(Style.FILL);
	}

	/**
	 * Specify the basic properties of this widget. Must be called before first
	 * call to draw(..)
	 *
	 * @param x
	 *            The x coordinate relative to the RenderSurface
	 * @param y
	 *            The y coordinate relative to the RenderSurface
	 * @param width
	 *            The width in pixels
	 * @param height
	 *            The height in pixels
	 */
	public void applyLayout(int x, int y, int width, int height) {
		mX = x;
		mY = y;
		mWidth = width;
		mHeight = height;
	}

	public int getX() {
		return mX;
	}

	public void setX(int x) {
		mX = x;
	}

	public int getY() {
		return mY;
	}

	public void setY(int y) {
		mY = y;
	}

	public int getWidth() {
		return mWidth;
	}

	public void setWidth(int width) {
		mWidth = width;
	}

	public int getHeight() {
		return mHeight;
	}

	public void setHeight(int height) {
		mWidth = height;
	}

	public float getRotation() {
		return mRotation;
	}

	/**
	 * Rotate the widget around its midpoint.
	 *
	 * @param angle
	 *            Rotation angle in degrees
	 */
	public void setRotation(float angle) {
		mRotation = angle;
	}

	public synchronized void setColor(int color) {
		// Synchronize to prevent mPaint being accessed simultaneously by the
		// RenderSurface thread (will cause a segmentation fault)
		mPaint.setColor(color);
	}

	/**
	 * Apply a drop shadow around the widget
	 *
	 * @param radius
	 *            The size of the shadow
	 * @param color
	 *            The color of the drop shadow. Alpha is observed.
	 */
	public synchronized void setShadow(int radius, int color) {
		// Synchronize to prevent mPaint being accessed simultaneously by the
		// RenderSurface thread
		mPaint.setShadowLayer(radius, 0, 0, color);
	}

	public synchronized void addAnimation(WidgetAnimation animation) {
		// Synchronize to prevent mAnimations being accessed simultaneously by
		// the RenderSurface thread
		animation.start();
		mAnimations.add(animation);
	}

	public synchronized void cancelAnimation(WidgetAnimation wa) {
		// Synchronize to prevent mAnimations being accessed simultaneously by
		// the RenderSurface thread
		wa.cancel();
		mAnimations.remove(wa);
	}

	public synchronized void cancelAllAnimations() {
		// Synchronize to prevent mAnimations being accessed simultaneously by
		// the RenderSurface thread
		ListIterator<WidgetAnimation> it = mAnimations.listIterator();

		while (it.hasNext()) {
			WidgetAnimation wa = it.next();
			wa.cancel();
			it.remove();
		}

	}

	public synchronized void move(long timeSinceLastFrame) {
		// Synchronize to prevent mAnimations being accessed simultaneously by
		// the RenderSurface thread
		ListIterator<WidgetAnimation> it = mAnimations.listIterator();
		while (it.hasNext()) {
			WidgetAnimation wa = it.next();
			if (!wa.isAnimating()) {
				it.remove();
			} else {
				wa.animate(timeSinceLastFrame);
			}
		}
	}

	public void preDraw(Canvas canvas) {
		canvas.save();
		canvas.rotate(mRotation, mX + mWidth / 2, mY + mHeight / 2);
	}

	public synchronized void draw(Canvas canvas) {
		canvas.drawRect(mX, mY, mX + mWidth, mY + mHeight, mPaint);
	}

	public void postDraw(Canvas canvas) {
		canvas.restore();
	}

	public boolean pointInBounds(int x, int y) {
		return x >= mX && x <= mX + mWidth && y >= mY && y <= mY + mHeight;
	}

	public void setClickListener(WidgetClickListener clickListener) {
		mClickListener = clickListener;
	}

	public void setDragListener(WidgetDragListener dragListener) {
		mDragListener = dragListener;
	}

	public boolean onTouchEvent(MotionEvent m) {

		if (mClickListener != null || mDragListener != null) {

			int click_x = (int) (m.getX());
			int click_y = (int) (m.getY());

			if (m.getAction() == MotionEvent.ACTION_DOWN) {

				mDragClickOffset_x = click_x - mX;
				mDragClickOffset_y = click_y - mY;

				mDragStart_x = click_x;
				mDragStart_y = click_y;

			} else if (m.getAction() == MotionEvent.ACTION_UP
					|| m.getAction() == MotionEvent.ACTION_CANCEL) {

				if (mDragInProgress) {

					if (mDragListener != null) {
						mDragListener.onDragEnd(this);
					}
					mDragInProgress = false;

				} else {

					if (mClickListener != null) {
						mClickListener.onClick(this);
					}

				}

			} else if (m.getAction() == MotionEvent.ACTION_MOVE) {

				if (mDragInProgress) {

					int newX = click_x - mDragClickOffset_x;
					int newY = click_y - mDragClickOffset_y;
					if (mDragListener != null) {
						mDragListener.onDrag(this, newX, newY);
					}

				} else {

					int dY = Math.abs(click_y - mDragStart_y);
					int dX = Math.abs(click_x - mDragStart_x);

					if (Math.hypot(dX, dY) > DRAG_THRESHOLD) {
						mDragInProgress = true;
						if (mDragListener != null) {
							mDragListener.onDragStart(this);
						}
					}
				}
			}

			return true;

		}

		return false;

	}

}
