package com.wowwee.revandroidsampleproject.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;

import android.content.Context;


public class AIPlayerManager {
	
	// Singleton
	private static AIPlayerManager instance = null;
	
	// AI players
	private AIPlayer aiPlayer;
	
	// AIPlayer mapping
	private HashMap<REVRobot, AIPlayer> revAIMapping = new HashMap<REVRobot, AIPlayer>();
	
	public static final String AI_CHOICE = "ai_01_apex01.json";
	
	//================================================================================
    // Singleton
    //================================================================================
	
	public static AIPlayerManager createInstance(Context context){
		if (instance == null) {
			instance = new AIPlayerManager(context);
		}
		return instance;
	}
	
	public static AIPlayerManager getInstance() {
		return instance;
	}
	
	//================================================================================
    // Constructor
    //================================================================================
	
	public AIPlayerManager(Context context) {
		super();
		aiPlayer = new AIPlayer(AI_CHOICE, context);
	}
	
	//================================================================================
    // Getter / Setter
    //================================================================================
	public AIPlayer getAIPlayer() {
		return aiPlayer;
	}
	
	//================================================================================
    // AI Mapping
    //================================================================================
	public void revAttachAI(REVRobot rev, AIPlayer ai) {
		revAIMapping.put(rev, ai);
		ai.setRev(rev);
	}
	
	public void revDetachAI(REVRobot rev) {
		revAIMapping.remove(rev);
	}
	
	public void clearAIMapping() {
		revAIMapping.clear();
	}
	
	public AIPlayer getAIPlayer(REVRobot rev) {
		return revAIMapping.get(rev);
	}

	public HashMap<REVRobot, AIPlayer> getRevAIMapping() {
		return revAIMapping;
	}

}
