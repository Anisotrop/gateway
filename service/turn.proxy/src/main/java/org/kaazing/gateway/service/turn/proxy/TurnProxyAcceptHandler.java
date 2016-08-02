/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
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
package org.kaazing.gateway.service.turn.proxy;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.proxy.AbstractProxyAcceptHandler;
import org.kaazing.gateway.service.proxy.AbstractProxyHandler;
import org.kaazing.gateway.service.turn.proxy.stun.StunCodecFilter;
import org.kaazing.gateway.service.turn.proxy.stun.StunMessageClass;
import org.kaazing.gateway.service.turn.proxy.stun.StunProxyMessage;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.Attribute;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.MappedAddress;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.XorMappedAddress;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.XorPeerAddress;
import org.kaazing.gateway.service.turn.proxy.stun.attributes.XorRelayAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TurnProxyAcceptHandler extends AbstractProxyAcceptHandler {

    static final Logger LOGGER = LoggerFactory.getLogger(TurnProxyAcceptHandler.class);
    public static final String TURN_STATE_KEY = "turn-state";

    private String connectURI;

    public TurnProxyAcceptHandler() {
        super();
    }

    public void init(ServiceContext serviceContext) {
        connectURI = serviceContext.getConnects().iterator().next();
    }

    @Override
    protected AbstractProxyHandler createConnectHandler() {
        return new TurnProxyConnectHandler(this);
    }

    @Override
    public void sessionCreated(IoSession acceptSession) {
        acceptSession.setAttribute(TURN_STATE_KEY, TurnSessionState.NOT_CONNECTED);
        acceptSession.getFilterChain().addLast("STUN_CODEC", new StunCodecFilter());
        // TODO
        // session.getFilterChain().addLast("STUN_MESSAGE_INTEGRITY_CHECK", new StunMessageIntegrityFilter());
        super.sessionCreated(acceptSession);
    }

    @Override
    public void sessionOpened(IoSession acceptSession) {
        ConnectSessionInitializer sessionInitializer = new ConnectSessionInitializer();
        ConnectFuture connectFuture = getServiceContext().connect(connectURI, getConnectHandler(), sessionInitializer);
        connectFuture.addListener(new ConnectListener(acceptSession));
        super.sessionOpened(acceptSession);
    }

    @Override
    public void messageReceived(IoSession session, Object message) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Received message [%s] from [%s]", message, session));
        }
        // For efficiency consider using map, set, or visitor pattern.
        if (message instanceof StunProxyMessage) {
            StunProxyMessage stunProxyMessage = (StunProxyMessage) message;
            for (Attribute attr : stunProxyMessage.getAttributes()) {
                if (attr instanceof MappedAddress || attr instanceof XorMappedAddress) {
                    // TODO
                } else if (attr instanceof XorPeerAddress || attr instanceof XorRelayAddress) {
                    // TODO

                }
            }
            super.messageReceived(session, message);

        }
    }

    /*
     * Initializer for connect session. It adds the processed accept session headers on the connect session
     */
    class ConnectSessionInitializer implements IoSessionInitializer<ConnectFuture> {

        @Override
        public void initializeSession(IoSession session, ConnectFuture future) {
            session.getFilterChain().addLast("STUN_CODEC", new StunCodecFilter());
            // TODO
            // session.getFilterChain().addLast("STUN_MESSAGE_INTEGRITY_CHECK", new StunMessageIntegrityFilter());
        }

    }

    private class ConnectListener implements IoFutureListener<ConnectFuture> {

        private final IoSession acceptSession;

        ConnectListener(IoSession acceptSession) {
            this.acceptSession = acceptSession;
        }

        @Override
        public void operationComplete(ConnectFuture future) {
            if (future.isConnected()) {
                IoSession connectSession = future.getSession();

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Connected to " + connectURI + " [" + acceptSession + "->" + connectSession + "]");
                }
                if (acceptSession == null || acceptSession.isClosing()) {
                    connectSession.close(true);
                } else {
                    AttachedSessionManager attachedSessionManager = attachSessions(acceptSession, connectSession);
                    flushQueuedMessages(acceptSession, attachedSessionManager);
                }
            } else {
                LOGGER.warn("Connection to " + connectURI + " failed [" + acceptSession + "->]");
                acceptSession.close(true);
            }
        }
    }

}