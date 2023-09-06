/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 Pasqual K. and contributors
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

package dev.derklaro.aerogel;

import dev.derklaro.aerogel.internal.DefaultElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Wraps a type which has annotation based attributes.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "2.0")
public interface Element {

  /**
   * Creates a new element for the given type, with no other requirements specified initially.
   *
   * @param type the type of the element.
   * @return a new element for the given type.
   * @throws NullPointerException if the given type is null.
   */
  @Contract(pure = true)
  static @NotNull Element forType(@NotNull Type type) {
    return new DefaultElement(type);
  }

  /**
   * Get the type of this element.
   *
   * @return the type of this element.
   */
  @NotNull Type componentType();

  /**
   * Get the raw type of the component type of this element. This type is extracted as follows (first element is the
   * component type):
   * <ol>
   *   <li>class: returns the component type itself as it is already raw.
   *   <li>generic array: returns the array type representation of the raw parameter type.
   *   <li>parameterized: returns the raw type of the type parameter.
   *   <li>type variable: returns the raw type of the first upper bound or Object if there is none.
   *   <li>wildcard: returns either the first lower or upper bound, Object if there is no bound.
   * </ol>
   *
   * @return the raw type of the element component type.
   * @see #componentType()
   */
  @API(status = API.Status.EXPERIMENTAL, since = "2.2.0")
  @NotNull Class<?> rawComponentType();

  /**
   * Get an unmodifiable view of all required annotations of this element.
   *
   * @return all required annotations of this element.
   */
  @UnmodifiableView
  @NotNull Collection<AnnotationPredicate> requiredAnnotations();

  /**
   * Returns a new element with the same requirements as the element being called on but with a different component
   * type.
   *
   * @param componentType the component type to use for the new element.
   * @return a new element with the same requirements as this element but using the given component type.
   */
  @Contract(value = "_ -> new", pure = true)
  @API(status = API.Status.EXPERIMENTAL, since = "2.2.0")
  @NotNull Element withComponentType(@NotNull Type componentType);

  /**
   * Adds the given annotations as required annotations.
   *
   * @param annotation the annotations to add.
   * @return a new element with the given annotation required.
   * @throws NullPointerException if the given annotation is null.
   */
  @Contract(value = "_ -> new", pure = true)
  @NotNull Element requireAnnotation(@NotNull Annotation annotation);

  /**
   * Constructs a proxy for the given annotation type and requires it. This method only works when all values of the
   * given annotation type are optional (defaulted). With this in mind, the annotation added to the annotated element is
   * required to have all values set to the default as well.
   * <p>
   * If a value of the annotation should have a different type, use {@link #requireAnnotation(Class, Map)} instead.
   *
   * @param annotationType the type of annotation to require.
   * @return a new element with the given annotation required.
   * @throws NullPointerException if the given annotation type is null.
   * @throws AerogelException     if the given annotation has a non-defaulted method.
   * @since 2.0
   */
  @Contract(value = "_ -> new", pure = true)
  @API(status = API.Status.STABLE, since = "2.0")
  @NotNull Element requireAnnotation(@NotNull Class<? extends Annotation> annotationType);

  /**
   * Constructs a proxy for the given annotation type and requires it. The construction process requires the caller to
   * give a value in the overridden values map for all methods which are not optional (defaulted) in the given
   * annotation type. Overridden values for defaulted values can be passed as well, but are optional.
   *
   * @param annotationType         the annotation types to add.
   * @param overriddenMethodValues the overridden method return values for the given annotation type.
   * @return a new element with the given annotation required.
   * @throws NullPointerException if the given annotation type or overridden value map is null.
   * @throws AerogelException     if the given annotation has a non-defaulted method which has no overridden value.
   * @since 2.0
   */
  @Contract(value = "_, _ -> new", pure = true)
  @API(status = API.Status.STABLE, since = "2.0")
  @NotNull Element requireAnnotation(
    @NotNull Class<? extends Annotation> annotationType,
    @NotNull Map<String, Object> overriddenMethodValues);

  /**
   * Get if this element has special requirements.
   *
   * @return true if this element has special requirements, false otherwise.
   * @since 2.0
   */
  @API(status = API.Status.STABLE, since = "2.0")
  boolean hasSpecialRequirements();

  /**
   * {@inheritDoc}
   */
  @Override
  @NotNull String toString();

  /**
   * {@inheritDoc}
   */
  @Override
  int hashCode();

  /**
   * {@inheritDoc}
   */
  @Override
  boolean equals(@NotNull Object other);
}
