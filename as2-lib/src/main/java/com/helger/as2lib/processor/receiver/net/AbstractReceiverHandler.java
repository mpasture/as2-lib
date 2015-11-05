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
package com.helger.as2lib.processor.receiver.net;

import java.io.IOException;
import java.net.Socket;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.message.IMessage;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.EContentTransferEncoding;
import com.helger.as2lib.util.http.HTTPHelper;
import com.helger.as2lib.util.http.IAS2HttpResponseHandler;
import com.helger.as2lib.util.http.IAS2InputStreamProvider;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.codec.IDecoder;
import com.helger.commons.codec.IdentityCodec;
import com.helger.commons.string.StringHelper;

/**
 * Abstract base class for Message and MDN receive handlers.
 *
 * @author Philip Helger
 */
public abstract class AbstractReceiverHandler implements INetModuleHandler
{
  /** The value of the Content-Transfer-Encoding header (if provided) */
  public static final String MA_HTTP_ORIGINAL_CONTENT_TRANSFER_ENCODING = "HTTP_ORIGINAL_CONTENT_TRANSFER_ENCODING";
  /**
   * The original content length before any eventual decoding (only if
   * Content-Transfer-Encoding is provided)
   */
  public static final String MA_HTTP_ORIGINAL_CONTENT_LENGTH = "HTTP_ORIGINAL_CONTENT_LENGTH";

  private static final Logger s_aLogger = LoggerFactory.getLogger (AbstractReceiverHandler.class);

  @Nonnull
  @Nonempty
  public String getClientInfo (@Nonnull final Socket aSocket)
  {
    return aSocket.getInetAddress ().getHostAddress () + ":" + aSocket.getPort ();
  }

  @Nonnull
  protected byte [] readAndDecodeHttpRequest (@Nonnull final IAS2InputStreamProvider aISP, @Nonnull final IAS2HttpResponseHandler aResponseHandler, @Nonnull final IMessage aMsg) throws IOException,
                                                                                                                                                                                  MessagingException
  {
    // Main read
    byte [] aPayload = HTTPHelper.readHttpRequest (aISP, aResponseHandler, aMsg);

    // Check the transfer encoding of the request. If none is provided, check
    // the partnership for a default one. If none is in the partnership used the
    // default one
    final String sContentTransferEncoding = aMsg.getHeader (CAS2Header.HEADER_CONTENT_TRANSFER_ENCODING,
                                                            aMsg.getPartnership ().getContentTransferEncodingReceive (EContentTransferEncoding.AS2_DEFAULT.getID ()));
    if (StringHelper.hasText (sContentTransferEncoding))
    {
      final EContentTransferEncoding eCTE = EContentTransferEncoding.getFromIDCaseInsensitiveOrNull (sContentTransferEncoding);
      if (eCTE == null)
        s_aLogger.warn ("Unsupported Content-Transfer-Encoding '" + sContentTransferEncoding + "' is used - ignoring!");
      else
      {
        // Decode data if necessary
        final IDecoder <byte []> aDecoder = eCTE.createDecoder ();
        if (!(aDecoder instanceof IdentityCodec <?>))
        {
          // Remember original length before continuing
          final int nOriginalContentLength = aPayload.length;

          s_aLogger.info ("Incoming message uses Content-Transfer-Encoding '" + sContentTransferEncoding + "' - decoding");
          aPayload = aDecoder.getDecoded (aPayload);

          // Remember that we potentially did something
          aMsg.setAttribute (MA_HTTP_ORIGINAL_CONTENT_TRANSFER_ENCODING, sContentTransferEncoding);
          aMsg.setAttribute (MA_HTTP_ORIGINAL_CONTENT_LENGTH, Integer.toString (nOriginalContentLength));
        }
      }
    }

    return aPayload;
  }
}
