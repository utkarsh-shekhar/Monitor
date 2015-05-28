import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Iterator;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;

public class ServerHelper {
	// private ServerSocket ss;
	// private Socket commandSocket;
	private Socket socket;
	DataInputStream is;
	DataOutputStream os;
	long uId;
	boolean stream = false;

	ServerHelper(Socket socket) throws IOException {
		this.socket = socket;
		is = new DataInputStream(socket.getInputStream());
		os = new DataOutputStream(socket.getOutputStream());

	}

	long generateUniqueId() {
		Random random = new Random();
		return random.nextLong();
	}

	void setUniqueId(long uId) throws IOException {
		this.uId = uId;
		os.writeLong(uId);
	}

	long getUniqueId() {
		return uId;
	}

	void command(char c) throws AWTException, IOException {
		if (c == 'S' || c == 's') {
			System.out.println("Here");
			os.write(224);
			System.out.println("Written code 224");
		} else if (c == 'T' || c == 't') {
			os.write(48);
		} else if (c == 'P' || c == 'p') {
			os.write(225);
		} else if (c == 'F' || c == 'f') {
			os.write(220);
			sendFile();
		} else if (c == 'V' || c == 'v') {
			stream = true;
			os.write(221);
		} else if (c == 'C' || c == 'c') {
			stream = false;
			os.write(222);
		} else if(c == 'W' || c == 'w') {
			stream = true;
			os.write(223);
		} else if(c == 'Q' || c == 'q') {
			os.write(224);
		}
		os.flush();
	}

	private static final int MAX_IMAGE_SIZE = 50 * 1024 * 1024;

	static void readImages(InputStream stream) throws IOException {
		stream = new BufferedInputStream(stream);

		while (true) {
			stream.mark(MAX_IMAGE_SIZE);

			ImageInputStream imgStream = ImageIO.createImageInputStream(stream);

			Iterator<ImageReader> i = ImageIO.getImageReaders(imgStream);
			if (!i.hasNext()) {
				// logger.log(Level.FINE, "No ImageReaders found, exiting.");
				break;
			}

			ImageReader reader = i.next();
			reader.setInput(imgStream);

			BufferedImage image = reader.read(0);
			if (image == null) {
				// logger.log(Level.FINE, "No more images to read, exiting.");
				break;
			}

			// logger.log(Level.INFO,
			// "Read {0,number}\u00d7{1,number} image",
			// new Object[] { image.getWidth(), image.getHeight() });

			long bytesRead = imgStream.getStreamPosition();

			stream.reset();
			stream.skip(bytesRead);
		}
	}

	void getResponse() {

		Thread thread = new Thread(new Runnable() {

			boolean running = true;
			int count = 0;

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (running) {
					int c;
					try {
						c = is.read();
						if (c == 224) {
							System.out.println("Received Request");
						} else if (c == 20) {
							int nameChars = is.readInt();
							byte[] fileNameBytes = new byte[nameChars];
							for (int i = 0; i < nameChars; i++) {
								fileNameBytes[i] = is.readByte();
							}
							String fileName = new String(fileNameBytes);
							System.out.println("Filename is: " + fileName);

							FileOutputStream fos = new FileOutputStream(
									"/home/utkarsh/lol/" + fileName.toString());

							long filesize = is.readLong();

							for (long i = 0; i < filesize; i++) {
								// System.out.println(i);
								fos.write(is.read());
								fos.flush();
							}
							fos.close();

							System.out
									.println("Picture received successfully.");
						} else if (c == 221) {
							System.out.println("Pressed V");
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							while (stream) {
								BufferedImage img = null;
								while (img == null) {
//									img = ImageIO.read(is);
									try {
										img = Imaging.getBufferedImage(is);
									} catch (ImageReadException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}

								GUI.icon = new ImageIcon(img);
								GUI.screen.setIcon(GUI.icon);
								
								try {
									Thread.sleep(33); // 66 for 15fps, 33 for 30fps.
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							
							
							System.out.println("Reading unwanted data");
							while(!stream) {
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

						} else if (c == 222) {
							System.out
									.println("Screenshot taken. Preparing for sending the file.");

							long filesize = is.readLong();
							System.out.println("File Size is" + filesize);

							int nameChars = is.readInt();
							System.out.println("Total characters in filename: "
									+ nameChars);

							byte[] fileNameBytes = new byte[nameChars];
							for (int i = 0; i < nameChars; i++) {
								fileNameBytes[i] = is.readByte();
							}
							String fileName = new String(fileNameBytes);
							System.out.println("Filename is: " + fileName);

							FileOutputStream fos = new FileOutputStream(
									"/home/utkarsh/lol/" + fileName.toString());
							// char ch;
							// byte[] buffer = new byte[filesize];
							for (long i = 0; i < filesize; i++) {
								// System.out.println(i);
								fos.write(is.read());
								fos.flush();
							}
							fos.close();

							System.out
									.println("Screenshot received successfully.");
						} else if (c == -1) {
							System.out.println("A client has been lost.");
							os.close();
							is.close();
							socket.close();
							Server.deleteSocketfromMap(getUniqueId());
							break;
						}

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						try {
							Thread.sleep(15 * 1000);
						} catch (InterruptedException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
						System.out.println("A client has been lost.");
						try {
							os.close();
							is.close();
							socket.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} finally {
							Server.deleteSocketfromMap(getUniqueId());
						}break;
					}
				}
			}

		});

		thread.start();

	}

	void takeScreenshot() throws AWTException, IOException {
		Robot robo = new Robot();
		BufferedImage image = robo.createScreenCapture(new Rectangle(Toolkit
				.getDefaultToolkit().getScreenSize()));
		ImageIO.write(image, "PNG", new File("screenshot.png"));

		transferFile("./screenshot.png");
	}

	void transferFile(String url) throws IOException {
		FileInputStream fis = new FileInputStream(url);
		int c;

		while ((c = fis.read()) != -1) {
			os.write(c);
		}
		os.write(9999);
		os.flush();
		fis.close();
	}

	void sendFile() throws IOException {
		System.out.println("Enter the path to the file and the pathname: ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String path = br.readLine();
		FileInputStream fis = new FileInputStream(path);

		String name = path.substring(path.lastIndexOf("/") + 1, path.length());
		System.out.println("File name is: " + name);
		byte[] fileName = name.getBytes();
		int nameLength = fileName.length;
		os.writeInt(nameLength);

		for (int i = 0; i < nameLength; i++) {
			os.write(fileName[i]);
		}

		long fileSize = 0;
		int c;
		while ((c = fis.read()) != -1) {
			fileSize += 1;
		}

		os.writeLong(fileSize);
		fis.close();

		FileInputStream fis2 = new FileInputStream(path);
		while ((c = fis2.read()) != -1) {
			os.write(c);
		}
		os.flush();
		fis2.close();
		System.out.println("File Sent.");

	}

}
