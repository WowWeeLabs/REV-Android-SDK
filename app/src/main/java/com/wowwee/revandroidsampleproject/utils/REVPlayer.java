package com.wowwee.revandroidsampleproject.utils;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;

public class REVPlayer {

	// Singleton
	private static REVPlayer instance = null;
	
	private REVRobot playerRev = null;

	//================================================================================
    // Singleton
    //================================================================================
	
	public static REVPlayer getInstance(){
		if (instance == null) {
			instance = new REVPlayer();
		}
		return instance;
	}
	
	//================================================================================
    // Constructor
    //================================================================================
	
	public REVPlayer() {
		super();
	}
	
	//================================================================================
    // Setter / Getter
    //================================================================================
	
	public void setPlayerRev(REVRobot rev) {
		playerRev = rev;
	}
	
	public REVRobot getPlayerRev() {
		return playerRev;
	}
}
