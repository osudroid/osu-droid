package ru.nsu.ccfit.zuev.osu.scoring;

import android.graphics.PointF;

import androidx.annotation.NonNull;

import org.anddev.andengine.util.Debug;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osuplus.R;

public class Replay {
    public static EnumSet<GameMod> mod = EnumSet.noneOf(GameMod.class);
    public static EnumSet<GameMod> oldMod = EnumSet.noneOf(GameMod.class);
    protected int pointsSkipped = 0;
    public ArrayList<MoveArray> cursorMoves = new ArrayList<>();
    public int[] cursorIndex;
    public int[] lastMoveIndex;
    public ReplayObjectData[] objectData = null;
    public int replayVersion;
    public StatisticV2 stat = null;
    private String md5 = "";
    private String mapFile = "";
    private String mapName = "";
    public static float oldChangeSpeed = 1.0f;
    public static float oldForceAR = 9.0f;
    public static boolean oldEnableForceAR = false;
    public static float oldFLFollowDelay = FlashLightEntity.defaultMoveDelayS;

    public Replay() {
        cursorMoves.add(new MoveArray(200));
        cursorMoves.add(new MoveArray(50));
        for (int i = 2; i < GameScene.CursorCount; i++) {
            cursorMoves.add(new MoveArray(15));
        }
        cursorIndex = new int[GameScene.CursorCount];
        lastMoveIndex = new int[GameScene.CursorCount];
        for (int i = 0; i < GameScene.CursorCount; i++) {
            cursorIndex[i] = 0;
            lastMoveIndex[i] = -1;
        }
    }

    public void setMap(String mapName, String file, String md5) {
        this.mapName = mapName;
        this.md5 = md5;
        this.mapFile = file;
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

    public void addPress(final float time, final PointF pos, final int pid) {
        if (pid > GameScene.CursorCount) return;
        int itime = Math.max(0, (int) (time * 1000));
        cursorMoves.get(pid).pushBack(this, itime, pos.x, pos.y, TouchType.DOWN);
    }

    public void addMove(final float time, final PointF pos, final int pid) {
        if (pid > GameScene.CursorCount) return;
        int itime = Math.max(0, (int) (time * 1000));
        cursorMoves.get(pid).pushBack(this, itime, pos.x, pos.y, TouchType.MOVE);
    }

    public void addUp(final float time, final int pid) {
        if (pid > GameScene.CursorCount) return;
        int itime = Math.max(0, (int) (time * 1000));
        cursorMoves.get(pid).pushBack(itime, TouchType.UP);
    }

    public void save(final String filename) {
        for (int i = 0; i < cursorMoves.size(); i++)
            Debug.i("Replay contains " + cursorMoves.get(i).size + " moves for finger " + i);
        Debug.i("Skipped " + pointsSkipped + " points");
        Debug.i("Replay contains " + objectData.length + " objects");
        ObjectOutputStream os;
        ZipOutputStream zip;
        try {
            zip = new ZipOutputStream(new FileOutputStream(filename));
            zip.setMethod(ZipOutputStream.DEFLATED);
            zip.setLevel(Deflater.DEFAULT_COMPRESSION);
            zip.putNextEntry(new ZipEntry("data"));
            os = new ObjectOutputStream(zip);
        } catch (final FileNotFoundException e) {
            Debug.e("File not found " + filename, e);
            return;
        } catch (final IOException e) {
            Debug.e("IOException: " + e.getMessage(), e);
            return;
        }

        try {
            os.writeObject(new ReplayVersion());
            os.writeObject(mapName);
            os.writeObject(mapFile);
            os.writeObject(md5);

            if (stat != null) {
                os.writeLong(stat.getTime());
                os.writeInt(stat.getHit300k());
                os.writeInt(stat.getHit300());
                os.writeInt(stat.getHit100k());
                os.writeInt(stat.getHit100());
                os.writeInt(stat.getHit50());
                os.writeInt(stat.getMisses());
                os.writeInt(stat.getModifiedTotalScore());
                os.writeInt(stat.getMaxCombo());
                os.writeFloat(stat.getAccuracy());
                os.writeBoolean(stat.isPerfect());
                os.writeObject(stat.getPlayerName());
                os.writeObject(stat.getMod());
                //Add in replay version 4
                os.writeObject(stat.getExtraModString());
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
                if (data.tickSet == null || data.tickSet.length() == 0) {
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
        } catch (final IOException e) {
            Debug.e("IOException: " + e.getMessage(), e);
            return;
        }

        try {
            os.flush();
            zip.flush();
            zip.closeEntry();
            zip.flush();
        } catch (final IOException e) {
            Debug.e("IOException: " + e.getMessage(), e);
        }

    }

    @SuppressWarnings("unchecked")
    public boolean loadInfo(final String filename) {
        ObjectInputStream os;
        try {
            final ZipInputStream zip = new ZipInputStream(new FileInputStream(filename));
            zip.getNextEntry();
            os = new ObjectInputStream(zip);
            // zip.close();
        } catch (final Exception e) {
            Debug.e("Cannot load replay: " + e.getMessage(), e);
            return false;
        }

        Debug.i("Loading replay " + filename);

        cursorMoves.clear();
        int version = 0;
        try {
            Object firstObject = os.readObject();
            Debug.i("Readed object: " + firstObject.getClass().getName());
            if (firstObject.getClass().equals(ReplayVersion.class)) {
                Debug.i("Other replay version");
                version = ((ReplayVersion) firstObject).version;
                replayVersion = version;
                mapName = (String) os.readObject();
            } else {
                mapName = (String) firstObject;
            }
            mapFile = (String) os.readObject();
            md5 = (String) os.readObject();

            Debug.i(mapName);
            Debug.i(mapFile);
            Debug.i(md5);

            if (version >= 3) {
                stat = new StatisticV2();
                stat.setTime(os.readLong());
                stat.setHit300k(os.readInt());
                stat.setHit300(os.readInt());
                stat.setHit100k(os.readInt());
                stat.setHit100(os.readInt());
                stat.setHit50(os.readInt());
                stat.setMisses(os.readInt());
                stat.setForcedScore(os.readInt());
                stat.setMaxCombo(os.readInt());
                stat.setAccuracy(os.readFloat());
                stat.setPerfect(os.readBoolean());
                stat.setPlayerName((String) os.readObject());
                stat.setMod((EnumSet<GameMod>) os.readObject());
            }

            if (version >= 4) {
                stat.setExtraModFromString((String) os.readObject());
            }

        } catch (EOFException e) {
            Debug.e("O_o eof...");
            ToastLogger.showTextId(R.string.replay_corrupted, true);
            return false;

        } catch (Exception e) {
            ToastLogger.showTextId(R.string.replay_corrupted, true);
            Debug.e("Cannot load replay: " + e.getMessage(), e);
            return false;
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public boolean load(final String filename) {
        ObjectInputStream os;
        try {
            final ZipInputStream zip = new ZipInputStream(new FileInputStream(filename));
            zip.getNextEntry();
            os = new ObjectInputStream(zip);
            // zip.close();
        } catch (final Exception e) {
            Debug.e("Cannot load replay: " + e.getMessage(), e);
            return false;
        }

        Debug.i("Loading replay " + filename);

        cursorMoves.clear();
        int version = 0;
        try {
            String mName;
            Object firstObject = os.readObject();
            Debug.i("Read object: " + firstObject.getClass().getName());
            if (firstObject.getClass().equals(ReplayVersion.class)) {
                Debug.i("Other replay version");
                version = ((ReplayVersion) firstObject).version;
                replayVersion = version;
                mName = (String) os.readObject();
            } else {
                mName = (String) firstObject;
            }
            String mFile = (String) os.readObject();
            String mmd5 = (String) os.readObject();

            if (!mName.equals(mapName) && !mFile.equals(mapFile)) {
                Debug.i("Replay doesn't match the map!");
                Debug.i(mapName + " ::: " + mName);
                Debug.i(mapFile + " ::: " + mFile);
                Debug.i(md5 + " ::: " + mmd5);
                ToastLogger.showTextId(R.string.replay_wrongmap, true);
                os.close();
                return false;
            }

            if (version >= 3) {
                stat = new StatisticV2();
                stat.setTime(os.readLong());
                stat.setHit300k(os.readInt());
                stat.setHit300(os.readInt());
                stat.setHit100k(os.readInt());
                stat.setHit100(os.readInt());
                stat.setHit50(os.readInt());
                stat.setMisses(os.readInt());
                stat.setForcedScore(os.readInt());
                stat.setMaxCombo(os.readInt());
                stat.setAccuracy(os.readFloat());
                stat.setPerfect(os.readBoolean());
                stat.setPlayerName((String) os.readObject());
                stat.setMod((EnumSet<GameMod>) os.readObject());
            }

            if  (version >= 4) {
                stat.setExtraModFromString((String) os.readObject());
            }

            int msize = os.readInt();
            for (int i = 0; i < msize; i++) {
                cursorMoves.add(MoveArray.readFrom(os, this));
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
        } catch (EOFException e) {
            Debug.e("O_o eof...");
            Debug.e(e);
            ToastLogger.showTextId(R.string.replay_corrupted, true);
            return false;

        } catch (Exception e) {
            ToastLogger.showTextId(R.string.replay_corrupted, true);
            Debug.e("Cannot load replay: " + e.getMessage(), e);
            return false;
        }

        for (int i = 0; i < cursorMoves.size(); i++)
            Debug.i("Loaded " + cursorMoves.get(i).size + " moves for finger " + i);
        Debug.i("Loaded " + objectData.length + " objects");
        return true;
    }

    public void countMarks(float difficulty) {
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

    public String getMapFile() {
        return mapFile;
    }

    public String getMapName() {
        return mapName;
    }

    /*
        Object used to store data about current replay version for compatibility purposes.
        Version 4: Adds ExtraModString's save and load in save()/load()/loadInfo()
        Version 5: Changes coordinates to use the float primitive type
     */
    public static class ReplayVersion implements Serializable {
        private static final long serialVersionUID = 4643121693566795335L;
        int version = 5;
    }

    public static class ReplayObjectData {
        public short accuracy = 0;
        public BitSet tickSet = null;
        public byte result = 0;
    }

    public static class ReplayMovement {
        protected int time;
        protected PointF point = new PointF();
        protected TouchType touchType;

        public int getTime() {
            return time;
        }

        public PointF getPoint() {
            return point;
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
        public static MoveArray readFrom(@NonNull ObjectInputStream is, Replay replay) throws IOException {
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
                            Utils.trackToRealCoords(gamePoint) :
                            Utils.trackToRealCoords(
                                    Utils.realToTrackCoords(gamePoint, 1024, 600, true)
                            );
                    movement.point.set(realPoint);
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

            float tx = (px + minusTwoMovement.point.x) * 0.5f;
            float ty = (py + minusTwoMovement.point.y) * 0.5f;

            return (Utils.sqr(previousMovement.point.x - tx) + Utils.sqr(previousMovement.point.y - ty)) <= 25;
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
            movement.point.x = x;
            movement.point.y = y;
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
                    os.writeFloat(movement.point.x * Config.getTextureQuality());
                    os.writeFloat(movement.point.y * Config.getTextureQuality());
                }
            }
        }
    }
}
