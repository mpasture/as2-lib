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
package com.helger.as2lib.params;

import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.as2lib.exception.OpenAS2Exception;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;

public abstract class AbstractParameterParser
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (AbstractParameterParser.class);

  public abstract void setParameter (@Nonnull String sKey, @Nonnull String sValue) throws InvalidParameterException;

  @Nullable
  public abstract String getParameter (@Nonnull String sKey) throws InvalidParameterException;

  /**
   * Set parameters from a string, like
   * "msg.sender.as2_id=ME,msg.headers.content-type=application/X12"
   *
   * @param sEncodedParams
   *        string to parse
   * @throws InvalidParameterException
   *         In case the string is incorrect
   */
  public void setParameters (@Nonnull final String sEncodedParams) throws InvalidParameterException
  {
    final StringTokenizer aParams = new StringTokenizer (sEncodedParams, "=,", false);
    while (aParams.hasMoreTokens ())
    {
      final String sKey = aParams.nextToken ().trim ();
      if (!aParams.hasMoreTokens ())
        throw new InvalidParameterException ("Invalid value", this, sKey, null);

      final String sValue = aParams.nextToken ();
      setParameter (sKey, sValue);
    }
  }

  /**
   * Set parameters from a string separated by delimiters.
   *
   * @param sFormat
   *        Comma separated list of parameters to set, like
   *        <code>msg.sender.as2_id,msg.receiver.as2_id,msg.header.content-type</code>
   * @param sDelimiters
   *        delimiters in string to parse, like "-."
   * @param sValue
   *        string to parse, like <code>"NORINCO-WALMART.application/X12"</code>
   * @throws OpenAS2Exception
   *         In case the string is incorrect
   */
  public void setParameters (@Nullable final String sFormat, @Nullable final String sDelimiters, @Nonnull final String sValue) throws OpenAS2Exception
  {
    final List <String> aKeys = StringHelper.getExploded (',', sFormat);

    final StringTokenizer aValueTokens = new StringTokenizer (sValue, sDelimiters, false);
    for (final String sKey : aKeys)
    {
      if (!aValueTokens.hasMoreTokens ())
        throw new OpenAS2Exception ("Invalid value: Format=" + sFormat + ", value=" + sValue);

      if (sKey.length () > 0)
        setParameter (sKey, aValueTokens.nextToken ());
    }
  }

  /**
   * Fill in a format string with information from a ParameterParser
   *
   * @param sFormat
   *        the format string to fill in
   * @return the filled in format string.
   * @throws InvalidParameterException
   *         In case the string is incorrect
   */
  @Nonnull
  public String format (@Nonnull final String sFormat) throws InvalidParameterException
  {
    if (s_aLogger.isTraceEnabled ())
      s_aLogger.trace ("Formatting '" + sFormat + "'");

    final StringBuilder aSB = new StringBuilder ();
    for (int nNext = 0; nNext < sFormat.length (); ++nNext)
    {
      int nPrev = nNext;

      // Find start of $xxx$ sequence.
      nNext = sFormat.indexOf ('$', nPrev);
      if (nNext == -1)
      {
        aSB.append (sFormat.substring (nPrev, sFormat.length ()));
        break;
      }

      // Save text before $xxx$ sequence, if there is any
      if (nNext > nPrev)
        aSB.append (sFormat.substring (nPrev, nNext));

      // Find end of $xxx$ sequence
      nPrev = nNext + 1;
      nNext = sFormat.indexOf ('$', nPrev);
      if (nNext == -1)
        throw new InvalidParameterException ("Invalid key (missing closing $)");

      // If we have just $$ then output $, else we have $xxx$, lookup xxx
      if (nNext == nPrev)
        aSB.append ('$');
      else
        aSB.append (getParameter (sFormat.substring (nPrev, nNext)));
    }

    if (s_aLogger.isTraceEnabled ())
      s_aLogger.trace ("Formatted value is now '" + aSB.toString () + "'");

    return aSB.toString ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).toString ();
  }
}
