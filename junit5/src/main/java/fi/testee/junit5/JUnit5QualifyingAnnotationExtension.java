/*
 * Copyright (C) 2017 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.testee.junit5;

import fi.testee.spi.QualifyingAnnotationExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Annotation;
import java.util.Collection;

import static java.util.Arrays.asList;

public class JUnit5QualifyingAnnotationExtension implements QualifyingAnnotationExtension {
    @Override
    public Collection<Class<? extends Annotation>> getQualifyingAnnotations() {
        return asList(Test.class, TestFactory.class, ExtendWith.class);
    }
}
