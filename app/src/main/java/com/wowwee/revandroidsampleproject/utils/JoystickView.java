package com.wowwee.revandroidsampleproject.utils;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.wowwee.revandroidsampleproject.R;

public class JoystickView extends FrameLayout {
	private float frameRadius;
	private float joystickCenterRadius;
	private Context context;
	private ImageView joystickFrame;
	private ImageView joystickCenter;
	private MotionEvent touchToTrack;
	private float joystickVectorX;
	private float joystickVectorY;
	private Point ptCenter;
	boolean isLeft;
	
	float scale_x;
	float scale_y;

	public static float scaleRatio = 1.0f;
	
	public JoystickView(Context context) {
		super(context);
		
		this.init(context);
	}

	public JoystickView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		this.init(context);		
	}
	
	public JoystickView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		this.init(context);
	}
	
	public void init(Context context) {		
		this.context = context;
		this.frameRadius = -1;
		this.joystickCenterRadius = -1;
	}
	
	public void setScale(float x, float y) {
		this.scale_x = x;
		this.scale_y = y;
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.getLayoutParams();
		params.width = (int) (params.width*x);
		params.height = (int) (params.height*y);
	}
		
	public void UpdateLeftView() {	
		this.joystickVectorX = 0;
		this.joystickVectorY = 0;
		
		this.joystickFrame = (ImageView) this.findViewById(R.id.joystickBaseL);
		this.joystickCenter = (ImageView) this.findViewById(R.id.joystickL);
		isLeft = true;
		
		this.setScale(scaleRatio, scaleRatio);
		LayoutParams params = (LayoutParams)this.joystickFrame.getLayoutParams();
		params.width = (int) (params.width*this.scale_x);
		params.height = (int) (params.height*this.scale_y);
		this.joystickFrame.setLayoutParams(params);

		params = (LayoutParams)this.joystickCenter.getLayoutParams();
		params.width = (int) (params.width*this.scale_x);
		params.height = (int) (params.height*this.scale_y);
		this.joystickCenter.setLayoutParams(params);
	}

	public void UpdateRightView() {
		this.joystickVectorX = 0;
		this.joystickVectorY = 0;

		this.joystickFrame = (ImageView) this.findViewById(R.id.joystickBaseR);
		this.joystickCenter = (ImageView) this.findViewById(R.id.joystickR);

		this.setScale(scaleRatio, scaleRatio);
		LayoutParams params = (LayoutParams)this.joystickFrame.getLayoutParams();
		params.width = (int) (params.width*this.scale_x);
		params.height = (int) (params.height*this.scale_y);
		this.joystickFrame.setLayoutParams(params);

		params = (LayoutParams)this.joystickCenter.getLayoutParams();
		params.width = (int) (params.width*this.scale_x);
		params.height = (int) (params.height*this.scale_y);
		this.joystickCenter.setLayoutParams(params);
	}

	public void updateJoystickVector() {
		LayoutParams params = (LayoutParams)this.joystickCenter.getLayoutParams();
		Point coreRingCenter = new Point();
		coreRingCenter.x = (int)(params.leftMargin-(this.frameRadius-this.joystickCenterRadius));
		coreRingCenter.y = (int)(params.topMargin-(this.frameRadius-this.joystickCenterRadius));

		float dx = (float)(coreRingCenter.x) / (this.frameRadius-this.joystickCenterRadius);
		float dy = (float)(coreRingCenter.y) / -(this.frameRadius-this.joystickCenterRadius);
		if (isLeft)
			this.joystickVectorY = Math.max(-1, Math.min(1, dy));
		if (!isLeft)
			this.joystickVectorX = Math.max(-1, Math.min(1, dx));
	}

	public float getJoystickVectorX() {
		return this.joystickVectorX;
	}

	public float getJoystickVectorY() {
		return this.joystickVectorY;
	}

	public boolean IsTouchToTrack() {
		return this.touchToTrack != null;
	}

	public boolean IsTouchToTrack(MotionEvent evt) {
		return this.touchToTrack == evt;
	}

	public boolean IsTouchToTrack(MotionEvent evt, int id) {
		return this.touchToTrack == evt && this.id == id;
	}

	public void SetTouchToTrack(MotionEvent touchToTrack, int anID) {
		this.touchToTrack = touchToTrack;
		this.id = anID;
	}

	public void SetCenter(Point pt) {
		if (this.frameRadius == -1)
			this.frameRadius = this.getWidth()/2;

		if (this.joystickCenterRadius == -1)
			this.joystickCenterRadius = this.joystickCenter.getWidth()/2;

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) this.getLayoutParams();
		params.setMargins(pt.x-(int)this.frameRadius, pt.y-(int)this.frameRadius, 0, 0);
		this.ptCenter = pt;

		this.setLayoutParams(params);

		LayoutParams paramsCoreRing = (LayoutParams)this.joystickCenter.getLayoutParams();
		paramsCoreRing.leftMargin = (int)this.frameRadius-(int)this.joystickCenterRadius;
		paramsCoreRing.topMargin = (int)this.frameRadius-(int)this.joystickCenterRadius;
		this.joystickCenter.setLayoutParams(paramsCoreRing);
	}

	int id;

	public boolean touchesBegan(MotionEvent event) {
		if (this.touchToTrack == null) {
			Rect rect = new Rect();
			this.getGlobalVisibleRect(rect);
			if (rect.contains((int)event.getX(event.getActionIndex()), (int)event.getY(event.getActionIndex()))) {
					this.id = event.getPointerId(event.getActionIndex());
					this.touchToTrack = event;
					return true;
				}
			}
		return false;
	}

	public boolean touchesMoved(MotionEvent event, int i) {
		if (this.touchToTrack == event && this.id == event.getPointerId(i)) {
			Point location = new Point();
			location.x = (int) event.getX(i);
			location.y = (int) event.getY(i);

			Point locationZeroed = new Point();
			locationZeroed.x = location.x-this.ptCenter.x;
			locationZeroed.y = location.y-this.ptCenter.y;

			float angle = (float) Math.atan2((float)locationZeroed.x,(float)locationZeroed.y);
			float maxX = (float)Math.sin(angle)*((float)frameRadius-(float)this.joystickCenter.getWidth()/2);
			float maxY = (float)Math.cos(angle)*((float)frameRadius-(float)this.joystickCenter.getHeight()/2);

			if ((maxX > 0 && locationZeroed.x > maxX) || (maxX <0 && locationZeroed.x < maxX)) {
				location.x = (int) (maxX)+this.ptCenter.x;
			}
			if ((maxY > 0 && locationZeroed.y > maxY) || (maxY <0 && locationZeroed.y < maxY)) {
				location.y = (int) (maxY)+this.ptCenter.y;
			}

			LayoutParams paramsCoreRing = (LayoutParams)this.joystickCenter.getLayoutParams();
			paramsCoreRing.leftMargin = (int)this.frameRadius-this.joystickCenter.getWidth()/2+location.x-this.ptCenter.x;
			paramsCoreRing.topMargin = (int)this.frameRadius-this.joystickCenter.getWidth()/2+location.y-this.ptCenter.y;
			this.joystickCenter.setLayoutParams(paramsCoreRing);
			
			
			this.updateJoystickVector();
			return true;
 		}
		return false;
	}
	
	public void touchesEnded(MotionEvent event) {
		if (this.touchToTrack != null) {
			if (this.touchToTrack == event && this.id == event.getPointerId(event.getActionIndex())) {
				this.touchToTrack = null;
			}
		}
	}

	
}
