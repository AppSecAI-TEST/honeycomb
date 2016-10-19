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

package io.fd.honeycomb.translate.v3po.interfaces.acl.ingress;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.v3po.interfaces.acl.common.AclTableContextManager;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSession;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelSessionReply;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTableReply;
import io.fd.vpp.jvpp.core.dto.ClassifyTableByInterface;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterface;
import io.fd.vpp.jvpp.core.dto.InputAclSetInterfaceReply;
import java.util.Arrays;
import java.util.Collections;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.EthAcl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.AccessListEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Deny;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.Permit;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.actions.packet.handling.PermitBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.IetfAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.ietf.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.ietf.acl.IngressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.context.rev161214.mapping.entry.context.attributes.acl.mapping.entry.context.mapping.table.MappingEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.AccessListsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.access.lists.AclBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IetfAclCustomizerTest extends WriterCustomizerTest {

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final InstanceIdentifier<Ingress> IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME)).augmentation(
            VppInterfaceAugmentation.class).child(IetfAcl.class).child(Ingress.class);
    private static final String ACL_NAME = "acl1";
    private static final Class<? extends AclBase> ACL_TYPE = EthAcl.class;

    @Mock
    private AclTableContextManager aclCtx;

    private IetfAclCustomizer customizer;
    private Ingress acl;
    private int DENY = 0;
    private int PERMIT = -1;

    @Override
    protected void setUp() {
        customizer = new IetfAclCustomizer(new IngressIetfAclWriter(api, aclCtx), new NamingContext("prefix", IFC_TEST_INSTANCE));
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_TEST_INSTANCE);
        acl = new IngressBuilder().setAccessLists(
            new AccessListsBuilder().setAcl(
                Collections.singletonList(new AclBuilder()
                    .setName(ACL_NAME)
                    .setType(ACL_TYPE)
                    .build())
            ).build()
        ).build();
    }

    @Test
    public void testWrite() throws WriteFailedException {
        when(api.classifyAddDelTable(any())).thenReturn(future(new ClassifyAddDelTableReply()));
        when(api.classifyAddDelSession(any())).thenReturn(future(new ClassifyAddDelSessionReply()));

        when(writeContext.readAfter(any())).thenReturn(Optional.of(
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclBuilder()
                .setAccessListEntries(
                    new AccessListEntriesBuilder().setAce(Arrays.asList(ace(permit()), ace(permit()), ace(deny())
                    )).build()
                ).build()

        ));
        when(api.inputAclSetInterface(any())).thenReturn(future(new InputAclSetInterfaceReply()));

        customizer.writeCurrentAttributes(IID, acl, writeContext);

        final InOrder inOrder = Mockito.inOrder(api);
        inOrder.verify(api).classifyAddDelTable(argThat(actionOnMissEquals(DENY))); // default action
        inOrder.verify(api).classifyAddDelTable(any());
        inOrder.verify(api).classifyAddDelSession(argThat(actionOnHitEquals(DENY))); // last deny ACE
        inOrder.verify(api).classifyAddDelTable(any());
        inOrder.verify(api).classifyAddDelSession(argThat(actionOnHitEquals(PERMIT)));
        inOrder.verify(api).classifyAddDelTable(any());
        inOrder.verify(api).classifyAddDelSession(argThat(actionOnHitEquals(PERMIT)));
        inOrder.verify(api).inputAclSetInterface(inputAclSetInterfaceWriteRequest()); // assignment
    }

    private Matcher<ClassifyAddDelTable> actionOnMissEquals(final int action) {
        return new BaseMatcher<ClassifyAddDelTable>() {
            public Object item;

            @Override
            public void describeTo(final Description description) {
                description.appendText("Expected ClassifyAddDelTable[missNextIndex=" + action + "] but was " + item);
            }

            @Override
            public boolean matches(final Object item) {
                this.item = item;
                if (item instanceof ClassifyAddDelTable) {
                    return ((ClassifyAddDelTable) item).missNextIndex == action;
                }
                return false;
            }
        };
    }

    private Matcher<ClassifyAddDelSession> actionOnHitEquals(final int action) {
        return new BaseMatcher<ClassifyAddDelSession>() {
            public Object item;

            @Override
            public void describeTo(final Description description) {
                description.appendText("Expected ClassifyAddDelSession[hitNextIndex=" + action + "] but was " + item);
            }

            @Override
            public boolean matches(final Object item) {
                this.item = item;
                if (item instanceof ClassifyAddDelSession) {
                    return ((ClassifyAddDelSession) item).hitNextIndex == action;
                }
                return false;
            }
        };
    }

    private Deny deny() {
        return new DenyBuilder().build();
    }

    private Permit permit() {
        return new PermitBuilder().build();
    }

    private static Ace ace(final PacketHandling action) {
        return new AceBuilder()
            .setMatches(new MatchesBuilder().setAceType(
                new AceIpBuilder()
                    .setAceIpVersion(new AceIpv6Builder().build())
                    .setProtocol((short) 1)
                    .build()
            ).build())
            .setActions(new ActionsBuilder().setPacketHandling(action).build())
            .build();
    }

    @Test
    public void testDelete() throws WriteFailedException {
        when(api.inputAclSetInterface(any())).thenReturn(future(new InputAclSetInterfaceReply()));
        when(api.classifyAddDelTable(any())).thenReturn(future(new ClassifyAddDelTableReply()));
        when(aclCtx.getEntry(IF_INDEX, mappingContext)).thenReturn(Optional.of(
            new MappingEntryBuilder()
                .setIndex(IF_INDEX)
                .setL2TableId(1)
                .setIp4TableId(2)
                .setIp6TableId(3)
                .build()));

        customizer.deleteCurrentAttributes(IID, acl, writeContext);

        final ClassifyTableByInterface expectedRequest = new ClassifyTableByInterface();
        expectedRequest.swIfIndex = IF_INDEX;
        verify(api).inputAclSetInterface(inputAclSetInterfaceDeleteRequest());
        verify(api).classifyAddDelTable(classifyAddDelTable(1));
        verify(api).classifyAddDelTable(classifyAddDelTable(2));
        verify(api).classifyAddDelTable(classifyAddDelTable(3));
    }

    private static InputAclSetInterface inputAclSetInterfaceDeleteRequest() {
        final InputAclSetInterface request = new InputAclSetInterface();
        request.swIfIndex = IF_INDEX;
        request.l2TableIndex = 1;
        request.ip4TableIndex = 2;
        request.ip6TableIndex = 3;
        return request;
    }

    private static ClassifyAddDelTable classifyAddDelTable(final int tableIndex) {
        final ClassifyAddDelTable reply = new ClassifyAddDelTable();
        reply.tableIndex = tableIndex;
        return reply;
    }

    private static InputAclSetInterface inputAclSetInterfaceWriteRequest() {
        final InputAclSetInterface request = new InputAclSetInterface();
        request.swIfIndex = IF_INDEX;
        request.isAdd = 1;
        request.l2TableIndex = -1;
        request.ip4TableIndex = -1;
        request.ip6TableIndex = 0;
        return request;
    }
}