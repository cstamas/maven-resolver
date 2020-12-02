package org.eclipse.aether.internal.named.it;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.eclipse.aether.internal.named.SyncContextFactoryAdapter;
import org.eclipse.aether.internal.named.SyncContextFactoryAdapterTestSupport;
import org.eclipse.aether.internal.named.providers.HazelcastClientSemaphoreProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class HazelcastClientSemaphoreAdapterIT
    extends SyncContextFactoryAdapterTestSupport {

    private static HazelcastClientUtils utils;

    @BeforeClass
    public static void createNamedLockFactory() {
        utils = new HazelcastClientUtils().createSingleServer();
        adapter = new SyncContextFactoryAdapter(new HazelcastClientSemaphoreProvider().get(), ADAPTER_TIME, ADAPTER_TIME_UNIT);
    }


    @AfterClass
    public static void cleanup() {
        if (adapter != null) {
            adapter.shutdown();
        }
        if (utils != null) {
            utils.cleanup();
        }
    }
}
