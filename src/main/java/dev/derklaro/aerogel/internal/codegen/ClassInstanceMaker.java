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

package dev.derklaro.aerogel.internal.codegen;

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.BindingHolder;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.Provider;
import dev.derklaro.aerogel.internal.asm.AsmPrimitives;
import dev.derklaro.aerogel.internal.asm.AsmUtils;
import dev.derklaro.aerogel.internal.jakarta.JakartaBridge;
import dev.derklaro.aerogel.internal.reflect.ReflectionUtil;
import dev.derklaro.aerogel.internal.unsafe.ClassDefiners;
import dev.derklaro.aerogel.internal.utility.ElementHelper;
import dev.derklaro.aerogel.internal.utility.ReferenceUtil;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * An instance maker generator which creates instances using constructor injection.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal")
public final class ClassInstanceMaker {

  // the super interface all instance makers must implement
  static final String[] INSTANCE_MAKER = new String[]{org.objectweb.asm.Type.getInternalName(InstanceMaker.class)};
  // the holder for singleton instances
  static final String HOLDER = "holder";
  static final String HOLDER_DESC = org.objectweb.asm.Type.getDescriptor(AtomicReference.class);
  static final String HOLDER_NAME = org.objectweb.asm.Type.getInternalName(AtomicReference.class);
  // the injection context
  static final String INJECTOR = "injector";
  static final String FIND_INSTANCE = "findInstance";
  static final String INJECTOR_DESC = AsmUtils.methodDesc(Injector.class);
  static final String FIND_CONSTRUCTED_VALUE = "findConstructedValue";
  static final String FIND_INSTANCE_DESC = AsmUtils.methodDesc(Object.class, Element.class);
  static final String FIND_CONSTRUCTED_VALUE_DESC = AsmUtils.methodDesc(Object.class, Element.class);
  static final String INJ_CONTEXT_DESC = org.objectweb.asm.Type.getDescriptor(InjectionContext.class);
  static final String INJ_CONTEXT_NAME = org.objectweb.asm.Type.getInternalName(InjectionContext.class);
  // the injector
  static final String INJECTOR_BINDING = "binding";
  static final String INJECTOR_NAME = org.objectweb.asm.Type.getInternalName(Injector.class);
  static final String INJECTOR_BINDING_DESC = AsmUtils.methodDesc(BindingHolder.class, Element.class);
  // the create result
  static final String CREATE_RESULT_NAME = org.objectweb.asm.Type.getInternalName(InstanceCreateResult.class);
  static final String CREATE_CONST_DESC = org.objectweb.asm.Type.getMethodDescriptor(
    org.objectweb.asm.Type.VOID_TYPE,
    org.objectweb.asm.Type.getType(Object.class),
    org.objectweb.asm.Type.BOOLEAN_TYPE);
  // the provider wrapping stuff
  static final String PROVIDER_NAME = org.objectweb.asm.Type.getInternalName(Provider.class);
  static final String JAKARTA_BRIDGE = org.objectweb.asm.Type.getInternalName(JakartaBridge.class);
  static final String PROV_JAKARTA_DESC = AsmUtils.methodDesc(jakarta.inject.Provider.class, Provider.class);
  // the element array access stuff
  static final String ELEMENTS = "elements";
  static final Element[] NO_ELEMENT = new Element[0];
  static final String ELEMENTS_DESC = org.objectweb.asm.Type.getDescriptor(Element[].class);
  // reference util
  static final String STORE_AND_PACK = "storeAndPack";
  static final String REFERENCE_UTIL = org.objectweb.asm.Type.getInternalName(ReferenceUtil.class);
  static final String STORE_REFERENCE_DESC = AsmUtils.methodDesc(
    InstanceCreateResult.class,
    AtomicReference.class,
    Object.class);
  // ReferenceUtil.unmask
  static final String UNMASK = "unmaskAndPack";
  static final String UNMASK_DESC = AsmUtils.methodDesc(InstanceCreateResult.class, Object.class);
  // the element access stuff
  static final String CUR_ELEMENT = "currentElement";
  static final String ELEMENT_DESC = org.objectweb.asm.Type.getDescriptor(Element.class);
  // other stuff
  static final String GET_INSTANCE = "getInstance";
  static final String PROXY_CLASS_NAME_FORMAT = "%s$Invoker_%d";

  private ClassInstanceMaker() {
    throw new UnsupportedOperationException();
  }

  /**
   * Makes an instance maker for the given constructor.
   *
   * @param self      the element type for which the instance maker gets created.
   * @param target    the target constructor to use for injection.
   * @param singleton if the resulting object should be a singleton.
   * @return the created instance maker for the constructor injection.
   * @throws AerogelException if an exception occurs when defining and loading the class.
   */
  public static @NotNull InstanceMaker forConstructor(
    @NotNull Element self,
    @NotNull Constructor<?> target,
    boolean singleton
  ) {
    // extract the wrapping class of the constructor
    Class<?> ct = target.getDeclaringClass();
    // the types used for the class init
    Element[] elements;
    // make a proxy name for the class
    String proxyName = String.format(
      PROXY_CLASS_NAME_FORMAT,
      org.objectweb.asm.Type.getType(ct).getInternalName(),
      System.nanoTime());

    MethodVisitor mv;
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    // target Java 8 classes as the minimum requirement
    cw.visit(V1_8, AsmUtils.PUBLIC_FINAL | ACC_SUPER, proxyName, null, AsmUtils.OBJECT, INSTANCE_MAKER);
    // writes all necessary fields to the class
    writeFields(cw, singleton);
    // write the constructor to the class
    writeConstructor(cw, proxyName, singleton);

    // visit the getInstance() method
    mv = cw.visitMethod(
      ACC_PUBLIC,
      GET_INSTANCE,
      AsmUtils.descToMethodDesc(INJ_CONTEXT_DESC, InstanceCreateResult.class),
      null,
      null);
    mv.visitCode();
    // if this is a singleton check first if the instance is already loaded
    if (singleton) {
      checkForConstructedValue(mv, proxyName, true);
    }
    // check if the constructor does take arguments (if not that makes the life easier)
    if (target.getParameterCount() == 0) {
      // construct the class
      mv.visitTypeInsn(NEW, AsmUtils.intName(ct));
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, AsmUtils.intName(ct), AsmUtils.CONSTRUCTOR_NAME, "()V", false);
      // no types for the class init are required
      elements = NO_ELEMENT;
    } else {
      // store all parameters to the stack
      elements = storeParameters(target, proxyName, mv, singleton);
      // begin the instance creation
      mv.visitTypeInsn(NEW, AsmUtils.intName(ct));
      mv.visitInsn(DUP);
      // load all elements from the stack
      loadParameters(elements, mv);
      // instantiate the constructor with the parameters
      mv.visitMethodInsn(
        INVOKESPECIAL,
        AsmUtils.intName(ct),
        AsmUtils.CONSTRUCTOR_NAME,
        AsmUtils.consDesc(target),
        false);
    }
    // if this is a singleton store the value in the AtomicReference
    if (singleton) {
      appendSingletonWrite(mv, proxyName);
    }

    // return the created value
    mv.visitVarInsn(ASTORE, 2);
    packValueIntoResultAndReturn(mv, visitor -> visitor.visitVarInsn(ALOAD, 2), true);

    // finish the class
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // finish & define the class
    cw.visitEnd();
    // construct
    return defineAndConstruct(cw, proxyName, ct, self, elements);
  }

  /**
   * Stores all parameters to the current object stack.
   *
   * @param exec      the executable for which the parameters should get stored.
   * @param name      the name of the proxy which holds the fields.
   * @param mv        the method visitor of the currently visiting method.
   * @param singleton if the resulting object should only have one instance per injector.
   * @return the elements of the stored parameters.
   */
  static @NotNull Element[] storeParameters(
    @NotNull Executable exec,
    @NotNull String name,
    @NotNull MethodVisitor mv,
    boolean singleton
  ) {
    // create an element for each parameter of the constructor
    Parameter[] parameters = exec.getParameters();
    // init the types directly while unboxing the parameters
    Element[] elements = new Element[parameters.length];
    // stores the current writer index as some types need more space on the stack
    AtomicInteger writerIndex = new AtomicInteger(0);
    for (int i = 0; i < parameters.length; i++) {
      elements[i] = unpackParameter(name, mv, parameters[i], writerIndex, parameters[i].getDeclaredAnnotations(), i);
      // add a check if the instance was created as a side effect after each parameter
      checkForConstructedValue(mv, name, singleton);
    }
    // return the types for later re-use
    return elements;
  }

  /**
   * Write the default fields to a newly created class.
   *
   * @param cw        the class writer of the class.
   * @param singleton if the resulting object should only have one instance per injector.
   */
  static void writeFields(@NotNull ClassWriter cw, boolean singleton) {
    // adds the type[] fields to the class
    cw.visitField(AsmUtils.PRIVATE_FINAL, ELEMENTS, ELEMENTS_DESC, null, null).visitEnd();
    // adds the element field of the constructing type to the class
    cw.visitField(AsmUtils.PRIVATE_FINAL, CUR_ELEMENT, ELEMENT_DESC, null, null).visitEnd();
    // if this is a singleton add the atomic reference field which will hold that instance later
    if (singleton) {
      cw.visitField(AsmUtils.PRIVATE_FINAL, HOLDER, HOLDER_DESC, null, null).visitEnd();
    }
  }

  /**
   * Writes the constructor with the required {@code Element[]} parameter to the class.
   *
   * @param cw        the class writer of the constructor.
   * @param proxyName the name of the proxy we are creating.
   * @param singleton if the resulting object should only have one instance per injector.
   */
  static void writeConstructor(@NotNull ClassWriter cw, @NotNull String proxyName, boolean singleton) {
    // visit the constructor
    MethodVisitor mv = AsmUtils.beginConstructor(
      cw,
      AsmUtils.descToMethodDesc(ELEMENTS_DESC + ELEMENT_DESC, void.class));
    // assign the type field
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(PUTFIELD, proxyName, ELEMENTS, ELEMENTS_DESC);
    // assign the element field holding information about the constructing element
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitFieldInsn(PUTFIELD, proxyName, CUR_ELEMENT, ELEMENT_DESC);
    // assign the singleton AtomicReference field if this is a singleton
    if (singleton) {
      // create a new instance of the singleton holder
      mv.visitVarInsn(ALOAD, 0);
      mv.visitTypeInsn(NEW, HOLDER_NAME);
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, HOLDER_NAME, AsmUtils.CONSTRUCTOR_NAME, "()V", false);
      mv.visitFieldInsn(PUTFIELD, proxyName, HOLDER, HOLDER_DESC);
    }
    // finish the constructor write
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Loads all previously stored parameters back to the stack.
   *
   * @param types the elements to load.
   * @param mv    the method visitor of the currently visiting method.
   */
  static void loadParameters(@NotNull Element[] types, @NotNull MethodVisitor mv) {
    int readerIndex = 0;
    for (Element element : types) {
      // primitive types need to get loaded in another way from the stack than objects
      if (ReflectionUtil.isPrimitive(element.componentType())) {
        readerIndex += AsmPrimitives.load((Class<?>) element.componentType(), mv, 3 + readerIndex);
      } else {
        mv.visitVarInsn(ALOAD, 3 + readerIndex++);
      }
    }
  }

  /**
   * Defines and construct the generated class.
   *
   * @param cw       the class writer used for construction of the type.
   * @param name     the name of the constructed class.
   * @param parent   the parent class of the constructed class (as we are generating anonymous classes).
   * @param self     the element for which the instance maker gets created.
   * @param elements the elements of the parameters used for injection.
   * @return the instance of the newly created instance maker.
   * @throws AerogelException if an exception occurs when defining and loading the class.
   */
  static @NotNull InstanceMaker defineAndConstruct(
    @NotNull ClassWriter cw,
    @NotNull String name,
    @NotNull Class<?> parent,
    @NotNull Element self,
    @NotNull Element[] elements
  ) {
    Class<?> defined = ClassDefiners.getDefiner().defineClass(name, parent, cw.toByteArray());
    // instantiate the class
    try {
      Constructor<?> ctx = defined.getDeclaredConstructor(Element[].class, Element.class);
      ctx.setAccessible(true);

      return (InstanceMaker) ctx.newInstance(elements, self);
    } catch (ReflectiveOperationException exception) {
      throw AerogelException.forException(exception);
    }
  }

  /**
   * Writes the current stack top element into the singleton AtomicReference stored in the class.
   *
   * @param mv        the method visitor of the current method.
   * @param proxyName the name of the proxy owning the singleton reference field.
   */
  static void appendSingletonWrite(@NotNull MethodVisitor mv, @NotNull String proxyName) {
    // temp store the previous return value
    mv.visitVarInsn(ASTORE, 2);
    // load the reference field to the stack
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, proxyName, HOLDER, HOLDER_DESC);
    // load the constructed value
    mv.visitVarInsn(ALOAD, 2);
    // set the value in the reference
    mv.visitMethodInsn(INVOKESTATIC, REFERENCE_UTIL, STORE_AND_PACK, STORE_REFERENCE_DESC, false);
    mv.visitInsn(ARETURN);
  }

  /**
   * Loads the previously constructed element to stack and returns it if the construction was done before.
   *
   * @param mv        the method visitor of the current method.
   * @param proxyName the name of the proxy owning the singleton reference field.
   * @param singleton if the resulting object should only have one instance per injector.
   */
  static void checkForConstructedValue(@NotNull MethodVisitor mv, @NotNull String proxyName, boolean singleton) {
    // check if we should visit the singleton holder
    if (singleton) {
      // check if the value is already present in the singleton holder
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, proxyName, HOLDER, HOLDER_DESC);
      mv.visitMethodInsn(INVOKEVIRTUAL, HOLDER_NAME, "get", "()" + AsmUtils.OBJECT_DESC, false);
      // check if we can return now
      visitReturnIfNonNull(mv, visitor -> {
        visitor.visitVarInsn(ALOAD, 2);
        visitor.visitMethodInsn(INVOKESTATIC, REFERENCE_UTIL, UNMASK, UNMASK_DESC, false);
        visitor.visitInsn(ARETURN);
      });
    }
    // check if the InjectionContext has a value available
    mv.visitVarInsn(ALOAD, 1);
    // load the element we are constructing
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, proxyName, CUR_ELEMENT, ELEMENT_DESC);
    // visit the findConstructedValue method in the InjectionContext
    mv.visitMethodInsn(INVOKEINTERFACE, INJ_CONTEXT_NAME, FIND_CONSTRUCTED_VALUE, FIND_CONSTRUCTED_VALUE_DESC, true);
    // check if we can return now
    visitReturnIfNonNull(mv, v -> packValueIntoResultAndReturn(v, visitor -> visitor.visitVarInsn(ALOAD, 2), false));
  }

  /**
   * Visits the return code to return the top element from the current stack.
   *
   * @param mv the method visitor to visit the operands on.
   * @since 1.3.0
   */
  static void visitReturnIfNonNull(@NotNull MethodVisitor mv, @NotNull Consumer<MethodVisitor> ifNonNull) {
    Label wasConstructedDimension = new Label();
    // store the current value to the stack
    mv.visitInsn(DUP);
    mv.visitVarInsn(ASTORE, 2);
    // if (val != null) then
    mv.visitJumpInsn(IFNULL, wasConstructedDimension);
    // return the current non-null value of the stack
    ifNonNull.accept(mv);
    mv.visitLabel(wasConstructedDimension);
  }

  static void packValueIntoResultAndReturn(
    @NotNull MethodVisitor mv,
    @NotNull Consumer<MethodVisitor> resultLoader,
    boolean doMemberInjection
  ) {
    // begin the construct
    mv.visitTypeInsn(NEW, CREATE_RESULT_NAME);
    mv.visitInsn(DUP);
    // push the created value and the boolean for member injection to the stack
    resultLoader.accept(mv);
    mv.visitInsn(doMemberInjection ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
    // construct and return the result
    mv.visitMethodInsn(INVOKESPECIAL, CREATE_RESULT_NAME, AsmUtils.CONSTRUCTOR_NAME, CREATE_CONST_DESC, false);
    mv.visitInsn(ARETURN);
  }

  /**
   * Ensures that the correct element gets loaded and pushed to the stack.
   *
   * @param ot              the proxy which the generator is generating.
   * @param mv              the method visitor of the current method.
   * @param parameter       the parameter which should get unboxed.
   * @param typeWriterIndex the current writer index of the stack.
   * @param annotations     the annotations of the parameter.
   * @param index           the current index for loading the element in the runtime from the class element array.
   * @return the element which will be used for loading the instance from the injection context.
   */
  private static @NotNull Element unpackParameter(
    @NotNull String ot,
    @NotNull MethodVisitor mv,
    @NotNull Parameter parameter,
    @NotNull AtomicInteger typeWriterIndex,
    @NotNull Annotation[] annotations,
    int index
  ) {
    // collect general information about the parameter we want to load
    String name = JakartaBridge.nameOf(parameter);
    // if the type is wrapped in a provider
    boolean provider = JakartaBridge.isProvider(parameter.getType());
    boolean jakartaProvider = JakartaBridge.needsProviderWrapping(parameter.getType());
    // filter out all qualifier annotations
    Annotation[] qualifiedAnnotations = ElementHelper.extractQualifierAnnotations(annotations);
    // the type of the parameter is important as we do need to consider either to push the real type of the super type later
    Type generic;
    Class<?> type;
    if (provider) {
      generic = ReflectionUtil.genericSuperType(parameter.getParameterizedType());
      type = ReflectionUtil.rawType(ReflectionUtil.genericSuperType(parameter.getParameterizedType()));
    } else {
      // just use the type of the parameter
      type = parameter.getType();
      generic = parameter.getParameterizedType();
    }

    // load the injection context to the stack
    mv.visitVarInsn(ALOAD, 1);
    // if we need a Provider we do need to call the binding(Element) method in the injector class - load the injector
    if (provider) {
      mv.visitMethodInsn(INVOKEINTERFACE, INJ_CONTEXT_NAME, INJECTOR, INJECTOR_DESC, true);
    }
    // read the element from the class intern array
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, ot, ELEMENTS, ELEMENTS_DESC);
    AsmUtils.pushInt(mv, index);
    mv.visitInsn(AALOAD);
    // get its value from the injection context, or just the binding if a Provider is requested
    if (provider) {
      // get the binding from the injector
      mv.visitMethodInsn(INVOKEINTERFACE, INJECTOR_NAME, INJECTOR_BINDING, INJECTOR_BINDING_DESC, true);
      mv.visitTypeInsn(CHECKCAST, PROVIDER_NAME);
      // bridge to a jakarta provider if needed
      if (jakartaProvider) {
        mv.visitMethodInsn(INVOKESTATIC, JAKARTA_BRIDGE, "bridgeJakartaProvider", PROV_JAKARTA_DESC, false);
      }
    } else {
      mv.visitMethodInsn(INVOKEINTERFACE, INJ_CONTEXT_NAME, FIND_INSTANCE, FIND_INSTANCE_DESC, true);
      // cast to the required type if the type is not primitive (that will be handled by AsmPrimitives.storeUnbox)
      if (!type.isPrimitive()) {
        mv.visitTypeInsn(CHECKCAST, AsmUtils.intName(type));
      }
    }
    // unwrap the primitive type if needed - a provider could cause a type mismatch here so ignore that check
    if (!provider && type.isPrimitive()) {
      typeWriterIndex.addAndGet(AsmPrimitives.storeUnbox(type, mv, 3 + typeWriterIndex.get()));
    } else {
      mv.visitVarInsn(ASTORE, 3 + typeWriterIndex.getAndIncrement());
    }

    // return the extracted generic type
    Element element = Element.forType(generic).requireName(name);
    for (Annotation annotation : qualifiedAnnotations) {
      element = element.requireAnnotation(annotation);
    }
    return element;
  }
}
