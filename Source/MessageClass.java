public class MessageClass{

	public MessageClass(byte[] o,char t, String s,String d,String e){
		MSG=o;
		type=t;
		sender=s;
		destination=d;
		extension=e;
	}

	public byte[] getMsg(){
		return MSG;
	}

	public char getType(){
		return type;
	}

	public String getSender(){
		return sender;
	}

	public String getDestination(){
		return destination;
	}

	public String getExtension(){
		return extension;
	}

	private byte[] MSG;
	private char type;
	private String sender;
	private String destination;
	private String extension;
}