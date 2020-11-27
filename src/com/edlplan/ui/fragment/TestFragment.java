package com.edlplan.ui.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.nsu.ccfit.zuev.osuplus.R;

public class TestFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.frg_test, container, false);
        TextView textView = root.findViewById(R.id.textView);
        //textView.animate().translationYBy(500).setDuration(1000).start();
        return root;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        System.out.println("detach " + this);
    }
}
