package de.domisum.lib.compitum.navmesh.path;

import de.domisum.lib.auxilium.data.container.Duo;
import de.domisum.lib.auxilium.data.container.math.LineSegment3D;
import de.domisum.lib.auxilium.data.container.math.Vector3D;
import de.domisum.lib.auxilium.util.java.debug.ProfilerStopWatch;
import de.domisum.lib.compitum.navmesh.geometry.NavMeshTriangle;
import de.domisum.lib.compitum.transitionalpath.node.TransitionType;

import java.util.ArrayList;
import java.util.List;

public class NavMeshTriangleTraverser
{

	// INPUT
	private Vector3D startPosition;
	private Vector3D targetPosition;
	private List<NavMeshTriangle> triangleSequence;

	// STATUS
	private List<Duo<Vector3D, Integer>> waypoints = new ArrayList<>();
	// triangle traversal
	private Vector3D currentPosition;
	private Vector3D visLeft;
	private Vector3D visRight;
	private int visLeftTriangleIndex;
	private int visRightTriangleIndex;

	private int currentTriangleIndex = 0;

	private Vector3D portalEndpointLeft;
	private Vector3D portalEndpointRight;

	private ProfilerStopWatch stopWatch = new ProfilerStopWatch("triangleTraverser");

	// OUTPUT


	// -------
	// CONSTRUCTOR
	// -------
	public NavMeshTriangleTraverser(Vector3D startPosition, Vector3D targetPosition, List<NavMeshTriangle> triangleSequence)
	{
		this.startPosition = startPosition;
		this.targetPosition = targetPosition;

		this.triangleSequence = triangleSequence;
	}


	// -------
	// GETTERS
	// -------
	public List<Duo<Vector3D, Integer>> getWaypoints()
	{
		return this.waypoints;
	}

	public ProfilerStopWatch getStopWatch()
	{
		return this.stopWatch;
	}


	// -------
	// TRAVERSAL
	// -------
	public void traverseTriangles()
	{
		this.stopWatch.start();

		triangleTraversal:
		{
			this.currentPosition = this.startPosition;

			if(this.triangleSequence.size() == 1)
			{
				this.waypoints.add(new Duo<>(this.targetPosition, TransitionType.WALK));
				break triangleTraversal;
			}

			for(this.currentTriangleIndex = 0;
			    this.currentTriangleIndex < this.triangleSequence.size(); this.currentTriangleIndex++)
				traverseTriangle();
		}

		this.stopWatch.stop();
	}

	private void traverseTriangle()
	{
		NavMeshTriangle triangle = this.triangleSequence.get(this.currentTriangleIndex);
		NavMeshTriangle triangleAfter = this.currentTriangleIndex+1 < this.triangleSequence.size() ?
				this.triangleSequence.get(this.currentTriangleIndex+1) :
				null;

		if(triangleAfter == null) // last triangle
		{
			// visLeft can be null if the transition into the last triangle was a turn
			if(this.visLeft != null)
			{
				Vector3D towardsVisLeft = this.visLeft.subtract(this.currentPosition);
				Vector3D towardsVisRight = this.visRight.subtract(this.currentPosition);

				Vector3D towardsTarget = this.targetPosition.subtract(this.currentPosition);

				if(isLeftOf(towardsVisRight, towardsTarget, false)) // right curve
				{
					newWaypoint(this.visRight, TransitionType.WALK);
				}
				else if(isLeftOf(towardsTarget, towardsVisLeft, false)) // left curve
				{
					newWaypoint(this.visLeft, TransitionType.WALK);
				}
			}

			this.waypoints.add(new Duo<>(this.targetPosition, TransitionType.WALK));
		}
		// either first triangle processing or after new corner
		else if(this.visLeft == null) // if visLeft is null, then visRight is also null
		{
			findPortalEndpoints(triangle, triangleAfter);
			this.visLeft = this.portalEndpointLeft;
			this.visRight = this.portalEndpointRight;
			this.visLeftTriangleIndex = this.currentTriangleIndex;
			this.visRightTriangleIndex = this.currentTriangleIndex;
		}
		else
		{
			findPortalEndpoints(triangle, triangleAfter);

			Vector3D towardsVisLeft = this.visLeft.subtract(this.currentPosition);
			Vector3D towardsVisRight = this.visRight.subtract(this.currentPosition);

			Vector3D towardsPortalEndpointLeft = this.portalEndpointLeft.subtract(this.currentPosition);
			Vector3D towardsPortalEndpointRight = this.portalEndpointRight.subtract(this.currentPosition);

			boolean leftSame = isSame(this.visLeft, this.currentPosition);
			boolean rightSame = isSame(this.visRight, this.currentPosition);

			// check if portal is out on one side
			if(isLeftOf(towardsVisRight, towardsPortalEndpointLeft, true) && !leftSame && !rightSame) // right turn
			{
				newWaypoint(this.visRight, TransitionType.WALK);

				this.currentTriangleIndex = this.visRightTriangleIndex;
				return;
			}
			else if(isLeftOf(towardsPortalEndpointRight, towardsVisLeft, true) && !leftSame && !rightSame) // left turn
			{
				newWaypoint(this.visLeft, TransitionType.WALK);

				this.currentTriangleIndex = this.visLeftTriangleIndex;
				return;
			}

			// confine movement cone
			if(isLeftOf(towardsVisLeft, towardsPortalEndpointLeft, true)) // left
			{
				this.visLeft = this.portalEndpointLeft;
				this.visLeftTriangleIndex = this.currentTriangleIndex;
			}
			if(isLeftOf(towardsPortalEndpointRight, towardsVisRight, true)) // right
			{
				this.visRight = this.portalEndpointRight;
				this.visRightTriangleIndex = this.currentTriangleIndex;
			}
		}
	}

	private void findPortalEndpoints(NavMeshTriangle from, NavMeshTriangle to)
	{
		LineSegment3D portalLineSegment = from.getPortalTo(to).getFullLineSegment();

		this.portalEndpointLeft = portalLineSegment.a;
		this.portalEndpointRight = portalLineSegment.b;

		Vector3D fromCenter = from.getCenter();
		if(isLeftOf(this.portalEndpointRight.subtract(fromCenter), this.portalEndpointLeft.subtract(fromCenter), false))
		{
			Vector3D temp = this.portalEndpointLeft;
			this.portalEndpointLeft = this.portalEndpointRight;
			this.portalEndpointRight = temp;
		}
	}

	private void newWaypoint(Vector3D position, int transitionType)
	{
		this.waypoints.add(new Duo<>(position, transitionType));
		this.currentPosition = position;

		this.visLeft = null;
		this.visRight = null;
	}


	// -------
	// UTIL
	// -------
	private static boolean isLeftOf(Vector3D v1, Vector3D v2, boolean onZero)
	{
		double crossY = v1.crossProduct(v2).y;

		if(crossY == 0)
			return onZero;

		return crossY < 0;
	}


	private static boolean isSame(Vector3D a, Vector3D b)
	{
		if(a == null)
			return b == null;

		return a.equals(b);
	}

}