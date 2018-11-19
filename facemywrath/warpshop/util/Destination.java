package facemywrath.warpshop.util;

public class Destination { 
	
	private Warp destination;
	private Long warpTime;
	
	public Destination(Warp destination, int delayTime) {
		warpTime = System.currentTimeMillis() + 20L*delayTime;
		this.destination = destination;
	}

	public Warp getDestination() {
		return destination;
	}

	public Long getWarpTime() {
		return warpTime;
	}

}
