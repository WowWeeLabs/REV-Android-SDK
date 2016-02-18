package com.wowwee.revandroidsampleproject.utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.wowwee.bluetoothrobotcontrollib.rev.REVCommandValues;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobot;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant;
import com.wowwee.bluetoothrobotcontrollib.rev.util.GunShotData;
import com.wowwee.revandroidsampleproject.R;
import com.wowwee.revandroidsampleproject.ai.AIAssistant;
import com.wowwee.revandroidsampleproject.ai.AIPlayer;
import com.wowwee.revandroidsampleproject.fragments.DriveViewFragment;
import com.wowwee.revandroidsampleproject.weapon.WeaponManager;
import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant.revRobotTrackingMode;

/**
 * Created by yinlau on 28/1/16.
 */
public class Player {

    private static Player instance = null;
    private Handler handler;
    private final int LED_COLOR_RED = 0;
    private final int LED_COLOR_YELLOW = 1;
    private final int LED_COLOR_BLUE = 2;
    private final int DELAY_HALF_SECOND = 500;
    private final int DELAY_ONE_SECOND = 1000;

    public Player() {
        handler = new Handler(Looper.getMainLooper());
    }

    public static Player getInstance(){
        if (instance == null){
            instance = new Player();
        }
        return instance;
    }

    public void gunFire(REVRobot rev, int selectedGun) {
        if (rev != null) {
            /***
             *
             * revSendIRCommand(PARAMETER A, PARAMETER B, PARAMETER C)
             *
             * PARAMETER A: gunID
             * Use this key to find gun's name and damage level from the json file "WeaponGunIDMap.json"
             *
             * PARAMETER B: Sound
             * Define the sound for shooting. Choose the parameter with prefix "kRevSoundFile" in REVCommandValues.
             *
             * PARAMETER C: Direction
             * Define the direction for shooting. Choose "REVTXFront" for front. Choose "REVTXAll" for all.
             *
             */
            switch(selectedGun) {
                // Send fire command
                case 0:
                    rev.revSendIRCommand((byte) 0, REVCommandValues.kRevSoundFile_REV_ATTACK_GUN_1_FIRE_A34, REVRobotConstant.revTXDirection.REVTXFront);
                    break;
                case 1:
                    rev.revSendIRCommand((byte) 1, REVCommandValues.kRevSoundFile_REV_ATTACK_GUN_2_FIRE_A34, REVRobotConstant.revTXDirection.REVTXAll);
                    break;
            }
            // Flash yellow light when fire
            flashLed(rev, LED_COLOR_YELLOW);
            // Reset led to blue light
            setLed(rev, LED_COLOR_BLUE);
        }
    }

    public float getShot(REVRobot rev, byte irCommand, FragmentActivity activity) {
        float health = 0;
        if (rev != null) {
            health = rev.health;

            // Play sound
            rev.revPlaySound(REVCommandValues.kRevSoundFile_REV_DAMAGE_METAL_1_A34);
            // Flash red light when get shot
            flashLed(rev, LED_COLOR_RED);
            // Reset led to blue light
            setLed(rev, LED_COLOR_BLUE);
            // Find enemy's gun data
            GunShotData refGunShotData = WeaponManager.getInstance().getGunShotData(irCommand);
            if (refGunShotData != null) {
                GunShotData gunShotData = new GunShotData(refGunShotData);

                // Deduct health
                float deductHealthValue = gunShotData.getDamageLevel() * 1.0f;
                float remainHealthValue = Math.min(Math.max(rev.health - deductHealthValue, 0), 1);
                rev.setHealth(remainHealthValue);
                health = remainHealthValue;

                if(rev.health <= 0) {
                    // Set dead status
                    rev.setIsDead(true);
                    // Popup revive dialog
                    doLose(rev, activity);
                }

                if(rev == AIPlayer.getRev() && rev.health > 0) {
                    // AI got shot may change the tracking mode
                    AIAssistant.getInstance().aiGotShot(remainHealthValue);
                }
            }
        }
        return health;
    }

    private void flashLed(final REVRobot rev, final int color) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(color == LED_COLOR_RED) {
                    // Flash red light 3 times
                    rev.revFlashRGBLed(REVRobotConstant.revRobotColor.REVRobotColorRed, 200, 200, 3);
                } else {
                    // Flash yellow light 1 time
                    rev.revFlashRGBLed(REVRobotConstant.revRobotColor.REVRobotColorYellow, 200, 200, 1);
                }
            }
        }, DELAY_HALF_SECOND);
    }

    private void setLed(final REVRobot rev, final int color) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(color == LED_COLOR_RED) {
                    // Set led to red light
                    rev.revSetRGBLed(REVRobotConstant.revRobotColor.REVRobotColorRed);
                } else {
                    // Set led to blue light
                    rev.revSetRGBLed(REVRobotConstant.revRobotColor.REVRobotColorBlue);
                }
            }
        }, DELAY_ONE_SECOND);
    }

    public void revive(REVRobot rev, FragmentActivity activity) {
        if (rev != null) {
            // Play sound for revive
            rev.revPlaySound(REVCommandValues.kRevSoundFile_REV_CAR_REVIVE_A34);
            // Reset led to blue light
            setLed(rev, LED_COLOR_BLUE);
            // Reset health value
            rev.revHealthReset();
            // Reset layout for revive in game page
            Intent intent = new Intent();
            intent.setAction(DriveViewFragment.BROADCAST_REVIVE);
            activity.sendBroadcast(intent);
            // Start AI car
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Player.getInstance().setAiEnable(true);
                }
            }, DELAY_HALF_SECOND);
        }
    }

    private void doLose(final REVRobot rev, final FragmentActivity activity) {
        // Stop AI car
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Player.getInstance().setAiEnable(false);
            }
        }, DELAY_HALF_SECOND);
        // Show dialog
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String message;
                if(rev == REVPlayer.getInstance().getPlayerRev()) {
                    message = activity.getString(R.string.lost_title);
                } else {
                    message = activity.getString(R.string.win_title);
                }
                // Play sound
                rev.revPlaySound(REVCommandValues.kRevSoundFile_REV_DAMAGE_DEATH_1_A34);
                // Set led to red light
                setLed(rev, LED_COLOR_RED);
                // Popup dialog for revive
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setCancelable(false);
                builder.setTitle(message);
                builder.setPositiveButton(activity.getString(R.string.lost_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        revive(rev, activity);
                    }
                });
                builder.show();
            }
        }, DELAY_ONE_SECOND);
    }

    public void aiShot() {
        if(AIAssistant.getInstance().isAiGaming() && AIPlayer.getRev() != null && AIPlayer.getRev().health > 0) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 0 means AI is using Gun A
                    gunFire(AIPlayer.getRev(), 0);
                    // AI fire repeatedly
                    aiShot();
                }
            }, DELAY_HALF_SECOND);
        }
    }

    public void aiChangeMode(revRobotTrackingMode mode){
        if(AIPlayer.getRev() != null) {
            AIPlayer.getRev().revSetTrackingMode(mode);
        }
    }

    public void setAiEnable(boolean aiEnable){
        if (aiEnable) {
            AIAssistant.getInstance().startAI();
            aiShot();
        }else{
            AIAssistant.getInstance().stopAI();
        }
    }

}
