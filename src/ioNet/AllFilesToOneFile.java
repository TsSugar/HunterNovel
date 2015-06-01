package ioNet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/** 
 * 多文件合并<br>
 * 功能：把一个目录下的全部文件合并为一个文件<br>
 * 
 * 描述：打开一个几十K的文件当然要比打开一个几兆的文件要省内存啦，<br>
 * 当然了，如果你的设备够给力打开一个大文件之后就不用重复地再打开文件了，也少去了很多打开的历史记录。
 */
public class AllFilesToOneFile {
	
	public static void main(String[] args) 
	{
		String folderStr = "D:\\books\\读书频道\\格林童话\\";
		File folder = new File(folderStr);
		File[] list = folder.listFiles();
		
		File all = new File(folderStr+"\\"+"000_"+folder.getName()+"_(全文).txt");
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(all);
			String startHead = "********************\r\n";
			appendFile("格林童话",fos);
			int i = 1;
			for (File f : list)
			{
				if (Integer.valueOf(f.getName().substring(0, 3)) != i) // 顺序不正确！
				{
					System.err.println(f.getName()+"\t顺序不正确错误！！！");
					break;
				}
				else
				{
					i++;
				}
				appendFile("\r\n",fos);
				appendFile(startHead,fos);
				appendFile("* "+f.getName()+"\r\n",fos);
				
				appendFile(startHead,fos);
				
				appendFile(f,fos);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fos != null) try {fos.close();} catch (Exception e) {e.printStackTrace();} 
		}
	}
	

	
	/** 
	 * 一个读，输入流，由方法内部关闭，
	 * 一个写，输出流，由方法外部关闭。 */
	public static void appendFile(File fromFile, FileOutputStream os) {
		try {
			FileInputStream is = new FileInputStream(fromFile);
			//FileOutputStream os = new FileOutputStream(destPath);
			byte[] buffer = new byte[4096];
			while (is.available() > 0) {
				int n = is.read(buffer);
				os.write(buffer, 0, n);
				os.flush();
			}
			is.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Write StringContent to File OutputStream, . */
	public static boolean appendFile(String appendContent, FileOutputStream os) { 
		try { 
			byte[] buffer = appendContent.getBytes("UTF-8");
			os.write(buffer/*, 0, buffer.length*/);
			os.flush();
		}  catch (Exception e) { 
			e.printStackTrace(); 
			return false;
		} 
		return true; 
	} 

}
