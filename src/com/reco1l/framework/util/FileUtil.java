package com.reco1l.framework.util;

import android.text.TextUtils;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * @author Reco1l
 */
public final class FileUtil
{

    //----------------------------------------------------------------------------------------------------------------//

    private FileUtil()
    {
    }

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Get file checksum.
     *
     * @param algorithm The checksum algorithm to be used, see {@link Algorithm}.
     */
    public static String getChecksum(@Algorithm String algorithm, File file) throws Exception
    {
        var md = MessageDigest.getInstance(algorithm);

        var fis = new FileInputStream(file);
        var dis = new DigestInputStream(fis, md);

        while (true)
        {
            if (dis.read() == -1)
            {
                break;
            }
        }

        byte[] hashBytes = md.digest();

        var builder = new StringBuilder();

        for (byte b : hashBytes)
        {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Compatible implementation with APIs < 26
     */
    public static byte[] readAllBytes(File file) throws IOException
    {
        Objects.requireNonNull(file);

        int length = (int) file.length();
        var bytes = new byte[length];

        try (var stream = new FileInputStream(file))
        {
            var read = stream.read(bytes);

            if (read != length)
            {
                throw new IOException("The file wasn't fully read!");
            }
        }
        return bytes;
    }

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Creates a .txt file and prints the String text inside it.
     *
     * @param folder The desired folder where the text file will be located.
     * @param name   The file name.
     * @param text   The text to be printed into the file.
     */
    public static File createNewText(String folder, String name, String text) throws IOException
    {
        var directory = new File(folder);

        if (!directory.exists() && !directory.mkdirs())
        {
            throw new IOException("Unable to create directory!");
        }

        var file = new File(directory, name + ".txt");

        try (var fw = new FileWriter(file, true); var pw = new PrintWriter(fw))
        {
            if (!file.createNewFile())
            {
                pw.print("\n\n");
            }

            pw.println(text);
        }

        return file;
    }

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Decode in UTF8 the filename from an encoded URL String.
     */
    public static String decodeUTF8(String name)
    {
        if (TextUtils.isEmpty(name))
        {
            return name;
        }

        try
        {
            return URLDecoder.decode(name, StandardCharsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException e)
        {
            return name;
        }
    }

    /**
     * Remove all invalid characters.
     */
    public static String validateChars(String name)
    {
        if (TextUtils.isEmpty(name))
        {
            return name;
        }
        return name.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }
}
