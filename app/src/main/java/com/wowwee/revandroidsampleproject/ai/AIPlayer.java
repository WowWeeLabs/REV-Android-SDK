package com.wowwee.revandroidsampleproject.ai;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;

public class AIPlayer {

	private int driverID;
	private String name;
	private static REVRobot rev;
	
	String jsonFile;
	
	//================================================================================
    // Constructor
    //================================================================================
	public AIPlayer(String jsonFile, Context context) {
		super();
		configAIPlayer(jsonFile, context);
	}
	
	public AIPlayer() {
		super();
	}
	
	public void configAIPlayer(String jsonFile, Context context) {
		this.jsonFile = jsonFile;
		String json = null;
	    try {
	        InputStream is = context.getAssets().open(jsonFile);
	        int size = is.available();
	        byte[] buffer = new byte[size];
	        is.read(buffer);
	        is.close();
	        json = new String(buffer, "UTF-8");
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    }
	    
	    try {
	    	Log.d("AI", "json = " + json);
	        JSONObject obj = new JSONObject(json);
	        driverID = obj.getInt("driverID");
	        name = obj.getString("name");
	    } catch (JSONException e) {
	        e.printStackTrace();
	    }
	}
	
	//================================================================================
    // Getter / Setter
    //================================================================================
	public int getDriverID() {
		return driverID;
	}

	public void setDriverID(int driverID) {
		this.driverID = driverID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static REVRobot getRev() {
		return rev;
	}

	public static void setRev(REVRobot r) {
		rev = r;
	}

	public String getJsonFile() {
		return jsonFile;
	}

	public void setJsonFile(String jsonFile) {
		this.jsonFile = jsonFile;
	}

}
