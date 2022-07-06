package jdk.classfile;

import jdk.classfile.constantpool.Utf8Entry;
import jdk.classfile.impl.OpenBuilderImpl;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.function.Consumer;

public sealed interface OpenBuilder<B>
        extends Consumer<Consumer<? super B>>, AutoCloseable
        permits OpenBuilderImpl, OpenBuilder.OpenClassBuilder, OpenBuilder.OpenFieldBuilder, OpenBuilder.OpenMethodBuilder, OpenBuilder.OpenCodeBuilder {

    @Override
    void close();

    sealed interface OpenClassBuilder extends OpenBuilder<ClassBuilder> permits OpenBuilderImpl.OpenClassBuilderImpl {

        OpenFieldBuilder withField(Utf8Entry name, Utf8Entry descriptor);

        OpenFieldBuilder withField(String name, ClassDesc descriptor);

        OpenMethodBuilder withMethod(Utf8Entry name, Utf8Entry descriptor, int methodFlags);

        OpenMethodBuilder withMethod(String name, MethodTypeDesc descriptor, int methodFlags);

        byte[] toClassfile();
    }

    sealed interface OpenFieldBuilder extends OpenBuilder<FieldBuilder>, AutoCloseable permits OpenBuilderImpl.OpenClassBuilderImpl.OpenFieldBuilderImpl {
    }

    sealed interface OpenMethodBuilder extends OpenBuilder<MethodBuilder>, AutoCloseable permits OpenBuilderImpl.OpenClassBuilderImpl.OpenMethodBuilderImpl {

        OpenCodeBuilder withCode();
    }

    sealed interface OpenCodeBuilder extends OpenBuilder<CodeBuilder>, AutoCloseable permits OpenBuilderImpl.OpenClassBuilderImpl.OpenMethodBuilderImpl.OpenCodeBuilderImpl {
    }
}
