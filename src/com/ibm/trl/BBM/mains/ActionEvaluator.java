package com.ibm.trl.BBM.mains;

import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

import com.ibm.trl.BBM.mains.Agent.Ability;
import com.ibm.trl.BBM.mains.BombTracker.Node;
import com.ibm.trl.BBM.mains.ForwardModel.Pack;
import com.ibm.trl.BBM.mains.StatusHolder.AgentEEE;

import ibm.ANACONDA.Core.MatrixUtility;
import ibm.ANACONDA.Core.MyMatrix;

public class ActionEvaluator {
	static Random rand = new Random();
	static NormalDistribution nd = new NormalDistribution();
	static final int numField = GlobalParameter.numField;
	static final boolean verbose = GlobalParameter.verbose;
	double worstScoreThreshold = Math.log(4.5);
	double attackThreshold = -5;

	/**
	 * �A�N�V���������肷��B
	 */
	public int ComputeOptimalAction(int me, int friend, int maxPower, Ability[] abs, MapInformation map, BombTracker.Node[][] bombMap, MyMatrix flameLife, double[][][][] worstScores)
			throws Exception {

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// ��{�ϐ��̌v�Z
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// TODO
		worstScoreThreshold = 1.3;

		MyMatrix board = map.board;

		AgentEEE[] agentsNow = new AgentEEE[4];
		for (int x = 0; x < numField; x++) {
			for (int y = 0; y < numField; y++) {
				int type = map.getType(x, y);
				if (Constant.isAgent(type)) {
					AgentEEE aaa = new AgentEEE(x, y, type);
					agentsNow[aaa.agentID - 10] = aaa;
				}
			}
		}

		AgentEEE agentMe = agentsNow[me - 10];

		Ability ab = abs[me - 10];

		// �����̈ʒu����A�e�Z���ւ̈ړ��������v�Z���Ă����B
		MyMatrix dis = BBMUtility.ComputeOptimalDistance(board, agentMe.x, agentMe.y, Integer.MAX_VALUE);

		int numVisibleTeam = 0;
		int numVisibleEnemy = 0;
		for (int ai = 0; ai < 4; ai++) {
			AgentEEE aaa = agentsNow[ai];
			if (aaa == null) continue;
			if (ai == me - 10) {
				numVisibleTeam++;
			} else if (ai == friend - 10) {
				numVisibleTeam++;
			} else {
				numVisibleEnemy++;
			}
		}

		int numAliveTeam = 0;
		int numAliveEnemy = 0;
		for (int ai = 0; ai < 4; ai++) {
			if (abs[ai].isAlive == false) continue;
			if (ai == me - 10) {
				numAliveTeam++;
			} else if (ai == friend - 10) {
				numAliveTeam++;
			} else {
				numAliveEnemy++;
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// safetyScore���v�Z����B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		double[][] safetyScore = new double[4][6];
		for (int ai = 0; ai < 4; ai++) {
			if (ai == me - 10 || ai == friend - 10) {

				int numzero = 6;

				// �����̃X�R�A�͍ň��P�[�X�Ō��ς���B
				if (numzero == 6) {
					for (int a = 0; a < 6; a++) {
						double min = Double.POSITIVE_INFINITY;
						for (int b = 0; b < 6; b++) {
							double num = worstScores[a][b][ai][3];
							if (num == 0) continue;
							double temp = worstScores[a][b][ai][1];
							if (temp < min) {
								min = temp;
							}
						}
						if (min == Double.POSITIVE_INFINITY) min = Double.NEGATIVE_INFINITY;
						safetyScore[ai][a] = min;
					}
				}

				numzero = 0;
				for (int a = 0; a < 6; a++) {
					if (safetyScore[ai][a] == Double.NEGATIVE_INFINITY) numzero++;
				}

				// �ň��P�[�X���S��0�Ȃ�A���傤���Ȃ��̂ŕ��ςŌ��ς���B
				if (numzero == 6) {
					System.out.println("�ň��P�[�X�ł͕K�����ʂ̂ŕ��ς��g���B");
					for (int a = 0; a < 6; a++) {
						double sum = Double.NEGATIVE_INFINITY;
						double count = 0;
						for (int b = 0; b < 6; b++) {
							double num = worstScores[a][b][ai][3];
							if (num == 0) continue;
							sum = BBMUtility.add_log(sum, worstScores[a][b][ai][0]);
							count += num;
						}
						double ave;
						if (count == 0) {
							ave = Double.NEGATIVE_INFINITY;
						} else {
							ave = sum - Math.log(count);
						}
						safetyScore[ai][a] = ave;
					}
				}

				numzero = 0;
				for (int a = 0; a < 6; a++) {
					if (safetyScore[ai][a] == Double.NEGATIVE_INFINITY) numzero++;
				}

				if (ai == me && numzero == 6) {
					System.out.println("���Ŏ��ʂ�");
				}
			} else {
				// �����ȊO�̃X�R�A�́A���ςŌ��ς���B
				for (int a = 0; a < 6; a++) {
					double sum = Double.NEGATIVE_INFINITY;
					double count = 0;
					for (int b = 0; b < 6; b++) {
						double num = worstScores[a][b][ai][3];
						if (num == 0) continue;
						sum = BBMUtility.add_log(sum, worstScores[a][b][ai][0]);
						count += num;
					}
					double ave;
					if (count == 0) {
						ave = Double.NaN;
					} else {
						ave = sum - Math.log(count);
					}
					safetyScore[ai][a] = ave;
				}
			}
		}

		if (verbose) {
			System.out.println("==============================");
			System.out.println("==============================");
			MatrixUtility.OutputMatrix(new MyMatrix(safetyScore));
			System.out.println("==============================");
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �A�N�V�������胋�[�`��
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		int action_final = -1;
		String reason = "�Ȃ�";

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �Q�F�P�̂Ƃ��ɁA�����������݂ł���ꍇ�́A���s����B(^o^)/
		// �P��
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (action_final == -1 && numAliveTeam == 2 && numAliveEnemy == 1) {
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (ai == friend - 10) continue;

				double max = Double.NEGATIVE_INFINITY;
				double min = Double.POSITIVE_INFINITY;
				int amin = -1;
				for (int a = 0; a < 6; a++) {
					double sen = safetyScore[ai][a];
					if (Double.isNaN(sen)) continue;
					if (sen < min) {
						min = sen;
						amin = a;
					}
					if (sen > max) {
						max = sen;
					}
				}

				if (amin != -1 && min == Double.NEGATIVE_INFINITY && max != Double.NEGATIVE_INFINITY) {
					action_final = amin;
					reason = "��������B(^o^)/";
					System.out.println("��������B(^o^)/" + action_final);
					System.out.println("�I�I�I");
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �S�s���y�A�Ō��āA�Е��i�܂��͗����j���m���ɂȂ�ꍇ�A��l�����čőP������B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		if (action_final == -1 && numVisibleTeam == 2) {
			double max = Double.NEGATIVE_INFINITY;
			int amax = -1;
			int bmax = -1;
			double aaa = 0;
			double bbb = 0;
			int count = 0;
			for (int a = 0; a < 6; a++) {
				for (int b = 0; b < 6; b++) {
					double sme = worstScores[a][b][me - 10][1];
					double sfr = worstScores[a][b][friend - 10][1];
					double nme = worstScores[a][b][me - 10][3];
					double nfr = worstScores[a][b][friend - 10][3];
					if (nme == 0) continue;
					if (nfr == 0) continue;
					count++;
					double temp = Math.min(sme, sfr);
					if (temp > max) {
						max = temp;
						amax = a;
						bmax = b;
						aaa = sme;
						bbb = sfr;
					}
				}
			}

			if (count > 0 && max == Double.NEGATIVE_INFINITY) {
				System.out.println("�I�I�I");
			}

			if (amax != -1 && bmax != -1 && max < -20) {
				action_final = amax;
				reason = "�ӂ���Ƃ���΂��I";
				System.out.println("�ӂ���Ƃ���΂��Ha=" + amax + ", b=" + bmax);
				System.out.println("�I�I�I");
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �l�߂����Ԃł���΁A�l�߂�B
		// TODO �l�߂�v�����N�Ȃ̂���Ԃ��悤�ɂ������B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		if (action_final == -1) {
			Pack packNow;
			if (true) {
				Ability[] abs2 = new Ability[4];
				for (int ai = 0; ai < 4; ai++) {
					abs2[ai] = new Ability(abs[ai]);
					if (ai + 10 == me) continue;
					abs2[ai].kick = true;
					abs2[ai].numMaxBomb = 3;
					abs2[ai].numBombHold = 3;
					if (abs2[ai].strength_fix == -1) {
						abs2[ai].strength = maxPower;
					} else {
						abs2[ai].strength = abs2[ai].strength_fix;
					}
				}

				StatusHolder sh = new StatusHolder(numField);
				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {
						int type = map.getType(x, y);
						if (Constant.isAgent(type)) {
							sh.setAgent(x, y, type);
						}
					}
				}

				for (int x = 0; x < numField; x++) {
					for (int y = 0; y < numField; y++) {
						Node node = bombMap[x][y];
						if (node == null) continue;
						// TODO �Ƃ肠������~���Ă锚�e�����l������B
						if (node.dirs[0] == false) continue;
						sh.setBomb(x, y, -1, node.life, 0, node.power);
					}
				}

				packNow = new Pack(map.board, flameLife, abs2, sh);
			}

			int killScoreMin = Integer.MAX_VALUE;
			int aiTarget = -1;
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (ai == friend - 10) continue;

				int killScore = KillScoreEvaluator.computeKillScore(packNow, ai);
				if (killScore < Integer.MAX_VALUE) {
					killScoreMin = killScore;
					aiTarget = ai;
					System.out.println("�l�߂�I�I");
					System.out.println("�I�I�I");
					break;
				}
			}

			if (aiTarget != -1) {
				ForwardModel fm = new ForwardModel();
				if (numVisibleTeam == 2) {
					int amin = -1;
					int bmin = -1;
					for (int a = 0; a < 6; a++) {
						for (int b = 0; b < 6; b++) {
							double sme = worstScores[a][b][me - 10][1];
							double sfr = worstScores[a][b][friend - 10][1];
							double nme = worstScores[a][b][me - 10][3];
							double nfr = worstScores[a][b][friend - 10][3];
							if (nme == 0) continue;
							if (nfr == 0) continue;
							if (sme < attackThreshold) continue;
							if (sfr < attackThreshold) continue;
							int[] actions = new int[4];
							actions[me - 10] = a;
							actions[friend - 10] = b;
							Pack packNext = fm.Step(packNow.board, packNow.flameLife, packNow.abs, packNow.sh, actions);
							int temp = KillScoreEvaluator.computeKillScore(packNext, aiTarget);
							if (temp < killScoreMin) {
								killScoreMin = temp;
								amin = a;
								bmin = b;
							}
						}
					}
					if (amin != -1 && bmin != -1) {
						action_final = amin;
						reason = "���l�߂�B";
						System.out.println("���l�߂�B�I�I�I" + action_final);
					}
				} else if (numVisibleTeam == 1) {
					int amin = -1;
					for (int a = 0; a < 6; a++) {
						double sme = safetyScore[me - 10][a];
						if (sme < attackThreshold) continue;
						int[] actions = new int[4];
						actions[me - 10] = a;
						Pack packNext = fm.Step(packNow.board, packNow.flameLife, packNow.abs, packNow.sh, actions);
						int temp = KillScoreEvaluator.computeKillScore(packNext, aiTarget);
						if (temp < killScoreMin) {
							killScoreMin = temp;
							amin = a;
						}
					}
					if (amin != -1) {
						action_final = amin;
						reason = "���l�߂�B";
						System.out.println("���l�߂�B�I�I�I" + action_final);
					}
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// �����̈��S���m�ۂ�����ԂŁA������댯�ɂł���P�[�X��T���B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// �y�A�U��
		if (action_final == -1 && numVisibleTeam == 2) {
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (ai == friend - 10) continue;

				double min = Double.POSITIVE_INFINITY;
				int mina = -1;
				int minb = -1;
				for (int a = 0; a < 6; a++) {
					for (int b = 0; b < 6; b++) {
						double numMe = worstScores[a][b][me - 10][3];
						double numFriend = worstScores[a][b][friend - 10][3];
						double numEnemy = worstScores[a][b][ai][3];
						if (numMe == 0) continue;
						if (numFriend == 0) continue;
						if (numEnemy == 0) continue;
						double scoreMe = worstScores[a][b][me - 10][1];
						double scoreFriend = worstScores[a][b][friend - 10][1];
						double scoreEnemy = worstScores[a][b][ai][0] - Math.log(numEnemy);
						if (scoreMe < attackThreshold || scoreFriend < attackThreshold) continue;
						if (scoreEnemy < min) {
							min = scoreEnemy;
							mina = a;
							minb = b;
						}
					}
				}

				if (min < -25) {
					System.out.println("�y�A�Œǂ��l�߉\�Hai=" + ai + ", amin=" + mina + ", bmin=" + minb);
					System.out.println("�I�I�I");
				}
			}
		}

		// �P��
		if (action_final == -1) {
			int action_attack = -1;
			for (int ai = 0; ai < 4; ai++) {
				if (ai == me - 10) continue;
				if (ai == friend - 10) continue;
				double min = Double.POSITIVE_INFINITY;
				int mina = -1;
				for (int a = 0; a < 6; a++) {
					double scoreMe = safetyScore[me - 10][a];
					double scoreEnemy = safetyScore[ai][a];
					if (Double.isNaN(scoreMe)) continue;
					if (Double.isNaN(scoreEnemy)) continue;
					if (scoreMe < attackThreshold) continue;
					if (scoreEnemy < min) {
						min = scoreEnemy;
						mina = a;
					}
				}

				if (min < -22) {
					action_attack = mina;
				}
			}
			if (action_attack != -1) {
				action_final = action_attack;
				reason = "�U������ׂ��B";
				System.out.println("�ǂ��l�߉\�Ha=" + action_attack);
				System.out.println("�I�I�I");
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// TODO �A�C�e��������Ȃ�A�A�C�e�������ɍs���B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (action_final == -1) {
			int minx = -1, miny = -1, mindis = Integer.MAX_VALUE;
			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					int type = map.getType(x, y);
					if (Constant.isItem(type)) {
						int ddd = (int) dis.data[x][y];
						if (ddd < mindis) {
							mindis = ddd;
							minx = x;
							miny = y;
						}
					}
				}
			}

			if (mindis < Integer.MAX_VALUE) {
				int action = BBMUtility.ComputeFirstDirection(dis, minx, miny);
				double score = safetyScore[me - 10][action];
				if (score > worstScoreThreshold) {
					action_final = action;
					reason = "�V�K�B�A�C�e������邽�߂Ɉړ�����B";
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// TODO �؂�����Ȃ�A�؂��󂵂ɍs���B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (action_final == -1) {
			int minx = -1, miny = -1, mindis = Integer.MAX_VALUE;
			for (int x = 0; x < numField; x++) {
				for (int y = 0; y < numField; y++) {
					int power = map.getPower(x, y);
					if (power > 0) continue;
					int ddd = (int) dis.data[x][y];
					if (ddd < mindis) {
						int num = BBMUtility.numWoodBrakable(board, x, y, ab.strength);
						if (num > 0) {
							mindis = ddd;
							minx = x;
							miny = y;
						}
					}
				}
			}

			if (action_final == -1 && mindis == 0) {
				// Wood���󂹂�ʒu�ɂ�����A���e��u���B
				if (ab.numBombHold > 0) {
					int action = 5;
					double score = safetyScore[me - 10][action];
					if (score > worstScoreThreshold) {
						action_final = action;
						reason = "�V�K�B�؂��󂷂��߂ɔ��e��ݒu����B";
					}
				}
			}

			if (action_final == -1 && mindis < Integer.MAX_VALUE && mindis > 0) {
				// Wood���󂹂�ꏊ���݂�������A�������Ɉړ�����B
				int action = BBMUtility.ComputeFirstDirection(dis, minx, miny);
				double score = safetyScore[me - 10][action];
				if (score > worstScoreThreshold) {
					action_final = action;
					reason = "�V�K�B�؂��󂷂��߂Ɉړ�����B";
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// TODO ���S�ȑI�������烉���_���ɑI�ԁB
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//
		// TODO ��̑I�����Ŋ댯�ȏ�ԂɊׂ�Ȃ�A�����Ƃ����S�ȃA�N�V���������B
		//
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if (action_final == -1) {
			double scoreMax = Double.NEGATIVE_INFINITY;
			for (int action = 0; action < 6; action++) {
				double score = safetyScore[me - 10][action];
				if (score > scoreMax) {
					scoreMax = score;
					action_final = action;
					reason = "�����Ƃ����S�ȑI������I�ԁB";
				}
			}

			if (verbose) {
				if (scoreMax <= 2) {
					System.out.println("��΂��󋵁I�I");
				}
			}
		}

		String line = String.format("%s, action=%d", reason, action_final);
		System.out.println(line);

		return action_final;
	}
}
