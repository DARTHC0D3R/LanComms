import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;



class ServerClass {
	ServerClass() throws Exception {
		ss = new ServerSocket(2145);
		msgs = new ArrayList<MessageClass>();
		users = new LinkedHashMap<String, ConnectedUser>();
		Database = new ApplicationUsersDB();
		userNames = null;
		HandleClient = new Thread(new Runnable() {

			public void run() {

				while (true) {
					try {
						Socket socket = ss.accept();
						socket.setTcpNoDelay(true);
						DataInputStream dis = new DataInputStream(socket.getInputStream());
						DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
						String checker = dis.readUTF();
						String temp = dis.readUTF();
						if (checker.equals("register")) {
							userNames = Database.getUsers();
							while (userNames.contains(temp)) {
								dos.writeUTF("0");
								checker = dis.readUTF();
								temp = dis.readUTF();
							}
							userNames.add(temp);
							dos.writeUTF("1");
							ConnectedUser obj = new ConnectedUser(temp, socket, dis, dos);
							System.out.println(temp + " Added");
							users.put(temp, obj);
						} else {
							synchronized (this) {
								int check = Database.checkUser(temp);
								while (check != 1) {
									if (checker.equals("login")) {
										dos.writeUTF("2");
									}
									checker = dis.readUTF();
									temp = dis.readUTF();
									if (checker.equals("register"))
										break;
									check = Database.checkUser(temp);
									if (users.containsKey(temp)) {
										dos.writeUTF("0");
										check = 0;
									}
								}
								ConnectedUser obj = new ConnectedUser(temp, socket, dis, dos);
								users.put(temp, obj);
								dos.writeUTF("1");

								System.out.println(temp + " Added");
							}
						}
						try {
							if (!checker.equals("register")) {
								String restoreflag = "";
								while (!restoreflag.equals("restored")) {
									restoreflag = dis.readUTF();
								}
								String msg[] = Database.getMsgs(temp);
								if (msg != null) {
									for (String m : msg) {
										dos.writeUTF(m);
									}
								}
							} else {
								Database.addUser(temp);
							}
						} catch (Exception E) {
							msgs.add(new MessageClass(new String("$exit").getBytes(StandardCharsets.UTF_8), 'c', temp, null, null));
							continue;
						}
						ServerRead read = new ServerRead(msgs, dis, dos, temp, Database, wait);
						read.start();
						HandleClient.sleep(20);


					} catch (Exception e) {
						System.err.println(e.getMessage());
					}

				}

			}
		});
		write = new Thread(new Runnable() {
			public void run() {
				while (true) {
					String name = "";
					try {
						int count = 0;
						for (MessageClass msg : msgs) {
							String to = msg.getDestination();
							System.out.println(to);
							if (wait.contains(to)) {
								System.out.println(to + " Waiting");
								continue;
							}
							count++;
							dest = null;
							name = msg.getSender();
							System.out.println(name);
							char msgType = msg.getType();
							System.out.println(msgType);
							if (msgType == 's') {
								String m = new String(msg.getMsg(), StandardCharsets.UTF_8);
								if (!to.equals("BroadCast")) {
									if (!users.containsKey(to)) {
										Database.addMsg(to, to + "~" + name + "~" + m);
										msgs.remove(msg);
										break;
									}
									else{
										users.get(to).dos.writeUTF(to + "~" + name + "~" + m);
										msgs.remove(msg);
										break;
									}
								} else {
									userNames = Database.getUsers();
									for (String u : userNames) {
										if (!u.equals(name)) {
											if (users.containsKey(u)) {
												users.get(u).dos.writeUTF(u + "~" + name + "~" + m);
											} else {
												Database.addMsg(u, u + "~" + name + "~" + m);
											}
										}
									}
									msgs.remove(msg);
									break;
								}



							} else if (msgType == 'c') {
								String m =  new String(msg.getMsg(), StandardCharsets.UTF_8);
								if (m.equals("$exit")) {
									users.get(name).closeConnection();
									users.remove(name);
									System.out.println(name + " Exited");
									msgs.remove(msg);
									break;
								}

							} else if (msgType == 'i') {
								if (!to.equals("BroadCast")) {
									wait.add(to);
									byte img[] = msg.getMsg();
									users.get(to).dos.writeUTF("$Image");
									users.get(to).dos.writeUTF(msg.getSender());
									users.get(to).dos.writeUTF(msg.getExtension());
									users.get(to).dos.writeInt(img.length);
									users.get(to).dos.write(img, 0, img.length);
									users.get(to).dos.flush();

								} else {
									userNames = Database.getUsers();
									for (String usr : userNames) {
										if (!users.containsKey(usr) || usr.equals(msg.getSender()) ) {
											continue;
										}
										wait.add(usr);
										byte img[] = msg.getMsg();
										users.get(usr).dos.writeUTF("$Image");
										users.get(usr).dos.writeUTF(msg.getSender());
										users.get(usr).dos.writeUTF(msg.getExtension());
										users.get(usr).dos.writeInt(img.length);
										users.get(usr).dos.write(img, 0, img.length);
										users.get(usr).dos.flush();
									}
								}
								msgs.remove(msg);
								break;
							}
						}
						if (count == 0)
							write.sleep(10);
					} catch (Exception E) {
						msgs.add(new MessageClass(new String("$exit").getBytes(StandardCharsets.UTF_8), 'c', name, null, null));
					}
				}

			}
		});
		HandleClient.start();
		write.start();


	}

	Thread HandleClient, write;
	private ServerSocket ss;
	private DataOutputStream dest;
	private LinkedHashMap<String, ConnectedUser> users;
	private ArrayList<MessageClass> msgs;
	private ArrayList<String> userNames;
	private int flag = 0;
	private String temp;
	private ApplicationUsersDB Database;
	private ArrayList<String> wait = new ArrayList<String>();
}

public class Server {
	public static void main(String[] args) {

		try {

			ServerClass obj = new ServerClass();
			obj.write.join();
		}

		catch (Exception err) {
			System.err.println(err.getMessage());
		}

	}
}