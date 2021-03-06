/********************************************
 * (C) Copyright IBM Corp. 2018
 ********************************************/
package com.ibm.trl.BBM.mains;

public class Constant {
	static public int Passage = 0;
	static public int Rigid = 1;
	static public int Wood = 2;
	static public int Bomb = 3;
	static public int Flames = 4;
	static public int Fog = 5;
	static public int ExtraBomb = 6;
	static public int IncrRange = 7;
	static public int Kick = 8;
	static public int AgentDummy = 9;
	static public int Agent0 = 10;
	static public int Agent1 = 11;
	static public int Agent2 = 12;
	static public int Agent3 = 13;

	static public boolean isAgent(int type) {
		if (type >= Agent0 && type <= Agent3) return true;
		else return false;
	}

	static public boolean isWall(int type) {
		if (type == Rigid || type == Wood) return true;
		else return false;
	}

	static public boolean isItem(int type) {
		if (type == ExtraBomb || type == IncrRange || type == Kick) return true;
		else return false;
	}
}
