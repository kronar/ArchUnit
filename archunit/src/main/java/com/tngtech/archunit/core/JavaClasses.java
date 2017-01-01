package com.tngtech.archunit.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class JavaClasses implements DescribedIterable<JavaClass>, Restrictable<JavaClass, JavaClasses> {
    private final ImmutableMap<String, JavaClass> classes;
    private final String description;

    JavaClasses(Map<String, JavaClass> classes) {
        this(classes, "classes");
    }

    JavaClasses(Map<String, JavaClass> classes, String description) {
        this.classes = ImmutableMap.copyOf(classes);
        this.description = description;
    }

    @Override
    public JavaClasses that(DescribedPredicate<? super JavaClass> predicate) {
        Map<String, JavaClass> matchingElements = Guava.Maps.filterValues(classes, predicate);
        String newDescription = String.format("%s that %s", description, predicate.getDescription());
        return new JavaClasses(matchingElements, newDescription);
    }

    public JavaClasses as(String description) {
        return new JavaClasses(classes, description);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{classes=" + classes + '}';
    }

    @Override
    public Iterator<JavaClass> iterator() {
        return classes.values().iterator();
    }

    public boolean contain(Class<?> reflectedType) {
        return classes.containsKey(reflectedType.getName());
    }

    public JavaClass get(Class<?> reflectedType) {
        return get(reflectedType.getName());
    }

    public JavaClass get(String typeName) {
        return checkNotNull(classes.get(typeName), "%s don't contain %s of type %s",
                getClass().getSimpleName(), JavaClass.class.getSimpleName(), typeName);
    }

    public static JavaClasses of(Iterable<JavaClass> classes) {
        Map<String, JavaClass> mapping = new HashMap<>();
        for (JavaClass clazz : classes) {
            mapping.put(clazz.getName(), clazz);
        }
        return new JavaClasses(mapping);
    }

    static JavaClasses of(Map<String, JavaClass> classes, ImportContext importContext) {
        CompletionProcess completionProcess = new CompletionProcess(classes.values(), importContext);
        for (JavaClass clazz : new JavaClasses(classes)) {
            completionProcess.completeClass(clazz);
        }
        completionProcess.finish();
        return new JavaClasses(classes);
    }

    private static class CompletionProcess {
        private final Set<JavaClass.CompletionProcess> classCompletionProcesses = new HashSet<>();
        private final Collection<JavaClass> classes;
        private final ImportContext context;

        CompletionProcess(Collection<JavaClass> classes, ImportContext context) {
            this.classes = classes;
            this.context = context;
        }

        void completeClass(JavaClass clazz) {
            classCompletionProcesses.add(clazz.completeFrom(context));
        }

        void finish() {
            AccessContext.TopProcess accessCompletionProcess = new AccessContext.TopProcess(classes);
            for (JavaClass.CompletionProcess process : classCompletionProcesses) {
                accessCompletionProcess.mergeWith(process.completeCodeUnitsFrom(context));
            }
            accessCompletionProcess.finish();
        }
    }
}
