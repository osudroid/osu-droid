package org.anddev.andengine.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 15:48:56 - 03.09.2009
 */
public class StreamUtils {
	// ===========================================================
	// Constants
	// ===========================================================

	public static final int IO_BUFFER_SIZE = 8 * 1024;

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public static String readFully(final InputStream pInputStream) throws IOException {
		/*final StringBuilder sb = new StringBuilder();
		final Scanner sc = new Scanner(pInputStream);
		while(sc.hasNextLine()) {
			sb.append(sc.nextLine());
		}
		return sb.toString();*/
		return new String(streamToBytes(pInputStream), StandardCharsets.UTF_8);
	}

	public static byte[] streamToBytes(final InputStream pInputStream) throws IOException {
		return StreamUtils.streamToBytes(pInputStream, -1);
	}

	public static byte[] streamToBytes(final InputStream pInputStream, final int pReadLimit) throws IOException {
		final ByteArrayOutputStream os = new ByteArrayOutputStream((pReadLimit == -1) ? IO_BUFFER_SIZE : pReadLimit);
		StreamUtils.copy(pInputStream, os, pReadLimit);
		return os.toByteArray();
	}

	public static void copy(final InputStream pInputStream, final OutputStream pOutputStream) throws IOException {
		StreamUtils.copy(pInputStream, pOutputStream, -1);
	}
	
	public static void copy(final InputStream pInputStream, final byte[] pData) throws IOException {
		int dataOffset = 0;
		final byte[] buf = new byte[IO_BUFFER_SIZE];
		int read;
		while((read = pInputStream.read(buf)) != -1) {
			System.arraycopy(buf, 0, pData, dataOffset, read);
			dataOffset += read;
		}
	}
	
	public static void copy(final InputStream pInputStream, final ByteBuffer pByteBuffer) throws IOException {
		final byte[] buf = new byte[IO_BUFFER_SIZE];
		int read;
		while((read = pInputStream.read(buf)) != -1) {
			pByteBuffer.put(buf, 0, read);
		}
	}

	/**
	 * Copy the content of the input stream into the output stream, using a temporary
	 * byte array buffer whose size is defined by {@link #IO_BUFFER_SIZE}.
	 *
	 * @param pInputStream The input stream to copy from.
	 * @param pOutputStream The output stream to copy to.
	 * @param pByteLimit not more than so much bytes to read, or unlimited if smaller than 0.
	 *
	 * @throws IOException If any error occurs during the copy.
	 */
	public static void copy(final InputStream pInputStream, final OutputStream pOutputStream, final long pByteLimit) throws IOException {
		if(pByteLimit < 0) {
			final byte[] buf = new byte[IO_BUFFER_SIZE];
			int read;
			while((read = pInputStream.read(buf)) != -1) {
				pOutputStream.write(buf, 0, read);
			}
		} else {
			final byte[] buf = new byte[IO_BUFFER_SIZE];
			final int bufferReadLimit = Math.min((int)pByteLimit, IO_BUFFER_SIZE);
			long pBytesLeftToRead = pByteLimit;
			
			int read;
			while((read = pInputStream.read(buf, 0, bufferReadLimit)) != -1) {
				if(pBytesLeftToRead > read) {
					pOutputStream.write(buf, 0, read);
					pBytesLeftToRead -= read;
				} else {
					pOutputStream.write(buf, 0, (int) pBytesLeftToRead);
					break;
				}
			}
		}
		pOutputStream.flush();
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
