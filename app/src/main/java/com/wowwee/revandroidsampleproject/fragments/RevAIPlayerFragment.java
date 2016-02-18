package com.wowwee.revandroidsampleproject.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.ai.AIPlayer;
import com.wowwee.revandroidsampleproject.ai.AIPlayerManager;
import com.wowwee.revandroidsampleproject.fragments.RevConnectAIFragment.SelectAICar;
import com.wowwee.revandroidsampleproject.fragments.BaseViewFragment;

public class RevAIPlayerFragment extends BaseViewFragment {

	private SelectAICar aiCar;
	private View view;
	private AIPlayer aiPlayer;
	
	public RevAIPlayerFragment(SelectAICar aiCar) {
		super(R.layout.fragment_connect_aiplayer);
		this.aiCar = aiCar;
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

		// Get AI player
		aiPlayer = AIPlayerManager.getInstance().getAIPlayer();

		// Close button
		Button closeBtn = (Button)view.findViewById(R.id.btn_close_aiplayer);
		closeBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (revAIPlayerFragmentListener != null){
					revAIPlayerFragmentListener.backPage();
				}
				
				FragmentHelper.removeFragment(getFragmentActivity().getSupportFragmentManager(), R.id.view_id_sub_overlay3);
			}
		});
		
		// Confirm button
		Button confirmBtn = (Button)view.findViewById(R.id.btn_confirm_connect_ai);
		confirmBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (revAIPlayerFragmentListener != null){
					if (aiCar != null){
						//create a new ai reference
						AIPlayer ai = aiPlayer;
						revAIPlayerFragmentListener.selectAIPlayer(aiCar, new AIPlayer(ai.getJsonFile(), RevAIPlayerFragment.getFragmentActivity()));
						revAIPlayerFragmentListener.backPage();
						// Enable connect button in connection page
						if (selectedCarListener != null){
							selectedCarListener.selected();
						}
						FragmentHelper.removeFragment(getFragmentActivity().getSupportFragmentManager(), R.id.view_id_sub_overlay3);
					}else{
						AIPlayer currentAI = AIPlayerManager.getInstance().getRevAIMapping().get(rev);
						if (currentAI == null)
							return;
						if (currentAI.getDriverID() == aiPlayer.getDriverID()){
							//already using, no need reset
							revAIPlayerFragmentListener.backPage();
							// Back to game page
							FragmentHelper.removeFragment(getFragmentActivity().getSupportFragmentManager(), R.id.view_id_sub_overlay3);
						}
					}
				}
			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		getView().setFocusableInTouchMode(true);
		getView().requestFocus();
		getView().setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){
					// Back action
					if (revAIPlayerFragmentListener != null){
						revAIPlayerFragmentListener.backPage();
					}
					// Back to game page
					FragmentHelper.removeFragment(getFragmentActivity().getSupportFragmentManager(), R.id.view_id_sub_overlay3);
					return true;
				}
				return false;
			}
		});
	}

	public void deselectCar() {
		if (selectedCarListener != null){
			selectedCarListener.deselected();
		}
		if (RevAIPlayerFragment.this.rev != null){
			if (revAIPlayerFragmentListener != null){
				revAIPlayerFragmentListener.disconnectAICar(rev);
			}
		}
		if (RevAIPlayerFragment.this.aiCar != null){
			if (revAIPlayerFragmentListener != null){
				revAIPlayerFragmentListener.disconnectAICar(RevAIPlayerFragment.this.aiCar.revRobot);
			}
		}
	}

	//================================================================================
	// RevAIPlayerFragmentListener
	//================================================================================

	public RevAIPlayerFragmentListener revAIPlayerFragmentListener;
	public interface RevAIPlayerFragmentListener{
		public void selectAIPlayer(SelectAICar aiCar, AIPlayer ai);
		public void backPage();
		public void disconnectAICar(REVRobot aiRev);
		public void selectAIPlayer(REVRobot rev, AIPlayer ai);
	}
	public void setRevAIPlayerFragmentListener(RevAIPlayerFragmentListener revAIPlayerFragmentListener) {
		this.revAIPlayerFragmentListener = revAIPlayerFragmentListener;
	}

	//================================================================================
	// SelectedCarListener
	//================================================================================

	public SelectedCarListener selectedCarListener;
	public interface SelectedCarListener{
		public void selected();
		public void deselected();
	}

	public void setSelectedCarListener(SelectedCarListener selectedCarListener) {
		this.selectedCarListener = selectedCarListener;
	}

}
