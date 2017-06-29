/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.coretutorials;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.List;
import org.opendaylight.mdsal.binding.javav2.generator.api.BindingGenerator;
import org.opendaylight.mdsal.binding.javav2.generator.impl.BindingGeneratorImpl;
import org.opendaylight.mdsal.binding.javav2.model.api.Type;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
import org.openjdk.jmh.annotations.Benchmark;

/**
 * $ cd binding2-benchmark/
 * $ mvn clean install
 * Running the benchmarks. After the build is done, you will get the self-contained executable JAR,
 *     which holds your benchmark, and all essential JMH infrastructure code:
 * $ java -jar target/benchmarks.jar
 */
public class MyBenchmark {

    @Benchmark
    public void testBindingV1() throws ReactorException, FileNotFoundException, URISyntaxException {
        final SchemaContext context = YangParserTestUtils.parseYangSources("/yangs/");
        final org.opendaylight.mdsal.binding.generator.api.BindingGenerator bindingGenerator = new org
                .opendaylight.mdsal.binding.generator.impl.BindingGeneratorImpl(true);
        final List<org.opendaylight.mdsal.binding.model.api.Type> types = bindingGenerator.generateTypes(context, context.getModules());
    }

    @Benchmark
    public void testBindingV2() throws ReactorException, FileNotFoundException, URISyntaxException {
        final SchemaContext context = YangParserTestUtils.parseYangSources("/yangs/");
        final BindingGenerator bindingGenerator = new BindingGeneratorImpl(true);
        final List<Type> types = bindingGenerator.generateTypes(context, context.getModules());
    }
}
