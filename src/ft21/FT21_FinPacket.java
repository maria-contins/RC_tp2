package ft21;

public class FT21_FinPacket extends FT21Packet {
	public final int seqN;
	public boolean opDataAcked;
	public int opDataTimeStarted;
	
	public FT21_FinPacket(int seqN) {
		super(PacketType.FIN);
		super.putInt( seqN );
		super.putByte( NO_OPTIONAL_DATA_LEN );
		this.seqN = seqN;
		this.opDataAcked = false;
		this.opDataTimeStarted = -1;
	}
	
	
	public String toString() {
		return String.format("FIN<%d>", seqN);
	}

	public void setACK() {
		opDataAcked = !opDataAcked;
	}

	public boolean getACK() {
		return opDataAcked;
	}

	public void setTime(int n) {
		opDataTimeStarted = n;
	}

	public int getTime() {
		return opDataTimeStarted;
	}
}