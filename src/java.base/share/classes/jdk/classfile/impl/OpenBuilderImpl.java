package jdk.classfile.impl;

import jdk.classfile.*;
import jdk.classfile.OpenBuilder;
import jdk.classfile.constantpool.Utf8Entry;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.function.Consumer;

public sealed abstract class OpenBuilderImpl<B, D extends B> implements OpenBuilder<B> {

    protected final D directBuilder;

    protected boolean closed;

    protected OpenBuilder<?> child;

    protected OpenBuilderImpl(D directBuilder) {
        this.directBuilder = directBuilder;
    }

    @Override
    public void accept(Consumer<? super B> consumer) {
        assertNotClosed();
        assertNoChild();
        consumer.accept(directBuilder);
    }

    void assertNotClosed() {
        if (closed) {
            throw new IllegalStateException();
        }
    }

    void assertNoChild() {
        if (child != null) {
            throw new IllegalStateException();
        }
    }

    void defineChild(OpenBuilder<?> newChild) {
        assertNotClosed();
        assertNoChild();
        child = newChild;
    }

    void clearChild(OpenBuilder<?> oldChild) {
        if (child != oldChild) {
            throw new IllegalStateException();
        }
        child = null;
    }

    public static final class OpenClassBuilderImpl extends OpenBuilderImpl<ClassBuilder, DirectClassBuilder> implements OpenClassBuilder {

        public OpenClassBuilderImpl(DirectClassBuilder directClassBuilder) {
            super(directClassBuilder);
        }

        @Override
        public OpenFieldBuilder withField(Utf8Entry name, Utf8Entry descriptor) {
            var directFieldBuilder = new DirectFieldBuilder(directBuilder.constantPool, name, descriptor, null);
            return new OpenFieldBuilderImpl(directFieldBuilder);
        }

        @Override
        public OpenFieldBuilder withField(String name, ClassDesc descriptor) {
            return withField(directBuilder.constantPool.utf8Entry(name), directBuilder.constantPool.utf8Entry(descriptor));
        }

        @Override
        public OpenMethodBuilder withMethod(Utf8Entry name, Utf8Entry descriptor, int methodFlags) {
            var directMethodBuilder = new DirectMethodBuilder(directBuilder.constantPool, name, descriptor, methodFlags, null);
            return new OpenMethodBuilderImpl(directMethodBuilder);
        }

        @Override
        public OpenMethodBuilder withMethod(String name, MethodTypeDesc descriptor, int methodFlags) {
            return withMethod(directBuilder.constantPool.utf8Entry(name), directBuilder.constantPool.utf8Entry(descriptor), methodFlags);
        }

        @Override
        public byte[] toClassfile() {
            close();
            return directBuilder.build();
        }

        @Override
        public void close() {
            if (!closed) {
                assertNoChild();
                closed = true;
            }
        }

        public final class OpenFieldBuilderImpl extends OpenBuilderImpl<FieldBuilder, DirectFieldBuilder> implements OpenFieldBuilder {

            public OpenFieldBuilderImpl(DirectFieldBuilder directFieldBuilder) {
                super(directFieldBuilder);
                OpenClassBuilderImpl.this.defineChild(this);
            }

            @Override
            public void close() {
                if (!closed) {
                    OpenClassBuilderImpl.this.clearChild(this);
                    assertNoChild();
                    OpenClassBuilderImpl.this.directBuilder.withField(directBuilder);
                    closed = true;
                }
            }
        }

        public final class OpenMethodBuilderImpl extends OpenBuilderImpl<MethodBuilder, DirectMethodBuilder> implements OpenMethodBuilder {

            public OpenMethodBuilderImpl(DirectMethodBuilder directMethodBuilder) {
                super(directMethodBuilder);
                OpenClassBuilderImpl.this.defineChild(this);
            }

            @Override
            public OpenCodeBuilder withCode() {
                // TODO: cannot reproduce duplicate application to fix wide forward jumps.
                var directCodeBuilder = new DirectCodeBuilder(directBuilder, OpenClassBuilderImpl.this.directBuilder.constantPool, null, false);
                return new OpenCodeBuilderImpl(directCodeBuilder);
            }

            @Override
            public void close() {
                if (!closed) {
                    OpenClassBuilderImpl.this.clearChild(this);
                    assertNoChild();
                    OpenClassBuilderImpl.this.directBuilder.withMethod(directBuilder);
                    closed = true;
                }
            }

            public final class OpenCodeBuilderImpl extends OpenBuilderImpl<CodeBuilder, DirectCodeBuilder> implements OpenCodeBuilder {

                public OpenCodeBuilderImpl(DirectCodeBuilder directCodeBuilder) {
                    super(directCodeBuilder);
                    OpenMethodBuilderImpl.this.defineChild(this);
                }

                @Override
                public void close() {
                    if (!closed) {
                        OpenMethodBuilderImpl.this.clearChild(this);
                        assertNoChild();
                        OpenMethodBuilderImpl.this.directBuilder.writeAttribute(directBuilder.buildContent());
                        closed = true;
                    }
                }
            }
        }
    }
}
