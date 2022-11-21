/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2022 Pasqual K. and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package dev.derklaro.aerogel.internal.utility;

import dev.derklaro.aerogel.AerogelException;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A little preconditions class.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel")
public final class Preconditions {

  private Preconditions() {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks if {@code argument} is {@code true}.
   *
   * @param argument     the argument to check.
   * @param errorMessage the error message to use if the argument is not true.
   * @throws AerogelException if the argument is not true.
   */
  public static void checkArgument(boolean argument, @NotNull String errorMessage) {
    if (!argument) {
      throw AerogelException.forMessage(errorMessage);
    }
  }

  /**
   * Checks if {@code argument} is {@code true}.
   *
   * @param argument           the argument to check.
   * @param errorMessageFormat the error message format to use if the argument is not true.
   * @param args               the arguments to use to format the error message format.
   * @throws AerogelException if the argument is not true.
   */
  public static void checkArgument(boolean argument, @NotNull String errorMessageFormat, Object... args) {
    if (!argument) {
      throw AerogelException.forMessage(String.format(errorMessageFormat, args));
    }
  }
}
