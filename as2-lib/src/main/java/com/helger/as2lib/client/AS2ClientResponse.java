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
package com.helger.as2lib.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.as2lib.message.AS2MessageMDN;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.commons.ValueEnforcer;

/**
 * This class contains the basic content that was received from an AS2 server as
 * a response.
 *
 * @author oleo Date: May 12, 2010 Time: 5:53:45 PM
 * @author Philip Helger
 */
public class AS2ClientResponse
{
  private String m_sOriginalMessageID;
  private Throwable m_aThrowable;
  private IMessageMDN m_aMDN;

  public AS2ClientResponse ()
  {}

  public void setOriginalMessageID (@Nonnull final String sOriginalMessageID)
  {
    ValueEnforcer.notNull (sOriginalMessageID, "OriginalMessageID");
    m_sOriginalMessageID = sOriginalMessageID;
  }

  /**
   * @return The message ID of the original AS2 message.
   */
  @Nullable
  public String getOriginalMessageID ()
  {
    return m_sOriginalMessageID;
  }

  public void setException (@Nonnull final Throwable t)
  {
    ValueEnforcer.notNull (t, "Throwable");
    m_aThrowable = t;
  }

  public boolean hasException ()
  {
    return m_aThrowable != null;
  }

  @Nullable
  public Throwable getException ()
  {
    return m_aThrowable;
  }

  public void setMDN (@Nonnull final IMessageMDN aMDN)
  {
    ValueEnforcer.notNull (aMDN, "MDN");
    m_aMDN = aMDN;
  }

  @Nullable
  public IMessageMDN getMDN ()
  {
    return m_aMDN;
  }

  @Nullable
  public String getMDNMessageID ()
  {
    return m_aMDN == null ? null : m_aMDN.getMessageID ();
  }

  @Nullable
  public String getMDNText ()
  {
    return m_aMDN == null ? null : m_aMDN.getText ();
  }

  @Nullable
  public String getMDNDisposition ()
  {
    return m_aMDN == null ? null : m_aMDN.getAttribute (AS2MessageMDN.MDNA_DISPOSITION);
  }

  @Nonnull
  public String getAsString ()
  {
    final StringBuilder aSB = new StringBuilder ();
    aSB.append ('\n');
    if (m_sOriginalMessageID != null)
      aSB.append ("OriginalMessageID: ").append (m_sOriginalMessageID).append ('\n');
    if (getMDNMessageID () != null)
      aSB.append ("MDN MessageID: ").append (getMDNMessageID ()).append ('\n');
    if (getMDNDisposition () != null)
      aSB.append ("MDN Disposition: ").append (getMDNDisposition ()).append ('\n');
    if (hasException ())
      aSB.append ("Error message: ").append (m_aThrowable.getMessage ()).append ('\n');
    if (getMDNText () != null)
      aSB.append ("MDN Text: ").append (getMDNText ()).append ('\n');
    return aSB.toString ();
  }
}
