import java.awt.AWTException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {

	/**
	 * @param args
	 */
	
	ServerSocket ss;
	Socket socket;
	boolean running = true;

	public static HashMap<Long ,ServerHelper> map = new HashMap<Long, ServerHelper>();

	Server(int port) throws IOException {
		ss = new ServerSocket(port);
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (running) {
					ServerHelper sh;
					Socket socket;
					try {
						socket = ss.accept();
						if (socket.isConnected()) {
							System.out.println("A client has connected.");
						}
						
						sh = new ServerHelper(socket);
						long uId;
						do {
							uId = sh.generateUniqueId();
						} while(map.containsKey(uId));
						
						GUI.listString.add(uId + "");
						GUI.list.setListData(GUI.listString.toArray());
						
						sh.setUniqueId(uId);
						map.put(uId, sh);
						
						sh.getResponse();
						map.put(uId, sh);
						
						Thread.sleep(25);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}

		});
		thread.start();
	}
	
	public static void deleteSocketfromMap(long uId) {
		map.remove(uId);
		GUI.listString.remove(uId + "");
		System.out.println("Removing id");
		GUI.list.setListData(GUI.listString.toArray());
		System.out.println("Removed.");
	}

	void broadcast(char message) throws AWTException, IOException {
		for (long uId : map.keySet()) {
			map.get(uId).command(message);
		}
	}

}
