/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2016 Philip Helger philip[at]helger[dot]com
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
package com.helger.as2lib.util.http;

import java.net.HttpURLConnection;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;

/**
 * Implementation of {@link IAS2HttpHeaderWrapper} for {@link HttpURLConnection}
 * .
 *
 * @author Philip Helger
 */
@Immutable
public final class AS2HttpHeaderWrapperHttpURLConnection implements IAS2HttpHeaderWrapper
{
  private final HttpURLConnection m_aConn;

  public AS2HttpHeaderWrapperHttpURLConnection (@Nonnull final HttpURLConnection aConn)
  {
    m_aConn = ValueEnforcer.notNull (aConn, "Connection");
  }

  public void setHttpHeader (@Nonnull final String sName, @Nonnull final String sValue)
  {
    if (sValue != null)
    {
      // Avoid having header values spanning multiple lines!
      // This has been deprecated by RFC 7230 and Jetty 9.3 refuses to parse
      // these
      // requests with HTTP 400 by default
      final String sHeaderValue = sValue.replace ('\t', ' ').replace ('\n', ' ').replace ('\r', ' ');

      m_aConn.setRequestProperty (sName, sHeaderValue);
    }
    else
      m_aConn.setRequestProperty (sName, "");
  }
}
