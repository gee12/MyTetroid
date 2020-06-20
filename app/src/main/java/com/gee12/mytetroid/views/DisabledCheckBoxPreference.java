package com.gee12.mytetroid.views;

import android.content.Context;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.RequiresApi;

import com.gee12.mytetroid.utils.ViewUtils;

/**
 * Обычный CheckBoxPreference, но при отключении (установке setEnabled(false)) реагирующий
 *  на нажатия (OnPreferenceClickListener).
 */
public class DisabledCheckBoxPreference extends CheckBoxPreference {

    protected boolean mIsEnabled2 = true;
    protected boolean mDependencyMet2 = true;
    protected boolean mParentDependencyMet2 = true;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DisabledCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public DisabledCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DisabledCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DisabledCheckBoxPreference(Context context) {
        super(context);
    }

    @Override
    public void setEnabled(boolean enabled) {
        // меняем "системный" isEnabled на свой,
        // т.к. при установке false у системного - у опции отключается вызов обработчика нажатия onClick
        this.mIsEnabled2 = enabled;
    }

    @Override
    public boolean isEnabled() {
        // возвращаем всегда true, чтобы не отключилась вьюшка "системой" и не
        // заблокировался вызов обработчика нажатия onClick
        return true;
    }

    public boolean isEnabled2() {
        return mIsEnabled2 && mDependencyMet2 && mParentDependencyMet2;
    }

    @Override
    protected void onClick() {
        // если опция не активна, блокируем ее установку в методе setChecked(), который
        // вызывается в специальном обработчике onClick()
        if (isEnabled2()) {
            super.onClick();
        }
    }

    @Override
    public boolean shouldDisableDependents() {
        // проверка нужно ли отключать зависимые опции
        boolean shouldDisable = getDisableDependentsState() ? isChecked() : !isChecked();
        return shouldDisable || !isEnabled2();
    }

    @Override
    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        // проверка активности вьюшки, от которой зависим
        this.mDependencyMet2 = !disableDependent;
        super.onDependencyChanged(dependency, disableDependent);
    }

    @Override
    public void onParentChanged(Preference parent, boolean disableChild) {
        // проверка активности родительской вьюшки
        this.mParentDependencyMet2 = !disableChild;
        super.onParentChanged(parent, disableChild);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        // отключаем вьюшки, если нужно (при этом сохраняется обработчик нажатия onClick)
        if (getShouldDisableView()) {
            ViewUtils.setEnabledStateOnViews(view, isEnabled2());
        }
    }
}
