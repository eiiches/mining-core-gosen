package net.thisptr.string.matcher;

import java.util.NoSuchElementException;

import net.java.sen.trie.CharIterator;

public class StringIterator implements CharIterator {
	private CharSequence input;
	private int position;
	
	public StringIterator(final CharSequence input) {
		this.input = input;
		this.position = -1;
	}
	
	public StringIterator(final StringIterator iter) {
		this.input = iter.input;
		this.position = iter.position;
	}
	
	@Override
	public boolean hasNext() {
		return position + 1 < input.length();
	}
	
	@Override
	public char next() throws NoSuchElementException {
		++position;
		if (position < 0 || input.length() <= position)
			throw new NoSuchElementException();
		return input.charAt(position);
	}
	
	public int currentIndex() {
		return position;
	}
}
