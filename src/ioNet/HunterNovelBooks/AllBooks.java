package ioNet.HunterNovelBooks;


public class AllBooks {

	/** 程序入口 */
	public static void main(String[] args) {
		handlAllBooks();
	}
	
	/** 网站根目录 */
	public static String BASE_URL = "http://www.cnfla.com";
	
	private static void handlAllBooks() {

		// String allURLs = "<dd style='display: block;'><a href='/dushu/gelintonghua/'>【格林童话】</a></dd><dd style='display: block;'><a href='/dushu/yisuoyuyan/'>【伊索寓言】</a></dd><dd style='display: block;'><a href='/dushu/lishigushi/'>【历史故事】</a></dd><dd style='display: block;'><a href='/dushu/antusheng/'>【安徒生童话】</a></dd><dd style='display: block;'><a href='/dushu/1001/'>【一千零一夜】</a></dd><dd style='display: block;'><a href='/dushu/erge/'>【儿歌】</a></dd><dd style='display: block;'><a href='/dushu/xilashenhua/'>【希腊神话】</a></dd><dd style='display: block;'><a href='/dushu/tonghuayuyan/'>【童话寓言】</a></dd><dd style='display: block;'><a href='/dushu/lizhigushi/'>【励志故事】</a></dd><dd style='display: block;'><a href='/dushu/youmogushi/'>【幽默故事】</a></dd><dd style='display: block;'><a href='/dushu/shuiqian/'>【睡前故事】</a></dd><dd style='display: block;'><a href='/dushu/zhengyuanjietonghua/'>【郑渊洁童话】</a></dd><dd style='display: block;'><a href='/dushu/raokouling/'>【绕口令】</a></dd><dd style='display: block;'><a href='/dushu/chengyugushi/'>【成语故事】</a></dd><dd style='display: block;'><a href='/dushu/youxiuzuowen/'>【优秀作文】</a></dd><dd style='display: block;'><a href='/dushu/youxiuzuowen1/'>【儿童诗歌】</a></dd><dd style='display: block;'><a href='/dushu/youerjiaoyu/'>【幼儿教育】</a></dd><dd style='display: block;'><a href='/dushu/miyu/'>【儿童谜语】</a></dd>";
		// 此网站有哪些书
		String allURLs = "<dd style='display: block;'><a href='/dushu/gelintonghua/'>【格林童话】</a></dd>";
		
		String[] urls = allURLs.split("</dd>");
		
		// 在这里每一本书无非就是要有URL和Title，你也可以这样写：
		/*
		Book book = new Book();
		book.url = "http://www.cnfla.com/dushu/gelintonghua/";
		book.title = "格林童话";
		book.hunt();
		*/
		
		for (int i=0; i<urls.length; i++)
		{
			String strBook = urls[i];
			Book book = new Book();
			
			// 一本书的URL
			int beginIndex = strBook.indexOf("<a href='")+"<a href='".length();
			int endIndex = strBook.indexOf("'>", beginIndex);
			book.url = BASE_URL + strBook.substring(beginIndex, endIndex);
			
			// 一本书的Title
			beginIndex = endIndex + 3;
			endIndex = strBook.indexOf("</a>", beginIndex) - 1;
			book.title = strBook.substring(beginIndex, endIndex);
			
			// 抓取所有章节及内容 
			book.hunt();
		}
	
	}

}
