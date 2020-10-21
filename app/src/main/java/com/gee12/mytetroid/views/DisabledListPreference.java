package com.gee12.mytetroid.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.gee12.mytetroid.utils.ViewUtils;

/**
 * Обычный ListPreference, но при отключении (установке setEnabled(false)) реагирующий
 *  на нажатия (OnPreferenceClickListener).
 */
public class DisabledListPreference extends ListPreference {

    protected boolean mIsEnabled2 = true;
    protected boolean mDependencyMet2 = true;
    protected boolean mParentDependencyMet2 = true;

    public DisabledListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public DisabledListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DisabledListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DisabledListPreference(Context context) {
        super(context);
    }

    @Override
    public void setEnabled(boolean enabled) {
        // меняем "системный" isEnabled на свой,
        // т.к. при установке false у системного - у опции отключается вызов обработчика нажатия onClick
        if (mIsEnabled2 != enabled) {
            mIsEnabled2 = enabled;

            // Enabled state can change dependent preferences' states, so notify
            notifyDependencyChange(shouldDisableDependents());

            notifyChanged();
        }
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
        return !isEnabled2();
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
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        // отключаем вьюшки, если нужно (при этом сохраняется обработчик нажатия onClick)
        if (getShouldDisableView()) {
            ViewUtils.setEnabledStateOnViews(holder.itemView, isEnabled2());
            holder.itemView.setEnabled(true);
        } else {
            ViewUtils.setEnabledStateOnViews(holder.itemView, true);
        }
    }
}
