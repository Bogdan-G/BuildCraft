package buildcraft.core;

public enum PowerMode {

	M2(10), M4(20), M8(40), M16(80), M32(160), M64(320), M128(640);
	public static final PowerMode[] VALUES = values();
	public final int maxPower;

	PowerMode(int max) {
		this.maxPower = max;
	}

	public PowerMode getNext() {
		PowerMode next = VALUES[(ordinal() + 1) % VALUES.length];
		return next;
	}

	public PowerMode getPrevious() {
		PowerMode previous = VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
		return previous;
	}

	public static PowerMode fromId(int id) {
		if (id < 0 || id >= VALUES.length) {
			return M128;
		}
		return VALUES[id];
	}
}