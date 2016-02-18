package com.wowwee.revandroidsampleproject.utils;

import java.util.List;

import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.ai.AIPlayer;
import com.wowwee.revandroidsampleproject.ai.AIPlayerManager;
import com.wowwee.revandroidsampleproject.fragments.FragmentHelper;
import com.wowwee.revandroidsampleproject.fragments.RevAIPlayerFragment;
import com.wowwee.revandroidsampleproject.fragments.RevConnectAIFragment;
import com.wowwee.revandroidsampleproject.fragments.RevConnectAIFragment.ConnectAIMode;
import com.wowwee.revandroidsampleproject.fragments.RevConnectAIFragment.SelectAICar;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class AICarListAdapter extends ArrayAdapter<SelectAICar> implements OnItemClickListener{

	private Context context;
	private List<SelectAICar> carList;
	private RevConnectAIFragment fragment;
	private RevAIPlayerFragment aiFragment;
	public ConnectAIMode connectAIMode;
	private ViewHolder holder;
	
	public AICarListAdapter(Context context, RevConnectAIFragment fragment, ConnectAIMode connectAIMode) {
		super(context, R.layout.list_item_ai);
		this.context = context;
		this.fragment = fragment;
		this.connectAIMode = connectAIMode;
	}

	public List<SelectAICar> getCarList() {
		return carList;
	}

	public void setCarList(List<SelectAICar> carList) {
		this.carList = carList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null){
			convertView = LayoutInflater.from(context).inflate(R.layout.list_item_ai, parent, false);
			holder = new ViewHolder();
			TextView carName = (TextView)convertView.findViewById(R.id.select_ai_car_textview);
			holder.carName = carName;
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		
		if (carList != null && position < carList.size()){
			SelectAICar aiCar = carList.get(position);

			if (aiCar.isSelected()){
				Log.d("", "selected position: " + position);
				// Selected car with red text color
				holder.carName.setTextColor(Color.RED);
			} else {
				// Deselected car with black text color
				holder.carName.setTextColor(Color.BLACK);
			}

			holder.carName.setText(aiCar.getRevRobot().getName());
		}
		return convertView;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (carList != null && position < carList.size()){
			SelectAICar aiCar = carList.get(position);
			if (aiCar.isSelected()){
				// Deselect the ai car
				aiCar.setSelected(false);
				AIPlayerManager.getInstance().revDetachAI(aiCar.getRevRobot());
				holder.carName.setTextColor(Color.BLACK);
				if(aiFragment != null) {
					aiFragment.deselectCar();
				}
			}else{
				// Set listener when select the ai car
				aiFragment = new RevAIPlayerFragment(aiCar);
				aiFragment.setRevAIPlayerFragmentListener(fragment);
				aiFragment.setSelectedCarListener(fragment);
				FragmentHelper.switchFragment(((FragmentActivity) context).getSupportFragmentManager(), aiFragment, R.id.view_id_sub_overlay3, false);
			}
		}
	}

	@Override
	public int getCount() {
		return (this.carList != null ? this.carList.size() : 0);
	}

	static class ViewHolder{
		TextView carName;
	}
}
