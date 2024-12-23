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
package org.eclipse.aether.spi.connector.transport.http.RFC9457;

import java.net.URI;

public class RFC9457Payload {
    public static final RFC9457Payload INSTANCE = new RFC9457Payload();

    private final URI type;

    private final Integer status;

    private final String title;

    private final String detail;

    private final URI instance;

    private RFC9457Payload() {
        this(null, null, null, null, null);
    }

    public RFC9457Payload(
            final URI type, final Integer status, final String title, final String detail, final URI instance) {
        this.type = type;
        this.status = status;
        this.title = title;
        this.detail = detail;
        this.instance = instance;
    }

    public URI getType() {
        return type;
    }

    public Integer getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public URI getInstance() {
        return instance;
    }

    @Override
    public String toString() {
        return "RFC9457Payload {" + "type="
                + type + ", status="
                + status + ", title='"
                + title + ", detail='"
                + detail + ", instance="
                + instance + '}';
    }
}
