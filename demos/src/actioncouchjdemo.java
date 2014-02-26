import com.skitsanos.api.CouchDb;
import com.skitsanos.api.ServerResponse;
import com.skitsanos.api.exceptions.CouchDbException;
import com.skitsanos.api.exceptions.InvalidServerResponseException;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class actioncouchjdemo {

	private JFrame frmActioncouchjDemo;
	private JTextField txtHostName;
	private JTextField txtPort;
	private JTextField txtUserName;
	private JPasswordField txtPassword;
	private JTextField txtDb;
	private JTextArea txtOutput;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					actioncouchjdemo window = new actioncouchjdemo();
					window.frmActioncouchjDemo.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public actioncouchjdemo() {
		initialize();
	}
	
	public CouchDb initCouchDb() {
		CouchDb db = new CouchDb(txtHostName.getText(), Integer.parseInt(txtPort.getText()), txtUserName.getText(), new String(txtPassword.getPassword()));
		return db;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmActioncouchjDemo = new JFrame();
		frmActioncouchjDemo.setTitle("ActionCouchJ Demo");
		
		
		
		
		//frame.setBounds(100, 100, 640, 482);
		frmActioncouchjDemo.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frmActioncouchjDemo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Box verticalBox = Box.createVerticalBox();
		
		JPanel panel = new JPanel();
		panel.setBorder(new EtchedBorder(EtchedBorder.RAISED, null, null));
		verticalBox.add(panel);
		FlowLayout fl_panel = new FlowLayout(FlowLayout.LEADING, 5, 5);
		fl_panel.setAlignOnBaseline(true);
		panel.setLayout(fl_panel);
		
		Box horizontalBox = Box.createHorizontalBox();
		
		JLabel lblHostName = new JLabel("Host Name");
		horizontalBox.add(lblHostName);
		
		txtHostName = new JTextField();
		horizontalBox.add(txtHostName);
		txtHostName.setColumns(10);
		txtHostName.setText("https://usladha.iriscouch.com");
		
		Component horizontalStrut_1 = Box.createHorizontalStrut(20);
		horizontalBox.add(horizontalStrut_1);
		
		JLabel lblPort = new JLabel("Port");
		horizontalBox.add(lblPort);
		
		txtPort = new JTextField();
		horizontalBox.add(txtPort);
		txtPort.setColumns(10);
		txtPort.setText("6984");
		
		Component horizontalStrut_2 = Box.createHorizontalStrut(20);
		horizontalBox.add(horizontalStrut_2);
		
		JLabel lblUserName = new JLabel("User Name");
		horizontalBox.add(lblUserName);
		
		txtUserName = new JTextField();
		horizontalBox.add(txtUserName);
		txtUserName.setColumns(10);
		txtUserName.setText("usladha");
		
		Component horizontalStrut_3 = Box.createHorizontalStrut(20);
		horizontalBox.add(horizontalStrut_3);
		
		JLabel lblPassword = new JLabel("Password");
		horizontalBox.add(lblPassword);
		
		txtPassword = new JPasswordField();
		horizontalBox.add(txtPassword);
		txtPassword.setColumns(10);
		txtPassword.setText("usladha");
		panel.add(horizontalBox);
		frmActioncouchjDemo.getContentPane().setLayout(new BoxLayout(frmActioncouchjDemo.getContentPane(), BoxLayout.X_AXIS));
		frmActioncouchjDemo.getContentPane().add(verticalBox);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		verticalBox.add(tabbedPane);
		
		JPanel panel_1 = new JPanel();
		tabbedPane.addTab("Details", null, panel_1, null);
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
		
		JButton btnVersion = new JButton("Version");
		btnVersion.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new Thread(new Runnable() {					
					@Override
					public void run() {
						try {
							String version = initCouchDb().version();
							txtOutput.append("\nCouchDb Version - " + version + "\n");
						} catch (InvalidServerResponseException | CouchDbException e1) {
							e1.printStackTrace();
						}						
					}
				}).start();				
			}
		});
		panel_1.add(btnVersion);
		
		JButton btnActiveTasks = new JButton("Active Tasks");
		btnActiveTasks.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				new Thread(new Runnable() {					
					@Override
					public void run() {
						ServerResponse response = initCouchDb().getActiveTasks();
						txtOutput.append("\nActive Tasks - " + response.data() + "\n Exec Time    - " + response.execTime() + " ms\n");
					}
				}).start();				
			}
		});
		
		Component horizontalStrut = Box.createHorizontalStrut(20);
		panel_1.add(horizontalStrut);
		panel_1.add(btnActiveTasks);
		
		JPanel panel_2 = new JPanel();
		tabbedPane.addTab("Databases", null, panel_2, null);
		panel_2.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		
		Box verticalBox_1 = Box.createVerticalBox();
		panel_2.add(verticalBox_1);
		
		Box horizontalBox_1 = Box.createHorizontalBox();
		verticalBox_1.add(horizontalBox_1);
		
		JLabel lblDatabaseName = new JLabel("Database Name :");
		horizontalBox_1.add(lblDatabaseName);
		
		txtDb = new JTextField();
		horizontalBox_1.add(txtDb);
		txtDb.setColumns(20);
		
		Box horizontalBox_2 = Box.createHorizontalBox();
		verticalBox_1.add(horizontalBox_2);
		
		JButton btnGetAllDatabases = new JButton("Get All Databases");
		btnGetAllDatabases.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {					
					@Override
					public void run() {
						ServerResponse response = initCouchDb().getDatabases();
						txtOutput.append("\nDatabases - " + response.data() + "\n Exec Time - " + response.execTime() + " ms\n");
					}
				}).start();
			}
		});
		btnGetAllDatabases.setHorizontalAlignment(SwingConstants.LEFT);
		horizontalBox_2.add(btnGetAllDatabases);
		
		JButton btnDatabaseExists = new JButton("Database Exists?");
		horizontalBox_2.add(btnDatabaseExists);
		
		JButton btnCreateDatabase = new JButton("Create Database");
		horizontalBox_2.add(btnCreateDatabase);
		
		JButton btnDeleteDatabase = new JButton("Delete Database");
		horizontalBox_2.add(btnDeleteDatabase);
		
		txtOutput = new JTextArea();
		verticalBox.add(txtOutput);
		txtOutput.setFont(new Font("Consolas", Font.PLAIN, 12));
		txtOutput.setTabSize(2);
		txtOutput.setRows(5);
		txtOutput.setEnabled(false);
		txtOutput.setEditable(false);
	}

}
