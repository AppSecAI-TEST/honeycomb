/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.lisp.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.MapReplyAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.Eid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.locator.list.NegativeMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.Locator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.LocatorBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.LispAddDelRemoteMapping;
import org.openvpp.jvpp.core.dto.LispAddDelRemoteMappingReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;


public class RemoteMappingCustomizerTest {

    @Mock
    private FutureJVppCore fakeJvpp;

    @Captor
    private ArgumentCaptor<LispAddDelRemoteMapping> mappingCaptor;

    private MappingId mappingId;
    private RemoteMappingCustomizer customizer;
    private RemoteMapping intf;
    private LispAddDelRemoteMappingReply fakeReply;
    private CompletableFuture<LispAddDelRemoteMappingReply> completeFuture;
    private InstanceIdentifier<RemoteMapping> id;
    private EidMappingContext remoteMappingContext;
    private WriteContext writeContext;
    private MappingContext mapping;


    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        final Eid eid = new EidBuilder().setAddress(
                new Ipv4Builder().setIpv4(
                        new Ipv4Address("192.168.2.1"))
                        .build())
                .build();

        mappingId = new MappingId("REMOTE");
        final RemoteMappingKey key = new RemoteMappingKey(mappingId);
        remoteMappingContext = new EidMappingContext("remote");
        mapping = mock(MappingContext.class);
        writeContext = mock(WriteContext.class);


        intf = new RemoteMappingBuilder()
                .setEid(
                        eid)
                .setLocatorList(new NegativeMappingBuilder().setMapReplyAction(MapReplyAction.Drop).build())
                .build();

        id = InstanceIdentifier.builder(Lisp.class).child(EidTable.class)
                .child(VniTable.class, new VniTableKey(25L))
                .child(RemoteMappings.class)
                .child(RemoteMapping.class, key).build();

        fakeReply = new LispAddDelRemoteMappingReply();
        completeFuture = new CompletableFuture<>();
        completeFuture.complete(fakeReply);
        customizer = new RemoteMappingCustomizer(fakeJvpp, remoteMappingContext);

        when(fakeJvpp.lispAddDelRemoteMapping(Mockito.any())).thenReturn(completeFuture);
        when(writeContext.getMappingContext()).thenReturn(mapping);
        when(mapping.read(Mockito.any())).thenReturn(com.google.common.base.Optional
                .of(new RemoteMappingBuilder().setKey(key).setId(mappingId).setEid(eid).build()));
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        customizer.writeCurrentAttributes(null, null, writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesBadData() throws WriteFailedException {
        customizer
                .writeCurrentAttributes(null, mock(RemoteMapping.class), writeContext);
    }

    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {
        //to simulate no mapping
        when(mapping.read(Mockito.any())).thenReturn(com.google.common.base.Optional.absent());

        customizer.writeCurrentAttributes(id, intf, writeContext);

        verify(fakeJvpp, times(1)).lispAddDelRemoteMapping(mappingCaptor.capture());

        LispAddDelRemoteMapping request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals(1, request.isAdd);
        assertEquals("1.2.168.192", TranslateUtils.arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(25, request.vni);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        customizer.updateCurrentAttributes(null, null, null, writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullData() throws WriteFailedException {
        customizer.deleteCurrentAttributes(null, null, writeContext);
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {
        customizer.deleteCurrentAttributes(id, intf, writeContext);

        verify(fakeJvpp, times(1)).lispAddDelRemoteMapping(mappingCaptor.capture());

        LispAddDelRemoteMapping request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals(0, request.isAdd);
        assertEquals("1.2.168.192", TranslateUtils.arrayToIpv4AddressNoZone(request.eid).getValue());
        assertEquals(25, request.vni);
    }

}
