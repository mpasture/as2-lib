/**
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2014 Philip Helger philip[at]helger[dot]com
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.security.cert.X509Certificate;

import javax.activation.DataHandler;
import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.cert.ECertificatePartnershipType;
import com.helger.as2lib.cert.ICertificateFactory;
import com.helger.as2lib.disposition.DispositionException;
import com.helger.as2lib.disposition.DispositionType;
import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.as2lib.exception.WrappedOpenAS2Exception;
import com.helger.as2lib.message.AS2Message;
import com.helger.as2lib.message.AS2MessageMDN;
import com.helger.as2lib.message.IMessageMDN;
import com.helger.as2lib.partner.CPartnershipIDs;
import com.helger.as2lib.processor.receiver.AS2MDNReceiverModule;
import com.helger.as2lib.processor.receiver.AbstractNetModule;
import com.helger.as2lib.processor.storage.IProcessorStorageModule;
import com.helger.as2lib.util.AS2Util;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.http.AS2HttpResponseHandlerSocket;
import com.helger.as2lib.util.http.AS2InputStreamProviderSocket;
import com.helger.as2lib.util.http.HTTPUtil;
import com.helger.as2lib.util.http.IAS2HttpResponseHandler;
import com.helger.as2lib.util.javamail.ByteArrayDataSource;
import com.helger.commons.annotations.Nonempty;
import com.helger.commons.io.streams.NonBlockingByteArrayOutputStream;
import com.helger.commons.io.streams.StreamUtils;
import com.helger.commons.string.StringParser;

public class AS2MDNReceiverHandler implements INetModuleHandler
{
  private static final String ATTR_PENDINGMDNINFO = "pendingmdninfo";
  private static final String ATTR_PENDINGMDN = "pendingmdn";

  private static final Logger s_aLogger = LoggerFactory.getLogger (AS2MDNReceiverHandler.class);

  private final AS2MDNReceiverModule m_aModule;

  public AS2MDNReceiverHandler (@Nonnull final AS2MDNReceiverModule aModule)
  {
    m_aModule = aModule;
  }

  @Nonnull
  @Nonempty
  public String getClientInfo (@Nonnull final Socket aSockt)
  {
    return aSockt.getInetAddress ().getHostAddress () + " " + aSockt.getPort ();
  }

  @Nonnull
  public AS2MDNReceiverModule getModule ()
  {
    return m_aModule;
  }

  public void handle (final AbstractNetModule aOwner, final Socket aSocket)
  {
    s_aLogger.info ("incoming connection [" + getClientInfo (aSocket) + "]");

    final AS2Message aMsg = new AS2Message ();

    final IAS2HttpResponseHandler aResponseHandler = new AS2HttpResponseHandlerSocket (aSocket);

    byte [] aData = null;

    // Read in the message request, headers, and data
    try
    {
      aData = HTTPUtil.readHttpRequest (new AS2InputStreamProviderSocket (aSocket), aResponseHandler, aMsg);
      // Asynch MDN 2007-03-12
      // check if the requested URL is defined in attribute "as2_receipt_option"
      // in one of partnerships, if yes, then process incoming AsyncMDN
      s_aLogger.info ("incoming connection for receiving AsyncMDN" +
                      " [" +
                      getClientInfo (aSocket) +
                      "]" +
                      aMsg.getLoggingText ());

      final MimeBodyPart aReceivedPart = new MimeBodyPart (aMsg.getHeaders (), aData);
      aMsg.setData (aReceivedPart);
      ContentType aReceivedContentType = new ContentType (aReceivedPart.getContentType ());

      aReceivedContentType = new ContentType (aMsg.getHeader (CAS2Header.HEADER_CONTENT_TYPE));

      // MimeBodyPart receivedPart = new MimeBodyPart();
      aReceivedPart.setDataHandler (new DataHandler (new ByteArrayDataSource (aData,
                                                                              aReceivedContentType.toString (),
                                                                              null)));
      aReceivedPart.setHeader (CAS2Header.HEADER_CONTENT_TYPE, aReceivedContentType.toString ());

      aMsg.setData (aReceivedPart);

      receiveMDN (aMsg, aData, aResponseHandler);
    }
    catch (final Exception ex)
    {
      final NetException ne = new NetException (aSocket.getInetAddress (), aSocket.getPort (), ex);
      ne.terminate ();
    }
  }

  // Asynch MDN 2007-03-12
  /**
   * method for receiving & processing Async MDN sent from receiver.
   */
  protected final void receiveMDN (final AS2Message aMsg,
                                   final byte [] aData,
                                   @Nonnull final IAS2HttpResponseHandler aResponseHandler) throws OpenAS2Exception,
                                                                                           IOException
  {
    try
    {
      // Create a MessageMDN and copy HTTP headers
      final IMessageMDN aMDN = new AS2MessageMDN (aMsg);
      // copy headers from msg to MDN from msg
      aMDN.setHeaders (aMsg.getHeaders ());
      final MimeBodyPart part = new MimeBodyPart (aMDN.getHeaders (), aData);
      aMsg.getMDN ().setData (part);

      // get the MDN partnership info
      aMDN.getPartnership ().setSenderID (CPartnershipIDs.PID_AS2, aMDN.getHeader (CAS2Header.HEADER_AS2_FROM));
      aMDN.getPartnership ().setReceiverID (CPartnershipIDs.PID_AS2, aMDN.getHeader (CAS2Header.HEADER_AS2_TO));
      getModule ().getSession ().getPartnershipFactory ().updatePartnership (aMDN, false);

      final ICertificateFactory aCertFactory = getModule ().getSession ().getCertificateFactory ();
      final X509Certificate aSenderCert = aCertFactory.getCertificate (aMDN, ECertificatePartnershipType.SENDER);

      AS2Util.parseMDN (aMsg, aSenderCert);

      // in order to name & save the mdn with the original AS2-From + AS2-To +
      // Message id.,
      // the 3 msg attributes have to be reset before calling MDNFileModule
      aMsg.getPartnership ().setSenderID (CPartnershipIDs.PID_AS2, aMDN.getHeader (CAS2Header.HEADER_AS2_TO));
      aMsg.getPartnership ().setReceiverID (CPartnershipIDs.PID_AS2, aMDN.getHeader (CAS2Header.HEADER_AS2_FROM));
      getModule ().getSession ().getPartnershipFactory ().updatePartnership (aMsg, false);
      aMsg.setMessageID (aMsg.getMDN ().getAttribute (AS2MessageMDN.MDNA_ORIG_MESSAGEID));
      getModule ().getSession ().getMessageProcessor ().handle (IProcessorStorageModule.DO_STOREMDN, aMsg, null);

      // check if the mic (message integrity check) is correct
      if (checkAsyncMDN (aMsg))
        HTTPUtil.sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_OK);
      else
        HTTPUtil.sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_NOT_FOUND);

      final String sDisposition = aMsg.getMDN ().getAttribute (AS2MessageMDN.MDNA_DISPOSITION);
      try
      {
        DispositionType.createFromString (sDisposition).validate ();
      }
      catch (final DispositionException ex)
      {
        ex.setText (aMsg.getMDN ().getText ());
        if (ex.getDisposition ().isWarning ())
        {
          ex.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
          ex.terminate ();
        }
        else
        {
          throw ex;
        }
      }
    }
    catch (final IOException ex)
    {
      HTTPUtil.sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_BAD_REQUEST);
      throw ex;
    }
    catch (final Exception ex)
    {
      HTTPUtil.sendSimpleHTTPResponse (aResponseHandler, HttpURLConnection.HTTP_BAD_REQUEST);

      final OpenAS2Exception we = WrappedOpenAS2Exception.wrap (ex);
      we.addSource (OpenAS2Exception.SOURCE_MESSAGE, aMsg);
      throw we;
    }
  }

  // Asynch MDN 2007-03-12
  /**
   * verify if the mic is matched.
   *
   * @param msg
   *        Message
   * @return true if mdn processed
   */
  public boolean checkAsyncMDN (final AS2Message msg)
  {
    try
    {
      // get the returned mic from mdn object
      final String returnmic = msg.getMDN ().getAttribute (AS2MessageMDN.MDNA_MIC);

      // use original message id. to open the pending information file
      // from pendinginfo folder.
      final String ORIG_MESSAGEID = msg.getMDN ().getAttribute (AS2MessageMDN.MDNA_ORIG_MESSAGEID);
      final String pendinginfofile = getModule ().getSession ()
                                                 .getMessageProcessor ()
                                                 .getAttributeAsString (ATTR_PENDINGMDNINFO) +
                                     "/" +
                                     ORIG_MESSAGEID.substring (1, ORIG_MESSAGEID.length () - 1);
      final BufferedReader pendinginfo = new BufferedReader (new FileReader (pendinginfofile));

      // Get the original mic from the first line of pending information
      // file
      final String originalmic = pendinginfo.readLine ();

      // Get the original pending file from the second line of pending
      // information file
      final File fpendingfile = new File (pendinginfo.readLine ());
      pendinginfo.close ();

      final String disposition = msg.getMDN ().getAttribute (AS2MessageMDN.MDNA_DISPOSITION);

      s_aLogger.info ("received MDN [" + disposition + "]" + msg.getLoggingText ());
      /*
       * original code just did string compare - returnmic.equals(originalmic).
       * Sadly this is not good enough as the mic fields are
       * "base64string, algorithm" taken from a rfc822 style
       * Returned-Content-MIC header and rfc822 headers can contain spaces all
       * over the place. (not to mention comments!). Simple fix - delete all
       * spaces.
       */
      if (originalmic == null || !returnmic.replaceAll ("\\s+", "").equals (originalmic.replaceAll ("\\s+", "")))
      {
        s_aLogger.info ("mic not matched, original mic: " +
                        originalmic +
                        " return mic: " +
                        returnmic +
                        msg.getLoggingText ());
        return false;
      }

      // delete the pendinginfo & pending file if mic is matched
      s_aLogger.info ("mic is matched, mic: " + returnmic + msg.getLoggingText ());
      final File fpendinginfofile = new File (pendinginfofile);
      s_aLogger.info ("delete pendinginfo file : " +
                      fpendinginfofile.getName () +
                      " from pending folder : " +
                      getModule ().getSession ().getMessageProcessor ().getAttributeAsString (ATTR_PENDINGMDN) +
                      msg.getLoggingText ());

      fpendinginfofile.delete ();

      s_aLogger.info ("delete pending file : " +
                      fpendingfile.getName () +
                      " from pending folder : " +
                      fpendingfile.getParent () +
                      msg.getLoggingText ());
      fpendingfile.delete ();
    }
    catch (final Exception e)
    {
      s_aLogger.error (e.getMessage (), e);
      return false;
    }
    return true;
  }

  public void reparse (final AS2Message aMsg, final HttpURLConnection aConn)
  {
    // Create a MessageMDN and copy HTTP headers
    final IMessageMDN aMDN = new AS2MessageMDN (aMsg);
    HTTPUtil.copyHttpHeaders (aConn, aMDN.getHeaders ());

    // Receive the MDN data
    NonBlockingByteArrayOutputStream aMDNStream = null;
    try
    {
      final InputStream aIS = aConn.getInputStream ();
      aMDNStream = new NonBlockingByteArrayOutputStream ();

      // Retrieve the message content
      final long nContentLength = StringParser.parseLong (aMDN.getHeader (CAS2Header.HEADER_CONTENT_LENGTH), -1);
      if (nContentLength >= 0)
        StreamUtils.copyInputStreamToOutputStreamWithLimit (aIS, aMDNStream, nContentLength);
      else
        StreamUtils.copyInputStreamToOutputStream (aIS, aMDNStream);
    }
    catch (final IOException ioe)
    {
      s_aLogger.error (ioe.getMessage (), ioe);
    }

    MimeBodyPart aPart = null;
    if (aMDNStream != null)
      try
      {
        aPart = new MimeBodyPart (aMDN.getHeaders (), aMDNStream.toByteArray ());
      }
      catch (final MessagingException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace ();
      }

    aMsg.getMDN ().setData (aPart);

    // get the MDN partnership info
    aMDN.getPartnership ().setSenderID (CPartnershipIDs.PID_AS2, aMDN.getHeader (CAS2Header.HEADER_AS2_FROM));
    aMDN.getPartnership ().setReceiverID (CPartnershipIDs.PID_AS2, aMDN.getHeader (CAS2Header.HEADER_AS2_TO));
  }
}
