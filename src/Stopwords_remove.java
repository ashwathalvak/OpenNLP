import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

public class Stopwords_remove {
	/**
	 * @param string
	 * @return
	 * @throws IOException
	 */
	public static String removeStopWords(String string) throws IOException {
		TokenStream tokenStream = tokenStream(new StringReader(string));
		StringBuilder sb = new StringBuilder();
		try {
			CharTermAttribute token = tokenStream
					.getAttribute(CharTermAttribute.class);
			tokenStream.reset();
			while (tokenStream.incrementToken()) {
				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(token.toString());
			}
		} finally {
			tokenStream.close();
		}
		return sb.toString();
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String args[]) throws IOException {
		File file = new File("input/tweets_orig.txt");
		List<String> lines = FileUtils.readLines(file, "UTF-8");
		List<String> linesNoStopWords = new ArrayList<String>();
		for (String line : lines) {
			linesNoStopWords.add(Stopwords_remove.removeStopWords(line));
		}
		File newFile = new File("input/tweets_new.txt");
		FileUtils.writeLines(newFile, linesNoStopWords);
	}
	
	@SuppressWarnings("deprecation")
	private static TokenStream tokenStream(Reader reader) throws IOException{		  
		  TokenStream result=new StandardTokenizer(Version.LUCENE_CURRENT,reader);
		  result=new StandardFilter(Version.LUCENE_CURRENT, result);
		  result=new LowerCaseFilter(Version.LUCENE_CURRENT, result);		  
		  CharArraySet engStopWords = new CharArraySet(Version.LUCENE_CURRENT, StandardAnalyzer.STOP_WORDS_SET.size(), Boolean.TRUE);
		  List<String> lines = FileUtils.readLines(new File("conf/english_stopwords.txt"));
		  for(String line: lines){
			  engStopWords.add(line);  
		  }		  
		  return new StopFilter(Version.LUCENE_CURRENT,result,engStopWords);
		  //return new PorterStemFilter(result);
		}
}