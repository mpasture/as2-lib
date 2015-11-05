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
package com.helger.as2lib.message;

import java.text.DecimalFormat;
import java.util.Random;

import javax.annotation.Nonnull;

import com.helger.as2lib.AS2GlobalSettings;
import com.helger.as2lib.CAS2Info;
import com.helger.as2lib.partner.Partnership;
import com.helger.as2lib.util.CAS2Header;
import com.helger.as2lib.util.DateHelper;
import com.helger.commons.random.VerySecureRandom;

public class AS2MessageMDN extends AbstractMessageMDN
{
  public static final String MDNA_REPORTING_UA = "REPORTING_UA";
  public static final String MDNA_ORIG_RECIPIENT = "ORIGINAL_RECIPIENT";
  public static final String MDNA_FINAL_RECIPIENT = "FINAL_RECIPIENT";
  public static final String MDNA_ORIG_MESSAGEID = "ORIGINAL_MESSAGE_ID";
  public static final String MDNA_DISPOSITION = "DISPOSITION";
  public static final String MDNA_MIC = "MIC";
  public static final String DEFAULT_DATE_FORMAT = "ddMMyyyyHHmmssZ";

  public AS2MessageMDN (@Nonnull final AS2Message aMsg)
  {
    super (aMsg);
    // Swap from and to
    setHeader (CAS2Header.HEADER_AS2_TO, aMsg.getAS2From ());
    setHeader (CAS2Header.HEADER_AS2_FROM, aMsg.getAS2To ());
  }

  @Override
  public String generateMessageID ()
  {
    final StringBuilder aSB = new StringBuilder ();
    final String sDateFormat = getPartnership ().getDateFormat (DEFAULT_DATE_FORMAT);
    aSB.append ("<").append (CAS2Info.NAME).append ("-").append (DateHelper.getFormattedDateNow (sDateFormat));

    final DecimalFormat aRandomFormatter = new DecimalFormat ("0000");
    int nRandom;
    if (AS2GlobalSettings.isUseSecureRandom ())
      nRandom = VerySecureRandom.getInstance ().nextInt (10000);
    else
      nRandom = new Random ().nextInt (10000);
    aSB.append ('-').append (aRandomFormatter.format (nRandom));

    // Message details
    final Partnership aPartnership = getMessage ().getPartnership ();
    final String sReceiverID = aPartnership.getReceiverAS2ID ();
    final String sSenderID = aPartnership.getSenderAS2ID ();
    aSB.append ('@').append (sReceiverID).append ('_').append (sSenderID);

    return aSB.append ('>').toString ();
  }
}
