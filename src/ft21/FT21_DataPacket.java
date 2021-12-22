package ft21;

public class FT21_DataPacket extends FT21Packet {
	public final int seqN;
	public final byte[] data;
	public final int trueSeq;
	
	public FT21_DataPacket(int seqN, byte[] data) {
		this(seqN, data, data.length);
	}

	public FT21_DataPacket(int seqN, byte[] data, int datalen) {
		super(PacketType.DATA);
		super.putInt(seqN);
		super.putByte(NO_OPTIONAL_DATA_LEN);
		super.putBytes(data, datalen);
		this.seqN = seqN;
		this.data = data;
		this.trueSeq = -1;
	}

	public FT21_DataPacket(int seqN, byte[] data, int datalen,  int trueSeq) {
		super(PacketType.DATA);
		super.putInt(seqN);
		super.putByte( Integer.BYTES );
		super.putInt(trueSeq);
		super.putBytes(data, datalen);
		this.seqN = seqN;
		this.data = data;
		this.trueSeq = trueSeq;
	}

	
	public String toString() {
		return String.format("DATA<%d, len: %d>", trueSeq, data.length);
	}
}