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
package com.helger.as2lib.exception;

import javax.annotation.Nonnull;

/**
 * An {@link OpenAS2Exception} that wraps an existing exception easily.
 *
 * @author Philip Helger
 */
public final class WrappedOpenAS2Exception extends OpenAS2Exception
{
  private WrappedOpenAS2Exception (final Throwable aCause)
  {
    super (aCause);
  }

  public WrappedOpenAS2Exception (final String sMsg, final Throwable aCause)
  {
    super (sMsg, aCause);
  }

  /**
   * Ensure the passed {@link Throwable} is an {@link OpenAS2Exception}.
   *
   * @param t
   *        The Throwable to use.
   * @return Either t or a {@link WrappedOpenAS2Exception} containing t.
   */
  @Nonnull
  public static OpenAS2Exception wrap (@Nonnull final Throwable t)
  {
    if (t instanceof OpenAS2Exception)
      return (OpenAS2Exception) t;
    return new WrappedOpenAS2Exception (t);
  }
}
