package com.wowwee.revandroidsampleproject.ai;

import com.wowwee.bluetoothrobotcontrollib.rev.REVRobotConstant.revRobotTrackingMode;
import com.wowwee.revandroidsampleproject.utils.Player;

/**
 * Created by yinlau on 15/2/16.
 */
public class AIAssistant {

    private static AIAssistant instance = null;

    private boolean aiGaming;

    //================================================================================
    // Singleton
    //================================================================================

    public static AIAssistant getInstance() {
        if (instance == null) {
            instance = new AIAssistant();
        }
        return instance;
    }


    public void aiGotShot(float healthRemain) {
        if (!aiGaming) {
            return;
        }
        if(healthRemain > 0.5) {
            Player.getInstance().aiChangeMode(revRobotTrackingMode.REVTrackingChase);
        }else if(healthRemain > 0.3){
            Player.getInstance().aiChangeMode(revRobotTrackingMode.REVTrackingCircle);
        }else{
            Player.getInstance().aiChangeMode(revRobotTrackingMode.REVTrackingAvoid);
        }
    }

    public void startAI() {
        Player.getInstance().aiChangeMode(revRobotTrackingMode.REVTrackingChase);
        aiGaming = true;
    }


    public void stopAI() {
        Player.getInstance().aiChangeMode(revRobotTrackingMode.REVTrackingUserControl);
        aiGaming = false;
    }

    public boolean isAiGaming() {
        return aiGaming;
    }

}
