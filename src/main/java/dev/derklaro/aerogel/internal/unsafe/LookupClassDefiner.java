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

package dev.derklaro.aerogel.internal.unsafe;

import dev.derklaro.aerogel.AerogelException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A class definer for modern jvm implementation (Java 15+) which makes use of the newly added {@code defineHiddenClass}
 * method in the {@code Lookup} class.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal.unsafe")
final class LookupClassDefiner implements ClassDefiner {

  /**
   * The jvm trusted lookup instance. It allows access to every lookup even if the access to these classes is denied for
   * the current module.
   */
  private static final MethodHandles.Lookup TRUSTED_LOOKUP;
  /**
   * The created option array to define a class.
   */
  private static final Object HIDDEN_CLASS_OPTIONS;
  /**
   * The method to define a hidden class using a lookup instance.
   */
  private static final Method DEFINE_HIDDEN_METHOD;

  static {
    Object hiddenClassOptions = null;
    Method defineHiddenMethod = null;
    MethodHandles.Lookup trustedLookup = null;

    if (UnsafeAccess.isAvailable()) {
      try {
        // get the trusted lookup field
        Field implLookup = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        trustedLookup = (MethodHandles.Lookup) UnsafeMemberAccess.forceMakeAccessible(implLookup).get(null);

        // get the options for defining hidden (or nestmate) classes
        hiddenClassOptions = classOptionArray();

        // get the method to define a hidden class (Java 9+)
        //noinspection JavaReflectionMemberAccess
        Method defineHiddenClassMethod = MethodHandles.Lookup.class.getMethod("defineHiddenClass",
          byte[].class,
          boolean.class,
          hiddenClassOptions.getClass());
        defineHiddenClassMethod.setAccessible(true);
        defineHiddenMethod = defineHiddenClassMethod;
      } catch (Throwable ignored) {
      }
    }

    // set the static final fields
    TRUSTED_LOOKUP = trustedLookup;
    HIDDEN_CLASS_OPTIONS = hiddenClassOptions;
    DEFINE_HIDDEN_METHOD = defineHiddenMethod;
  }

  /**
   * Creates a new array of class options which is used to define a class in a lookup.
   *
   * @return the created class option array to define a class.
   * @throws Exception if any exception occurs during the array lookup.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static @NotNull Object classOptionArray() throws Exception {
    // the ClassOption enum is a subclass of the Lookup class
    Class optionClass = Class.forName(MethodHandles.Lookup.class.getName() + "$ClassOption");
    // create an array of these options (for now always one option)
    Object resultingOptionArray = Array.newInstance(optionClass, 1);
    // set the first option to NESTMATE
    Array.set(resultingOptionArray, 0, Enum.valueOf(optionClass, "NESTMATE"));
    // that's it
    return resultingOptionArray;
  }

  /**
   * Get if the lookup class definer requirements are met to use the definer in the current jvm.
   *
   * @return if the lookup class definer requirements are met to use the definer in the current jvm.
   */
  public static boolean isAvailable() {
    return TRUSTED_LOOKUP != null && HIDDEN_CLASS_OPTIONS != null && DEFINE_HIDDEN_METHOD != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Class<?> defineClass(@NotNull String name, @NotNull Class<?> parent, byte[] bytecode) {
    try {
      // define the method using the method handle
      MethodHandles.Lookup lookup = (MethodHandles.Lookup) DEFINE_HIDDEN_METHOD.invoke(
        TRUSTED_LOOKUP.in(parent),
        bytecode,
        false,
        HIDDEN_CLASS_OPTIONS);
      // get the class from the lookup
      return lookup.lookupClass();
    } catch (Throwable throwable) {
      throw AerogelException.forMessagedException("Exception defining class " + name, throwable);
    }
  }
}
