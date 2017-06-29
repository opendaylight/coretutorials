/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.coretutorials;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import org.opendaylight.mdsal.binding.javav2.generator.api.BindingGenerator;
import org.opendaylight.mdsal.binding.javav2.generator.impl.BindingGeneratorImpl;
import org.opendaylight.mdsal.binding.javav2.model.api.Type;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * IMPORTANT NOTE:
 * Please bear in mind that Binding v1 codebase, with its limitations,
 * is not able to deal with all YANG inputs as Binding v2 does. In other words,
 * during benchmarking binding v1, process of transforming schema context to generated types may fail,
 * but it will continue for binding v2. For this reason, benchmark comparison itself is limited to YANG input set
 * processable by both bindings.
 *
 * $ cd binding2-benchmark/
 * copy your YANG files to src/main/resources
 * $ mvn clean install
 * Running the benchmarks. After the build is done, you will get the self-contained executable JAR,
 *     which holds your benchmark, and all essential JMH infrastructure code:
 * $ java -jar target/benchmarks.jar
 *
 *
 */
@State(Scope.Benchmark)
public class MyBenchmark {

    private static final Collection<String> LOCAL_SCHEMA_PATHS =
        Lists.newArrayList("/yangs/foo1.yang", "/yangs/test-augment-choice-case.yang");

    private SchemaContext context = null;

    @Setup
    public void setup() throws ReactorException, FileNotFoundException, URISyntaxException {
        context = YangParserTestUtils.parseYangStreams(loadYangs(LOCAL_SCHEMA_PATHS));
    }

    @Benchmark
    public void testBindingV1() {
        final org.opendaylight.mdsal.binding.generator.api.BindingGenerator bindingGenerator =
                new org.opendaylight.mdsal.binding.generator.impl.BindingGeneratorImpl(true);
        final List<org.opendaylight.mdsal.binding.model.api.Type> types = bindingGenerator.generateTypes(context, context.getModules());
    }

    @Benchmark
    public void testBindingV2() throws ReactorException, FileNotFoundException, URISyntaxException {
        final BindingGenerator bindingGenerator = new BindingGeneratorImpl(true);
        final List<Type> types = bindingGenerator.generateTypes(context, context.getModules());
    }

    private static List<InputStream> loadYangs(final Collection<String> yangPaths) {
        return Lists.newArrayList(Collections2.transform(Lists.newArrayList(yangPaths),
            input -> {
                final InputStream resourceAsStream = MyBenchmark.class.getResourceAsStream(input);
                Preconditions.checkNotNull(resourceAsStream, "File %s was null", input);
                return resourceAsStream;
            }));
    }
}
