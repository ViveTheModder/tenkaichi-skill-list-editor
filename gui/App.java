package gui;
//Tenkaichi Skill List Editor by ViveTheModder
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import cmd.Main;

public class App 
{
	public static JLabel fileLabel, fileCntLabel;
	private static boolean bt2Sel;
	private static double seconds=0;
	private static File currFolder,lastFolder;
	private static final Font BOLD = new Font("Tahoma", 1, 24);
	private static final Font BOLD_S = new Font("Tahoma", 1, 14);
	private static final Font MED = new Font("Tahoma", 0, 18);
	private static final String HTML_A_START = "<html><a href=''>";
	private static final String HTML_A_END = "</a></html>";
	private static final String WINDOW_TITLE = "Tenkaichi Skill List Editor v1.3";
	private static final Toolkit DEF_TOOLKIT = Toolkit.getDefaultToolkit();

	private static File getFolderFromFileChooser()
	{
		File folder=null;
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Select Folder with PAK Files...");
		if (lastFolder!=null) chooser.setCurrentDirectory(lastFolder);
		while (folder==null)
		{
			int result = chooser.showOpenDialog(chooser);
			if (result==0)
			{
				folder = chooser.getSelectedFile();
				lastFolder=folder;
			}
			else break;
		}
		return folder;
	}
	private static void errorBeep()
	{
		Runnable runWinErrorSnd = (Runnable) DEF_TOOLKIT.getDesktopProperty("win.sound.exclamation");
		if (runWinErrorSnd!=null) runWinErrorSnd.run();
	}
	private static void setApp()
	{
		String[] text = {"String to Find:","String to Replace:","Budokai Tenkaichi 2","Budokai Tenkaichi 3"};
		//initialize components
		Box btnBox = Box.createHorizontalBox();
		ButtonGroup gameBtnGrp = new ButtonGroup();
		ButtonGroup sklLstGrp = new ButtonGroup();
		Dimension textFieldSize = new Dimension(256,48);
		GridBagConstraints gbc = new GridBagConstraints();
		JButton apply = new JButton("Apply Changes");
		JCheckBox replaceCheck = new JCheckBox("Replace Once");
		JFrame frame = new JFrame(WINDOW_TITLE);
		JLabel emptyLblForBtn = new JLabel(" ");
		JLabel[] labels = new JLabel[4];
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu helpMenu = new JMenu("Help");
		JMenuItem about = new JMenuItem("About");
		JMenuItem select = new JMenuItem("Select Folder with PAK Files...");
		JPanel panel = new JPanel(new GridBagLayout());
		JRadioButton[] gameBtns = new JRadioButton[2];
		JRadioButton[] sklLstBtns = new JRadioButton[Main.BT3_SKL_LST_LANGS.length];
		JTextField[] textFields = new JTextField[2];
		//set component properties
		apply.setFont(MED);
		apply.setToolTipText("Quotation marks will be excluded from the string to find.");
		select.setToolTipText("Changes will be applied recursively, meaning that subfolders will also be detected in the chosen folder.");
		replaceCheck.setFont(BOLD_S);
		replaceCheck.setToolTipText("If selected, only the first instance of the string to find will be replaced.");
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		for (int i=0; i<2; i++)
		{
			labels[i] = new JLabel(text[i]);
			labels[i].setFont(BOLD);
			labels[i].setToolTipText("String is case sensitive.");
			textFields[i] = new JTextField();
			textFields[i].setFont(MED);
			textFields[i].setHorizontalAlignment(JTextField.CENTER);
			textFields[i].setMinimumSize(textFieldSize);
			textFields[i].setMaximumSize(textFieldSize);
			textFields[i].setPreferredSize(textFieldSize);
			panel.add(labels[i],gbc);
			panel.add(textFields[i],gbc);
			if (i==1) panel.add(replaceCheck,gbc);
			panel.add(new JLabel(" "),gbc);
		}
		labels[2] = new JLabel("Game Version:");
		labels[2].setFont(BOLD);
		panel.add(labels[2],gbc);
		for (int i=0; i<2; i++)
		{
			final int index=i;
			gameBtns[i] = new JRadioButton(text[i+2]);
			if (i==1) gameBtns[i].setSelected(true); 
			gameBtns[i].setFont(MED);
			gameBtnGrp.add(gameBtns[i]);
			gameBtns[i].addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e) 
				{
					sklLstBtns[0].setSelected(true);
					for (int i=0; i<sklLstBtns.length; i++)
					{
						if (index==0)
						{
							bt2Sel=true;
							if (i<Main.BT2_SKL_LST_LANGS.length) 
							{
								sklLstBtns[i].setText(Main.BT2_SKL_LST_LANGS[i]);
								if (i==Main.BT2_SKL_LST_LANGS.length-1)
								{
									panel.remove(emptyLblForBtn);
									panel.remove(apply);
									panel.add(emptyLblForBtn,gbc);
									panel.add(apply,gbc);
									panel.updateUI();
								}
							}
							else
							{
								panel.remove(sklLstBtns[i]);
								panel.updateUI();
							}
						}
						else
						{
							sklLstBtns[i].setText(Main.BT3_SKL_LST_LANGS[i]);
							if (bt2Sel==true)
							{
								bt2Sel=false;
								panel.add(sklLstBtns[8],gbc);
								panel.add(emptyLblForBtn,gbc);
								panel.add(apply,gbc);
								panel.updateUI();
							}
						}
					}
				}
			});
			btnBox.add(gameBtns[i]);
		}
		panel.add(btnBox,gbc);
		panel.add(new JLabel(" "),gbc);
		labels[3] = new JLabel("Skill List to Edit:");
		labels[3].setFont(BOLD);
		panel.add(labels[3],gbc);
		for (int i=0; i<sklLstBtns.length; i++)
		{
			sklLstBtns[i] = new JRadioButton(Main.BT3_SKL_LST_LANGS[i]);
			if (i==0) sklLstBtns[i].setSelected(true);
			sklLstBtns[i].setFont(MED);
			sklLstGrp.add(sklLstBtns[i]);
			panel.add(sklLstBtns[i],gbc);
		}
		//add action listeners
		about.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				Box horBox = Box.createHorizontalBox();
				JLabel label = new JLabel("Made by: ");
				JLabel author = new JLabel(HTML_A_START+"ViveTheModder"+HTML_A_END);
				author.addMouseListener(new MouseAdapter() 
				{
					@Override
					public void mouseClicked(MouseEvent e) 
					{
						try 
						{
							Desktop.getDesktop().browse(new URI("https://github.com/ViveTheModder"));
						} 
						catch (IOException | URISyntaxException e1) 
						{
							errorBeep();
							JOptionPane.showMessageDialog(frame, e1.getClass().getSimpleName()+": "+e1.getMessage(), "Exception", 0);
						}
					}});
				horBox.add(label);
				horBox.add(author);
				JOptionPane.showMessageDialog(null, horBox, WINDOW_TITLE, 1);
			}
		});
		apply.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				if (currFolder!=null)
				{
					try 
					{
						int sklLstIndex=0;
						for (int i=0; i<sklLstBtns.length; i++)
						{
							if (sklLstBtns[i].isSelected()) 
							{
								sklLstIndex=i;
								break;
							}
						}
						String in = textFields[0].getText().replace("\"", "");
						String out = textFields[1].getText().replace("\"", "");
						if (in.equals(""))
						{
							errorBeep();
							JOptionPane.showMessageDialog(null, "No string to find has been specified!", WINDOW_TITLE, 0);
						}
						else if (in.equals(out))
						{
							errorBeep();
							JOptionPane.showMessageDialog(null, "The strings to find and replace are identical!", WINDOW_TITLE, 0);
						}
						else 
						{
							Main.replaceOnce = replaceCheck.isSelected();
							setProgress(frame, sklLstIndex, in, out);
						}
					} 
					catch (IOException e1) 
					{
						errorBeep();
						JOptionPane.showMessageDialog(null, e1.getClass().getSimpleName()+": "+e1.getMessage(), "Exception", 0);	
					}
				}
				else
				{
					errorBeep();
					JOptionPane.showMessageDialog(null, "No folder has been selected!", WINDOW_TITLE, 0);
				}
			}
		});
		select.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				currFolder = getFolderFromFileChooser();
				if (currFolder!=null) frame.setTitle(WINDOW_TITLE+" - "+currFolder.getAbsolutePath());
				else frame.setTitle(WINDOW_TITLE);
			}
		});
		//add components
		fileMenu.add(select);
		helpMenu.add(about);
		menuBar.add(fileMenu);
		menuBar.add(helpMenu);
		panel.add(emptyLblForBtn,gbc);
		panel.add(apply,gbc);
		frame.add(panel);
		//set frame components
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setJMenuBar(menuBar);
		frame.setSize(512,768);
		frame.setTitle(WINDOW_TITLE);
		frame.setVisible(true);
	}
	private static void setProgress(JFrame frame, int sklLstIndex, String in, String out) throws IOException
	{
		Main.fileCnt=0;
		seconds=0;
		//initialize components
		JDialog loading = new JDialog();
		JPanel panel = new JPanel();
		JLabel[] labels = {new JLabel("Working on:"),new JLabel("Time elapsed:")};
		fileLabel = new JLabel("(File Not Found)"); fileCntLabel = new JLabel("Overwritten Costumes: 0");
		JLabel timeLabel = new JLabel();
		GridBagConstraints gbc = new GridBagConstraints();
		Timer timer = new Timer(100, e -> 
		{
			seconds+=0.1;
			timeLabel.setText((int)(seconds/3600)+"h"+(int)(seconds/60)%60+"m"+String.format("%.1f",seconds%60)+"s");
		});
		timer.start();
		//set component properties
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		fileLabel.setHorizontalAlignment(SwingConstants.CENTER);
		fileCntLabel.setHorizontalAlignment(SwingConstants.CENTER);
		labels[0].setHorizontalAlignment(SwingConstants.CENTER);
		labels[1].setHorizontalAlignment(SwingConstants.CENTER);
		timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
		fileLabel.setFont(MED);
		fileCntLabel.setFont(MED);
		labels[0].setFont(BOLD);
		labels[1].setFont(BOLD);
		timeLabel.setFont(MED);
		panel.setLayout(new GridBagLayout());
		//add components
		panel.add(labels[0],gbc);
		panel.add(fileLabel,gbc);
		panel.add(new JLabel(" "),gbc);
		panel.add(labels[1],gbc);
		panel.add(timeLabel,gbc);
		panel.add(new JLabel(" "),gbc);
		panel.add(fileCntLabel,gbc);
		//add window listener
		loading.addWindowListener(new WindowAdapter()
		{
			@Override
            public void windowClosed(WindowEvent e) 
			{
				frame.setEnabled(true);
				timer.stop();
            }
		});
		loading.add(panel);
		loading.setTitle(WINDOW_TITLE+" - Progress Report");
		loading.setSize(1024,256);
		loading.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		loading.setLocationRelativeTo(null);
		loading.setVisible(true);
		//initialize worker
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground() throws Exception 
			{
				long start = System.currentTimeMillis();
				frame.setEnabled(false);
				Main.traverse(currFolder, sklLstIndex, in, out);
				long finish = System.currentTimeMillis();
				double time = (finish-start)/1000.0;
				loading.setVisible(false); 
				loading.dispose();
				if (Main.fileCnt>0)
				{
					DEF_TOOLKIT.beep();
					JOptionPane.showMessageDialog(null, Main.fileCnt+" character costume files "
					+ "overwritten successfully in "+time+" seconds!", WINDOW_TITLE, 1);
				}
				else
				{
					errorBeep();
					JOptionPane.showMessageDialog(null, "No character costume files "
					+ "found or changed!\nIf the PAKs are actually present, check if the string\n"
					+ "to find and/or the affected Skill List is also present.", WINDOW_TITLE, 0);
				}
				frame.setEnabled(true);
				timer.stop();
				return null;
			}
		};
		worker.execute();
	}
	public static void main(String[] args) 
	{
		try 
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			setApp();
		} 
		catch (Exception e) 
		{
			errorBeep();
			JOptionPane.showMessageDialog(null, e.getClass().getSimpleName()+": "+e.getMessage(), "Exception", 0);		
		}
	}
}