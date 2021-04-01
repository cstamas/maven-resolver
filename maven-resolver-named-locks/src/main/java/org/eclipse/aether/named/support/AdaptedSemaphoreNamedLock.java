package org.eclipse.aether.named.support;

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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

/**
 * Named lock support implementation that is using "adapted" semaphore (to be able to use semaphores not sharing common
 * API).
 */
public class AdaptedSemaphoreNamedLock
    extends NamedLockSupport
{
    /**
     * Wrapper for semaphore-like stuff, that do not share common ancestor. Semaphore must be created to support
     * {@link Integer#MAX_VALUE} permissions.
     */
    public interface AdaptedSemaphore
    {
        boolean tryAcquire( int perms, long timeout, TimeUnit unit ) throws InterruptedException;

        void release( int perms );
    }

    private final ThreadLocal<Deque<Integer>> threadPerms;

    private final AdaptedSemaphore semaphore;

    public AdaptedSemaphoreNamedLock( final String name,
                                      final NamedLockFactorySupport factory,
                                      final AdaptedSemaphore semaphore )
    {
        super( name, factory );
        this.threadPerms = ThreadLocal.withInitial( ArrayDeque::new );
        this.semaphore = semaphore;
    }

    @Override
    public boolean lockShared( final long time, final TimeUnit unit ) throws InterruptedException
    {
        Deque<Integer> perms = threadPerms.get();
        if ( !perms.isEmpty() )
        { // we already own shared or exclusive lock
            perms.push( 0 );
            return true;
        }
        if ( semaphore.tryAcquire( 1, time, unit ) )
        {
            perms.push( 1 );
            return true;
        }
        return false;
    }

    @Override
    public boolean lockExclusively( final long time, final TimeUnit unit ) throws InterruptedException
    {
        Deque<Integer> perms = threadPerms.get();
        if ( !perms.isEmpty() )
        { // we already own shared or exclusive lock
            if ( perms.contains( Integer.MAX_VALUE ) )
            {
                perms.push( 0 );
                return true;
            }
            else
            {
                return false; // Lock upgrade not supported
            }
        }
        if ( semaphore.tryAcquire( Integer.MAX_VALUE, time, unit ) )
        {
            perms.push( Integer.MAX_VALUE );
            return true;
        }
        return false;
    }

    @Override
    public void unlock()
    {
        Deque<Integer> steps = threadPerms.get();
        if ( steps.isEmpty() )
        {
            throw new IllegalStateException( "Wrong API usage: unlock w/o lock" );
        }
        int step = steps.pop();
        if ( step > 0 )
        {
            semaphore.release( step );
        }
    }
}
