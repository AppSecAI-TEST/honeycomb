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

package io.fd.honeycomb.v3po.translate.v3po.interfaces;

import static io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils.booleanToByte;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.SubInterfaceUtils;
import io.fd.honeycomb.v3po.translate.v3po.util.TagRewriteOperation;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527._802dot1q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.l2.Rewrite;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.l2.RewriteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.tag.rewrite.PushTags;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.L2InterfaceVlanTagRewrite;
import org.openvpp.jvpp.dto.L2InterfaceVlanTagRewriteReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer Customizer responsible for vlan tag rewrite.<br> Sends {@code l2_interface_vlan_tag_rewrite} message to
 * VPP.<br> Equivalent of invoking {@code vppctl set interface l2 tag-rewrite} command.
 */
public class RewriteCustomizer extends FutureJVppCustomizer implements ChildWriterCustomizer<Rewrite> {

    private static final Logger LOG = LoggerFactory.getLogger(
            io.fd.honeycomb.v3po.translate.v3po.interfacesstate.RewriteCustomizer.class);
    private final NamingContext interfaceContext;

    public RewriteCustomizer(@Nonnull final FutureJVpp futureJvpp,
                             @Nonnull final NamingContext interfaceContext) {
        super(futureJvpp);
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Nonnull
    @Override
    public Optional<Rewrite> extract(@Nonnull final InstanceIdentifier<Rewrite> currentId,
                                     @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((L2) parentData).getRewrite());
    }

    @Override
    public void writeCurrentAttributes(final InstanceIdentifier<Rewrite> id, final Rewrite dataAfter,
                                       final WriteContext writeContext)
            throws WriteFailedException.CreateFailedException {

        try {
            setTagRewrite(getSubInterfaceName(id), dataAfter, writeContext);
        } catch (VppApiInvocationException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    private static String getSubInterfaceName(final InstanceIdentifier<Rewrite> id) {
        return SubInterfaceUtils.getSubInterfaceName(id.firstKeyOf(Interface.class).getName(),
                Math.toIntExact(id.firstKeyOf(SubInterface.class).getIdentifier()));
    }

    private void setTagRewrite(final String ifname, final Rewrite rewrite, final WriteContext writeContext)
            throws VppApiInvocationException {
        final int swIfIndex = interfaceContext.getIndex(ifname, writeContext.getMappingContext());
        LOG.debug("Setting tag rewrite for interface {}(id=): {}", ifname, swIfIndex, rewrite);

        final CompletionStage<L2InterfaceVlanTagRewriteReply> replyCompletionStage =
                getFutureJVpp().l2InterfaceVlanTagRewrite(getTagRewriteRequest(swIfIndex, rewrite));

        final L2InterfaceVlanTagRewriteReply reply =
                TranslateUtils.getReply(replyCompletionStage.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.debug("Failed to set tag rewrite for interface {}(id=): {}", ifname, swIfIndex, rewrite);
            throw new VppApiInvocationException("l2InterfaceVlanTagRewrite", reply.context, reply.retval);
        } else {
            LOG.debug("Tag rewrite for interface {}(id=) set successfully: {}", ifname, swIfIndex, rewrite);
        }
    }

    private L2InterfaceVlanTagRewrite getTagRewriteRequest(final int swIfIndex, final Rewrite rewrite) {
        final L2InterfaceVlanTagRewrite request = new L2InterfaceVlanTagRewrite();
        request.swIfIndex = swIfIndex;
        request.pushDot1Q = booleanToByte(_802dot1q.class == rewrite.getVlanType());

        final List<PushTags> pushTags = rewrite.getPushTags();
        final Short popTags = rewrite.getPopTags();

        final int numberOfTagsToPop = popTags == null
                ? 0
                : popTags.intValue();
        final int numberOfTagsToPush = pushTags == null
                ? 0
                : pushTags.size();

        request.vtrOp = TagRewriteOperation.get(numberOfTagsToPop, numberOfTagsToPush).ordinal();

        if (numberOfTagsToPush > 0) {
            for (final PushTags tag : pushTags) {
                if (tag.getIndex() == 0) {
                    request.tag1 = tag.getDot1qTag().getVlanId().getValue();
                } else {
                    request.tag2 = tag.getDot1qTag().getVlanId().getValue();
                }
            }
        }

        LOG.debug("Generated tag rewrite request: {}", ReflectionToStringBuilder.toString(request));
        return request;
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Rewrite> id,
                                        @Nonnull final Rewrite dataBefore,
                                        @Nonnull final Rewrite dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        try {
            setTagRewrite(getSubInterfaceName(id), dataAfter, writeContext);
        } catch (VppApiInvocationException e) {
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Rewrite> id,
                                        @Nonnull final Rewrite dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException.DeleteFailedException {
        try {
            final String subifName = getSubInterfaceName(id);
            LOG.debug("Disabling tag rewrite for interface {}", subifName);
            final Rewrite rewrite = new RewriteBuilder().build(); // rewrite without push and pops will cause delete
            setTagRewrite(subifName, rewrite, writeContext);
        } catch (VppApiInvocationException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }
}