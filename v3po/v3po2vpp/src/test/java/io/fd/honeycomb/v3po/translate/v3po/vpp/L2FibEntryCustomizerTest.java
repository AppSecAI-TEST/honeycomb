/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.v3po.translate.v3po.vpp;

import static io.fd.honeycomb.v3po.translate.v3po.test.ContextTestUtils.mockMapping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.v3po.translate.MappingContext;
import io.fd.honeycomb.v3po.translate.v3po.test.TestHelperUtils;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.L2FibFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.L2FibTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.VppInvocationException;
import org.openvpp.jvpp.dto.L2FibAddDel;
import org.openvpp.jvpp.dto.L2FibAddDelReply;
import org.openvpp.jvpp.dto.L2InterfaceVlanTagRewriteReply;
import org.openvpp.jvpp.future.FutureJVpp;

public class L2FibEntryCustomizerTest {
    private static final String BD_CTX_NAME = "bd-test-instance";
    private static final String IFC_CTX_NAME = "ifc-test-instance";

    private static final String BD_NAME = "testBD0";
    private static final int BD_ID = 111;
    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_ID = 123;

    @Mock
    private FutureJVpp api;
    @Mock
    private WriteContext writeContext;
    @Mock
    private MappingContext mappingContext;

    private NamingContext bdContext;
    private NamingContext interfaceContext;

    private L2FibEntryCustomizer customizer;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        doReturn(mappingContext).when(writeContext).getMappingContext();
        bdContext = new NamingContext("generatedBdName", BD_CTX_NAME);
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);

        customizer = new L2FibEntryCustomizer(api, bdContext, interfaceContext);
    }

    private static InstanceIdentifier<L2FibEntry> getL2FibEntryId(final PhysAddress address) {
        return InstanceIdentifier.create(BridgeDomains.class).child(BridgeDomain.class, new BridgeDomainKey(BD_NAME))
            .child(L2FibTable.class).child(L2FibEntry.class, new L2FibEntryKey(address));
    }

    private void whenL2FibAddDelThenSuccess() {
        final CompletableFuture<L2FibAddDelReply> replyFuture = new CompletableFuture<>();
        final L2FibAddDelReply reply = new L2FibAddDelReply();
        replyFuture.complete(reply);
        doReturn(replyFuture).when(api).l2FibAddDel(any(L2FibAddDel.class));
    }

    private void whenL2FibAddDelThenFailure() {
        doReturn(TestHelperUtils.<L2InterfaceVlanTagRewriteReply>createFutureException()).when(api)
            .l2FibAddDel(any(L2FibAddDel.class));
    }

    private L2FibAddDel generateL2FibAddDelRequest(final long mac, final byte isAdd) {
        final L2FibAddDel request = new L2FibAddDel();
        request.mac = mac;
        request.bdId = BD_ID;
        request.swIfIndex = IFACE_ID;
        request.isAdd = isAdd;
        if (isAdd == 1) {
            request.staticMac = 1;
            request.filterMac = 1;
        }
        return request;
    }

    private L2FibEntry generateL2FibEntry(final PhysAddress address) {
        final L2FibEntryBuilder entry = new L2FibEntryBuilder();
        entry.setKey(new L2FibEntryKey(address));
        entry.setPhysAddress(address);
        entry.setStaticConfig(true);
        entry.setBridgedVirtualInterface(false);
        entry.setAction(L2FibFilter.class);
        entry.setOutgoingInterface(IFACE_NAME);
        return entry.build();
    }

    private void verifyL2FibAddDelWasInvoked(final L2FibAddDel expected) throws
        VppInvocationException {
        ArgumentCaptor<L2FibAddDel> argumentCaptor = ArgumentCaptor.forClass(L2FibAddDel.class);
        verify(api).l2FibAddDel(argumentCaptor.capture());
        final L2FibAddDel actual = argumentCaptor.getValue();
        assertEquals(expected.mac, actual.mac);
        assertEquals(expected.bdId, actual.bdId);
        assertEquals(expected.swIfIndex, actual.swIfIndex);
        assertEquals(expected.isAdd, actual.isAdd);
        assertEquals(expected.staticMac, actual.staticMac);
        assertEquals(expected.filterMac, actual.filterMac);
    }

    @Test
    public void testCreate() throws Exception {
        final long address_vpp = 0x0102030405060000L;
        final PhysAddress address = new PhysAddress("01:02:03:04:05:06");
        final L2FibEntry entry = generateL2FibEntry(address);
        final InstanceIdentifier<L2FibEntry> id = getL2FibEntryId(address);

        mockMapping(mappingContext, BD_NAME, BD_ID, BD_CTX_NAME);
        mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);

        whenL2FibAddDelThenSuccess();

        customizer.writeCurrentAttributes(id, entry, writeContext);

        verifyL2FibAddDelWasInvoked(generateL2FibAddDelRequest(address_vpp, (byte) 1));
    }

    @Test
    public void testCreateFailed() throws Exception {
        final long address_vpp = 0x1122334455660000L;
        final PhysAddress address = new PhysAddress("11:22:33:44:55:66");
        final L2FibEntry entry = generateL2FibEntry(address);
        final InstanceIdentifier<L2FibEntry> id = getL2FibEntryId(address);

        mockMapping(mappingContext, BD_NAME, BD_ID, BD_CTX_NAME);
        mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);

        whenL2FibAddDelThenFailure();

        try {
            customizer.writeCurrentAttributes(id, entry, writeContext);
        } catch (WriteFailedException.CreateFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyL2FibAddDelWasInvoked(generateL2FibAddDelRequest(address_vpp, (byte) 1));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws Exception {
        customizer.updateCurrentAttributes(InstanceIdentifier.create(L2FibEntry.class), mock(L2FibEntry.class),
            mock(L2FibEntry.class), writeContext);
    }

    @Test
    public void testDelete() throws Exception {
        final long address_vpp = 0x1122334455660000L;
        final PhysAddress address = new PhysAddress("11:22:33:44:55:66");
        final L2FibEntry entry = generateL2FibEntry(address);
        final InstanceIdentifier<L2FibEntry> id = getL2FibEntryId(address);

        mockMapping(mappingContext, BD_NAME, BD_ID, BD_CTX_NAME);
        mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);

        whenL2FibAddDelThenSuccess();

        customizer.deleteCurrentAttributes(id, entry, writeContext);

        verifyL2FibAddDelWasInvoked(generateL2FibAddDelRequest(address_vpp, (byte) 0));
    }

    @Test
    public void testDeleteFailed() throws Exception {
        final long address_vpp = 0x0102030405060000L;
        final PhysAddress address = new PhysAddress("01:02:03:04:05:06");
        final L2FibEntry entry = generateL2FibEntry(address);
        final InstanceIdentifier<L2FibEntry> id = getL2FibEntryId(address);

        mockMapping(mappingContext, BD_NAME, BD_ID, BD_CTX_NAME);
        mockMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);

        whenL2FibAddDelThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, entry, writeContext);
        } catch (WriteFailedException.DeleteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyL2FibAddDelWasInvoked(generateL2FibAddDelRequest(address_vpp, (byte) 0));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}