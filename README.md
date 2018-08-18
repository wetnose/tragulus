# Tragulus

It is a helper library that is useful for creating java compiler plugins 
based on the Java Annotation Processing mechanism. It utilizes the internal Javac API 
for modifying java sources.

## Getting Started

Look at the source code of the **Tragulus Sugar** sample.
It declares a custom operator `opt` that works in the same way as the Elvis Operator in Kotlin.

It converts the following code

    int fileCount = opt(getFilesDir().listFiles().length, 0)

to something like 

    File dir;
    File[] files;
    int fileCount = (dir = getFilesDir()) != null 
                 && (files = dir.listFiles()) != null 
                   ? files.length : 0;

To play with the operator just add dependencies to `tragulus` and `tragulus-sugar` libraries 
within the compileOnly (provided) scope.