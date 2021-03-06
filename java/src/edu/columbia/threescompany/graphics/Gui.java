package edu.columbia.threescompany.graphics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import edu.columbia.threescompany.client.ChatThread;
import edu.columbia.threescompany.client.LocalGameState;
import edu.columbia.threescompany.common.ConditionVariable;
import edu.columbia.threescompany.common.Coordinate;
import edu.columbia.threescompany.game.Player;
import edu.columbia.threescompany.game.EventMove.MOVE_TYPE;
import edu.columbia.threescompany.game.graphics.GUIGameMove;
import edu.columbia.threescompany.gameobjects.Blob;
import edu.columbia.threescompany.gameobjects.DeathRayBlob;
import edu.columbia.threescompany.gameobjects.ExplodingBlob;
import edu.columbia.threescompany.gameobjects.GameObject;
import edu.columbia.threescompany.gameobjects.GameParameters;
import edu.columbia.threescompany.gameobjects.PullBlob;
import edu.columbia.threescompany.gameobjects.PushBlob;
import edu.columbia.threescompany.gameobjects.SlipperyBlob;

public class Gui extends JFrame {
	
	private static final int 	ACTION_MOVE		= 0;
	private static final int 	ACTION_SPLIT	= 1;
	private static final int 	ACTION_DEATH	= 2;
	private static final int 	ACTION_SLIPPERY	= 3;
	private static final int 	ACTION_EXPLODE	= 4;
	private static final int 	ACTION_PUSH		= 5;
	private static final int 	ACTION_PULL 	= 6;
	
	private static final long 	serialVersionUID = -5234906655320340040L;
	private int 				_xPos, _yPos;
	private Board 				_board;
	private BoardMouseListener	_boardMouseListener;
	private JTextField			_txtLine;
	private JTextArea			_txtAreaChat;
	private JTextArea			_txtAreaQueue;
	private double				_ap = 10;
	private JPanel[] 			_ap_panes;
	private JLabel	 			_ap_label;
	private Color[] 			_ap_colors = {Color.RED, Color.RED, Color.RED, 
			 								  Color.YELLOW, Color.YELLOW, Color.YELLOW,
			 								  Color.GREEN, Color.GREEN, Color.GREEN, Color.GREEN};
	private ChatThread 			_chatThread;
	private LocalGameState 		_gameState;
	private GraphicalGameState	_graphicalState;
	private Map<Blob, Coordinate> _blobMoves = new HashMap<Blob, Coordinate>(); // final positions
	private int 				_selectedAction	= 0;
	private Player 				_activePlayer; // Null means no one's turn
	public ConditionVariable 	_turnOver;
	
	private List<String> 		_buttonCmds = new ArrayList<String>();
	private List<JButton>		_buttons = new ArrayList<JButton>();
	private HashMap<Blob, MOVE_TYPE>
								_blobsToActivate = new HashMap<Blob, MOVE_TYPE>();
	
	private static Gui 			_instance;
	private String				_username;

	// This sucks a little. We used to load images dynamically by looking in the right directory, but that doesn't work
	// in a jar. Now we have to list the images here.
	private String[] _backgroundFiles = {
			"/textures/math_graph_small.gif",
			"/textures/blank_small.jpg",
			"/textures/concrete_triangles_small.jpg",
			"/textures/geometric_small.jpg",
			"/textures/geometric2_small.jpg",
			"/textures/heated_metal_small.jpg",
			"/textures/money_small.jpg",
			"/textures/red_clover_brick_small.jpg",
			"/textures/stone_small.jpg",
			"/textures/wood_panels_small.jpg",
	};
	private URL _defaultBackgroundUrl = null; // Gets set to the first image loaded
	private JPanel _actionButtonsPane;
	private RedrawThread _redrawThread;
	private List<Player> _localPlayers;
	
	public static Gui getInstance(ChatThread thread, List<Player> players) {
		if (_instance == null) _instance = new Gui(thread, players);
		return _instance;
	}

	private Gui(ChatThread chatThread, List<Player> players) {
		super();
		setTitle(getPlayerNamesString(players) + " - Welcome to Blobs!");
		_username = getPlayerNamesString(players);
		_gameState = null;
		_graphicalState = new GraphicalGameState();
		_turnOver = new ConditionVariable();
		_activePlayer = null;
		_localPlayers = players;
		
		try{ UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() ); }
        catch( Exception e ) { e.printStackTrace(); }
        
		_chatThread = chatThread;
        
        // Get coordinates such that window's centered on screen
		Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		_xPos = (int)(rect.getWidth() - GuiConstants.GUI_WIDTH)/2;
		_yPos = (int)(rect.getHeight() - GuiConstants.GUI_HEIGHT)/2;
		
		JPanel mainControlsPane = new JPanel(new BorderLayout());
		JPanel controlsPane = new JPanel(new BorderLayout());
		
		/* setup menubar, main pane, and board pane */
		JMenuBar menubar = getNewMenuBar();
		JPanel mainpane = getMainPane(); 
		JPanel boardpane = getBoardPane();
		
		/* setup AP bar pane */
		JPanel ap_pane = getAPPane();
		controlsPane.add(ap_pane, BorderLayout.NORTH);
		
		/* setup Chat pane */
		JPanel insideControlsPane = getChatPane();
		controlsPane.add(insideControlsPane, BorderLayout.CENTER);
		
		/* setup actions and queue pane */
		JPanel actionsPane = new JPanel(new GridLayout(2,1));
		
		_actionButtonsPane = getActionsPanel();
		actionsPane.add(_actionButtonsPane, BorderLayout.WEST);
		
		JPanel actionQueuePane = getQueuePanel();
		actionsPane.add(actionQueuePane, BorderLayout.EAST);
		controlsPane.add(actionsPane, BorderLayout.SOUTH);
		
		/* setup textures pane */
		JPanel texturesPane = getTexturesPane();
		
		/* setup Done button pane */
		JPanel buttonpane = getButtonPane();
		
		mainControlsPane.add(controlsPane, BorderLayout.NORTH);
		mainControlsPane.add(texturesPane, BorderLayout.CENTER);
		mainControlsPane.add(buttonpane, BorderLayout.SOUTH);
		mainControlsPane.setBackground(GuiConstants.BG_COLOR);
		mainControlsPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		
		mainpane.add(boardpane, BorderLayout.WEST);
		mainpane.add(mainControlsPane, BorderLayout.EAST);
		mainpane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));		
		
		setJMenuBar(menubar);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocation(_xPos, _yPos);
		setResizable(false);
		pack();
		setVisible(true);

		_board.initGraphicsBuffer();
		_board.setBackground(_defaultBackgroundUrl);
		
		_redrawThread = new RedrawThread(_board);
	}

	private JPanel getTexturesPane() {
		JPanel pane = new JPanel();
		pane.setBackground(Color.WHITE);
		for (int i=0; i<_backgroundFiles.length; i++) {
			URL url = this.getClass().getResource(_backgroundFiles[i]);
			if (url == null) {
				System.out.println("Error loading resource " + _backgroundFiles[i]);
				continue;
			}
			ImageIcon image = new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).getScaledInstance(16, 16, Image.SCALE_DEFAULT));
			if (_defaultBackgroundUrl == null) {
				_defaultBackgroundUrl = url;
			}
			JLabel label = new JLabel(image);
			label.addMouseListener(new TextureListener());
			label.setName(url.toString());
			label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			pane.add(label);
		}
		return pane;
	}

	private JPanel getButtonPane() {
		JPanel pane = new JPanel(new BorderLayout());
		
		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 3));
		JButton clearButton = new JButton("Clear Queue");
		clearButton.setMnemonic(KeyEvent.VK_C);
		clearButton.setPreferredSize(new Dimension(220, 25));
		clearButton.setFont(new Font("Tahoma", Font.PLAIN, 12));
		clearButton.addActionListener(new MainButtonListener());
		p.add(clearButton);
		p.setBackground(GuiConstants.BG_COLOR);
		pane.add(p, BorderLayout.NORTH);
		
		p = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 3));
		JButton donebutton = new JButton("Done");
		donebutton.setMnemonic(KeyEvent.VK_D);
		donebutton.setPreferredSize(new Dimension(220, 25));
		donebutton.setFont(new Font("Tahoma", Font.BOLD, 14));
		donebutton.addActionListener(new MainButtonListener());
		p.add(donebutton);
		p.setBackground(GuiConstants.BG_COLOR);
		pane.add(p, BorderLayout.SOUTH);
		
		pane.setBackground(GuiConstants.BG_COLOR);
		return pane;
	}

	private JPanel getQueuePanel() {
		JPanel pane = new JPanel(new BorderLayout());
		_txtAreaQueue = new JTextArea();
		_txtAreaQueue.setRows(9);
		_txtAreaQueue.setColumns(20);
		_txtAreaQueue.setEditable(false);
		_txtAreaQueue.setFont(GuiConstants.CHAT_FONT);
		_txtAreaQueue.setForeground(Color.BLACK);
		_txtAreaQueue.setBackground(Color.WHITE);
		_txtAreaQueue.setLineWrap(true);
		JScrollPane scrollPane = new JScrollPane(_txtAreaQueue);
		pane.add(scrollPane, BorderLayout.CENTER);
		
		pane.setBorder(BorderFactory.createCompoundBorder(
										BorderFactory.createTitledBorder(
										BorderFactory.createLineBorder(Color.GRAY), "Action Queue"),
										BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		pane.setBackground(Color.WHITE);
		return pane;
	}

	private JMenuBar getNewMenuBar() {
		MenuItemListener menuHandler = new MenuItemListener();
//		Insets in;

		JMenuItem settingsItem = new JMenuItem("Settings", 'S');
		settingsItem.setActionCommand("Settings");
		settingsItem.addActionListener(menuHandler);
		
		// Setup exit menuitem with 'X' icon
		JMenuItem exitItem = new JMenuItem("Exit", 'E');
		// menuitem.setIcon(new
		// ImageIcon(GuiConstants.IMAGES_MENU_DIR+"exit16.gif"));
		// in = menuitem.getInsets();
		// in.left -= 16;
		// menuitem.setMargin(in);
		exitItem.setActionCommand("Exit");
		exitItem.addActionListener(menuHandler);

		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		fileMenu.add(settingsItem);
		fileMenu.addSeparator();
		fileMenu.add(exitItem);
		fileMenu.getPopupMenu().setLightWeightPopupEnabled(false);

		// Setup contents menuitem with question mark icon
		JMenuItem helpItem = new JMenuItem("Help", 'H');
		// menuitem.setIcon(new
		// ImageIcon(GuiConstants.IMAGES_MENU_DIR+"help16.gif"));
		// in = menuitem.getInsets();
		// in.left -= 16;
		// menuitem.setMargin(in);
		helpItem.setActionCommand("Help");
		helpItem.addActionListener(menuHandler);
		// menuitem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H,
		// ActionEvent.CTRL_MASK));

		// Setup the About menuitem with the exclamation icon
		JMenuItem aboutItem = new JMenuItem("About", 'A');
//		aboutItem.setIcon(new ImageIcon(GuiConstants.IMAGES_MENU_DIR + "about16.gif"));
//		in = aboutItem.getInsets();
//		in.left -= 16;
//		aboutItem.setMargin(in);
		aboutItem.setActionCommand("About");
		aboutItem.addActionListener(menuHandler);

		// Setup the Help menu
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		helpMenu.add(helpItem);
		helpMenu.addSeparator();
		helpMenu.add(aboutItem);
		helpMenu.getPopupMenu().setLightWeightPopupEnabled(false);

		JMenuBar menubar = new JMenuBar();
		menubar.add(fileMenu);
		menubar.add(helpMenu);
		return menubar;
	}

	private JPanel getAPPane() {
		JPanel pane = new JPanel(new GridLayout(1,11));
		_ap_panes = new JPanel[11];
		_ap_panes[0] = new JPanel();
		_ap_panes[0].setPreferredSize(new Dimension(20,20));
		_ap_panes[0].setBackground(Color.WHITE);
		
		_ap_label = new JLabel(Double.toString(_ap));
		_ap_label.setFont(GuiConstants.AP_LABEL_FONT);
		_ap_panes[0].add(_ap_label);
		
		pane.add(_ap_panes[0]);
		for (int i=1; i<11; i++) {
			_ap_panes[i] = new JPanel();
			_ap_panes[i].setPreferredSize(new Dimension(20,20));
			_ap_panes[i].setBackground(_ap_colors[i-1]);
			_ap_panes[i].setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
			pane.add(_ap_panes[i]);
		}
		pane.setBorder(	BorderFactory.createCompoundBorder(
							BorderFactory.createTitledBorder(
							BorderFactory.createLineBorder(Color.GRAY), "Action Point Meter"),
							BorderFactory.createEmptyBorder(0, 5, 5, 0)));
		pane.setBackground(GuiConstants.BG_COLOR);
		return pane;
	}

	private JPanel getChatPane() {
		JPanel pane = new JPanel(new BorderLayout());
		_txtAreaChat = new JTextArea();
		_txtAreaChat.setRows(9);
		_txtAreaChat.setColumns(20);
		_txtAreaChat.setEditable(false);
		_txtAreaChat.setFont(GuiConstants.CHAT_FONT);
		_txtAreaChat.setForeground(Color.GRAY);
		_txtAreaChat.setLineWrap(true);
		//_txtAreaChat.setAutoscrolls(true);
		JScrollPane scrollPane = new JScrollPane(_txtAreaChat);
		pane.add(scrollPane, BorderLayout.NORTH);
		
		pane.add(new JLabel("<html>&nbsp;</html>"), BorderLayout.CENTER);
		
		_txtLine = new JTextField("Send message to player");
		_txtLine.setFont(GuiConstants.CHAT_FONT);
		_txtLine.setForeground(Color.GRAY);
		_txtLine.addFocusListener(new ChatFocusListener());
		pane.add(_txtLine, BorderLayout.SOUTH);
		_txtLine.addKeyListener(new ChatSendListener());
		pane.setBorder(	BorderFactory.createCompoundBorder(
										BorderFactory.createTitledBorder(
										BorderFactory.createLineBorder(Color.GRAY), "Chat"),
										BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		pane.setBackground(GuiConstants.BG_COLOR);
		return pane;
	}
	
	private JPanel getActionsPanel() {
		setupButtons();
		JPanel pane = new JPanel(new GridLayout(4,2));
		//pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBackground(GuiConstants.BG_COLOR);
		pane.setBorder(	BorderFactory.createCompoundBorder(
										BorderFactory.createTitledBorder(
										BorderFactory.createLineBorder(Color.GRAY), "Available Actions"),
										BorderFactory.createEmptyBorder(5, 5, 5, 5)));
		
		// start from 1 to ignore move button
		for (int i=1; i < _buttonCmds.size(); i++)
			pane.add(getActionButton(_buttonCmds.get(i), i));
		return pane;
	}

	private JButton getActionButton(String label, int i) {
		_buttons.get(i).setAlignmentX(Component.CENTER_ALIGNMENT);
		_buttons.get(i).setPreferredSize(new Dimension(100, 20));
		_buttons.get(i).setEnabled(false);
		_buttons.get(i).setFont(GuiConstants.BUTTON_DISABLED_FONT);
		_buttons.get(i).setBackground(Color.WHITE);
		_buttons.get(i).addActionListener(new BlobAbilitiesButtonListener());
		_buttons.get(i).setActionCommand(label);
		return _buttons.get(i);
	}
	
	private String getPlayerNamesString(List<Player> players) {
		String result = "";
		for (Player player : players) {
			result += player.getName() + ",";
		}
		return result.substring(0,result.length()-1);
	}

	private JPanel getBoardPane() {
		JPanel pane = new JPanel();
		_board = new Board(_graphicalState);
		_boardMouseListener = new BoardMouseListener();
		_board.addMouseListener(_boardMouseListener);
		_board.addMouseMotionListener(_boardMouseListener);
		pane.add(_board);
		
		return pane;
	}

	private JPanel getMainPane() {
		JPanel mainpane = (JPanel)this.getContentPane();
		mainpane.setLayout(new BorderLayout());
		mainpane.setBackground(GuiConstants.BG_COLOR);
		
		return mainpane;
	}
	
	
	private void setAP() {
		for (int i=1; i < 11; i++)
			_ap_panes[i].setBackground(Color.WHITE);
		for (int i=1; i < (int)_ap+1; i++) {
			if (i<=10) _ap_panes[i].setBackground(_ap_colors[i-1]);
		}
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(1);
		df.setMinimumFractionDigits(1);
		_ap_label.setText(df.format(_ap));
	}
	
	public void drawState(LocalGameState gameState)
	{
		_gameState = gameState;
		_board.drawState(gameState);
	}

	private void updateActionsBorder(Blob newSelection) {
		String title;
		if (newSelection == null) {
			title = "Available Actions";
		} else {
			String blobType = newSelection.getClass().toString();
			blobType = blobType.substring(blobType.lastIndexOf('.') + 1);
			title = blobType + ": Available Actions";
		}
		_actionButtonsPane.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(BorderFactory
						.createLineBorder(Color.GRAY), title),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

	}

	private class ChatFocusListener implements FocusListener {
		public void focusGained(FocusEvent e) {
			if (_txtLine.getText().equals("Send message to player"))
				_txtLine.setText("");
			_txtLine.setForeground(Color.BLACK);
		}

		public void focusLost(FocusEvent e) {
			_txtLine.setForeground(Color.GRAY);
		}
	}
	
	private class ChatSendListener implements KeyListener {
		public void keyPressed(KeyEvent e) {}
		public void keyTyped(KeyEvent e) {}
		
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				addChatLine(_username + ": " + _txtLine.getText());
				_chatThread.sendLine(_txtLine.getText());
				_txtLine.setText("");
			}
		}
	}
	
    /**
     * Listener for JMenuBar menu items
     */
    private class MenuItemListener implements ActionListener 
    {
        public void actionPerformed( ActionEvent e ) {
            if( e.getActionCommand().equals( "Exit" ) ) {
            	// TODO want to broadcast message that i'm exiting?
            	_redrawThread.stop();
            	System.exit(0);
			}
            else if (e.getActionCommand().equals("Settings"))
            	new SettingsFrame();
            else if( e.getActionCommand().equals( "About" ) ) // Display about dialog box
                JOptionPane.showMessageDialog(null, GuiConstants.HELP_ABOUT,
                									"About Blobs",
                									JOptionPane.INFORMATION_MESSAGE );
            else if( e.getActionCommand().equals( "Help" ) ) {
            	new HelpFrame();
            }
        }
    }
    
    private class BoardMouseListener implements MouseListener, MouseMotionListener
    {
    	public void mouseReleased(MouseEvent e) {
			if (_gameState == null) { // drawBoard hasn't been called yet
				return;
			}
			if (_activePlayer == null) { // It's no one's turn
				return;
			}
			if (!isLocalPlayer(_activePlayer)) {
				addChatLine("It's not your turn.");
				return;
			}
			Coordinate worldClick = pointToWorldCoordinate(e.getPoint());
			if (e.getButton() == MouseEvent.BUTTON1) { // Left click selects a blob
				Blob newSelection = getNewSelection(worldClick);
				if (newSelection != null) { // clicked a blob that player controls
					_selectedAction = 0;
					_graphicalState.setSelectedBlob(newSelection);
					updateActionsBorder(newSelection);
					updateAvailableActions();
					_board.repaint();
					debug("Selected blob " + _graphicalState.getSelectedBlob());
				}
			} else if (e.getButton() == MouseEvent.BUTTON2 || 
					e.getButton() == MouseEvent.BUTTON3) { // Middle and right click moves
				if (_graphicalState.getSelectedBlob() == null) {
					addChatLine("You must select a blob before trying to move it.");
					return;
				}
				addMoveForCurrentBlob(worldClick);
			}
		}

		private Blob getNewSelection(Coordinate worldClick) {
			Blob newSelection = blobClickedOn(worldClick);
			if (newSelection == null) {
				return null;
			}
			debug("Clicked blob owned by player " + newSelection.getOwner().getName());
			if (!newSelection.getOwner().equals(_activePlayer)) {
				addChatLine("Blob does not belong to you.");
				return null;
			}
			return newSelection;
		}

		private void addMoveForCurrentBlob(Coordinate position) {
			enqueueMove("movement", position);
		}
		
		private Blob blobClickedOn(Coordinate worldClick) {
			for (GameObject object : _gameState.getObjects()) {
				if (object instanceof Blob && !object.isDead()) {
					double clickObjectDistance = object.getPosition().distanceFrom(worldClick);
					if (clickObjectDistance <= object.getRadius()) {
						return (Blob)object;
					}
				}
			}
	    	return null;
	    }

		private void updateAvailableActions() {
			setButtonEnabled(ACTION_MOVE);
			setButtonEnabled(ACTION_SPLIT);
			setButtonDisabled(ACTION_DEATH);
			setButtonDisabled(ACTION_SLIPPERY);
			setButtonDisabled(ACTION_EXPLODE);
			setButtonDisabled(ACTION_PUSH);
			setButtonDisabled(ACTION_PULL);
			
			if (_graphicalState.getSelectedBlob().getRadius() < GameParameters.BLOB_INITIAL_SIZE) {
				setButtonDisabled(ACTION_SPLIT); // can't split if below initial size
			}
			if (_graphicalState.getSelectedBlob() instanceof DeathRayBlob) {
				if (_graphicalState.getSelectedBlob().getRadius() == GameParameters.BLOB_SIZE_LIMIT)
					setButtonEnabled(ACTION_DEATH); // can only fire if at size limit
			}
			else if (_graphicalState.getSelectedBlob() instanceof SlipperyBlob) {
				setButtonEnabled(ACTION_SLIPPERY); // can only fire if at size limit
			}
			else if (_graphicalState.getSelectedBlob() instanceof ExplodingBlob)
				setButtonEnabled(ACTION_EXPLODE);
			else if (_graphicalState.getSelectedBlob() instanceof SlipperyBlob)
				setButtonEnabled(ACTION_SLIPPERY);	
			else if (_graphicalState.getSelectedBlob() instanceof PushBlob)
				setButtonEnabled(ACTION_PUSH);
			else if (_graphicalState.getSelectedBlob() instanceof PullBlob)
				setButtonEnabled(ACTION_PULL);
		}
		
		private Coordinate pointToWorldCoordinate(Point p) {
			return _board.screenToWorld(new Coordinate(p.x, p.y));
		}
		
		public void mouseMoved(MouseEvent e) {
			Blob selectedBlob = _graphicalState.getSelectedBlob();
			Point p = e.getPoint();
			if (selectedBlob == null || !isOnBoard(p)) return;
			Coordinate worldClick = pointToWorldCoordinate(p);
			double cost = ActionPointEngine.getCostOfPhysicalMove(selectedBlob, worldClick);
			_board.setMovementCost(worldClick, cost);
		}
		
		private boolean isOnBoard(Point p) {
			return p.x > 0 && p.x < GuiConstants.BOARD_LENGTH
					&& p.y > 0 && p.y < GuiConstants.BOARD_LENGTH;
		}
		
		/** not needed */
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseClicked(MouseEvent e) {}
		public void mouseDragged(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
	}
    
	/**
	 * Listener for Done and Clear Queue buttons
	 */
	private class MainButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			if (event.getActionCommand().equals("Done")) {
				_graphicalState.setSelectedBlob(null);
				_txtAreaQueue.setText("");
				_turnOver.setTrue();
			}
			else { // clear queue
				_ap = _activePlayer.getActionPoints();
				setAP();
				_blobMoves.clear();
				_blobsToActivate.clear();
				_txtAreaQueue.setText("");
			}
		}
	}
	
	private class BlobAbilitiesButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			// TODO Move as much of this logic as possible into enqueueMove.
			// While doing this I'd like to get rid of _buttonCmds unless 
			// they're doing something cool that I don't see.
			
			String cmd = event.getActionCommand();
			String text = "you've selected to ";
			String message = null;
			double cost = 0.0;
			
			if (_graphicalState.getSelectedBlob() == null) {
				showBlobNotSelectedDialog();
				return;
			}
			if (cmd.equals(_buttonCmds.get(ACTION_SPLIT))) {
				message = "split a blob";
				_selectedAction = ACTION_SPLIT;
				cost = ActionPointEngine.getCostOfSplit(_graphicalState.getSelectedBlob());
			}
			else if (cmd.equals(_buttonCmds.get(ACTION_DEATH))) {
				message = "fire a death ray";
				_selectedAction = ACTION_DEATH;
				cost = ActionPointEngine.getCostOfProjectile(_graphicalState.getSelectedBlob());
			}
			else if (cmd.equals(_buttonCmds.get(ACTION_SLIPPERY))) {
				message = "fire slippery goop";
				_selectedAction = ACTION_SLIPPERY;
				cost = ActionPointEngine.getCostOfProjectile(_graphicalState.getSelectedBlob());
			}
			else if (cmd.equals(_buttonCmds.get(ACTION_EXPLODE))) {
				message = "explode a blob";
				_selectedAction = ACTION_EXPLODE;
				cost = ActionPointEngine.getCostOfProratedAction(_graphicalState.getSelectedBlob());
			}
			else if (cmd.equals(_buttonCmds.get(ACTION_PUSH))) {
				message = "apply a push force";
				_selectedAction = ACTION_PUSH;
				cost = ActionPointEngine.getCostOfProratedAction(_graphicalState.getSelectedBlob());
			}
			else if (cmd.equals(_buttonCmds.get(ACTION_PULL))) {
				message = "apply a pull force";
				_selectedAction = ACTION_PULL;
				cost = ActionPointEngine.getCostOfProratedAction(_graphicalState.getSelectedBlob());
			}
			
			if (_ap - cost <= 0.0) {
				addChatLine("You do not have enough action points to " + message);
			} else {
				_blobsToActivate.put(_graphicalState.getSelectedBlob(), moveTypeFor(_selectedAction));
				
				addQueueLine(_buttonCmds.get(_selectedAction) + " with cost of " + cost);
				debug(text+=message);
				debug("Queueing action " + _buttonCmds.get(_selectedAction)+ " for blob " + _graphicalState.getSelectedBlob());
				_ap -= cost;
				setAP();
			}
		}

		private MOVE_TYPE moveTypeFor(int selectedAction) {
			switch (selectedAction) {
			case ACTION_DEATH:
			case ACTION_EXPLODE:
			case ACTION_PULL:
			case ACTION_PUSH:
			case ACTION_SLIPPERY:
				return MOVE_TYPE.ACTIVATE;
			case ACTION_SPLIT:
				return MOVE_TYPE.SPAWN;
			default:
				throw new RuntimeException("Unknown");
			}
		}

		private void showBlobNotSelectedDialog() {
			JOptionPane.showMessageDialog(null, "You must select a blob before clicking on an action", 
					"No blob selected",
					JOptionPane.INFORMATION_MESSAGE );
		}
	}
	
	private void setupButtons() {
		_buttonCmds.add("Move");
		_buttons.add(new JButton(_buttonCmds.get(ACTION_MOVE)));
		_buttonCmds.add("Split");
		_buttons.add(new JButton(_buttonCmds.get(ACTION_SPLIT)));
		_buttonCmds.add("Fire");
		_buttons.add(new JButton(_buttonCmds.get(ACTION_DEATH)));
		_buttonCmds.add("Slippery");
		_buttons.add(new JButton(_buttonCmds.get(ACTION_SLIPPERY)));
		_buttonCmds.add("Explode");
		_buttons.add(new JButton(_buttonCmds.get(ACTION_EXPLODE)));
		_buttonCmds.add("Push");
		_buttons.add(new JButton(_buttonCmds.get(ACTION_PUSH)));
		_buttonCmds.add("Pull");
		_buttons.add(new JButton(_buttonCmds.get(ACTION_PULL)));
	}
	
	public boolean isLocalPlayer(Player player) {
		for (Player p : _localPlayers) {
			if (player.equals(p)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Enqueues a move for the selected blob. Does nothing if no blob is selected.
	 * @param destination Meaning depends on move type.
	 */
	public void enqueueMove(String moveType, Coordinate destination) {
		debug("enqueueMove called: " + moveType);
		Blob selectedBlob = _graphicalState.getSelectedBlob();
		double cost = -1;
		if (selectedBlob == null) return;
		if (moveType == "movement") {			
			cost = ActionPointEngine.getCostOfPhysicalMove(selectedBlob, destination);
			if (_ap - cost <= 0.0) {
				//showNotEnoughAPDialog("move this blob");
				return;
			} else {
				_blobMoves.put(selectedBlob, destination);

				DecimalFormat fmt = new DecimalFormat();
				fmt.setMinimumFractionDigits(1);
				fmt.setMaximumFractionDigits(1);
				addQueueLine("Moving blob to " + destination.toRoundedString() + " with cost of " + fmt.format(cost));
			}
		} else {
			throw new RuntimeException("enqueueMove called with invalid move type");
		}
		debug("Queueing action " + _buttonCmds.get(_selectedAction)+ 
				" for blob " + selectedBlob + " to " + destination.toString());
		assert(cost > 0);
		_ap -= cost;
		setAP();
	}

	public GUIGameMove getMoveFor(Player activePlayer) {
		addChatLine("It's " + activePlayer + "'s turn.");
		_ap = activePlayer.getActionPoints();
		setAP();
		_turnOver.setFalse();
		_blobMoves.clear();
		_blobsToActivate.clear();
		_graphicalState.setSelectedBlob(null);
		_board.setMovementCost(null, 0);
		updateActionsBorder(null);
		_activePlayer = activePlayer;
		_turnOver.waitUntilTrue();
		_activePlayer = null;

		return new GUIGameMove(_blobMoves, _blobsToActivate);
	}

	public void addChatLine(String line) {
		_txtAreaChat.setText(_txtAreaChat.getText() + line + "\n");
	}

	private void addQueueLine(String str) {
		_txtAreaQueue.setText(_txtAreaQueue.getText()+str+"\n");
	}
	
	private void setButtonEnabled(int button) {
		_buttons.get(button).setEnabled(true);
		_buttons.get(button).setFont(GuiConstants.BUTTON_ENABLED_FONT);
		Blob blob = _graphicalState.getSelectedBlob();
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(1);
		df.setMinimumFractionDigits(1);
		if (button == ACTION_DEATH) {
			_buttons.get(button).setText("Fire ("+df.format(ActionPointEngine.getCostOfProjectile(blob))+")");
		} else if (button == ACTION_EXPLODE) {
			_buttons.get(button).setText("Explode ("+df.format(ActionPointEngine.getCostOfProratedAction(blob))+")");
		} else if (button == ACTION_PUSH) {
			_buttons.get(button).setText("Push ("+df.format(ActionPointEngine.getCostOfProratedAction(blob))+")");
		} else if (button == ACTION_PULL) {
			_buttons.get(button).setText("Pull ("+df.format(ActionPointEngine.getCostOfProratedAction(blob))+")");
		} else if (button == ACTION_SLIPPERY) {
			_buttons.get(button).setText("Slippery ("+df.format(ActionPointEngine.getCostOfProjectile(blob))+")");
		} else if (button == ACTION_SPLIT) {
			_buttons.get(button).setText("Split ("+df.format(ActionPointEngine.getCostOfSplit(blob))+")");
		}
	}
	
	private void setButtonDisabled(int button) {
		_buttons.get(button).setEnabled(false);
		_buttons.get(button).setFont(GuiConstants.CHAT_FONT);
		_buttons.get(button).setText(_buttonCmds.get(button));
	}

	private class TextureListener implements MouseListener
    {
		public void mouseClicked(MouseEvent e) {
			try {
				_board.setBackground(new URL(e.getComponent().getName()));
			} catch (MalformedURLException e1) {
				System.out.println("Error creating url for loading image from label name.");
				e1.printStackTrace();
			}
		}
		
		/** not needed */
		public void mouseEntered(MouseEvent arg0) {}
		public void mouseExited(MouseEvent arg0) {}
		public void mousePressed(MouseEvent arg0) {}
		public void mouseReleased(MouseEvent arg0) {}
    }
	
	private void debug(String string) {
		System.out.println(string);
	}

}
