/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
 * Copyright (C) 2017 The MoKee Open Source Project
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
import static org.mokee.setupwizard.SetupWizardApp.KEY_PRIVACY_GUARD;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.android.setupwizardlib.util.WizardManagerHelper;

import org.mokee.setupwizard.R;

import mokee.hardware.MKHardwareManager;
import mokee.providers.MKSettings;

public class MoKeeSettingsActivity extends BaseSetupWizardActivity {

    public static final String TAG = MoKeeSettingsActivity.class.getSimpleName();

    public static final String PRIVACY_POLICY_URI = "http://www.mokeedev.com/en/legal";

    private SetupWizardApp mSetupWizardApp;

    private View mNavKeysRow;
    private View mPrivacyGuardRow;
    private CheckBox mNavKeys;
    private CheckBox mPrivacyGuard;

    private boolean mHideNavKeysRow = false;

    private View.OnClickListener mNavKeysClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean checked = !mNavKeys.isChecked();
            mNavKeys.setChecked(checked);
            mSetupWizardApp.getSettingsBundle().putBoolean(DISABLE_NAV_KEYS, checked);
        }
    };

    private View.OnClickListener mPrivacyGuardClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean checked = !mPrivacyGuard.isChecked();
            mPrivacyGuard.setChecked(checked);
            mSetupWizardApp.getSettingsBundle().putBoolean(KEY_PRIVACY_GUARD, checked);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSetupWizardApp = (SetupWizardApp) getApplication();
        setNextText(R.string.next);
        String privacy_policy = getString(R.string.services_privacy_policy);
        String policySummary = getString(R.string.services_explanation, privacy_policy);
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
                policySummary.length() - privacy_policy.length() - 1,
                policySummary.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView privacyPolicy = (TextView) findViewById(R.id.privacy_policy);
        privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
        privacyPolicy.setText(ss);

        mNavKeysRow = findViewById(R.id.nav_keys);
        mNavKeysRow.setOnClickListener(mNavKeysClickListener);
        mNavKeys = (CheckBox) findViewById(R.id.nav_keys_checkbox);
        boolean needsNavBar = true;
        try {
            IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
            needsNavBar = windowManager.needsNavigationBar();
        } catch (RemoteException e) {
        }
        mHideNavKeysRow = hideKeyDisabler(this);
        if (mHideNavKeysRow || needsNavBar) {
            mNavKeysRow.setVisibility(View.GONE);
        } else {
            boolean navKeysDisabled = isKeyDisablerActive(this);
            mNavKeys.setChecked(navKeysDisabled);
        }

        mPrivacyGuardRow = findViewById(R.id.privacy_guard);
        mPrivacyGuardRow.setOnClickListener(mPrivacyGuardClickListener);
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
        if (!mHideNavKeysRow) {
            final Bundle myPageBundle = mSetupWizardApp.getSettingsBundle();
            boolean enabled = MKSettings.Global.getInt(getContentResolver(),
                    MKSettings.Global.DEV_FORCE_SHOW_NAVBAR, 0) != 0;
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

    private static boolean hideKeyDisabler(Context context) {
        final MKHardwareManager hardware = MKHardwareManager.getInstance(context);
        return !hardware.isSupported(MKHardwareManager.FEATURE_KEY_DISABLE);
    }

    private static boolean isKeyDisablerActive(Context context) {
        final MKHardwareManager hardware = MKHardwareManager.getInstance(context);
        return hardware.get(MKHardwareManager.FEATURE_KEY_DISABLE);
    }
}
