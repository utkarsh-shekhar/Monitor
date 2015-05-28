import java.awt.AWTException;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class GUI extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * @param args
	 */
	Server server = null;
	long selectedId = 1;

	static ImageIcon icon = null;
	static JLabel screen = null;
	static JList list = null;
	static JButton screenCapture, webcamCapture, stop;
	static JLabel clients = null;

	static List<String> listString = new ArrayList<String>();

	GUI() {
		// Start server
		try {
			server = new Server(9999);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// The look and feel
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setSize((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth(),
				(int) Toolkit.getDefaultToolkit().getScreenSize().getHeight());
		setVisible(true);

		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				System.exit(0);
			}
		});
		setLayout(null);

		screen = new JLabel(icon);
		screen.setBackground(new Color(0, 0, 0, 1));
		screen.setBounds(300, 50, 1000, 667);

		screenCapture = new JButton("Stream Screencast");
		screenCapture.setBounds(25, 25, 200, 25);
		screenCapture.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					screenCapture.setEnabled(false);
					webcamCapture.setEnabled(false);
					stop.setEnabled(true);
					Server.map.get(selectedId).command('c');
					Server.map.get(selectedId).command('v');
				} catch (AWTException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		webcamCapture = new JButton("Stream Webcam");
		webcamCapture.setBounds(25, 70, 200, 25);
		webcamCapture.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					screenCapture.setEnabled(false);
					webcamCapture.setEnabled(false);
					stop.setEnabled(true);
					Server.map.get(selectedId).command('c');
					Server.map.get(selectedId).command('w');
				} catch (AWTException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		stop = new JButton("Stop");
		stop.setBounds(25, 115, 200, 25);
		stop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					screenCapture.setEnabled(true);
					webcamCapture.setEnabled(true);
					stop.setEnabled(false);
					Server.map.get(selectedId).command('c');
				} catch (AWTException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
		
		clients = new JLabel("Client list: ");
		clients.setBounds(25, 145, 200, 30);
		
		list = new JList(listString.toArray());
		list.setBounds(25, 180, 200, 600);
		list.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent arg0) {
                if (!arg0.getValueIsAdjusting()) {
                  selectedId = Long.parseLong(list.getSelectedValue().toString());
                }
            }
        });
		
		add(screen);
		add(screenCapture);
		add(webcamCapture);
		add(stop);
		add(clients);
		add(list);
	}

	public static void main(String[] args) {
		new GUI();
	}

}
