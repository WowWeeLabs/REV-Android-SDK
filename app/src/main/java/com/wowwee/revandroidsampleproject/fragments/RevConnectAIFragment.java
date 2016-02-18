package com.wowwee.revandroidsampleproject.fragments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot.REVType;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotFinder;
import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.ai.AIPlayer;
import com.wowwee.revandroidsampleproject.ai.AIPlayerManager;
import com.wowwee.revandroidsampleproject.fragments.RevAIPlayerFragment.RevAIPlayerFragmentListener;
import com.wowwee.revandroidsampleproject.fragments.RevAIPlayerFragment.SelectedCarListener;
import com.wowwee.revandroidsampleproject.utils.AICarListAdapter;
import com.wowwee.revandroidsampleproject.utils.REVPlayer;

public class RevConnectAIFragment extends BaseViewFragment implements RevAIPlayerFragmentListener, SelectedCarListener{

	private AICarListAdapter listAdapter;
	private ListView aiCarListView;

	private List<SelectAICar> selectedCarList;
	private ArrayList<REVRobot> connectedRevList = new ArrayList<REVRobot>();
	private int numOfSelectedCars;
	private RevConnectAICallback revConnectAICallback = null;
	
	private ProgressDialog loading;

	private Button connectBtn;
	private Button disconnectBtn;
	private View view;
	private Handler handler;
	private Handler connectAIHandler;
	private Runnable connectAIRunnable;
	private int retryTime;

	public enum ConnectAIMode {BATTLE_MODE, CAMPAIGN_MODE};
	public ConnectAIMode connectAIMode = ConnectAIMode.BATTLE_MODE;
	
	//================================================================================
    // Interface
    //================================================================================
	public interface RevConnectAICallback {
	    public void revConnectDidConnectRev(RevConnectAIFragment sender, List<REVRobot> revList);
	}
	
	//================================================================================
    // Constructor
    //================================================================================
	public RevConnectAIFragment() {
		super(R.layout.fragment_connect_ai);
		this.connectAIMode = ConnectAIMode.BATTLE_MODE;
	}
	
	public RevConnectAIFragment(ConnectAIMode connectAIMode) {
		super(R.layout.fragment_connect_ai);
		this.connectAIMode = connectAIMode;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		view = super.onCreateView(inflater, container, savedInstanceState);
		//block touch from root view
		view.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				return true;
			}
		});	
		
		//block touch from root view
		view.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				return true;
			}
		});
		
		selectedCarList = new ArrayList<SelectAICar>();
		handler = new Handler();
		connectAIHandler = new Handler();

		// Define close button
		Button closeBtn = (Button)view.findViewById(R.id.btn_close_connect_ai);
		closeBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if (revConnectAIFragmentListener != null){
					revConnectAIFragmentListener.exitREVConnectAIView();
				}
				FragmentHelper.removeFragment(getFragmentActivity().getSupportFragmentManager(), R.id.view_id_sub_overlay2);
			}
		});

		// Define connection button
		connectBtn = (Button)view.findViewById(R.id.btn_confirm_connect_ai);
		connectBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// Connect to all selected cars
				numOfSelectedCars = 0;
				connectedRevList.clear();
				for (SelectAICar car : selectedCarList) {
					if (car.isSelected()) {
						numOfSelectedCars++;
					}
				}
				if (numOfSelectedCars > 0) {
					// Default disable connection button
					getFragmentActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							connectBtn.setClickable(false);
							connectBtn.setEnabled(false);
							connectBtn.setFocusable(false);
							connectRev();
						}
					});

					// Show progress dialog for AI connection
					loading = ProgressDialog.show(getFragmentActivity(), "", "Connecting", true);
					if (connectAIRunnable == null) {
						connectAIRunnable = new Runnable() {

							@Override
							public void run() {
								if (loading != null && loading.isShowing()) {
									//reconnect again
									if (retryTime < 3) {
										retryTime++;
										connectRev();
										connectAIHandler.postDelayed(connectAIRunnable, 5000);
									} else {
										loading.dismiss();
										connectBtn.setClickable(true);
										connectBtn.setEnabled(true);
										connectBtn.setFocusable(true);
									}
								}
							}
						};
					}
					connectAIHandler.postDelayed(connectAIRunnable, 5000);

				}
			}
		});
		connectBtn.setEnabled(false);

		// Define disconnect button
		disconnectBtn = (Button)view.findViewById(R.id.btn_confirm_disconnect_ai);
		if(AIPlayerManager.getInstance().getRevAIMapping().size() > 0 &&
				REVRobotFinder.getInstance().getmRevRobotConnectedList().size() > 1) {
			connectBtn.setVisibility(View.GONE);
			disconnectBtn.setVisibility(View.VISIBLE);
			disconnectBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					disConnectRev();
				}
			});
		} else {
			connectBtn.setVisibility(View.VISIBLE);
			disconnectBtn.setVisibility(View.GONE);
		}

		// Define AI car listview
		aiCarListView = (ListView)view.findViewById(R.id.listview_ai_car);
		view.post(new Runnable() {
		    @Override
		    public void run() {
		    	listAdapter = new AICarListAdapter(RevConnectAIFragment.this.view.getContext(), RevConnectAIFragment.this, connectAIMode);
		    	aiCarListView.setAdapter(listAdapter);
		    	aiCarListView.setOnItemClickListener(listAdapter);
		    }
		});
		
		return view;
	}

	private void disConnectRev() {
		// Detach AI car
		REVRobot aiRev = AIPlayer.getRev();
		AIPlayerManager.getInstance().revDetachAI(aiRev);
		aiRev.setCallbackInterface(RevConnectAIFragment.this);
		aiRev.disconnect();
		// Update button layout
		connectBtn.setVisibility(View.VISIBLE);
		disconnectBtn.setVisibility(View.GONE);
	}

	private void connectRev(){
		for(final SelectAICar car : selectedCarList) {
			if(car.isSelected() && !car.isConnected()) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						REVRobot revToConnect = car.getRevRobot();
						revToConnect.setCallbackInterface(RevConnectAIFragment.this);
						revToConnect.connect();
					}
				});
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	//================================================================================
    // Setter / Getter
    //================================================================================
	public void setRevConnectAICallback(RevConnectAICallback callback) {
		revConnectAICallback = callback;
	}
	
	//================================================================================
    // REVRobot callback
    //================================================================================
	
	@Override
	public void revDeviceReady(REVRobot rev) {
		connectedRevList.add(rev);
		final REVRobot robot = rev;
		numOfSelectedCars--;
		for(SelectAICar car : selectedCarList) {
			if(car.isSelected() && car.getRevRobot() == rev) {
				car.setConnected(true);
				break;
			}
		}
		if(numOfSelectedCars == 0) {
			// All cars connected. Go back now.
			if(revConnectAICallback != null) {
				revConnectAICallback.revConnectDidConnectRev(this, connectedRevList);
			}
			
			if(loading != null) {
				loading.dismiss();
			}
			if (connectAIHandler != null && connectAIRunnable != null){
				connectAIHandler.removeCallbacks(connectAIRunnable);
			}
			getFragmentActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (revConnectAIFragmentListener != null){
						revConnectAIFragmentListener.exitREVConnectAIView();
					}
					FragmentHelper.removeFragment(getFragmentActivity().getSupportFragmentManager(), R.id.view_id_sub_overlay2);
				}
			});
		}
	}
	
	//================================================================================
    // REVRobotFinder broadcast receiver
    //================================================================================

	private final BroadcastReceiver mRevFinderBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	final String action = intent.getAction();
        	if (REVRobotFinder.REVRobotFinder_REVFound.equals(action)){
        		List<REVRobot> revFoundList = REVRobotFinder.getInstance().getRevFoundList();
        		
        		for (REVRobot robot : revFoundList){
        			if(robot != REVPlayer.getInstance().getPlayerRev()){
        				boolean isExist = false;
        				for (SelectAICar aiCar : selectedCarList){
        					if (aiCar.getRevRobot().getBluetoothDevice().toString().equals(robot.getBluetoothDevice().toString())){
        						isExist = true;
        						break;
        					}
        				}

						try {
							Iterator<Entry<REVRobot, AIPlayer>> it = AIPlayerManager.getInstance().getRevAIMapping().entrySet().iterator();
							while (it.hasNext()){
                                Entry<REVRobot, AIPlayer> entry = it.next();

                                if (entry.getKey() == robot){
                                    isExist = true;
                                    break;
                                }
                            }
						} catch (Exception e) {
							e.printStackTrace();
						}

						if (isExist){
        					continue;
        				}else{
        					selectedCarList.add(new SelectAICar(robot));
        				}
        			}
        		}
        		
        		if (selectedCarList.size() > 0){
	        		listAdapter.setCarList(selectedCarList);
	        		listAdapter.notifyDataSetChanged();
        		}
        	}
        }
	};
	
	@Override
	public void onResume() {
		super.onResume();
		
		getFragmentActivity().registerReceiver(mRevFinderBroadcastReceiver, REVRobotFinder.getRevRobotFinderIntentFilter());
		
		// Start scan for ai cars
		REVRobotFinder.getInstance().stopScanForREV();
		REVRobotFinder.getInstance().stopScanForREVContinuous();
		REVRobotFinder.getInstance().scanForREVContinuous();
		REVRobotFinder.getInstance().clearFoundREVList();
		
		getView().setFocusableInTouchMode(true);
		getView().requestFocus();
		getView().setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
					if (revConnectAIFragmentListener != null) {
						revConnectAIFragmentListener.exitREVConnectAIView();
					}

					FragmentHelper.removeFragment(getFragmentActivity().getSupportFragmentManager(), R.id.view_id_sub_overlay2);
					return true;
				}
				return false;
			}
		});
	}
	
	@Override
	public void onPause() {
		super.onPause();

		// Stop scan for ai cars
		REVRobotFinder.getInstance().stopScanForREV();
		REVRobotFinder.getInstance().stopScanForREVContinuous();
		
		getFragmentActivity().unregisterReceiver(mRevFinderBroadcastReceiver);
	}
	
	public class SelectAICar{

		REVRobot revRobot;
		boolean selected;
		boolean isConnected;
		
		SelectAICar(REVRobot robot){
			this.revRobot = robot;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}

		public REVRobot getRevRobot() {
			return revRobot;
		}

		public void setRevRobot(REVRobot revRobot) {
			this.revRobot = revRobot;
		}

		public boolean isConnected() {
			return isConnected;
		}

		public void setConnected(boolean isConnected) {
			this.isConnected = isConnected;
		}

	}

	//================================================================================
	// RevAIPlayerFragmentListener callback
	//================================================================================

	@Override
	public void backPage() {
		
	}
	@Override
	public void selectAIPlayer(SelectAICar aiCar, AIPlayer ai) {
		AIPlayerManager.getInstance().revAttachAI(aiCar.getRevRobot(), ai);
		aiCar.setSelected(true);
		listAdapter.notifyDataSetChanged();
	}
	@Override
	public void selectAIPlayer(REVRobot rev, AIPlayer ai) {
		
	}
	@Override
	public void disconnectAICar(REVRobot aiRev) {
		
	}

	//================================================================================
	// SelectedCarListener callback
	//================================================================================

	@Override
	public void selected() {
		connectBtn.setClickable(true);
		connectBtn.setEnabled(true);
	}

	@Override
	public void deselected() {
		connectBtn.setEnabled(false);
	}

	//================================================================================
	// REVConnectAIFragmentListener
	//================================================================================

	public REVConnectAIFragmentListener revConnectAIFragmentListener;
	public interface REVConnectAIFragmentListener{
		public void exitREVConnectAIView();
	}

	public void setRevConnectAIFragmentListener(
			REVConnectAIFragmentListener revConnectAIFragmentListener) {
		this.revConnectAIFragmentListener = revConnectAIFragmentListener;
	}
}
