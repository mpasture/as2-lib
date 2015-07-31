/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2015 Philip Helger philip[at]helger[dot]com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE FREEBSD PROJECT ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE FREEBSD PROJECT OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 */
package com.helger.as2lib.util.cert;

import java.io.InputStream;
import java.security.KeyStore;

import javax.annotation.Nonnull;

import com.helger.as2lib.crypto.ICryptoHelper;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.stream.StreamHelper;

/**
 * KeyStore reader helper class
 *
 * @author Philip Helger
 */
public final class KeyStoreReader
{
  private KeyStoreReader ()
  {}

  @Nonnull
  public static OpenAS2KeyStore read (@Nonnull final String sFilename,
                                      @Nonnull final char [] aPassword,
                                      @Nonnull final ICryptoHelper aCryptoHelper) throws Exception
  {
    final InputStream aIS = FileHelper.getInputStream (sFilename);
    if (aIS == null)
      throw new IllegalArgumentException ("Failed to open KeyStore '" + sFilename + "' for reading");

    try
    {
      final KeyStore aKeyStore = aCryptoHelper.loadKeyStore (aIS, aPassword);
      return new OpenAS2KeyStore (aKeyStore);
    }
    finally
    {
      StreamHelper.close (aIS);
    }
  }
}
