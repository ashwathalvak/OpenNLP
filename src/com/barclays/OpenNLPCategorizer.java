package com.barclays;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.util.Version;

public class OpenNLPCategorizer {
	private static final String CONF_ENGLISH_CUSTOM_STOPWORDS_TXT = "conf/english_stopwords.txt";
	private static final String INPUT_TWEETS_REMOVED_STOPWORDS_TXT = "input/tweets_new.txt";
	private static final String INPUT_TWEETS_ORIG_TXT = "input/tweets_orig.txt";
	// Specifies the minimum number of times a feature must be seen and is used
	// to designate a method of reducing the size of n-gram language models.
	// Removal of n-grams that occur infrequently in the training data.
	private static final int CUTOFF_FREQ = 2;
	// refers to the general notion of iterative algorithms, where one sets out
	// to solve a problem by successively producing (hopefully increasingly more
	// accurate) approximations of some "ideal" solution. Generally speaking,
	// the more iterations, the more accurate ("better") the result will be, but
	// of course the more computational steps have to be carried out.
	private static final int TRAINING_ITERATIONS = 30;
	DoccatModel model;

	public static void main(String[] args) throws IOException {
		createModelInput();
		OpenNLPCategorizer twitterCategorizer = new OpenNLPCategorizer();
		twitterCategorizer.trainModel();
		twitterCategorizer.classifyNewTweet("Have a nice day!");
		twitterCategorizer.classifyNewTweet("To everybody's surprise the critics hated it!");
	}

	/**
	 * Create the filtered base data for the model to train on
	 * @throws IOException
	 */
	private static void createModelInput() throws IOException {
		File file = new File(INPUT_TWEETS_ORIG_TXT);
		List<String> lines = FileUtils.readLines(file, "UTF-8");
		List<String> linesNoStopWords = new ArrayList<String>();
		for (String line : lines) {
			linesNoStopWords.add(removeStopWords(line));
		}
		File newFile = new File(INPUT_TWEETS_REMOVED_STOPWORDS_TXT);
		FileUtils.writeLines(newFile, linesNoStopWords);
	}

	/**
	 * Generate the filtered base data 
	 * @param line Base data line by line
	 * @return Filtered base data line by line
	 * @throws IOException if there is an exception reading the data
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
	 * @param reader Handle to base data file reader
	 * @return Handle to filtered base data file reader
	 * @throws IOException if there is an exception reading the data
	 */
	private static TokenStream createTokenStream(Reader reader)
			throws IOException {
		TokenStream result = new LowerCaseFilter(Version.LUCENE_48,
				new StandardFilter(Version.LUCENE_48, new StandardTokenizer(
						Version.LUCENE_48, reader)));
		TFIDFSimilarity tfidf = new DefaultSimilarity();
		return new StopFilter(Version.LUCENE_48, result, readCustomStopWords());
		// return new PorterStemFilter(result);
	}

	/**
	 * Read the stop word configuration file into a @CharArraySet and return the
	 * same
	 * @return Set of stop words
	 * @throws IOException if there was an error reading the stop-word configuration
	 *             file
	 */
	private static CharArraySet readCustomStopWords() throws IOException {
		CharArraySet engStopWords = new CharArraySet(Version.LUCENE_48,
				StandardAnalyzer.STOP_WORDS_SET.size(), Boolean.TRUE);
		List<String> lines = FileUtils.readLines(new File(
				CONF_ENGLISH_CUSTOM_STOPWORDS_TXT));
		for (String line : lines) {
			engStopWords.add(line);
		}
		return engStopWords;
	}

	/**
	 * Train and create the model on the stop-word filtered data
	 */
	public void trainModel() {
		InputStream dataIn = null;
		try {
			ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(
					new PlainTextByLineStream(new FileInputStream(
							INPUT_TWEETS_REMOVED_STOPWORDS_TXT), "UTF-8"));
			model = DocumentCategorizerME.train("en", sampleStream,
					CUTOFF_FREQ, TRAINING_ITERATIONS);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (dataIn != null) {
				try {
					dataIn.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Evaluate the tweet against the trained model and output the sentiment
	 * @param tweet Incoming tweet
	 */
	public void classifyNewTweet(String tweet) {
		DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
		double[] outcomes = myCategorizer.categorize(tweet);
		String category = myCategorizer.getBestCategory(outcomes);

		if ("1".equals(category)) {
			System.out.println(" Tweet: " + tweet + "\tSentiment: +ve");
		} else {
			System.out.println(" Tweet: " + tweet + "\tSentiment: -ve");
		}
	}
}
