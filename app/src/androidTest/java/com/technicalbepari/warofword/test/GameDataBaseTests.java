package com.technicalbepari.warofword.test;

import java.util.List;

import junit.framework.Assert;

import com.technicalbepari.warofword.model.GameDataBase;
import com.technicalbepari.warofword.model.GameModel;

import android.test.AndroidTestCase;

public class GameDataBaseTests extends AndroidTestCase {

	public void test() {

		final GameModel gm = new GameModel(getContext());

		// Create
		GameDataBase gdb = new GameDataBase(getContext());
		Assert.assertNotNull(gdb);

		// Clear
		gdb.deleteAll();
		List<Long> keys = gdb.getAllGameKeys();
		Assert.assertTrue(keys.size()==0);

		// Add
		long key = gdb.addGame(gm);
		Assert.assertTrue(key>=0);
		GameModel game = gdb.getGame(key);
		Assert.assertNotNull(game);

		Assert.assertTrue(game.getGameState() == gm.getGameState());
		Assert.assertTrue(game.getPoints(GameModel.PLAYER1) == gm.getPoints(GameModel.PLAYER1) );
		Assert.assertTrue(game.getPoints(GameModel.PLAYER2) == gm.getPoints(GameModel.PLAYER2) );

		for (int i=0; i<GameModel.GRID_ITEMS; ++i) {
			Assert.assertTrue(game.getLetter(i)==gm.getLetter(i));
			Assert.assertTrue(game.getLetterState(i)==gm.getLetterState(i));
		}

		keys = gdb.getAllGameKeys();
		Assert.assertTrue(keys.size()==1);

		// Update
		int rowsUpdated = gdb.updateGame(key, gm);
		Assert.assertTrue(rowsUpdated>0);
		game = gdb.getGame(key);

		// Delete
		gdb.deleteGame(key);
		game = gdb.getGame(key);
		Assert.assertTrue(game==null);

		// Test adding null
		key = gdb.addGame(null);
		Assert.assertTrue(key==GameDataBase.NULL_PARAMETER_ERROR);

	}

}
