import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;


public class ApplicationUsersDB {

	ApplicationUsersDB() {
		try {
			new File("../Users").mkdir();
			Class.forName("org.sqlite.JDBC");
			Con = DriverManager.getConnection("jdbc:sqlite:../Users/UserDB.db");
			stmnt = Con.createStatement();
			stmnt.executeUpdate("create table if not exists Users (userName TEXT primary key)");
			stmnt.executeUpdate("Create Table if not exists undeliveredMsg (userName TEXT,Msg TEXT)");
			addUser = Con.prepareStatement("insert into Users (userName) values(?)");
			addMsg = Con.prepareStatement("insert into undeliveredMsg(userName,Msg) values(?,?)");
			checkUsr = Con.prepareStatement("select userName from Users where(userName=?)");
			DelMsg = Con.prepareStatement("delete from undeliveredMsg where userName=?");
		}

		catch (Exception err) {
			System.err.println(err.getMessage());
		}
	}

	public synchronized int addUser(String Name) {
		try {
			addUser.setString(1, Name);
			addUser.executeUpdate();
		}

		catch (SQLException err) {

			return 0;

		}
		return 1;
	}

	public synchronized  void addMsg(String userName, String msg) {
		try {
			addMsg.setString(1, userName);
			addMsg.setString(2, msg);
			addMsg.executeUpdate();
		}

		catch (Exception err) {
			System.err.println(err.getMessage());
		}
	}

	public synchronized  int checkUser(String name) {
		try {
			checkUsr.setString(1, name);
			if (!checkUsr.executeQuery().next()) {
				return 0;
			}

		}

		catch (Exception err) {
			System.err.println(err.getMessage());
		}
		return 1;
	}
	public synchronized ArrayList<String> getUsers() {
		ArrayList<String> userNames = new ArrayList<String>();
		try {
			ResultSet users = stmnt.executeQuery("select * from Users");

			while (users.next()) {
				userNames.add(users.getString(1));
			}
		}

		catch (Exception err) {
			System.err.println(err.getMessage());
		}
		return userNames;
	}

	public synchronized String[] getMsgs(String name) {
		ArrayList<String>msgs = new ArrayList<String>();
		try {
			int count = 0;
			ResultSet msg = stmnt.executeQuery("select Msg from undeliveredMsg where(userName='" + name + "')");
			while (msg.next()) {
				msgs.add(msg.getString(1));
				count++;
			}
			if (count == 0) {
				return null;
			}
			DelMsg.setString(1, name);
			DelMsg.executeUpdate();
		}

		catch (Exception err) {
			System.err.println(err.getMessage());
		}

		return msgs.toArray(new String[0]);
	}
	private Connection Con;
	private Statement stmnt;
	PreparedStatement addUser, addMsg, checkUsr, DelMsg;
}