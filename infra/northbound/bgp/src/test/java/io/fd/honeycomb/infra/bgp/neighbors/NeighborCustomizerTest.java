/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.infra.bgp.neighbors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.config.PeerBean;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.RIB;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafiBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.AfiSafisBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.Neighbor;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbors.NeighborKey;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Neighbors;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.PeerType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Config2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.BgpRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class NeighborCustomizerTest {
    private static final IpAddress IP = new IpAddress(new Ipv4Address("10.25.1.9"));
    private static final InstanceIdentifier<Neighbor> ID =
        InstanceIdentifier.create(Neighbors.class).child(Neighbor.class, new NeighborKey(IP));
    private static final KeyedInstanceIdentifier<Rib, RibKey> RIB_IID =
        InstanceIdentifier.create(BgpRib.class).child(Rib.class, new RibKey(new RibId("test-rib-id")));

    @Mock
    private RIB globalRib;
    @Mock
    private BGPPeerRegistry peerRegistry;
    @Mock
    private BGPTableTypeRegistryConsumer tableTypeRegistry;
    @Mock
    private WriteContext ctx;

    private NeighborCustomizer customizer;

    @Before
    public void setUp() {
        initMocks(this);
        when(globalRib.getYangRibId()).thenReturn(YangInstanceIdentifier.EMPTY);
        when(globalRib.getRibIServiceGroupIdentifier()).thenReturn(ServiceGroupIdentifier.create("sgid"));
        when(globalRib.getInstanceIdentifier()).thenReturn(RIB_IID);
        customizer = new NeighborCustomizer(globalRib, peerRegistry, tableTypeRegistry);
    }

    @Test
    public void testAddAppPeer() throws WriteFailedException {
        final Neighbor neighbor = new NeighborBuilder()
            .setNeighborAddress(IP)
            .setConfig(
                new ConfigBuilder()
                    .addAugmentation(
                        Config2.class,
                        new Config2Builder().setPeerGroup("application-peers").build()
                    ).build())
            .build();
        customizer.writeCurrentAttributes(ID, neighbor, ctx);
        assertTrue(customizer.isPeerConfigured(ID));
    }

    @Test
    public void testAddInternalPeer() throws WriteFailedException {
        final Neighbor neighbor = new NeighborBuilder()
            .setNeighborAddress(IP)
            .setAfiSafis(new AfiSafisBuilder()
                .setAfiSafi(Collections.singletonList(
                    new AfiSafiBuilder().setAfiSafiName(IPV4UNICAST.class).build()
                )).build())
            .setConfig(
                new ConfigBuilder()
                    .setPeerType(PeerType.INTERNAL)
                    .build())
            .build();
        customizer.writeCurrentAttributes(ID, neighbor, ctx);
        assertTrue(customizer.isPeerConfigured(ID));
    }

    @Test
    public void testUpdate() throws WriteFailedException {
        final PeerBean peer = mock(PeerBean.class);
        customizer.addPeer(ID, peer);
        final Neighbor before = mock(Neighbor.class);
        final Neighbor after = mock(Neighbor.class);
        customizer.updateCurrentAttributes(ID, before, after, ctx);
        verify(peer).closeServiceInstance();
        verify(peer).close();
        verify(peer).start(globalRib, after, tableTypeRegistry, null);
    }

    @Test
    public void testDelete() throws WriteFailedException {
        final PeerBean peer = mock(PeerBean.class);
        customizer.addPeer(ID, peer);
        final Neighbor before = mock(Neighbor.class);
        customizer.deleteCurrentAttributes(ID, before, ctx);
        verify(peer).closeServiceInstance();
        verify(peer).close();
        assertFalse(customizer.isPeerConfigured(ID));
    }
}