package com.wowwee.revandroidsampleproject.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FragmentHelper {
	public static List<WeakReference<Fragment>> fragments = new ArrayList<WeakReference<Fragment>>();
	
	private static ArrayList<String> backStackKeys = new ArrayList<String>();
	private static int backStackIndex = 0;
	
	public static void switchFragment(FragmentManager fragmentManager, Fragment fragment, int containViewId, boolean addToBackStack) {
		boolean isContain = false;
		for (int i = 0; i < fragments.size(); i++){
			if (fragments.get(i).get() != null && fragments.get(i).get().getClass() == fragment.getClass()){
				isContain = true;
				break;
			}
		}
		if (!isContain) {
			fragments.add(new WeakReference<Fragment>(fragment));
		}
		
		if (fragment.getView() != null) {
			fragment.getView().setClickable(true);
		}
		
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		if (addToBackStack) {
			backStackIndex++;
			String key = "" + backStackIndex;
			transaction.addToBackStack(key);
			backStackKeys.add(key);
		}
		transaction.replace(containViewId, fragment, ""+backStackIndex);
		transaction.commitAllowingStateLoss();
	}

	public static void removeFragment(FragmentManager fragmentManager, int containViewId) {
		if(fragmentManager != null) {
			Fragment fragment = fragmentManager.findFragmentById(containViewId);
			if (fragment != null) {
				fragments.remove(fragment);
				FragmentTransaction transaction = fragmentManager.beginTransaction();
				transaction.remove(fragment);
				transaction.commitAllowingStateLoss();
			}
		}
	}
}
