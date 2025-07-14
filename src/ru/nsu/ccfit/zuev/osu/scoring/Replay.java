package ru.nsu.ccfit.zuev.osu.scoring;

import android.graphics.PointF;

import androidx.annotation.NonNull;

import com.rian.osu.mods.LegacyModConverter;
import com.rian.osu.mods.ModHardRock;
import com.rian.osu.mods.ModReplayV6;
import com.rian.osu.utils.ModHashMap;
import com.rian.osu.utils.ModUtils;

import org.anddev.andengine.util.Debug;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;

public class Replay {
    protected int pointsSkipped = 0;
    public ArrayList<MoveArray> cursorMoves = new ArrayList<>();
    public int[] cursorIndex;
    public int[] lastMoveIndex;
    public ReplayObjectData[] objectData = null;
    public int replayVersion;
    public StatisticV2 stat = null;
    private String md5 = "";
    private String beatmapName = "";
    private String beatmapsetName = "";
    private boolean isSaving;

    public Replay() {
        this(false);
    }

    public Replay(boolean allocateMoves) {
        if (allocateMoves) {
            cursorMoves.add(new MoveArray(200));
            cursorMoves.add(new MoveArray(50));
            for (int i = 2; i < GameScene.CursorCount; i++) {
                cursorMoves.add(new MoveArray(15));
            }
        }

        cursorIndex = new int[GameScene.CursorCount];
        lastMoveIndex = new int[GameScene.CursorCount];
        for (int i = 0; i < GameScene.CursorCount; i++) {
            cursorIndex[i] = 0;
            lastMoveIndex[i] = -1;
        }
    }

    public void setBeatmap(String beatmapsetName, String beatmapName, String md5) {
        this.beatmapsetName = beatmapsetName;
        this.md5 = md5;
        this.beatmapName = beatmapName;
    }

    public void setObjectCount(int count) {
        objectData = new ReplayObjectData[count];
    }

    public void addObjectResult(int id, short accuracy, BitSet ticks) {
        if (id < 0 || objectData == null || id >= objectData.length)
            return;

        ReplayObjectData data = objectData[id] == null ? new ReplayObjectData() : objectData[id];
        data.accuracy = accuracy;
        data.tickSet = ticks;
        objectData[id] = data;
    }

    public void addObjectScore(int id, ResultType score) {
        if (id < 0 || objectData == null || id >= objectData.length)
            return;

        if (objectData[id] == null)
            objectData[id] = new ReplayObjectData();

        objectData[id].result = score.getId();
    }

    public void addPress(final int timeMs, final PointF pos, final int pid) {
        if (pid > cursorMoves.size() || isSaving) return;
        cursorMoves.get(pid).pushBack(this, timeMs, pos.x, pos.y, TouchType.DOWN);
    }

    public void addMove(final int timeMs, final PointF pos, final int pid) {
        if (pid > cursorMoves.size() || isSaving) return;
        cursorMoves.get(pid).pushBack(this, timeMs, pos.x, pos.y, TouchType.MOVE);
    }

    public void addUp(final int timeMs, final int pid) {
        if (pid > cursorMoves.size() || isSaving) return;
        cursorMoves.get(pid).pushBack(timeMs, TouchType.UP);
    }

    public void save(final String filename) {
        isSaving = true;

        for (int i = 0; i < cursorMoves.size(); i++)
            Debug.i("Replay contains " + cursorMoves.get(i).size + " moves for finger " + i);
        Debug.i("Skipped " + pointsSkipped + " points");
        Debug.i("Replay contains " + objectData.length + " objects");

        try (var zip = new ZipOutputStream(new FileOutputStream(filename))) {
            zip.setMethod(ZipOutputStream.DEFLATED);
            zip.setLevel(Deflater.DEFAULT_COMPRESSION);
            zip.putNextEntry(new ZipEntry("data"));

            try (var os = new ObjectOutputStream(zip)) {
                os.writeObject(new ReplayVersion());
                os.writeObject(beatmapsetName);
                os.writeObject(beatmapName);
                os.writeObject(md5);

                if (stat != null) {
                    os.writeLong(stat.getTime());
                    os.writeInt(stat.getHit300k());
                    os.writeInt(stat.getHit300());
                    os.writeInt(stat.getHit100k());
                    os.writeInt(stat.getHit100());
                    os.writeInt(stat.getHit50());
                    os.writeInt(stat.getMisses());
                    os.writeInt(stat.getTotalScoreWithMultiplier());
                    os.writeInt(stat.getScoreMaxCombo());
                    os.writeObject(stat.getPlayerName());
                    os.writeObject(stat.getMod().serializeMods().toString());
                }

                os.writeInt(cursorMoves.size());
                //Storing all moves
                for (final MoveArray move : cursorMoves) {
                    move.writeTo(os);
                }
                os.writeInt(objectData.length);
                for (ReplayObjectData data : objectData) {
                    if (data == null) data = new ReplayObjectData();
                    os.writeShort(data.accuracy);
                    if (data.tickSet == null || data.tickSet.isEmpty()) {
                        os.writeByte(0);
                    } else {
                        byte[] bytes = new byte[(data.tickSet.length() + 7) / 8];
                        for (int i = 0; i < data.tickSet.length(); i++) {
                            if (data.tickSet.get(i)) {
                                bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
                            }
                        }
                        os.writeByte(bytes.length);
                        os.write(bytes);
                    }
                    os.writeByte(data.result);
                }
            }
        } catch (final IOException e) {
            Debug.e("IOException: " + e.getMessage(), e);
        } finally {
            isSaving = false;
        }
    }

    public boolean load(final String replayFilePath, boolean withGameplayData) {
        try (var stream = new FileInputStream(replayFilePath)) {
            return load(stream, new File(replayFilePath).getName(), withGameplayData);
        } catch (final IOException e) {
            Debug.e("Cannot load replay: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean load(InputStream inputStream, String replayFilename, boolean withGameplayData) {
        try (var zip = new ZipInputStream(inputStream)) {
            zip.getNextEntry();

            try (var os = new ObjectInputStream(zip)) {
                cursorMoves.clear();
                int version = 0;

                String mBeatmapsetName;
                Object firstObject = os.readObject();
                Debug.i("Read object: " + firstObject.getClass().getName());
                if (firstObject.getClass().equals(ReplayVersion.class)) {
                    Debug.i("Other replay version");
                    version = ((ReplayVersion) firstObject).version;
                    replayVersion = version;
                    mBeatmapsetName = (String) os.readObject();
                } else {
                    mBeatmapsetName = (String) firstObject;
                }
                String mBeatmapName = (String) os.readObject();
                String mMD5 = (String) os.readObject();

                beatmapsetName = mBeatmapsetName;
                beatmapName = mBeatmapName;
                md5 = mMD5;

                if (version >= 3) {
                    stat = new StatisticV2();
                    stat.setReplayFilename(replayFilename);
                    stat.setBeatmapMD5(md5);
                    stat.setTime(os.readLong());
                    stat.setHit300k(os.readInt());
                    stat.setHit300(os.readInt());
                    stat.setHit100k(os.readInt());
                    stat.setHit100(os.readInt());
                    stat.setHit50(os.readInt());
                    stat.setMisses(os.readInt());
                    stat.setForcedScore(os.readInt());
                    stat.setScoreMaxCombo(os.readInt());

                    if (version < 6) {
                        // Consume `accuracy` in replays older than v6 (which is not used anymore as the result is derived from hit results)
                        os.readFloat();

                        // Consume `perfect` in replays older than v6 (which is not used anymore as the result is derived from hit results)
                        os.readBoolean();
                    }

                    stat.setPlayerName((String) os.readObject());

                    if (version >= 7) {
                        var modJsonStr = (String) os.readObject();
                        stat.setMod(ModUtils.deserializeMods(modJsonStr));
                    } else {
                        //noinspection unchecked
                        var mod = (EnumSet<GameMod>) os.readObject();
                        var extraModString = "";

                        if (version >= 4) {
                            extraModString = (String) os.readObject();
                        }

                        stat.setMod(LegacyModConverter.convert(mod, extraModString));
                        stat.getMod().put(new ModReplayV6());
                    }
                }

                if (withGameplayData) {
                    int msize = os.readInt();
                    cursorIndex = new int[msize];
                    lastMoveIndex = new int[msize];

                    for (int i = 0; i < msize; i++) {
                        cursorMoves.add(MoveArray.readFrom(os, this, stat.getMod()));
                        cursorIndex[i] = 0;
                        lastMoveIndex[i] = -1;
                    }

                    os.readInt();
                    for (int i = 0; i < objectData.length; i++) {
                        ReplayObjectData data = new ReplayObjectData();
                        data.accuracy = os.readShort();
                        int len = os.readByte();
                        if (len > 0) {
                            data.tickSet = new BitSet();
                            byte[] bytes = new byte[len];
                            if (os.read(bytes) > 0) {
                                System.out.println("Read " + len + " bytes");
                            }
                            for (int j = 0; j < len * 8; j++) {
                                data.tickSet.set(j, (bytes[len - j / 8 - 1] & 1 << (j % 8)) != 0);
                            }
                        }
                        if (version >= 1) {
                            data.result = os.readByte();
                        }
                        objectData[i] = data;
                    }
                }
            }
        } catch (EOFException e) {
            Debug.e("O_o eof...");
            Debug.e(e);
            ToastLogger.showText(com.osudroid.resources.R.string.replay_corrupted, true);
            return false;
        } catch (Exception e) {
            ToastLogger.showText(com.osudroid.resources.R.string.replay_corrupted, true);
            Debug.e("Cannot load replay: " + e.getMessage(), e);
            return false;
        }

        if (withGameplayData) {
            for (int i = 0; i < cursorMoves.size(); i++)
                Debug.i("Loaded " + cursorMoves.get(i).size + " moves for finger " + i);
            Debug.i("Loaded " + objectData.length + " objects");
        }

        return true;
    }

    public StatisticV2 getStat() {
        return stat;
    }

    public void setStat(StatisticV2 stat) {
        this.stat = stat;
    }

    public String getMd5() {
        return md5;
    }

    public String getBeatmapName() {
        return beatmapName;
    }

    public String getBeatmapsetName() {
        return beatmapsetName;
    }

    /*
        Object used to store data about current replay version for compatibility purposes.
        Version 4: Adds ExtraModString's save and load in save()/load()/loadInfo()
        Version 5: Changes coordinates to use the float primitive type
        Version 6: Removed accuracy and perfect, slider ends no longer give combo when not hit
        Version 7: Reworked mod storage to not serialize GameMod, object stacking behavior overhaul, device-independent
                   object scaling
     */
    public static class ReplayVersion implements Serializable {
        private static final long serialVersionUID = 4643121693566795335L;
        int version = 7;
    }

    public static class ReplayObjectData {
        public short accuracy = 0;
        public BitSet tickSet = null;
        public byte result = 0;
    }

    public static class ReplayMovement {
        protected int time;
        protected float x;
        protected float y;
        protected TouchType touchType;

        public int getTime() {
            return time;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public TouchType getTouchType() {
            return touchType;
        }
    }

    public static class MoveArray {
        public ReplayMovement[] movements;
        public int size;
        public int allocated;

        public MoveArray(int startSize) {
            allocated = startSize;
            size = 0;
            movements = new ReplayMovement[allocated];
        }

        private static float readTouchPoint(ObjectInputStream is, @NonNull Replay replay) throws IOException {
            return replay.replayVersion < 5 ? is.readShort() : is.readFloat();
        }

        @NonNull
        public static MoveArray readFrom(@NonNull ObjectInputStream is, Replay replay, ModHashMap mods) throws IOException {
            boolean isHardRock = mods.contains(ModHardRock.class);
            int size = is.readInt();
            MoveArray array = new MoveArray(size);
            array.size = size;
            for (int i = 0; i < size; i++) {
                ReplayMovement movement = new ReplayMovement();
                array.movements[i] = movement;
                movement.time = is.readInt();
                movement.touchType = TouchType.getByID((byte) (movement.time & 3));
                movement.time >>= 2;
                if (movement.touchType != TouchType.UP) {
                    float baseX = readTouchPoint(is, replay);
                    float baseY = readTouchPoint(is, replay);
                    PointF gamePoint = new PointF(
                            baseX / Config.getTextureQuality(),
                            baseY / Config.getTextureQuality()
                    );
                    PointF realPoint = replay.replayVersion > 1 ?
                        Utils.trackToRealCoords(gamePoint, isHardRock) :
                        Utils.trackToRealCoords(
                            Utils.realToTrackCoords(gamePoint, 1024, 600, true),
                            isHardRock
                        );
                    movement.x = realPoint.x;
                    movement.y = realPoint.y;
                }
            }

            return array;
        }

        public void reallocate(int newSize) {
            if (newSize <= allocated) return;
            ReplayMovement[] newMovements = new ReplayMovement[newSize];
            System.arraycopy(movements, 0, newMovements, 0, size);
            movements = newMovements;
            allocated = newSize;
        }

        public boolean checkNewPoint(float px, float py) {
            if (size < 2) return false;

            ReplayMovement minusTwoMovement = movements[size - 2];
            ReplayMovement previousMovement = movements[size - 1];

            float tx = (px + minusTwoMovement.x) * 0.5f;
            float ty = (py + minusTwoMovement.y) * 0.5f;

            return (Utils.sqr(previousMovement.x - tx) + Utils.sqr(previousMovement.y - ty)) <= 25;
        }

        public void pushBack(Replay replay, int time, float x, float y, TouchType touchType) {
            int idx = size;
            if (touchType == TouchType.MOVE && checkNewPoint(x, y)) {
                idx = size - 1;
                replay.pointsSkipped++;
            } else {
                if (size + 1 >= allocated) {
                    reallocate((allocated * 3) / 2);
                }
                size++;
            }
            ReplayMovement movement = new ReplayMovement();
            movements[idx] = movement;
            movement.time = time;
            movement.x = x;
            movement.y = y;
            movement.touchType = touchType;
        }

        public void pushBack(int time, TouchType touchType) {
            if (size >= allocated) {
                reallocate((allocated * 3) / 2);
            }
            movements[size] = new ReplayMovement();
            ReplayMovement movement = movements[size];
            movement.time = time;
            movement.touchType = touchType;
            size++;
        }

        public void writeTo(@NonNull ObjectOutputStream os) throws IOException {
            os.writeInt(size);
            for (int i = 0; i < size; i++) {
                ReplayMovement movement = movements[i];
                os.writeInt((movement.time << 2) + movement.touchType.getId());
                if (movement.touchType != TouchType.UP) {
                    os.writeFloat(movement.x);
                    os.writeFloat(movement.y);
                }
            }
        }
    }
}
