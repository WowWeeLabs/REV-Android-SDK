package com.wowwee.revandroidsampleproject.fragments;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant.revRobotTrackingMode;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;

import java.util.Timer;

import com.wowwee.revandroidsampleproject.data.JoystickData;
import com.wowwee.revandroidsampleproject.data.JoystickData.TYPE;
import com.wowwee.revandroidsampleproject.drawer.JoystickDrawer;
import com.wowwee.revandroidsampleproject.utils.JoystickView;
import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.utils.Player;
import com.wowwee.revandroidsampleproject.weapon.WeaponManager;

@SuppressLint("ValidFragment")
public class DriveViewFragment extends BaseViewFragment implements OnTouchListener {
	public final static String BROADCAST_REVIVE = "com.revsampleproject.revive";

	protected SurfaceView touchArea;

	protected JoystickData leftJoystickData;
	protected JoystickData rightJoystickData;

	protected JoystickDrawer leftJoystickDrawer;
	protected JoystickDrawer rightJoystickDrawer;

	protected Bitmap outerRingBitmap;
	protected Bitmap outerRingLeftBitmap;
	protected Bitmap outerRingRightBitmap;
	protected Bitmap leftBitmap;
	protected Bitmap rightBitmap;

	protected int leftJoystickDrawableId;
	protected int rightJoystickDrawableId;
	protected int outerRingDrawableId;
	protected int outerRingLeftDrawableId;
	protected int outerRingRightDrawableId;

	protected float[] movementVector = new float[]{0, 0};
	protected float[] sendVector = new float[]{0, 0};
	protected Timer joystickTimer;
	protected boolean moveMip;
	protected boolean isJoystickTimerRunning = false;

	protected boolean isOpening = false;

	protected boolean driveEnabled = true;

	protected JoystickView joystickLeft;
	protected JoystickView joystickRight;

	private View view;

	private TextView tvHealth;
	private Button btnFire;
	private Spinner spTrackingMode;
	private Spinner spGun;

	private int selectedGun = 0;

	private final float DEFAULT_DRIVE_SPEED = 1.0f;
	private final float DEFAULT_TURN_SPEED = 1.0f;
	private final float DEFAULT_DRAW_RATIO = 2.0f;

	public DriveViewFragment() {
		super(R.layout.fragment_drive);

		leftJoystickDrawableId = R.drawable.drive_core_ring;
		rightJoystickDrawableId = R.drawable.drive_core_ring;
		outerRingDrawableId = R.drawable.drive_outer_gear_ring;
		outerRingLeftDrawableId = R.drawable.drive_outer_gear_ring;
		outerRingRightDrawableId = R.drawable.drive_outer_gear_ring;
	}

	protected void setDriveEnabled(boolean driveEnabled) {
		this.driveEnabled = driveEnabled;
		if(!driveEnabled) {
			movementVector[0] = 0;
			movementVector[1] = 0;
		}
		btnFire.setEnabled(driveEnabled);
		spGun.setEnabled(driveEnabled);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		// Unregister broadcast
		getActivity().unregisterReceiver(mBroadcast);

		if(leftJoystickDrawer != null) {
			leftJoystickDrawer.destroy();
			leftJoystickDrawer = null;
		}
		if(rightJoystickDrawer != null) {
			rightJoystickDrawer.destroy();
			rightJoystickDrawer = null;
		}
		outerRingBitmap.recycle();
		outerRingBitmap = null;
		outerRingLeftBitmap.recycle();
		outerRingLeftBitmap = null;
		outerRingRightBitmap.recycle();
		outerRingRightBitmap = null;
		leftBitmap.recycle();
		leftBitmap = null;
		rightBitmap.recycle();
		rightBitmap = null;
		if(joystickTimer != null) {
			joystickTimer.cancel();
			joystickTimer.purge();
			joystickTimer = null;
		}
		isJoystickTimerRunning = false;
	}

	@Override
	public void onPause() {
		super.onPause();
		if(joystickTimer != null) {
			joystickTimer.cancel();
			joystickTimer.purge();
			joystickTimer = null;
		}
		isJoystickTimerRunning = false;
	}

	@Override
	public void onResume() {
		super.onResume();

		if(!isJoystickTimerRunning) {
			Thread newThread = new Thread(new JoystickRunnable());
			newThread.start();
			isJoystickTimerRunning = true;
		}

		if(REVRobotFinder.getInstance().firstConnectedREV() != null) {
			REVRobotFinder.getInstance().firstConnectedREV().revSetTrackingMode(revRobotTrackingMode.REVTrackingUserControl);
			setDriveEnabled(true);
		}
		else {
			FragmentHelper.switchFragment(getFragmentActivity().getSupportFragmentManager(), new ScanFragment(), R.id.view_id_content, false);
		}
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null)
			return null;

		// Register broadcast
		getActivity().registerReceiver(mBroadcast, new IntentFilter(BROADCAST_REVIVE));

		// Init weapon manager
		WeaponManager.getInstance().Load(getFragmentActivity());

		view = super.onCreateView(inflater, container, savedInstanceState);

		// Handle the touch area
		touchArea = (SurfaceView)view.findViewById(R.id.view_id_touch_area);
		touchArea.setZOrderOnTop(true);
		touchArea.getHolder().setFormat(PixelFormat.TRANSLUCENT);

		// Create the joystick data
		leftJoystickData = new JoystickData(TYPE.LEFT);
		rightJoystickData = new JoystickData(TYPE.RIGHT);

		// Create the bitmaps for joystick
		BitmapFactory.Options bitmapFactoryOption = new BitmapFactory.Options();
		bitmapFactoryOption.inScaled = false;
		outerRingBitmap = BitmapFactory.decodeResource(getResources(), outerRingDrawableId, bitmapFactoryOption);
		outerRingLeftBitmap = BitmapFactory.decodeResource(getResources(), outerRingLeftDrawableId, bitmapFactoryOption);
		outerRingRightBitmap = BitmapFactory.decodeResource(getResources(), outerRingRightDrawableId, bitmapFactoryOption);

		leftBitmap = BitmapFactory.decodeResource(getResources(), leftJoystickDrawableId, bitmapFactoryOption);
		rightBitmap = BitmapFactory.decodeResource(getResources(), rightJoystickDrawableId, bitmapFactoryOption);

		// Create the joystick drawer
		leftJoystickDrawer = new JoystickDrawer(outerRingLeftBitmap, leftBitmap);
		rightJoystickDrawer = new JoystickDrawer(outerRingRightBitmap, rightBitmap);

		// Compute the draw ratio for joystick
		leftJoystickDrawer.setDrawRatio(DEFAULT_DRAW_RATIO);
		rightJoystickDrawer.setDrawRatio(DEFAULT_DRAW_RATIO);
		leftJoystickData.setMaxJoystickValue(leftJoystickDrawer.getMaxJoystickValue());
		rightJoystickData.setMaxJoystickValue(rightJoystickDrawer.getMaxJoystickValue());

		// Handle the touches
		touchArea.setOnTouchListener(this);

		// Start joystick send BLE data loop
		Thread newThread = new Thread(new JoystickRunnable());
		newThread.start();
		isJoystickTimerRunning = true;

		this.joystickLeft = (JoystickView)view.findViewById(R.id.layoutleftJoystick);
		this.joystickLeft.UpdateLeftView();
		this.joystickLeft.setVisibility(View.INVISIBLE);
		this.joystickRight = (JoystickView)view.findViewById(R.id.layoutrightJoystick);
		this.joystickRight.UpdateRightView();
		this.joystickRight.setVisibility(View.INVISIBLE);

		// Define health text
		tvHealth = (TextView)view.findViewById(R.id.tvHealth);
		tvHealth.setText(getString(R.string.health) + " " + rev.health);

		// Define shoot button
		btnFire = (Button)view.findViewById(R.id.btnFire);
		btnFire.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Player.getInstance().gunFire(rev, selectedGun);
			}
		});

		// Define tracking mode spinner
		spTrackingMode = (Spinner)view.findViewById(R.id.spTrackingMode);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter_tracking_mode = ArrayAdapter.createFromResource(getActivity(), R.array.tracking_mode_array, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter_tracking_mode.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spTrackingMode.setAdapter(adapter_tracking_mode);
		spTrackingMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
				if(rev != null) {
					switch (i) {
						case 0:
							rev.revSetTrackingMode(revRobotTrackingMode.REVTrackingUserControl);
							setDriveEnabled(true);
							break;
						case 1:
							rev.revSetTrackingMode(revRobotTrackingMode.REVTrackingChase);
							setDriveEnabled(false);
							break;
						case 2:
							rev.revSetTrackingMode(revRobotTrackingMode.REVTrackingTurret);
							setDriveEnabled(false);
							break;
						case 3:
							rev.revSetTrackingMode(revRobotTrackingMode.REVTrackingAvoid);
							setDriveEnabled(false);
							break;
						case 4:
							rev.revSetTrackingMode(revRobotTrackingMode.REVTrackingBeacon);
							setDriveEnabled(false);
							break;
						case 5:
							rev.revSetTrackingMode(revRobotTrackingMode.REVTrackingRamp);
							setDriveEnabled(false);
							break;
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {

			}
		});

		// Define tracking mode spinner
		spGun = (Spinner)view.findViewById(R.id.spGun);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter_gun = ArrayAdapter.createFromResource(getActivity(), R.array.gun_array, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter_gun.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spGun.setAdapter(adapter_gun);
		spGun.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
				selectedGun = i;
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {

			}
		});

		// Set led to blue color when game start
		rev.revSetRGBLed(REVRobotConstant.revRobotColor.REVRobotColorBlue);

		return view;
	}

	//================================================================================
	// OnTouch listener
	//================================================================================

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				break;
			case MotionEvent.ACTION_UP:
				v.performClick();
				break;
			default:
				break;
		}

		if (touchArea.getHolder().isCreating()) {
			return true;
		}

		Canvas canvas = touchArea.getHolder().lockCanvas();
		if(canvas != null) {
			canvas.drawColor(0, PorterDuff.Mode.CLEAR);
		}
		if (v == touchArea && driveEnabled) {
			switch(event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_POINTER_DOWN: {
					Point pt = new Point();
					pt.x = (int) event.getX(event.getActionIndex());
					pt.y = (int) event.getY(event.getActionIndex());
					Display display = getFragmentActivity().getWindowManager().getDefaultDisplay();
					Point size = new Point();
					display.getSize(size);
					if (pt.x < size.x/2) {
						if (!this.joystickLeft.IsTouchToTrack()) {
							this.joystickLeft.setVisibility(View.VISIBLE);
							this.joystickLeft.SetCenter(pt);
							this.joystickLeft.SetTouchToTrack(event, event.getPointerId(event.getActionIndex()));
						}
					}
					else {
						if (!this.joystickRight.IsTouchToTrack()) {
							this.joystickRight.setVisibility(View.VISIBLE);
							this.joystickRight.SetCenter(pt);
							this.joystickRight.SetTouchToTrack(event, event.getPointerId(event.getActionIndex()));
						}
					}
				}
					break;
				case MotionEvent.ACTION_MOVE: {
					// Update the drag points
					for (int i=0; i<event.getPointerCount(); i++) {
						if (this.joystickLeft.IsTouchToTrack(event, event.getPointerId(i))) {
							this.joystickLeft.touchesMoved(event, i);
							movementVector[1] = this.joystickLeft.getJoystickVectorY();
							moveMip = true;
						}
						if (this.joystickRight.IsTouchToTrack(event, event.getPointerId(i))) {
							this.joystickRight.touchesMoved(event, i);
							movementVector[0] = this.joystickRight.getJoystickVectorX();
							moveMip = true;
						}
					}
				}
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_POINTER_UP:
				case MotionEvent.ACTION_CANCEL: {
					if (this.joystickLeft.IsTouchToTrack(event, event.getPointerId(event.getActionIndex()))){
						this.joystickLeft.setVisibility(View.INVISIBLE);
						this.joystickLeft.touchesEnded(event);
						movementVector[1] = 0;
					}
					if (this.joystickRight.IsTouchToTrack(event, event.getPointerId(event.getActionIndex()))){
						this.joystickRight.setVisibility(View.INVISIBLE);
						this.joystickRight.touchesEnded(event);
						movementVector[0] = 0;
					}
				}
					break;
			}
		}

		if(!isOpening && driveEnabled && canvas != null) {
			// Draw the joysticks
			if(leftJoystickDrawer != null) {
				leftJoystickDrawer.drawJoystick(canvas, leftJoystickData);
			}
			if(rightJoystickDrawer != null) {
				rightJoystickDrawer.drawJoystick(canvas, rightJoystickData);
			}

			// Handle extra onTouchEvent
			extraDrawForOnTouch(canvas, v, event);
		}
		if(canvas != null) {
			touchArea.getHolder().unlockCanvasAndPost(canvas);
		}

		return true;
	}

	protected void extraDrawForOnTouch(Canvas canvas, View v, MotionEvent event) {

	}

	class JoystickRunnable implements Runnable {
		public void run() {
			do {
				if(moveMip && (movementVector[0] != 0 || movementVector[1] != 0) && !(rev.isDead())) {
					sendVector[0] = movementVector[0];
					sendVector[1] = movementVector[1];
					if(sendVector[1] < 0) {
						sendVector[0] = sendVector[0]*-1;
					}
					REVRobot robot = REVRobotFinder.getInstance().firstConnectedREV();
					if (robot != null) {
						robot.revDrive(sendVector, DEFAULT_DRIVE_SPEED, DEFAULT_TURN_SPEED);
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}while(true);
		}
	}

	//================================================================================
	// REVRobot callback
	//================================================================================

	@Override
	public void revDidReceiveIRCommand(final REVRobot rev, final byte irCommand, final byte rxSensor) {
		getFragmentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(!rev.isDead()) {
					Log.d(getClass().getName(), "Get shot rev: " + rev.getName() + ", sensor = " + rxSensor + ", irCommand = " + irCommand);
					float remainHealthValue = Player.getInstance().getShot(rev, irCommand, getActivity());
					tvHealth.setText(getString(R.string.health) + " " + remainHealthValue);
				} else {
					tvHealth.setText(getString(R.string.dead));
				}
			}
		});
	}

	@Override
	public void revDeviceDisconnected(REVRobot rev) {
		// Clear connected rev
		REVRobotFinder.getInstance().clearFoundREVList();
		FragmentActivity activity = getFragmentActivity();
		if (activity != null) {
			FragmentManager mgr = activity.getSupportFragmentManager();
			if (mgr != null) {
				// Go back to scan page
				FragmentHelper.switchFragment(activity.getSupportFragmentManager(), new ScanFragment(), R.id.view_id_content, false);
			}
		}
	}

	//================================================================================
	// Broadcast receiver for revive
	//================================================================================

	private BroadcastReceiver mBroadcast =  new BroadcastReceiver() {
		@Override
		public void onReceive(Context mContext, Intent mIntent) {
			if(mIntent.getAction().equals(BROADCAST_REVIVE)){
				// Refresh layout for revive
				tvHealth.setText(getString(R.string.health) + " " + rev.health);
			}
		}
	};
}
