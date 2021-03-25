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
package com.rapidminer.math.aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.rapidminer.tools.container.Pair;


/**
 * The tree formed by aggregation tree nodes is used to collect all information for doing a pivot in a single pass over
 * a table. Edges to the next node are labelled by Objects in the table. Nodes connected to leaves have an additional
 * integer indicating in which row in the table the path to this node appears first. Leaves contain the aggregation
 * functions for every desired aggregation. The results of the aggregation functions of a leaf build a row in the
 * pivot table.
 * <p>
 * A tree is formed like this for example: Assume there are group-by columns G1 and G2, a column grouping column C
 * and a aggregation column A with aggregation function f. For every row (g1, g2, c, a) in the input table, we create
 * (or go along if it already exists) a path in the tree {@code root -(g1)- node1 -(g2)- node2 -(c)- leaf}. In the
 * leaf we apply the function f to {@code a}. The node2 has as additional information the row index where the path
 * {@code root--node1--node2} was first created.
 *
 * @author Gisa Meier
 * @since 9.1
 */
public class AggregationTreeNode {


	private Map<Object, AggregationTreeNode> nextMap = null;
	private Pair<Integer, Map<Object, AggregationTreeLeaf>> leafMapPair = null;

	/**
	 * Gets the node connected by an edge labelled with the value or creates one.
	 *
	 * @param value
	 * 		the edge value
	 * @return an existing or new node
	 */
	public AggregationTreeNode getOrCreateNext(Object value) {
		if (nextMap == null) {
			nextMap = new LinkedHashMap<>();
		}
		return nextMap.computeIfAbsent(value, k-> new AggregationTreeNode());
	}

	/**
	 * Gets the leaf connected by an edge labelled with the value or creates one. When creating one, the current row
	 * index is saved in the current node and the supplier is used to create the desired aggregation functions.
	 *
	 * @param value
	 * 		the edge value
	 * @param functions
	 * 		a supplier for the functions of the {@link AggregationTreeLeaf}
	 * @param rowIndex
	 * 		the current row index in the table
	 * @return an existing or new leaf
	 */
	public AggregationTreeLeaf getOrCreateLeaf(Object value, Supplier<List<AggregationFunction>> functions,
											   int rowIndex) {
		if (leafMapPair == null) {
			leafMapPair = new Pair<>(rowIndex, new HashMap<>());
		}
		AggregationTreeLeaf leaf = leafMapPair.getSecond().get(value);
		if (leaf == null) {
			leaf = new AggregationTreeLeaf(functions.get());
			leafMapPair.getSecond().put(value, leaf);
		}
		return leaf;
	}

	/**
	 * @return the number of connected nodes (that are not leaves)
	 */
	public int size(){
		if (nextMap == null) {
			return 0;
		}
		return nextMap.size();
	}


	/**
	 * An aggregation leaf contains the aggregation functions.
	 */
	public static class AggregationTreeLeaf {

		private final List<AggregationFunction> functions;

		AggregationTreeLeaf(List<AggregationFunction> functions) {
			this.functions = functions;
		}

		/**
		 * @return the aggregation functions of this leaf
		 */
		public List<AggregationFunction> getFunctions() {
			return functions;
		}

	}

	/**
	 * Counts all the nodes (not the leaves) reachable from the given node.
	 *
	 * @param node
	 * 		the node where to start
	 * @return the number of nodes in the tree with the given node as root
	 */
	public static int countLength(AggregationTreeNode node) {
		int count = 0;
		if (node.nextMap != null) {
			for (AggregationTreeNode next : node.nextMap.values()) {
				count += countLength(next);
			}
		} else {
			count++;
		}
		return count;
	}

	/**
	 * Merges the tree with root node1 with the tree with root node2. The edges from node2 are recursively added to
	 * the node1-tree. The aggregation functions of the same leaves are merged.
	 *
	 * @param node1
	 * 		the root of the first tree
	 * @param node2
	 * 		the root of the second tree
	 */
	public static void merge(AggregationTreeNode node1, AggregationTreeNode node2) {
		if (node2.nextMap != null) {
			for (Map.Entry<Object, AggregationTreeNode> entry : node2.nextMap.entrySet()) {
				AggregationTreeNode from1 = node1.nextMap.get(entry.getKey());
				if (from1 == null) {
					node1.nextMap.put(entry.getKey(), entry.getValue());
				} else {
					merge(from1, entry.getValue());
				}
			}
		} else {
			Map<Object, AggregationTreeLeaf> from1 = node1.leafMapPair.getSecond();
			mergeLeaves(from1, node2.leafMapPair.getSecond());
		}
	}

	/**
	 * Merges the outgoing edges and their leaves. If an edge is only in from2 then it is just added to from1.
	 * Otherwise the aggregation functions at the leaf are merged.
	 */
	private static void mergeLeaves(Map<Object, AggregationTreeLeaf> from1, Map<Object, AggregationTreeLeaf> from2) {
		for (Map.Entry<Object, AggregationTreeLeaf> entry : from2.entrySet()) {
			AggregationTreeLeaf leaf1 = from1.get(entry.getKey());
			if (leaf1 == null) {
				from1.put(entry.getKey(), entry.getValue());
			} else {
				List<AggregationFunction> list1 = leaf1.getFunctions();
				List<AggregationFunction> list2 = entry.getValue().getFunctions();
				int functionIndex = 0;
				for (AggregationFunction function : list2) {
					list1.get(functionIndex++).merge(function);
				}
			}
		}
	}

	/**
	 * Recursively converts an aggregation tree into building blocks for a table. Goes down until it finds the node
	 * which is only connected to leaves. The row indices stored in those node are written into the mapping at the
	 * current index and the aggregations stored in the aggregation leaves are written to the associated collectors.
	 *
	 * @param node
	 * 		the current node
	 * @param indexValueToCollector
	 * 		a map from edge values to aggregation collectors
	 * @param managers
	 * 		aggregation managers to construct collectors if they do not exist yet
	 * @param mapping
	 * 		the mapping array to fill, it has the same length as the future table
	 * @param index
	 * 		the current row index in the future table
	 * @return the current row index
	 */
	public static int treeToData(AggregationTreeNode node,
								 Map<Object, List<AggregationCollector>> indexValueToCollector,
								 List<AggregationManager> managers, int[] mapping, int index) {
		int lastIndex = index;
		if (node.nextMap != null) {
			//not a node connected to the leaves, go deeper
			for (AggregationTreeNode nextNode : node.nextMap.values()) {
				lastIndex = treeToData(nextNode, indexValueToCollector, managers, mapping, lastIndex);
			}
		} else if (node.leafMapPair != null) {
			//node connected to leaves found, store information
			mapping[lastIndex] = node.leafMapPair.getFirst();
			for (Map.Entry<Object, AggregationTreeLeaf> entry : node.leafMapPair.getSecond().entrySet()) {
				Object key = entry.getKey();
				AggregationTreeLeaf leaf = entry.getValue();
				List<AggregationCollector> collectors =
						indexValueToCollector.computeIfAbsent(key, k -> createCollectors(managers, mapping.length));

				// collect the values of the aggregation functions at the leaf
				int functionIndex = 0;
				for (AggregationFunction function : leaf.functions) {
					collectors.get(functionIndex++).set(lastIndex, function);
				}
			}
			lastIndex++;
		}
		return lastIndex;
	}

	/**
	 * Creates collectors from the managers.
	 */
	private static List<AggregationCollector> createCollectors(List<AggregationManager> managers, int length) {
		List<AggregationCollector> collectors = new ArrayList<>(managers.size());
		for (AggregationManager manager : managers) {
			collectors.add(manager.getCollector(length));
		}
		return collectors;
	}

}