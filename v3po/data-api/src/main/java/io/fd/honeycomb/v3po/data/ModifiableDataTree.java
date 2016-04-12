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

package io.fd.honeycomb.v3po.data;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.v3po.translate.TranslationException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;

/**
 * Facade over data tree that allows tree modification.
 */
@Beta
public interface ModifiableDataTree {
    /**
     * Alters data tree using supplied modification.
     *
     * @param modification data tree modification
     * @throws DataValidationFailedException if modification data is not valid
     * @throws TranslationException if failed while updating data tree state
     */
    void modify(final DataTreeModification modification) throws DataValidationFailedException, TranslationException;

    /**
     * Creates read-only snapshot of a ModifiableDataTree.
     *
     * @return Data tree snapshot.
     */
    DataTreeSnapshot takeSnapshot();
}