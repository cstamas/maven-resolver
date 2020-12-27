package org.eclipse.aether.named.providers;

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

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import org.eclipse.aether.named.NamedLockFactory;
import org.eclipse.aether.named.support.hazelcast.HazelcastSemaphoreNamedLockFactory;

/**
 * Provider of {@link HazelcastSemaphoreNamedLockFactory} using Hazelcast Client and {@link
 * HazelcastInstance#getSemaphore(String)} method.
 */
@Singleton
@Named( "semaphore-hazelcast-client" )
public class HazelcastClientSemaphoreProvider
    implements Provider<NamedLockFactory>
{
  @Override
  public NamedLockFactory get()
  {
    return new HazelcastSemaphoreNamedLockFactory(
        HazelcastClient.newHazelcastClient(),
        HazelcastInstance::getSemaphore,
        true,
        true
    );
  }
}
