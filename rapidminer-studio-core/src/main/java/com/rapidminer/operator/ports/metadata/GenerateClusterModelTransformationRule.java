/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.ports.metadata;

import java.util.function.BooleanSupplier;

import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;


/**
 * Generates transformation rule for {@link ClusterModelMetaData}.
 * The transformation expects an {@link InputPort} providing {@link ExampleSetMetaData} and adds the clustering
 * information to this metadata either as a cluster or as a label attribute, depending on the value supplied by
 * <code>addsLabelAttribute</code>. The result is delivered to the {@link OutputPort} <code>outputPort</code>.
 *
 * @author Balazs Prehoda
 *
 * @see ClusterModelMetaData
 * @see MDTransformationRule
 *
 * @since 9.8.1
 */
public class GenerateClusterModelTransformationRule implements MDTransformationRule {

    /** The OutputPort to which ClusterModelMetaData should be delivered. */
    private final OutputPort outputPort;

    /** The InputPort of the input ExampleSet. */
    private final InputPort exampleSetInput;

    /** The specific class of the cluster model. */
    private final Class<? extends ClusterModel> modelClass;

    /** Whether to add the clustering information as label or cluster attribute */
    private final BooleanSupplier addsLabelAttribute;

    /**
     * Creates a transformation rule for cluster model metadata.
     *
     * @param exampleSetInput The {@link InputPort} of the input {@link com.rapidminer.example.ExampleSet}.
     * @param outputPort The {@link OutputPort} to which the cluster model metadata should be delivered.
     * @param modelClass The specific class of the cluster model.
     * @param addsLabelAttribute Whether to add the clustering information as label or cluster attribute.
     */
    public GenerateClusterModelTransformationRule(InputPort exampleSetInput,
                                                  OutputPort outputPort,
                                                  Class<? extends ClusterModel> modelClass,
                                                  BooleanSupplier addsLabelAttribute) {
        this.outputPort = outputPort;
        this.exampleSetInput = exampleSetInput;
        this.modelClass = modelClass;
        this.addsLabelAttribute = addsLabelAttribute;
    }

    /**
     * Creates a {@link ClusterModelMetaData} from the {@link ExampleSetMetaData} coming from the <code>exampleSetInput</code> port,
     * and delivers it to the <code>outputPort</code>. If the input {@link ExampleSetMetaData} is <code>null</code>, delivers a
     * {@link ClusterModelMetaData} object with <code>trainingSetMetaData = null</code>.
     */
    @Override
    public void transformMD() {
        ExampleSetMetaData input = exampleSetInput.getMetaDataAsOrNull(ExampleSetMetaData.class);
        ClusterModelMetaData clusterModelMetaData;
        if (input != null) {
            clusterModelMetaData = new ClusterModelMetaData(
                    modelClass,
                    addsLabelAttribute.getAsBoolean(),
                    input
            );
            clusterModelMetaData.addToHistory(outputPort);
            outputPort.deliverMD(clusterModelMetaData);
            return;
        }
        outputPort.deliverMD(new ClusterModelMetaData(
                modelClass,
                addsLabelAttribute.getAsBoolean(),
                null)
        );
    }
}
