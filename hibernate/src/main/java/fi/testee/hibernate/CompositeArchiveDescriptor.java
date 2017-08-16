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
package fi.testee.hibernate;

import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;

import java.util.Collection;

public class CompositeArchiveDescriptor implements ArchiveDescriptor {
    private final Collection<ArchiveDescriptor> archiveDescriptors;

    public CompositeArchiveDescriptor(final Collection<ArchiveDescriptor> archiveDescriptors) {
        this.archiveDescriptors = archiveDescriptors;
    }

    @Override
    public void visitArchive(final ArchiveContext archiveContext) {
        archiveDescriptors.forEach(it -> it.visitArchive(archiveContext));
    }
}
