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

package io.fd.honeycomb.translate.impl.write;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.write.AbstractGenericWriter;
import io.fd.honeycomb.translate.write.ListWriter;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Generic list node writer with customizable behavior thanks to injected customizer.
 */
public final class GenericListWriter<D extends DataObject & Identifiable<K>, K extends Identifier<D>> extends
        AbstractGenericWriter<D> implements ListWriter<D, K> {

    private final WriterCustomizer<D> customizer;

    public GenericListWriter(@Nonnull final InstanceIdentifier<D> type,
                             @Nonnull final ListWriterCustomizer<D, K> customizer) {
        super(type);
        this.customizer = customizer;
    }

    @Override
    protected void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D data,
                                          @Nonnull final WriteContext ctx) throws WriteFailedException {
        try {
            customizer.writeCurrentAttributes(id, data, ctx);
        } catch (RuntimeException e) {
            throw new WriteFailedException.CreateFailedException(id, data, e);
        }
    }

    @Override
    protected void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull final WriteContext ctx) throws WriteFailedException {
        try {
            customizer.deleteCurrentAttributes(id, dataBefore, ctx);
        } catch (RuntimeException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    @Override
    protected void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull final D dataAfter, @Nonnull final WriteContext ctx)
        throws WriteFailedException {
        try {
            customizer.updateCurrentAttributes(id, dataBefore, dataAfter, ctx);
        } catch (RuntimeException e) {
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    protected void writeCurrent(final InstanceIdentifier<D> id, final D data, final WriteContext ctx)
        throws WriteFailedException {
        // Make sure the key is present
        if (isWildcarded(id)) {
            super.writeCurrent(getSpecificId(id, data), data, ctx);
        } else {
            super.writeCurrent(id, data, ctx);
        }
    }

    @Override
    protected void updateCurrent(final InstanceIdentifier<D> id, final D dataBefore, final D dataAfter,
                                 final WriteContext ctx) throws WriteFailedException {
        // Make sure the key is present
        if (isWildcarded(id)) {
            super.updateCurrent(getSpecificId(id, dataBefore), dataBefore, dataAfter, ctx);
        } else {
            super.updateCurrent(id, dataBefore, dataAfter, ctx);
        }
    }

    @Override
    protected void deleteCurrent(final InstanceIdentifier<D> id, final D dataBefore, final WriteContext ctx)
        throws WriteFailedException {
        // Make sure the key is present
        if (isWildcarded(id)) {
            super.deleteCurrent(getSpecificId(id, dataBefore), dataBefore, ctx);
        } else {
            super.deleteCurrent(id, dataBefore, ctx);
        }
    }

    private boolean isWildcarded(final InstanceIdentifier<D> id) {
        return id.firstIdentifierOf(getManagedDataObjectType().getTargetType()).isWildcarded();
    }

    private InstanceIdentifier<D> getSpecificId(final InstanceIdentifier<D> currentId, final D current) {
        return RWUtils.replaceLastInId(currentId,
            new InstanceIdentifier.IdentifiableItem<>(currentId.getTargetType(), current.getKey()));
    }
}
