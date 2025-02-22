package com.edaoyou.collections.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * 
 * @Description: 用于缩放裁剪的自定义ImageView视图
 * @author rongwei.mei mayroewe@live.cn 
 * @date 2014-10-14 下午3:57:09  
 * Copyright © 北京易道游网络技术有限公司 版权所有
 */
public class ClipImageView extends ImageView implements View.OnTouchListener,
		ViewTreeObserver.OnGlobalLayoutListener {
	private static final int BORDERDISTANCE = ClipView.BORDERDISTANCE;

	public static final float DEFAULT_MAX_SCALE = 4.0f;
	public static final float DEFAULT_MID_SCALE = 2.0f;
	public static final float DEFAULT_MIN_SCALE = 1.0f;

	private float minScale = DEFAULT_MIN_SCALE;
	private float midScale = DEFAULT_MID_SCALE;
	private float maxScale = DEFAULT_MAX_SCALE;

	private MultiGestureDetector multiGestureDetector;

	private int borderlength;

	private boolean isJusted;

	private final Matrix baseMatrix = new Matrix();
	private final Matrix drawMatrix = new Matrix();
	private final Matrix suppMatrix = new Matrix();
	private final RectF displayRect = new RectF();
	private final float[] matrixValues = new float[9];

	public ClipImageView(Context context) {
		this(context, null);
	}

	public ClipImageView(Context context, AttributeSet attr) {
		this(context, attr, 0);
	}

	public ClipImageView(Context context, AttributeSet attr, int defStyle) {
		super(context, attr, defStyle);

		super.setScaleType(ScaleType.MATRIX);

		setOnTouchListener(this);

		multiGestureDetector = new MultiGestureDetector(context);

	}

	/**
	 * 依据图片宽高比例，设置图像初始缩放等级和位置
	 */
	private void configPosition() {
		super.setScaleType(ScaleType.MATRIX);
		Drawable d = getDrawable();
		if (d == null) {
			return;
		}
		final float viewWidth = getWidth();
		final float viewHeight = getHeight();
		final int drawableWidth = d.getIntrinsicWidth();
		final int drawableHeight = d.getIntrinsicHeight();

		borderlength = (int) (viewWidth - BORDERDISTANCE * 2);
		float scale = 1.0f;
		/**
		 * 判断图片宽高比例，调整显示位置和缩放大小
		 */
		// 图片宽度小于等于高度
		if (drawableWidth <= drawableHeight) {
			// 判断图片宽度是否小于边框, 缩放铺满裁剪边框
			if (drawableWidth < borderlength) {
				baseMatrix.reset();
				scale = (float) borderlength / drawableWidth;
				// 缩放
				baseMatrix.postScale(scale, scale);
			}
			// 图片宽度大于高度
		} else {
			if (drawableHeight < borderlength) {
				baseMatrix.reset();
				scale = (float) borderlength / drawableHeight;
				// 缩放
				baseMatrix.postScale(scale, scale);
			}
		}
		// 移动居中
		baseMatrix.postTranslate((viewWidth - drawableWidth * scale) / 2,
				(viewHeight - drawableHeight * scale) / 2);

		resetMatrix();
		isJusted = true;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return multiGestureDetector.onTouchEvent(event);
	}

	private class MultiGestureDetector extends
			GestureDetector.SimpleOnGestureListener implements
			OnScaleGestureListener {

		private final ScaleGestureDetector scaleGestureDetector;
		private final GestureDetector gestureDetector;
		private final float scaledTouchSlop;

		private VelocityTracker velocityTracker;
		private boolean isDragging;

		private float lastTouchX;
		private float lastTouchY;
		private float lastPointerCount;

		public MultiGestureDetector(Context context) {
			scaleGestureDetector = new ScaleGestureDetector(context, this);

			gestureDetector = new GestureDetector(context, this);
			gestureDetector.setOnDoubleTapListener(this);

			final ViewConfiguration configuration = ViewConfiguration
					.get(context);
			scaledTouchSlop = configuration.getScaledTouchSlop();
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float scale = getScale();
			float scaleFactor = detector.getScaleFactor();
			if (getDrawable() != null
					&& ((scale < maxScale && scaleFactor > 1.0f) || (scale > minScale && scaleFactor < 1.0f))) {
				if (scaleFactor * scale < minScale) {
					scaleFactor = minScale / scale;
				}
				if (scaleFactor * scale > maxScale) {
					scaleFactor = maxScale / scale;
				}
				suppMatrix.postScale(scaleFactor, scaleFactor, getWidth() / 2,
						getHeight() / 2);
				checkAndDisplayMatrix();
			}
			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
		}

		public boolean onTouchEvent(MotionEvent event) {
			if (gestureDetector.onTouchEvent(event)) {
				return true;
			}

			scaleGestureDetector.onTouchEvent(event);
			float x = 0, y = 0;
			final int pointerCount = event.getPointerCount();
			for (int i = 0; i < pointerCount; i++) {
				x += event.getX(i);
				y += event.getY(i);
			}
			x = x / pointerCount;
			y = y / pointerCount;

			if (pointerCount != lastPointerCount) {
				isDragging = false;
				if (velocityTracker != null) {
					velocityTracker.clear();
				}
				lastTouchX = x;
				lastTouchY = y;
			}
			lastPointerCount = pointerCount;

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (velocityTracker == null) {
					velocityTracker = VelocityTracker.obtain();
				} else {
					velocityTracker.clear();
				}
				velocityTracker.addMovement(event);

				lastTouchX = x;
				lastTouchY = y;
				isDragging = false;
				break;

			case MotionEvent.ACTION_MOVE: {
				final float dx = x - lastTouchX, dy = y - lastTouchY;

				if (isDragging == false) {
					isDragging = Math.sqrt((dx * dx) + (dy * dy)) >= scaledTouchSlop;
				}

				if (isDragging) {
					if (getDrawable() != null) {
						suppMatrix.postTranslate(dx, dy);
						checkAndDisplayMatrix();
					}

					lastTouchX = x;
					lastTouchY = y;

					if (velocityTracker != null) {
						velocityTracker.addMovement(event);
					}
				}
				break;
			}
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				lastPointerCount = 0;
				if (velocityTracker != null) {
					velocityTracker.recycle();
					velocityTracker = null;
				}
				break;
			default:
				break;
			}

			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent event) {
			try {
				float scale = getScale();
				float x = getWidth() / 2;
				float y = getHeight() / 2;

				if (scale < midScale) {
					post(new AnimatedZoomRunnable(scale, midScale, x, y));
				} else if ((scale >= midScale) && (scale < maxScale)) {
					post(new AnimatedZoomRunnable(scale, maxScale, x, y));
				} else {
					post(new AnimatedZoomRunnable(scale, minScale, x, y));
				}
			} catch (Exception e) {

			}
			return true;
		}
	}

	private class AnimatedZoomRunnable implements Runnable {
		static final float ANIMATION_SCALE_PER_ITERATION_IN = 1.07f;
		static final float ANIMATION_SCALE_PER_ITERATION_OUT = 0.93f;

		private final float focalX, focalY;
		private final float targetZoom;
		private final float deltaScale;

		public AnimatedZoomRunnable(final float currentZoom,
				final float targetZoom, final float focalX, final float focalY) {
			this.targetZoom = targetZoom;
			this.focalX = focalX;
			this.focalY = focalY;

			if (currentZoom < targetZoom) {
				deltaScale = ANIMATION_SCALE_PER_ITERATION_IN;
			} else {
				deltaScale = ANIMATION_SCALE_PER_ITERATION_OUT;
			}
		}

		public void run() {
			suppMatrix.postScale(deltaScale, deltaScale, focalX, focalY);
			checkAndDisplayMatrix();

			final float currentScale = getScale();

			if (((deltaScale > 1f) && (currentScale < targetZoom))
					|| ((deltaScale < 1f) && (targetZoom < currentScale))) {
			} else {
				final float delta = targetZoom / currentScale;
				suppMatrix.postScale(delta, delta, focalX, focalY);
				checkAndDisplayMatrix();
			}
		}
	}

	public final float getScale() {
		suppMatrix.getValues(matrixValues);
		return matrixValues[Matrix.MSCALE_X];
	}

	@Override
	public void onGlobalLayout() {
		if (isJusted) {
			return;
		}
		//调整视图位置
		configPosition();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		getViewTreeObserver().addOnGlobalLayoutListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		getViewTreeObserver().removeGlobalOnLayoutListener(this);
	}

	private void checkAndDisplayMatrix() {
		checkMatrixBounds();
		setImageMatrix(getDisplayMatrix());
	}

	private void checkMatrixBounds() {
		final RectF rect = getDisplayRect(getDisplayMatrix());
		if (null == rect) {
			return;
		}

		float deltaX = 0, deltaY = 0;
		final float viewWidth = getWidth();
		final float viewHeight = getHeight();
		// 判断移动或缩放后，图片显示是否超出裁剪框边界
		if (rect.top > (viewHeight - borderlength) / 2) {
			deltaY = (viewHeight - borderlength) / 2 - rect.top;
		}
		if (rect.bottom < (viewHeight + borderlength) / 2) {
			deltaY = (viewHeight + borderlength) / 2 - rect.bottom;
		}
		if (rect.left > (viewWidth - borderlength) / 2) {
			deltaX = (viewWidth - borderlength) / 2 - rect.left;
		}
		if (rect.right < (viewWidth + borderlength) / 2) {
			deltaX = (viewWidth + borderlength) / 2 - rect.right;
		}
		suppMatrix.postTranslate(deltaX, deltaY);
	}

	private RectF getDisplayRect(Matrix matrix) {
		Drawable d = getDrawable();
		if (null != d) {
			displayRect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
			matrix.mapRect(displayRect);
			return displayRect;
		}
		return null;
	}

	private void resetMatrix() {
		if (suppMatrix == null) {
			return;
		}
		suppMatrix.reset();
		setImageMatrix(getDisplayMatrix());
	}

	protected Matrix getDisplayMatrix() {
		drawMatrix.set(baseMatrix);
		drawMatrix.postConcat(suppMatrix);
		return drawMatrix;
	}

	/**
	 * 剪切图片，返回剪切后的bitmap对象
	 * 
	 * @return
	 */
	public Bitmap clip() {
		int width = this.getWidth();
		int height = this.getHeight();

		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		draw(canvas);
		return Bitmap.createBitmap(bitmap,
				(getWidth() - borderlength) / 2,
				(getHeight() - borderlength) / 2, borderlength, borderlength);
	}

}
