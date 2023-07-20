@file:JvmName("FileUtil")
/*
 * @author Reco1l
 */

package com.reco1l.framework.extensions

import com.reco1l.framework.util.Algorithm
import net.lingala.zip4j.ZipFile
import org.apache.commons.io.FileUtils
import java.io.*
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.*


/**
 * Creates a `.txt` file and prints the String text inside it.
 *
 * @param text   The text to be printed into the file.
 */
@Throws(IOException::class)
fun File.printText(text: String?)
{
    FileWriter(this, true).use { fw ->

        PrintWriter(fw).use { pw ->

            if (!createNewFile())
            {
                pw.print("\n\n")
            }
            pw.println(text)
        }
    }
}


/**
 * Get file checksum.
 *
 * @param algorithm The checksum algorithm to be used, see [Algorithm].
 */
@Throws(IOException::class)
fun File.getChecksum(@Algorithm algorithm: String): String
{
    val md = MessageDigest.getInstance(algorithm)

    FileInputStream(this).use { fis ->

        DigestInputStream(fis, md).use { dis ->

            while (true)
            {
                if (dis.read() == -1) break
            }

            val hashBytes = md.digest()
            val builder = StringBuilder()

            for (byte in hashBytes)
            {
                builder.append(String.format("%02x", byte))
            }

            return builder.toString()
        }
    }
}


/**
 * List all files from a specific extension.
 */
fun File.listFilesOf(extension: String): Array<File>? = listFiles { it -> it.extension == extension }?.takeUnless { it.isEmpty() }


/**
 * Move a file or a directory into another directory.
 */
fun File.moveTo(dest: File, createDestIfNotExist: Boolean) = if (isFile)
{
    FileUtils.moveDirectoryToDirectory(this, dest, createDestIfNotExist)
}
else
{
    FileUtils.moveFileToDirectory(this, dest, createDestIfNotExist)
}


/**
 * Extract all in a separate folder with the name of the file (without extension) into the desired directory.
 *
 * @throws IOException If failed it'll delete the created directory only if it was created during the extract process,
 * and throw the corresponding exception.
 * @return The destination directory where all files were extracted.
 */
fun ZipFile.extractSeparateFolder(directory: File) : File
{
    var alreadyExist = false

    val folder = File(directory, file.nameWithoutExtension).apply {

        if (exists())
        {
            alreadyExist = true
        }
        else mkdirs()

    }.takeIf { it.exists() } ?: throw IOException("Unable to create directory.")

    try
    {
        extractAll(folder.absolutePath)
    }
    catch (e: Exception)
    {
        if (!alreadyExist)
        {
            folder.delete()
        }
        throw e
    }
    return folder
}