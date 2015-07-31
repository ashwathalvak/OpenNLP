/**
 * 
 */
package com.barclays.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

/**
 * @author G01013268
 * 
 */
public class NLPUtils {

	private static final String CONF_ENGLISH_CUSTOM_STOPWORDS_TXT = "english_stopwords.txt";

	/**
	 * Generate the filtered base data
	 * 
	 * @param line
	 *            Base data line by line
	 * @return Filtered base data line by line
	 * @throws IOException
	 *             if there is an exception reading the data
	 */
	public static String removeStopWords(String line) throws IOException {
		TokenStream tokenStream = createTokenStream(new StringReader(line));
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
	 * Create a chained filter stream to filter the base data
	 * 
	 * @param reader
	 *            Handle to base data file reader
	 * @return Handle to filtered base data file reader
	 * @throws IOException
	 *             if there is an exception reading the data
	 */
	private static TokenStream createTokenStream(Reader reader)
			throws IOException {
		TokenStream result = new LowerCaseFilter(Version.LUCENE_48,
				new StandardFilter(Version.LUCENE_48, new StandardTokenizer(
						Version.LUCENE_48, reader)));
		return  new StopFilter(Version.LUCENE_48, result,
				readCustomStopWords());
		//return new PorterStemFilter(result);
	}

	/**
	 * Read the stop word configuration file into a @CharArraySet and return the
	 * same
	 * 
	 * @return Set of stop words
	 * @throws IOException
	 *             if there was an error reading the stop-word configuration
	 *             file
	 */
	private static CharArraySet readCustomStopWords() throws IOException {
		CharArraySet engStopWords = new CharArraySet(Version.LUCENE_48,
				StandardAnalyzer.STOP_WORDS_SET.size(), Boolean.TRUE);
		List<String> lines = IOUtils.readLines(Thread.currentThread()
				.getContextClassLoader()
				.getResourceAsStream(CONF_ENGLISH_CUSTOM_STOPWORDS_TXT));
		for (String line : lines) {
			engStopWords.add(line);
		}
		return engStopWords;
	}

}
