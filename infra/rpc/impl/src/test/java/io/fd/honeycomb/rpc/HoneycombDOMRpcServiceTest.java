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

package io.fd.honeycomb.rpc;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class HoneycombDOMRpcServiceTest {

    @Mock
    private BindingNormalizedNodeSerializer serializer;
    @Mock
    private RpcRegistry registry;
    @Mock
    private SchemaPath path;
    @Mock
    private DataObject input;
    @Mock
    private ContainerNode output;

    private ContainerNode node;
    private HoneycombDOMRpcService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        node = mockContainerNode(QName.create("a"));

        service = new HoneycombDOMRpcService(serializer, registry);

        Mockito.when(serializer.fromNormalizedNodeRpcData(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(input);
        Mockito.when(serializer.toNormalizedNodeRpcData(ArgumentMatchers.any())).thenReturn(output);
    }

    @Test
    public void testInvokeRpc() throws Exception {
        Mockito.when(registry.invoke(path, input)).thenReturn(CompletableFuture.completedFuture(input));

        assertEquals(output, service.invokeRpc(path, node).get().getResult());
    }

    @Test(expected = RpcException.class)
    public void testInvokeRpcFailed() throws Exception {
        final CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new RuntimeException());
        Mockito.when(registry.invoke(path, input)).thenReturn(future);

        service.invokeRpc(path, node).checkedGet();
    }

    private ContainerNode mockContainerNode(final QName nn1) {
        final ContainerNode nn1B = Mockito.mock(ContainerNode.class);
        Mockito.when(nn1B.getNodeType()).thenReturn(nn1);
        return nn1B;
    }
}