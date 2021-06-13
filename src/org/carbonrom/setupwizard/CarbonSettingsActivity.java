/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017-2019 The LineageOS Project
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

package org.carbonrom.setupwizard;

import static org.carbonrom.setupwizard.SetupWizardApp.DISABLE_NAV_KEYS;
import static org.carbonrom.setupwizard.SetupWizardApp.KEY_SEND_METRICS;
import static org.carbonrom.setupwizard.SetupWizardApp.KEY_DARK_THEME;

import android.app.Activity;
import android.content.ComponentName;
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

import com.google.android.setupcompat.util.WizardManagerHelper;

import org.carbonrom.setupwizard.R;

//import lineageos.hardware.LineageHardwareManager;
//import lineageos.providers.LineageSettings;

public class CarbonSettingsActivity extends BaseSetupWizardActivity {

    public static final String TAG = CarbonSettingsActivity.class.getSimpleName();

    public static final String PRIVACY_POLICY_URI = "http://legal.carbonrom.org";

    private SetupWizardApp mSetupWizardApp;

    private CheckBox mMetrics;
    private CheckBox mNavKeys;
    private CheckBox mDarkTheme;

    private boolean mSupportsKeyDisabler = false;

    private View.OnClickListener mMetricsClickListener = view -> {
        boolean checked = !mMetrics.isChecked();
        mMetrics.setChecked(checked);
        mSetupWizardApp.getSettingsBundle().putBoolean(KEY_SEND_METRICS, checked);
    };

    private View.OnClickListener mNavKeysClickListener = view -> {
        boolean checked = !mNavKeys.isChecked();
        mNavKeys.setChecked(checked);
        mSetupWizardApp.getSettingsBundle().putBoolean(DISABLE_NAV_KEYS, checked);
    };

    private View.OnClickListener mCustomThemeClickListener = view -> {
        this.startActivity(new Intent().setComponent(new ComponentName("com.android.wallpaper",
            "com.android.customization.picker.CustomizationPickerActivity")));
    };

    private View.OnClickListener mDarkThemeClickListener = view -> {
        boolean checked = !mDarkTheme.isChecked();
        mDarkTheme.setChecked(checked);
        mSetupWizardApp.getSettingsBundle().putBoolean(KEY_DARK_THEME, checked);
    };

    private View.OnClickListener mSystemNavClickListener = view -> {
        this.startActivity(new Intent().setComponent(new ComponentName("com.android.settings",
                "com.android.settings.Settings$SystemNavActivity")));
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
        int ppStartIndex = policySummary.indexOf(privacy_policy);
        ss.setSpan(clickableSpan,
                ppStartIndex, ppStartIndex + privacy_policy.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView privacyPolicy = (TextView) findViewById(R.id.privacy_policy);
        privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
        privacyPolicy.setText(ss);

        View metricsRow = findViewById(R.id.metrics);
        metricsRow.setOnClickListener(mMetricsClickListener);
        String metricsHelpImproveCarbon =
                getString(R.string.services_help_improve_cm, getString(R.string.os_name));
        String metricsSummary = getString(R.string.services_metrics_label,
                metricsHelpImproveCarbon, getString(R.string.os_name));
        final SpannableStringBuilder metricsSpan = new SpannableStringBuilder(metricsSummary);
        metricsSpan.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                0, metricsHelpImproveCarbon.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView metrics = (TextView) findViewById(R.id.enable_metrics_summary);
        metrics.setText(metricsSpan);
        mMetrics = (CheckBox) findViewById(R.id.enable_metrics_checkbox);

        View systemNavRow = findViewById(R.id.system_nav);
        systemNavRow.setOnClickListener(mSystemNavClickListener);

        View customThemeRow = findViewById(R.id.customize_theme);
        customThemeRow.setOnClickListener(mCustomThemeClickListener);

        View darkThemeRow = findViewById(R.id.dark_theme);
        darkThemeRow.setOnClickListener(mDarkThemeClickListener);
        mDarkTheme = (CheckBox) findViewById(R.id.enable_dark_theme);

        /*View navKeysRow = findViewById(R.id.nav_keys);
        navKeysRow.setOnClickListener(mNavKeysClickListener);
        mNavKeys = (CheckBox) findViewById(R.id.nav_keys_checkbox);
        mSupportsKeyDisabler = isKeyDisablerSupported(this);
        if (mSupportsKeyDisabler) {
            mNavKeys.setChecked(LineageSettings.System.getIntForUser(getContentResolver(),
                    LineageSettings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0);
        } else {
            navKeysRow.setVisibility(View.GONE);
        }*/
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDisableNavkeysOption();
        updateMetricsOption();
    }

    @Override
    protected void onNextPressed() {
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), Activity.RESULT_OK);
        nextAction(NEXT_REQUEST, intent);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_carbon_settings;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_services;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_features;
    }

    private void updateMetricsOption() {
        final Bundle myPageBundle = mSetupWizardApp.getSettingsBundle();
        boolean metricsChecked =
                !myPageBundle.containsKey(KEY_SEND_METRICS) || myPageBundle
                        .getBoolean(KEY_SEND_METRICS);
        mMetrics.setChecked(metricsChecked);
        myPageBundle.putBoolean(KEY_SEND_METRICS, metricsChecked);
    }

    private void updateDisableNavkeysOption() {
        if (mSupportsKeyDisabler) {
            final Bundle myPageBundle = mSetupWizardApp.getSettingsBundle();
            boolean enabled = false; //LineageSettings.System.getIntForUser(getContentResolver(),
                    //LineageSettings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;
            boolean checked = myPageBundle.containsKey(DISABLE_NAV_KEYS) ?
                    myPageBundle.getBoolean(DISABLE_NAV_KEYS) :
                    enabled;
            mNavKeys.setChecked(checked);
            myPageBundle.putBoolean(DISABLE_NAV_KEYS, checked);
        }
    }

    private static boolean isKeyDisablerSupported(Context context) {
        //final LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        return false; //hardware.isSupported(LineageHardwareManager.FEATURE_KEY_DISABLE);
    }
}
