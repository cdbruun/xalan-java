/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xalan" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, Lotus
 * Development Corporation., http://www.lotus.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.xml.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;


/**
 * This class writes ASCII to a byte stream as quickly as possible.  For the
 * moment it does not do buffering, though I reserve the right to do some
 * buffering down the line if I can prove that it will be faster even if the
 * output stream is buffered.
 */
public final class WriterToUTF8Buffered extends Writer
{
    
  /** number of characters that the buffer can hold.
   * This is a fixed constant is used rather than m_outputBytes.lenght for performance.
   */
  private static final int buf_length=16*1024;
  
  /** The byte stream to write to. (sc & sb remove final to compile in JDK 1.1.8) */
  private final OutputStream m_os;

  /**
   * The internal buffer where data is stored.
   * (sc & sb remove final to compile in JDK 1.1.8)
   */
  private final byte m_outputBytes[];
  
  private final char m_inputChars[];

  /**
   * The number of valid bytes in the buffer. This value is always
   * in the range <tt>0</tt> through <tt>m_outputBytes.length</tt>; elements
   * <tt>m_outputBytes[0]</tt> through <tt>m_outputBytes[count-1]</tt> contain valid
   * byte data.
   */
  private int count;

  /**
   * Create an buffered UTF-8 writer.
   *
   *
   * @param   out    the underlying output stream.
   *
   * @throws UnsupportedEncodingException
   */
  public WriterToUTF8Buffered(OutputStream out)
          throws UnsupportedEncodingException
  {
      m_os = out;
      // get 3 extra bytes to make buffer overflow checking simpler and faster
      // we won't have to keep checking for a few extra characters
      m_outputBytes = new byte[buf_length + 3];
      
      // Big enough to hold the input chars that will be transformed
      // into output bytes in m_ouputBytes.
      m_inputChars = new char[(buf_length/3) + 1];
      count = 0;
      
//      the old body of this constructor, before the buffersize was changed to a constant      
//      this(out, 8*1024);
  }

  /**
   * Create an buffered UTF-8 writer to write data to the
   * specified underlying output stream with the specified buffer
   * size.
   *
   * @param   out    the underlying output stream.
   * @param   size   the buffer size.
   * @exception IllegalArgumentException if size <= 0.
   */
//  public WriterToUTF8Buffered(final OutputStream out, final int size)
//  {
//
//    m_os = out;
//
//    if (size <= 0)
//    {
//      throw new IllegalArgumentException(
//        SerializerMessages.createMessage(SerializerErrorResources.ER_BUFFER_SIZE_LESSTHAN_ZERO, null)); //"Buffer size <= 0");
//    }
//
//    m_outputBytes = new byte[size];
//    count = 0;
//  }

  /**
   * Write a single character.  The character to be written is contained in
   * the 16 low-order bits of the given integer value; the 16 high-order bits
   * are ignored.
   *
   * <p> Subclasses that intend to support efficient single-character output
   * should override this method.
   *
   * @param c  int specifying a character to be written.
   * @exception  IOException  If an I/O error occurs
   */
  public void write(final int c) throws IOException
  {
    
    /* If we are close to the end of the buffer then flush it.
     * Remember the buffer can hold a few more characters than buf_length
     */ 
    if (count >= buf_length)
        flushBuffer();

    if (c < 0x80)
    {
       m_outputBytes[count++] = (byte) (c);
    }
    else if (c < 0x800)
    {
      m_outputBytes[count++] = (byte) (0xc0 + (c >> 6));
      m_outputBytes[count++] = (byte) (0x80 + (c & 0x3f));
    }
    else
    {
      m_outputBytes[count++] = (byte) (0xe0 + (c >> 12));
      m_outputBytes[count++] = (byte) (0x80 + ((c >> 6) & 0x3f));
      m_outputBytes[count++] = (byte) (0x80 + (c & 0x3f));
    }
  }

  /**
   * Write a portion of an array of characters.
   *
   * @param  chars  Array of characters
   * @param  start   Offset from which to start writing characters
   * @param  length   Number of characters to write
   *
   * @exception  IOException  If an I/O error occurs
   *
   * @throws java.io.IOException
   */
  private final void writeWithoutBuffering(
          final char chars[], final int start, final int length)
            throws java.io.IOException
  {

    final OutputStream os = m_os;

    final int n = length+start;
    for (int i = start; i < n; i++)
    {
      final char c = chars[i];

      if (c < 0x80)
        os.write(c);
      else if (c < 0x800)
      {
        os.write(0xc0 + (c >> 6));
        os.write(0x80 + (c & 0x3f));
      }
      else
      {
        os.write(0xe0 + (c >> 12));
        os.write(0x80 + ((c >> 6) & 0x3f));
        os.write(0x80 + (c & 0x3f));
      }
    }
  }

  /**
   * Write a string.
   *
   * @param  s  String to be written
   *
   * @exception  IOException  If an I/O error occurs
   */
  private final void writeWithoutBuffering(final String s) throws IOException
  {

    final int n = s.length();
    final OutputStream os = m_os;

    for (int i = 0; i < n; i++)
    {
      final char c = s.charAt(i);

      if (c < 0x80)
        os.write(c);
      else if (c < 0x800)
      {
        os.write(0xc0 + (c >> 6));
        os.write(0x80 + (c & 0x3f));
      }
      else
      {
        os.write(0xe0 + (c >> 12));
        os.write(0x80 + ((c >> 6) & 0x3f));
        os.write(0x80 + (c & 0x3f));
      }
    }
  }

  /**
   * Write a portion of an array of characters.
   *
   * @param  chars  Array of characters
   * @param  start   Offset from which to start writing characters
   * @param  length   Number of characters to write
   *
   * @exception  IOException  If an I/O error occurs
   *
   * @throws java.io.IOException
   */
  public void write(final char chars[], final int start, final int length)
          throws java.io.IOException
  {

    // We multiply the length by three since this is the maximum length
    // of the characters that we can put into the buffer.  It is possible
    // for each Unicode character to expand to three bytes.

    int lengthx3 = (length << 1) + length;

    if (lengthx3 >= buf_length - count)
    {
      // The requested length is greater than the unused part of the buffer
      flushBuffer();

      if (lengthx3 >= buf_length)
      {
        /*
         * The requested length exceeds the size of the buffer,
         * so don't bother to buffer this one, just write it out
         * directly. The buffer is already flushed so this is a 
         * safe thing to do.
         */
        writeWithoutBuffering(chars, start, length);
        return;
      }
    }



    final int n = length+start;
    final byte[] buf_loc = m_outputBytes; // local reference for faster access
    int count_loc = count;      // local integer for faster access
    int i = start;
    {
        /* This block could be omitted and the code would produce
         * the same result. But this block exists to give the JIT
         * a better chance of optimizing a tight and common loop which
         * occurs when writing out ASCII characters. 
         */ 
        char c;
        for(; i < n && (c = chars[i])< 0x80 ; i++ )
            buf_loc[count_loc++] = (byte)c;
    }
    for (; i < n; i++)
    {

      final char c = chars[i];

      if (c < 0x80)
        buf_loc[count_loc++] = (byte) (c);
      else if (c < 0x800)
      {
        buf_loc[count_loc++] = (byte) (0xc0 + (c >> 6));
        buf_loc[count_loc++] = (byte) (0x80 + (c & 0x3f));
      }
      else
      {
        buf_loc[count_loc++] = (byte) (0xe0 + (c >> 12));
        buf_loc[count_loc++] = (byte) (0x80 + ((c >> 6) & 0x3f));
        buf_loc[count_loc++] = (byte) (0x80 + (c & 0x3f));
      }
    }
    // Store the local integer back into the instance variable
    count = count_loc;

  }

  /**
   * Write a string.
   *
   * @param  s  String to be written
   *
   * @exception  IOException  If an I/O error occurs
   */
  public void write(final String s) throws IOException
  {

    // We multiply the length by three since this is the maximum length
    // of the characters that we can put into the buffer.  It is possible
    // for each Unicode character to expand to three bytes.
    final int length = s.length();
    int lengthx3 = (length << 1) + length;

    if (lengthx3 >= buf_length - count)
    {
      // The requested length is greater than the unused part of the buffer
      flushBuffer();

      if (lengthx3 >= buf_length)
      {
        /*
         * The requested length exceeds the size of the buffer,
         * so don't bother to buffer this one, just write it out
         * directly. The buffer is already flushed so this is a 
         * safe thing to do.
         */
        writeWithoutBuffering(s);
        return;
      }
    }


    s.getChars(0, length , m_inputChars, 0);
    final char[] chars = m_inputChars;
    final int n = length;
    final byte[] buf_loc = m_outputBytes; // local reference for faster access
    int count_loc = count;      // local integer for faster access
    int i = 0;
    {
        /* This block could be omitted and the code would produce
         * the same result. But this block exists to give the JIT
         * a better chance of optimizing a tight and common loop which
         * occurs when writing out ASCII characters. 
         */ 
        char c;
        for(; i < n && (c = chars[i])< 0x80 ; i++ )
            buf_loc[count_loc++] = (byte)c;
    }
    for (; i < n; i++)
    {

      final char c = chars[i];

      if (c < 0x80)
        buf_loc[count_loc++] = (byte) (c);
      else if (c < 0x800)
      {
        buf_loc[count_loc++] = (byte) (0xc0 + (c >> 6));
        buf_loc[count_loc++] = (byte) (0x80 + (c & 0x3f));
      }
      else
      {
        buf_loc[count_loc++] = (byte) (0xe0 + (c >> 12));
        buf_loc[count_loc++] = (byte) (0x80 + ((c >> 6) & 0x3f));
        buf_loc[count_loc++] = (byte) (0x80 + (c & 0x3f));
      }
    }
    // Store the local integer back into the instance variable
    count = count_loc;

  }

  /**
   * Flush the internal buffer
   *
   * @throws IOException
   */
  public void flushBuffer() throws IOException
  {

    if (count > 0)
    {
      m_os.write(m_outputBytes, 0, count);

      count = 0;
    }
  }

  /**
   * Flush the stream.  If the stream has saved any characters from the
   * various write() methods in a buffer, write them immediately to their
   * intended destination.  Then, if that destination is another character or
   * byte stream, flush it.  Thus one flush() invocation will flush all the
   * buffers in a chain of Writers and OutputStreams.
   *
   * @exception  IOException  If an I/O error occurs
   *
   * @throws java.io.IOException
   */
  public void flush() throws java.io.IOException
  {
    flushBuffer();
    m_os.flush();
  }

  /**
   * Close the stream, flushing it first.  Once a stream has been closed,
   * further write() or flush() invocations will cause an IOException to be
   * thrown.  Closing a previously-closed stream, however, has no effect.
   *
   * @exception  IOException  If an I/O error occurs
   *
   * @throws java.io.IOException
   */
  public void close() throws java.io.IOException
  {
    flushBuffer();
    m_os.close();
  }

  /**
   * Get the output stream where the events will be serialized to.
   *
   * @return reference to the result stream, or null of only a writer was
   * set.
   */
  public OutputStream getOutputStream()
  {
    return m_os;
  }
  
  /**
   * 
   * @param s A string with only ASCII characters
   * @throws IOException
   */
  public void directWrite(final String s) throws IOException
  {

    // We multiply the length by three since this is the maximum length
    // of the characters that we can put into the buffer.  It is possible
    // for each Unicode character to expand to three bytes.
    final int length = s.length();
    int lengthx3 = (length << 1) + length;

    if (lengthx3 >= buf_length - count)
    {
      // The requested length is greater than the unused part of the buffer
      flushBuffer();

      if (lengthx3 >= buf_length)
      {
        /*
         * The requested length exceeds the size of the buffer,
         * so don't bother to buffer this one, just write it out
         * directly. The buffer is already flushed so this is a 
         * safe thing to do.
         */
        writeWithoutBuffering(s);
        return;
      }
    }


    s.getChars(0, length , m_inputChars, 0);
    final char[] chars = m_inputChars;
    final byte[] buf_loc = m_outputBytes; // local reference for faster access
    int count_loc = count;      // local integer for faster access
    int i = 0;
    while( i < length) 
        buf_loc[count_loc++] = (byte)chars[i++];

 
    // Store the local integer back into the instance variable
    count = count_loc;

  }
}
