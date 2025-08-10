package cmd;
//Tenkaichi Skill List Editor by ViveTheModder
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import gui.App;

public class Main 
{
	public static boolean bt2Mode, gui, replaceOnce;
	public static int fileCnt=0;
	public static final String[] BT2_SKL_LST_LANGS = 
	{"German","French","Italian","Japanese","Korean (Unused)","Spanish","British English","American English"};
	public static final String[] BT3_SKL_LST_LANGS = 
	{"Japanese","American English","Spanish (Unused)","British English","Spanish","German","French","Italian","Korean (Unused)"};
	public static final String HELP = 
	"[Tenkaichi Skill List Editor by ViveTheModder]\n"
	+ "Usage: java -jar bt-skl-lst-editor.jar [arg1] [arg2] [arg3]\n"
	+ "* arg1: Skill List ID;\n"
	+ "* arg2: Input Text;\n"
	+ "* arg3: Output Text.\n";
	
	public static boolean isCharaCostumePak(RandomAccessFile pak) throws IOException
	{
		int numPakContents = LittleEndian.getInt(pak.readInt());
		if (numPakContents<0 && numPakContents>252) return false;
		if (numPakContents==250) bt2Mode=true;
		else if (numPakContents==252) bt2Mode=false;
		pak.seek((numPakContents+1)*4);
		int fileSize = LittleEndian.getInt(pak.readInt());
		int actualFileSize = (int)pak.length();
		if (fileSize==actualFileSize) return true;
		return false;
	}
	public static byte[] getSkillList(RandomAccessFile pak, int sklLstId) throws IOException
	{
		int start=43;
		if (bt2Mode) start=40;
		pak.seek((sklLstId+start+1)*4);
		int selStart = LittleEndian.getInt(pak.readInt());
		int size = LittleEndian.getInt(pak.readInt())-selStart;
		if (size>0)
		{
			pak.seek(selStart);
			byte[] sklLst = new byte[size];
			pak.read(sklLst);
			return sklLst;
		}
		return null;
	}
	public static byte[] getUpdatedSkillList(byte[] sklLst, String in, String out) throws IOException
	{
		if (sklLst==null) return null;
		byte[] inBytes = in.getBytes(StandardCharsets.UTF_16LE);
		byte[] outBytes = out.getBytes(StandardCharsets.UTF_16LE);
 		int inLen=inBytes.length, outLen=outBytes.length, diff=inLen-outLen;
 		int fileSize = sklLst.length, replacements=0, newFileSize = fileSize-diff;
		byte[] searchBytes = new byte[inLen];
		//make temporary file to dump byte array contents
		RandomAccessFile skl = new RandomAccessFile("skl-lst","rw");
		skl.write(sklLst);
		for (int pos=0; pos<fileSize; pos+=2)
		{
			skl.seek(pos);
			if (pos+inLen<=fileSize) skl.read(searchBytes);
			if (Arrays.equals(inBytes, searchBytes))
			{
				replacements++;
				int restOfFileSize = fileSize-(pos+inLen);
				byte[] restOfFile = new byte[restOfFileSize];
				//overwriting process
				if (diff!=0)
				{
					skl.read(restOfFile);
					skl.seek(pos);
					skl.write(outBytes);
					skl.write(restOfFile);
				}
				else
				{
					skl.seek(pos);
					skl.write(outBytes);
				}
				if (replaceOnce) break;
			}
		}
		//if skill list gets smaller, overwrite leftover bytes with zeroes
		if (newFileSize<fileSize)
		{
			skl.seek(newFileSize);
			for (int i=newFileSize; i<fileSize; i++) skl.writeByte(0);
		}
		//remove excess rows of zeroes (16 bytes worth)
		int bytesToRemove=0;
		newFileSize = (int)skl.length();
		for (int pos=newFileSize-16; pos>=(3*newFileSize)/4; pos-=16)
		{
			long footer1,footer2;
			skl.seek(pos);
			footer1 = skl.readLong();
			footer2 = skl.readLong();
			if (footer1==0 && footer2==0) bytesToRemove+=16;
		}
		newFileSize-=bytesToRemove;
		skl.setLength(newFileSize);
		//round file size to the nearest multiple of 16
		if (newFileSize%16!=0) newFileSize = newFileSize+16-(newFileSize%16);
		skl.setLength(newFileSize);

		String nvm="nevermind.";
		if (replacements!=0) nvm="";
		if (!gui) System.out.println(nvm);
		else
		{
			if (nvm.equals("")) 
			{
				fileCnt++;
				App.fileLabel.setForeground(new Color(133,170,5));
			}
			else App.fileLabel.setForeground(new Color(215,6,1));
		}
		
		byte[] newSklLst = new byte[(int)skl.length()];
		skl.seek(0);
		skl.read(newSklLst);
		skl.close();
		new File("skl-lst").delete();
		return newSklLst;
	}
	public static void overwritePak(RandomAccessFile pak, byte[] sklLst, int sklLstId) throws IOException
	{
		if (sklLst==null) return;
		int start=43, numPakContents=252, entryId=sklLstId+start+1;
		if (bt2Mode) 
		{
			start=40;
			numPakContents=250;
		}
		pak.seek(entryId*4);
		int selStart = LittleEndian.getInt(pak.readInt());
		int selEnd = LittleEndian.getInt(pak.readInt());
		int pakSize = (int)pak.length();
		int size = selEnd-selStart, newSize = sklLst.length;
		if (size>0)
		{
			int diff=size-newSize;
			if (diff!=0)
			{
				//fix index
				pak.seek((entryId+1)*4);
				for (int i=0; i<=numPakContents-entryId; i++)
				{
					int offset = LittleEndian.getInt(pak.readInt());
					pak.seek(pak.getFilePointer()-4);
					pak.writeInt(LittleEndian.getInt(offset-diff));
				}
				//overwrite
				pak.seek(selEnd);
				byte[] restOfFile = new byte[pakSize-selEnd];
				pak.read(restOfFile);
				pak.seek(selStart);
				pak.write(sklLst);
				pak.write(restOfFile);
				pak.setLength(pakSize-diff);
			}
			else
			{
				pak.seek(selStart);
				pak.write(sklLst);
			}
		}
		pak.close();
	}
	public static void traverse(File src, int sklLstId, String in, String out) throws IOException
	{
		if (src.isDirectory())
		{
			File[] dirs = src.listFiles();
			if (dirs!=null)
			{
				for (File newSrc: dirs) traverse(newSrc,sklLstId,in,out);
			}
		}
		else if (src.isFile())
		{
			String nameLower = src.getName().toLowerCase();
			String[] nameArr = nameLower.split("_");
			boolean checkForRegCostume=false, checkForDmgCostume=false;
			checkForRegCostume = nameArr[nameArr.length-1].matches("\\dp.pak");
			if (nameArr.length>2) 
				checkForDmgCostume = nameArr[nameArr.length-2].matches("\\dp") && nameArr[nameArr.length-1].equals("dmg.pak");
			if (checkForRegCostume || checkForDmgCostume)
			{
				RandomAccessFile pak = new RandomAccessFile(src,"rw");
				if (isCharaCostumePak(pak))
				{
					if (!gui) System.out.print("Replacing "+"\""+in+"\""+" with "+"\""+out+"\""+" for "+src.getName()+"... ");
					else
					{
						App.fileLabel.setText(src.getAbsolutePath());
						App.fileCntLabel.setText("Overwritten Costumes: "+fileCnt);
					}
					byte[] newSklLst = getUpdatedSkillList(getSkillList(pak,sklLstId),in,out);
					if (newSklLst!=null && newSklLst.length%16==0) overwritePak(pak,newSklLst,sklLstId);
					else
					{
						if (!gui)  System.out.println("Skipping "+src.getName()+" (Reason: Absent Skill List)...");
					}
				}
				else 
				{
					if (!gui) System.out.println("Skipping "+src.getName()+" (Reason: Faulty character costume PAK)...");
				}
			}
		}
	}
	public static void main(String[] args) 
	{
		int sklLstId=0;
		File src=null;
		Scanner sc = new Scanner(System.in);

		if (args.length>0)
		{
			System.out.println(HELP);
			System.out.println("Possible Skill List ID values:\n* Budokai Tenkaichi 2");
			for (int i=0; i<BT2_SKL_LST_LANGS.length; i++) System.out.println(i+". "+BT2_SKL_LST_LANGS[i]);
			System.out.println("* Budokai Tenkaichi 3");
			for (int i=0; i<BT3_SKL_LST_LANGS.length; i++) System.out.println(i+". "+BT3_SKL_LST_LANGS[i]);
			System.out.println();

			if (args.length>=3)
			{
				if (args[0].matches("[0-8]")) sklLstId=Integer.parseInt(args[0]);
				else 
				{
					System.out.println("Not a Skill List ID!");
					System.exit(1);
				}
			}
			else
			{
				if (args[0].equals("-h")) 
				{
					System.out.println(HELP);
					System.out.println("Possible Skill List ID values:\n* Budokai Tenkaichi 2");
					for (int i=0; i<BT2_SKL_LST_LANGS.length; i++) System.out.println(i+". "+BT2_SKL_LST_LANGS[i]);
					System.out.println("* Budokai Tenkaichi 3");
					for (int i=0; i<BT3_SKL_LST_LANGS.length; i++) System.out.println(i+". "+BT3_SKL_LST_LANGS[i]);
					System.out.println();
					System.exit(0);
				}
				else 
				{
					System.out.println("Not enough arguments provided!");
					System.exit(2);
				}
			}
			
			while (src==null)
			{
				System.out.println("Enter a valid path to a folder (with or without subfolders) containing Skill Lists:");
				String path = sc.nextLine();
				File tmp = new File(path);
				if (tmp.isDirectory()) src=tmp;
			}
			sc.close();
			try 
			{
				long start = System.currentTimeMillis();
				traverse(src,sklLstId,args[1].replace("\"", ""),args[2].replace("\"", ""));
				long finish = System.currentTimeMillis();
				System.out.println("Time elapsed: "+(finish-start)/1000.0+" s");
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		else 
		{
			gui=true;
			App.main(args);
		}
	}
}