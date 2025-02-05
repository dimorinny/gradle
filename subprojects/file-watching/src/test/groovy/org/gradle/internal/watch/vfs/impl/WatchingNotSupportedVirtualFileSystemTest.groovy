/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.watch.vfs.impl

import org.gradle.internal.operations.TestBuildOperationExecutor
import org.gradle.internal.snapshot.CaseSensitivity
import org.gradle.internal.snapshot.SnapshotHierarchy
import org.gradle.internal.vfs.impl.DefaultSnapshotHierarchy
import org.gradle.internal.vfs.impl.VfsRootReference
import org.gradle.internal.watch.vfs.VfsLogging
import org.gradle.internal.watch.vfs.WatchLogging
import org.gradle.internal.watch.vfs.WatchMode
import spock.lang.Specification

class WatchingNotSupportedVirtualFileSystemTest extends Specification {
    def emptySnapshotHierarchy = DefaultSnapshotHierarchy.empty(CaseSensitivity.CASE_SENSITIVE)
    def nonEmptySnapshotHierarchy = Stub(SnapshotHierarchy) {
        empty() >> emptySnapshotHierarchy
    }
    def rootReference = new VfsRootReference(nonEmptySnapshotHierarchy)
    def watchingNotSupportedHandler = new WatchingNotSupportedVirtualFileSystem(rootReference)
    def buildOperationRunner = new TestBuildOperationExecutor()

    def "invalidates the virtual file system before and after the build (watch mode: #watchMode.description)"() {
        when:
        watchingNotSupportedHandler.afterBuildStarted(watchMode, VfsLogging.NORMAL, WatchLogging.NORMAL, buildOperationRunner)
        then:
        rootReference.getRoot() == emptySnapshotHierarchy

        when:
        rootReference.update { root -> nonEmptySnapshotHierarchy }
        watchingNotSupportedHandler.beforeBuildFinished(watchMode, VfsLogging.NORMAL, WatchLogging.NORMAL, buildOperationRunner, Integer.MAX_VALUE)
        then:
        rootReference.getRoot() == emptySnapshotHierarchy

        where:
        watchMode << WatchMode.values().toList()
    }
}
