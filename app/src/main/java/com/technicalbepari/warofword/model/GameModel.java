package com.technicalbepari.warofword.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.content.Context;

/**
 * Manage the state of a game, including the state of the grid, word, previously
 * played words, and player scores.
 *
 * A GameModel provides the data that is rendered on a Board object, and accepts
 * user input via a Board object.
 *
 * @author Andrew Smith
 */
public class GameModel implements Serializable {

	public static final int GRID_COLUMNS = 5;
	public static final int GRID_ROWS = 5;
	public static final int GRID_ITEMS = GRID_COLUMNS * GRID_ROWS;
	public static final int PLAYER1 = 0;
	public static final int PLAYER2 = 1;
	private static final long serialVersionUID = 1L;

	private static final int NUMBER_OF_VOWELS_ON_BOARD = 4;
	private static final String CONSONANTS = "BCDFHJKLMNPRSTVWXYZQ";
	private static final String VOWELS = "AEIOU";

	public enum GameState {
		PLAYER1_TURN, PLAYER2_TURN, GAME_OVER
	}

	public enum LetterState {
		UNPLAYED, PLAYER1_OWNED, PLAYER1_SURROUNDED, PLAYER2_OWNED, PLAYER2_SURROUNDED,
	}

	public enum TurnResult {
		WORD_NOT_IN_DICTIONARY, WORD_ALREADY_PLAYED, WORD_IS_PREFIX_OF_PREVIOUS_TURN, WORD_LESS_THAN_TWO_LETTERS, SUCCESS,
	}

	public enum GameResult {
		PLAYER1_WIN, PLAYER2_WIN, DRAW
	}

	private class Letter implements Serializable {

		private static final long serialVersionUID = 1L;
		public char mLetter;
		public LetterState mLetterState;

		public Letter(char letter) {
			mLetter = letter;
			mLetterState = LetterState.UNPLAYED;
		}

		public Letter(char letter, LetterState state) {
			mLetter = letter;
			mLetterState = state;
		}

	}

	private final Letter[] mGrid;
	private List<Integer> mWord;

	private GameState mGameState;
	private GameResult mGameResult;
	private boolean mHasPassed;
	private WordTrie mPlayedWords = new WordTrie();

	private int mPlayer1Points;
	private int mPlayer2Points;

	transient protected Context mContext;

	public GameModel(Context context) {

		mGrid = generateRandomLetterGrid();

		mGameState = GameState.PLAYER1_TURN;
		mPlayer1Points = 0;
		mPlayer2Points = 0;

		mContext = context;

	}

	public static GameModel deserialize(byte[] stream, Context context) {
		GameModel gm = (GameModel) Serializer.deserialize(stream);
		if (gm != null)
			gm.setContext(context);
		return gm;
	}

	private void setContext(Context context) {
		mContext = context;
	}

	public byte[] serialize() {
		return Serializer.serialize(this);
	}

	public GameModel(char[] grid, LetterState[] states, GameState gameState,
			int p1Points, int p2Points, Context context) {

		mGrid = new Letter[GRID_ITEMS];

		for (int i = 0; i < GRID_ITEMS; ++i) {
			mGrid[i] = new Letter(grid[i], states[i]);
		}

		mGameState = gameState;
		mPlayer1Points = p1Points;
		mPlayer2Points = p2Points;

		mContext = context;

	}

	/* DataSource Methods */

	public GameState getGameState() {
		return mGameState;
	}

	public int getGridSize() {
		return GRID_ITEMS;
	}

	public int getGridHeight() {
		return GRID_ROWS;
	}

	public int getGridWidth() {
		return GRID_COLUMNS;
	}

	public char getLetter(int index) {
		return mGrid[index].mLetter;
	}

	public LetterState getLetterState(int index) {
		return mGrid[index].mLetterState;
	}

	public int getPoints(int player) {

		int p1Points = mPlayer1Points;
		int p2Points = mPlayer2Points;

		if (mWord != null) {
			for (int i : mWord) {
				LetterState state = mGrid[i].mLetterState;

				if (mGameState == GameState.PLAYER1_TURN) {
					if (state == LetterState.UNPLAYED) {
						++p1Points;
					} else if (state == LetterState.PLAYER2_OWNED) {
						++p1Points;
						--p2Points;
					}
				} else if (mGameState == GameState.PLAYER2_TURN) {
					if (state == LetterState.UNPLAYED) {
						++p2Points;
					} else if (state == LetterState.PLAYER1_OWNED) {
						++p2Points;
						--p1Points;
					}
				}
			}
		}

		return (player == PLAYER1) ? p1Points : p2Points;

	}

	public GameResult getResult() {
		return mGameResult;
	}

	public String getWord() {
		return decodeWordFromTileIndexes(mWord);
	}

	/* Delegate Methods */

	public TurnResult playTurn() {

		if (mWord == null)
			return TurnResult.WORD_LESS_THAN_TWO_LETTERS;

		String word = decodeWordFromTileIndexes(mWord);

		TurnResult ret = applyRules(word);

		if (ret == TurnResult.SUCCESS) {
			mHasPassed = false;

			mPlayedWords.add(word);

			mPlayer1Points = getPoints(PLAYER1);
			mPlayer2Points = getPoints(PLAYER2);

			// Assign new states to played word
			for (int i : mWord) {

				if (mGrid[i].mLetterState != LetterState.PLAYER1_SURROUNDED
						&& mGrid[i].mLetterState != LetterState.PLAYER2_SURROUNDED) {

					mGrid[i].mLetterState = (mGameState == GameState.PLAYER1_TURN) ? LetterState.PLAYER1_OWNED
							: LetterState.PLAYER2_OWNED;
				}

			}

			mWord = null;

			makeCaptures();

			// check if game is over
			boolean gameIsOver = true;
			for (int i = 0; i < GRID_ITEMS; ++i) {
				gameIsOver &= mGrid[i].mLetterState != LetterState.UNPLAYED;
				if (!gameIsOver)
					break;
			}

			if (gameIsOver) {
				endGame();
			} else if (mGameState == GameState.PLAYER1_TURN) {
				mGameState = GameState.PLAYER2_TURN;
			} else if (mGameState == GameState.PLAYER2_TURN) {
				mGameState = GameState.PLAYER1_TURN;
			}

		}

		return ret;

	}

	public void passTurn() {
		if (mHasPassed) {
			endGame();
		} else if (mGameState == GameState.PLAYER1_TURN) {
			mGameState = GameState.PLAYER2_TURN;
		} else if (mGameState == GameState.PLAYER2_TURN) {
			mGameState = GameState.PLAYER1_TURN;
		}
		mHasPassed = true;
	}

	private void endGame() {
		mGameState = GameState.GAME_OVER;

		if (mPlayer1Points > mPlayer2Points) {
			mGameResult = GameResult.PLAYER1_WIN;
		} else if (mPlayer1Points < mPlayer2Points) {
			mGameResult = GameResult.PLAYER2_WIN;
		} else {
			mGameResult = GameResult.DRAW;
		}
	}

	public void setWord(List<Integer> letters) {
		mWord = letters;
	}

	/* Internal */

	private String decodeWordFromTileIndexes(List<Integer> word) {

		if (word == null)
			return "";

		StringBuilder sb = new StringBuilder();
		for (int i : word) {
			sb.append(mGrid[i].mLetter);
		}
		return sb.toString();
	}

	private Letter[] generateRandomLetterGrid() {

		Letter[] grid = new Letter[GRID_ITEMS];

		Random r = new Random();

		boolean hasU = false;
		List<Integer> v = new LinkedList<Integer>();
		while (v.size() < NUMBER_OF_VOWELS_ON_BOARD) {
			int index = r.nextInt(GRID_ITEMS);
			if (!v.contains(index))
			{
				v.add(index);
				// pick a random vowel
				int pos = r.nextInt(VOWELS.length());
				char c = VOWELS.charAt(pos);
				grid[index] = new Letter(c);
				hasU = hasU || (c == 'U');
			}
		}

		for (int i = 0; i < GRID_ITEMS; ++i) {
			if ( ! v.contains(i)) {
				// pick a random consonant, but skip Q unless we have a U
				int consonantChoices = CONSONANTS.length() - (hasU ? 0 : 1);
				int pos = r.nextInt(consonantChoices);
				char c = CONSONANTS.charAt(pos);
				grid[i] = new Letter(c);
			}
		}

		return grid;

	}

	/**
	 * Check for letters that are surrounded by a single player, and change
	 * their state if needed. Also adjust the scores if letters change owner.
	 */
	private void makeCaptures() {

		for (int tile = 0; tile < GRID_ITEMS; ++tile) {

			LetterState currentTileState = mGrid[tile].mLetterState;

			final int left = tile - 1;
			final int right = tile + 1;
			final int above = tile - GRID_COLUMNS;
			final int below = tile + GRID_COLUMNS;

			Letter[] surrounding = new Letter[4];

			surrounding[0] = (left >= 0 && left % GRID_COLUMNS < tile
					% GRID_COLUMNS) ? mGrid[left] : null;
			surrounding[1] = (tile % GRID_COLUMNS < right % GRID_COLUMNS) ? mGrid[right]
					: null;
			surrounding[2] = (above >= 0) ? mGrid[above] : null;
			surrounding[3] = (below < GRID_ITEMS) ? mGrid[below] : null;

			boolean player1Surrounded = true;
			boolean player2Surrounded = true;

			for (int i = 0; i < 4; ++i) {

				player1Surrounded &= (surrounding[i] == null
						|| surrounding[i].mLetterState == LetterState.PLAYER1_OWNED || surrounding[i].mLetterState == LetterState.PLAYER1_SURROUNDED);

				player2Surrounded &= (surrounding[i] == null
						|| surrounding[i].mLetterState == LetterState.PLAYER2_OWNED || surrounding[i].mLetterState == LetterState.PLAYER2_SURROUNDED);

			}

			// Owners: player 1: 1, player 2: -1, neutral: 0
			int oldOwner = getOwner(tile);
			if (player1Surrounded) {
				mGrid[tile].mLetterState = LetterState.PLAYER1_SURROUNDED;
			} else if (!player1Surrounded
					&& currentTileState == LetterState.PLAYER1_SURROUNDED) {
				mGrid[tile].mLetterState = LetterState.PLAYER1_OWNED;
			} else if (player2Surrounded) {
				mGrid[tile].mLetterState = LetterState.PLAYER2_SURROUNDED;
			} else if (!player2Surrounded
					&& currentTileState == LetterState.PLAYER2_SURROUNDED) {
				mGrid[tile].mLetterState = LetterState.PLAYER2_OWNED;
			}
			int newOwner = getOwner(tile);
			if (newOwner != oldOwner) {
				if (oldOwner == 1) {
					mPlayer1Points -= oldOwner;
				} else {
					mPlayer2Points += oldOwner;
				}
				if (newOwner == 1) {
					mPlayer1Points += newOwner;
				} else {
					mPlayer2Points -= newOwner;
				}
			}
		}
	}

	/**
	 * Look up who owns a tile.
	 * @param tile - the index of the tile to look at.
	 * @return 1 for player 1, -1 for player 2, 0 for neutral.
	 */
	private int getOwner(int tile) {
		int oldOwner;
		switch (mGrid[tile].mLetterState) {
		case PLAYER1_SURROUNDED:
		case PLAYER1_OWNED:
			oldOwner = 1;
			break;
		case PLAYER2_SURROUNDED:
		case PLAYER2_OWNED:
			oldOwner = -1;
			break;
		default:
			oldOwner = 0;
			break;
		}
		return oldOwner;
	}

	private TurnResult applyRules(String word) {

		// Rule 1. Word must be two or more characters
		if (word.length() < 2)
			return TurnResult.WORD_LESS_THAN_TWO_LETTERS;

		// Rule 2. Word must not have been played already
		if (mPlayedWords.contains(word))
			return TurnResult.WORD_ALREADY_PLAYED;

		// Rule 3. Word must not be a suffix of a previously played word
		if (mPlayedWords.containsPrefix(word))
			return TurnResult.WORD_IS_PREFIX_OF_PREVIOUS_TURN;

		// Rule 4. Word must be in the English dictionary
		WordList db = new WordList(mContext);
		if (!db.wordInDictionary(word))
			return TurnResult.WORD_NOT_IN_DICTIONARY;

		return TurnResult.SUCCESS;
	}

}
