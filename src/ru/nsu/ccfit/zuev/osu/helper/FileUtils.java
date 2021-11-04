package ru.nsu.ccfit.zuev.osu.helper;

import android.os.Build;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

// TODO: Implement zhanghai/AndroidRetroFile
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Arrays;
import java.util.LinkedList;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import org.anddev.andengine.util.Debug;

public class FileUtils {

    private FileUtils() {}

    public static void copy(File from, File to) throws FileNotFoundException, IOException {
        try (Source source = Okio.source(from);
            BufferedSink bufferedSink = Okio.buffer(Okio.sink(to))) {
            bufferedSink.writeAll(source);
        }
    }

    public static void move(File from, File to) throws FileNotFoundException, IOException {
        copy(from, to);
        from.delete();
    }

    public static String getFileChecksum(String algorithm, File file) {
        StringBuilder sb = new StringBuilder();

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            BufferedInputStream in = new BufferedInputStream(
                new FileInputStream(file));
            byte[] byteArray = new byte[1024];
            int bytesCount = 0; 

            while((bytesCount = in.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            in.close();

            byte[] bytes = digest.digest();
            for(int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
        }catch(IOException e) {
            Debug.e("getFileChecksum " + e.getMessage(), e);
        }catch(NoSuchAlgorithmException e) {
            Debug.e(e.getMessage(), e);
        }
        return sb.toString();
    }

    public static String getMD5Checksum(File file) {
        return getFileChecksum("MD5", file);
    }

    public static String getSHA256Checksum(File file) {
        return getFileChecksum("SHA-256", file);
    }

    public static File[] listFiles(File directory) {
        return listFiles(directory, file -> true);
    }

    public static File[] listFiles(File directory, String endsWith) {
        return listFiles(directory, file ->
            file.getName().toLowerCase().endsWith(endsWith));
    }

    public static File[] listFiles(File directory, String[] endsWithExtensions) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return listFiles(directory, file -> {
                for(String extension : endsWithExtensions) {
                    if(file.getName().toLowerCase().endsWith(extension)) {
                        return true;
                    }
                }
                return false;
            });
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return listFiles(directory, file -> {
                String filename = file.getName().toLowerCase();
                return Arrays.stream(endsWithExtensions).anyMatch(filename::endsWith);
            });
        }
        return null;
    }

    public static File[] listFiles(File directory, FileUtilsFilter filter) {
        File[] filelist = null;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            filelist = directory.listFiles(pathname -> filter.accept(pathname));
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LinkedList<File> cachedFiles = new LinkedList<File>();
            DirectoryStream.Filter<Path> directoryFilter = new DirectoryStream.Filter<Path>() {
                @Override
                public boolean accept(Path entry) {
                    return filter.accept(entry.toFile());
                }
            };
            try(DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(directory.getAbsolutePath()), directoryFilter)) {
                for(Path path : stream) {
                    cachedFiles.add(path.toFile());
                }
            }catch(Exception err) {
                Debug.e("FileUtils.listFiles: " + err.getMessage(), err);
            }
            filelist = cachedFiles.toArray(new File[cachedFiles.size()]);
        }
        return filelist;
    }

    public interface FileUtilsFilter {
        boolean accept(File file);
    }

}