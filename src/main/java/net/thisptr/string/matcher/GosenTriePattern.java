package net.thisptr.string.matcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.java.sen.trie.TrieBuilder;
import net.java.sen.trie.TrieSearcher;

public class GosenTriePattern implements StringPattern {
	private IntBuffer trieBuffer;
	private String[] wordArray;

	private GosenTriePattern(final IntBuffer trieBuffer, final String[] wordArray) {
		this.trieBuffer = trieBuffer;
		this.wordArray = wordArray;
	}

	public static GosenTriePattern compile(final Collection<String> words) throws IOException {
		List<String> copyOfWords = new ArrayList<String>(words);
		Collections.sort(copyOfWords);
		String[] wordArray = copyOfWords.toArray(new String[copyOfWords.size()]);
		int[] sequencialIds = new int[wordArray.length];
		for (int i = 0; i < wordArray.length; ++i)
			sequencialIds[i] = i;

		File trieFile = File.createTempFile(GosenTriePattern.class.getCanonicalName() + ".", ".trie");
		trieFile.deleteOnExit();

		TrieBuilder trieBuilder = new TrieBuilder(wordArray, sequencialIds, wordArray.length);
		trieBuilder.build(trieFile.getAbsolutePath());

		try (FileInputStream trieFileStream = new FileInputStream(trieFile)) {
			FileChannel trieFileChannel = trieFileStream.getChannel();
			IntBuffer trieBuffer = trieFileChannel.map(FileChannel.MapMode.READ_ONLY, 0, trieFileChannel.size()).asIntBuffer();
			while (trieBuffer.hasRemaining())
				trieBuffer.get();
			return new GosenTriePattern(trieBuffer, wordArray);
		}
	}

	private IntBuffer getTrieBuffer() {
		return trieBuffer;
	}

	private String getWord(int id) {
		return wordArray[id];
	}

	private List<Integer> commonPrefixSearch(final StringIterator iterator) {
		StringIterator copyOfIterator = new StringIterator(iterator);
		List<Integer> result = new ArrayList<Integer>();
		int[] resultArray = new int[1024]; // FIXME: handle ArrayOutOfBoundsException
		int nResults = TrieSearcher.commonPrefixSearch(getTrieBuffer(), copyOfIterator, resultArray);
		for (int i = 0; i < nResults; ++i)
			result.add(resultArray[i]);
		return result;
	}

	private static class GosenTrieMatcher implements StringMatcher {
		private GosenTriePattern pattern;

		private StringIterator seqIter;
		private Iterator<Integer> iter;
		private int current = -1;

		public GosenTrieMatcher(final GosenTriePattern pattern, final CharSequence seq) {
			this.pattern = pattern;
			this.seqIter = new StringIterator(seq);
		}

		@Override
		public boolean find() {
			if (iter != null && iter.hasNext()) {
				current = iter.next();
				return true;
			}

			if (seqIter.hasNext()) {
				iter = pattern.commonPrefixSearch(seqIter).iterator();
				seqIter.next();
				return find();
			}

			current = -1;
			return false;
		}

		@Override
		public int at() {
			if (current < 0)
				throw new NoSuchElementException();
			return seqIter.currentIndex();
		}

		@Override
		public String text() {
			if (current < 0)
				throw new NoSuchElementException();
			return pattern.getWord(current);
		}
	}

	@Override
	public StringMatcher matcher(final CharSequence seq) {
		return new GosenTrieMatcher(this, seq);
	}
}
