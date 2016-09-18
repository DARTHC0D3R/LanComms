import java.util.ArrayList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;


public class ServerRead extends Thread {

	ServerRead(ArrayList<MessageClass> msgs, DataInputStream dis, DataOutputStream dos, String Name, ApplicationUsersDB d, ArrayList<String> a) {
		msgbuffer = msgs;
		istream = dis;
		ostream = dos;
		this.Name = Name;
		Database = d;
		waitList = a;
	}

	public void run() {
		synchronized (this) {
			try {
				while (true) {
					msg = "";
					msg = istream.readUTF();
					System.out.println(msg);
					if (msg.equals("$complete")) {
						waitList.remove(Name);
						continue;
					}
					if (msg.equals("$exit")) {
						msgbuffer.add(new MessageClass(msg.getBytes(StandardCharsets.UTF_8), 'c', Name, null, null));
						break;
					} else if (msg.equals("$Image")) {
						String dest = istream.readUTF();
						String ext = istream.readUTF();
						int size = istream.readInt();
						ByteArrayOutputStream stream = new ByteArrayOutputStream();
						byte imageBytes[];
						imageBytes = new byte[5000];
						int bytesRead = 0;
						while (stream.toByteArray().length != size) {
							bytesRead = istream.read(imageBytes, 0, 5000);
							stream.write(imageBytes, 0, bytesRead);
							imageBytes = new byte[5000];
						}
						imageBytes = null;
						ostream.writeUTF("$complete");
						msgbuffer.add(new MessageClass(stream.toByteArray(), 'i', Name, dest, ext));
					} else if (msg.equals("$getNames")) {
						String nameslist[] = Database.getUsers().toArray(new String[0]);
						String names = "";
						if (nameslist.length != 1) {

							for (String Names : nameslist) {
								if (!Name.equals(Names))
									names = names + Names + "\n";
							}
							ostream.writeUTF(names);
						} else {
							ostream.writeUTF("NOUSERFOUND");
						}

					} else {
						if (String.valueOf(msg.charAt(0)).equals("@")) {
							String temp[] = msg.split(" ", 2);
							System.out.println(temp[0] + " " + temp[1]);
							temp[0] = temp[0].substring(1);
							System.out.println(temp[0] + "----" + temp[1]);

							msgbuffer.add(new MessageClass(temp[1].getBytes(StandardCharsets.UTF_8), 's', Name, temp[0], null));
						} else {
							msgbuffer.add(new MessageClass(msg.getBytes(StandardCharsets.UTF_8), 's', Name, "BroadCast", null));
						}
					}
				}
			}

			catch (Exception err) {
				err.printStackTrace();
				msgbuffer.add(new MessageClass(new String("$exit").getBytes(StandardCharsets.UTF_8), 'c', Name, null, null));
			}
		}
	}
	private ArrayList<MessageClass> msgbuffer;
	private DataInputStream istream;
	private DataOutputStream ostream;
	private String msg = "";
	private ArrayList<String> waitList;
	final private String Name;
	ApplicationUsersDB Database;
}