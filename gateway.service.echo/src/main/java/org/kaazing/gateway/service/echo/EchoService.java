/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 * 
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

package org.kaazing.gateway.service.echo;

import static org.kaazing.gateway.service.util.ServiceUtils.getOptionalIntProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.kaazing.gateway.service.AcceptOptionsContext;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;

/**
 * Gateway service of type "echo".
 */
public class EchoService implements Service {
    private EchoServiceHandler handler;
    private ServiceContext serviceContext;

    public EchoService() {
    }

    @Override
    public String getType() {
        return "echo";
    }

    @Override
    public void init(ServiceContext serviceContext) throws Exception {
        this.serviceContext = serviceContext;

        ServiceProperties properties = serviceContext.getProperties();
        int repeatCount = getOptionalIntProperty(properties, "repeat", EchoServiceHandler.DEFAULT_REPEAT_COUNT);

        handler = new EchoServiceHandler(repeatCount);
    }

    @Override
    public void start() throws Exception {
        AcceptOptionsContext acceptOptions = serviceContext.getAcceptOptionsContext();
        serviceContext.bind(serviceContext.getAccepts(), handler, new AcceptOptionsContext.Wrapper(acceptOptions) {
            @Override
            public List<String> getWsProtocols() {

                List<String> result;

                List<String> supportedProtocols = super.getWsProtocols();
                List<String> echoProtocols = Arrays.asList("org.kaazing.echo", null);
                if ( supportedProtocols == null ) {
                    result = echoProtocols;
                } else {
                    result = new ArrayList<String>(supportedProtocols);
                    result.addAll(echoProtocols);
                }

                return result;
            }
            
        });
    }

    @Override
    public void stop() throws Exception {
        quiesce();

        if (serviceContext != null) {
            for (IoSession session : serviceContext.getActiveSessions()) {
                session.close(true);
            }
        }
    }

    @Override
    public void quiesce() throws Exception {
        if (serviceContext != null) {
            serviceContext.unbind(serviceContext.getAccepts(), handler);
        }
    }

    @Override
    public void destroy() throws Exception {
    }
}