/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu.impl.compiler;

import juzu.impl.common.Logger;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.tools.Diagnostic;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Abstract processing tool specific.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public enum ProcessingTool {

  JAVAC() {
    @Override
    public void report(Messager messager, Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationMirror annotation, AnnotationValue value) {
      if (element == null) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg);
      }
      else if (annotation == null) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg, element);
      }
      else {
        messager.printMessage(Diagnostic.Kind.ERROR, msg, element, annotation);
      }
    }

    @Override
    public boolean getOverwriteReadingResource() {
      return false;
    }
  },

  ECLIPSE_IDE() {
    @Override
    public void report(Messager messager, Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationMirror annotation, AnnotationValue value) {
      if (element == null) {
        // ECJ does not like reporting null elements (i.e call to messager.printMessage(Kind,CharSequence)
        // java.lang.NullPointerException
        // at java.lang.String.<init>(String.java:177)
        // at org.eclipse.jdt.internal.compiler.tool.EclipseCompilerImpl$4.getSource(EclipseCompilerImpl.java:546)
        // at org.eclipse.jdt.internal.compiler.tool.EclipseCompilerImpl$4.getSource(EclipseCompilerImpl.java:1)
        System.err.println(kind.name() + ": " + msg);
      }
      else if (element.getKind() == ElementKind.PACKAGE) {
        report(messager, kind, msg, (PackageElement)element, annotation, value);
      }
      else {
        JAVAC.report(messager, kind, msg, element, annotation, value);
      }
    }

    private void report(Messager messager, Diagnostic.Kind kind, CharSequence msg, PackageElement pkgElt, AnnotationMirror annotation, AnnotationValue value) {
      try {
        // package-info


        Class c = pkgElt.getClass();

        //
        Field bindingField = c.getField("_binding");
        Object binding = bindingField.get(pkgElt);

        //
        Class packageBindingClass = binding.getClass();
        log.log("Packaging binding class " + packageBindingClass.getName());

        //
        Field compoundNameField = packageBindingClass.getField("compoundName");
        char[][] compoundName = (char[][])compoundNameField.get(binding);
        log.log("About to hack " + compoundName);

        // Compute name
        char[][] name = new char[compoundName.length + 1][];
        System.arraycopy(compoundName, 0, name, 0, compoundName.length);
        name[compoundName.length] = "package-info".toCharArray();

        // Just display name in log
        StringBuilder sb = new StringBuilder();
        for (int i = 0;i < name.length;i++) {
          if (i > 0) {
            sb.append('.');
          }
          sb.append(name[i]);
        }
        log.log("Name is " + sb);

        //
        Field envField = c.getField("_env");
        Object _env = envField.get(pkgElt);
        log.log("env: " + _env);

        //
        Class _envClass = _env.getClass();
        Method getLookupEnvironmentMethod = _envClass.getMethod("getLookupEnvironment");
        Object le = getLookupEnvironmentMethod.invoke(_env);
        log.log("lookupEnvironment: " + le);

        //
        Class leClass = le.getClass();
        Method getTypeMethod = leClass.getMethod("getType", char[][].class);
        Object nameObj = name;
        Object referenceBinding = getTypeMethod.invoke(le, nameObj);
        log.log("Reference binding : " + referenceBinding);
        log.log("Reference binding type : " + referenceBinding.getClass());

        //
        Field scopeField = referenceBinding.getClass().getField("scope");
        Object scope = scopeField.get(referenceBinding);
        log.log("Scope is " + scope);

        //
        Field referenceContextField = scope.getClass().getField("referenceContext");
        Object referenceContext = referenceContextField.get(scope);
        log.log("Reference context : " + referenceContext);
        log.log("Reference context type : " + referenceContext.getClass().getName());

        Class referenceContextClass = referenceContext.getClass();
        Field annotationsField = referenceContextClass.getField("annotations");
        Field sourceStartField = referenceContextClass.getField("sourceStart");
        Field sourceEndField = referenceContextClass.getField("sourceEnd");
        Object annotations = annotationsField.get(referenceContext);
        int sourceStart = (Integer)sourceStartField.get(referenceContext);
        Object sourceEnd = sourceEndField.get(referenceContext);
        log.log("Annotations : " + annotations);
        log.log("Source start : " + sourceStart);
        log.log("Source end : " + sourceEnd);

        //
        Class referenceContextType = referenceContextClass.getClassLoader().loadClass("org.eclipse.jdt.internal.compiler.impl.ReferenceContext");

        //
        Method compilationResultMethod = referenceContextClass.getMethod("compilationResult");
        Object compilationResult = compilationResultMethod.invoke(referenceContext);
        log.log("Compilation result : " + compilationResult);

        //
        Class<?> compilationResultClass = compilationResult.getClass();
        Method getLineSeparatorPositionsMethod = compilationResultClass.getMethod("getLineSeparatorPositions");
        int[] lineEnds = (int[])getLineSeparatorPositionsMethod.invoke(compilationResult);

        StringBuilder sb2 = new StringBuilder("Line ends :");
        for (int i : lineEnds) {
          sb2.append(" ").append(i);
        }
        log.log(sb2);

        //
        Class utilClass = referenceContextClass.getClassLoader().loadClass("org.eclipse.jdt.internal.compiler.util.Util");
        Method getLineNumberMethod = utilClass.getMethod("getLineNumber", int.class, int[].class, int.class, int.class);
        Method searchColumnNumber = utilClass.getMethod("searchColumnNumber", int[].class, int.class, int.class);
        int lineNumber = sourceStart >= 0 ? (Integer)getLineNumberMethod.invoke(null, sourceStart, lineEnds, 0, lineEnds.length - 1) : 0;
        log.log("Line number : " + lineNumber);
        int columnNumber = sourceStart >= 0 ? (Integer)searchColumnNumber.invoke(null, lineEnds, lineNumber, sourceStart) : 0;
        log.log("Column number : " + columnNumber);

        //
        Field fileNameField = compilationResultClass.getField("fileName");
        char[] fileName = (char[])fileNameField.get(compilationResult);
        log.log("File name : " + new String(fileName));

        int severity = 1;

        Class<?> aptProblemClass = utilClass.getClassLoader().loadClass("org.eclipse.jdt.internal.compiler.apt.dispatch.AptProblem");
        Constructor aptProblemCtor = aptProblemClass.getConstructor(
          referenceContextType,
          char[].class,
          String.class,
          int.class,
          String[].class,
          int.class,
          int.class,
          int.class,
          int.class,
          int.class
        );
        Object aptProblem = aptProblemCtor.newInstance(
          referenceContext,
          fileName,
          msg.toString(),
          0,
          new String[0],
          severity,
          sourceStart,
          sourceEnd,
          lineNumber,
          columnNumber
        );

        log.log("Apt problem " + aptProblem);

        //
        Field _processingEnvField = messager.getClass().getDeclaredField("_processingEnv");
        _processingEnvField.setAccessible(true);
        Object _processingEnv = _processingEnvField.get(messager);
        Class<?> categorizedProblemClass = compilationResultClass.getClassLoader().loadClass("org.eclipse.jdt.core.compiler.CategorizedProblem");

        //
        Field _compilerField = messager.getClass().getDeclaredField("_compiler");
        _compilerField.setAccessible(true);
        Object _compiler = _compilerField.get(messager);
        Method addExtraProblemsMethod = _compiler.getClass().getMethod("addExtraProblems", categorizedProblemClass);
        Method setErrorRaisedMethod = _processingEnv.getClass().getMethod("setErrorRaised", boolean.class);

        // if (kind == Kind.ERROR) {
        //   _processingEnv.setErrorRaised(true);
        // }
        if (kind == Diagnostic.Kind.ERROR) {
          setErrorRaisedMethod.invoke(_processingEnv, true);
        }

        // _compiler.addExtraProblems(problem);
        addExtraProblemsMethod.invoke(_compiler, aptProblem);


        // OLD WAY / WE KEEP HERE IF WE NEED IT
//        Class<?> categorizedProblemClass = compilationResultClass.getClassLoader().loadClass("org.eclipse.jdt.core.compiler.CategorizedProblem");
//        Method recordMethod = compilationResultClass.getMethod(
//          "record",
//          categorizedProblemClass,
//          referenceContextType);
//        recordMethod.invoke(compilationResult, aptProblem, referenceContext);
//        log.log("Recorded problem");

        //


      }
      catch (Exception e) {
        log.log("Could not make it work", e);
      }
    }

    @Override
    public boolean getOverwriteReadingResource() {
      return true;
    }
  };

  /** . */
  private static final Logger log = BaseProcessor.getLogger(ProcessingTool.class);

  public abstract void report(
    Messager messager,
    Diagnostic.Kind kind,
    CharSequence msg,
    Element element,
    AnnotationMirror annotation,
    AnnotationValue value);

  /**
   * Returns true if the processing context should reuse a cached resource when writing a resource.
   *
   * @return the write cached resource value
   */
  public abstract boolean getOverwriteReadingResource();

}
