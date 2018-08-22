package com.ibm.trl.BBM.mains;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import com.ibm.trl.BBM.mains.Agent.Ability;
import com.ibm.trl.BBM.mains.Agent.Node;
import com.ibm.trl.BBM.mains.StatusHolder.AgentEEE;
import com.ibm.trl.BBM.mains.StatusHolder.BombEEE;
import com.ibm.trl.BBM.mains.StatusHolder.EEE;
import com.ibm.trl.BBM.mains.StatusHolder.FlameCenterEEE;

import ibm.ANACONDA.Core.MyMatrix;

public class ForwardModel {

	int numField;

	public ForwardModel(int numField) {
		this.numField = numField;
	}

	static public class Pack implements Serializable {
		private static final long serialVersionUID = -8052436421835761684L;
		MyMatrix board;
		Ability[] abs;
		StatusHolder sh;

		public Pack(MyMatrix board, Ability[] abs, StatusHolder sh) {
			this.board = board;
			this.abs = abs;
			this.sh = sh;
		}
	}

	public Pack Step(MyMatrix boardNow, Ability absNow[], StatusHolder shNow, int[] actions) throws Exception {

		MyMatrix boardNext = new MyMatrix(boardNow);

		Ability[] absNext = new Ability[4];
		for (int i = 0; i < 4; i++) {
			absNext[i] = new Ability(absNow[i]);
		}

		boolean[][] bombExistingMap = new boolean[numField][numField];
		for (EEE bbb : shNow.getBombEntry()) {
			bombExistingMap[bbb.x][bbb.y] = true;
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// FlameCenter�̎�����i�߂�B
		/////////////////////////////////////////////////////////////////////////////////////
		List<FlameCenterEEE> flameCenterNext = new ArrayList<FlameCenterEEE>();
		for (FlameCenterEEE fffNow : shNow.getFlameCenterEntry()) {
			if (fffNow.life == 1) continue;
			FlameCenterEEE fffNext = new FlameCenterEEE(fffNow);
			fffNext.life--;
			flameCenterNext.add(fffNext);
		}

		// �c���Ă���FrameCenter����MyFlame������āAboard��Flame�Ŏc���Ă��镔������������BPassage��\������B
		MyMatrix myFlameNext = new MyMatrix(numField, numField);
		for (FlameCenterEEE fffNext : flameCenterNext) {
			BBMUtility.PrintFlame(boardNext, myFlameNext, fffNext.x, fffNext.y, fffNext.power, 1);
		}

		// �Â�FlameCenter�������_�����O����ƁA�t���[���I�[���؂������̂���������Ȃ��BBoardNow��MyFlameNext�̋��ʕ��������c���Ă���Flame�ɂȂ�B���̏���������B
		for (int x = 0; x < numField; x++) {
			for (int y = 0; y < numField; y++) {
				if (myFlameNext.data[x][y] == 1) {
					if (boardNext.data[x][y] != Constant.Flames) {
						myFlameNext.data[x][y] = 0;
					}
				} else {
					if (boardNext.data[x][y] == Constant.Flames) {
						boardNext.data[x][y] = Constant.Passage;
					}
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// Agent��Bomb�̈ړ��̏����B
		/////////////////////////////////////////////////////////////////////////////////////
		AgentEEE[] agentsNow = new AgentEEE[4];
		AgentEEE[] agentsNext = new AgentEEE[4];
		List<BombEEE> added = new ArrayList<BombEEE>();
		for (AgentEEE eee : shNow.getAgentEntry()) {
			int agentID = eee.agentID;
			int agentIndex = agentID - 10;
			int action = actions[agentIndex];

			int x2 = eee.x;
			int y2 = eee.y;
			if (action == 0) {
			} else if (action == 1) {
				x2 -= 1;
			} else if (action == 2) {
				x2 += 1;
			} else if (action == 3) {
				y2 -= 1;
			} else if (action == 4) {
				y2 += 1;
			} else if (action == 5) {
				if (bombExistingMap[x2][y2] == false) {
					if (absNext[agentIndex].numBombHold > 0) {
						// ���e��ǉ��B
						added.add(new BombEEE(eee.x, eee.y, agentID, 10, 0, absNext[agentIndex].strength));
						absNext[agentIndex].numBombHold--;
					}
				}
			}
			if (x2 < 0 || x2 >= numField || y2 < 0 || y2 >= numField) {
				x2 = eee.x;
				y2 = eee.y;
			} else {
				int type = (int) boardNext.data[x2][y2];
				if (Constant.isWall(type)) {
					x2 = eee.x;
					y2 = eee.y;
				}
			}

			agentsNow[agentIndex] = eee;
			agentsNext[agentIndex] = new AgentEEE(x2, y2, eee.agentID);
		}

		BombEEE[] bombsNow;
		BombEEE[] bombsNext;
		int numBomb;
		{
			Collection<BombEEE> eees = shNow.getBombEntry();
			numBomb = eees.size();
			bombsNow = new BombEEE[numBomb];
			bombsNext = new BombEEE[numBomb];

			int index = 0;
			for (BombEEE bombNow : eees) {
				bombsNow[index] = bombNow;
				int life = bombNow.life;
				int dir = bombNow.dir;
				int x2 = bombNow.x;
				int y2 = bombNow.y;
				if (dir == 1) {
					x2--;
				} else if (dir == 2) {
					x2++;
				} else if (dir == 3) {
					y2--;
				} else if (dir == 4) {
					y2++;
				}
				if (x2 < 0 || x2 >= numField || y2 < 0 || y2 >= numField) {
					x2 = bombNow.x;
					y2 = bombNow.y;
				} else {
					int type = (int) boardNext.data[x2][y2];
					if (Constant.isWall(type) || Constant.isItem(type)) {
						x2 = bombNow.x;
						y2 = bombNow.y;
					}
				}
				bombsNext[index] = new BombEEE(x2, y2, bombNow.owner, life - 1, dir, bombNow.power);
				index++;
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// �V�X�e�b�v�ŐV���ɔz�u���ꂽ���e��ǉ�����B
		/////////////////////////////////////////////////////////////////////////////////////
		if (true) {
			BombEEE[] bombsNowAdded = new BombEEE[numBomb + added.size()];
			BombEEE[] bombsNextAdded = new BombEEE[numBomb + added.size()];
			for (int i = 0; i < numBomb; i++) {
				bombsNowAdded[i] = bombsNow[i];
				bombsNextAdded[i] = bombsNext[i];
			}
			for (int i = 0; i < added.size(); i++) {
				bombsNowAdded[numBomb + i] = added.get(i);
				bombsNextAdded[numBomb + i] = new BombEEE(added.get(i));
				bombsNextAdded[numBomb + i].life--;
			}

			numBomb = numBomb + added.size();
			bombsNow = bombsNowAdded;
			bombsNext = bombsNextAdded;
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// Agent��Bomb���N���X���Ă�������߂��B
		/////////////////////////////////////////////////////////////////////////////////////
		if (true) {
			// Agent���m�ŃN���X����ꍇ
			boolean[] backAgent = new boolean[4];
			for (int ai = 0; ai < 4; ai++) {
				if (absNow[ai].isAlive == false) continue;
				for (int aj = ai + 1; aj < 4; aj++) {
					if (absNow[aj].isAlive == false) continue;
					EEE eee1Now = agentsNow[ai];
					EEE eee2Now = agentsNow[aj];
					EEE eee1Next = agentsNext[ai];
					EEE eee2Next = agentsNext[aj];
					if (eee1Now.isSamePosition(eee2Next) && eee2Now.isSamePosition(eee1Next)) {
						backAgent[ai] = true;
						backAgent[aj] = true;
					}
				}
			}

			// Bomb���m�ŃN���X����ꍇ�B
			boolean[] backBomb = new boolean[numBomb];
			for (int bi = 0; bi < numBomb; bi++) {
				for (int bj = bi + 1; bj < numBomb; bj++) {
					EEE eee1Now = bombsNow[bi];
					EEE eee2Now = bombsNow[bj];
					EEE eee1Next = bombsNext[bi];
					EEE eee2Next = bombsNext[bj];
					if (eee1Now.isSamePosition(eee2Next) && eee2Now.isSamePosition(eee1Next)) {
						backBomb[bi] = true;
						backBomb[bj] = true;
					}
				}
			}

			// Agent��Bomb�ŃN���X����ꍇ�BAgent�͈��߂��Ȃ��B
			for (int ai = 0; ai < 4; ai++) {
				if (absNow[ai].isAlive == false) continue;
				for (int j = 0; j < numBomb; j++) {
					EEE eee1Now = agentsNow[ai];
					EEE eee1Next = agentsNext[ai];
					EEE eee2Now = bombsNow[j];
					EEE eee2Next = bombsNext[j];
					if (eee1Now.isSamePosition(eee2Next) && eee2Now.isSamePosition(eee1Next)) {
						backBomb[j] = true;
					}
				}
			}

			// �����߂��K�v�������́A�ʒu�������߂��B
			for (int ai = 0; ai < 4; ai++) {
				if (absNow[ai].isAlive == false) continue;
				if (backAgent[ai]) {
					agentsNext[ai].x = agentsNow[ai].x;
					agentsNext[ai].y = agentsNow[ai].y;
				}
			}
			for (int bi = 0; bi < numBomb; bi++) {
				if (backBomb[bi]) {
					bombsNext[bi].x = bombsNow[bi].x;
					bombsNext[bi].y = bombsNow[bi].y;
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// Agent��Bomb�������ꏊ�Ɍ��������Ƃ��Ă�������߂��B
		/////////////////////////////////////////////////////////////////////////////////////
		MyMatrix occupancyAgent = new MyMatrix(numField, numField);
		MyMatrix occupancyBomb = new MyMatrix(numField, numField);
		{
			for (EEE eee : agentsNext) {
				if (eee == null) continue;
				occupancyAgent.data[eee.x][eee.y]++;
			}

			for (EEE eee : bombsNext) {
				occupancyBomb.data[eee.x][eee.y]++;
			}

			while (true) {
				boolean isChanged = false;

				for (int ai = 0; ai < 4; ai++) {
					if (absNow[ai].isAlive == false) continue;
					EEE eeeNow = agentsNow[ai];
					EEE eeeNext = agentsNext[ai];
					if (eeeNext.isSamePosition(eeeNow)) continue;
					if (occupancyAgent.data[eeeNext.x][eeeNext.y] > 1 || occupancyBomb.data[eeeNext.x][eeeNext.y] > 1) {
						eeeNext.x = eeeNow.x;
						eeeNext.y = eeeNow.y;
						occupancyAgent.data[eeeNext.x][eeeNext.y]++;
						isChanged = true;
					}
				}

				for (int bi = 0; bi < numBomb; bi++) {
					EEE eeeNow = bombsNow[bi];
					EEE eeeNext = bombsNext[bi];
					if (eeeNext.x == eeeNow.x && eeeNext.y == eeeNow.y) continue;
					if (occupancyAgent.data[eeeNext.x][eeeNext.y] > 1 || occupancyBomb.data[eeeNext.x][eeeNext.y] > 1) {
						eeeNext.x = eeeNow.x;
						eeeNext.y = eeeNow.y;
						occupancyBomb.data[eeeNext.x][eeeNext.y]++;
						isChanged = true;
					}
				}

				if (isChanged == false) break;
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// �Փ˂��������Ă���Agent��Bomb�̏���������B
		/////////////////////////////////////////////////////////////////////////////////////
		AgentEEE[] agentsNext2 = new AgentEEE[4];
		BombEEE[] bombsNext2 = new BombEEE[numBomb];
		int[] bomb_kicked_by = new int[numBomb];
		for (int bi = 0; bi < numBomb; bi++) {
			bomb_kicked_by[bi] = -1;
		}

		for (int bi = 0; bi < numBomb; bi++) {
			BombEEE bbbNow = bombsNow[bi];
			BombEEE bbbNext = bombsNext[bi];

			// �ړ��������ꏊ��Agent�����Ȃ��B���Ȃ��ړ��ł���B
			if (occupancyAgent.data[bbbNext.x][bbbNext.y] == 0) continue;

			// �Փˑ���̃G�[�W�F���g��T���B
			int agentIndex = -1;
			for (int j = 0; j < 4; j++) {
				if (absNow[j].isAlive == false) continue;
				if (bbbNext.isSamePosition(agentsNext[j])) {
					agentIndex = j;
					break;
				}
			}
			if (agentIndex == -1) continue;
			AgentEEE aaaNow = agentsNow[agentIndex];
			AgentEEE aaaNext = agentsNext[agentIndex];

			// �G�[�W�F���g�������Ă��Ȃ��ꍇ�B�u���e��V�K�ݒu���Ă��瓮���Ă��Ȃ��P�[�X�v�u���e�������Ă���P�[�X�v�����Ȃ��B
			if (aaaNow.isSamePosition(aaaNext)) {
				if (bbbNow.isSamePosition(bbbNext) == false) {
					// ���e�������Ă�ꍇ�A�~�߂�B
					bombsNext2[bi] = new BombEEE(bbbNow.x, bbbNow.y, bbbNext.owner, bbbNext.life, 0, bbbNext.power);
				}
				continue;
			}

			// �ȍ~�̏����ł́A�G�[�W�F���g�������Ă���O��B

			// �G�[�W�F���g���L�b�N�ł��Ȃ��ꍇ�A�G�[�W�F���g�����e����~����B
			if (absNow[agentIndex].kick == false) {
				bombsNext2[bi] = new BombEEE(bbbNow.x, bbbNow.y, bbbNext.owner, bbbNext.life, 0, bbbNext.power);
				agentsNext2[agentIndex] = new AgentEEE(aaaNow.x, aaaNow.y, aaaNext.agentID);
				continue;
			}

			// �ȍ~�̏����ł́A�G�[�W�F���g�͓����Ă���L�b�N�ł���O��B

			int dir = actions[agentIndex];
			int x2 = aaaNext.x;
			int y2 = aaaNext.y;
			if (dir == 1) {
				x2--;
			} else if (dir == 2) {
				x2++;
			} else if (dir == 3) {
				y2--;
			} else if (dir == 4) {
				y2++;
			}

			boolean kickable = true;
			if (x2 < 0 || x2 >= numField || y2 < 0 || y2 >= numField) {
				kickable = false;
			} else {
				int type = (int) boardNext.data[x2][y2];
				if (occupancyAgent.data[x2][y2] > 0) {
					kickable = false;
				} else if (occupancyBomb.data[x2][y2] > 0) {
					kickable = false;
				} else if (Constant.isWall(type)) {
					kickable = false;
				} else if (Constant.isItem(type)) {
					kickable = false;
				}
			}

			// ���e���L�b�N�ł���Ƃ��́A�L�b�N����B�ł��Ȃ��Ƃ��́A��~����B
			if (kickable) {
				bombsNext2[bi] = new BombEEE(x2, y2, bbbNext.owner, bbbNext.life, dir, bbbNext.power);
				bomb_kicked_by[bi] = agentIndex;
			} else {
				bombsNext2[bi] = new BombEEE(bbbNow.x, bbbNow.y, bbbNext.owner, bbbNext.life, 0, bbbNext.power);
				agentsNext2[agentIndex] = new AgentEEE(aaaNow.x, aaaNow.y, aaaNext.agentID);
			}
		}

		boolean isChanged = false;
		for (int bi = 0; bi < numBomb; bi++) {
			BombEEE bbb = bombsNext2[bi];
			if (bbb == null) continue;
			bombsNext[bi] = bbb;
			occupancyBomb.data[bbb.x][bbb.y]++;
			isChanged = true;
		}

		for (int ai = 0; ai < 4; ai++) {
			AgentEEE aaa = agentsNext2[ai];
			if (aaa == null) continue;
			agentsNext[ai] = aaa;
			occupancyAgent.data[aaa.x][aaa.y]++;
			isChanged = true;
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// �Ƃ肠�����A�����܂ł̎葱���Ŗ����͖����͂������ǁA����ł��������������Ă���ꍇ�́A���̈ʒu�ɖ߂��B
		/////////////////////////////////////////////////////////////////////////////////////
		while (isChanged) {
			isChanged = false;
			for (int ai = 0; ai < 4; ai++) {
				if (absNow[ai].isAlive == false) continue;
				EEE aaaNow = agentsNow[ai];
				EEE aaaNext = agentsNext[ai];
				if (aaaNow.isSamePosition(aaaNext) == false && (occupancyAgent.data[aaaNext.x][aaaNext.y] > 1 || occupancyBomb.data[aaaNext.x][aaaNext.y] > 0)) {
					aaaNext.x = aaaNow.x;
					aaaNext.y = aaaNow.y;
					occupancyAgent.data[aaaNext.x][aaaNext.y]++;
					isChanged = true;
				}
			}

			for (int bi = 0; bi < numBomb; bi++) {
				BombEEE bbbNow = bombsNow[bi];
				BombEEE bbbNext = bombsNext[bi];

				if (bbbNow.isSamePosition(bbbNext) && bomb_kicked_by[bi] == -1) continue;

				if (occupancyAgent.data[bbbNext.x][bbbNext.y] > 1 || occupancyBomb.data[bbbNext.x][bbbNext.y] > 1) {
					bbbNext.x = bbbNow.x;
					bbbNext.y = bbbNow.y;
					bbbNext.dir = 0;
					occupancyBomb.data[bbbNext.x][bbbNext.y]++;
					int agentIndex = bomb_kicked_by[bi];
					if (agentIndex != -1) {
						EEE aaaNext = agentsNext[agentIndex];
						EEE aaaNow = agentsNow[agentIndex];
						aaaNext.x = aaaNow.x;
						aaaNext.y = aaaNow.y;
						occupancyAgent.data[aaaNext.x][aaaNext.y]++;
						bomb_kicked_by[bi] = -1;
					}
					isChanged = true;
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// agentsNext�̈ʒu�ŃA�C�e������������A�\�͂ɔ��f������B
		/////////////////////////////////////////////////////////////////////////////////////
		for (int ai = 0; ai < 4; ai++) {
			if (absNow[ai].isAlive == false) continue;
			EEE eeeNext = agentsNext[ai];
			int type = (int) boardNow.data[eeeNext.x][eeeNext.y];
			if (type == Constant.ExtraBomb) {
				absNext[ai].numBombHold++;
				absNext[ai].numMaxBomb++;
			} else if (type == Constant.Kick) {
				absNext[ai].kick = true;
			} else if (type == Constant.IncrRange) {
				absNext[ai].strength++;
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// Flame�̏���
		/////////////////////////////////////////////////////////////////////////////////////
		boolean hasNewExplosions = false;

		for (int bi = 0; bi < numBomb; bi++) {
			BombEEE bbbNext = bombsNext[bi];
			if (bbbNext.life == 0) {
				absNext[bbbNext.owner - 10].numBombHold++;
				FlameCenterEEE fff = new FlameCenterEEE(bbbNext.x, bbbNext.y, 3, bbbNext.power);
				flameCenterNext.add(fff);
				bombsNext[bi] = null;
				BBMUtility.PrintFlame(boardNext, myFlameNext, fff.x, fff.y, fff.power, 1);
				hasNewExplosions = true;
			}
		}

		if (hasNewExplosions) {
			while (true) {
				hasNewExplosions = false;
				for (int bi = 0; bi < numBomb; bi++) {
					BombEEE bbbNext = bombsNext[bi];
					if (bbbNext == null) continue;
					if (myFlameNext.data[bbbNext.x][bbbNext.y] == 1) {
						absNext[bbbNext.owner - 10].numBombHold++;
						FlameCenterEEE fff = new FlameCenterEEE(bbbNext.x, bbbNext.y, 3, bbbNext.power);
						flameCenterNext.add(fff);
						bombsNext[bi] = null;
						BBMUtility.PrintFlame(boardNext, myFlameNext, fff.x, fff.y, fff.power, 1);
						hasNewExplosions = true;
					}
				}
				if (hasNewExplosions == false) break;
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// Agent��Flame�Ɋ������܂�Ă�����E���B
		/////////////////////////////////////////////////////////////////////////////////////
		for (int ai = 0; ai < 4; ai++) {
			Ability abNext = absNext[ai];
			if (abNext.isAlive == false) continue;
			EEE aaaNext = agentsNext[ai];
			if (myFlameNext.data[aaaNext.x][aaaNext.y] == 1) {
				abNext.isAlive = false;
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// ���X�e�b�v�̏�Ԃ����B
		/////////////////////////////////////////////////////////////////////////////////////
		StatusHolder shNext = new StatusHolder(numField);
		for (int ai = 0; ai < 4; ai++) {
			Ability abNext = absNext[ai];
			if (abNext.isAlive == false) continue;
			AgentEEE aaaNext = agentsNext[ai];
			shNext.setAgent(aaaNext.x, aaaNext.y, aaaNext.agentID);
		}

		for (int i = 0; i < numBomb; i++) {
			BombEEE bbbNext = bombsNext[i];
			if (bbbNext == null) continue;
			shNext.setBomb(bbbNext.x, bbbNext.y, bbbNext.owner, bbbNext.life, bbbNext.dir, bbbNext.power);
		}

		for (FlameCenterEEE fffNext : flameCenterNext) {
			shNext.setFlameCenter(fffNext.x, fffNext.y, fffNext.life, fffNext.power);
		}

		for (int x = 0; x < numField; x++) {
			for (int y = 0; y < numField; y++) {
				int type = (int) boardNext.data[x][y];
				if (Constant.isAgent(type) || type == Constant.Bomb || type == Constant.Flames) {
					boardNext.data[x][y] = Constant.Passage;
				}
			}
		}

		for (int i = 0; i < numBomb; i++) {
			BombEEE bbbNext = bombsNext[i];
			if (bbbNext != null) {
				boardNext.data[bbbNext.x][bbbNext.y] = Constant.Bomb;
			}
		}

		for (int ai = 0; ai < 4; ai++) {
			if (absNext[ai].isAlive) {
				AgentEEE aaa = agentsNext[ai];
				boardNext.data[aaa.x][aaa.y] = ai + 10;
			}
		}

		for (int x = 0; x < numField; x++) {
			for (int y = 0; y < numField; y++) {
				if (myFlameNext.data[x][y] == 1) {
					boardNext.data[x][y] = Constant.Flames;
				}
			}
		}

		// System.out.println(shNow);
		// System.out.println(shNext);

		return new Pack(boardNext, absNext, shNext);
	}

	static double timeTotal = 0;
	static double counter = 0;
	static int TakeLearningSampleCounter = 0;

	static List<LearningData> learningDataList = new ArrayList<LearningData>();

	public static class LearningData implements Serializable {
		private static final long serialVersionUID = 379292266174819897L;
		Pack pack;
		double[] aliveCount;
		double[] tryCount;
		int firstAction;

		public LearningData(Pack pack, double[] aliveCount, double[] tryCount, int firstAction) {
			this.pack = pack;
			this.aliveCount = aliveCount;
			this.tryCount = tryCount;
			this.firstAction = firstAction;
		}
	}

	public int Compute(MyMatrix board, Node[][] bombMap, Ability abs[]) throws Exception {

		//////////////////////////////////////////////////////////////////
		// ����0�̏�����Ԃ����߂�B
		//////////////////////////////////////////////////////////////////

		MyMatrix boardNow = new MyMatrix(board);

		StatusHolder shNow = new StatusHolder(numField);
		{
			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					int type = (int) board.data[x][y];
					if (Constant.isAgent(type)) {
						shNow.setAgent(x, y, type);
					}
				}
			}

			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					Node node = bombMap[x][y];
					if (node == null) continue;
					if (node.type == Constant.Bomb) {
						shNow.setBomb(x, y, node.owner, node.lifeBomb, node.moveDirection, node.power);
					} else if (node.type == Constant.Flames) {
						shNow.setFlameCenter(x, y, node.lifeFlameCenter, node.power);
					}
				}
			}
		}

		Ability[] absNow = new Ability[4];
		for (int i = 0; i < 4; i++) {
			absNow[i] = new Ability(abs[i]);
		}

		//////////////////////////////////////////////////////////////////
		// ���߂ɃG�[�W�F���g�𓮂����āA�����̐����m�����v�Z���A�\�����f���̂��߂̃f�[�^�Ƃ��ăt�@�C���ɕۑ�����B
		//////////////////////////////////////////////////////////////////

		if (TakeLearningSampleCounter % 10 == 0) {
			Random rand = new Random();
			int maxDepth = 50;
			int numEpoch = 3000;
			double decayRate = 0.99;

			for (int firstAction = 0; firstAction < 6; firstAction++) {
				double[] aliveCount = new double[4];
				double[] tryCount = new double[4];

				long timeStart = System.currentTimeMillis();
				for (int frame = 0; frame < numEpoch; frame++) {
					double[] points = new double[4];
					double totalPoint = 0;
					Pack pack = new Pack(boardNow, absNow, shNow);
					for (int depth = 0; depth < maxDepth; depth++) {
						int[] actions = { rand.nextInt(6), rand.nextInt(6), rand.nextInt(6), rand.nextInt(6) };
						if (depth == 0) {
							actions[3] = firstAction;
						}
						pack = Step(pack.board, pack.abs, pack.sh, actions);

						double point = Math.pow(decayRate, depth);
						totalPoint += point;
						for (int i = 0; i < 4; i++) {
							if (abs[i].isAlive) {
								if (pack.abs[i].isAlive) {
									points[i] += point;
								}
							}
						}

						// �o�͂���B
						if (false) {
							MyMatrix life = new MyMatrix(numField, numField);
							MyMatrix power = new MyMatrix(numField, numField);
							for (BombEEE bbb : pack.sh.getBombEntry()) {
								life.data[bbb.x][bbb.y] = bbb.life;
								power.data[bbb.x][bbb.y] = bbb.power;
							}
							System.out.println("/////////////////////////////////////////////////////");
							System.out.println("// " + depth);
							System.out.println("/////////////////////////////////////////////////////");
							BBMUtility.printBoard2(pack.board, life, power);
						}
					}
					// �G�[�W�F���g�������Ă邩����ł邩���ׂ�B
					for (int i = 0; i < 4; i++) {
						if (abs[i].isAlive) {
							tryCount[i] += 1;
							aliveCount[i] += points[i] / totalPoint;
						}
					}
				}
				long timeEnd = System.currentTimeMillis();
				double timeDel = (timeEnd - timeStart) * 0.001;
				timeTotal += timeDel;
				System.out.println("timeDel = " + timeDel);

				for (int i = 0; i < 4; i++) {
					double rateAlive = 1.0 * aliveCount[i] / tryCount[i];
					System.out.println("firstAction=" + firstAction + ", i=" + i + ", rateAlive=" + rateAlive);
				}

				Pack pack = new Pack(boardNow, absNow, shNow);
				LearningData ld = new LearningData(pack, aliveCount, tryCount, firstAction);
				learningDataList.add(ld);
			}

			// �t�@�C���ɕۑ�����B
			{
				if (learningDataList.size() > 1000) {
					String filename;
					for (int i = 0;; i++) {
						String filename2 = String.format("data/ld_%05d.dat", i);
						if (new File(filename2).exists() == false) {
							filename = filename2;
							break;
						}
					}
					ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(filename)));
					oos.writeObject(learningDataList);
					oos.flush();
					oos.close();
					learningDataList.clear();
				}
			}
		}
		TakeLearningSampleCounter++;

		return -1;
	}

}
