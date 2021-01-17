package buildcraft.core.tablet.manager;

import java.util.Date;

import buildcraft.core.tablet.TabletBase;

public class TabletThread implements Runnable {
	private final TabletBase tablet;

	private long begunTickDate;
	private long lastTickReceivedDate;
	private float ticksLeft = 0.0F;
	private boolean isRunning = false;

	public TabletThread(TabletBase tablet) {
		this.tablet = tablet;
		lastTickReceivedDate = begunTickDate = System.nanoTime();//Bogdan-G: emm..., why Date?
	}

	public TabletBase getTablet() {
		return tablet;
	}

	@Override
	public void run() {
		isRunning = true;
		while (isRunning) {
			if (ticksLeft > 0.0F) {
				begunTickDate = System.nanoTime();
				tablet.tick(ticksLeft);
				float timeElapsed = (float) (lastTickReceivedDate - begunTickDate) / 1000.00000F;
				if (timeElapsed > 0.00000f) {
					ticksLeft -= timeElapsed;
				}
			} else {
				try {
					Thread.sleep(2, 500000);
				} catch (Exception e) {
				}
			}
		}
	}

	public void stop() {
		isRunning = false;
	}

	public void tick(float time) {
		ticksLeft += time;
		lastTickReceivedDate = System.nanoTime();
	}
}
