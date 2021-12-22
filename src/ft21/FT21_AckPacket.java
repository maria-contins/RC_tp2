package ft21;

public class FT21_AckPacket extends FT21Packet {
	public final int cSeqN;
	public final boolean outsideWindow;
	public final int trueSeq;
	
	FT21_AckPacket(byte[] bytes) {
		super( bytes );		
		int seqN = super.getInt();
		this.cSeqN = Math.abs( seqN );
		this.outsideWindow = seqN < 0;

		if(bytes.length - (Integer.BYTES + 1) != 0) {
			this.trueSeq = super.getInt();
		} else {
			this.trueSeq = -1;
		}
	}

	public String toString() {
		return String.format("ACK<%d>", cSeqN);
	}
	
}