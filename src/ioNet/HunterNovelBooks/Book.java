package ioNet.HunterNovelBooks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class Book {
	/** 书保存路径(不含书名) */
	public String saveDir = "D:\\books\\读书频道\\";
	
	/** 本书根目录 */
	public String url;
	/** 书名 */
	public String title;
	
	/** 如果章节非常多或命名不规范，在章节名称前面加上001, 002, ... */
	public boolean addHeadNum = true;
	/** 解析到第几章了 */
	public int sectionIndex = 1;

	/** 所有章节，开始与结束标志 */
	private String[] sectionStart = {"<div class=\"title_list\"><ul>"};;
	private String[] sectionEnd = {"</ul>"};
	
	/** 章节内容开始与结束标签 */
	private String[] contextStart = {"<div id=\"text\">"};;
	private String[] contextEnd = {"</div>"};
	
	/** 删除哪些字符串(如果有广告等内容，你可以放在这里把它删掉) */
	private String[] deleteStr = {};
	/** 替换哪些字符串(HTML 标签要转成换行才好看) */
	private String[] replaceStr = {"</p>", 		"\r\n", 
									"</span>", 	"\r\n",
									"<br />", 	"\r\n",
									"<br>",		"\r\n"};
	
	/** 章节也是分面显示的，如共100章节可分4页显示，每页仅显示了25章 */
	public int sectionPartsCount = 1;

	
	/** 抓取所有章节及内容 */
	public void hunt() {
		long startTime = System.currentTimeMillis();
		// 创建保存此书的目录结构
		File file = new File(saveDir + title);
		file.mkdirs();
		
		try {
			// 取所有的章节
			String strSections_ = getBookData(this.url, "gb2312", sectionStart, sectionEnd);
			// 抓取所有章节的具体内容
			huntAllSections(strSections_);
			
			// 章节还被分页了，再分别抓取后面每页的内容（如每页显示50章，共150章则分3页显示）
			for (int sectionPartIndex = 2; sectionPartIndex <= sectionPartsCount; sectionPartIndex++)
			{
				strSections_ = getBookData(this.url + "index_"+sectionPartIndex+".html", "gb2312", sectionStart, sectionEnd);
				huntAllSections(strSections_);
			}
		} catch (Exception e) {
			System.err.println(this.title + " " + this.url + " Hunt Error!");
			e.printStackTrace();
		}
		
		System.out.println("访问网站"+netInvokeCount+"次，总花了"+(System.currentTimeMillis() - startTime) / 1000 + "s");
	}
	
	/** 抓取所有章节的具体内容 */
	private void huntAllSections(String strSections_) {
		String[] strSectionsArr = strSections_.split("</li>");
		for (String strSec : strSectionsArr) {
			
			if (strSec == null || strSec.equals("") || strSec.equals("</ul>"))
				continue;
			
			Section sec = new Section();
			// 某些章节被插入了特殊的内容，如视频等，这些资源就跳过了不抓了。
			int beginIndex = strSec.indexOf("<a href=\"");
			if (beginIndex == -1)
			{
				System.err.println(strSec+" Passed.");
				continue;
			}
			
			// 取章节URL
			beginIndex += "<a href=\"".length();
			int endIndex = strSec.indexOf("\" ", beginIndex);
			sec.url = strSec.substring(beginIndex, endIndex);
			
			// 取章节Title
			beginIndex = strSec.indexOf("\">", beginIndex) + 2;
			endIndex = strSec.indexOf("</a>", beginIndex);
			sec.title = strSec.substring(beginIndex, endIndex);

			// 如果是相对路径就改为全路径
			if (sec.url.startsWith("/"))
				sec.url = AllBooks.BASE_URL + sec.url;
			else
				System.err.println(sec.url);
			
			if (addHeadNum)
			{
				if (sectionIndex < 10)
					sec.title = "00"+sectionIndex+"_"+sec.title;
				else if (sectionIndex < 100)
					sec.title = "0"+sectionIndex+"_"+sec.title;
				else 
					sec.title = sectionIndex+"_"+sec.title;
			}
			
			// 替换特殊字符，下面这些字符是不能用作文件名的要替换掉，否则会报错的
			while (sec.title.indexOf("\\") != -1)
				sec.title = sec.title.replace("\\", "、");
			while (sec.title.indexOf("/") != -1)
				sec.title = sec.title.replace("/", "、");
			while (sec.title.indexOf(":") != -1)
				sec.title = sec.title.replace(":", "：");
			while (sec.title.indexOf("*") != -1)
				sec.title = sec.title.replace("*", "＊");
			while (sec.title.indexOf("?") != -1)
				sec.title = sec.title.replace("?", "？");
			while (sec.title.indexOf("\"") != -1)
				sec.title = sec.title.replace("\"", "“");
			while (sec.title.indexOf("<") != -1)
				sec.title = sec.title.replace("<", "〈");
			while (sec.title.indexOf(">") != -1)
				sec.title = sec.title.replace(">", "〉");
			while (sec.title.indexOf("|") != -1)
				sec.title = sec.title.replace("|", "｜");
			
			// 取此章节的内容
			huntSection(sec);
			sectionIndex++;
		}
	}

	/** 取此章节的内容 */
	private void huntSection(Section sec) {
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		String fileName = saveDir + title + File.separator+ sec.title + ".txt";
		try {
			// 每一章节的内容保存为一个TXT文件
			fos = new FileOutputStream(fileName); 
			osw = new OutputStreamWriter(fos, "utf-8"); 
			
			// 取此章节第一页的内容，并写入文件
			String strContext_ = getSectionContextData(sec.url, "gb2312", contextStart, contextEnd, sec);
			osw.write(strContext_); 
			osw.flush();
			
			// 此章节有多页，如第1章有100页，上面只取了第一页，继续取后面的99页
			for (int pageIndex = 2; pageIndex <= sec.pageCount; pageIndex++)
			{
				// 拼下一页的URL
				String url = sec.url.substring(0,sec.url.lastIndexOf("."));
				url += "_" + pageIndex + ".html";
				
				// 取此面内容，并“追加”写入到文件
				strContext_ = getSectionContextData(url, "gb2312", contextStart, contextEnd, null);
				osw.write(strContext_); 
				osw.flush();
			}
			System.out.println("Successed --->  "+fileName);
		} catch (Exception e) {
			// 出错分析
			try {
				osw.write(this.title + " >>>> " + sec.title + "("+sec.url+")" + " Hunt Error!"); 
				osw.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.err.println(this.title + " >>>> " + sec.title + "("+sec.url+")" + " Hunt Error!");
			e.printStackTrace();
		} finally {
			// 最后要把这个文件关了，不要不关闭系统资源
			if (osw != null) try {osw.close(); } catch (Exception e) {e.printStackTrace();}
			if (fos != null) try {fos.close(); } catch (Exception e) {e.printStackTrace();}
		}
	}

	/** 抓取页面所有内容，即页面上“查看源码”所看到的文字 */
	public String getBookData(String URL,String charset,String[] contextStart,String[] contextEnd) throws Exception {
		URL url = new URL(URL);
		StringBuffer sb = new StringBuffer(30000);
		HttpURLConnection connection = null;
		BufferedReader br = null;
		InputStreamReader isr = null;
		InputStream is = null;
		try {
			connection = (HttpURLConnection)url.openConnection();
			connection.setDoInput(true);
			connection.setUseCaches(false);
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(30000);
			connection.connect();
			is =connection.getInputStream();
			isr = new InputStreamReader(is, charset);
			br = new BufferedReader(isr);
			String line = null;
			boolean startRead = false;
			
			while ((line = br.readLine()) != null) {
				// System.out.println(line);
				if (line.equals("")) {
					continue;
				}
				if (!startRead) {
					if (contextStart != null) {
						for (String s : contextStart) {
							if (line.indexOf(s) != -1) {
								line = line.substring(line.indexOf(s));
								startRead = true;
								break;
							}
						}
					}
				}
				if (startRead) {
					while(line.indexOf("		") != -1)
					line = line.replaceAll("		", " ");
					while(line.indexOf("	") != -1)
					line = line.replaceAll("	", " ");
					while(line.indexOf("  ") != -1)
						line = line.replaceAll("  ", " ");
//					line = line.replaceAll(" : ", ":");
//					line = line.replaceAll(" }", "}");
//					line = line.replaceAll(" ]", "]");
					
					sb.append(line);
					
					for (String s : contextEnd) { // 已经到结束
						if (line.indexOf(s) != -1) {
							startRead = false;
							break;
						}
					}
					if (!startRead)
						break;
				}
			}
			
			// 分析总共有多少页，如果没有网站没有分页，你可以删除此段代码
			while ((line = br.readLine()) != null) {
				// 页次：1/4&nbsp;
				if (line.indexOf("页次：") != -1) {
					
					int beginIndex = line.indexOf("页次：");
					beginIndex = line.indexOf("/",beginIndex) + 1;
					int endIndex = line.indexOf("&nbsp;", beginIndex);
					
					String parts = line.substring(beginIndex, endIndex);
					this.sectionPartsCount = Integer.valueOf(parts);
					
					break;
				}
			}
			haveARest();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (br != null) {
				is.close();
				isr.close();
				br.close();
			}
			if (connection != null)
				connection.disconnect();
		}
		return sb.toString();
	}

	/** 抓取此章节页面所有内容，即页面上“查看源码”所看到的文字 */
	public String getSectionContextData(String URL,String charset,String[] contextStart,String[] contextEnd, Section sec) throws Exception {
		URL url = new URL(URL);
		StringBuffer sb = new StringBuffer(30000);
		HttpURLConnection connection = null;
		BufferedReader br = null;
		InputStreamReader isr = null;
		InputStream is = null;
		try {
			connection = (HttpURLConnection)url.openConnection();
			connection.setDoInput(true);
			connection.setUseCaches(false);
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(30000);
			connection.connect();
			is =connection.getInputStream();
			isr = new InputStreamReader(is, charset);
			br = new BufferedReader(isr);
			String line = null;
			boolean startRead = false;
			
			while ((line = br.readLine()) != null) {
				// System.out.println(line);
				if (line.equals("")) {
					continue;
				}
				if (!startRead) {
					if (contextStart != null) {
						for (String s : contextStart) {
							if (line.indexOf(s) != -1) {
								line = line.substring(line.indexOf(s));
								startRead = true;
								break;
							}
						}
					}
				}
				if (startRead) {
					while(line.indexOf("		") != -1)
						line = line.replaceAll("		", " ");
					while(line.indexOf("	") != -1)
						line = line.replaceAll("	", " ");
					while(line.indexOf("  ") != -1)
						line = line.replaceAll("  ", " ");

					while (line.indexOf("&gt;") != -1)
						line = line.replace("&gt;", ">");
					while (line.indexOf("&lt;") != -1)
						line = line.replace("&lt;", "<");
					
					while (line.indexOf("&ldquo;") != -1)
						line = line.replace("&ldquo;", "“");
					while (line.indexOf("&rdquo;") != -1)
						line = line.replace("&rdquo;", "”");
					
					while (line.indexOf("&quot;") != -1)
						line = line.replace("&quot;", "“");
					while (line.indexOf("&lsquo;") != -1)
						line = line.replace("&lsquo;", "‘");
					while (line.indexOf("&rsquo;") != -1)
						line = line.replace("&rsquo;", "’");
					
					while (line.indexOf("&mdash;") != -1)
						line = line.replace("&mdash;", "—");
					while (line.indexOf("&hellip;") != -1)
						line = line.replace("&hellip;", "…");
					
					while (line.indexOf("&nbsp;") != -1)
						line = line.replace("&nbsp;", " ");
					while (line.indexOf("  ") != -1)
						line = line.replace("  ", " ");
					
					// 删除对称的标签
					line = deleteTag(line, "img");
					line = deleteTag(line, "span");
					line = deleteTag(line, "p");
					line = deleteTag(line, "div");
					line = deleteTag(line, "strong");
					
					for (String del : deleteStr)
					{
						while(line.indexOf(del) != -1)
							line = line.replaceAll(del, "");
					}
					
					for (int old=0; old < replaceStr.length - 1; old += 2)
					{
						while(line.indexOf(replaceStr[old]) != -1)
							line = line.replaceAll(replaceStr[old], replaceStr[old+1]);
					}
					
//					line = line.replaceAll(" : ", ":");
//					line = line.replaceAll(" }", "}");
//					line = line.replaceAll(" ]", "]");
					
					sb.append(line);
					
					
					for (String s : contextEnd) { // 已经到结束
						if (line.indexOf(s) != -1) {
							startRead = false;
							
							int start = sb.indexOf(s);
							int end = start + s.length();
							sb.delete(start, end);
							break;
						}
					}
					if (!startRead)
						break;
				}
			}
			
			if (sec != null)
			{
				// 分析总共有多少页，如果没有网站没有分页，你可以删除此段代码
				while ((line = br.readLine()) != null) {
					// <a title="Page"><a title="Page">&nbsp;<b>1</b>"/"<b>7</b> </a>&nbsp;&nbsp;&nbsp;
					int temp = line.indexOf("<a title=\"Page\">");
					if (temp != -1) {
						if (line.indexOf("<a title=\"Page\"></div>") != -1) // No Page
							break;
						
						int beginIndex = line.indexOf("<a title=\"Page\">");
						beginIndex = line.indexOf("<b>",beginIndex) + 1;
						beginIndex = line.indexOf("<b>",beginIndex) + 3;
						
						int endIndex = line.indexOf("</b>", beginIndex);
						
						if (beginIndex < 0 || endIndex < 0)
						{
							System.err.println("beginIndex:"+beginIndex+" endIndex:"+endIndex);
						}
						String parts = line.substring(beginIndex, endIndex);
						sec.pageCount = Integer.valueOf(parts);
						
						break;
					}
				}
			}
			haveARest();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (br != null) {
				is.close();
				isr.close();
				br.close();
			}
			if (connection != null)
				connection.disconnect();
		}
		return sb.toString();
	}

	/** 删除对称的HTML标签级内容*/
	private String deleteTag(String line, String string) {
		while(true) {
			int start = line.indexOf("<"+string);
			if (start == -1)
				break;
			
			int end = line.indexOf(">", start);
			if (end == -1)
				break;
			
			String del = line.substring(start, end+1);
			line = line.replace(del, "");
		}
		return line;
	}

	/** 
	 * 更加道德的做法是每一次网络请求后，都加一个休息时间，如3秒及以上，<br>
	 * 不然别人做得好一点的网站加点逻辑控制，如1分钟访问几百次以上就把你的IP加入黑名单，<br>
	 * 你不换IP都访问不了他的网站了！<br>
	 * 当然了这个网站暂时还没有这个限制，你就噼里啪啦花个13来秒钟访问它个472次，把你的书“拿”下来了就行了。
	 */
	int netInvokeCount = 0;
	private void haveARest() {
		netInvokeCount ++;
		/*
		// 让程序暂停多少毫秒(1秒 = 1000毫秒)
		long sleepTime = 300L;
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
}
