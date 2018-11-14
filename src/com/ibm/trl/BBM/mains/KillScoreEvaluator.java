/********************************************
 * (C) Copyright IBM Corp. 2018
 ********************************************/
package com.ibm.trl.BBM.mains;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.ibm.trl.BBM.mains.ForwardModel.Pack;
import com.ibm.trl.BBM.mains.StatusHolder.AgentEEE;
import com.ibm.trl.BBM.mains.StatusHolder.BombEEE;

public class KillScoreEvaluator {

	static final Random rand = new Random();
	static final int numThread = GlobalParameter.numThread;
	static final int numField = GlobalParameter.numField;
	static final ForwardModel fm = new ForwardModel();
	static int[] phi = new int[45 * 45];

	static {
		try {
			learn();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static int index(Pack pack, int ai) throws Exception {
		AgentEEE[] agents = new AgentEEE[4];
		for (AgentEEE aaa : pack.sh.getAgentEntry()) {
			if (aaa == null) continue;
			agents[aaa.agentID - 10] = aaa;
		}

		AgentEEE target = agents[ai];
		if (target == null) return -1;

		////////////////////////////////////////////////////////////////////////////////////
		// ���͂�Wall�̐���2��3�̎��ȊO�͏��O����B
		////////////////////////////////////////////////////////////////////////////////////
		boolean[] isWall = new boolean[5];
		{
			int numWall = 0;
			int x = target.x;
			int y = target.y;
			for (int[] vec : GlobalParameter.onehopList) {
				int dir = vec[0];
				int dx = vec[1];
				int dy = vec[2];
				if (dir == 0) continue;
				int x2 = x + dx;
				int y2 = y + dy;
				if (x2 < 0 || x2 >= numField || y2 < 0 || y2 >= numField) {
					isWall[dir] = true;
					numWall++;
				} else {
					int type = (int) pack.board.data[x2][y2];
					if (Constant.isWall(type)) {
						isWall[dir] = true;
						numWall++;
					}
				}
			}
			if (numWall != 2 && numWall != 3) return -1;
		}

		BombEEE[][] bombMap = new BombEEE[numField][numField];
		for (BombEEE bbb : pack.sh.getBombEntry()) {
			bombMap[bbb.x][bbb.y] = bbb;
		}

		////////////////////////////////////////////////////////////////////////////////////
		// �G��l��T���B
		////////////////////////////////////////////////////////////////////////////////////
		// TODO �ǂ��Agent�z��ł������悤�ɏC�����ׂ��B
		int enemy1 = -1, enemy2 = -1;
		if (ai == 0) {
			enemy1 = 1;
			enemy2 = 3;
		} else if (ai == 1) {
			enemy1 = 0;
			enemy2 = 2;
		} else if (ai == 2) {
			enemy1 = 1;
			enemy2 = 3;
		} else if (ai == 3) {
			enemy1 = 0;
			enemy2 = 2;
		}
		int[] enemies = new int[] { enemy1, enemy2 };

		////////////////////////////////////////////////////////////////////////////////////
		// �^�[�Q�b�g�̎l���𒲂ׂ�B
		////////////////////////////////////////////////////////////////////////////////////
		int[][] ids = new int[5][2];
		if (true) {
			int x = target.x;
			int y = target.y;
			for (int dis = 1; dis <= 2; dis++) {
				for (int[] vec : GlobalParameter.onehopList) {
					int dir = vec[0];
					int dx = vec[1];
					int dy = vec[2];
					if (dir == 0) continue;
					int x2 = x + dx * dis;
					int y2 = y + dy * dis;
					int id = 0;
					if (x2 < 0 || x2 >= numField || y2 < 0 || y2 >= numField) {
						id = 1;
					} else {
						int type = (int) pack.board.data[x2][y2];
						BombEEE bbb = bombMap[x2][y2];
						if (Constant.isWall(type)) {
							id = 1;
						} else if (bbb != null && bbb.dir == 0) {
							id = 2;
						}
					}
					ids[dir][dis - 1] = id;
				}
			}

			for (int dir = 0; dir < 5; dir++) {
				if (ids[dir][0] == 1) {
					ids[dir][1] = 1;
				}
			}
		}

		////////////////////////////////////////////////////////////////////////////////////
		// �l���̒ʘH�ɑ΂��āA�G�G�[�W�F���g���ǂ̕�����S�����邩�R�t����B
		// �G�G�[�W�F���g���Ή��ł�������E�����̃��X�g�����B
		////////////////////////////////////////////////////////////////////////////////////
		int[][][] tantouList = new int[2][3][2];
		int x = target.x;
		int y = target.y;
		for (int i = 0; i < 2; i++) {
			int enemy = enemies[i];
			AgentEEE aaa = agents[enemy];

			int count = 0;
			if (aaa != null) {
				for (int[] vec : GlobalParameter.onehopList) {
					int dir = vec[0];
					int dx = vec[1];
					int dy = vec[2];
					if (dir == 0) continue;
					if (isWall[dir]) continue;
					for (int dis = 1; dis <= 2; dis++) {
						int x2 = x + dx * dis;
						int y2 = y + dy * dis;
						int ddd = Math.abs(aaa.x - x2) + Math.abs(aaa.y - y2);
						if (ddd == 0) {
							if (dis == 1) {
								tantouList[i][count][0] = dir;
								tantouList[i][count][1] = 0;
								count++;
							} else if (dis == 2) {
								tantouList[i][count][0] = dir;
								tantouList[i][count][1] = 1;
								count++;
							}
						}
					}
				}

				if (count == 0) {
					for (int[] vec : GlobalParameter.onehopList) {
						int dir = vec[0];
						int dx = vec[1];
						int dy = vec[2];
						if (dir == 0) continue;
						if (isWall[dir]) continue;
						for (int dis = 1; dis <= 2; dis++) {
							int x2 = x + dx * dis;
							int y2 = y + dy * dis;
							int ddd = Math.abs(aaa.x - x2) + Math.abs(aaa.y - y2);
							if (ddd == 1) {
								if (dis == 1) {
									tantouList[i][count][0] = dir;
									tantouList[i][count][1] = 2;
									count++;
								} else if (dis == 2) {
									tantouList[i][count][0] = dir;
									tantouList[i][count][1] = 3;
									count++;
								}
							}
						}
					}
				}
			}

			if (count == 0) {
				tantouList[i][count][0] = 100;
				count++;
			}
		}

		////////////////////////////////////////////////////////////////////////////////////
		// �S����������E�����̑g�ɑ΂��āA�����ʂ��v�Z����B
		////////////////////////////////////////////////////////////////////////////////////
		for (int[] tantou1 : tantouList[0]) {
			int dir1 = tantou1[0];
			int pos1 = tantou1[1];
			if (dir1 == 0) break;
			for (int[] tantou2 : tantouList[1]) {
				int dir2 = tantou2[0];
				int pos2 = tantou2[1];
				if (dir2 == 0) break;
				if (dir1 != 100 && dir1 == dir2) continue;

				// �^����ꂽ�G�[�W�F���g�ʒu�A���͊��ŁAIndex���v�Z����B
				int[] apos = new int[] { 4, 4, 4, 4, 4 };
				if (dir1 != 100) apos[dir1] = pos1;
				if (dir2 != 100) apos[dir2] = pos2;

				int[] nonwalldirList = new int[] { -1, -1 };
				int count = 0;
				for (int dir = 1; dir < 5; dir++) {
					if (ids[dir][0] == 1) continue;
					nonwalldirList[count] = dir;
					count++;
				}

				// dirList�̕��p�ɑ΂��āAids, apos��������ʂ����B
				int[][] stateList = new int[2][3];
				for (int i = 0; i < 2; i++) {
					int dir = nonwalldirList[i];
					if (dir == -1) {
						stateList[i] = new int[] { 1, 1, 4 };
					} else {
						stateList[i] = new int[] { ids[dir][0], ids[dir][1], apos[dir] };
					}
				}
				int index = state2index(stateList[0], stateList[1]);
				return index;
			}
		}

		return -1;
	}

	private static int state2index(int[] state1, int[] state2) {
		int[][] temp = new int[2][];
		temp[0] = state1;
		temp[1] = state2;
		int index = 0;
		for (int i = 0; i < 2; i++) {
			int type1 = temp[i][0];
			int type2 = temp[i][1];
			int apos = temp[i][2];
			int t = apos * 9 + type1 * 3 + type2;
			index = index * 45 + t;
		}
		return index;
	}

	private static List<int[]> generateNextState(int[] state_now) {
		List<int[]> state_next = new ArrayList<int[]>();
		{
			int type1 = state_now[0];
			int type2 = state_now[1];
			int apos = state_now[2];

			if (apos == 0) {

				// �O�Ɉړ�����B
				state_next.add(new int[] { type1, type2, 2 });

				// ��2�Z���Ɉړ�����B
				if (type2 == 0) state_next.add(new int[] { type1, type2, 1 });

				// ���e��u��
				if (type1 == 0) state_next.add(new int[] { 2, type2, apos });

			} else if (apos == 1) {

				// �O�Ɉړ�����B
				state_next.add(new int[] { type1, type2, 3 });

				// ��1�Z���Ɉړ�����B
				if (type1 == 0) state_next.add(new int[] { type1, type2, 0 });

				// ���e��u��
				if (type2 == 0) state_next.add(new int[] { type1, 2, apos });

			} else if (apos == 2) {

				// �O�Ɉړ�����B
				state_next.add(new int[] { type1, type2, 4 });

				// ��1�Z���Ɉړ�����B
				if (type1 == 0) state_next.add(new int[] { type1, type2, 0 });

			} else if (apos == 3) {

				// �O�Ɉړ�����B
				state_next.add(new int[] { type1, type2, 4 });

				// ��2�Z���Ɉړ�����B
				if (type2 == 0) state_next.add(new int[] { type1, type2, 1 });

			}
		}

		List<int[]> next2 = new ArrayList<int[]>();
		for (int[] state : state_next) {
			if (isTsume(state) == false) continue;
			next2.add(state);
		}

		return next2;
	}

	private static boolean isTsume(int[] state) {
		int type1 = state[0];
		int type2 = state[1];
		int apos = state[2];

		// Wall�̏�ɃG�[�W�F���g�������炨�������B
		if (type1 == 1 && apos == 0) return false;
		if (type2 == 1 && apos == 1) return false;

		// �אڃZ�����󔒂�������l�߂Ă��Ԃ���Ȃ��̂ŁA���������B
		if (type1 == 0) {
			if (type2 == 0) {
				// ����
				if (apos == 0) return true;
				if (apos == 1) return true;
				if (apos == 2) return true;
				if (apos == 3) return false;
				if (apos == 4) return false;
			} else if (type2 == 1) {
				// ����
				if (apos == 0) return true;
				if (apos == 1) return false;
				if (apos == 2) return true;
				if (apos == 3) return false;
				if (apos == 4) return false;
			} else if (type2 == 2) {
				// ����
				if (apos == 0) return true;
				if (apos == 1) return false;
				if (apos == 2) return true;
				if (apos == 3) return false;
				if (apos == 4) return false;
			}
		} else if (type1 == 1) {
			if (type2 == 0) {
				// ����
				return true;
			} else if (type2 == 1) {
				// ����
				return true;
			} else if (type2 == 2) {
				// ����
				return true;
			}
		} else if (type1 == 2) {
			if (type2 == 0) {
				// ����
				if (apos == 0) return true;
				if (apos == 1) return true;
				if (apos == 2) return false;
				if (apos == 3) return true;
				if (apos == 4) return false;
			} else if (type2 == 1) {
				// ����
				return true;
			} else if (type2 == 2) {
				// ����
				return true;
			}
		}
		return true;
	}

	public static void learn() throws Exception {

		for (int i = 0; i < phi.length; i++) {
			phi[i] = Integer.MAX_VALUE;
		}

		// �S�[���̑J�ڐ���0�ɐݒ肷��B
		List<int[]> state_comp = new ArrayList<int[]>();
		state_comp.add(new int[] { 2, 2, 4 });
		state_comp.add(new int[] { 2, 1, 4 });
		for (int type2 = 0; type2 < 3; type2++) {
			for (int apos = 0; apos < 5; apos++) {
				state_comp.add(new int[] { 1, type2, apos });
			}
		}
		for (int[] s1 : state_comp) {
			for (int[] s2 : state_comp) {
				int index = state2index(s1, s2);
				phi[index] = 0;
			}
		}

		List<int[]> stateList = new ArrayList<int[]>();
		for (int type1 = 0; type1 < 3; type1++) {
			for (int type2 = 0; type2 < 3; type2++) {
				for (int apos = 0; apos < 5; apos++) {
					int[] state = new int[] { type1, type2, apos };
					// ��ԂƂ��Ă��蓾�邩���ׂ�B
					if (isTsume(state) == false) continue;
					stateList.add(state);
				}
			}
		}

		// �e��Ԃ���1�X�e�b�v����������āA�L���ȑJ�ډ񐔂ŋl�߂����ԂɑJ�ڂ�����A���ڏ�Ԃ̑J�ډ񐔂�Update����B
		while (true) {
			boolean changed = false;
			for (int[] state1 : stateList) {
				for (int[] state2 : stateList) {
					int indexNow = state2index(state1, state2);
					int phiNow = phi[indexNow];

					// if (indexNow == 445) {
					// int temp = 0;
					// System.out.println(temp);
					// }

					System.out.println(state1[0] + ", " + state1[1] + ", " + state1[2] + ", " + state2[0] + ", " + state2[1] + ", " + state2[2] + ", " + indexNow + ", " + phiNow);

					// s1�𓮂����B
					{
						List<int[]> nextList = generateNextState(state1);
						for (int[] state_next : nextList) {
							int indexNext = state2index(state_next, state2);
							if (phi[indexNext] != Integer.MAX_VALUE && phi[indexNext] + 1 < phi[indexNow]) {
								phi[indexNow] = phi[indexNext] + 1;
								changed = true;
							}
						}
					}

					// s2�𓮂����B
					{
						List<int[]> nextList = generateNextState(state2);
						for (int[] state_next : nextList) {
							int indexNext = state2index(state1, state_next);
							if (phi[indexNext] != Integer.MAX_VALUE && phi[indexNext] + 1 < phi[indexNow]) {
								phi[indexNow] = phi[indexNext] + 1;
								changed = true;
							}
						}
					}

					System.out.println(state1[0] + ", " + state1[1] + ", " + state1[2] + ", " + state2[0] + ", " + state2[1] + ", " + state2[2] + ", " + indexNow + ", " + phiNow);

					if (state1[0] == 1 && state1[1] == 1 && state1[2] == 4) {
						if (state2[0] == 2 && state2[1] == 0 && state2[2] == 1) {
							int temp = 0;
							System.out.println(temp);
						}
					}

				}
			}
			if (changed == false) {
				break;
			}
		}
	}

	static public int computeKillScore(Pack pack, int ai) throws Exception {
		int index = index(pack, ai);
		if (index == -1) return Integer.MAX_VALUE;
		return phi[index];
	}
}
