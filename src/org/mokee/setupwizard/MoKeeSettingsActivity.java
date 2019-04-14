/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017-2019 The LineageOS Project
 * Copyright (C) 2017-2019 The MoKee Open Source Project
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

package org.mokee.setupwizard;

import static org.mokee.setupwizard.SetupWizardApp.DISABLE_NAV_KEYS;
import static org.mokee.setupwizard.SetupWizardApp.KEY_BOTTOM_GESTURE_NAV;
import static org.mokee.setupwizard.SetupWizardApp.KEY_PRIVACY_GUARD;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.IWindowManager;
import android.view.View;
import android.view.WindowManagerGlobal;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.setupwizardlib.util.WizardManagerHelper;

import org.mokee.setupwizard.R;

import mokee.hardware.MKHardwareManager;
import mokee.providers.MKSettings;

public class MoKeeSettingsActivity extends BaseSetupWizardActivity {

    public static final String TAG = MoKeeSettingsActivity.class.getSimpleName();

    public static final String PRIVACY_POLICY_URI = "https://www.mokeedev.com/en/legal";

    private SetupWizardApp mSetupWizardApp;

    private CheckBox mNavKeys;
    private CheckBox mBottomGestureNav;
    private CheckBox mPrivacyGuard;

    private View navKeysRow;
    private View bottomGestureNavRow;

    private boolean mSupportsKeyDisabler = false;

    private View.OnClickListener mNavKeysClickListener = view -> {
        boolean checked = !mNavKeys.isChecked();
        mNavKeys.setChecked(checked);
        bottomGestureNavRow.setEnabled(!checked);
        mSetupWizardApp.getSettingsBundle().putBoolean(DISABLE_NAV_KEYS, checked);
    };

    private View.OnClickListener mBottomGestureNavClickListener = view -> {
        boolean checked = !mBottomGestureNav.isChecked();
        mBottomGestureNav.setChecked(checked);
        navKeysRow.setEnabled(!checked);
        mSetupWizardApp.getSettingsBundle().putBoolean(KEY_BOTTOM_GESTURE_NAV, checked);
    };

    private View.OnClickListener mPrivacyGuardClickListener = view -> {
        boolean checked = !mPrivacyGuard.isChecked();
        mPrivacyGuard.setChecked(checked);
        mSetupWizardApp.getSettingsBundle().putBoolean(KEY_PRIVACY_GUARD, checked);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSetupWizardApp = (SetupWizardApp) getApplication();
        setNextText(R.string.next);
        String policySummary = getString(R.string.services_explanation);
        String privacy_policy = getString(R.string.services_privacy_policy);
        int spanStart = policySummary.indexOf("%s");
        int spanEnd = spanStart + privacy_policy.length();
        policySummary = policySummary.replace("%s", privacy_policy);
        SpannableString ss = new SpannableString(policySummary);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                // At this point of the setup, the device has already been unlocked (if frp
                // had been enabled), so there should be no issues regarding security
                final Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(PRIVACY_POLICY_URI));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Unable to start activity " + intent.toString(), e);
                }
            }
        };
        ss.setSpan(clickableSpan,
                spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView privacyPolicy = (TextView) findViewById(R.id.privacy_policy);
        privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
        privacyPolicy.setText(ss);

        navKeysRow = findViewById(R.id.nav_keys);
        navKeysRow.setOnClickListener(mNavKeysClickListener);
        mNavKeys = (CheckBox) findViewById(R.id.nav_keys_checkbox);
        mSupportsKeyDisabler = isKeyDisablerSupported(this);
        if (mSupportsKeyDisabler) {
            mNavKeys.setChecked(MKSettings.System.getIntForUser(getContentResolver(),
                    MKSettings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0);
        } else {
            navKeysRow.setVisibility(View.GONE);
        }

        bottomGestureNavRow = findViewById(R.id.bottom_gesture_nav);
        bottomGestureNavRow.setOnClickListener(mBottomGestureNavClickListener);
        mBottomGestureNav = (CheckBox) findViewById(R.id.bottom_gesture_nav_checkbox);
        TextView bottomGestureNavSummary = findViewById(R.id.bottom_gesture_nav_summary);
        boolean hasNavigationBar = true;
        try {
            IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
            hasNavigationBar = windowManager.hasNavigationBar();
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }
        if (!hasNavigationBar) {
            if (mSupportsKeyDisabler) {
                bottomGestureNavSummary.setText(R.string.services_os_bottom_gesture_nav_keys);
            } else {
                bottomGestureNavSummary.setText(R.string.services_os_bottom_gesture);
            }
        } else {
            bottomGestureNavSummary.setText(R.string.services_os_bottom_gesture_nav_bar);
        }
        mBottomGestureNav.setChecked(MKSettings.System.getIntForUser(getContentResolver(),
                MKSettings.System.USE_BOTTOM_GESTURE_NAVIGATION, 0, UserHandle.USER_CURRENT) != 0);

        View privacyGuardRow = findViewById(R.id.privacy_guard);
        privacyGuardRow.setOnClickListener(mPrivacyGuardClickListener);
        mPrivacyGuard = (CheckBox) findViewById(R.id.privacy_guard_checkbox);
        mPrivacyGuard.setChecked(MKSettings.Secure.getInt(getContentResolver(),
                MKSettings.Secure.PRIVACY_GUARD_DEFAULT, 0) == 1);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDisableNavkeysOption();
        updatePrivacyGuardOption();
    }

    @Override
    public void onNavigateBack() {
        onBackPressed();
    }

    @Override
    public void onNavigateNext() {
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), Activity.RESULT_OK);
        startActivityForResult(intent, 1);
    }

    @Override
    protected int getTransition() {
        return TRANSITION_ID_SLIDE;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_mokee_settings;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_services;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_features;
    }

    private void updateDisableNavkeysOption() {
        if (mSupportsKeyDisabler) {
            final Bundle myPageBundle = mSetupWizardApp.getSettingsBundle();
            boolean enabled = MKSettings.System.getIntForUser(getContentResolver(),
                    MKSettings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;
            boolean checked = myPageBundle.containsKey(DISABLE_NAV_KEYS) ?
                    myPageBundle.getBoolean(DISABLE_NAV_KEYS) :
                    enabled;
            mNavKeys.setChecked(checked);
            myPageBundle.putBoolean(DISABLE_NAV_KEYS, checked);
        }
    }

    private void updatePrivacyGuardOption() {
        final Bundle bundle = mSetupWizardApp.getSettingsBundle();
        boolean enabled = MKSettings.Secure.getInt(getContentResolver(),
                MKSettings.Secure.PRIVACY_GUARD_DEFAULT, 0) != 0;
        boolean checked = bundle.containsKey(KEY_PRIVACY_GUARD) ?
                bundle.getBoolean(KEY_PRIVACY_GUARD) :
                enabled;
        mPrivacyGuard.setChecked(checked);
        bundle.putBoolean(KEY_PRIVACY_GUARD, checked);
    }

    private static boolean isKeyDisablerSupported(Context context) {
        final MKHardwareManager hardware = MKHardwareManager.getInstance(context);
        return hardware.isSupported(MKHardwareManager.FEATURE_KEY_DISABLE);
    }
}
