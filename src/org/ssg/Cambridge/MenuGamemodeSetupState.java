package org.ssg.Cambridge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.newdawn.slick.AngelCodeFont;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.KeyListener;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;

import paulscode.sound.SoundSystem;

public class MenuGamemodeSetupState extends BasicGameState implements KeyListener {
	private GlobalData data;
	private String[] menuOptions;
	private Ini ini;
	private int stateID;
	SoundSystem mySoundSystem;
	Ini.Section display, sound, gameplay;

	CambridgeController[] controllers;
	CambridgePlayerAnchor[] anchors;

	private boolean down, up, left, right, back, enter;
	private final float deadzone = 0.28f;
	private int inputDelay;
	private final int inputDelayConst = 10;

	private boolean shouldRender;

	final static String RESDIR = "res/";
	private AngelCodeFont font, font_white, font_small;

	private Cambridge cambridge;
	private AppGameContainer appGc;

	private int selected;

	//Constructor
	public MenuGamemodeSetupState(int i, boolean renderon) {
		stateID = i;
		shouldRender = renderon;
	}

	@Override
	public void init(GameContainer gc, StateBasedGame sbg)
			throws SlickException {

		data = ((Cambridge) sbg).getData();
		mySoundSystem = data.mySoundSystem();

		anchors = data.playerAnchors();

		up = false;
		down = false;
		left = false;
		right = false;
		enter = false;
		back = false;
		inputDelay = 0;

		font = new AngelCodeFont(data.RESDIR + "8bitoperator.fnt", new Image(data.RESDIR + "8bitoperator_0.png"));
		font_white = new AngelCodeFont(data.RESDIR + "8bitoperator.fnt", new Image(data.RESDIR + "8bitoperator_0_white.png"));
		font_small = new AngelCodeFont(data.RESDIR + "8bitoperator_small.fnt", new Image(data.RESDIR + "8bitoperator_small_0.png"));

		cambridge = (Cambridge) sbg;
		appGc = (AppGameContainer) gc;

		selected = 0;

		menuOptions = new String[] {
				"Time Limit",
				"Score Limit",
				"Action Camera",
				"Start Game"
		};
	}

	@Override
	public void render(GameContainer gc, StateBasedGame sbg, Graphics g)
			throws SlickException {
		if (!shouldRender)
			return;

		g.setAntiAlias(true);
		
		g.setLineWidth(2f);

		g.setBackground(Color.black);

		g.setColor(Color.white);

		g.setFont(font_white);

		g.drawString(
				data.gameConfig().get(data.gameType()+"").get("NAME", String.class),
				data.screenWidth()/2 - font_white.getWidth(data.gameConfig().get(data.gameType()+"").get("NAME", String.class))/2,
				data.screenHeight() * 1/3 - font_white.getLineHeight()/2
				);

		g.setFont(font_small);

		if (selected > 0) {
			g.drawRect(
					data.screenWidth() * 1/4 - 5,
					data.screenHeight() * (6+selected)/12 + (data.screenHeight() * 1/12 - font_small.getLineHeight()) / 2 - 5,
					font_small.getWidth(menuOptions[selected-1]) + 10,
					font_small.getLineHeight() + 10
					);
		}

		for (int i = 0; i < menuOptions.length; i++) {
			g.drawString(
					menuOptions[i],
					data.screenWidth() * 1/4,
					data.screenHeight() * (7+i)/12 + (data.screenHeight() * 1/12 - font_small.getLineHeight()) / 2
					);
			switch(i) {
			case 0:
				g.drawString(data.timeLimit()/60 + ":" + String.format("%02d",data.timeLimit()%60), data.screenWidth()/2, data.screenHeight() * (7+i)/12 + (data.screenHeight() * 1/12 - font_small.getLineHeight()) / 2);
				break;
			case 1:
				g.drawString(data.scoreLimit()+"", data.screenWidth()/2, data.screenHeight() * (7+i)/12 + (data.screenHeight() * 1/12 - font_small.getLineHeight()) / 2);
				break;
			case 2:
				g.drawString((data.actionCam() ? "On" : "Off"), data.screenWidth()/2, data.screenHeight() * (7+i)/12 + (data.screenHeight() * 1/12 - font_small.getLineHeight()) / 2);
				break;
			default:
				break;
			}
		}


	}

	@Override
	public void update(GameContainer gc, StateBasedGame sbg, int delta)
			throws SlickException {
		Input input = gc.getInput();

		if (input.isKeyPressed(Input.KEY_ESCAPE)) {
			((MenuMainState)sbg.getState(data.MENUMAINSTATE)).setShouldRender(true);
			setShouldRender(false);
			sbg.enterState(data.MENUMAINSTATE);
		}

		up = false;
		down = false;
		left = false;
		right = false;
		back = false;
		enter = false;
		if (inputDelay <= 0) {
			for (CambridgePlayerAnchor a : anchors) {
				if (a.initiated()) {
					if (up || down || left || right || enter || back) {
						inputDelay = inputDelayConst;
					} else {
						if (a.down(gc, delta)) {
							down = true;
						} else if (a.up(gc, delta)) {
							up = true;
						} else if (a.left(gc, delta)) {
							left = true;
						} else if (a.right(gc, delta)) {
							right = true;
						} else if (a.select(gc, delta)) {
							enter = true;
						} else if (a.back(gc, delta)) {
							back = true;
						}
					}
				}
			}
		} else {
			inputDelay--;
		}

		if (up) {
			if (selected > 0) {
				selected--;
			}
		} else if (down) {
			if (selected < 4) {
				selected++;
			}
		} else if (left) {
			switch(selected) {
			case 0:
				data.setGameType((data.gameType() > 0) ? data.gameType()-1 : data.GAMEMODES-1);
				break;
			case 1:
				data.setTimeLimit(-1);
				break;
			case 2:
				data.setScoreLimit(-1);
				break;
			case 3:
				data.setActionCam(!data.actionCam());
				break;
			case 4:
				break;
			default:
				break;
			}
		} else if (right) {
			switch(selected) {
			case 0:
				data.setGameType((data.gameType() < data.GAMEMODES-1) ? data.gameType()+1 : 0);
				break;
			case 1:
				data.setTimeLimit(1);
				break;
			case 2:
				data.setScoreLimit(1);
				break;
			case 3:
				data.setActionCam(!data.actionCam());
				break;
			case 4:
				break;
			default:
				break;
			}
		} else if(back) {
			for (CambridgePlayerAnchor a: anchors) {
				a.setCharacter(false);
			}
			((MenuPlayerSetupState)sbg.getState(data.MENUPLAYERSETUPSTATE)).setShouldRender(true);
			setShouldRender(false);
			sbg.enterState(data.MENUPLAYERSETUPSTATE);
		} else if (enter) {
			if (selected != 4) {
				selected = 4;
			} else {
				if (data.gameType() == data.GAMEMODES-1) {
					((GameplayState)sbg.getState(data.GAMEPLAYSTATE)).setShouldRender(true);
					setShouldRender(false);
					sbg.enterState(data.GAMEPLAYSTATE);
				} else {
					((MenuTeamSetupState)sbg.getState(data.MENUTEAMSETUPSTATE)).setShouldRender(true);
					setShouldRender(false);
					sbg.enterState(data.MENUTEAMSETUPSTATE);
				}
			}
		}

		input.clearKeyPressedRecord();
	}

	@Override
	public void enter(GameContainer gc, StateBasedGame sbg) throws SlickException {

	}

	@Override
	public void leave(GameContainer gc, StateBasedGame sbg) throws SlickException {

	}

	public void setShouldRender(boolean shouldRender) {
		this.shouldRender = shouldRender;
	}

	@Override
	public int getID() {
		// TODO Auto-generated method stub
		return stateID;
	}

}