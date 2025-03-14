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
package org.eclipse.aether.internal.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.internal.test.util.TestFileUtils;
import org.eclipse.aether.internal.test.util.TestPathProcessor;
import org.eclipse.aether.internal.test.util.TestUtils;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.MergeableMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.metadata.Metadata.Nature;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.ArtifactUpload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.spi.connector.MetadataUpload;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultDeployerTest {

    private Artifact artifact;

    private DefaultMetadata metadata;

    private DefaultRepositorySystemSession session;

    private StubRepositoryConnectorProvider connectorProvider;

    private DefaultDeployer deployer;

    private DeployRequest request;

    private RecordingRepositoryConnector connector;

    private RecordingRepositoryListener listener;

    @BeforeEach
    void setup() throws IOException {
        artifact = new DefaultArtifact("gid", "aid", "jar", "ver");
        artifact = artifact.setFile(TestFileUtils.createTempFile("artifact"));
        metadata = new DefaultMetadata(
                "gid", "aid", "ver", "type", Nature.RELEASE_OR_SNAPSHOT, TestFileUtils.createTempFile("metadata"));

        session = TestUtils.newSession();
        connectorProvider = new StubRepositoryConnectorProvider();

        deployer = new DefaultDeployer(
                new TestPathProcessor(),
                new StubRepositoryEventDispatcher(),
                connectorProvider,
                new StubRemoteRepositoryManager(),
                new StaticUpdateCheckManager(true),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                new StubSyncContextFactory(),
                new DefaultOfflineController());

        request = new DeployRequest();
        request.setRepository(new RemoteRepository.Builder("id", "default", "file:///").build());
        connector = new RecordingRepositoryConnector(session);
        connectorProvider.setConnector(connector);

        listener = new RecordingRepositoryListener();
        session.setRepositoryListener(listener);
    }

    @AfterEach
    void teardown() throws Exception {
        if (session.getLocalRepository() != null) {
            TestFileUtils.deleteFile(session.getLocalRepository().getBasedir());
        }
        session = null;
        listener = null;
        connector = null;
        connectorProvider = null;
        deployer = null;
    }

    @Test
    void testSuccessfulDeploy() throws DeploymentException {

        connector.setExpectPut(artifact);
        connector.setExpectPut(metadata);

        request.addArtifact(artifact);
        request.addMetadata(metadata);

        deployer.deploy(session, request);

        connector.assertSeenExpected();
    }

    @Test
    void testNullArtifactFile() {
        request.addArtifact(artifact.setFile(null));
        assertThrows(DeploymentException.class, () -> deployer.deploy(session, request));
    }

    @Test
    void testNullMetadataFile() {
        request.addMetadata(metadata.setFile(null));
        assertThrows(DeploymentException.class, () -> deployer.deploy(session, request));
    }

    @Test
    void testSuccessfulArtifactEvents() throws DeploymentException {
        request.addArtifact(artifact);

        deployer.deploy(session, request);

        List<RepositoryEvent> events = listener.getEvents();
        assertEquals(2, events.size());

        RepositoryEvent event = events.get(0);
        assertEquals(EventType.ARTIFACT_DEPLOYING, event.getType());
        assertEquals(artifact, event.getArtifact());
        assertNull(event.getException());

        event = events.get(1);
        assertEquals(EventType.ARTIFACT_DEPLOYED, event.getType());
        assertEquals(artifact, event.getArtifact());
        assertNull(event.getException());
    }

    @Test
    void testFailingArtifactEvents() {
        connector.fail = true;

        request.addArtifact(artifact);

        try {
            deployer.deploy(session, request);
            fail("expected exception");
        } catch (DeploymentException e) {
            List<RepositoryEvent> events = listener.getEvents();
            assertEquals(2, events.size());

            RepositoryEvent event = events.get(0);
            assertEquals(EventType.ARTIFACT_DEPLOYING, event.getType());
            assertEquals(artifact, event.getArtifact());
            assertNull(event.getException());

            event = events.get(1);
            assertEquals(EventType.ARTIFACT_DEPLOYED, event.getType());
            assertEquals(artifact, event.getArtifact());
            assertNotNull(event.getException());
        }
    }

    @Test
    void testSuccessfulMetadataEvents() throws DeploymentException {
        request.addMetadata(metadata);

        deployer.deploy(session, request);

        List<RepositoryEvent> events = listener.getEvents();
        assertEquals(2, events.size());

        RepositoryEvent event = events.get(0);
        assertEquals(EventType.METADATA_DEPLOYING, event.getType());
        assertEquals(metadata, event.getMetadata());
        assertNull(event.getException());

        event = events.get(1);
        assertEquals(EventType.METADATA_DEPLOYED, event.getType());
        assertEquals(metadata, event.getMetadata());
        assertNull(event.getException());
    }

    @Test
    void testFailingMetdataEvents() {
        connector.fail = true;

        request.addMetadata(metadata);

        try {
            deployer.deploy(session, request);
            fail("expected exception");
        } catch (DeploymentException e) {
            List<RepositoryEvent> events = listener.getEvents();
            assertEquals(2, events.size());

            RepositoryEvent event = events.get(0);
            assertEquals(EventType.METADATA_DEPLOYING, event.getType());
            assertEquals(metadata, event.getMetadata());
            assertNull(event.getException());

            event = events.get(1);
            assertEquals(EventType.METADATA_DEPLOYED, event.getType());
            assertEquals(metadata, event.getMetadata());
            assertNotNull(event.getException());
        }
    }

    @Test
    void testStaleLocalMetadataCopyGetsDeletedBeforeMergeWhenMetadataIsNotCurrentlyPresentInRemoteRepo()
            throws Exception {
        MergeableMetadata metadata = new MergeableMetadata() {

            public Metadata setFile(File file) {
                return this;
            }

            public Metadata setPath(Path path) {
                return this;
            }

            public String getVersion() {
                return "";
            }

            public String getType() {
                return "test.properties";
            }

            public Nature getNature() {
                return Nature.RELEASE;
            }

            public String getGroupId() {
                return "org";
            }

            public File getFile() {
                return null;
            }

            public Path getPath() {
                return null;
            }

            public String getArtifactId() {
                return "aether";
            }

            public Metadata setProperties(Map<String, String> properties) {
                return this;
            }

            public Map<String, String> getProperties() {
                return Collections.emptyMap();
            }

            public String getProperty(String key, String defaultValue) {
                return defaultValue;
            }

            public void merge(Path current, Path result) throws RepositoryException {
                merge(current != null ? current.toFile() : null, result != null ? result.toFile() : null);
            }

            public void merge(File current, File result) throws RepositoryException {
                requireNonNull(current, "current cannot be null");
                requireNonNull(result, "result cannot be null");
                Properties props = new Properties();

                try {
                    if (current.isFile()) {
                        TestFileUtils.readProps(current, props);
                    }

                    props.setProperty("new", "value");

                    TestFileUtils.writeProps(result, props);
                } catch (IOException e) {
                    throw new RepositoryException(e.getMessage(), e);
                }
            }

            public boolean isMerged() {
                return false;
            }
        };

        connectorProvider.setConnector(new RepositoryConnector() {

            public void put(
                    Collection<? extends ArtifactUpload> artifactUploads,
                    Collection<? extends MetadataUpload> metadataUploads) {}

            public void get(
                    Collection<? extends ArtifactDownload> artifactDownloads,
                    Collection<? extends MetadataDownload> metadataDownloads) {
                if (metadataDownloads != null) {
                    for (MetadataDownload download : metadataDownloads) {
                        download.setException(new MetadataNotFoundException(download.getMetadata(), null, null));
                    }
                }
            }

            public void close() {}
        });

        request.addMetadata(metadata);

        File metadataFile = new File(
                session.getLocalRepository().getBasedir(),
                session.getLocalRepositoryManager().getPathForRemoteMetadata(metadata, request.getRepository(), ""));
        Properties props = new Properties();
        props.setProperty("old", "value");
        TestFileUtils.writeProps(metadataFile, props);

        deployer.deploy(session, request);

        props = new Properties();
        TestFileUtils.readProps(metadataFile, props);
        assertNull(props.get("old"), props.toString());
    }
}
