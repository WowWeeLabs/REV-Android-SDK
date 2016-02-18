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
import android.os.Handler;
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

import java.util.List;
import java.util.Timer;

import com.wowwee.revandroidsampleproject.ai.AIPlayer;
import com.wowwee.revandroidsampleproject.ai.AIPlayerManager;
import com.wowwee.revandroidsampleproject.data.JoystickData;
import com.wowwee.revandroidsampleproject.data.JoystickData.TYPE;
import com.wowwee.revandroidsampleproject.drawer.JoystickDrawer;
import com.wowwee.revandroidsampleproject.utils.JoystickView;
import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.utils.Player;
import com.wowwee.revandroidsampleproject.utils.REVPlayer;
import com.wowwee.revandroidsampleproject.weapon.WeaponManager;
import com.wowwee.revandroidsampleproject.fragments.RevConnectAIFragment.SelectAICar;
import com.wowwee.revandroidsampleproject.fragments.RevAIPlayerFragment.RevAIPlayerFragmentListener;
import com.wowwee.revandroidsampleproject.fragments.RevConnectAIFragment.RevConnectAICallback;
import com.wowwee.revandroidsampleproject.fragments.RevConnectAIFragment.REVConnectAIFragmentListener;

@SuppressLint("ValidFragment")
public class DriveViewFragment extends BaseViewFragment implements OnTouchListener, REVConnectAIFragmentListener, RevConnectAICallback, RevAIPlayerFragmentListener {
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

	private REVRobot revInControl;
	private Handler handler;

	private TextView tvHealth;
	private TextView tvAiHealth;
	private Button btnAI;
	private Button btnFire;
	private Spinner spTrackingMode;
	private Spinner spGun;

	private int selectedGun = 0;

	private final float DEFAULT_DRIVE_SPEED = 1.0f;
	private final float DEFAULT_TURN_SPEED = 1.0f;
	private final float DEFAULT_DRAW_RATIO = 2.0f;

	private final int DELAY_HALF_SECOND = 500;
	private final int DELAY_ONE_SECOND = 1000;

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

		// Set car states
		resumeDriveViewFragment();

		// Start read the movement value
		if(!isJoystickTimerRunning) {
			Thread newThread = new Thread(new JoystickRunnable());
			newThread.start();
			isJoystickTimerRunning = true;
		}

		if(REVRobotFinder.getInstance().firstConnectedREV() != null) {
			// Set tracking mode for player REV
			REVRobotFinder.getInstance().firstConnectedREV().revSetTrackingMode(revRobotTrackingMode.REVTrackingUserControl);
			setDriveEnabled(true);
		} else {
			// Back to scan page if the REV is disconnected
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

		// Init AIPlayerManager
		AIPlayerManager.createInstance(getFragmentActivity());

		handler = new Handler();

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

		// Define joystick view
		this.joystickLeft = (JoystickView)view.findViewById(R.id.layoutleftJoystick);
		this.joystickLeft.UpdateLeftView();
		this.joystickLeft.setVisibility(View.INVISIBLE);
		this.joystickRight = (JoystickView)view.findViewById(R.id.layoutrightJoystick);
		this.joystickRight.UpdateRightView();
		this.joystickRight.setVisibility(View.INVISIBLE);

		// Define health text
		tvHealth = (TextView)view.findViewById(R.id.tvHealth);
		tvHealth.setText(getString(R.string.health) + " " + rev.health);

		tvAiHealth = (TextView)view.findViewById(R.id.tvAiHealth);

		// Define AI button
		btnAI = (Button)view.findViewById(R.id.btn_ai);
		btnAI.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// Pause all AI car
				Player.getInstance().setAiEnable(false);
				setViewTouchable(false);
				RevConnectAIFragment connectAIFragment = new RevConnectAIFragment();
				connectAIFragment.setRevConnectAICallback(DriveViewFragment.this);
				connectAIFragment.setRevConnectAIFragmentListener(DriveViewFragment.this);
				FragmentHelper.switchFragment(getFragmentActivity().getSupportFragmentManager(), connectAIFragment, R.id.view_id_sub_overlay2, false);
				// Hide game page layout
				getActivity().findViewById(R.id.view_id_content).setVisibility(View.INVISIBLE);
			}
		});

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

		// Set car states
		resumeDriveViewFragment();

		return view;
	}

	private void resumeDriveViewFragment() {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				revInControl = REVPlayer.getInstance().getPlayerRev();
				if (revInControl != null)
					revInControl.setCallbackInterface(DriveViewFragment.this);

				// By default set all car to Idle
				for (final REVRobot r : REVRobotFinder.getInstance().getmRevRobotConnectedList()) {
					// If this is the control car
					if (r == revInControl) {
						// If no AI car connected
						if (REVRobotFinder.getInstance().getmRevRobotConnectedList().size() == 1) {
							// Set REV state
							handler.postDelayed(new Runnable() {
								@Override
								public void run() {
									Log.d("AIState", "set revInControl to REVTrackingUserControl");
									if (r != null)
										r.revSetTrackingMode(revRobotTrackingMode.REVTrackingUserControl);
									handler.postDelayed(new Runnable() {
										@Override
										public void run() {
											if (r != null)
												r.revGetTrackingMode();
										}
									}, DELAY_HALF_SECOND);
								}
							}, DELAY_ONE_SECOND);
						}
						// If there is AI car connected
						else {
							// Set REV state
							handler.postDelayed(new Runnable() {
								@Override
								public void run() {
									if (r != null)
										r.revSetTrackingMode(revRobotTrackingMode.REVTrackingBeacon);
									handler.postDelayed(new Runnable() {
										@Override
										public void run() {
											if (r != null)
												r.revGetTrackingMode();
										}
									}, DELAY_HALF_SECOND);
								}
							}, DELAY_ONE_SECOND);
						}
					}
					r.revSetTrackingSensorStatus(false);
				}

				// Refresh all car AI
				refreshAllCarAI();
			}
		}, DELAY_ONE_SECOND);
	}

	public void setViewTouchable(boolean isTouchable) {
		if (isTouchable) {
			touchArea.setEnabled(true);
		} else {
			touchArea.setEnabled(false);
		}
	}

	private void refreshAllCarAI() {
		// Enable AI car
		for(REVRobot r : REVRobotFinder.getInstance().getmRevRobotConnectedList()) {
			if(r != revInControl) {
				AIPlayer.setRev(r);
				Player.getInstance().setAiEnable(true);
			}
		}
		// Show AI health
		getFragmentActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(REVRobotFinder.getInstance().getmRevRobotConnectedList().size() > 1 && AIPlayer.getRev() != null && tvAiHealth != null) {
					tvAiHealth.setText(getString(R.string.health) + " " + AIPlayer.getRev().health);
					tvAiHealth.setVisibility(View.VISIBLE);
				} else {
					tvAiHealth.setVisibility(View.GONE);
				}
			}
		});
	}

	private void refreshPlayerCarTrackingMode() {
		// If no AI car connected
		if(REVRobotFinder.getInstance().getmRevRobotConnectedList().size() == 1) {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					REVRobot robot = REVPlayer.getInstance().getPlayerRev();
					if (robot != null)
						robot.revSetTrackingMode(revRobotTrackingMode.REVTrackingBeacon);
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							REVRobot robot = REVPlayer.getInstance().getPlayerRev();
							if (robot != null)
								robot.revGetTrackingMode();
							handler.postDelayed(new Runnable() {
								@Override
								public void run() {
									REVRobot robot = REVPlayer.getInstance().getPlayerRev();
									if (robot != null)
										robot.revGetRGBLed();
								}
							}, DELAY_HALF_SECOND);
						}
					}, DELAY_HALF_SECOND);
				}
			}, DELAY_HALF_SECOND);
		}
		// If there is AI car connected
		else {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					REVRobot robot = REVPlayer.getInstance().getPlayerRev();
					if (robot != null)
						robot.revSetTrackingMode(revRobotTrackingMode.REVTrackingBeacon);
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							REVRobot robot = REVPlayer.getInstance().getPlayerRev();
							if (robot != null)
								robot.revGetTrackingMode();
							handler.postDelayed(new Runnable() {
								@Override
								public void run() {
									REVRobot robot = REVPlayer.getInstance().getPlayerRev();
									if (robot != null)
										robot.revGetRGBLed();
								}
							}, DELAY_HALF_SECOND);
						}
					}, DELAY_HALF_SECOND);
				}
			}, DELAY_HALF_SECOND);
		}
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
				// Show joystick
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
					// Hide joystick
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

	@Override
	public void exitREVConnectAIView() {
		setViewTouchable(true);
		// Start battle after one second
		resumeDriveViewFragment();
		// Show game page layout
		if(getActivity() != null) {
			View gameView = getActivity().findViewById(R.id.view_id_content);
			if(gameView != null) {
				gameView.setVisibility(View.VISIBLE);
			}
		}
	}

	//================================================================================
	// RevConnectAICallback
	//================================================================================
	@Override
	public void revConnectDidConnectRev(RevConnectAIFragment sender, List<REVRobot> revList) {
		for(REVRobot revRobot : revList) {
			Log.d("Connect", "Drive View Connected REV: " + revRobot.getName());
			revRobot.setCallbackInterface(DriveViewFragment.this);
		}
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
				if(rev != null) {
					if(rev == REVPlayer.getInstance().getPlayerRev()) {
						// Update player health
						if(!rev.isDead()) {
							Log.d(getClass().getName(), "Get shot rev: " + rev.getName() + ", sensor = " + rxSensor + ", irCommand = " + irCommand);
							float remainHealthValue = Player.getInstance().getShot(rev, irCommand, getActivity());
							tvHealth.setText(getString(R.string.health) + " " + remainHealthValue);
						} else {
							tvHealth.setText(getString(R.string.dead));
						}
					} else {
						// Update AI health
						tvAiHealth.setVisibility(View.VISIBLE);
						if(!rev.isDead()) {
							Log.d(getClass().getName(), "Get shot rev: " + rev.getName() + ", sensor = " + rxSensor + ", irCommand = " + irCommand);
							float remainHealthValue = Player.getInstance().getShot(rev, irCommand, getActivity());
							tvAiHealth.setText(getString(R.string.health) + " " + remainHealthValue);
						} else {
							tvAiHealth.setText(getString(R.string.dead));
						}
					}
				}
			}
		});
	}

	@Override
	public void revDeviceReady(REVRobot rev) {
		// Do this when the REV is connected
		refreshPlayerCarTrackingMode();
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

	@Override
	public void revDidReceiveTrackingMode(final REVRobot rev, byte mode) {
		if(REVRobotFinder.getInstance().getmRevRobotConnectedList().size() == 1) {
			if (mode != revRobotTrackingMode.REVTrackingUserControl.getValue()) {
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						rev.revSetTrackingMode(revRobotTrackingMode.REVTrackingUserControl);
						handler.postDelayed(new Runnable() {
							@Override
							public void run() {
								rev.revGetTrackingMode();
							}
						}, DELAY_HALF_SECOND);
					}
				}, DELAY_HALF_SECOND);
			}
		}
		// If there is AI car connected
		else {
			if (mode != revRobotTrackingMode.REVTrackingBeacon.getValue()) {
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						rev.revSetTrackingMode(revRobotTrackingMode.REVTrackingBeacon);
						handler.postDelayed(new Runnable() {
							@Override
							public void run() {
								rev.revGetTrackingMode();
							}
						}, DELAY_HALF_SECOND);
					}
				}, DELAY_HALF_SECOND);
			}
		}
	}

	//================================================================================
	// RevAIPlayerFragmentListener callback
	//================================================================================

	@Override
	public void backPage() {
		setViewTouchable(true);
		resumeDriveViewFragment();
	}
	@Override
	public void selectAIPlayer(REVRobot rev, AIPlayer ai) {
		AIPlayerManager.getInstance().revAttachAI(rev, ai);
	}
	@Override
	public void selectAIPlayer(SelectAICar aiCar, AIPlayer ai) {

	}

	@Override
	public void disconnectAICar(REVRobot aiRev) {
		AIPlayerManager.getInstance().revDetachAI(aiRev);
		aiRev.setCallbackInterface(this);
		aiRev.disconnect();
		setViewTouchable(true);
		resumeDriveViewFragment();
	}

	//================================================================================
	// Broadcast receiver for revive
	//================================================================================

	private BroadcastReceiver mBroadcast =  new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					final String action = intent.getAction();
					if(action.equals(BROADCAST_REVIVE)){
						// Refresh layout for revive
						if(REVPlayer.getInstance().getPlayerRev() != null) {
							tvHealth.setText(getString(R.string.health) + " " + REVPlayer.getInstance().getPlayerRev().health);
						}
						if(AIPlayer.getRev() != null) {
							tvAiHealth.setText(getString(R.string.health) + " " + AIPlayer.getRev().health);
							tvAiHealth.setVisibility(View.VISIBLE);
						} else {
							tvAiHealth.setVisibility(View.GONE);
						}
					}
				}});
		}
	};
}
