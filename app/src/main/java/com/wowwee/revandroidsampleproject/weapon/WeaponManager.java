package com.wowwee.revandroidsampleproject.weapon;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wowwee.bluetoothrobotcontrollib.rev.util.GunShotData;

import android.content.Context;
import android.util.Log;
import android.widget.Spinner;

public class WeaponManager  {
	private final String WEAPON_JSON_FILE_NAME = "WeaponGunIDMap.json";

	private static WeaponManager s_instance = null;

	public static WeaponManager getInstance() {
		if (s_instance == null) {
			s_instance = new WeaponManager();
		}
		return s_instance;
	}

	public WeaponManager(){

	}

	public void Load(Context context) {
		this.loadWeaponGunIDMap(context);
	}

	private List<GunShotData> gunShotDataList;

	private void loadWeaponGunIDMap(Context context) {
		String json = null;
		try {
			// Read json file from assets folder
			InputStream is = context.getAssets().open(WEAPON_JSON_FILE_NAME);
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			json = new String(buffer, "UTF-8");
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		Gson gson = new Gson();
		TypeToken<List<GunShotData>> typetoken = new TypeToken<List<GunShotData>>(){};
		Type type = (Type) typetoken.getType();
		this.gunShotDataList = null;
		// Parse json to get gun data
		try {
			this.gunShotDataList = gson.fromJson(json, type);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public GunShotData getGunShotData(int gunShotID) {
		for (int i=0;i<this.gunShotDataList.size();i++) {
			GunShotData data = this.gunShotDataList.get(i);
			if (data.getgunShotID() == gunShotID)
				return data;
		}
		return null;
	}

}