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

package io.fd.honeycomb.infra.bgp;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4LABELLEDUNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6LABELLEDUNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.IPV6UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L2VPNEVPN;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV4UNICAST;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.L3VPNIPV6UNICAST;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.EvpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.evpn.rev160321.L2vpnAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.labeled.unicast.rev150525.LabeledUnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev150210.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.LINKSTATE;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.MplsLabeledVpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

/**
 * Registers BGP extensions provided by ODL implementation.
 * TODO(HONEYCOMB-363): create module per BGP extension
 * TODO(HONEYCOMB-378): add support for flowspec (requires some special initialization)
 */
final class BgpExtensionsModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(BgpExtensionsModule.class);

    protected void configure() {
        LOG.debug("Initializing BgpExtensionsModule");
        // This should be part of BgpModule, but that one is Private and Multibinders + private BASE_MODULES
        // do not work together, that's why there's a dedicated module here
        // https://github.com/google/guice/issues/906
        configureRIBExtensions();
        configureBGPExtensions();
        configureTableTypes();
    }

    private void configureRIBExtensions() {
        final Multibinder<RIBExtensionProviderActivator> ribExtensionBinder = Multibinder.newSetBinder(binder(),
            RIBExtensionProviderActivator.class);
        ribExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.evpn.impl.RIBActivator.class);
        ribExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.inet.RIBActivator.class);
        ribExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.labeled.unicast.RIBActivator.class);
        ribExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.linkstate.impl.RIBActivator.class);
        ribExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.l3vpn.ipv4.RibIpv4Activator.class);
        ribExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.l3vpn.ipv6.RibIpv6Activator.class);
        bind(RIBExtensionConsumerContext.class).toProvider(RIBExtensionConsumerContextProvider.class)
            .in(Singleton.class);
    }

    private void configureBGPExtensions() {
        final Multibinder<BGPExtensionProviderActivator> bgpExtensionBinder = Multibinder.newSetBinder(binder(),
            BGPExtensionProviderActivator.class);
        bgpExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.parser.impl.BGPActivator.class);
        bgpExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.evpn.impl.BGPActivator.class);
        bgpExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.inet.BGPActivator.class);
        bgpExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.labeled.unicast.BGPActivator.class);
        bgpExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.linkstate.impl.BGPActivator.class);
        bgpExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.l3vpn.ipv4.BgpIpv4Activator.class);
        bgpExtensionBinder.addBinding().to(org.opendaylight.protocol.bgp.l3vpn.ipv6.BgpIpv6Activator.class);
        bind(BGPExtensionConsumerContext.class).toProvider(BGPExtensionConsumerContextProvider.class)
            .in(Singleton.class);
    }

    private void configureTableTypes() {
        final Multibinder<BGPTableTypeRegistryConsumerProvider.BGPTableType> tableTypeBinder =
            Multibinder.newSetBinder(binder(),
                BGPTableTypeRegistryConsumerProvider.BGPTableType.class);

        tableTypeBinder.addBinding().toInstance(provider -> provider
            .registerBGPTableType(L2vpnAddressFamily.class, EvpnSubsequentAddressFamily.class, L2VPNEVPN.class));

        tableTypeBinder.addBinding().toInstance(provider -> provider
            .registerBGPTableType(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class, IPV4UNICAST.class));
        tableTypeBinder.addBinding().toInstance(provider -> provider
            .registerBGPTableType(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class, IPV6UNICAST.class));

        tableTypeBinder.addBinding().toInstance(provider -> provider
            .registerBGPTableType(Ipv4AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class,
                L3VPNIPV4UNICAST.class));
        tableTypeBinder.addBinding().toInstance(provider -> provider
            .registerBGPTableType(Ipv6AddressFamily.class, MplsLabeledVpnSubsequentAddressFamily.class,
                L3VPNIPV6UNICAST.class));

        tableTypeBinder.addBinding().toInstance(provider -> provider
            .registerBGPTableType(Ipv4AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class,
                IPV4LABELLEDUNICAST.class));
        tableTypeBinder.addBinding().toInstance(provider -> provider
            .registerBGPTableType(Ipv6AddressFamily.class, LabeledUnicastSubsequentAddressFamily.class,
                IPV6LABELLEDUNICAST.class));

        tableTypeBinder.addBinding().toInstance(provider -> provider
            .registerBGPTableType(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class,
                LINKSTATE.class));

        bind(BGPTableTypeRegistryConsumer.class).toProvider(BGPTableTypeRegistryConsumerProvider.class)
            .in(Singleton.class);
    }
}
