package com.gee12.mytetroid.views.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.gee12.mytetroid.R;
import com.gee12.mytetroid.viewmodels.BaseStorageViewModel;
import com.gee12.mytetroid.viewmodels.factory.TetroidViewModelFactory;
import com.gee12.mytetroid.views.Message;

import org.jetbrains.annotations.NotNull;

public abstract class TetroidFragment<VM extends BaseStorageViewModel> extends Fragment implements View.OnTouchListener {

    protected Context context;
    protected GestureDetectorCompat mGestureDetector;

    protected VM viewModel;

    public TetroidFragment() {}

    public TetroidFragment(GestureDetectorCompat detector) {
        this.mGestureDetector = detector;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initViewModel();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, int resId) {
        initViewModel();
        return inflater.inflate(resId, container, false);
    }

    protected void initViewModel() {
        this.viewModel = new ViewModelProvider(getActivity(), new TetroidViewModelFactory(getActivity().getApplication()))
                .get(getViewModelClazz());
        viewModel.logDebug(getString(R.string.log_fragment_opened_mask, getClass().getSimpleName()));
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        this.context = getContext();
    }

    protected abstract Class<VM> getViewModelClazz();

    public abstract String getTitle();

    /**
     *
     * @param detector
     */
    public void setGestureDetector(GestureDetectorCompat detector) {
        this.mGestureDetector = detector;
    }

    /**
     * Переопределяем обработчик нажатия на экране
     * для обработки перехода в полноэкранный режим.
     * @param v
     * @param event
     * @return
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector != null)
            mGestureDetector.onTouchEvent(event);
        return false;
    }

    /**
     * Вывод интерактивного уведомления SnackBar "Подробнее в логах".
     */
    protected void showSnackMoreInLogs() {
        Message.showSnackMoreInLogs(this, R.id.layout_coordinator);
    }
}
