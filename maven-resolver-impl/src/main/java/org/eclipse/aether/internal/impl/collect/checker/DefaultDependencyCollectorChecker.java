/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.eclipse.aether.internal.impl.collect.checker;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.internal.impl.collect.DependencyCollectorChecker;
import org.eclipse.aether.internal.impl.scope.OptionalDependencySelector;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.util.ConfigUtils;

/**
 * Default implementation.
 */
@Singleton
@Named
public class DefaultDependencyCollectorChecker implements DependencyCollectorChecker {
    private final ArtifactDescriptorReader artifactDescriptorReader;

    @Inject
    public DefaultDependencyCollectorChecker(ArtifactDescriptorReader artifactDescriptorReader) {
        this.artifactDescriptorReader = artifactDescriptorReader;
    }

    @Override
    public RepositorySystemSession prepare(RepositorySystemSession session, CollectRequest request) {
        if (isActive(session)) {
            session.getData().set(OptionalDependencySelector.UNSELECTED_KEYS, ConcurrentHashMap.newKeySet());
        }
        return session;
    }

    @Override
    public boolean isSatisfactory(RepositorySystemSession session, CollectRequest request, CollectResult result)
            throws DependencyCollectionException {
        if (isActive(session)) {
            boolean finished = checkOptionalConditions(session, request, result);
            if (finished) {
                finished = checkConsumersProviders(session, request, result);
            }
            return finished;
        }
        return true;
    }

    private boolean isActive(RepositorySystemSession session) {
        return !ConfigUtils.getBoolean(session, false, COLLECTOR_CHECKER_SUPPRESSED);
    }

    @SuppressWarnings("unchecked")
    private boolean checkOptionalConditions(
            RepositorySystemSession session, CollectRequest request, CollectResult result) {
        Set<String> unselectedKeys = (Set<String>) session.getData()
                .computeIfAbsent(OptionalDependencySelector.UNSELECTED_KEYS, ConcurrentHashMap::newKeySet);
        if (!unselectedKeys.isEmpty()) {
            Set<String> activatedOptionals = new HashSet<>();
            for (String key : unselectedKeys) {
                Artifact artifact = new DefaultArtifact(key);
                try {
                    ArtifactDescriptorResult descriptor = artifactDescriptorReader.readArtifactDescriptor(
                            session,
                            new ArtifactDescriptorRequest(
                                    artifact, request.getRepositories(), request.getRequestContext()));
                    if (descriptor.getArtifact().getProperties().containsKey("activationCondition")) {
                        // artifactPresent(org.slf4j:slf4j-api)
                        String condition =
                                descriptor.getArtifact().getProperties().get("activationCondition");
                        boolean activated = false;
                        if (condition.startsWith("artifactPresent(") && condition.endsWith(")")) {
                            String cond = condition.substring("artifactPresent(".length(), condition.length() - 1);
                            activated = artifactPresent(
                                    a -> cond.equals(a.getGroupId() + ":" + a.getArtifactId()), result.getRoot());
                        }
                        if (activated) {
                            activatedOptionals.add(key);
                        }
                    }
                } catch (ArtifactDescriptorException e) {
                    // ignore
                }
            }
            if (!activatedOptionals.isEmpty()) {
                session.getData().set(OptionalDependencySelector.IGNORED_KEYS, activatedOptionals);
                session.getData().set(OptionalDependencySelector.UNSELECTED_KEYS, ConcurrentHashMap.newKeySet());
                return false;
            }
        }
        return true;
    }
    // conditional dependencies: go over list of rejected deps due optional=true, look up their descriptors, check do
    // they have
    // conditions. If they have, calculate list of those excluded by should not have been, stick into session; return
    // false;

    private boolean checkConsumersProviders(
            RepositorySystemSession session, CollectRequest request, CollectResult result)
            throws DependencyCollectionException {
        return true;
    }
    // capabilities: compare consumes/provides matches? Unpaired: fail, paired return = true; dynamic: modify request
    // add new dep

    // ---

    private boolean artifactPresent(Predicate<Artifact> predicate, DependencyNode root) {
        AtomicBoolean found = new AtomicBoolean(false);
        DependencyVisitor search = new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode node) {
                if (!found.get() && node.getArtifact() != null && predicate.test(node.getArtifact())) {
                    found.set(true);
                    return false;
                }
                return !found.get();
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                return !found.get();
            }
        };
        root.accept(search);
        return found.get();
    }
}
