package com.framework.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * RetryTransformer — Applies RetryAnalyzer to every @Test automatically.
 *
 * Register in testng.xml:
 *   <listeners>
 *     <listener class-name="com.framework.listeners.RetryTransformer"/>
 *   </listeners>
 */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation,
                          Class testClass,
                          Constructor testConstructor,
                          Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
