package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
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
import com.edlplan.ui.InputDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

public class FavoriteManagerFragment extends BaseFragment {

    private String selected;

    private FMAdapter adapter;

    private Runnable onLoadViewFunc;

    @Override
    protected int getLayoutID() {
        return R.layout.favorite_manager_dialog;
    }

    @Override
    protected void onLoadView() {
        setDismissOnBackgroundClick(true);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        ((RecyclerView) findViewById(R.id.main_recycler_view)).setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.VERTICAL);

        Button newFolder = findViewById(R.id.new_folder);
        newFolder.setOnClickListener(v -> {
            InputDialog dialog = new InputDialog(getContext());
            dialog.showForResult(s -> {
                if (s.isEmpty()) return;
                if (FavoriteLibrary.get().getMaps(s) == null && !s.equals(StringTable.get(R.string.favorite_default))) {
                    FavoriteLibrary.get().addFolder(s);
                    adapter.add(s);
                }
            });
        });

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
            ArrayList<String> tmp = new ArrayList<>(folders);
            folders = new ArrayList<>();
            folders.add(tmp.get(0));
            folders.add(folder);
            tmp.remove(tmp.get(0));
            folders.addAll(tmp);
            notifyDataSetChanged();
        }

        protected void load() {
            ArrayList<String> tmp = new ArrayList<>(FavoriteLibrary.get().getFolders());
            Collections.sort(tmp);
            folders = new ArrayList<>();
            folders.add(StringTable.get(R.string.favorite_default));
            folders.addAll(tmp);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.favorite_folder_item, parent, false));
        }

        @Override
        public int getItemCount() {
            return folders.size();
        }

    }

    public class AddAdapter extends FMAdapter {

        private String track;

        public AddAdapter(String track) {
            this.track = track;
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final String f = folders.get(position);
            holder.folderName.setText(String.format("%s (%s)",
                    f, FavoriteLibrary.get().getMaps(f) == null ? "*" : ("" + FavoriteLibrary.get().getMaps(f).size())));
            holder.button1.setText(R.string.favorite_delete);
            if (position == 0) {
                holder.button2.setText("( • ̀ω•́ )✧");
            } else {
                holder.button2.setText(FavoriteLibrary.get().inFolder(f, track) ? R.string.favorite_remove : R.string.favorite_add);
            }

            holder.button1.setOnClickListener(new View.OnClickListener() {
                boolean clicked = false;
                boolean deleted = false;

                @Override
                public void onClick(View v) {
                    if (clicked) {
                        FavoriteLibrary.get().remove(f);
                        deleted = true;
                        holder.button1.setTextColor(Color.argb(255, 255, 255, 255));
                        load();
                        notifyDataSetChanged();
                    } else {
                        clicked = true;
                        holder.button1.setText(R.string.favorite_ensure);
                        holder.button1.setTextColor(Color.argb(255, 255, 0, 0));
                        findViewById(R.id.main_recycler_view).postDelayed(() -> {
                            if (!deleted) {
                                clicked = false;
                                holder.button1.setTextColor(Color.argb(255, 0, 0, 0));
                                holder.button1.setText(R.string.favorite_delete);
                            }
                        }, 2000);
                    }
                }
            });

            holder.button2.setOnClickListener(view -> {
                if (FavoriteLibrary.get().inFolder(f, track)) {
                    FavoriteLibrary.get().remove(f, track);
                    holder.folderName.setText(String.format("%s (%s)",
                            f, FavoriteLibrary.get().getMaps(f) == null ? "*" : ("" + FavoriteLibrary.get().getMaps(f).size())));
                    holder.button2.setText(R.string.favorite_add);
                } else {
                    FavoriteLibrary.get().add(f, track);
                    holder.folderName.setText(String.format("%s (%s)",
                            f, FavoriteLibrary.get().getMaps(f) == null ? "*" : ("" + FavoriteLibrary.get().getMaps(f).size())));
                    holder.button2.setText(R.string.favorite_remove);
                }
            });

            if (position == 0) {
                holder.button1.setOnClickListener(view -> {
                    ToastLogger.showText(StringTable.get(R.string.favorite_warning_on_delete_default), false);
                });
                holder.button2.setOnClickListener(view -> {
                });
            }
        }
    }

    public class SelectAdapter extends FMAdapter {

        private OnSelectListener onSelectListener;

        public SelectAdapter(OnSelectListener onSelectListener) {
            this.onSelectListener = onSelectListener;
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final String f = folders.get(position);
            holder.folderName.setText(String.format("%s (%s)",
                    f, FavoriteLibrary.get().getMaps(f) == null ? "*" : ("" + FavoriteLibrary.get().getMaps(f).size())));
            holder.button1.setText(R.string.favorite_delete);
            holder.button2.setText(R.string.favorite_select);

            View.OnClickListener mainClick;
            if (position != 0) {
                mainClick = view -> {
                    selected = folders.get(position);
                    dismiss();
                    onSelectListener.onSelect(selected);
                };
            } else {
                mainClick = view -> {
                    selected = folders.get(position);
                    dismiss();
                    onSelectListener.onSelect(null);
                };
            }

            holder.button2.setOnClickListener(mainClick);
            holder.mainBody.setOnClickListener(mainClick);

            holder.button1.setOnClickListener(new View.OnClickListener() {
                boolean clicked = false;
                boolean deleted = false;

                @Override
                public void onClick(View v) {
                    if (clicked) {
                        FavoriteLibrary.get().remove(f);
                        deleted = true;
                        holder.button1.setTextColor(Color.argb(255, 255, 255, 255));
                        load();
                        notifyDataSetChanged();
                    } else {
                        clicked = true;
                        holder.button1.setText(R.string.favorite_ensure);
                        holder.button1.setTextColor(Color.argb(255, 255, 0, 0));
                        findViewById(R.id.main_recycler_view).postDelayed(() -> {
                            if (!deleted) {
                                clicked = false;
                                holder.button1.setTextColor(Color.argb(255, 255, 255, 255));
                                holder.button1.setText(R.string.favorite_delete);
                            }
                        }, 2000);
                    }
                }
            });

            if (position == 0) {
                holder.button1.setOnClickListener(view -> {
                    ToastLogger.showText(StringTable.get(R.string.favorite_warning_on_delete_default), false);
                });
            }
        }
    }
}
