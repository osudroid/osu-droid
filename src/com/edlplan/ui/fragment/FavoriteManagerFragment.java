package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.edlplan.favorite.FavoriteLibrary;
import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;
import com.reco1l.osu.ui.Dialog;
import com.reco1l.toolkt.android.Texts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

public class FavoriteManagerFragment extends BaseFragment {

    private String selected;

    private FMAdapter adapter;

    private Runnable onLoadViewFunc;

    @Override
    protected int getLayoutID() {
        return R.layout.collections_fragment;
    }

    @Override
    protected void onLoadView() {
        setDismissOnBackgroundClick(true);

        var layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.VERTICAL);

        ((RecyclerView) findViewById(R.id.main_recycler_view)).setLayoutManager(layoutManager);

        findViewById(R.id.new_folder).setOnClickListener(v -> Dialog.showPrompt("New folder", null, true, input -> {

            if (input.isEmpty())
                return null;

            if (FavoriteLibrary.get().getMaps(input) == null && !input.equals(StringTable.get(R.string.favorite_default))) {
                FavoriteLibrary.get().addFolder(input);
                adapter.add(input);
            }

            return null;
        }));

        if (onLoadViewFunc != null) {
            onLoadViewFunc.run();
        }

        playOnLoadAnim();
    }

    private void playOnLoadAnim() {
        View body = findViewById(R.id.frg_body);
        body.setTranslationY(600);
        body.animate().cancel();
        body.animate()
                .translationY(0)
                .setDuration(200)
                .start();
        playBackgroundHideInAnim(200);
    }

    private void playEndAnim(Runnable action) {
        View body = findViewById(R.id.frg_body);
        body.animate().cancel();
        body.animate()
                .translationXBy(50)
                .alpha(0)
                .setDuration(150)
                .setInterpolator(EasingHelper.asInterpolator(Easing.OutQuad))
                .setListener(new BaseAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (action != null) {
                            action.run();
                        }
                    }
                })
                .start();
        playBackgroundHideOutAnim(200);
    }

    @Override
    public void dismiss() {
        playEndAnim(super::dismiss);
    }

    public String getSelected() {
        return selected;
    }

    public void showToSelectFolder(OnSelectListener onSelectListener) {
        onLoadViewFunc = () -> {
            adapter = new SelectAdapter(onSelectListener);
            ((RecyclerView) findViewById(R.id.main_recycler_view)).setAdapter(adapter);
        };
        show();
    }

    public void showToAddToFolder(String track) {
        onLoadViewFunc = () -> {
            adapter = new AddAdapter(track);
            ((RecyclerView) findViewById(R.id.main_recycler_view)).setAdapter(adapter);
        };
        show();
    }

    public interface OnSelectListener {
        void onSelect(String folder);
    }

    private static class VH extends RecyclerView.ViewHolder {

        public TextView folderName;

        public Button button1;

        public Button button2;

        public View mainBody;

        public VH(View itemView) {
            super(itemView);
            mainBody = itemView.findViewById(R.id.mainBody);
            folderName = itemView.findViewById(R.id.folder_name);
            button1 = itemView.findViewById(R.id.button);
            button2 = itemView.findViewById(R.id.button2);
        }
    }

    public abstract class FMAdapter extends RecyclerView.Adapter<VH> {

        protected List<String> folders;

        public FMAdapter() {
            load();
        }

        public void add(String folder) {
            folders.add(includeDefaultFolder() ? 1 : 0, folder);
            notifyDataSetChanged();
        }


        protected abstract boolean includeDefaultFolder();


        protected void load() {
            folders = new ArrayList<>(FavoriteLibrary.get().getFolders());
            Collections.sort(folders);

            if (includeDefaultFolder()) {
                folders.add(0, StringTable.get(R.string.favorite_default));
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.collections_item, parent, false));
        }

        @Override
        public int getItemCount() {
            return folders.size();
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {

            // User cannot delete or remove beatmaps from default folder which will always be at first position.
            var isDefaultFolder = includeDefaultFolder() && position == 0;
            var folder = folders.get(position);

            updateFolderNameText(holder, folder);

            Texts.setDrawableLeft(holder.button1, getContext().getDrawable(R.drawable.delete_24px));
            Texts.getDrawableLeft(holder.button1).setTint(0xFFFFBFBF);
            holder.button1.setVisibility(isDefaultFolder ? View.GONE : View.VISIBLE);

            holder.button1.setOnClickListener(v -> new AlertDialog.Builder(getContext()) {{

                setTitle("Remove collection");
                setMessage(R.string.favorite_ensure);

                setPositiveButton("Yes", (d, __) -> {
                    FavoriteLibrary.get().remove(folder);
                    load();
                    notifyDataSetChanged();
                });

                setNegativeButton("No", (d, __) -> d.dismiss());

            }}.show());

        }

        protected void updateFolderNameText(VH holder, String name) {
            var maps = FavoriteLibrary.get().getMaps(name);

            holder.folderName.setText(String.format("%s (%s)", name, maps == null ? "*" : maps.size()));
        }
    }

    public class AddAdapter extends FMAdapter {

        private String track;

        public AddAdapter(String track) {
            this.track = track;
        }

        @Override
        protected boolean includeDefaultFolder() {
            return false;
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {

            super.onBindViewHolder(holder, position);

            var folder = folders.get(position);

            Texts.setDrawableLeft(holder.button2, getContext().getDrawable(FavoriteLibrary.get().inFolder(folder, track) ? R.drawable.remove_24px : R.drawable.add_24px));
            holder.button2.setOnClickListener(view -> {

                if (FavoriteLibrary.get().inFolder(folder, track)) {
                    FavoriteLibrary.get().remove(folder, track);
                    Texts.setDrawableLeft(holder.button2, getContext().getDrawable(R.drawable.add_24px));
                } else {
                    FavoriteLibrary.get().add(folder, track);
                    Texts.setDrawableLeft(holder.button2, getContext().getDrawable(R.drawable.remove_24px));
                }

                updateFolderNameText(holder, folder);
            });

        }
    }

    public class SelectAdapter extends FMAdapter {

        private OnSelectListener onSelectListener;

        public SelectAdapter(OnSelectListener onSelectListener) {
            this.onSelectListener = onSelectListener;
        }

        @Override
        protected boolean includeDefaultFolder() {
            return true;
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {

            super.onBindViewHolder(holder, position);

            // The button isn't needed here.
            holder.button2.setVisibility(View.GONE);

            holder.mainBody.setOnClickListener(v -> {
                selected = folders.get(position);
                dismiss();
                onSelectListener.onSelect(position == 0 ? null : selected);
            });

        }
    }
}
