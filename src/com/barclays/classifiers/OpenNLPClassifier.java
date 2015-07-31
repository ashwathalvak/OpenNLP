package com.barclays.classifiers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.barclays.util.NLPUtils;

public class OpenNLPClassifier implements IClassifier {
	private static final String INPUT_TWEETS_REMOVED_STOPWORDS_TXT = "tweets_filtered_stopwords.txt";
	private static final String INPUT_TWEETS_ORIG_TXT = "tweets.txt";
	private static final String TEST_TWEETS_TXT = "tweets_test.txt";
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
	

	public static void main(String[] args) throws IOException {
		OpenNLPClassifier classifier = new OpenNLPClassifier();

		List<String> lines = IOUtils.readLines(Thread.currentThread()
				.getContextClassLoader().getResourceAsStream(TEST_TWEETS_TXT));
		for (String line : lines) {
			String[] words = line.split("\\t");
			int sentiment = classifier.predict(words[1]);
			System.out.println("Tweet: " + words[1] + " :OrigSentiment: "
					+ words[0] + ":Prediction:" + sentiment);
		}
	}

	/**
	 * Create the filtered base data for the model to train on
	 * 
	 * @throws IOException
	 */
	private File createModelInput(String fileName) throws IOException {		
		List<String> lines = IOUtils.readLines(Thread.currentThread()
				.getContextClassLoader().getResourceAsStream(fileName));
		List<String> linesNoStopWords = new ArrayList<String>();
		for (String line : lines) {
			linesNoStopWords.add(NLPUtils.removeStopWords(line));
		}
		File newFile = new File(INPUT_TWEETS_REMOVED_STOPWORDS_TXT);
		FileUtils.writeLines(newFile, linesNoStopWords);
		return newFile;
	}

	/**
	 * Train and create the model on the stop-word filtered data
	 * 
	 * @throws IOException
	 */
	private DoccatModel trainModel(boolean readExistingModel, String trainingDataFile) throws IOException {		
		InputStream dataIn = null;
		final String MAXENTROPY_SERIALIZED_FILE_NAME = "maxEntropyModel";
		if(readExistingModel){
			return deserialize(MAXENTROPY_SERIALIZED_FILE_NAME);
		}
		DoccatModel model = null;
		try {
			ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(
					new PlainTextByLineStream(new FileInputStream(
							createModelInput(trainingDataFile)), "UTF-8"));
			model = DocumentCategorizerME.train("en", sampleStream,
					CUTOFF_FREQ, TRAINING_ITERATIONS);
			OutputStream file = new FileOutputStream(MAXENTROPY_SERIALIZED_FILE_NAME);
			model.serialize(file);
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
		return model;
	}

	/**
	 * Evaluate the tweet against the trained model and output the sentiment
	 * 
	 * @param tweet
	 *            Incoming tweet
	 * @throws IOException
	 */
	public int predict(String tweet) {
		DoccatModel model = null;
		try {
			model = trainModel(true, INPUT_TWEETS_ORIG_TXT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
		double[] outcomes = myCategorizer.categorize(tweet);
		return Integer.parseInt(myCategorizer.getBestCategory(outcomes));
	}
	
	private DoccatModel deserialize(String fileName) throws IOException {
		InputStream fileStream = Thread.currentThread()
				.getContextClassLoader().getResourceAsStream(fileName);
		return new DoccatModel(fileStream); 
				
	}	
}
