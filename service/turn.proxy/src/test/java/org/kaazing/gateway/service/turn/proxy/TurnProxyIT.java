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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.rules.RuleChain.outerRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.gateway.util.feature.EarlyAccessFeatures;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class TurnProxyIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/gateway/service/turn/proxy");

    private final TestRule timeout = new DisableOnDebug(new Timeout(5, SECONDS));

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .property(EarlyAccessFeatures.TURN_PROXY.getPropertyName(), "true")
                        .service()
                            .accept("tcp://localhost:3478")
                            .connect("tcp://localhost:3479")
                            .type("turn.proxy")
//                            .property("relay.address.mask", propertyValue)
//                            .property("mapped.address", propertyValue)
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public final TestRule chain = outerRule(gateway).around(k3po).around(timeout);

    @Test
    @Specification({"default.turn.protocol.test/request", "default.turn.protocol.test/response"})
    public void shouldPassWithDefaultTurnProtocolTest() throws Exception {
        // TODO
        k3po.finish();
    }

}
