
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Pos;
import javafx.geometry.Pos.*;
import javafx.concurrent.Task;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javafx.application.Platform;
import java.net.Socket;
import javafx.scene.control.ListView;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Screen;
import javafx.scene.text.TextAlignment;
import javafx.scene.layout.BorderPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.HBox;
import javafx.scene.Node;
import java.util.LinkedHashMap;
import javafx.concurrent.WorkerStateEvent;
import java.util.ArrayList;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import java.io.File;
import java.io.ByteArrayOutputStream;
import javafx.embed.swing.SwingFXUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.ByteArrayInputStream;



public class UserGUI extends Application {
	/*
	--------------------------------------------------------------------------------
	    For Initializing Application
	--------------------------------------------------------------------------------
	*/
	public void init() {
		MainPane = new BorderPane();
		Tabs = new TabPane();
		Tabs.getStyleClass().add("tab-pane");
		MainScene = new Scene(MainPane);
		MainScene.getStylesheets().add("Main.css");
		MainPane.getStyleClass().add("mainPane");
		broadCast = new Tab();
		menuBar = new MenuBar();
	}

	/*
	--------------------------------------------------------------------------------
	    For Starting Application
	--------------------------------------------------------------------------------
	*/
	public void start(Stage mystage) {
		this.MainStage = mystage;
		MainStage.setTitle("Chat APP");
		MainStage.setResizable(false);
		CreateErrorBox();
		configMenuBar();
		CreateLoginForm();
		broadCast.setText("BroadCast");
		broadCast.setClosable(false);
		Tabs.getTabs().add(broadCast);
		MainPane.setTop(menuBar);
		MainPane.setCenter(Tabs);
		MainPane.setAlignment(menuBar, Pos.TOP_CENTER);
		MainStage.setScene(MainScene);

	}

	/*
	--------------------------------------------------------------------------------
	    For Closing the Connection
	--------------------------------------------------------------------------------
	*/
	public void stop() {
		try {
			dos.writeUTF("$exit");
			dos.close();
			dis.close();
			s.close();
			Thread t = new Thread(backup);
			t.start();
			t.join();

		}

		catch (Exception err) {
			System.err.println(err.getMessage());
		}
	}
	/*
	--------------------------------------------------------------------------------
	  A Daemon to create Backup of current instance before closing
	--------------------------------------------------------------------------------
	*/
	Task<Void> backup = new Task<Void>() {
		public Void call() {
			try {
				BackUPChat b = new BackUPChat(name);
				for (Tab u : Tabs.getTabs()) {
					String tabName = u.getText();
					String msg = "";
					BorderPane tempb = (BorderPane)u.getContent();
					if (!(tempb.getCenter() instanceof ScrollPane)) {
						continue;
					}
					ScrollPane temppane = (ScrollPane)tempb.getCenter();
					VBox box = (VBox)temppane.getContent();
					ArrayList<String[]> msglist = new ArrayList<String[]>();
					for (Node n : box.getChildren()) {
						TilePane tp = (TilePane)n;
						String m[] = new String[2];
						if (((((VBox)((HBox)tp.getChildren().get(0)).getChildren().get(0)).getChildren().get(1))) instanceof Label) {
							m[0] = ((Label)(((VBox)((HBox)tp.getChildren().get(0)).getChildren().get(0)).getChildren().get(0))).getText();
							m[1] = ((Label)(((VBox)((HBox)tp.getChildren().get(0)).getChildren().get(0)).getChildren().get(1))).getText();
							msglist.add(m);
						}
					}
					b.addTab(tabName, msglist);
				}
				b.Save();
			}

			catch (Exception err) {
				err.printStackTrace();
			}
			return null;
		}
	};
	/*
	--------------------------------------------------------------------------------
	  A Daemon to restore previous instance of application
	--------------------------------------------------------------------------------
	*/
	Task<Void> restore = new Task<Void>() {
		public Void call() {
			retrieveChat b = new retrieveChat(name);
			if (b.getFlag() == 1) {
				LinkedHashMap<String, ArrayList<String>> map = b.restore();
				try {
					Platform.runLater(new Runnable() {
						public void run() {
							try {

								for (String TabName : map.keySet()) {
									Tab newTab;
									if (!TabName.equals("BroadCast")) {
										newTab = new Tab(TabName);
										newTab.setContent(CreateChatBox(TabName, "", null));
										Tabs.getTabs().add(newTab);
									} else {
										newTab = Tabs.getTabs().get(0);
									}
									BorderPane tempb = (BorderPane)newTab.getContent();
									ScrollPane temppane = (ScrollPane)tempb.getCenter();
									VBox box = (VBox)temppane.getContent();
									for (String msg : map.get(TabName)) {
										String temp[] = msg.split("~", 2);
										if (!temp[0].equals(name)) {
											createIncomingMsg(temppane, box, temp[0], temp[1], null);
										} else {
											createOutgoingMsg(temppane, box, temp[0], temp[1], null);
										}
									}
								}
								dos.writeUTF("restored");

							}

							catch (Exception err) {
								System.err.println(err.getMessage());
							}

						}
					});

				} catch (Exception err) {
					System.err.println(err.getMessage());
				}
			} else {
				try {
					dos.writeUTF("restored");
				}

				catch (Exception err) {
					System.err.println(err.getMessage());
				}
			}
			return null;
		}
	};
	/*
	--------------------------------------------------------------------------------
	 	Background Process to send image
	--------------------------------------------------------------------------------
	*/
	Service<Void> sendImage = new Service<Void>() {
		protected Task<Void> createTask() {
			return new Task<Void>() {
				public Void call() {
					try {
						String dest = Tabs.getSelectionModel().getSelectedItem().getText();
						if (dest.equals("Add New Chat")) {
							GenerateError("Select A User Tab");
							return null;
						}
						ByteArrayOutputStream stream = new ByteArrayOutputStream();
						ImageIO.write(sendImg, ext, stream);
						final byte image[] = stream.toByteArray();
						dos.writeUTF("$Image");
						dos.writeUTF(dest);
						dos.writeUTF(ext);
						dos.writeInt(image.length);
						dos.write(image, 0, image.length);
						dos.flush();
						readFlag = 0;
						Platform.runLater(new Runnable() {
							public void run() {
								try {
									BufferedImage im = ImageIO.read(new ByteArrayInputStream(image));
									Image i = SwingFXUtils.toFXImage(im, null);
									for (Tab u : Tabs.getTabs()) {
										if (u.getText().equals(to)) {
											BorderPane tempb = (BorderPane)u.getContent();
											ScrollPane temppane = (ScrollPane)tempb.getCenter();
											VBox box = (VBox)temppane.getContent();
											ImageView imageView = new ImageView(i);
											imageView.setFitWidth(400);
											imageView.setFitHeight(400);
											imageView.setSmooth(true);
											imageView.setPreserveRatio(true);
											createOutgoingMsg(temppane, box, name, null, imageView);
											return;
										}
									}
								} catch (Exception err) {
									System.err.println(err.getMessage());
								}
							}
						});

					}

					catch (Exception err) {
						GenerateError(err.getMessage());
						return null;
					}
					return null;
				}
			};
		}
	};
	/*
	--------------------------------------------------------------------------------
	 	Background Process to clear Message Buffer
	--------------------------------------------------------------------------------
	*/
	Service<Void> clearBuffer = new Service<Void>() {
		protected Task<Void> createTask() {
			return new Task<Void>() {
				public Void call() {
					try {
						for (String msg : msgBuffer) {
							dos.writeUTF(msg);
						}
						msgBuffer.clear();
					}

					catch (Exception err) {
						System.err.println(err.getMessage());
					}
					return null;
				}
			};
		}
	};
	/*
	--------------------------------------------------------------------------------
	 	Function to get Current Time Stamp for naming images
	--------------------------------------------------------------------------------
	*/
	public static String getCurrentTimeStamp() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMddHHmmss");
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

	/*
	--------------------------------------------------------------------------------
	  A Daemon to read Data Continuously and update Messsage Container
	   And to add new Chat Box
	--------------------------------------------------------------------------------
	*/
	Service<Void> ReadData = new Service<Void>() {
		protected Task<Void> createTask() {
			return new Task<Void>() {
				public Void call() {
					String msg = "";
					while (true) {
						try {
							msg = (String) dis.readUTF();
							System.out.println("Msg =" + msg);
							if (msg.equals("$complete")) {
								readFlag = 1;
								continue;
							}
							if (flag == 1) {
								flag = 0;
								if (msg.equals("NOUSERFOUND")) {
									GenerateError(msg);
								} else {
									String listofName[] = msg.split("\n");
									Platform.runLater(new Runnable() {
										public void run() {
											ListView<String> list = new ListView<String>();
											ObservableList<String> Userlist = FXCollections.observableArrayList();
											Userlist.clear();
											for (String n : listofName) {
												int addflag = 1;
												if (!n.equals(name)) {
													for (Tab u : Tabs.getTabs()) {
														if (!n.equals(u.getText())) {
															continue;
														} else {
															addflag = 0;
														}
													}
													if (addflag == 1)
														Userlist.add(n);
												}
											}
											list.setItems(Userlist);
											Tab newUserTab = new Tab("Add New Chat");
											BorderPane pane = new BorderPane();
											pane.setCenter(list);
											Button select = new Button("Select");
											select.setOnAction(new EventHandler<ActionEvent>() {
												public void handle(ActionEvent ae) {
													newUserTab.setText(list.getSelectionModel().getSelectedItem());
													newUserTab.setContent(CreateChatBox("", "", null));
												}
											});
											pane.setBottom(select);
											newUserTab.setContent(pane);
											Tabs.getTabs().add(newUserTab);
											Tabs.getSelectionModel().select(newUserTab);
										}
									});
								}

							} else {

								if (msg.equals("$Image")) {
									String Sender = dis.readUTF();
									System.out.println("sender " + Sender );
									String ext = dis.readUTF();
									int size = dis.readInt();
									System.out.println("Extension- " + ext);
									ByteArrayOutputStream stream = new ByteArrayOutputStream();
									byte imageBytes[];
									imageBytes = new byte[5000];
									int bytesRead = 0;
									while (stream.toByteArray().length != size) {
										bytesRead = dis.read(imageBytes, 0, 5000);
										System.out.println(bytesRead);
										stream.write(imageBytes, 0, bytesRead);
										imageBytes = new byte[5000];
									}
									imageBytes = null;
									dos.writeUTF("$complete");
									BufferedImage img = ImageIO.read(new ByteArrayInputStream(stream.toByteArray()));
									ImageIO.write(img, ext, new File("../images/" + getCurrentTimeStamp() + "." + ext));
									Image i = SwingFXUtils.toFXImage(img, null);
									img = null;
									stream = null;
									Platform.runLater(new Runnable() {
										public void run() {
											for (Tab u : Tabs.getTabs()) {
												if (u.getText().equals(Sender)) {
													BorderPane tempb = (BorderPane)u.getContent();
													ScrollPane temppane = (ScrollPane)tempb.getCenter();
													VBox box = (VBox)temppane.getContent();
													ImageView imageView = new ImageView(i);
													imageView.setFitWidth(350);
													imageView.setFitHeight(350);
													imageView.setSmooth(true);
													imageView.setPreserveRatio(true);
													createIncomingMsg(temppane, box, Sender, null, imageView);
													Tabs.getSelectionModel().select(u);
													return;
												}
											}

											Tab newTab = new Tab(Sender);
											ImageView imageView = new ImageView(i);
											imageView.setFitWidth(350);
											imageView.setFitHeight(350);
											imageView.setSmooth(true);
											imageView.setPreserveRatio(true);
											newTab.setContent(CreateChatBox(Sender, null, imageView));
											Tabs.getTabs().add(newTab);
											Tabs.getSelectionModel().select(newTab);

										}
									});
								} else {
									String temp[] = msg.split("~", 2);
									msg = temp[1];
									String temp1[] = temp[1].split("~", 2);
									Platform.runLater(new Runnable() {
										public void run() {
											for (Tab u : Tabs.getTabs()) {
												if (u.getText().equals(temp1[0])) {
													BorderPane tempb = (BorderPane)u.getContent();
													ScrollPane temppane = (ScrollPane)tempb.getCenter();
													VBox box = (VBox)temppane.getContent();
													createIncomingMsg(temppane, box, temp1[0], temp1[1], null);
													Tabs.getSelectionModel().select(u);
													return;
												}
											}

											Tab newTab = new Tab(temp1[0]);
											newTab.setContent(CreateChatBox(temp1[0], temp1[1], null));
											Tabs.getTabs().add(newTab);
											Tabs.getSelectionModel().select(newTab);

										}
									});
								}
							}
						} catch (Exception err) {
							GenerateError(err.getMessage());
							return null;
						}
					}

				}
			};
		}
	};

	/*
	--------------------------------------------------------------------------------
	 	For Creating Incoming Msgs
	--------------------------------------------------------------------------------
	*/

	private void createIncomingMsg(ScrollPane mainpane, VBox box, String Source, String msg, ImageView img) {

		TilePane pane = new TilePane();
		pane.setId("incomingMsg");
		pane.setPrefColumns(2);
		pane.setPrefRows(1);
		HBox labelContainer = new HBox(20);
		VBox finalMsg = new VBox(0);
		Label sourceName = new Label(Source);
		sourceName.getStyleClass().add("iuserName");
		if (msg != null) {
			Label msgs = new Label(msg);
			msgs.setWrapText(true);
			msgs.setTextAlignment(TextAlignment.LEFT);
			msgs.setMaxWidth(MainStage.getWidth() / 2);
			msgs.getStyleClass().add("incomingMsg");
			finalMsg.getChildren().addAll(sourceName, msgs);
		} else {
			finalMsg.getChildren().addAll(sourceName, img);
		}
		labelContainer.getChildren().add(finalMsg);
		pane.getChildren().add(labelContainer);
		pane.setAlignment(labelContainer, Pos.CENTER_RIGHT);
		Button del = new Button("Delete");
		pane.setOnMousePressed(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent me) {

				del.setOnAction(new EventHandler<ActionEvent>() {
					public void handle(ActionEvent ae) {
						box.getChildren().remove(pane);
					}
				});
				if (!labelContainer.getChildren().contains(del)) {
					labelContainer.getChildren().add(del);
				}
			}
		});
		pane.setOnMouseExited(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent me) {
				if (labelContainer.getChildren().contains(del))
					labelContainer.getChildren().remove(del);
			}
		});
		box.getChildren().add(pane);
		mainpane.setVvalue(1);
	}

	/*
	--------------------------------------------------------------------------------
	 	For Creating OutGoing Msgs
	--------------------------------------------------------------------------------
	*/
	private void createOutgoingMsg(ScrollPane mainpane, VBox box, String Source, String msg, ImageView img) {
		TilePane pane = new TilePane();
		pane.setId("OutGoingMsg");
		HBox labelContainer = new HBox(20);
		VBox finalMsg = new VBox(0);
		Label sourceName = new Label(Source);
		sourceName.getStyleClass().add("ouserName");
		pane.setPrefColumns(2);
		pane.setPrefRows(1);
		if (msg != null) {
			Label out = new Label(msg);
			out.setMaxWidth(MainStage.getWidth() / 2);
			out.getStyleClass().add("OutGoingMsg");
			out.setWrapText(true);
			out.setTextAlignment(TextAlignment.LEFT);
			finalMsg.getChildren().addAll(sourceName, out);
		} else {
			finalMsg.getChildren().addAll(sourceName, img);
		}
		labelContainer.getChildren().add(finalMsg);
		pane.getChildren().add(labelContainer);
		pane.setAlignment(labelContainer, Pos.CENTER_LEFT);
		Button del = new Button("Delete");
		pane.setOnMousePressed(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent me) {

				del.setOnAction(new EventHandler<ActionEvent>() {
					public void handle(ActionEvent ae) {
						box.getChildren().remove(pane);
					}
				});
				if (!labelContainer.getChildren().contains(del)) {
					labelContainer.getChildren().add(del);
				}
			}
		});
		pane.setOnMouseExited(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent me) {
				if (labelContainer.getChildren().contains(del))
					labelContainer.getChildren().remove(del);
			}
		});
		box.getChildren().add(pane);
		mainpane.setVvalue(1);
	}

	/*
	--------------------------------------------------------------------------------
	    For Creating A Login Form to connect to the server and registering the user
	    Name
	--------------------------------------------------------------------------------
	*/
	private void CreateLoginForm() {
		Stage LoginFormStage = new Stage();
		LoginFormStage.setResizable(false);
		GridPane LoginFormPane = new GridPane();
		LoginFormPane.setVgap(10);
		LoginFormPane.setHgap(10);
		LoginFormPane.setPadding(new Insets(10, 10, 10, 10));
		LoginFormPane.getStyleClass().add("loginpane");
		Scene LoginFormScene = new Scene(LoginFormPane);
		LoginFormScene.getStylesheets().add("Login.css");
		LoginFormPane.getColumnConstraints().add(new ColumnConstraints(100));
		LoginFormPane.getColumnConstraints().add(new ColumnConstraints(200));
		LoginFormStage.setScene(LoginFormScene);
		Label userName = new Label("Name");
		Label ipAddress = new Label("IP");
		TextField UserNameField = new TextField();
		TextField IpAddressField = new TextField();
		Button OK = new Button("OK");
		OK.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent ae) {
				name = UserNameField.getText();
				if (IpAddressField.isVisible());
				ip = IpAddressField.getText();
				if (ip.isEmpty())
					ip = "localhost";
				if (!name.isEmpty()) {
					try {
						if (ConnectionSuccessfulFlag == 0) {
							s = new Socket(ip, 2145);
							s.setTcpNoDelay(true);
							dis = new DataInputStream(s.getInputStream());
							dos = new DataOutputStream(s.getOutputStream());

						}
						dos.writeUTF("login");
						ConnectionSuccessfulFlag = 1;
						dos.writeUTF(name);
						String check = dis.readUTF();

						if (check.equals("0")) {
							GenerateError("User is already Logged In");
							IpAddressField.setVisible(false);
							ipAddress.setVisible(false);
							name = "";
						} else if (check.equals("2")) {
							GenerateError("UserName Doesn't Exist Register First");
							IpAddressField.setVisible(false);
							ipAddress.setVisible(false);
							name = "";
						} else if (check.equals("1")) {
							broadCast.setContent(CreateChatBox("", "", null));
							LoginFormStage.close();
							Thread t = new Thread(restore);
							t.setDaemon(true);
							t.start();
							MainStage.show();
							ReadData.start();
							MainStage.setTitle("Chat Application User: " + name);
						}
					} catch (Exception err) {
						GenerateError(err.getMessage());

					}
				}
			}
		});
		OK.setMaxWidth(100);
		Button Register = new Button("Register");
		Register.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent ae) {
				try {
					LoginFormStage.close();
					if (s != null) {
						if (s.isConnected())
							CreateRegistrationForm(1);
					} else
						CreateRegistrationForm(0);
				}

				catch (Exception err) {
					GenerateError(err.getMessage());
				}

			}
		});
		LoginFormPane.add(userName, 0, 0);
		LoginFormPane.add(UserNameField, 1, 0);
		LoginFormPane.add(ipAddress, 0, 1);
		LoginFormPane.add(IpAddressField, 1, 1);
		LoginFormPane.add(OK, 0, 2);
		LoginFormPane.add(Register, 2, 2);
		LoginFormStage.show();

	}
	/*
	--------------------------------------------------------------------------------
	  Registration Form
	--------------------------------------------------------------------------------
	*/
	private void CreateRegistrationForm(int ipflag) {
		Stage RegistrationStage = new Stage();
		RegistrationStage.setResizable(false);
		BorderPane registerPane = new BorderPane();
		registerPane.getStyleClass().add("loginpane");
		Scene Register = new Scene(registerPane);
		Register.getStylesheets().add("Login.css");
		Label UserName = new Label("User Name");
		Label ipAddress = new Label("IP");
		TextField ipField = new TextField();
		if (ipflag == 1) {
			ipField.setVisible(false);
			ipAddress.setVisible(false);
		}
		TextField userName = new TextField();
		Button Submit = new Button("Submit");
		Submit.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent ae) {
				name = userName.getText();
				if (ipField.isVisible())
					ip = ipField.getText();
				if (ip.equals("")) {
					ip = "localhost";
				}
				if (!name.isEmpty()) {
					try {
						if (ConnectionSuccessfulFlag == 0) {
							s = new Socket(ip, 2145);
							s.setTcpNoDelay(true);
							dis = new DataInputStream(s.getInputStream());
							dos = new DataOutputStream(s.getOutputStream());
						}
						ConnectionSuccessfulFlag = 1;
						dos.writeUTF("register");
						dos.writeUTF(name);
						String num = dis.readUTF();
						if (num.equals("0")) {
							GenerateError("UserName Already Exists");
							name = "";
						} else {
							broadCast.setContent(CreateChatBox("", "", null));
							RegistrationStage.close();
							MainStage.show();
							ReadData.start();
							MainStage.setTitle("Chat Application User: " + name);
						}
					}

					catch (Exception err) {
						GenerateError(err.getMessage());
						System.out.println(err.getMessage());
					}

				}
			}
		});
		GridPane ipane = new GridPane();
		ipane.add(UserName, 0, 0);
		ipane.add(userName, 1, 0);
		GridPane.setMargin(UserName, new Insets(10, 10, 10, 10));
		GridPane.setMargin(userName, new Insets(10, 10, 10, 10));
		registerPane.setTop(ipane);
		registerPane.setLeft(ipAddress);
		registerPane.setRight(ipField);
		registerPane.setBottom(Submit);
		registerPane.setAlignment(ipAddress, Pos.TOP_LEFT);
		registerPane.setAlignment(ipField, Pos.TOP_RIGHT);
		registerPane.setAlignment(Submit, Pos.BOTTOM_CENTER);
		registerPane.setMargin(ipAddress, new Insets(10, 10, 10, 10));
		registerPane.setMargin(ipField, new Insets(10, 10, 10, 85));
		registerPane.setMargin(Submit, new Insets(20, 10, 10, 10));
		RegistrationStage.setScene(Register);
		RegistrationStage.show();
	}
	/*
	--------------------------------------------------------------------------------
	  Configure MenuBar And MenuItem
	--------------------------------------------------------------------------------
	*/
	private void configMenuBar() {
		Menu New = new Menu("New");
		MenuItem addNewUser = new MenuItem("Add New User");
		New.getItems().add(addNewUser);
		addNewUser.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent ae) {
				try {
					flag = 1;
					dos.writeUTF("$getNames");
				}

				catch (Exception err) {
					GenerateError(err.getMessage());
				}
			}
		});
		Menu attach = new Menu("Attach");
		MenuItem image = new MenuItem("Image");
		attach.getItems().add(image);
		image.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent Ae) {
				try {

					to = Tabs.getSelectionModel().getSelectedItem().getText();
					if (to.equals("Add New Chat")) {
						GenerateError("Not A valid Tab");
						return;
					}
					selectImage();
					if (sendImg != null) {
						if (!sendImage.isRunning()) {
							sendImage.reset();
							sendImage.start();
						}
					} else {
						GenerateError("Image can not be greater than 25 Mb ");
					}
				}

				catch (Exception err) {
					GenerateError(err.getMessage());
				}
			}
		});
		menuBar.getMenus().add(New);
		menuBar.getMenus().add(attach);
	}

	/*
	--------------------------------------------------------------------------------
	    For Selecting an Image
	--------------------------------------------------------------------------------
	*/

	public void selectImage() {
		try {
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Open Resource File");
			chooser.getExtensionFilters().addAll(
			    new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
			File selectedFile = chooser.showOpenDialog(MainStage);
			if (selectedFile.length() > 26214400) {
				sendImg = null;
				ext = null;
				return;
			}
			sendImg = ImageIO.read(selectedFile);
			ext = getFileExtension(selectedFile);

		}

		catch (Exception err) {
			sendImg = null;
			ext = null;
			GenerateError(err.getMessage());

		}
	}
	/*
	--------------------------------------------------------------------------------
	 	Function to obtain file Extension
	--------------------------------------------------------------------------------
	*/
	private static String getFileExtension(File file) {
		String fileName = file.getName();
		if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
			return fileName.substring(fileName.lastIndexOf(".") + 1);
		else return "";
	}

	/*
	--------------------------------------------------------------------------------
	    For Creating the UI for ChatBox
	--------------------------------------------------------------------------------
	*/
	private BorderPane CreateChatBox(String Source, String defaultMsg, ImageView img) {
		BorderPane OuterPane = new BorderPane();
		OuterPane.getStyleClass().add("ChatBox");
		BorderPane TextPane = new BorderPane();
		TextPane.getStyleClass().add("Textpane");
		TextField InputTextField = new TextField();
		Button Send = new Button("SEND");
		ScrollPane MessageBox = new ScrollPane();
		MessageBox.setPrefHeight(400);
		MessageBox.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		VBox MessageContainer = new VBox(10);
		MessageContainer.getStyleClass().add("MessageContainer");
		MessageContainer.prefHeightProperty().bind(MessageBox.heightProperty());
		MessageContainer.setFillWidth(true);
		MessageContainer.prefWidthProperty().bind(MessageBox.widthProperty());
		if (defaultMsg != null) {
			if (!defaultMsg.equals(""))
				createIncomingMsg(MessageBox, MessageContainer, Source, defaultMsg, null);
		} else {
			createIncomingMsg(MessageBox, MessageContainer, Source, null, img);
		}

		MessageBox.setContent(MessageContainer);
		Send.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent ae) {
				String msg = InputTextField.getText();
				InputTextField.setText("");
				if (!msg.isEmpty()) {
					try {
						createOutgoingMsg(MessageBox, MessageContainer, name, msg, null);
						String dest = Tabs.getSelectionModel().getSelectedItem().getText();
						if (readFlag == 0) {
							if (dest.equals("BroadCast")) {
								msgBuffer.add(msg);
							} else {
								msgBuffer.add("@" + dest + " " + msg);
							}
							return;
						} else {
							if (!msgBuffer.isEmpty()) {
								if (!clearBuffer.isRunning()) {
									clearBuffer.reset();
									clearBuffer.start();
								}
							}

						}
						if (dest.equals("BroadCast")) {
							dos.writeUTF(msg);
						} else {
							dos.writeUTF("@" + dest + " " + msg);
						}

					} catch (Exception err) {
						GenerateError(err.getMessage());
					}
				}
			}
		});
		InputTextField.setPrefWidth(600);
		TextPane.setCenter(InputTextField);
		TextPane.setAlignment(InputTextField, Pos.BOTTOM_CENTER);
		TextPane.setRight(Send);
		TextPane.setAlignment(Send, Pos.BOTTOM_CENTER);
		TextPane.setMargin(InputTextField, new Insets(0, 5, 0, 5));
		OuterPane.setBottom(TextPane);
		OuterPane.setCenter(MessageBox);
		OuterPane.setAlignment(MessageBox, Pos.BOTTOM_CENTER);
		OuterPane.setAlignment(TextPane, Pos.BOTTOM_CENTER);
		return OuterPane;
	}
	/*
	--------------------------------------------------------------------------------
	    For Creating A Dialog Box to generate any custom Error Dialogs
	--------------------------------------------------------------------------------
	*/

	private void CreateErrorBox() {
		ErrorStage = new Stage();
		ErrorPane = new BorderPane();
		ErrorScene = new Scene(ErrorPane);
		ErrorLabel = new Label();
		ErrorStage.setResizable(false);
		ErrorPane.setPadding(new Insets(20, 20, 20, 20));
		ErrorStage.setScene(ErrorScene);
		ErrorPane.setTop(ErrorLabel);
		ErrorPane.setCenter(new Label(" "));
		ErrorScene.getStylesheets().add("Main.css");
		ErrorPane.getStyleClass().add("ErrorPane");
		ErrorStage.setResizable(false);
		Button ok = new Button("Ok");
		ok.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent ae) {
				ErrorStage.close();
			}
		});
		ErrorPane.setBottom(ok);
		ErrorPane.setAlignment(ok, Pos.BOTTOM_CENTER);
		ErrorPane.setAlignment(ErrorLabel, Pos.TOP_CENTER);
	}

	/*
	--------------------------------------------------------------------------------
	    For Displaying A Dialog Box to generate any custom Error
	--------------------------------------------------------------------------------
	*/

	private void GenerateError(String error) {
		ErrorLabel.setText(error);
		ErrorStage.show();

	}
	/*
	--------------------------------------------------------------------------------
	    For Executing Application
	--------------------------------------------------------------------------------
	*/
	public static void main(String[] args) {
		launch(args);
	}

	/*
	--------------------------------------------------------------------------------
	    Private Global Variables
	--------------------------------------------------------------------------------
	*/
	private Stage MainStage;
	private Scene MainScene;
	private BorderPane MainPane;
	private TabPane Tabs;
	private Socket s;
	private DataInputStream dis;
	private DataOutputStream dos;
	private String name = "", ip = "";
	private int flag = 0;
	private Tab broadCast;
	private MenuBar menuBar;
	private int ConnectionSuccessfulFlag = 0;
	private Stage ErrorStage;
	private Scene ErrorScene;
	private BorderPane ErrorPane;
	private int flags = 0;
	private Label ErrorLabel;
	private BufferedImage sendImg;
	private String ext;
	private int readFlag = 1;
	private ArrayList<String> msgBuffer = new ArrayList<String>();
	private String to;
}
