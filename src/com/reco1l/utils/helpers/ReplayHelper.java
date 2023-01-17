package com.reco1l.utils.helpers;

import static androidx.activity.result.contract.ActivityResultContracts.*;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider;

import com.edlplan.replay.OdrDatabase;
import com.edlplan.replay.OsuDroidReplay;
import com.edlplan.replay.OsuDroidReplayPack;
import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.data.GameNotification;
import com.reco1l.tables.Res;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.custom.DialogBuilder;

import java.io.File;
import java.util.List;

import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

public final class ReplayHelper {

    //--------------------------------------------------------------------------------------------//

    private ReplayHelper() {}

    //--------------------------------------------------------------------------------------------//

    public static void export(int pId) {
        List<OsuDroidReplay> replays = OdrDatabase.get().getReplayById(pId);

        GameNotification notification = GameNotification.of("replay export")
                .setMessage("Exporting replay...")
                .showProgress(true);

        notification.commit();
        try {
            OsuDroidReplay replay = replays.get(0);
            File parent = new File(Environment.getExternalStorageDirectory(), "osu!droid/export");

            CharSequence name = replay.getFileName().subSequence(
                    replay.getFileName().indexOf('/') + 1,
                    replay.getFileName().lastIndexOf('.')
            );

            File file = new File(parent, String.format("%s [%s]-%d.edr", name, replay.getPlayerName(), replay.getTime()));

            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            OsuDroidReplayPack.packTo(file, replay);

            notification.runOnClick(() -> {

                Uri uri = FileProvider.getUriForFile(Game.activity, BuildConfig.APPLICATION_ID + ".fileProvider", file);

                Intent intent = new Intent()
                        .setAction(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "*/*")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Game.activity.registerForActivityResult(new StartActivityForResult(), result -> {})
                        .launch(intent);
            });

            notification.setMessage("Saved replay to " + file.getAbsolutePath());
        } catch (Exception exception) {
            exception.printStackTrace();
            notification.setMessage("Failed to export replay!\n" + exception.getMessage());
        }
    }

    public static void delete(int pId) {
        DialogBuilder dialog = new DialogBuilder();

        dialog.title = "Delete replay";
        dialog.message = "Are you sure?";
        dialog.addButton("Yes", d -> {
            List<OsuDroidReplay> replays = OdrDatabase.get().getReplayById(pId);
            if (replays.size() > 0) {

               /* GameNotification n = GameNotification.of("replay delete");
                n.setMessage("Failed to delete replay!");

                try {
                    if (OdrDatabase.get().deleteReplay(data.id) != 0) {
                        n.message = Res.str(R.string.menu_deletescore_delete_success);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    n.message += "\n" + exception.getMessage();
                }
                UI.notificationCenter.add(n);*/
            }
            d.close();
        });

        new Dialog(dialog).show();
    }
}
