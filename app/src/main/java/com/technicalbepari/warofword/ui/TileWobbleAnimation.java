package com.technicalbepari.warofword.ui;

import com.technicalbepari.warofword.gameframework.Interpolators;
import com.technicalbepari.warofword.gameframework.Widget;
import com.technicalbepari.warofword.gameframework.WidgetAnimation;

/**
 * Animate a tile to "wobble" on the spot. Used to draw attention to a tile
 * after it has been played.
 *
 * @author Andrew Smith
 */
public class TileWobbleAnimation extends WidgetAnimation {

	private float mStartAngle;
	private float mEndAngle;

	public TileWobbleAnimation(Widget widget, long duration, float endAngle) {
		super(widget, duration, false);

		mStartAngle = widget.getRotation();
		mEndAngle = endAngle;
	}

	@Override
	public void animationCallback(double fractionComplete) {
		double interpolatedCompleteness = Interpolators.elastic(fractionComplete);
		float newAngle = (float) (mStartAngle + (mEndAngle - mStartAngle)*interpolatedCompleteness);
		mWidget.setRotation(newAngle);

		mWidget.setShadow(10,0xFF444444);
	}

	@Override
	public void complete(){
		mWidget.setShadow(0,0);
	}

	@Override
	public void cancel(){
		complete();
	}
}
