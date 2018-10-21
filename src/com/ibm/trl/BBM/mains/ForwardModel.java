package com.ibm.trl.BBM.mains;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.trl.BBM.mains.Agent.Ability;
import com.ibm.trl.BBM.mains.StatusHolder.AgentEEE;
import com.ibm.trl.BBM.mains.StatusHolder.BombEEE;
import com.ibm.trl.BBM.mains.StatusHolder.EEE;

import ibm.ANACONDA.Core.MyMatrix;

public class ForwardModel {

	private static final int numField = GlobalParameter.numField;

	static public class Pack implements Serializable {
		private static final long serialVersionUID = -8052436421835761684L;
		MyMatrix board;
		MyMatrix flameLife;
		Ability[] abs;
		StatusHolder sh;

		public Pack(MyMatrix board, MyMatrix flameLife, Ability[] abs, StatusHolder sh) {
			this.board = board;
			this.flameLife = flameLife;
			this.abs = abs;
			this.sh = sh;
		}

		public Pack(Pack pack) {
			this.board = new MyMatrix(pack.board);
			this.flameLife = new MyMatrix(pack.flameLife);
			this.abs = new Ability[4];
			for (int ai = 0; ai < 4; ai++) {
				abs[ai] = new Ability(pack.abs[ai]);
			}
			this.sh = new StatusHolder(pack.sh);
		}

		public void removeAgent(int agentID) {
			AgentEEE aaa = sh.getAgent(agentID);
			if (aaa == null) return;
			BombEEE bbb = sh.getBomb(aaa.x, aaa.y);
			if (bbb == null) {
				board.data[aaa.x][aaa.y] = Constant.Passage;
			} else {
				board.data[aaa.x][aaa.y] = Constant.Bomb;
			}
			sh.removeAgent(agentID);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Pack == false) return false;
			Pack pack = (Pack) obj;

			for (int ai = 0; ai < 4; ai++) {
				if (abs[ai].equals(pack.abs[ai]) == false) return false;
			}

			if (sh.equals(pack.sh) == false) return false;

			try {
				if (board.minus(pack.board).normF() > 0) return false;
				if (flameLife.minus(pack.flameLife).normF() > 0) return false;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			return true;
		}

		@Override
		public String toString() {
			MyMatrix life = new MyMatrix(numField, numField);
			MyMatrix power = new MyMatrix(numField, numField);
			for (BombEEE bbb : sh.getBombEntry()) {
				life.data[bbb.x][bbb.y] = bbb.life;
				power.data[bbb.x][bbb.y] = bbb.power;
			}
			String text = "";
			text += "===================================\n";
			text += "===================================\n";
			text += BBMUtility.printBoard2_str(board, board, life, power);
			text += "===================================\n";
			text += "===================================\n";
			return text;
		}
	}

	public Pack Step(Pack pack, int[] actions) throws Exception {
		return Step(pack.board, pack.flameLife, pack.abs, pack.sh, actions);
	}

	public Pack Step(MyMatrix boardNow, MyMatrix flameLifeNow, Ability absNow[], StatusHolder shNow, int[] actions) throws Exception {

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
		// FlameCenterの時刻を進める。
		/////////////////////////////////////////////////////////////////////////////////////
		// List<FlameCenterEEE> flameCenterNext = new ArrayList<FlameCenterEEE>();
		// for (FlameCenterEEE fffNow : shNow.getFlameCenterEntry()) {
		// if (fffNow.life == 1) continue;
		// FlameCenterEEE fffNext = new FlameCenterEEE(fffNow);
		// fffNext.life--;
		// flameCenterNext.add(fffNext);
		// }

		MyMatrix flameLifeNext = new MyMatrix(flameLifeNow);
		for (int x = 0; x < numField; x++) {
			for (int y = 0; y < numField; y++) {
				if (flameLifeNext.data[x][y] >= 1) {
					flameLifeNext.data[x][y]--;
				}
			}
		}
		// 残っているFrameCenterからMyFlameを作って、boardのFlameで残っている部分があったら。Passageを表示する。
		// for (FlameCenterEEE fffNext : flameCenterNext) {
		// BBMUtility.PrintFlame(boardNext, myFlameNext, fffNext.x, fffNext.y, fffNext.power, 1);
		// }

		// 古いFlameCenterをレンダリングすると、フレーム終端が木だったのかが分からない。BoardNowとMyFlameNextの共通部分が今残っているFlameになる。その処理をする。
		for (int x = 0; x < numField; x++) {
			for (int y = 0; y < numField; y++) {
				if (flameLifeNext.data[x][y] == 0) {
					// myFlame側でFlameが消滅したのに、board側でFlamesが残っていたら、boardのFlamesをPassageにする。
					if (boardNext.data[x][y] == Constant.Flames) {
						boardNext.data[x][y] = Constant.Passage;
					}
				} else {
					// myFlame側でFlameが発生してるのに、board側でFlamesが消えていたら、myFlame側をクリアする。
					if (boardNext.data[x][y] != Constant.Flames) {
						flameLifeNext.data[x][y] = 0;
						// TODO ここは到達し得ない？
						System.out.println("Forward Model: error??");
					}
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// AgentとBombの移動の処理。
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
						// 爆弾を追加。
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
		// 新ステップで新たに配置された爆弾を追加する。
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
		// AgentとBombがクロスしてたら引き戻す。
		/////////////////////////////////////////////////////////////////////////////////////
		if (true) {
			// Agent同士でクロスする場合
			boolean[] backAgent = new boolean[4];
			for (int ai = 0; ai < 4; ai++) {
				if (absNow[ai].isAlive == false) continue;
				if (agentsNow[ai] == null) continue;
				for (int aj = ai + 1; aj < 4; aj++) {
					if (absNow[aj].isAlive == false) continue;
					if (agentsNow[aj] == null) continue;
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

			// Bomb同士でクロスする場合。
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

			// AgentとBombでクロスする場合。Agentは引戻さない。
			for (int ai = 0; ai < 4; ai++) {
				if (absNow[ai].isAlive == false) continue;
				if (agentsNow[ai] == null) continue;
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

			// 引き戻す必要があるやつは、位置を引き戻す。
			for (int ai = 0; ai < 4; ai++) {
				if (absNow[ai].isAlive == false) continue;
				if (agentsNow[ai] == null) continue;
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
		// AgentとBombが同じ場所に向かおうとしてたら引き戻す。
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
					if (agentsNow[ai] == null) continue;
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
		// 衝突が発生しているAgentとBombの処理をする。
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

			// 移動したい場所にAgentがいない。問題なく移動できる。
			if (occupancyAgent.data[bbbNext.x][bbbNext.y] == 0) continue;

			// 衝突相手のエージェントを探す。
			int agentIndex = -1;
			for (int aj = 0; aj < 4; aj++) {
				if (absNow[aj].isAlive == false) continue;
				if (agentsNow[aj] == null) continue;
				if (bbbNext.isSamePosition(agentsNext[aj])) {
					agentIndex = aj;
					break;
				}
			}
			if (agentIndex == -1) continue;
			AgentEEE aaaNow = agentsNow[agentIndex];
			AgentEEE aaaNext = agentsNext[agentIndex];

			// エージェントが動いていない場合。「爆弾を新規設置してから動いていないケース」「爆弾が動いているケース」しかない。
			if (aaaNow.isSamePosition(aaaNext)) {
				if (bbbNow.isSamePosition(bbbNext) == false) {
					// 爆弾が動いてる場合、止める。
					bombsNext2[bi] = new BombEEE(bbbNow.x, bbbNow.y, bbbNext.owner, bbbNext.life, 0, bbbNext.power);
				}
				continue;
			}

			// 以降の処理では、エージェントが動いている前提。

			// エージェントがキックできない場合、エージェントも爆弾も停止する。
			if (absNow[agentIndex].kick == false) {
				bombsNext2[bi] = new BombEEE(bbbNow.x, bbbNow.y, bbbNext.owner, bbbNext.life, 0, bbbNext.power);
				agentsNext2[agentIndex] = new AgentEEE(aaaNow.x, aaaNow.y, aaaNext.agentID);
				continue;
			}

			// 以降の処理では、エージェントは動いておりキックできる前提。

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

			// 爆弾がキックできるときは、キックする。できないときは、停止する。
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
		// とりあえず、ここまでの手続きで矛盾は無いはずだけど、それでも矛盾が発生している場合は、元の位置に戻す。
		/////////////////////////////////////////////////////////////////////////////////////
		while (isChanged) {
			isChanged = false;
			for (int ai = 0; ai < 4; ai++) {
				if (absNow[ai].isAlive == false) continue;
				if (agentsNow[ai] == null) continue;
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
		// agentsNextの位置でアイテムがあったら、能力に反映させる。
		/////////////////////////////////////////////////////////////////////////////////////
		for (int ai = 0; ai < 4; ai++) {
			if (absNow[ai].isAlive == false) continue;
			if (agentsNow[ai] == null) continue;
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
		// Flameの処理
		/////////////////////////////////////////////////////////////////////////////////////
		for (int bi = 0; bi < numBomb; bi++) {
			BombEEE bbbNext = bombsNext[bi];
			if (bbbNext.life == 0) {
				if (bbbNext.owner != -1) absNext[bbbNext.owner - 10].numBombHold++;
				// FlameCenterEEE fff = new FlameCenterEEE(bbbNext.x, bbbNext.y, 3, bbbNext.power);
				// flameCenterNext.add(fff);
				bombsNext[bi] = null;
				BBMUtility.PrintFlame(boardNext, flameLifeNext, bbbNext.x, bbbNext.y, bbbNext.power, 3);
			}
		}

		while (true) {
			boolean hasNewExplosions = false;
			for (int bi = 0; bi < numBomb; bi++) {
				BombEEE bbbNext = bombsNext[bi];
				if (bbbNext == null) continue;
				if (flameLifeNext.data[bbbNext.x][bbbNext.y] > 0) {
					if (bbbNext.owner != -1) absNext[bbbNext.owner - 10].numBombHold++;
					// FlameCenterEEE fff = new FlameCenterEEE(bbbNext.x, bbbNext.y, 3, bbbNext.power);
					// flameCenterNext.add(fff);
					bombsNext[bi] = null;
					BBMUtility.PrintFlame(boardNext, flameLifeNext, bbbNext.x, bbbNext.y, bbbNext.power, 3);
					hasNewExplosions = true;
				}
			}
			if (hasNewExplosions == false) break;
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// AgentがFlameに巻き込まれていたら殺す。
		/////////////////////////////////////////////////////////////////////////////////////
		for (int ai = 0; ai < 4; ai++) {
			Ability abNext = absNext[ai];
			if (abNext.isAlive == false) continue;
			if (agentsNow[ai] == null) continue;
			EEE aaaNext = agentsNext[ai];
			if (flameLifeNext.data[aaaNext.x][aaaNext.y] > 0) {
				abNext.isAlive = false;
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// 次ステップの状態を作る。
		/////////////////////////////////////////////////////////////////////////////////////
		StatusHolder shNext = new StatusHolder();
		for (int ai = 0; ai < 4; ai++) {
			Ability abNext = absNext[ai];
			if (abNext.isAlive == false) continue;
			if (agentsNow[ai] == null) continue;
			AgentEEE aaaNext = agentsNext[ai];
			shNext.setAgent(aaaNext.x, aaaNext.y, aaaNext.agentID);
		}

		for (int i = 0; i < numBomb; i++) {
			BombEEE bbbNext = bombsNext[i];
			if (bbbNext == null) continue;
			shNext.setBomb(bbbNext.x, bbbNext.y, bbbNext.owner, bbbNext.life, bbbNext.dir, bbbNext.power);
		}

		// for (FlameCenterEEE fffNext : flameCenterNext) {
		// shNext.setFlameCenter(fffNext.x, fffNext.y, fffNext.life, fffNext.power);
		// }

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
			if (absNext[ai].isAlive == false) continue;
			if (agentsNow[ai] == null) continue;
			AgentEEE aaa = agentsNext[ai];
			boardNext.data[aaa.x][aaa.y] = ai + 10;
		}

		for (int x = 0; x < numField; x++) {
			for (int y = 0; y < numField; y++) {
				if (flameLifeNext.data[x][y] > 0) {
					boardNext.data[x][y] = Constant.Flames;
				}
			}
		}

		// TODO ログ
		if (false) {
			MyMatrix lifeNext = new MyMatrix(numField, numField);
			MyMatrix powerNext = new MyMatrix(numField, numField);
			for (int i = 0; i < numBomb; i++) {
				BombEEE bbbNext = bombsNext[i];
				if (bbbNext == null) continue;
				lifeNext.data[bbbNext.x][bbbNext.y] = bbbNext.life;
				powerNext.data[bbbNext.x][bbbNext.y] = bbbNext.power;
			}

			System.out.println("==========================================");
			System.out.println("==========================================");
			System.out.println("board picture");
			BBMUtility.printBoard2(boardNext, boardNext, lifeNext, powerNext);
		}

		return new Pack(boardNext, flameLifeNext, absNext, shNext);
	}
}
