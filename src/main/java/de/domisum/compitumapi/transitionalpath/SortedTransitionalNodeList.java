package de.domisum.compitumapi.transitionalpath;

import de.domisum.compitumapi.transitionalpath.node.TransitionalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SortedTransitionalNodeList
{

	private List<TransitionalNode> nodes;
	private Set<TransitionalNode> nodesContainsTester;


	// -------
	// CONSTRUCTOR
	// -------
	public SortedTransitionalNodeList(int length)
	{
		this.nodes = new ArrayList<>(length);
		this.nodesContainsTester = new HashSet<>(length);
	}


	// -------
	// GETTERS
	// -------
	public TransitionalNode getAndRemoveFirst()
	{
		if(this.nodes.isEmpty())
			return null;

		TransitionalNode firstNode = this.nodes.remove(0);
		this.nodesContainsTester.remove(firstNode);

		return firstNode;
	}

	public boolean contains(TransitionalNode node)
	{
		return this.nodesContainsTester.contains(node);
	}

	public int getSize()
	{
		return this.nodes.size();
	}

	private double getValueToCompare(TransitionalNode node)
	{
		return node.getEstimatedCombinedWeight();
	}


	@Deprecated
	public List<TransitionalNode> getNodes()
	{
		return this.nodes;
	}


	// -------
	// CHANGERS
	// -------
	public void addSorted(TransitionalNode node)
	{
		// don't subtract one from the size since the element could be added after the last current entry
		this.nodesContainsTester.add(node);
		insertIntoList(node, 0, this.nodes.size());
	}

	private void insertIntoList(TransitionalNode node, int lowerBound, int upperBound)
	{
		if(lowerBound == upperBound)
		{
			this.nodes.add(lowerBound, node);
			return;
		}

		double nodeValue = getValueToCompare(node);

		int dividingIndex = (lowerBound+upperBound)/2;
		TransitionalNode dividingNode = this.nodes.get(dividingIndex);
		double dividingValue = getValueToCompare(dividingNode);

		if(nodeValue > dividingValue)
			insertIntoList(node, dividingIndex+1, upperBound);
		else
			insertIntoList(node, lowerBound, dividingIndex);
	}

	public void clear()
	{
		this.nodes.clear();
		this.nodesContainsTester.clear();
	}


	public void sort()
	{
		Collections.sort(this.nodes, (n1, n2)->getValueToCompare(n1) > getValueToCompare(n2) ? 1 : -1);
	}

}
