/**
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.biometrics;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.provider.Settings;
import android.os.Process;
import com.android.systemui.R;

import java.util.List;

public class FODAnimation extends ImageView {

    private final WindowManager.LayoutParams mAnimParams = new WindowManager.LayoutParams();

    private boolean mShowing = false;
    private Context mContext;
    private int mAnimationSize;
    private int mAnimationOffset;
    private int mAnimationPositionY;
    private AnimationDrawable recognizingAnim;
    private WindowManager mWindowManager;
    private boolean mIsKeyguard;

    private int mSelectedAnim;
    private TypedArray mAnimationStyles;
    private int mAnimationStylesCount;
    private boolean mIsDreaming;
    private boolean mKeyguardAnim;

    public FODAnimation(Context context, int mPositionX, int mPositionY) {
        super(context);

        mContext = context;
        mWindowManager = mContext.getSystemService(WindowManager.class);
        mAnimationSize = mContext.getResources().getDimensionPixelSize(R.dimen.fod_animation_size);
        mAnimationOffset = mContext.getResources().getDimensionPixelSize(R.dimen.fod_animation_offset);
        mAnimParams.height = mAnimationSize;
        mAnimParams.width = mAnimationSize;

        mAnimParams.format = PixelFormat.TRANSLUCENT;
        mAnimParams.type = WindowManager.LayoutParams.TYPE_VOLUME_OVERLAY; // it must be behind FOD icon
        mAnimParams.flags =  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mAnimParams.gravity = Gravity.TOP | Gravity.CENTER;
        mAnimParams.y = mPositionY - (mAnimationSize / 2) + mAnimationOffset;

        setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        update();
        setVisibility(View.GONE);

        mWindowManager.addView(this, mAnimParams);
    }

    public void update() {
        mAnimationStyles = mContext.getResources().obtainTypedArray(R.array.fod_animation_resources);
        mAnimationStylesCount = mAnimationStyles.length();
        if (mAnimationStylesCount > 0) {
            mSelectedAnim = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.FOD_ANIM, 29);
            setBackgroundResource(mAnimationStyles.getResourceId(mSelectedAnim, -1));
        } else {
            setBackgroundResource(R.drawable.fod_miui_pulse_recognizing_white_anim);
        }
        recognizingAnim = (AnimationDrawable) getBackground();
    }

    public void updateParams(int mDreamingOffsetY) {
        mAnimParams.y = mDreamingOffsetY - (mAnimationSize / 2) + mAnimationOffset;
        mWindowManager.updateViewLayout(this, mAnimParams);
    }

    public void setAnimationKeyguard(boolean state) {
        mIsKeyguard = state;
    }

    public void setDreamingAnimation(boolean dreaming) {
         mIsDreaming = dreaming;
    }

    public void showFODanimation() {
        boolean isenabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.FOD_RECOGNIZING_ANIMATION, 0) != 0;
        if (!isenabled) {
            return;
        }
        mKeyguardAnim = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.FOD_ANIM_KEYGUARD, 0) == 1;
        if (!mIsKeyguard) {
            checkapps();
        }
        boolean allow = mKeyguardAnim || mIsKeyguard;
        if (mAnimParams != null && !mShowing && allow) {
            mShowing = true;
            setVisibility(View.VISIBLE);
            recognizingAnim.start();
        }
    }

    public void checkapps() {
        if (!mKeyguardAnim) return;
        try {
               IActivityManager am = ActivityManagerNative.getDefault();
               String pkgName;
               List<RunningAppProcessInfo> apps = am.getRunningAppProcesses();
               for (RunningAppProcessInfo appInfo : apps) {
                    int uid = appInfo.uid;
                    if (uid >= Process.FIRST_APPLICATION_UID
                       && uid <= Process.LAST_APPLICATION_UID
                       && appInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        if (appInfo.pkgList != null
                           && (appInfo.pkgList.length > 0)) {
                           for (String pkg : appInfo.pkgList) {
                                if (pkg.equals("net.one97.paytm")
                                    || pkg.equals("com.google.android.apps.nbu.paisa.user")
                                    || pkg.equals("com.anz.android.gomoney")
                                    || pkg.equals("com.sec.android.app.sbrowser")
                                    || pkg.equals("com.sec.android.app.sbrowser.beta")
                                    || pkg.equals("com.google.android.gms")
                                    || pkg.equals("com.dashlane")
                                    || pkg.equals("com.google.android.apps.walletnfcrel")) {
                                    mKeyguardAnim = false;
                                }
                            }
                        }
                    }
               }
        } catch (Exception e) {
          //do nothing
        }
    }

    public void hideFODanimation() {
        if (mShowing) {
            mShowing = false;
            if (recognizingAnim != null) {
                clearAnimation();
                recognizingAnim.stop();
                recognizingAnim.selectDrawable(0);
            }
            setVisibility(View.GONE);
        }
    }
}
