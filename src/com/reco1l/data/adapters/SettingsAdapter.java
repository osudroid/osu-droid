package com.reco1l.data.adapters;

import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.reco1l.data.BaseAdapter;
import com.reco1l.data.BaseViewHolder;
import com.reco1l.data.Settings;
import com.reco1l.interfaces.fields.Identifiers;
import com.reco1l.ui.BasePreferenceFragment;
import com.reco1l.ui.fragments.SettingsMenu;
import com.reco1l.utils.Views;

import java.util.ArrayList;

public class SettingsAdapter extends BaseAdapter<SettingsAdapter.VH, Settings.Wrapper> {

    private int mCount;

    //----------------------------------------------------------------------------------------//

    public SettingsAdapter(ArrayList<Settings.Wrapper> list) {
        super(list);
    }

    //----------------------------------------------------------------------------------------//

    @Override
    protected VH getViewHolder(View rootView) {
        int id = Identifiers.SettingMenu_SectionFrame + mCount;

        FrameLayout layout = new FrameLayout(context());
        layout.setLayoutParams(Views.wrap_content);
        layout.setId(id);

        mCount++;

        return new VH(layout);
    }

    //----------------------------------------------------------------------------------------//

    // Avoids IllegalStateException because fragments need to be public static
    public static class SectionFragment extends BasePreferenceFragment {

        private final VH mHolder;

        //----------------------------------------------------------------------------------------//

        public SectionFragment(VH holder) {
            mHolder = holder;
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected int getPreferenceXML() {
            return mHolder.getItem().getPreferenceXML();
        }

        @Override
        protected void onLoad() {
            mHolder.getItem().onLoad(this);
        }
    }

    //----------------------------------------------------------------------------------------//

    public static class VH extends BaseViewHolder<Settings.Wrapper> {

        //------------------------------------------------------------------------------------//

        public VH(@NonNull View root) {
            super(root);

            SectionFragment fragment = new SectionFragment(this);

            // It will run once root view get in layout.
            fragment.replace(root, SettingsMenu.instance.getChildFragmentManager());
        }

        //------------------------------------------------------------------------------------//

        @Override
        protected void onBind(Settings.Wrapper wrapper, int position) {
            // Unused
        }
    }
}
