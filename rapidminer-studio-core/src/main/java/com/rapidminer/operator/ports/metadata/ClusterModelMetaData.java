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

import com.rapidminer.example.Attributes;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.tools.Ontology;


/**
 * Metadata class for cluster models. It should be used to display clustering information in the output metadata
 * of a clusterer operator and to transform the input metadata of operators such as the
 * {@link com.rapidminer.operator.ModelApplier} operator.
 *
 * @author Balazs Prehoda
 *
 * @since 9.8.1
 */
public class ClusterModelMetaData extends ModelMetaData {

    private static final long serialVersionUID = 1L;

    /** The clustering information */
    private AttributeMetaData clusterMetaData;

    /** Whether to add the clustering information as label or cluster attribute */
    private boolean addAsLabel;

    /**
     * Clone constructor
     */
    protected ClusterModelMetaData() {
    }

    /**
     * Create ClusterModelMetaData instance.
     *
     * @param modelClass          The class of the cluster model instance.
     * @param addAsLabel          Tells whether the clustering information should be added as label or cluster
     *                            attribute.
     * @param trainingSetMetaData {@link ExampleSetMetaData} of the ExampleSet on which the cluster model was trained.
     */
    public ClusterModelMetaData(Class<? extends ClusterModel> modelClass, boolean addAsLabel, ExampleSetMetaData trainingSetMetaData) {
        super(modelClass, trainingSetMetaData);
        this.addAsLabel = addAsLabel;
        if (isAddAsLabel()) {
            clusterMetaData = new AttributeMetaData(Attributes.LABEL_NAME, Ontology.NOMINAL, Attributes.LABEL_NAME);
        } else {
            clusterMetaData = new AttributeMetaData(Attributes.CLUSTER_NAME, Ontology.NOMINAL, Attributes.CLUSTER_NAME);
        }
    }

    /**
     * Add clustering information to the input {@link ExampleSetMetaData}.
     *
     * @param emd       Input {@link ExampleSetMetaData} to which clustering information should be added.
     * @param inputPort This parameter is ignored. It is only present because of the super method's signature
     *                  ({@link com.rapidminer.operator.ports.metadata.ModelMetaData}).
     * @return The input {@link ExampleSetMetaData} containing clustering information.
     */
    @Override
    public ExampleSetMetaData applyEffects(ExampleSetMetaData emd, InputPort inputPort) {
        if (clusterMetaData == null) {
            return emd;
        }
        if (isAddAsLabel() && emd.getLabelMetaData() != null) {
            // Remove label metadata if present
            emd.removeAttribute(emd.getLabelMetaData());
        }

        emd.addAttribute(clusterMetaData);
        emd.mergeSetRelation(getClusterAttributeSetRelation());
        return emd;
    }

    public AttributeMetaData getClusterMetaData() {
        return clusterMetaData;
    }

    public boolean isAddAsLabel() {
        return addAsLabel;
    }

    /**
     * Obtains the attribute set relation. Gets it from the cluster model metadata if known,
     * returns {@link SetRelation#UNKNOWN} otherwise.
     *
     * @return The value set relation of the cluster attribute.
     */
    public SetRelation getClusterAttributeSetRelation() {
        if (clusterMetaData != null) {
            return clusterMetaData.getValueSetRelation();
        } else {
            return SetRelation.UNKNOWN;
        }
    }

    @Override
    public String getDescription() {
        return super.getDescription() + "; generates: " + clusterMetaData;
    }

    @Override
    public ClusterModelMetaData clone() {
        ClusterModelMetaData clone = (ClusterModelMetaData) super.clone();
        if (this.clusterMetaData != null) {
            clone.clusterMetaData = this.clusterMetaData.clone();
            clone.addAsLabel = this.isAddAsLabel();
        }
        return clone;
    }
}
