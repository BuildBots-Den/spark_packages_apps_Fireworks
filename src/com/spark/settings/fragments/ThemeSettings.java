package com.spark.settings.fragments;

import com.android.internal.logging.nano.MetricsProto;

import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import com.android.internal.util.spark.SparkUtils;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import com.android.settings.development.OverlayCategoryPreferenceController;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.content.om.IOverlayManager;
import android.content.res.Resources;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;
import android.provider.Settings;
import com.spark.support.preferences.SystemSettingMasterSwitchPreference;
import com.spark.support.preferences.SystemSettingSwitchPreference;
import com.spark.support.preferences.SystemSettingListPreference;
import com.spark.support.preferences.SystemSettingEditTextPreference;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import java.util.Locale;
import android.text.TextUtils;
import android.content.Context;
import androidx.fragment.app.Fragment;
import android.view.View;
import android.os.UserHandle;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import android.util.Log;
import com.android.internal.util.spark.SparkUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import org.json.JSONException;
import org.json.JSONObject;
import static android.os.UserHandle.USER_SYSTEM;
import android.os.RemoteException;
import android.os.ServiceManager;
import static android.os.UserHandle.USER_CURRENT;
import java.util.List;
import java.util.ArrayList;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.regex.Pattern;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ThemeSettings extends DashboardFragment implements
        OnPreferenceChangeListener {

    public static final String TAG = "ThemeSettings";

    private static final String QS_FOOTER_TEXT_STRING = "qs_footer_text_string";
    private static final String ALT_SETTINGS_LAYOUT = "alt_settings_layout";
    private static final String SETTINGS_DASHBOARD_STYLE = "settings_dashboard_style";
    private static final String USE_STOCK_LAYOUT = "use_stock_layout";
    private static final String DISABLE_USERCARD = "disable_usercard";
    private static final String PREF_TILE_ANIM_STYLE = "qs_tile_animation_style";
    private static final String PREF_TILE_ANIM_DURATION = "qs_tile_animation_duration";
    private static final String PREF_TILE_ANIM_INTERPOLATOR = "qs_tile_animation_interpolator";
    private static final String QS_PANEL_STYLE  = "qs_panel_style";
    private static final String KEY_QS_UI_STYLE = "qs_ui_style";
    private static final String KEY_SYS_INFO = "qs_system_info";
    private static final String KEY_SYS_INFO_ICON = "qs_system_info_icon";

    private Handler mHandler;
    private IOverlayManager mOverlayManager;
    private IOverlayManager mOverlayService;
    private SystemSettingListPreference mQsStyle;
    private SystemSettingListPreference mQsUI;
    private SystemSettingEditTextPreference mFooterString;
    private SystemSettingListPreference mSettingsDashBoardStyle;
    private ListPreference mTileAnimationStyle;
    private ListPreference mTileAnimationDuration;
    private SystemSettingSwitchPreference mAltSettingsLayout;
    private ListPreference mTileAnimationInterpolator;
    private SystemSettingSwitchPreference mUseStockLayout;
    private SystemSettingSwitchPreference mDisableUserCard;
    private ListPreference mSystemInfo;
    private SwitchPreference mSystemInfoIcon;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

	Context mContext = getActivity().getApplicationContext();

        final ContentResolver resolver = getActivity().getContentResolver();

        PreferenceScreen prefSet = getPreferenceScreen();

        mFooterString = (SystemSettingEditTextPreference) findPreference(QS_FOOTER_TEXT_STRING);
        mFooterString.setOnPreferenceChangeListener(this);
        String footerString = Settings.System.getString(getContentResolver(),
                QS_FOOTER_TEXT_STRING);
        if (footerString != null && !footerString.isEmpty())
            mFooterString.setText(footerString);
        else {
            mFooterString.setText("Spark");
            Settings.System.putString(getActivity().getContentResolver(),
                    Settings.System.QS_FOOTER_TEXT_STRING, "Spark");
        }
        mSettingsDashBoardStyle = (SystemSettingListPreference) findPreference(SETTINGS_DASHBOARD_STYLE);
        mSettingsDashBoardStyle.setOnPreferenceChangeListener(this);
        mAltSettingsLayout = (SystemSettingSwitchPreference) findPreference(ALT_SETTINGS_LAYOUT);
        mAltSettingsLayout.setOnPreferenceChangeListener(this);
        mUseStockLayout = (SystemSettingSwitchPreference) findPreference(USE_STOCK_LAYOUT);
        mUseStockLayout.setOnPreferenceChangeListener(this);
        mDisableUserCard = (SystemSettingSwitchPreference) findPreference(DISABLE_USERCARD);
        mDisableUserCard.setOnPreferenceChangeListener(this);

        mTileAnimationStyle = (ListPreference) findPreference(PREF_TILE_ANIM_STYLE);
        int tileAnimationStyle = Settings.System.getIntForUser(resolver,
                Settings.System.ANIM_TILE_STYLE, 0, UserHandle.USER_CURRENT);
        mTileAnimationStyle.setValue(String.valueOf(tileAnimationStyle));
        updateTileAnimationStyleSummary(tileAnimationStyle);
        updateAnimTileStyle(tileAnimationStyle);
        mTileAnimationStyle.setOnPreferenceChangeListener(this);

        mTileAnimationDuration = (ListPreference) findPreference(PREF_TILE_ANIM_DURATION);
        int tileAnimationDuration = Settings.System.getIntForUser(resolver,
                Settings.System.ANIM_TILE_DURATION, 2000, UserHandle.USER_CURRENT);
        mTileAnimationDuration.setValue(String.valueOf(tileAnimationDuration));
        updateTileAnimationDurationSummary(tileAnimationDuration);
        mTileAnimationDuration.setOnPreferenceChangeListener(this);

        mTileAnimationInterpolator = (ListPreference) findPreference(PREF_TILE_ANIM_INTERPOLATOR);
        int tileAnimationInterpolator = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.ANIM_TILE_INTERPOLATOR, 0, UserHandle.USER_CURRENT);
        mTileAnimationInterpolator.setValue(String.valueOf(tileAnimationInterpolator));
        updateTileAnimationInterpolatorSummary(tileAnimationInterpolator);
        mTileAnimationInterpolator.setOnPreferenceChangeListener(this);

        mOverlayService = IOverlayManager.Stub
        .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));

        mQsStyle = (SystemSettingListPreference) findPreference(QS_PANEL_STYLE);
        mQsUI = (SystemSettingListPreference) findPreference(KEY_QS_UI_STYLE);
        mCustomSettingsObserver.observe();

        mSystemInfo = (ListPreference) findPreference(KEY_SYS_INFO);
	mSystemInfoIcon = (SwitchPreference) findPreference(KEY_SYS_INFO_ICON);
        boolean mSystemInfoSupported = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_supportSystemInfo);
        if (!mSystemInfoSupported) {
            prefSet.removePreference(mSystemInfo);
            prefSet.removePreference(mSystemInfoIcon);
        }
    }

    private CustomSettingsObserver mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
    private class CustomSettingsObserver extends ContentObserver {

        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            Context mContext = getContext();
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QS_PANEL_STYLE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.QS_UI_STYLE),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(Settings.System.QS_PANEL_STYLE))) {
                updateQsStyle();
	    }
            else if  (uri.equals(Settings.System.getUriFor(Settings.System.QS_UI_STYLE))) {
                updateQsStyle(true /*QS UI theme*/);
	    }
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.spark_settings_themes;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mFooterString) {
            String value = (String) newValue;
            if (value != "" && !value.isEmpty())
                Settings.System.putString(getActivity().getContentResolver(),
                        Settings.System.QS_FOOTER_TEXT_STRING, value);
            else {
                mFooterString.setText("Spark");
                Settings.System.putString(getActivity().getContentResolver(),
                        Settings.System.QS_FOOTER_TEXT_STRING, "Spark");
            }
            return true;
        } else if (preference == mSettingsDashBoardStyle) {
            SparkUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mAltSettingsLayout) {
            SparkUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mUseStockLayout) {
            SparkUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mDisableUserCard) {
            SparkUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mTileAnimationStyle) {
            int tileAnimationStyle = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(resolver, Settings.System.ANIM_TILE_STYLE,
                    tileAnimationStyle, UserHandle.USER_CURRENT);
            updateTileAnimationStyleSummary(tileAnimationStyle);
            updateAnimTileStyle(tileAnimationStyle);
            return true;
        } else if (preference == mTileAnimationDuration) {
            int tileAnimationDuration = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(resolver, Settings.System.ANIM_TILE_DURATION,
                    tileAnimationDuration, UserHandle.USER_CURRENT);
            updateTileAnimationDurationSummary(tileAnimationDuration);
            return true;
        } else if (preference == mTileAnimationInterpolator) {
            int tileAnimationInterpolator = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(resolver, Settings.System.ANIM_TILE_INTERPOLATOR,
                    tileAnimationInterpolator, UserHandle.USER_CURRENT);
            updateTileAnimationInterpolatorSummary(tileAnimationInterpolator);
            return true;
        } else if (preference == mQsStyle) {
            mCustomSettingsObserver.observe();
            return true;
        } else if (preference == mQsUI) {
            mCustomSettingsObserver.observe();
            return true;
	}
        return false;
    }


    private void updateQsStyle(boolean isQsUI) {
        ContentResolver resolver = getActivity().getContentResolver();

	boolean isA11Style = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.QS_UI_STYLE , 1, UserHandle.USER_CURRENT) == 1;
	if (isQsUI) {
	    setQsStyle(isA11Style ? "com.android.system.qs.ui.A11" : "com.android.systemui");
	} else {
        int qsPanelStyle = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.QS_PANEL_STYLE , 0, UserHandle.USER_CURRENT);

        switch (qsPanelStyle) {
    	  case 0:
            setQsStyle(mOverlayService, "com.android.systemui");
          break;
          case 1:
            setQsStyle(mOverlayService, "com.android.system.qs.roundedrectangle");
	  break;
          case 2:
            setQsStyle(mOverlayService, "com.android.system.qs.outline");
	  break;
          case 3:
	  case 4:
            setQsStyle(mOverlayService, "com.android.system.qs.twotoneaccent");
	  break;
          case 5:
            setQsStyle(mOverlayService, "com.android.system.qs.shaded");
	  break;
          case 6:
            setQsStyle(mOverlayService, "com.android.system.qs.cyberpunk");
 	  break;
          case 7:
            setQsStyle(mOverlayService, "com.android.system.qs.neumorph");
	  break;
          case 8:
            setQsStyle(mOverlayService, "com.android.system.qs.reflected");
	  break;
          case 9:
            setQsStyle(mOverlayService, "com.android.system.qs.surround");
              break;
          case 10:
            setQsStyle(mOverlayService, "com.android.system.qs.thin");
              break;
            default:
              break;
        }
	 }
    }

    public static void setDefaultStyle(IOverlayManager overlayManager) {
        for (int i = 0; i < QS_STYLES.length; i++) {
            String qsStyles = QS_STYLES[i];
            try {
                overlayManager.setEnabled(qsStyles, false, USER_SYSTEM);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setQsStyle(String overlayName) {
        boolean isA11Style = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.QS_UI_STYLE , 1, UserHandle.USER_CURRENT) == 1;
        mThemeUtils.setOverlayEnabled(isA11Style ? "android.theme.customization.qs_ui" : "android.theme.customization.qs_panel", overlayName, "com.android.systemui");
    }
    }

    public static final String[] QS_STYLES = {
        "com.android.system.qs.roundedrectangle",
        "com.android.system.qs.outline",
        "com.android.system.qs.twotoneaccent",
        "com.android.system.qs.shaded",
        "com.android.system.qs.cyberpunk",
        "com.android.system.qs.neumorph",
        "com.android.system.qs.reflected",
        "com.android.system.qs.surround",
        "com.android.system.qs.thin"
    };


	private void updateTileAnimationStyleSummary(int tileAnimationStyle) {
        String prefix = (String) mTileAnimationStyle.getEntries()[mTileAnimationStyle.findIndexOfValue(String
                .valueOf(tileAnimationStyle))];
        mTileAnimationStyle.setSummary(getResources().getString(R.string.qs_set_animation_style, prefix));
    }

     private void updateTileAnimationDurationSummary(int tileAnimationDuration) {
        String prefix = (String) mTileAnimationDuration.getEntries()[mTileAnimationDuration.findIndexOfValue(String
                .valueOf(tileAnimationDuration))];
        mTileAnimationDuration.setSummary(getResources().getString(R.string.qs_set_animation_duration, prefix));
    }

    private void updateTileAnimationInterpolatorSummary(int tileAnimationInterpolator) {
        String prefix = (String) mTileAnimationInterpolator.getEntries()[mTileAnimationInterpolator.findIndexOfValue(String
                .valueOf(tileAnimationInterpolator))];
        mTileAnimationInterpolator.setSummary(getResources().getString(R.string.qs_set_animation_interpolator, prefix));
    }

    private void updateAnimTileStyle(int tileAnimationStyle) {
        if (mTileAnimationDuration != null) {
            if (tileAnimationStyle == 0) {
                mTileAnimationDuration.setSelectable(false);
                mTileAnimationInterpolator.setSelectable(false);
            } else {
                mTileAnimationDuration.setSelectable(true);
                mTileAnimationInterpolator.setSelectable(true);
            }
        }
        }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle(), this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, Lifecycle lifecycle, Fragment fragment) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        return controllers;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SPARK_SETTINGS;
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.spark_settings_themes);
}
